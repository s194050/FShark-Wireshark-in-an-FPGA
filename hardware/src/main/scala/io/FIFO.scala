package io

import Chisel._
import chisel3.{WireInit, when}
import patmos.Constants._
import ocp._


object FIFO extends DeviceObject {

  def init(params: Map[String, String]) = {}

  def create(params: Map[String, String]): FIFO = Module(new FIFO())
}
// Temp FIFO in chisel3 to be written in VHDL for final version

class FIFO() extends CoreDevice() { // Create DecoupledIO, which is a bundle with ready-valid interface
  // Include FSM
  val FSM = Module(new SimpleFSM)
  // Default response
  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL

  // the register based memory
  val memReg = Reg(Vec(32, UInt(32.W)))
  val incrRead = WireInit(false.B)
  val incrWrite = WireInit(false.B)
  val (readPtr, nextRead) = counter(incrRead)
  val (writePtr, nextWrite) = counter(incrWrite)

  // Initiate boolean values and OCPcore
  val masterReg = Reg(next = io.ocp.M) // To use OCPcore
  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B) // Bool to signal whether FIFO is full


  def counter(incr: Bool): (UInt, UInt) = { // To count elements in register
    val cntReg = RegInit(0.U(log2Ceil(32).W))
    val nextVal = Mux(cntReg === (32-1).U, 0.U, cntReg + 1.U)
    when (incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }

  when (!fullReg) {
    memReg(writePtr) := FSM.io.enq.bits
    emptyReg := false.B
    fullReg := nextWrite === readPtr
    incrWrite := true.B
  }

  when (masterReg.Cmd === OcpCmd.RD) {
    respReg := OcpResp.DVA
    fullReg := false.B
    emptyReg := nextRead === writePtr
    incrRead := true.B
  }
  when(masterReg.Cmd === OcpCmd.WR){
  }

  FSM.io.enq.ready := !fullReg
  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := memReg(readPtr)
}