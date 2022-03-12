/*
OCP interface for Verilog Ethernet MAC 1g - RGMII
 */


package io
import Chisel._

import ocp._

class FPSMpins extends Bundle {
  //Clock and reset
  val clk = Bool(INPUT)
  val clk90 = Bool(INPUT)
  val rst = Bool(INPUT)
  // GPIO for 7-sequence display
  val btn = UInt(INPUT,4)
  val sw = UInt(INPUT,18)
  val ledg = UInt(OUTPUT,9)
  val ledr = UInt(OUTPUT,18)
  val hex0 = UInt(OUTPUT,7)
  val hex1 = UInt(OUTPUT,7)
  val hex2 = UInt(OUTPUT,7)
  val hex3 = UInt(OUTPUT,7)
  val hex4 = UInt(OUTPUT,7)
  val hex5 = UInt(OUTPUT,7)
  val hex6 = UInt(OUTPUT,7)
  val hex7 = UInt(OUTPUT,7)
  val gpio = UInt(OUTPUT,36)


  // Ethernet: 1000BASE-T RGMII
  // --------------------------
  val phy0_rx_clk = Bool(INPUT)
  val phy0_rxd = UInt(INPUT,4)
  val phy0_rx_ctl = Bool(INPUT)
  val phy0_tx_clk = Bool(OUTPUT)
  val phy0_reset_n = Bool(OUTPUT)
  val phy0_int_n = Bool(INPUT)

  val phy1_rx_clk = Bool(INPUT)
  val phy1_rxd = UInt(INPUT,4)
  val phy1_rx_ctl = Bool(INPUT)
  val phy1_tx_clk = Bool(OUTPUT)
  val phy1_reset_n = Bool(OUTPUT)
  val phy1_int_n = Bool(INPUT)
}

class FPSMIO extends FPSMpins {
  // Reciever AXI
  // ------------
  val rx_axis_tdata = UInt(OUTPUT,8)
  val rx_axis_tvalid = Bool(OUTPUT)
  val rx_axis_tready = Bool(INPUT)
  val rx_axis_tlast = Bool(OUTPUT)
  // Transmitter AXI
  //----------------
  val tx_axis_tdata = UInt(INPUT,8)
  val tx_axis_tvalid = Bool(INPUT)
  val tx_axis_tready = Bool(OUTPUT)
  val tx_axis_tlast = Bool(INPUT)
}

class FPSMBB extends BlackBox {
  val io = new FPSMIO()
}
object FPGAsharkMAC extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FPGAsharkMAC= Module(new FPGAsharkMAC())
}


class FPGAsharkMAC extends CoreDevice {
  override val io = new CoreDeviceIO with patmos.HasPins {
    override val pins = new FPSMpins
  }

  val blackbox = Module(new FPSMBB)
  io.pins <> blackbox.io

  // Default response
  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL

  //Initiate states:
  //----------------
  //Reciever
  val rx_axis_tready_Reg = Reg(init = Bool(false))
  rx_axis_tready_Reg := Bool(false)
  blackbox.io.rx_axis_tready := rx_axis_tready_Reg
  //Transmitter
  val tx_axis_tvalid_Reg = Reg(init = Bool(false))
  tx_axis_tvalid_Reg := Bool(false)
  blackbox.io.tx_axis_tvalid := tx_axis_tvalid_Reg

  // Data recieved from Verilog Ethernet MAC
  val dataReader = Reg(init = Bits(0,32))

  // Data to Verilog Ethernet MAC
  val dataWriter= Reg(init = Bits(0,32))
  blackbox.io.tx_axis_tlast := dataWriter(30)
  blackbox.io.tx_axis_tdata := dataWriter(7,0)


  val macIdle :: macWait :: macRead :: Nil = Enum(UInt(), 3)
  val stateMAC = Reg(init = macIdle)

  when(io.ocp.M.Cmd === OcpCmd.WR) {
    respReg := OcpResp.DVA
    tx_axis_tvalid_Reg := Bool(true)
    dataWriter := io.ocp.M.Data
  }

  when(stateMAC === macIdle) {
    when(io.ocp.M.Cmd === OcpCmd.RD) {
      when(io.ocp.M.Addr(0) === Bool(false)) {
        stateMAC := macWait
        rx_axis_tready_Reg := Bool(true)
      }
        .otherwise {
          respReg := OcpResp.DVA
          dataReader := Cat(blackbox.io.tx_axis_tready,Bits(0,31))
        }
    }
  }
  when(stateMAC === macWait) {
    stateMAC := macRead
  }
  when(stateMAC === macRead) {
    stateMAC := macIdle
    respReg := OcpResp.DVA
    dataReader := Cat(blackbox.io.rx_axis_tvalid,Cat(blackbox.io.rx_axis_tlast,Cat(Bits(0,22),blackbox.io.rx_axis_tdata)))
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := dataReader
}
