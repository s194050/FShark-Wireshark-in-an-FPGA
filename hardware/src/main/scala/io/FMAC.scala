/*
OCP interface for Verilog Ethernet MAC 1g w/ FIFO - RGMII
 */

package io
import chisel3._
import chisel3.util._
import chisel3.internal.HasId
import patmos.Constants.CLOCK_FREQ
import ocp._

object FMAC extends DeviceObject {
  var target = "SIM"

  def init(params: Map[String, String]) = {
      target = getParam(params,"target")
  }

  def create(params: Map[String, String]): FMAC = Module(new FMAC(target))

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



class eth_mac_1gBB(target: String) extends BlackBox(Map("TARGET" -> target)) {
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
    val tx_axis_tdata = Input(UInt(8.W))
    val tx_axis_tkeep = Input(Bool())
    val tx_axis_tvalid = Input(Bool())
    val tx_axis_tready = Output(Bool())
    val tx_axis_tlast = Input(Bool())
    val tx_axis_tuser = Input(Bool())
    //AXI Output
    //----------
    val rx_axis_tdata = Output(UInt(8.W))
    val rx_axis_tkeep = Output(Bool())
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


class FMAC(target: String = "SIM") extends CoreDevice() {
  override val io = IO(new CoreDeviceIO() with FMAC.Pins {})
  val ethmac1g = Module(new eth_mac_1gBB("SIM"))
  // Connect the pins straight through
  // Clock and logic
  ethmac1g.io.gtx_clk := io.pins.gtx_clk
  ethmac1g.io.gtx_rst := io.pins.gtx_rst
  ethmac1g.io.gtx_clk90 := io.pins.gtx_clk90
  ethmac1g.io.logic_clk := clock
  ethmac1g.io.logic_rst := reset
  // Configuration
  ethmac1g.io.ifg_delay := WireInit(12.U(8.W))
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
  //Reciever

  val rx_axis_tready_Reg = RegInit(false.B)
  rx_axis_tready_Reg := false.B
  ethmac1g.io.rx_axis_tready := rx_axis_tready_Reg
  //Transmitter
  val tx_axis_tvalid_Reg = RegInit(false.B)
  tx_axis_tvalid_Reg := false.B
  ethmac1g.io.tx_axis_tvalid := tx_axis_tvalid_Reg
  // Data recieved from Verilog Ethernet MAC
  val dataReader = RegInit(0.U(32.W))

  // Data to Verilog Ethernet MAC
  val dataWriter= RegInit(0.U(32.W))
  ethmac1g.io.tx_axis_tlast := dataWriter(30)
  ethmac1g.io.tx_axis_tdata := dataWriter(7,0)
  /*
  when(io.ocp.M.Cmd === OcpCmd.RD){
    //rx_axis_tready_Reg := true.B
    respReg := OcpResp.DVA
    dataReader := Cat(ethmac1g.io.rx_axis_tvalid,Cat(ethmac1g.io.rx_axis_tlast,0.U(31.W)),ethmac1g.io.rx_axis_tdata)
  }
  */
    val macIdle :: macWait :: macRead :: Nil = Enum(3)
  val stateMAC = RegInit(macIdle)

  when(io.ocp.M.Cmd === OcpCmd.WR) {
    respReg := OcpResp.DVA
    tx_axis_tvalid_Reg := true.B
    dataWriter := io.ocp.M.Data
  }

  when(stateMAC === macIdle) {
    when(io.ocp.M.Cmd === OcpCmd.RD) {
      when(io.ocp.M.Addr(0) === false.B) {
        stateMAC := macWait
        rx_axis_tready_Reg := true.B
      }
        .otherwise {
          respReg := OcpResp.DVA
          dataReader := Cat(ethmac1g.io.tx_axis_tready,0.U(31.W))
        }
    }
  }
  when(stateMAC === macWait) {
    stateMAC := macRead
  }
  when(stateMAC === macRead) {
    stateMAC := macIdle
    respReg := OcpResp.DVA
    dataReader := Cat(ethmac1g.io.rx_axis_tvalid,Cat(ethmac1g.io.rx_axis_tlast,0.U(31.W)),ethmac1g.io.rx_axis_tdata)
  }
  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := dataReader

}
