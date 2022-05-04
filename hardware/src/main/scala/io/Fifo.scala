// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package io

import chisel3._
import chisel3.util._
import ocp._

/**
  * FIFO IO with enqueue and dequeue ports using the ready/valid interface.
  */
class FifoIO[T <: Data](private val gen: T, val addrWidth: Int,val dataWidth: Int, val inputDatawidth: Int) extends Bundle {
  val enq = Flipped(new DecoupledIO(gen))
  //val deq = new DecoupledIO(gen)
  val ocp = new OcpCoreSlavePort(addrWidth,dataWidth)
  val endOfFrame = Input(Bool())

  val stopFrameRecording = Input(Bool())
}

/**
  * Base class for all FIFOs.
  */
abstract class Fifo[T <: Data](gen: T, val depth: Int, addrWidth: Int, dataWidth: Int, val inputDatawidth: Int) extends Module {
  val io = IO(new FifoIO(gen,addrWidth, dataWidth, inputDatawidth))

  assert(depth > 0, "Number of buffer elements needs to be larger than 0")
}
