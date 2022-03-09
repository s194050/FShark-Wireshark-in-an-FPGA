package io
import chisel3._
import chisel3.util._

class IWToplevel extends  Module {
  val io = IO(new Bundle{
    val deq = new DecoupledIO(UInt(32.W))
  })
  //Connect via toplevel file
  //val DFA = Module(new DFA)
  //val DFAFIFO = Module(new DFAinputFIFO)
  val FSM = Module(new SimpleFSM)

  // Connect DFA and DFA FIFO
  //DFAFIFO.io.deqDFA.ready := DFA.io.enq.ready
  //DFA.io.enq.valid := DFAFIFO.io.deqDFA.valid
  //DFA.io.enq.bits := DFAFIFO.io.deqDFA.bits
  //DFAFIFO.io.read := DFA.io.read

  // Connect DFA FIFO and Simple FSM
  //FSM.io.FSMdeq.ready := DFAFIFO.io.enqDFA.ready
  //DFAFIFO.io.enqDFA.valid := FSM.io.FSMdeq.valid
  //DFAFIFO.io.enqDFA.bits := FSM.io.FSMdeq.bits

  // Connect DFA to Simple FSM output
  //FSM.io.FSMdeq.ready := DFA.io.enqFSM.ready
  //DFA.io.enqFSM.valid := FSM.io.FSMdeq.valid
  //DFA.io.enqFSM.bits := FSM.io.FSMdeq.bits

  // Connect FIFO and FSM
  FSM.io.FSMdeq.ready := io.deq.ready
  io.deq.valid := FSM.io.FSMdeq.valid
  io.deq.bits := FSM.io.FSMdeq.bits
}
