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
class MemFifo[T <: Data](gen: T, depth: Int,addrWidth: Int, dataWidth: Int, inputDataWidth: Int) extends Fifo(gen: T,
  depth: Int, addrWidth: Int, dataWidth: Int, inputDataWidth : Int) {

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
  val writeToFIFO = RegInit(true.B)
  val frameLength = RegInit(0.U(inputDataWidth.W))

  val idle :: fill :: fill_empty :: waitForEOF :: emptyToPatmos :: e_idle :: e_valid :: e_full :: Nil = Enum(8)
  val stateReg = RegInit(idle)
  val shadowReg = Reg(gen)

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  val data = mem.read(readPtr)

  switch(stateReg) {
    is(idle){ // Idle state
      when(io.endOfFrame){ // When the first EOF occurs go to fill the FIFO
        stateReg := fill
      }
    }

    is(fill){ // Keep filling the FIFO with frame data until trigger or the FIFO is full
      when(io.enq.valid && !fullReg) {
        mem.write(writePtr, io.enq.bits)
        emptyReg := false.B
        fullReg := nextWrite === readPtr
        incrWrite := true.B
      }

      when(io.stopFrameRecording){ // Trigger, that determines that the current data is to be sent along
        stateReg := waitForEOF
      }
      when(fullReg){ // When the FIFO is full, empty some of the first elements to avoid stall
        writeToFIFO := false.B
        when(frameLength === 0.U){ // Read length of frame
          frameLength := data
        }
        stateReg := fill_empty
      }
    }

    is(fill_empty){
      writeToFIFO := true.B
      // Keep dumping frame

      when(io.enq.valid && !fullReg) {
        mem.write(writePtr, io.enq.bits)
        emptyReg := false.B
        fullReg := nextWrite === readPtr
        incrWrite := true.B
      }
      when(!emptyReg) { // We dump the frame to avoid a full FIFO
        when(frameLength =/= 0.U){
          frameLength := frameLength - 1.U
        }
        fullReg := false.B
        emptyReg := nextRead === writePtr
        incrRead := true.B
      }
      when(frameLength === 0.U && io.stopFrameRecording) { // If trigger, go to read from FIFO
        stateReg := waitForEOF
        frameLength := 0.U
      }.elsewhen(frameLength === 0.U){ // Otherwise go back and check if FIFO is full
        stateReg := fill
        frameLength := 0.U
      }
    }

    is(waitForEOF){ // Wait for the current frame to be filled
      when(io.enq.valid && !fullReg && !io.endOfFrame) { // Write to the FIFO if frame is not done
        mem.write(writePtr, io.enq.bits)
        emptyReg := false.B
        fullReg := nextWrite === readPtr
        incrWrite := true.B
      }
      when(io.endOfFrame){ // When the end of the frame is met, go to empty
        stateReg := emptyToPatmos
      }
    }

    is(emptyToPatmos){ // Read from FIFO to Patmos
      writeToFIFO := false.B // Stop writing and stop recording frame data

      stateReg := e_idle
    }

    is(e_idle) {
      when(io.ocp.M.Cmd === OcpCmd.RD) {
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := e_valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }
      }
    }
    is(e_valid) {
      when(io.ocp.M.Cmd === OcpCmd.RD) {
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := e_valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }.otherwise {
          stateReg := idle
        }
      }.otherwise {
        shadowReg := data
        stateReg := e_full
      }

    }
    is(e_full) {
      when(io.ocp.M.Cmd === OcpCmd.RD) {
        when(!emptyReg) {
          respReg := OcpResp.DVA
          stateReg := e_valid
          fullReg := false.B
          emptyReg := nextRead === writePtr
          incrRead := true.B
        }.otherwise {
          stateReg := idle
        }

      }
    }
  }


  io.enq.ready := writeToFIFO
  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := Mux(stateReg === e_valid, data, shadowReg)
}
