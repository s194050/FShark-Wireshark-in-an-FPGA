package io

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
    val bufferFull = Output(Bool())
    val bufferEmpty = Output(Bool())

    val filter_bus = Flipped(Decoupled(new Bundle {
      val flushFrame = Output(Bool())
      val goodFrame = Output(Bool())
      val addHeader = Output(Bool())
      val tdata = Output(UInt((datawidth).W))
    }))
  })
  //Initialize signals
  io.bufferEmpty := WireInit(false.B)
  io.bufferFull := WireInit(false.B)

  /*
  IO's  to push and pop and get status
  */
  val data = Reg(Vec(depth, Bool()))
  val head = RegInit(0.U(datawidth.W))
  // For handling flushing of a bad frame
  val temp = Mux(io.filter_bus.bits.flushFrame,head-io.filter_bus.bits.tdata,head)
  // For adding the header containing length in front of the frame
  val Address = Mux(io.filter_bus.bits.addHeader,head-(io.filter_bus.bits.tdata + 2.U),temp)

  val tail = RegInit(0.U(datawidth.W))
  val bufferValue = RegInit(0.U(datawidth.W))

  //Status booleans
  io.bufferEmpty := (head === tail)

  when(head + 1.U === depth.U){
    head := 0.U
  }

  when(tail + 1.U === depth.U){
    tail := 0.U
  }

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL
  io.filter_bus.ready := true.B

  when(io.ocp.M.Cmd === OcpCmd.RD && !io.bufferEmpty && io.filter_bus.bits.goodFrame) { // Read from buffer (Pop)
    respReg := OcpResp.DVA
    when(tail + 1.U === depth.U){
      tail := 0.U
    }
    io.bufferFull := false.B
    tail := tail + 1.U
    bufferValue := io.bufferData(tail)
  }

  // Write to the buffer (Push)
  when(io.filter_bus.valid && !io.bufferFull){
    io.bufferData(Address) := io.filter_bus.bits.tdata
    head := head + 1.U
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
    bufferElement := data(mappedIndex(index))
  }

  // The number of valid data in the current buffer
  val difference = tail - head
  when((difference) < 0.U) {
    io.bufferLength := (difference) + depth.U
  }.otherwise {
    io.bufferLength := (difference)
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := bufferValue
}
