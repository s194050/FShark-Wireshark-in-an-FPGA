/*
OCP interface for Verilog Ethernet MAC 1g - RGMII
 */


package io
import chisel3._
import chisel3.util._
import chisel3.internal.HasId
import patmos.Constants.CLOCK_FREQ
import ocp._

object FPGAsharkMAC extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FPGAsharkMAC= Module(new FPGAsharkMAC())

  trait Pins extends patmos.HasPins {
    override val pins = new Bundle() {
      // GPIO for 7-sequence display
      val btn = Input(UInt(4.W))
      val sw = Input(UInt(18.W))
      val ledg = Input(UInt(9.W))
      val ledr = Input(UInt(18.W))
      val hex0 = Input(UInt(7.W))
      val hex1 = Input(UInt(7.W))
      val hex2 = Input(UInt(7.W))
      val hex3 = Input(UInt(7.W))
      val hex4 = Input(UInt(7.W))
      val hex5 = Input(UInt(7.W))
      val hex6 = Input(UInt(7.W))
      val hex7 = Input(UInt(7.W))
      val gpio = Output(UInt(36.W))


      // Ethernet: 1000BASE-T RGMII
      // --------------------------
      val phy0_rx_clk = Input(Bool())
      val phy0_rxd = Input(UInt(4.W))
      val phy0_rx_ctl = Input(Bool())
      val phy0_tx_clk = Output(Bool())
      val phy0_reset_n = Output(Bool())
      val phy0_int_n = Input(Bool())

      val phy1_rx_clk = Input(Bool())
      val phy1_rxd = Input(UInt(4.W))
      val phy1_rx_ctl = Input(Bool())
      val phy1_tx_clk = Output(Bool())
      val phy1_reset_n = Output(Bool())
      val phy1_int_n = Input(Bool())

      // Reciever AXI
      // ------------
      val rx_axis_tdata = Output(UInt(8.W))
      val rx_axis_tvalid = Output(Bool())
      val rx_axis_tready = Input(Bool())
      val rx_axis_tlast = Output(Bool())
      // Transmitter AXI
      //----------------
      val tx_axis_tdata = Input(UInt(8.W))
      val tx_axis_tvalid = Input(Bool())
      val tx_axis_tready = Output(Bool())
      val tx_axis_tlast = Input(Bool())
    }
  }
}


class VeriMacBB extends BlackBox {
  val io = IO(new OcpCoreSlavePort(32,32) {
  val clk = Input(Clock())
  val clk90 = Input(Clock())
  val rst = Input(Bool())
  // GPIO for 7-sequence display
  val btn = Input(UInt(4.W))
  val sw = Input(UInt(18.W))
  val ledg = Input(UInt(9.W))
  val ledr = Input(UInt(18.W))
  val hex0 = Input(UInt(7.W))
  val hex1 = Input(UInt(7.W))
  val hex2 = Input(UInt(7.W))
  val hex3 = Input(UInt(7.W))
  val hex4 = Input(UInt(7.W))
  val hex5 = Input(UInt(7.W))
  val hex6 = Input(UInt(7.W))
  val hex7 = Input(UInt(7.W))
  val gpio = Output(UInt(36.W))


  // Ethernet: 1000BASE-T RGMII
  // --------------------------
  val phy0_rx_clk = Input(Bool())
  val phy0_rxd = Input(UInt(4.W))
  val phy0_rx_ctl = Input(Bool())
  val phy0_tx_clk = Output(Bool())
  val phy0_reset_n = Output(Bool())
  val phy0_int_n = Input(Bool())

  val phy1_rx_clk = Input(Bool())
  val phy1_rxd = Input(UInt(4.W))
  val phy1_rx_ctl = Input(Bool())
  val phy1_tx_clk = Output(Bool())
  val phy1_reset_n = Output(Bool())
  val phy1_int_n = Input(Bool())

  // Reciever AXI
  // ------------
  val rx_axis_tdata = Output(UInt(8.W))
  val rx_axis_tvalid = Output(Bool())
  val rx_axis_tready = Input(Bool())
  val rx_axis_tlast = Output(Bool())
  // Transmitter AXI
  //----------------
  val tx_axis_tdata = Input(UInt(8.W))
  val tx_axis_tvalid = Input(Bool())
  val tx_axis_tready = Output(Bool())
  val tx_axis_tlast = Input(Bool())
  })

  io.clk.suggestName("clk")
  io.clk90.suggestName("clk90")
  io.rst.suggestName("rst")

  io.M.Cmd.suggestName("MCmd")
  io.M.Addr.suggestName("MAddr")
  io.M.Data.suggestName("MData")
  io.M.ByteEn.suggestName("MByteEn")
  io.S.Resp.suggestName("SResp")
  io.S.Data.suggestName("SData")

  io.tx_axis_tdata.suggestName("tx_axis_tdata")
  io.tx_axis_tvalid.suggestName("tx_axis_tvalid")
  io.tx_axis_tready.suggestName("tx_axis_tready")
  io.tx_axis_tlast.suggestName("rx_axis_tlast.")
  io.rx_axis_tdata.suggestName("rx_axis_tdata")
  io.rx_axis_tvalid.suggestName("rx_axis_tvalid")
  io.rx_axis_tready.suggestName("rx_axis_tready")
  io.rx_axis_tlast.suggestName("rx_axis_tlast.")
  io.phy0_rx_clk.suggestName("phy0_rx_clk")
  io.phy0_rxd.suggestName("phy0_rxd")
  io.phy0_rx_ctl.suggestName("phy0_rx_ctl")
  io.phy0_tx_clk.suggestName("phy0_tx_clk")
  io.phy0_reset_n.suggestName("phy0_reset_n")
  io.phy0_int_n.suggestName("phy0_int_n")
  io.phy1_rx_clk.suggestName("phy1_rx_clk")
  io.phy1_rxd.suggestName("phy1_rxd")
  io.phy1_rx_ctl.suggestName("phy1_rx_ctl")
  io.phy1_tx_clk.suggestName("phy1_tx_clk")
  io.phy1_reset_n.suggestName("phy1_reset_n")
  io.phy1_int_n.suggestName("phy1_int_n")
  io.btn.suggestName("btn")
  io.sw.suggestName("sw")
  io.ledg.suggestName("ledg")
  io.ledr.suggestName("ledr")
  io.hex0.suggestName("hex0")
  io.hex1.suggestName("hex1")
  io.hex2.suggestName("hex2")
  io.hex3.suggestName("hex3")
  io.hex4.suggestName("hex4")
  io.hex5.suggestName("hex5")
  io.hex6.suggestName("hex6")
  io.hex7.suggestName("hex7")
  io.gpio.suggestName("gpio")

  override def desiredName: String = "fpga_core"
}



class FPGAsharkMAC extends CoreDevice() {
  override val io = IO(new CoreDeviceIO() with FPGAsharkMAC.Pins {})

  val verimac = Module(new VeriMacBB())
  // Wire pins straight through
  verimac.io.clk  := clock
  verimac.io.clk90  := clock
  verimac.io.rst := reset
  verimac.io.tx_axis_tdata := io.pins.tx_axis_tdata
  verimac.io.tx_axis_tvalid := io.pins.tx_axis_tvalid
  io.pins.tx_axis_tready := verimac.io.tx_axis_tready
  verimac.io.tx_axis_tlast := io.pins.tx_axis_tlast
  io.pins.rx_axis_tdata := verimac.io.rx_axis_tdata
  io.pins.rx_axis_tvalid := verimac.io.rx_axis_tvalid
  verimac.io.rx_axis_tready := io.pins.rx_axis_tready
  verimac.io.phy0_rx_clk := io.pins.phy0_rx_clk
  verimac.io.phy0_rxd := io.pins.phy0_rxd
  verimac.io.phy0_rx_ctl := io.pins.phy0_rx_ctl
  io.pins.phy0_tx_clk := verimac.io.phy0_tx_clk
  io.pins.phy0_reset_n := verimac.io.phy0_reset_n
  verimac.io.phy0_int_n := io.pins.phy0_int_n
  verimac.io.phy1_rx_clk := io.pins.phy1_rx_clk
  verimac.io.phy1_rxd := io.pins.phy1_rxd
  verimac.io.phy1_rx_ctl := io.pins.phy1_rx_ctl
  io.pins.phy1_tx_clk := verimac.io.phy1_tx_clk
  io.pins.phy1_reset_n := verimac.io.phy1_reset_n
  verimac.io.phy1_int_n := io.pins.phy1_int_n

  verimac.io.btn := io.pins.btn
  verimac.io.sw := io.pins.sw
  verimac.io.ledg := io.pins.ledg
  verimac.io.ledr := io.pins.ledr
  verimac.io.hex0 := io.pins.hex0
  verimac.io.hex1 := io.pins.hex1
  verimac.io.hex2 := io.pins.hex2
  verimac.io.hex3 := io.pins.hex3
  verimac.io.hex4 := io.pins.hex4
  verimac.io.hex5 := io.pins.hex5
  verimac.io.hex6 := io.pins.hex6
  verimac.io.hex7 := io.pins.hex7
  io.pins.gpio := verimac.io.gpio

  val btn = Input(UInt(4.W))
  val sw = Input(UInt(18.W))
  val ledg = Input(UInt(9.W))
  val ledr = Input(UInt(18.W))
  val hex0 = Input(UInt(7.W))
  val hex1 = Input(UInt(7.W))
  val hex2 = Input(UInt(7.W))
  val hex3 = Input(UInt(7.W))
  val hex4 = Input(UInt(7.W))
  val hex5 = Input(UInt(7.W))
  val hex6 = Input(UInt(7.W))
  val hex7 = Input(UInt(7.W))
  val gpio = Output(UInt(36.W))

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  //Initiate states:
  //----------------
  //Reciever
  val rx_axis_tready_Reg = RegInit(false.B)
  rx_axis_tready_Reg := false.B
  verimac.io.rx_axis_tready := rx_axis_tready_Reg
  //Transmitter
  val tx_axis_tvalid_Reg = RegInit(false.B)
  tx_axis_tvalid_Reg := false.B
  verimac.io.tx_axis_tvalid := tx_axis_tvalid_Reg

  // Data recieved from Verilog Ethernet MAC
  val dataReader = RegInit(0.U(32.W))

  // Data to Verilog Ethernet MAC
  val dataWriter= RegInit(0.U(32.W))
  verimac.io.tx_axis_tlast := dataWriter(30)
  verimac.io.tx_axis_tdata := dataWriter(7,0)


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
          dataReader := Cat(verimac.io.tx_axis_tready,0.U(31.W))
        }
    }
  }
  when(stateMAC === macWait) {
    stateMAC := macRead
  }
  when(stateMAC === macRead) {
    stateMAC := macIdle
    respReg := OcpResp.DVA
    dataReader := Cat(verimac.io.rx_axis_tvalid,Cat(verimac.io.rx_axis_tlast,0.U(31.W)),verimac.io.rx_axis_tdata)
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := dataReader
}
