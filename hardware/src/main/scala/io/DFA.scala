package io
import Chisel._

class DFA() extends Module{
  val io = IO(new Bundle{
    val enq = Flipped(new DecoupledIO(UInt(32.W)))
    val deq = new DecoupledIO(UInt(32.W))
  })

  val matchPat = RegInit(false.B)
  val idle :: start :: s2 :: s3 :: s4 :: end :: Nil = Enum(6)
  val stateRegDFA = RegInit(idle)

  when(io.deq.ready) {
    switch(stateRegDFA) {
      is(idle) {
        when(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
      is(start) {
        when(io.enq.bits === 66.U) {
          stateRegDFA := s2
        }.elsewhen(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
      is(s2) {
        when(io.enq.bits === 67.U) {
          stateRegDFA := s3
        }.elsewhen(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
      is(s3) {
        when(io.enq.bits === 68.U) {
          stateRegDFA := s4
        }.elsewhen(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
      is(s4) {
        when(io.enq.bits === 69.U) {
          stateRegDFA := end
        }.elsewhen(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
      is(end) {
        matchPat := true.B
        when(io.enq.bits === 65.U) {
          stateRegDFA := start
        }.otherwise {
          stateRegDFA := idle
        }
      }
    }
    when(matchPat) {
      io.deq.bits := io.enq.bits
      matchPat := false.B
    }
  }
}
