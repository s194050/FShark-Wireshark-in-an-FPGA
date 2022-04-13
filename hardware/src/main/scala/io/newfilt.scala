package io
import chisel3._
import chisel3.util._

class newfilt(datawidth: Int = 16) extends Module{
  val io = IO(new Bundle{
    // MAC input and output
    val axis_tvalid = Input(Bool())
    val axis_tkeep = Input(UInt((datawidth/8).W))
    val axis_tready = Output(Bool())
    val axis_tdata = Input(UInt((datawidth).W))
    val axis_tlast = Input(Bool())

    // Bus for connecting filter and buffer
    val filter_bus = Decoupled(new Bundle{
      val flushFrame = Output(Bool())
      val goodFrame = Output(Bool())
      val addHeader = Output(Bool())
      val tdata = Output(UInt((datawidth).W))
    })
  })
  // Initialize registers
  val cntFrame = RegInit(0.U(datawidth.W))
  val frameSize = WireInit(0.U(datawidth.W))
  // Initialize booleans
  val GoodFrame = WireInit(false.B)
  val BadFrame = WireInit(false.B)
  io.axis_tready := WireInit(false.B)
  io.filter_bus.valid := WireInit(false.B)
  io.filter_bus.bits.flushFrame := WireInit(false.B)
  io.filter_bus.bits.goodFrame := WireInit(false.B)
  io.filter_bus.bits.addHeader := WireInit(false.B)
  io.filter_bus.bits.tdata := WireInit(0.U(datawidth.W))

  // Counter
  when(io.axis_tvalid) {
    cntFrame := cntFrame + 2.U // Increment by two as bus width is 16 bit = 2 bytes
  }

  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferEvaluate:: bufferBadFrame :: bufferGoodFrame :: bufferFlushFrame :: bufferAddHeader :: Nil = Enum(6)
  val stateBuffer = RegInit(bufferIdle)


  switch(stateBuffer) {
    is(bufferIdle) {
      // Reset booleans
      GoodFrame := false.B
      BadFrame := false.B
      io.filter_bus.valid := false.B
      io.filter_bus.bits.flushFrame := false.B
      io.filter_bus.bits.goodFrame := false.B
      io.filter_bus.bits.addHeader := false.B
      io.axis_tready := false.B
      cntFrame := 0.U
      frameSize := 0.U

      when(io.axis_tvalid && io.filter_bus.ready){
        stateBuffer := bufferEvaluate
      }
    }

    is(bufferEvaluate){
      // Check if the current frame is wanted or unwanted
      // Set status
      io.axis_tready := true.B
      io.filter_bus.valid := true.B
      io.filter_bus.bits.tdata := io.axis_tdata
      when(cntFrame === 18.U){
        when(io.axis_tdata === 0x1312.U){
          stateBuffer := bufferGoodFrame
          GoodFrame := true.B
        }.otherwise{
          stateBuffer := bufferBadFrame
          BadFrame := true.B
        }
      }
    }

    is(bufferGoodFrame){
      // When the frame is good we can begin reading the frame in the buffer
      io.filter_bus.valid := true.B
      io.axis_tready := true.B
      io.filter_bus.bits.goodFrame := true.B
      io.filter_bus.bits.tdata := io.axis_tdata

      when(io.axis_tlast){
        cntFrame := cntFrame
        io.axis_tready := false.B
        frameSize := (cntFrame + 1.U) >> 1
        stateBuffer := bufferAddHeader
      }
    }

    is(bufferAddHeader){
      // Add header which is the length of the frame for usage in the buffer
      io.axis_tready := true.B
      io.filter_bus.valid := true.B
      io.filter_bus.bits.goodFrame := true.B
      io.filter_bus.bits.addHeader := true.B

      io.filter_bus.bits.tdata := (cntFrame + 1.U) >> 1


      stateBuffer := bufferIdle

    }

    is(bufferBadFrame){
      io.axis_tready := true.B
      io.filter_bus.valid := false.B


      when(io.axis_tlast){
        io.axis_tready := false.B
        cntFrame := cntFrame
        frameSize := (cntFrame + 1.U) >> 1
        stateBuffer := bufferFlushFrame
      }
    }

    is(bufferFlushFrame){
      io.axis_tready := true.B
      io.filter_bus.valid := false.B
      io.filter_bus.bits.flushFrame := true.B
      io.filter_bus.bits.tdata := frameSize

      stateBuffer := bufferIdle

    }
  }
}
