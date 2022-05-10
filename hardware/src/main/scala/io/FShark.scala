package io
import chisel3._
import chisel3.util._
import ocp._

object FShark extends DeviceObject {
  // target for sim = SIM / GENERIC, target for synth = ALTERA / XILINX
  var target = "SIM"
  var datawidth = 16

  def init(params: Map[String, String]) = {
    target = getParam(params,"target")
    datawidth = getIntParam(params, "datawidth")
  }

  def create(params: Map[String, String]): FShark = Module(new FShark(target,datawidth))

  trait Pins extends patmos.HasPins {
    override val pins = new Bundle() {
      // Clock and reset logic
      //----------------------
      val gtx_clk = Input(Clock())
      val gtx_clk90 = Input(Clock())
      val gtx_rst = Input(Bool())
      //RGMII interface
      //---------------
      val rgmii_rx_clk = Input(Clock())
      val rgmii_rxd = Input(UInt(4.W))
      val rgmii_rx_ctl = Input(Bool())
      val rgmii_tx_clk = Output(Clock())
      val rgmii_txd = Output(UInt(4.W))
      val rgmii_tx_ctl = Output(Bool())
    }
  }
}

class eth_mac_1gBB(target: String, datawidth: Int) extends BlackBox(Map("TARGET" -> target,"AXIS_DATA_WIDTH" -> datawidth)) {
  // target for sim = SIM / GENERIC, target for synth = ALTERA / XILINX
  val io = IO(new Bundle(){
    // Clock and reset logic
    //----------------------
    val gtx_clk = Input(Clock())
    val gtx_clk90 = Input(Clock())
    val gtx_rst = Input(Bool())
    val logic_clk = Input(Clock())
    val logic_rst = Input(Bool())
    // AXI Input
    //----------
    val tx_axis_tdata = Input(UInt(datawidth.W))
    val tx_axis_tkeep = Input(UInt((datawidth/8).W))
    val tx_axis_tvalid = Input(Bool())
    val tx_axis_tready = Output(Bool())
    val tx_axis_tlast = Input(Bool())
    val tx_axis_tuser = Input(Bool())
    //AXI Output
    //----------
    val rx_axis_tdata = Output(UInt(datawidth.W))
    val rx_axis_tkeep = Output(UInt((datawidth/8).W))
    val rx_axis_tvalid = Output(Bool())
    val rx_axis_tready = Input(Bool())
    val rx_axis_tlast = Output(Bool())
    val rx_axis_tuser = Output(Bool())
    //RGMII interface
    //---------------
    val rgmii_rx_clk = Input(Clock())
    val rgmii_rxd = Input(UInt(4.W))
    val rgmii_rx_ctl = Input(Bool())
    val rgmii_tx_clk = Output(Clock())
    val rgmii_txd = Output(UInt(4.W))
    val rgmii_tx_ctl = Output(Bool())
    //Status
    //------
    val tx_error_underflow = Output(Bool())
    val tx_fifo_overflow = Output(Bool())
    val tx_fifo_bad_frame = Output(Bool())
    val tx_fifo_good_frame = Output(Bool())
    val rx_error_bad_frame = Output(Bool())
    val rx_error_bad_fcs = Output(Bool())
    val rx_fifo_overflow = Output(Bool())
    val rx_fifo_bad_frame = Output(Bool())
    val rx_fifo_good_frame = Output(Bool())
    val speed = Output(UInt(2.W))
    //Configuration
    //-------------
    val ifg_delay = Input(UInt(8.W))
  })
  override def desiredName: String = "eth_mac_1g_rgmii_fifo"
}


// Top file for MAC, filter and circular buffer
class FShark(target: String,datawidth: Int) extends CoreDevice {
  override val io = IO(new CoreDeviceIO() with FShark.Pins {})
  // Initiate FIFO reader
  val endOfFrame = WireInit(false.B)
  val fullFIFO = RegInit(false.B)
  val frameLength = RegInit(0.U(datawidth.W))
  val sendToPatmos = WireInit(false.B)
  val readHolder = RegInit(false.B)
  val writeToFIFO = RegInit(true.B)
  // Initiate OCP interface variables
  val stopFrameRecording = RegInit(false.B)
  val filterIndex = RegInit(0.U((datawidth/2).W))
  val filterValue = RegInit(0.U((datawidth/2).W)) // Only check a byte 16/2 = 8 bit = 1 byte
  val filterSet = RegInit(false.B)
  // Verilog Ethernet MAC blackbox
  val ethmac1g = Module(new eth_mac_1gBB(target,datawidth))
  //Filter for FMAC, input to the Circular buffer
  val FShark_filter = Module(new FShark_filter(datawidth))
  // Connecting MAC and filter
  //--------------------------
  ethmac1g.io.rx_axis_tready := FShark_filter.io.axis_tready
  FShark_filter.io.axis_tvalid := ethmac1g.io.rx_axis_tvalid
  FShark_filter.io.axis_tkeep := ethmac1g.io.rx_axis_tkeep
  FShark_filter.io.axis_tlast := ethmac1g.io.rx_axis_tlast
  FShark_filter.io.axis_tdata := ethmac1g.io.rx_axis_tdata
  // Circular buffer for frame holding
  val CircBuffer = Module(new CircularBuffer(200,datawidth))
  val memFifo = Module(new MemFifo(UInt(datawidth.W),300))
  // Connecting buffer and FIFO
  //---------------------------
  endOfFrame := CircBuffer.io.endOfFrame
  fullFIFO := !memFifo.io.enq.ready
  CircBuffer.io.deq.ready := memFifo.io.enq.ready
  memFifo.io.enq.valid := CircBuffer.io.deq.valid
  memFifo.io.enq.bits := CircBuffer.io.deq.bits
  memFifo.io.deq.ready := RegInit(false.B)
  memFifo.io.writeToFIFO := writeToFIFO
  // Connecting buffer and filter
  //-----------------------------
  CircBuffer.io.filter_bus.bits.flushFrame := FShark_filter.io.filter_bus.bits.flushFrame
  CircBuffer.io.filter_bus.bits.addHeader := FShark_filter.io.filter_bus.bits.addHeader
  CircBuffer.io.filter_bus.bits.tdata := FShark_filter.io.filter_bus.bits.tdata
  CircBuffer.io.filter_bus.valid := FShark_filter.io.filter_bus.valid
  FShark_filter.io.filter_bus.ready := CircBuffer.io.filter_bus.ready

  // Connecting filter and FShark
  //-----------------------------
  FShark_filter.io.filterIndex := filterIndex
  FShark_filter.io.filterValue := filterValue
  FShark_filter.io.filterSet := filterSet
  //----------------------------------
  // Connect the pins straight through
  // Clock and logic
  ethmac1g.io.gtx_clk := io.pins.gtx_clk
  ethmac1g.io.gtx_rst := io.pins.gtx_rst
  ethmac1g.io.gtx_clk90 := io.pins.gtx_clk90
  ethmac1g.io.logic_clk := clock
  ethmac1g.io.logic_rst := reset
  // Configuration
  ethmac1g.io.ifg_delay := WireInit(12.U(datawidth.W))
  // RGMII Interface
  ethmac1g.io.rgmii_rx_clk := io.pins.rgmii_rx_clk
  ethmac1g.io.rgmii_rxd := io.pins.rgmii_rxd
  ethmac1g.io.rgmii_rx_ctl := io.pins.rgmii_rx_ctl
  io.pins.rgmii_tx_clk := ethmac1g.io.rgmii_tx_clk
  io.pins.rgmii_txd := ethmac1g.io.rgmii_txd
  io.pins.rgmii_tx_ctl := ethmac1g.io.rgmii_tx_ctl

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  //Initiate states:
  //----------------
  //Transmitter
  val tx_axis_tvalid_Reg = RegInit(false.B)
  tx_axis_tvalid_Reg := false.B
  ethmac1g.io.tx_axis_tvalid := tx_axis_tvalid_Reg

  // Data to Verilog Ethernet MAC
  val dataWriter= RegInit(0.U(32.W))
  ethmac1g.io.tx_axis_tlast := dataWriter(30)
  ethmac1g.io.tx_axis_tdata := dataWriter(7,0)


  //FIFO handling:
  //--------------
  val idle :: fill :: fill_empty :: waitForEOF :: emptyToPatmos :: Nil = Enum(5)
  val stateReg = RegInit(idle)

  switch(stateReg) {
    is(idle) { // Idle state
      when(endOfFrame) { // When the first EOF occurs go to fill the FIFO
        stateReg := fill
      }
    }

    is(fill) { // Keep filling the FIFO with frame data until trigger or the FIFO is full

      when(stopFrameRecording) { // Trigger, that determines that the current data is to be sent along
        stateReg := waitForEOF
      }
      when(fullFIFO) { // When the FIFO is full, empty some of the first elements to avoid stall
        when(frameLength === 0.U) { // Read length of frame
          frameLength := ((memFifo.io.deq.bits + 1.U) >> 1) + 1.U
        }
        stateReg := fill_empty
      }
    }

    is(fill_empty) { // Keep filling and dumping frames, to avoid stall due to lack of space
      when(frameLength =/= 0.U) {
        frameLength := frameLength - 1.U
      }
      // Keep dumping frame
      when(memFifo.io.deq.valid){
          memFifo.io.deq.ready := true.B
      }
      when(frameLength === 0.U && stopFrameRecording) { // If trigger, go to read from FIFO
        stateReg := waitForEOF
        frameLength := 0.U
      }.elsewhen(frameLength === 0.U) { // Otherwise go back and check if FIFO is full
        stateReg := fill
        frameLength := 0.U
      }
    }

    is(waitForEOF) { // Wait for the current frame to be filled
      memFifo.io.deq.ready := false.B // Write to the FIFO if frame is not done

      when(endOfFrame) { // When the end of the frame is met, go to empty
        stateReg := emptyToPatmos
      }.elsewhen(!CircBuffer.io.frameRecieving && !CircBuffer.io.deq.valid){ // When data input stops, allow emptying
        stateReg := emptyToPatmos
      }
    }

    is(emptyToPatmos) { // Read from FIFO to Patmos
      writeToFIFO := false.B
      when(sendToPatmos) { // When OCP.RD
        when(memFifo.io.deq.valid) { // When FIFO is ready to output
          respReg := OcpResp.DVA // Response to OCP to signal data is sent
          memFifo.io.deq.ready := true.B
        }.otherwise {
          stateReg := idle
          stopFrameRecording := false.B
          sendToPatmos := false.B
          writeToFIFO := true.B
        }
      }
    }
  }

  // Statement to act as enable for register, to keep read value high for multiple clocks
  when(respReg === OcpResp.DVA || (io.ocp.M.Cmd === OcpCmd.RD)){
    readHolder := (io.ocp.M.Cmd === OcpCmd.RD)
  }

  // Mux to hold the read pulse, until a valid response is sent back to OCP
  sendToPatmos := Mux(((io.ocp.M.Cmd === OcpCmd.RD) || (respReg === OcpResp.DVA)), io.ocp.M.Cmd === OcpCmd.RD, readHolder)

  // OCP write to define specific variables in the filter, as well as setting the trigger
  when(io.ocp.M.Cmd === OcpCmd.WR) {
    respReg := OcpResp.DVA
    switch(io.ocp.M.Addr(4, 2)) {
      is(0.U) { // Assign trigger to true
        stopFrameRecording := true.B
      }
      is(1.U) { // For writing to the MAC
        tx_axis_tvalid_Reg := true.B
        dataWriter := io.ocp.M.Data
      }
      is(2.U){ // Assign the input value to filter index
        filterIndex := io.ocp.M.Data(7,0)
        filterSet := false.B
      }
      is(3.U){ // Which value to filter through, if not this value the frame is discarded
        filterValue := io.ocp.M.Data(7,0)
        filterSet := true.B // Do not process any values until filter is set.
      }
    }
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := RegNext(memFifo.io.deq.bits)
}
