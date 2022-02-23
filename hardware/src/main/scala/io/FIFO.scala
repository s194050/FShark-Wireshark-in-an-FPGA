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
override val io = IO(new CoreDeviceIO(){
  val enq = Flipped(new DecoupledIO(0.U)) //Writer to FIFO
  val deq = new DecoupledIO(0.U) // Reader to FIFO
})
  def counter(incr: Bool): (UInt, UInt) = {
    val cntReg = RegInit(0.U(log2Ceil(32).W))
    val nextVal = Mux(cntReg === (32-1).U, 0.U, cntReg + 1.U)
    when (incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }

  // the register based memory
  val memReg = Reg(Vec(32, 0.U))

  val incrRead = WireInit(false.B)
  val incrWrite = WireInit(false.B)
  val (readPtr, nextRead) = counter(incrRead)
  val (writePtr, nextWrite) = counter(incrWrite)

  // Initiate boolean values and OCPcore
  val masterReg = Reg(next = io.ocp.M) // To use OCPcore
  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B) // Bool to signal whether FIFO is full
  val dataReg = Reg(32.U)

  // Default response
  val respReg = Reg(init = OcpResp.NULL)
  respReg := OcpResp.NULL

  when (io.enq.valid && !fullReg) {
    memReg(writePtr) := io.enq.bits
    emptyReg := false.B
    fullReg := nextWrite === readPtr
    incrWrite := true.B
  }
  when (io.deq.ready && !emptyReg) {
    fullReg := false.B
    emptyReg := nextRead === writePtr
    incrRead := true.B
  }
  //Writer
  when(masterReg.Cmd === OcpCmd.WR){ // Write to FIFO
    respReg := OcpResp.DVA
    masterReg.Data := io.enq.bits
  }

//Reader
  when(masterReg.Cmd === OcpCmd.RD){ // Read from FIFO
    respReg := OcpResp.DVA
    io.deq.bits := memReg(readPtr)
  }
  io.enq.ready := !fullReg
  io.deq.valid := !emptyReg
  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := io.deq.bits
}