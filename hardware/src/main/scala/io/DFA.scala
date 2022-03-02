package io
import Chisel._
import patmos.Constants._
class dequeue extends Bundle(){
  val deq = (new DecoupledIO(UInt(32.W)))
}




class DFA() extends Module{
  val io = IO(new dequeue())
  val FSM = Module(new SimpleFSM())
  // Connecting FIFO and FSM
  FSM.io.enq.ready := io.deq.ready
  io.deq.bits := FSM.io.enq.bits


  val matchPat = RegInit(false.B)
  val idle :: start :: s2 :: s3 :: s4 :: end :: Nil = Enum(6)
  val stateRegDFA = RegInit(idle)

  switch(stateRegDFA) {
    is(idle) {
      io.deq.valid := false.B
      when(io.deq.bits === 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
    is(start) {
      when(io.deq.bits === 66.U){
        stateRegDFA := s2
      }.elsewhen(io.deq.bits === 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
    is(s2) {
      when(io.deq.bits === 67.U){
        stateRegDFA := s3
      }.elsewhen(io.deq.bits === 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
    is(s3) {
      when(io.deq.bits === 68.U){
        stateRegDFA := s4
      }.elsewhen(io.deq.bits === 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
    is(s4) {
      when(io.deq.bits === 69.U){
        stateRegDFA := end
      }.elsewhen(io.deq.bits === 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
    is(end) {
      matchPat := true.B
      when(io.deq.bits=== 65.U){
        stateRegDFA := start
      }.otherwise{
        stateRegDFA := idle
      }
    }
  }

  when(matchPat){
    io.deq.valid := true.B
    matchPat := false.B
  }

}
