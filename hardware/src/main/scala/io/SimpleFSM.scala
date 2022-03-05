package io

import Chisel._
import patmos.Constants._


class SimpleFSM() extends Module {
  val io = IO(new Bundle{
    val FSMdeq = new DecoupledIO(UInt(32.W))
  })
  val idle :: transfer :: end :: Nil = Enum(3)
  val stateReg = RegInit(idle)

  val count = RegInit(65.U(8.W))
  when(io.FSMdeq.ready ) {
  switch(stateReg) {
    is(idle) {
      count := count + 1.U
      stateReg := transfer
    }
    is(transfer) {
      count := count + 1.U
      stateReg := end
    }
    is(end) {
      count := count + 1.U
      stateReg := idle
    }
  }
    when(count === 90.U){
      count := 65.U
    }
    io.FSMdeq.bits := count
}
}