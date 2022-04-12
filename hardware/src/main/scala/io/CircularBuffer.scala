package io

import Chisel.debug
import chisel3._
import chisel3.util._
import chisel3.internal.HasId
import patmos.Constants.{CLOCK_FREQ, UART_BAUD}
import ocp._

// Length = 1536
class CircularBuffer(depth: Int = 204, datawidth: Int = 16,addrWidth: Int, dataWidth: Int) extends Module() {
  val io = IO(new Bundle{
    val ocp = new OcpCoreSlavePort(addrWidth,dataWidth)
    val bufferData = Output(Vec(depth, UInt(datawidth.W)))
    val bufferLength = Output(UInt(log2Ceil(depth).W))

    val filter_bus = Flipped(Decoupled(new Bundle {
      val flushFrame = Output(Bool())
      val goodFrame = Output(Bool())
      val addHeader = Output(Bool())
      val tdata = Output(UInt(datawidth.W))
    }))
  })
  //Initialize signals
  val bufferFull = RegInit(false.B)
  val bufferEmpty = RegInit(true.B)
  io.filter_bus.ready := WireInit(false.B)
  /*
  IO's  to push and pop and get status
  */
  val data = Reg(Vec(depth, UInt(datawidth.W)))
  val head = RegInit(0.U(datawidth.W))
  // For handling flushing of a bad frame
  val temp = Mux(io.filter_bus.bits.flushFrame,head-io.filter_bus.bits.tdata,head)
  // For adding the header containing length in front of the frame
  val Address = Mux(io.filter_bus.bits.addHeader,head-(io.filter_bus.bits.tdata + 2.U),temp)
  val tail = RegInit(0.U(datawidth.W))
  val bufferValue = RegInit(0.U(datawidth.W))

  //Status booleans
  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  // Check buffer status
  bufferEmpty := head === tail && !bufferFull
  when(bufferEmpty){
    io.filter_bus.ready := true.B
  }

  when(tail - 1.U === head){
    bufferFull := true.B
  }.elsewhen((tail === 0.U) && head === (depth.U - 1.U)){
    bufferFull := true.B
  }

  when(!bufferFull){
    io.filter_bus.ready := true.B
  }

  when(io.ocp.M.Cmd === OcpCmd.RD && !bufferEmpty) { // Read from buffer (Pop)
    respReg := OcpResp.DVA
    when(tail === depth.U) {
        tail := 0.U
    }.otherwise{
      tail := tail + 1.U
    }
    bufferValue := data(tail)
    bufferFull := false.B
    io.filter_bus.ready := true.B
  }

  // Write to the buffer (Push)
  when(io.filter_bus.valid && !bufferFull){
    io.filter_bus.ready := true.B
    when(head === depth.U){
      head := 0.U
    }.otherwise{
      head := head + 1.U
    }
    data(Address) := io.filter_bus.bits.tdata
    bufferEmpty := false.B
  }

  // Mapping the indices between current head and tail
  // to the 0 to n indices of the buffer
  def mappedIndex(i: Int): UInt = {
    val out = Wire(UInt(log2Ceil(depth).W))
    when((i.U + head) <= depth.U) {
      out := i.U + head
    }.otherwise {
      out := (i.U + head) - depth.U
    }
    out
  }

  // Mux to map between the circular buffer elements and 0 to n style vector
  io.bufferData.zipWithIndex.foreach { case (bufferElement, index) =>
    bufferElement := data(mappedIndex((index)))
  }

  // The number of valid data in the current buffer
  val difference = tail - head
  when(difference < 0.U) {
    io.bufferLength := (difference) + depth.U
  }.otherwise {
    io.bufferLength := (difference)
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := bufferValue
}
