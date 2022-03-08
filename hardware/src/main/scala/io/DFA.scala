package io
import Chisel._

class DFA() extends Module{
  val io = IO(new Bundle{
    val enq = Flipped(new DecoupledIO(UInt(32.W)))
    val deq = new DecoupledIO(UInt(32.W))
    val read = Output(Bool())
  })

  val matchPat = RegInit(false.B)
  val readFIFO = RegInit(false.B)
  val idle :: start :: s2 :: s3 :: s4 :: end :: Nil = Enum(6)
  val stateRegDFA = RegInit(idle)
  val fifoData = RegInit(0.U(32.W))

  fifoData := io.enq.bits

  switch(stateRegDFA) {
    is(idle) {
      when(fifoData === 65.U) {
        stateRegDFA := start
      }
    }
    is(start) {
      readFIFO := false.B
      io.read := false.B
      when(fifoData === 66.U) {
        stateRegDFA := s2
      }.elsewhen(fifoData === 65.U) {
        stateRegDFA := start
      }.otherwise {
        stateRegDFA := idle
      }
    }
    is(s2) {
      io.read := false.B
      when(fifoData === 67.U) {
        stateRegDFA := s3
      }.elsewhen(fifoData === 65.U) {
        stateRegDFA := start
      }.otherwise {
        stateRegDFA := idle
      }
    }
    is(s3) {
      io.read := false.B
      when(fifoData === 68.U) {
        stateRegDFA := s4
      }.elsewhen(fifoData === 65.U) {
        stateRegDFA := start
      }.otherwise {
        stateRegDFA := idle
      }
    }
    is(s4) {
      io.read := false.B
      when(fifoData === 69.U) {
        stateRegDFA := end
      }.elsewhen(fifoData === 65.U) {
        stateRegDFA := start
      }.otherwise {
        stateRegDFA := idle
      }
    }
    is(end) {
      matchPat := true.B
      when(fifoData === 65.U) {
        stateRegDFA := start
      }.otherwise {
        stateRegDFA := idle
      }
    }
  }
  when(matchPat) {
    readFIFO := true.B
    matchPat := false.B
  }
  when(io.deq.ready && readFIFO) {
    io.deq.bits := io.enq.bits
  }
  io.read := true.B
}
