package io

import chisel3._
import chisel3.util._
import chisel3.internal.HasId
import patmos.Constants.{CLOCK_FREQ, UART_BAUD}
import ocp._
class IObuffer(depth: Int, width: Int) extends Module {
  val io = IO(new Bundle{
    val bufferData = Output(Vec(depth,UInt(width.W)))
    val bufferLength = Output(UInt(log2Ceil(depth).W))
    val bufferFull = Output(Bool())
    val bufferEmpty = Output(Bool())
  })
}


object CircularBuffer extends DeviceObject {
  var depth = 1536+2 // +2 For header bytes signaling frame length
  var width = 12

  def init(params: Map[String, String]) = {
    depth = getIntParam(params,"depth")
    width = getIntParam(params,"width")
  }

  def create(params: Map[String, String]): CircularBuffer = Module(new CircularBuffer(depth,width))
}


class CircularBuffer(depth: Int, width: Int) extends CoreDevice() {
  val out = new IObuffer(depth,width) // I/O to avoid overriding OCP interface
  val in = Module(new FMAC_filter)
  /*
  IO's  to push and pop and get status
  */
  val data = Reg(Vec(depth, Bool()))
  val start = RegInit(0.U(width.W))
  val end = RegInit(0.U(width.W))
  val bufferValue = RegInit(0.U(width.W))

  //Status booleans
  out.io.bufferFull := false.B
  out.io.bufferEmpty := !out.io.bufferFull && (start === end)

  // Default response
  val respReg = RegInit(OcpResp.NULL)
  respReg := OcpResp.NULL


  // Flush the buffer as frame is unwanted
  when(in.io.flushBuffer){
    when(start - in.io.frameSize < 0.U){
      start := 0.U
    }.otherwise{
      start := start - in.io.frameSize
    }
   /* when(end - in.io.frameSize < 0.U){
      end := 0.U
    }.otherwise{
      end := end - - in.io.frameSize
    }

    */
    in.io.flushBuffer := false.B
  }

  // When new frame is loaded make room for length bytes.
  when(in.io.incrPntr){
    start := start + 2.U
    in.io.incrPntr := false.B
  }
  // Need some code here to push and pop elements
  when(io.ocp.M.Cmd === OcpCmd.RD && !out.io.bufferEmpty) { // Read from buffer (Pop)
    respReg := OcpResp.DVA
    when(end + 1.U === depth.U){
      end := 0.U
    }.otherwise{
      out.io.bufferFull := false.B
      end := end + 1.U
      bufferValue := out.io.bufferData(end)
    }
  }

  // Write to the buffer (Push)
  when(!in.io.flushBuffer){

  }

  // Mapping the indices between current head and tail
  // to the 0 to n indices of the buffer
  def mappedIndex(i: Int): UInt = {
    val out = Wire(UInt(log2Ceil(depth).W))
    when((i.U + start) <= depth.U) {
      out := i.U + start
    }.otherwise {
      out := (i.U + start) - depth.U
    }
    out
  }

  // Mux to map between the circular buffer elements and 0 to n style vector
  out.io.bufferData.zipWithIndex.foreach { case (bufferElement, index) =>
    bufferElement := data(mappedIndex(index))
  }

  // The number of valid data in the current buffer
  val difference = end - start
  when((difference) < 0.U) {
    out.io.bufferLength := (difference) + depth.U
  }.otherwise {
    out.io.bufferLength := (difference)
  }

  // Connections to master
  io.ocp.S.Resp := respReg
  io.ocp.S.Data := bufferValue
}
