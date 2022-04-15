package io

import Chisel.debug
import chisel3._
import chisel3.util._
import chisel3.internal.HasId
import chisel3.util.ImplicitConversions.intToUInt
import patmos.Constants.{CLOCK_FREQ, UART_BAUD}
import ocp._

// Length = 1536
class CircularBuffer(depth: Int = 512, datawidth: Int = 16,addrWidth: Int, dataWidth: Int) extends Module() {
  val bitWidth = log2Ceil(depth)
  val actualDepth = math.pow(2,bitWidth).toInt// Calculate the actual depth, to confer with log2 logic
  val io = IO(new Bundle{
    val ocp = new OcpCoreSlavePort(addrWidth,dataWidth)
    val bufferData = Output(Vec(actualDepth, UInt(datawidth.W)))
    val bufferLength = Output(UInt((bitWidth).W))

    val filter_bus = Flipped(Decoupled(new Bundle {
      val flushFrame = Output(Bool())
      val goodFrame = Output(Bool())
      val badFrame = Output(Bool())
      val addHeader = Output(Bool())
      val tdata = Output(UInt(datawidth.W))
    }))
  })

  //Initialize signals
  val bufferFull = WireInit(false.B)
  val bufferEmpty = WireInit(true.B)
  val bufferFullNext = WireInit(false.B)
  val bufferEmptyNext = WireInit(false.B)
  io.filter_bus.ready := WireInit(false.B)
  /*
  IO's  to push and pop and get status
  */
  val data = Reg(Vec(actualDepth, UInt(datawidth.W)))
  val head = RegInit(1.U(bitWidth.W))
  val tail = RegInit(0.U(bitWidth.W))
  val bufferValue = RegInit(0.U(datawidth.W))


  // For handling flushing of a bad frame
   val temp = Mux(io.filter_bus.bits.flushFrame,head-(io.filter_bus.bits.tdata + 1.U) , head)
  // For adding the header containing length in front of the frame
  val Address = Mux(io.filter_bus.bits.addHeader,head-(io.filter_bus.bits.tdata + 2.U), temp)

  //Status booleans
  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL

  // Check buffer status
  bufferEmpty := io.bufferLength === 0.U
  bufferEmptyNext := io.bufferLength - 1.U === 0.U
  //----------------
  bufferFull := io.bufferLength >= actualDepth - 1.U
  bufferFullNext := io.bufferLength >= actualDepth - 2.U

  when(bufferFull){
    io.filter_bus.ready := false.B
  }.otherwise{
    io.filter_bus.ready := true.B
  }

  when(io.filter_bus.valid && !bufferFull){
    when(head === actualDepth){
      head := 1.U
    }.otherwise{
      head := head + 1.U
    }
    data(Address) := io.filter_bus.bits.tdata
  }

  when(io.ocp.M.Cmd === OcpCmd.RD && !bufferEmpty){ // && io.filter_bus.bits.goodFrame When ocp is removed.
    respReg := OcpResp.DVA
    when(tail === actualDepth){
      tail  := 0.U
    }.otherwise{
      tail := tail + 1.U
    }
    bufferValue := data(tail)
  }

  when(io.filter_bus.bits.flushFrame){
    head := temp
  }

  // Mapping the indices between current head and tail
  // to the 0 to n indices of the buffer
  def mappedIndex(i: Int): UInt = {
    val out = Wire(UInt((actualDepth).W))
    when((i.U + head) <= actualDepth.U) {
      out := i.U + head
    }.otherwise {
      out := (i.U + head) - actualDepth.U
    }
    out
  }

  // Mux to map between the circular buffer elements and 0 to n style vector
  io.bufferData.zipWithIndex.foreach { case (bufferElement, index) =>
    bufferElement := data(mappedIndex((index)))
  }

  // The number of valid data in the current buffer
  val difference = (head - 1.U) - tail
  when(difference < 0.U) {
    io.bufferLength := (difference) + actualDepth.U
  }.otherwise {
    io.bufferLength := (difference)
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := bufferValue
}
