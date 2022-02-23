package io

import Chisel._
import chisel3.util._
import patmos.Constants._

import ocp._

object FIFO extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FIFO = Module(new FIFO())
}
// Temp FIFO in chisel3 to be written in VHDL for final version

class FIFO() extends CoreDevice() { // Create DecoupledIO, which is a bundle with ready-valid interface
override val io = IO(new CoreDeviceIO(){
  val enq = Flipped(new DecoupledIO(32.U)) //Writer to FIFO
  val deq = new DecoupledIO(32.U) // Reader to FIFO
})

  val masterReg = Reg(next = io.ocp.M) // To use OCPcore
  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B) // Bool to signal whether FIFO is full
  val dataReg = Reg(32.U)

  // Default response
  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL

  when(masterReg.Cmd === OcpCmd.WR){ // Write to FIFO
    when(io.enq.valid && !fullReg){
      respReg := OcpResp.DVA
      io.enq.bits := masterReg.Data
      emptyReg := false.B
      fullReg := true.B
      dataReg := io.enq.bits
    }
  }

  when(masterReg.Cmd === OcpCmd.RD){ // Read from FIFO
    when(io.deq.ready && !emptyReg){
      respReg := OcpResp.DVA
      fullReg := false.B
      emptyReg := true.B
    }
  }

  io.enq.ready := !fullReg
  io.deq.valid := !emptyReg
  io.deq.bits := dataReg

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := io.deq.bits
}