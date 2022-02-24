package io

import Chisel._
import patmos.Constants._


class data extends Bundle(){
  val enq = (new DecoupledIO(UInt(32.W)))
}


class SimpleFSM() extends Module {
  val count= Reg(init = UInt(0, 32))
  count := 65.U

  val io = IO(new data())
  val idle :: transfer :: end :: Nil = Enum(3)
  val stateReg = RegInit(idle)
  when(io.enq.ready) {
    when(count === 90.U){
      count := 65.U
    }
  switch(stateReg) {
    is(idle) {
      io.enq.bits := count
      stateReg := transfer
      count := count + 1.U
    }
    is(transfer) {
      io.enq.bits := count
      stateReg := end
      count := count + 1.U
    }
    is(end) {
      io.enq.bits := count
      stateReg := idle
      count := count + 1.U
    }
  }
}
}