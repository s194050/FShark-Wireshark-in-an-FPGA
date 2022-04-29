package io
import chisel3._
import chisel3.util._
import ocp._

object FShark extends DeviceObject {
  var target = "ALTERA"
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
class FShark(target: String = "ALTERA",datawidth: Int = 16) extends CoreDevice {
  override val io = IO(new CoreDeviceIO() with FShark.Pins {})
  // Verilog Ethernet MAC blackbox
  val ethmac1g = Module(new eth_mac_1gBB("ALTERA",16))
  //Filter for FMAC, input to the Circular buffer
  val FMAC_filter = Module(new FMAC_filter(datawidth))
  // Connecting MAC and filter
  //--------------------------
  ethmac1g.io.rx_axis_tready := FMAC_filter.io.axis_tready
  FMAC_filter.io.axis_tvalid := ethmac1g.io.rx_axis_tvalid
  FMAC_filter.io.axis_tkeep := ethmac1g.io.rx_axis_tkeep
  FMAC_filter.io.axis_tlast := ethmac1g.io.rx_axis_tlast
  FMAC_filter.io.axis_tdata := ethmac1g.io.rx_axis_tdata
  // Circular buffer for frame holding
  val CircBuffer = Module(new CircularBuffer(500,datawidth,io.ocp.addrWidth,io.ocp.dataWidth))
  // Connecting buffer and filter
  //-----------------------------
  CircBuffer.io.filter_bus.bits.flushFrame := FMAC_filter.io.filter_bus.bits.flushFrame
  CircBuffer.io.filter_bus.bits.addHeader := FMAC_filter.io.filter_bus.bits.addHeader
  CircBuffer.io.filter_bus.bits.goodFrame := FMAC_filter.io.filter_bus.bits.goodFrame
  CircBuffer.io.filter_bus.bits.badFrame := FMAC_filter.io.filter_bus.bits.badFrame
  CircBuffer.io.filter_bus.bits.tdata := FMAC_filter.io.filter_bus.bits.tdata
  CircBuffer.io.filter_bus.valid := FMAC_filter.io.filter_bus.valid
  FMAC_filter.io.filter_bus.ready := CircBuffer.io.filter_bus.ready
  CircBuffer.io.ocp.M := io.ocp.M
  io.ocp.S := CircBuffer.io.ocp.S
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

  when(io.ocp.M.Cmd === OcpCmd.WR) {
    respReg := OcpResp.DVA
    tx_axis_tvalid_Reg := true.B
    dataWriter := io.ocp.M.Data
  }

}
