--
-- Copyright: 2013, Technical University of Denmark, DTU Compute
-- Author: Martin Schoeberl (martin@jopdesign.com)
--         Rasmus Bo Soerensen (rasmus@rbscloud.dk)
-- License: Simplified BSD License
--

-- VHDL top level for Patmos in Chisel on Altera de2-115 board
--
-- Includes some 'magic' VHDL code to generate a reset after FPGA configuration.
--

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity patmos_top is
  port(
    clk : in  std_logic;
    oLedsPins_led : out std_logic_vector(8 downto 0);
    iKeysPins_key : in std_logic_vector(3 downto 0);
    oUartPins_txd : out std_logic;
    iUartPins_rxd : in  std_logic;
	 oUart2Pins_txd : out   std_logic;
    iUart2Pins_rxd : in    std_logic;
    oUart3Pins_txd : out   std_logic;
    iUart3Pins_rxd : in    std_logic;
	 oGpioPins_gpio_0 : inout std_logic_vector(7 downto 0);
    oSRAM_A : out std_logic_vector(19 downto 0);
    SRAM_DQ : inout std_logic_vector(15 downto 0);
    oSRAM_CE_N : out std_logic;
    oSRAM_OE_N : out std_logic;
    oSRAM_WE_N : out std_logic;
    oSRAM_LB_N : out std_logic;
    oSRAM_UB_N : out std_logic;

    -- Phy0
    ENET0_RX_CLK : in std_logic;
    ENET0_RX_DATA : in std_logic_vector(3 downto 0);
    ENET0_RX_DV : in std_logic;
    ENET0_GTX_CLK : out std_logic;
    ENET0_TX_DATA : out std_logic_vector(3 downto 0);
    ENET0_TX_EN : out std_logic;
    ENET0_RST_N : in std_logic;
    ENET0_INT_N : in std_logic;
    --PHY1
    ENET1_RX_CLK : in std_logic;
    ENET1_RX_DATA : in std_logic_vector(3 downto 0);
    ENET1_RX_DV : in std_logic;
    ENET1_GTX_CLK : out std_logic;
    ENET1_TX_DATA : out std_logic_vector(3 downto 0);
    ENET1_TX_EN : out std_logic;
    ENET1_RST_N : in std_logic;
    ENET1_INT_N : in std_logic
  );
end entity patmos_top;

architecture rtl of patmos_top is
	component Patmos is
		port(
			clock           : in  std_logic;
			reset           : in  std_logic;

      io_Leds_led : out std_logic_vector(8 downto 0);
      io_Keys_key : in  std_logic_vector(3 downto 0);
		  io_Gpio_gpios_0 : inout std_logic_vector(5 downto 0);
      io_UartCmp_tx                    : out std_logic;
		  io_UartCmp_rx                    : in  std_logic;
		  io_Uart_tx                       : out std_logic;
  		io_Uart_rx                       : in  std_logic;
  		io_Uart_1_tx                     : out std_logic;
  		io_Uart_1_rx                     : in  std_logic;
	   --Clock and logic
      io_FPGAsharkMAC_gtx_clk : in std_logic;
      io_FPGAsharkMAC_gtx_clk90 : in std_logic;
      io_FPGAsharkMAC_gtx_rst : in std_logic;
      --io_FPGAsharkMAC_logic_clk : in std_logic;
      --io_FPGAsharkMAC_logic_rst : in std_logic;
      -- Axi input
      --io_FPGAsharkMAC_tx_axis_tdata : in std_logic_vector(7 downto 0);
      --io_FPGAsharkMAC_tx_axis_tkeep : in std_logic_vector(7 downto 0);
      --io_FPGAsharkMAC_tx_axis_tvalid : in std_logic;
      --io_FPGAsharkMAC_tx_axis_tready : out std_logic;
      --io_FPGAsharkMAC_tx_axis_tlast : in std_logic;
      --io_FPGAsharkMAC_tx_axis_tuser : in std_logic;
      -- AXI output
      --io_FPGAsharkMAC_rx_axis_tdata : out std_logic_vector(7 downto 0);
      --io_FPGAsharkMAC_rx_axis_tkeep : out std_logic_vector(7 downto 0);
      --io_FPGAsharkMAC_rx_axis_tvalid : out std_logic;
      --io_FPGAsharkMAC_rx_axis_tready : in std_logic;
      --io_FPGAsharkMAC_rx_axis_tlast : out std_logic;
      --io_FPGAsharkMAC_rx_axis_tuser : out std_logic;
      -- RGMII interface
      io_FPGAsharkMAC_rgmii_rx_clk : in std_logic;
      io_FPGAsharkMAC_rgmii_rxd : in std_logic_vector(3 downto 0);
      io_FPGAsharkMAC_rgmii_rx_ctl : in std_logic;
      io_FPGAsharkMAC_rgmii_tx_clk : out std_logic;
      io_FPGAsharkMAC_rgmii_txd : out std_logic_vector(3 downto 0);
      io_FPGAsharkMAC_rgmii_tx_ctl : out std_logic;
      -- MAC status
      --io_FPGAsharkMAC_tx_error_underflow : out std_logic;
      --io_FPGAsharkMAC_tx_fifo_overflow : out std_logic;
      --io_FPGAsharkMAC_tx_fifo_bad_frame : out std_logic;
      --io_FPGAsharkMAC_tx_fifo_good_frame : out std_logic;
      --io_FPGAsharkMAC_rx_error_bad_frame : out std_logic;
      --io_FPGAsharkMAC_rx_error_bad_fcs : out std_logic;
      --io_FPGAsharkMAC_rx_fifo_overflow : out std_logic;
      --io_FPGAsharkMAC_rx_fifo_bad_frame : out std_logic;
      --io_FPGAsharkMAC_rx_fifo_good_frame : out std_logic;
      --io_FPGAsharkMAC_speed : out std_logic_vector(1 downto 0);
      -- configuration
      --io_FPGAsharkMAC_ifg_delay : in std_logic_vector(7 downto 0);
      io_SramCtrl_ramOut_addr : out std_logic_vector(19 downto 0);
      io_SramCtrl_ramOut_doutEna : out std_logic;
      io_SramCtrl_ramIn_din : in std_logic_vector(15 downto 0);
      io_SramCtrl_ramOut_dout : out std_logic_vector(15 downto 0);
      io_SramCtrl_ramOut_nce : out std_logic;
      io_SramCtrl_ramOut_noe : out std_logic;
      io_SramCtrl_ramOut_nwe : out std_logic;
      io_SramCtrl_ramOut_nlb : out std_logic;
      io_SramCtrl_ramOut_nub : out std_logic

    );
  end component;

  -- DE2-70: 50 MHz clock => 80 MHz
  -- BeMicro: 16 MHz clock => 25.6 MHz
  constant pll_infreq : real    := 50.0;
  constant pll_mult   : natural := 8; --8
  constant pll_div    : natural := 5; --5

  constant clk1_mult   : natural := 5;
  constant clk1_div    : natural := 2;
  constant clk1_phase : string := "2000";

  signal clk_int : std_logic;
  signal clk_125 : std_logic;
  signal clk_125_90 : std_logic;

  -- for generation of internal reset
  signal int_res            : std_logic;
  signal res_reg1, res_reg2 : std_logic;
  signal res_cnt            : unsigned(2 downto 0) := "000"; -- for the simulation

    -- sram signals for tristate inout
    signal sram_out_dout_ena : std_logic;
    signal sram_out_dout : std_logic_vector(15 downto 0);

  attribute altera_attribute : string;
  attribute altera_attribute of res_cnt : signal is "POWER_UP_LEVEL=LOW";

begin
  pll_inst : entity work.pll generic map(
      input_freq  => pll_infreq,
      multiply_by => pll_mult,
      divide_by   => pll_div,

      clk1_multiply_by => clk1_mult,
      clk1_divide_by => clk1_div,
      clk1_phase_shift => clk1_phase
    )
    port map(
      inclk0 => clk,
      c0     => clk_int,
      --c1     => clk_125,
      c2     => clk_125_90
    );
  -- we use a PLL
  -- clk_int <= clk;

  --
  --  internal reset generation
  --  should include the PLL lock signal
  --
  process(clk_int)
  begin
    if rising_edge(clk_int) then
      if (res_cnt /= "111") then
        res_cnt <= res_cnt + 1;
      end if;
      res_reg1 <= not res_cnt(0) or not res_cnt(1) or not res_cnt(2);
      res_reg2 <= res_reg1;
      int_res  <= res_reg2;
    end if;
  end process;


    -- tristate output to ssram
    process(sram_out_dout_ena, sram_out_dout)
    begin
      if sram_out_dout_ena='1' then
        SRAM_DQ <= sram_out_dout;
      else
        SRAM_DQ <= (others => 'Z');
      end if;
    end process;

    -- Initiate Patmos
    patmos_inst : Patmos port map(
    clock => clk_int,
    reset => int_res,
    io_FPGAsharkMAC_gtx_clk => clk_int, -- clk_125
    io_FPGAsharkMAC_gtx_clk90 => clk_125_90,
    io_FPGAsharkMAC_gtx_rst =>  int_res,

    io_Leds_led => oLedsPins_led,
    io_Keys_key => iKeysPins_key,
    io_Gpio_gpios_0(4 downto 0) => oGpioPins_gpio_0(4 downto 0),
    io_UartCmp_tx => oUartPins_txd,
    io_UartCmp_rx => iUartPins_rxd,
    io_Uart_tx => oUart2Pins_txd,
    io_Uart_rx => iUart2Pins_rxd,
    io_Uart_1_tx => oUart3Pins_txd,
    io_Uart_1_rx => iUart3Pins_rxd,


    io_FPGAsharkMAC_rgmii_rx_clk => ENET0_RX_CLK,
    io_FPGAsharkMAC_rgmii_rxd => ENET0_RX_DATA,
    io_FPGAsharkMAC_rgmii_rx_ctl =>  ENET0_RX_DV,
    io_FPGAsharkMAC_rgmii_tx_clk => ENET0_GTX_CLK,
    io_FPGAsharkMAC_rgmii_txd => ENET0_TX_DATA,
    io_FPGAsharkMAC_rgmii_tx_ctl => ENET0_TX_EN,

    io_SRamCtrl_ramOut_addr => oSRAM_A,
    io_SRamCtrl_ramOut_doutEna => sram_out_dout_ena,
    io_SRamCtrl_ramIn_din => SRAM_DQ,
    io_SRamCtrl_ramOut_dout => sram_out_dout,
    io_SRamCtrl_ramOut_nce => oSRAM_CE_N,
    io_SRamCtrl_ramOut_noe => oSRAM_OE_N,
    io_SRamCtrl_ramOut_nwe => oSRAM_WE_N,
    io_SRamCtrl_ramOut_nlb => oSRAM_LB_N,
    io_SRamCtrl_ramOut_nub => oSRAM_UB_N

    );

end architecture rtl;
