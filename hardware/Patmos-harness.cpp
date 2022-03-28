/*
NOTES ON STUFF MISSING FROM THE OLD EMULATOR
- generating the emulator_config. These care hardcoded below for now
- VCD dump
- init_extmem - should not be needed as the current state of the new emulator
  uses hashmap for memory. Only for testing width of data channel to memory.
  but i haven't found a way to get signal width from verilator.

*/
#include "emulator_config.h"


//OTHER INTEMEDIATE HARDCODE




#include <fstream>
#include <iostream>
#include <string>
#include <libelf.h>
#include <gelf.h>
#include <sys/poll.h>
#include <fcntl.h>

#include "VPatmos.h"
#include "verilated.h"
#if VM_TRACE
#include "verilated_vcd_c.h"
#endif
#if CORE_COUNT > 1
#include "VPatmos_PatmosCore.h"
#endif

#define OCMEM_ADDR_BITS 16

typedef uint64_t val_t;

using namespace std;

class Emulator
{
  unsigned long m_tickcount;
  public: VPatmos *c;
  VerilatedVcdC	*c_trace;
  // For Uart:
  bool UART_on;
  // RGMII
  bool RGMII_on;
  int baudrate;
  int freq;
  char in_byte;
  char out_byte;
  int sample_counter_out;
  int sample_counter_in;
  int bit_counter_out;
  int bit_counter_in;
  char state;
  bool writing;
  string write_str;
  int write_cntr;
  int write_len;
  bool trace;
  // For multi-clock simulation
  const int c80_period =  12500/2;
  const int c125_period = 8000/2;
  const int c125_90_period = 8000/2;
  unsigned long c80 = c80_period;
  unsigned long c125 = c125_period;
  unsigned long c125_90 = c125_90_period + c125_90_period*0.5;

  ostream *outputTarget = &std::cout;

  //elf - mem - ram
  #ifdef EXTMEM_SSRAM32CTRL
  uint32_t *ram_buf;
  #endif /* EXTMEM_SSRAM32CTRL */

  #ifdef EXTMEM_SRAMCTRL
  uint16_t *ram_buf;
  #endif /* EXTMEM_SRAMCTRL */

public:
  Emulator(void)
  {
    Verilated::traceEverOn(true);
    c = new VPatmos;
    m_tickcount = 0l;

    //for UART
    UART_on = false;
    c->io_UartCmp_rx = 1; // keep UART tx high when idle
    outputTarget = &cout; // default uart print to terminal

    //for RGMII
    RGMII_on = false;
    c->io_FPGAsharkMAC_rgmii_txd = 1; // keep RGMII tx high when idle

    #ifdef EXTMEM_SSRAM32CTRL
    ram_buf = (uint32_t *)calloc(1 << EXTMEM_ADDR_BITS, sizeof(uint32_t));
    #endif /* EXTMEM_SSRAM32CTRL */

    #ifdef EXTMEM_SRAMCTRL
    ram_buf = (uint16_t *)calloc(1 << EXTMEM_ADDR_BITS, sizeof(uint16_t));
    #endif /* EXTMEM_SRAMCTRL */

    trace = false;

  }

  ~Emulator(void)
  {
    delete c;
    c = NULL;

    if (trace){
      if (c_trace) {
        c_trace->close();
        c_trace = NULL;
      }
    }
  }

  void setTrace(){
    trace = true;
    if (!c_trace){
      c_trace = new VerilatedVcdC;
			c->trace(c_trace, 99);
			c_trace->open("Patmos.vcd");
    }
  }

  void stopTrace(){
    if (trace){
      if (c_trace) {
        c_trace->close();
        c_trace = NULL;
      }
    }
    trace = false;
  }

  void reset(int cycles)
  {
    c->reset = 1;
    // Make sure any inheritance gets applied
    for (int i = 0; i < cycles; i++)
    {
      this->tick(STDIN_FILENO, STDOUT_FILENO, STDIN_FILENO, STDOUT_FILENO);
    }
    c->reset = 0;
  }

  unsigned long mintime(){
    // Function to find mintime of the three clocks
    return std::min(std::min(c80,c125),c125_90);
  }

  void tick(int uart_in,int uart_out, int RGMII_in, int RGMII_out)
  {
    // Increment our own internal time reference
    m_tickcount++;

    // Make sure any combinatorial logic depending upon
    // inputs that may have changed before we called tick()
    // has settled before the rising edge of the clock.
    unsigned long time = 0; // Global time tracker

    bool c80_zeroed = false; // Flag to register whether c80 is at 0
    // This while loop adds the functionallity of simulating with
    // multiple clocks. It keeps track of the individual clocks
    // and at which point they need to be clocked.
    while(!c80_zeroed){
      unsigned long inc_time = mintime();
      time += inc_time;
      c80 -= inc_time;
      c125 -= inc_time;
      c125_90 -= inc_time;

      if(c80 == 0){ // 80 MHz clock
        c80 = c80_period;
        c->clock = 0;
        c80_zeroed = true;
      }

      if(c125 == 0){ // 125 MHz clock
        c125 = c125_period;
        c->io_FPGAsharkMAC_gtx_clk = !c->io_FPGAsharkMAC_gtx_clk;
      }
      if(c125_90 == 0){ // 125 MHz phase shifted 90 deg clock
        c125_90 = c125_90_period;
        c->io_FPGAsharkMAC_gtx_clk90 = !c->io_FPGAsharkMAC_gtx_clk90;
      }
      c->eval();
      if (trace) {
        c_trace->dump((c80_period*2)*m_tickcount+time+1);
      }
    }
    // Toggle the clock
    // Rising edge
    c80_zeroed = false;
    // Does the same as above, but for the rising edge case
    while(!c80_zeroed){
      unsigned long inc_time = mintime();
      time += inc_time;
      c80 -= inc_time;
      c125 -= inc_time;
      c125_90 -= inc_time;

      if(c80 == 0){ // 80 MHz clock
        c80 = c80_period;
        c->clock = 1;
        c80_zeroed = true;
      }

      if(c125 == 0){ // 125 MHz clock
        c125 = c125_period;
        c->io_FPGAsharkMAC_gtx_clk = !c->io_FPGAsharkMAC_gtx_clk;
      }
      if(c125_90 == 0){ // 125 MHz phase shifted 90 deg clock
        c125_90 = c125_90_period;
        c->io_FPGAsharkMAC_gtx_clk90 = !c->io_FPGAsharkMAC_gtx_clk90;
        if (RGMII_on) //Check for RGMII emulation each 125MHz tick
        {
          emu_RGMII(RGMII_in, RGMII_out);
        }
      }
      c->eval();
      if (trace && !c80_zeroed) {
        c_trace->dump((c80_period*2)*m_tickcount+time+2);
      }

    }

    //UART emulation
    if (UART_on)
    {
      emu_uart(uart_in, uart_out);
    }

    if (trace) {
      c_trace->dump((c80_period*2)*m_tickcount+time+3);
      c_trace->flush();
    }
  }

  long int get_tick_count(void)
  {
    return m_tickcount;
  }

  bool done(void) { return (Verilated::gotFinish()); }

  // UART methods below
  void UART_init()
  {
    baudrate = BAUDRATE;
    freq = FREQ;
    UART_on = true;
  }

  void RGMII_init()
  {
    baudrate = BAUDRATE;
    freq = FREQ;
    RGMII_on = true;
  }


  void emu_uart(int uart_in,int uart_out) {//int uart_in, int uart_out
    static unsigned baud_counter = 0;

    // Pass on data from UART
    if (c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Cmd == 0x1
        && (c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Addr & 0xff) == 0x04) {
      unsigned char d = c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Data;
      int w = write(uart_out, &d, 1);
      if (w != 1) {
        cerr << "patemu: error: Cannot write UART output" << endl;
      }
    }

    // Pass on data to UART
    bool baud_tick = c->Patmos__DOT__UartCmp__DOT__uart__DOT__tx_baud_tick;
    if (baud_tick) {
      baud_counter = (baud_counter + 1) % 10;
    }
    if (baud_tick && baud_counter == 0) {
      struct pollfd pfd;
      pfd.fd = uart_in;
      pfd.events = POLLIN;
      if (poll(&pfd, 1, 0) > 0) {
        unsigned char d;
        int r = read(uart_in, &d, 1);
        if (r != 0) {
          if (r != 1) {
            cerr << "patemu: error: Cannot read UART input" << endl;
          } else {
            c->Patmos__DOT__UartCmp__DOT__uart__DOT__rx_state = 0x3; // rx_stop_bit
            c->Patmos__DOT__UartCmp__DOT__uart__DOT__rx_baud_tick = 1;
            c->Patmos__DOT__UartCmp__DOT__uart__DOT__rxd_reg2 = 1;
            c->Patmos__DOT__UartCmp__DOT__uart__DOT__rx_buff = d;
          }
        }
      }
    }
  }


  void emu_RGMII(int RGMII_in,int RGMII_out) {
    static unsigned baud_counter = 0;

    // Seperate Chisel code neccessary?
  /*if (c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Cmd == 0x1
        && (c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Addr & 0xff) == 0x04) {
      unsigned char d = c->Patmos__DOT__UartCmp__DOT__uart__DOT__uartOcpEmu_Data;
      int w = write(RGMII_out, &d, 1);
      if (w != 1) {
        cerr << "patemu: error: Cannot write RGMII output" << endl;
      }
    }
   */
    // Pass on data to RGMII
    bool baud_tick = c->io_FPGAsharkMAC_gtx_clk90;
    if (baud_tick) {
      baud_counter = (baud_counter + 1) % 10;
    }
    if (baud_tick && baud_counter == 0) {
      struct pollfd pfd;
      pfd.fd = RGMII_in;
      pfd.events = POLLIN;
      if (poll(&pfd, 1, 0) > 0) {
        unsigned char d;
        int r = read(RGMII_in, &d, 1);
        if (r != 0) {
          if (r != 1) {
            cerr << "patemu: error: Cannot read RGMII input" << endl;
          } else {
            c->io_FPGAsharkMAC_rgmii_tx_ctl = 0x3; // rx_stop_bit
            c->io_FPGAsharkMAC_rgmii_txd = d;
          }
        }
      }
    }
  }

  void emu_keys(void){
#ifdef IO_KEYS
    if ((rand() % 0x10000) == 0) {
      c->io_Keys_key = rand();
    }
#endif
  }

  void UART_to_file(string path)
  {
    static ofstream outFile;
    if (!outFile.is_open())
    {
      outFile.open(path);
    }
    outputTarget = &outFile;
  }

  void UART_to_console(void)
  {
    outputTarget = &cout;
  }

  //static val_t readelf(istream &is, Patmos_t *c)
  val_t readelf(istream &is)
  {
    vector<unsigned char> elfbuf;
    elfbuf.reserve(1 << 20);

    // read the whole stream.
    while (!is.eof())
    {
      char buf[1024];

      // read into buffer
      is.read(&buf[0], sizeof(buf));

      // check how much was read
      streamsize count = is.gcount();
      assert(count <= 1024);

      // write into main memory
      for (unsigned i = 0; i < count; i++)
        elfbuf.push_back(buf[i]);
    }

    // check libelf version
    elf_version(EV_CURRENT);

    // open elf binary
    Elf *elf = elf_memory((char *)&elfbuf[0], elfbuf.size());
    assert(elf);

    // check file kind
    Elf_Kind ek = elf_kind(elf);
    if (ek != ELF_K_ELF)
    {
      cerr << "readelf: ELF file must be of kind ELF.\n";
      exit(EXIT_FAILURE);
    }

    // get elf header
    GElf_Ehdr hdr;
    GElf_Ehdr *tmphdr = gelf_getehdr(elf, &hdr);
    assert(tmphdr);

    if (hdr.e_machine != 0xBEEB)
    {
      cerr << "readelf: unsupported architecture: ELF file is not a Patmos ELF file.\n";
      exit(EXIT_FAILURE);
    }

    // check class
    int ec = gelf_getclass(elf);
    if (ec != ELFCLASS32)
    {
      cerr << "readelf: unsupported architecture: ELF file is not a 32bit Patmos ELF file.\n";
      exit(EXIT_FAILURE);
    }

    // get program headers
    size_t n;
    int ntmp = elf_getphdrnum(elf, &n);
    assert(ntmp == 0);

    for (size_t i = 0; i < n; i++)
    {
      // get program header
      GElf_Phdr phdr;
      GElf_Phdr *phdrtmp = gelf_getphdr(elf, i, &phdr);
      assert(phdrtmp);

      if (phdr.p_type == PT_LOAD)
      {
        // some assertions
        //assert(phdr.p_vaddr == phdr.p_paddr);
        assert(phdr.p_filesz <= phdr.p_memsz);

        // copy from the buffer into the on-chip memories
        for (size_t k = 0; k < phdr.p_memsz; k++)
        {
          if ((phdr.p_flags & PF_X) != 0 &&
              ((phdr.p_paddr + k) >> OCMEM_ADDR_BITS) == 0x1 &&
              ((phdr.p_paddr + k) & 0x3) == 0)
          {
            // Address maps to ISPM and is at a word boundary

            val_t word = k >= phdr.p_filesz ? 0 :
              (((val_t)elfbuf[phdr.p_offset + k + 0] << 24) |
               ((val_t)elfbuf[phdr.p_offset + k + 1] << 16) |
               ((val_t)elfbuf[phdr.p_offset + k + 2] << 8) |
               ((val_t)elfbuf[phdr.p_offset + k + 3] << 0));
            val_t addr = ((phdr.p_paddr + k) - (0x1 << OCMEM_ADDR_BITS)) >> 3;
            #if CORE_COUNT == 1
            unsigned size = (sizeof(c->Patmos__DOT__cores_0__DOT__fetch__DOT__MemBlock__DOT__mem) / //ANTHON THIS MIGHT BE WRONG - SHOULD BE OKAY
                             sizeof(c->Patmos__DOT__cores_0__DOT__fetch__DOT__MemBlock__DOT__mem[0]));
            #endif
            #if CORE_COUNT > 1
            unsigned size = (sizeof(c->__PVT__Patmos__DOT__cores_0->__PVT__fetch__DOT__MemBlock__DOT__mem) / //ANTHON THIS MIGHT BE WRONG - SHOULD BE OKAY
                             sizeof(c->__PVT__Patmos__DOT__cores_0->__PVT__fetch__DOT__MemBlock__DOT__mem[0]));
            #endif
            assert(addr < size && "Instructions mapped to ISPM exceed size");

            // Write to even or odd block
            #if CORE_COUNT == 1
            if (((phdr.p_paddr + k) & 0x4) == 0)
            {

              c->Patmos__DOT__cores_0__DOT__fetch__DOT__MemBlock__DOT__mem[addr] = word;
            }
            else
            {
              c->Patmos__DOT__cores_0__DOT__fetch__DOT__MemBlock_1__DOT__mem[addr] = word;
            }
            #endif
            #if CORE_COUNT > 1
            if (((phdr.p_paddr + k) & 0x4) == 0)
            {

              c->__PVT__Patmos__DOT__cores_0->__PVT__fetch__DOT__MemBlock__DOT__mem[addr] = word;
            }
            else
            {
              c->__PVT__Patmos__DOT__cores_0->__PVT__fetch__DOT__MemBlock_1__DOT__mem[addr] = word;
            }
            #endif

          }

          if (((phdr.p_paddr + k) & 0x3) == 0)
          {
            // Address maps to SRAM and is at a word boundary
            val_t word = k >= phdr.p_filesz ? 0 : (((val_t)elfbuf[phdr.p_offset + k + 0] << 24) | ((val_t)elfbuf[phdr.p_offset + k + 1] << 16) | ((val_t)elfbuf[phdr.p_offset + k + 2] << 8) | ((val_t)elfbuf[phdr.p_offset + k + 3] << 0));

            val_t addr = ((phdr.p_paddr + k) >> 2);
            write_extmem(addr, word);
          }
        }
      }
    }

    // get entry point
    val_t entry = hdr.e_entry;

    elf_end(elf);

    return entry;
  }

#ifdef EXTMEM_SSRAM32CTRL //TODO test this
  void write_extmem(val_t address, val_t word)
  {
    ram_buf[address] = word; // This gives segmentation fault dumb on second run!
  }

  void init_extmem() {
    //only needed for random init
    for (int i = 0; i < (1 << EXTMEM_ADDR_BITS); i++) {
      write_extmem(i, rand());
    }
  }

static void emu_extmem() {
  static uint32_t addr_cnt;
  static uint32_t address;
  static uint32_t counter;

  // Start of request
  if (c->io_sSRam32CtrlPins_ramOut_nadsc != 1) {
    address = c->Patmos__io_sSRam32CtrlPins_ramOut_addr.to_ulong();
    addr_cnt = address;
    counter = 0;
  }

  // Advance address for burst
  if (c->io_sSRam32CtrlPins_ramOut_nadv != 1) {
    addr_cnt++;
  }

  // Read from external memory
  if (c->io_sSRam32CtrlPins_ramOut_noe != 1) {
    counter++;
    if (counter >= SRAM_CYCLES) {
      c->io_sSRam32CtrlPins_ramIn_din = ram_buf[address];
      if (address <= addr_cnt) {
        address++;
      }
    }
  }

  // Write to external memory
  if (c->io_sSRam32CtrlPins_ramOut_nbwe == 0) {
    uint32_t nbw = c->io_sSRam32CtrlPins_ramOut_nbw;
    uint32_t mask = 0x00000000;
    for (unsigned i = 0; i < 4; i++) {
      if ((nbw & (1 << i)) == 0) {
        mask |= 0xff << (i*8);
      }
    }

    ram_buf[address] &= ~mask;
    ram_buf[address] |= mask & ((unsigned long int) c->io_sSRam32CtrlPins_ramOut_dout);

    if (address <= addr_cnt) {
      address++;
    }
  }
}
#elif defined EXTMEM_SRAMCTRL
  void write_extmem(val_t address, val_t word) {
    ram_buf[(address << 1) | 0] = word & 0xffff;
    ram_buf[(address << 1) | 1] = word >> 16;
  }

  void init_extmem() {
    //only needed for random init
    for (int i = 0; i < (1 << EXTMEM_ADDR_BITS)/2; i++) {
      write_extmem(i, rand());
    }
  }

  void emu_extmem() {
    uint32_t address = (uint32_t) c->io_SRamCtrl_ramOut_addr;
    // Read from external memory unconditionally
    c->io_SRamCtrl_ramIn_din = ram_buf[address];

    // Write to external memory
    if (c->io_SRamCtrl_ramOut_nwe != 1) {
      uint16_t mask = 0x0000;
      if (c->io_SRamCtrl_ramOut_nub != 1) {
        mask |= 0xff00;
      }
      if (c->io_SRamCtrl_ramOut_nlb != 1) {
        mask |= 0x00ff;
      }
      ram_buf[address] &= ~mask;
      ram_buf[address] |= mask & ((unsigned long int) c->io_SRamCtrl_ramOut_dout);
    }
  }
#else
void write_extmem(val_t address, val_t word) {}
void init_extmem() {}
void emu_extmem() {}
#endif


  void init_icache(val_t entry)
  {

    tick(STDIN_FILENO, STDOUT_FILENO,STDIN_FILENO, STDOUT_FILENO);
    if (entry != 0)
    {
      if (entry >= 0x20000)
      {
#ifdef ICACHE_METHOD
        //init for method cache
#if CORE_COUNT == 1
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__pcReg = -1;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__pcNext = -1;
        c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__hitReg = 0;
        c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__hitNext = 0;
// add multicore support, at the moment only for the method cache and not the ISPM
#endif


#if CORE_COUNT > 1
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__hitNext = 0;

        c->__PVT__Patmos__DOT__cores_1->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_1->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_1->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_1->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 2
        c->__PVT__Patmos__DOT__cores_2->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_2->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_2->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_2->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 3
        c->__PVT__Patmos__DOT__cores_3->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_3->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_3->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_3->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 4
        c->__PVT__Patmos__DOT__cores_4->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_4->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_4->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_4->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 5
        c->__PVT__Patmos__DOT__cores_5->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_5->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_5->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_5->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 6
        c->__PVT__Patmos__DOT__cores_6->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_6->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_6->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_6->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 7
        c->__PVT__Patmos__DOT__cores_7->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_7->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_7->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_7->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 8
        c->__PVT__Patmos__DOT__cores_8->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_8->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_8->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_8->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 9
        c->__PVT__Patmos__DOT__cores_9->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_9->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_9->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_9->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 10
        c->__PVT__Patmos__DOT__cores_10->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_10->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_10->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_10->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 11
        c->__PVT__Patmos__DOT__cores_11->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_11->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_11->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_11->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 12
        c->__PVT__Patmos__DOT__cores_12->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_12->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_12->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_12->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 13
        c->__PVT__Patmos__DOT__cores_13->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_13->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_13->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_13->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 14
        c->__PVT__Patmos__DOT__cores_14->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_14->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_14->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_14->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 15
        c->__PVT__Patmos__DOT__cores_15->fetch__DOT__pcReg = -1;
        c->__PVT__Patmos__DOT__cores_15->fetch__DOT__pcNext = -1;
        c->__PVT__Patmos__DOT__cores_15->icache__DOT__repl__DOT__hitReg = 0;
        c->__PVT__Patmos__DOT__cores_15->icache__DOT__repl__DOT__hitNext = 0;
#endif
#if CORE_COUNT > 16
#warning "Core count is currently limited to 16 in Patmos-harness.cpp"
#endif
#endif /* ICACHE_METHOD */
#ifdef ICACHE_LINE
        //init for icache
        #if CORE_COUNT == 1
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__pcNext = (entry >> 2) - 1;
        #endif
        #if CORE_COUNT > 1
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__pcNext = (entry >> 2) - 1;
        #endif
#endif /* ICACHE_LINE */
#if CORE_COUNT == 1
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__relBaseReg = 0;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__relocReg = (entry >> 2) - 1;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__selCache = 1;
        c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 1
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__selCacheReg = 1;

        c->__PVT__Patmos__DOT__cores_1->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_1->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_1->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_1->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 2
        c->__PVT__Patmos__DOT__cores_2->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_2->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_2->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_2->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 3
        c->__PVT__Patmos__DOT__cores_3->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_3->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_3->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_3->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 4
        c->__PVT__Patmos__DOT__cores_4->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_4->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_4->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_4->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 5
        c->__PVT__Patmos__DOT__cores_5->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_5->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_5->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_5->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 6
        c->__PVT__Patmos__DOT__cores_6->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_6->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_6->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_6->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 7
        c->__PVT__Patmos__DOT__cores_7->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_7->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_7->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_7->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 8
        c->__PVT__Patmos__DOT__cores_8->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_8->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_8->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_8->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 9
        c->__PVT__Patmos__DOT__cores_9->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_9->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_9->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_9->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 10
        c->__PVT__Patmos__DOT__cores_10->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_10->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_10->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_10->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 11
        c->__PVT__Patmos__DOT__cores_11->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_11->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_11->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_11->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 12
        c->__PVT__Patmos__DOT__cores_12->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_12->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_12->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_12->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 13
        c->__PVT__Patmos__DOT__cores_13->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_13->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_13->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_13->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 14
        c->__PVT__Patmos__DOT__cores_14->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_14->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_14->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_14->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
#if CORE_COUNT > 15
        c->__PVT__Patmos__DOT__cores_15->fetch__DOT__relBaseReg = 0;
        c->__PVT__Patmos__DOT__cores_15->fetch__DOT__relocReg = (entry >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_15->fetch__DOT__selCache = 1;
        c->__PVT__Patmos__DOT__cores_15->icache__DOT__repl__DOT__selCacheReg = 1;
#endif
      }
      else
      {
        // pcReg for ispm starts at entry point - ispm base
#if CORE_COUNT == 1
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__pcNext = ((entry - 0x10000) >> 2) - 1;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__relBaseNext = (entry - 0x10000) >> 2;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__relocNext = 0x10000 >> 2;
        c->Patmos__DOT__cores_0__DOT__fetch__DOT__selSpmNext = 1;
        c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__selSpmNext = 1;
#endif
#if CORE_COUNT > 1
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__pcNext = ((entry - 0x10000) >> 2) - 1;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relBaseNext = (entry - 0x10000) >> 2;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relocNext = 0x10000 >> 2;
        c->__PVT__Patmos__DOT__cores_0->fetch__DOT__selSpmNext = 1;
        c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__selSpmNext = 1;
#endif
      }
#if CORE_COUNT == 1
      c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 1
      c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);

      c->__PVT__Patmos__DOT__cores_1->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 2
      c->__PVT__Patmos__DOT__cores_2->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 3
      c->__PVT__Patmos__DOT__cores_3->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 4
      c->__PVT__Patmos__DOT__cores_4->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 5
      c->__PVT__Patmos__DOT__cores_5->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 6
      c->__PVT__Patmos__DOT__cores_6->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 7
      c->__PVT__Patmos__DOT__cores_7->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 8
      c->__PVT__Patmos__DOT__cores_8->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 9
      c->__PVT__Patmos__DOT__cores_9->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 10
      c->__PVT__Patmos__DOT__cores_10->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 11
      c->__PVT__Patmos__DOT__cores_11->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 12
      c->__PVT__Patmos__DOT__cores_12->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 13
      c->__PVT__Patmos__DOT__cores_13->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 14
      c->__PVT__Patmos__DOT__cores_14->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 15
      c->__PVT__Patmos__DOT__cores_15->icache__DOT__repl__DOT__callRetBaseNext = (entry >> 2);
#endif

#ifdef ICACHE_METHOD
#if CORE_COUNT == 1
      c->Patmos__DOT__cores_0__DOT__icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 1
      c->__PVT__Patmos__DOT__cores_0->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);

      c->__PVT__Patmos__DOT__cores_1->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 2
      c->__PVT__Patmos__DOT__cores_2->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 3
      c->__PVT__Patmos__DOT__cores_3->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 4
      c->__PVT__Patmos__DOT__cores_4->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 5
      c->__PVT__Patmos__DOT__cores_5->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 6
      c->__PVT__Patmos__DOT__cores_6->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 7
      c->__PVT__Patmos__DOT__cores_7->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 8
      c->__PVT__Patmos__DOT__cores_8->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 9
      c->__PVT__Patmos__DOT__cores_9->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 10
      c->__PVT__Patmos__DOT__cores_10->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 11
      c->__PVT__Patmos__DOT__cores_11->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 12
      c->__PVT__Patmos__DOT__cores_12->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 13
      c->__PVT__Patmos__DOT__cores_13->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 14
      c->__PVT__Patmos__DOT__cores_14->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 15
      c->__PVT__Patmos__DOT__cores_15->icache__DOT__ctrl__DOT__callRetBaseNext = (entry >> 2);
#endif
#endif /* ICACHE_METHOD */
#ifdef ICACHE_LINE
#if CORE_COUNT == 1
      c->Patmos__DOT__cores_0__DOT__fetch__DOT__relBaseNext = (entry >> 2);
#endif
#if CORE_COUNT > 1
      c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relBaseNext = (entry >> 2);
#endif
#endif /* ICACHE_LINE */
    }
  }
  void print_state()
  {
    static unsigned int baseReg = 0;
    #if CORE_COUNT == 1
    *outputTarget << ((baseReg + c->Patmos__DOT__cores_0__DOT__fetch__DOT__pcNext) * 4 - c->Patmos__DOT__cores_0__DOT__fetch__DOT__relBaseNext * 4) << " - ";
    baseReg = c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__callRetBaseNext;

    for (unsigned i = 0; i < 32; i++) {
      *outputTarget << c->Patmos__DOT__cores_0__DOT__decode__DOT__rf__DOT__rf[i] << " ";
    }
    #endif
    #if CORE_COUNT > 1
      *outputTarget << ((baseReg + c->__PVT__Patmos__DOT__cores_0->fetch__DOT__pcNext) * 4 - c->__PVT__Patmos__DOT__cores_0->fetch__DOT__relBaseNext * 4) << " - ";
    baseReg = c->__PVT__Patmos__DOT__cores_0->icache__DOT__repl__DOT__callRetBaseNext;

    for (unsigned i = 0; i < 32; i++) {
      *outputTarget << c->__PVT__Patmos__DOT__cores_0->__PVT__decode__DOT__rf__DOT__rf[i] << " ";
    }
    #endif

    *outputTarget << endl;
  }

};

// Override Verilator definition so first $finish ends simulation
// Note: VL_USER_FINISH needs to be defined when compiling Verilator code
void vl_finish(const char *filename, int linenum, const char *hier)
{
  Verilated::flushCall();
  exit(0);
}

static void usage(ostream &out, const char *name) {
  out << "Usage: " << name
      << " <options> [file]" << endl;
}

static void help(ostream &out) {
  out << endl << "Options:" << endl
      << "  -h            Print this help" << endl
      << "  -i            Initialize memory with random values" << endl
      << "  -l <N>        Stop after <N> cycles" << endl
      << "  -v            Dump wave forms file \"Patmos.vcd\"" << endl
      << "  -r            Print register values in each cycle" << endl
      #ifdef IO_KEYS
      << "  -k            Simulate random input from keys" << endl
      #endif /* IO_KEYS */
      #ifdef IO_UART
      << "  -I <file>     Read input for UART from file <file>" << endl
      << "  -O <file>     Write output from UART to file <file>" << endl
      #endif
      #ifdef IO_RGMII
      << "   -L <file>    Read input from file for RGMII <file>" << endl
      << "   -S <file>    Write output to file for RGMII <file>" << endl
      #endif
  ;
}


int main(int argc, char **argv, char **env)
{
  Verilated::commandArgs(argc, argv);
  Emulator *emu = new Emulator();
  int opt;
  int limit = -1;
  bool halt = false;
  bool reg_print = false;

  int uart_in = STDIN_FILENO;
  int uart_out = STDOUT_FILENO;
  int RGMII_in = STDIN_FILENO;
  int RGMII_out = STDOUT_FILENO;
  bool keys = false;

  //Parse Arguments
  while ((opt = getopt(argc, argv, "hvl:iO:I:rk")) != -1){
    switch (opt) {
      case 'v':
        emu->setTrace();
        break;
      case 'l':
        limit = atoi(optarg);
        break;
      case 'i':
        emu->init_extmem();
        break;
      #ifdef IO_UART
      case 'I':
        if (strcmp(optarg, "-") == 0) {
          uart_in = STDIN_FILENO;
        } else {
          uart_in = open(optarg, O_RDONLY);
          if (uart_in < 0) {
            cerr << argv[0] << "error: Cannot open input file " << optarg << endl;
            exit(EXIT_FAILURE);
          }
        }
        break;
      case 'O':
        if (strcmp(optarg, "-") == 0) {
          uart_out = STDOUT_FILENO;
        } else {
          uart_out = open(optarg, O_WRONLY|O_CREAT|O_TRUNC, 0644);
          if (uart_out < 0) {
            cerr << argv[0] << ": error: Cannot open output file " << optarg << endl;
            exit(EXIT_FAILURE);
          }
        }
        break;
      #endif
      #ifdef IO_RGMII
      case 'L':
        if (strcmp(optarg, "-") == 0) {
          RGMII_in = STDIN_FILENO;
        } else {
          RGMII_in = open(optarg, O_RDONLY);
          if (RGMII_in < 0) {
            cerr << argv[0] << "error: Cannot open input file " << optarg << endl;
            exit(EXIT_FAILURE);
          }
        }
        break;
      case 'S':
        if (strcmp(optarg, "-") == 0) {
          RGMII_out = STDOUT_FILENO;
        } else {
          RGMII_out = open(optarg, O_WRONLY|O_CREAT|O_TRUNC, 0644);
          if (RGMII_out < 0) {
            cerr << argv[0] << ": error: Cannot open output file " << optarg << endl;
            exit(EXIT_FAILURE);
          }
        }
        break;
      #endif
      case 'r':
        reg_print = true;
        break;
      #ifdef IO_KEYS
      case 'k':
      keys = true;
      break;
      #endif /* IO_KEYS */
      case 'h':
        usage(cout, argv[0]);
        help(cout);
        exit(EXIT_SUCCESS);
      default: /* '?' */
        usage(cerr, argv[0]);
        cerr << "Try '" << argv[0] << " -h' for more information" << endl;
        exit(EXIT_FAILURE);
    }
  }


  emu->reset(1);
  emu->tick(uart_in, uart_out, RGMII_in, RGMII_out);
  emu->UART_init();
  emu->RGMII_init();

  val_t entry = 0;
  if (optind < argc)
  {
    ifstream *fs = new ifstream(argv[optind]);
    if (!fs->good())
    {
      cerr << "Error: Cannot open elf file " << endl;
      exit(EXIT_FAILURE);
    }
    entry = emu->readelf(*fs);
  }

  emu->reset(5);
  emu->tick(uart_in, uart_out, RGMII_in, RGMII_out);

  emu->init_icache(entry);


  int cnt = 0;
  int waituart = 0;
  if(reg_print){
    printf("Patmos start\n");
  }
  while (limit < 0 || emu->get_tick_count() < limit)
  {
    cnt++;
    emu->tick(uart_in, uart_out, RGMII_in, RGMII_out);
    if(keys){
      emu->emu_keys();
    }
    emu->emu_extmem();
     // Return to address 0 halts the execution after one more iteration
    if (halt) {
      break;
    }
    #if CORE_COUNT == 1
    if (reg_print && emu->c->Patmos__DOT__cores_0__DOT__enableReg) {
      emu->print_state();
    }

    if ((emu->c->Patmos__DOT__cores_0__DOT__memory__DOT__memReg_mem_brcf == 1
         || emu->c->Patmos__DOT__cores_0__DOT__memory__DOT__memReg_mem_ret == 1)
        && emu->c->Patmos__DOT__cores_0__DOT__icache__DOT__repl__DOT__callRetBaseReg == 0) {
      halt = true;
    }
    #endif
    #if CORE_COUNT > 1
    if (reg_print && emu->c->__PVT__Patmos__DOT__cores_0->__PVT__enableReg) {
      emu->print_state();
    }

    if ((emu->c->__PVT__Patmos__DOT__cores_0->__PVT__memory__DOT__memReg_mem_brcf == 1
         || emu->c->__PVT__Patmos__DOT__cores_0->__PVT__memory__DOT__memReg_mem_ret == 1)
        && emu->c->__PVT__Patmos__DOT__cores_0->__PVT__icache__DOT__repl__DOT__callRetBaseReg == 0) {
      halt = true;
    }
    #endif
  }

  emu->stopTrace();
  exit(EXIT_SUCCESS);
}
