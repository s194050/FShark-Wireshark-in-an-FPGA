package io

import Chisel._
import chisel3.{WireInit, when}
import ocp._



// Temp FIFO in chisel3 to be written in VHDL for final version

class DFAinputFIFO() extends Module() { // Create DecoupledIO, which is a bundle with ready-valid interface
  val io = IO(new Bundle{
    val enqDFA = Flipped(new DecoupledIO(UInt(32.W)))
    val deqDFA = new DecoupledIO(UInt(32.W))
  })

  def counter(incr: Bool): (UInt, UInt) = { // To count elements in register
    val cntReg = RegInit(0.U(log2Ceil(32).W))
    val nextVal = Mux(cntReg === (31).U, 0.U, cntReg + 1.U)
    when (incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }

  // the register based memory
  val memReg = Reg(Vec(32, UInt(32.W)))
  val incrRead = WireInit(false.B)
  val incrWrite = WireInit(false.B)
  val (readPtr, nextRead) = counter(incrRead)
  val (writePtr, nextWrite) = counter(incrWrite)

  // Initiate boolean values
  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B) // Bool to signal whether FIFO is full

  when (!fullReg) {
    memReg(writePtr) := io.enqDFA.bits
    emptyReg := false.B
    fullReg := nextWrite === readPtr
    incrWrite := true.B
  }

  when (!emptyReg) {
    fullReg := false.B
    emptyReg := nextRead === writePtr
    incrRead := true.B
  }


  io.deqDFA.bits := memReg(readPtr)
  io.enqDFA.ready := !fullReg
  io.deqDFA.valid := !emptyReg
}