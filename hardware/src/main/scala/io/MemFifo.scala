// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package io

import chisel3._
import chisel3.util._
import firrtl.FirrtlProtos.Firrtl.Statement.When
import ocp._

/**
  * FIFO with memory and read and write pointers.
  * Extra shadow register to handle the one cycle latency of the synchronous memory.
  */
class MemFifo[T <: Data](gen: T, depth: Int,addrWidth: Int, dataWidth: Int) extends Fifo(gen: T, depth: Int, addrWidth: Int, dataWidth: Int) {

  def counter(depth: Int, incr: Bool): (UInt, UInt) = {
    val cntReg = RegInit(0.U(log2Ceil(depth).W))
    val nextVal = Mux(cntReg === (depth - 1).U, 0.U, cntReg + 1.U)
    when(incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }
  val mem = SyncReadMem(depth, gen)
  val incrRead = WireInit(false.B)
  val incrWrite = WireInit(false.B)
  val (readPtr, nextRead) = counter(depth, incrRead)
  val (writePtr, nextWrite) = counter(depth, incrWrite)

  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B)

  val idle :: valid :: full :: Nil = Enum(3)
  val stateReg = RegInit(idle)
  val shadowReg = Reg(gen)

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  when(io.enq.valid && !fullReg) {
      mem.write(writePtr, io.enq.bits)
      emptyReg := false.B
      fullReg := nextWrite === readPtr
      incrWrite := true.B

  }

  val data = mem.read(readPtr)

  switch(stateReg) {
    is(idle) {
      when(io.ocp.M.Cmd === OcpCmd.RD) {
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }
      }
    }
    is(valid) {
      when(io.ocp.M.Cmd === OcpCmd.RD) {
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }.otherwise {
          stateReg := idle
        }
      }.otherwise {
        shadowReg := data
        stateReg := full
      }

    }
    is(full) {
      when(io.ocp.M.Cmd === OcpCmd.RD) { // io.deq.ready
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }.otherwise {
          stateReg := idle
        }

      }
    }
  }


  io.enq.ready := !fullReg
  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := Mux(stateReg === valid, data, shadowReg)
}
