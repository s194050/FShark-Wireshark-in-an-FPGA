package io

import Chisel._
import patmos.Constants._

import ocp._

object FIFOtest extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FIFOtest = Module(new FIFOtest())
}
// Temp FIFO in chisel3 to be written in VHDL for final version

class FIFOtest() extends CoreDevice() { // Create DecoupledIO, which is a bundle with ready-valid interface
  def counter(incr: Bool): (UInt, UInt) = {
    val cntReg = RegInit(0.U(log2Ceil(32).W))
    val nextVal = Mux(cntReg === (32-1).U, 0.U, cntReg + 1.U)
    when (incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }


  val masterReg = Reg(next = io.ocp.M) // To use OCPcore
  val dataReg = Reg(32.U)

  // Default response
  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL

  when(masterReg.Cmd === OcpCmd.WR){ // Write to FIFO

    respReg := OcpResp.DVA
    dataReg := masterReg.Data
  }
  when(masterReg.Cmd === OcpCmd.RD){ // Read from FIFO
    respReg := OcpResp.DVA
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := dataReg
}