package io
import chisel3._
import chisel3.util._

class IWToplevel extends  Module {
  val io = IO(new Bundle{
    val deq = new DecoupledIO(UInt(32.W))
  })
  //Connect via toplevel file
  val DFA = Module(new DFA)
  val DFAFIFO = Module(new DFAinputFIFO)
  val FSM = Module(new SimpleFSM)

  // Connect DFA and DFA FIFO
  DFAFIFO.io.deqDFA.ready := DFA.io.enq.ready
  DFA.io.enq.valid := DFAFIFO.io.deqDFA.valid
  DFA.io.enq.bits := DFAFIFO.io.deqDFA.bits

  // Connect DFA FIFO and Simple FSM
  FSM.io.FSMdeq.ready := DFAFIFO.io.enqDFA.ready
  DFAFIFO.io.enqDFA.valid := FSM.io.FSMdeq.valid
  DFAFIFO.io.enqDFA.bits := FSM.io.FSMdeq.bits

  // Connect FIFO and DFA
  DFA.io.deq.ready := io.deq.ready
  io.deq.valid := DFA.io.deq.valid
  io.deq.bits := DFA.io.deq.bits
}
