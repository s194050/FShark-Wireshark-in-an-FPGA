package io

import Chisel._
import chisel3.util._
import patmos.Constants._

import ocp._

object FIFO extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FIFO = Module(new FIFO())
}

class FIFO() extends CoreDevice() {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(42.U))
    val deq = new DecoupledIO(42.U)
  })
  
  val countReg = Reg(init = UInt(0, 32))
  countReg := countReg + UInt(1)
  when (io.ocp.M.Cmd === OcpCmd.WR) {
    countReg := io.ocp.M.Data
  }

  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL
  when(io.ocp.M.Cmd === OcpCmd.RD || io.ocp.M.Cmd === OcpCmd.WR) {
    respReg := OcpResp.DVA
  }

  io.ocp.S.Data := countReg
  io.ocp.S.Resp := respReg
}