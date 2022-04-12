package io
import chisel3._
import chisel3.util._
/*
  Hardware filter for use on the DE2-115 board, for filtering frames at 1G speeds, this is done due to the proce-
  ssor used, is clocked slower than the utilised MAC.
 */

class FMAC_filter(datawidth: Int = 16) extends  Module{
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
  // Counter to count the amount of bytes in each frame recieved
  val cntFrame = RegInit(0.U(datawidth.W))
  val GoodFrame = WireInit(false.B)
  val BadFrame = WireInit(false.B)
  val frameSize = RegInit(0.U(datawidth.W))


  // Initialize signals
  io.axis_tready := WireInit(false.B)
  io.filter_bus.valid := WireInit(false.B)
  io.filter_bus.bits.flushFrame := WireInit(false.B)
  io.filter_bus.bits.goodFrame := WireInit(false.B)
  io.filter_bus.bits.addHeader := WireInit(false.B)
  io.filter_bus.bits.tdata := WireInit(io.axis_tdata)

 // If ctl is high, frame is recieved, increase counter at each
  // rising edge, as a byte is transmitted each rising edge
  when(io.axis_tvalid){
    when(io.axis_tkeep === 0.U){ // If tkeep is low, it means that a byte is invalid
      cntFrame := cntFrame + 1.U // thus increment by one
    }.otherwise{
      cntFrame := cntFrame + 2.U // Increment by two as bus width is 16 bit = 2 bytes
    }
  }

  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferEvaluate:: bufferBadFrame :: bufferGoodFrame :: bufferFlushFrame :: bufferAddHeader :: Nil = Enum(6)
  val stateBuffer = RegInit(bufferIdle)

  when(stateBuffer === bufferIdle) {
    io.filter_bus.bits.flushFrame := false.B
    io.filter_bus.bits.addHeader := false.B
    io.filter_bus.bits.goodFrame := false.B
    BadFrame := false.B
    GoodFrame := false.B

    when(io.axis_tvalid && io.filter_bus.ready) {
      io.axis_tready := true.B
      stateBuffer := bufferEvaluate
    }.otherwise {
      stateBuffer := bufferIdle
    }
  }
  when(stateBuffer === bufferEvaluate) {
    io.filter_bus.valid := true.B
    io.axis_tready := true.B
    // Check if frame[18] is the correct value otherwise filter it out
    when(cntFrame === 18.U) {
      when(io.axis_tdata === 0x1312.U) {
        GoodFrame := true.B
        BadFrame := false.B
      }.otherwise {
        BadFrame := true.B
        GoodFrame := false.B
      }
    }
    // Further filter logic for increasing complexity

    // Check frame whether it is good or bad
    when(GoodFrame === true.B && BadFrame === false.B) {
      stateBuffer := bufferGoodFrame
    }
    when(GoodFrame === false.B && BadFrame === true.B) {
      stateBuffer := bufferBadFrame
    }
  }

  when(stateBuffer === bufferGoodFrame) {
    io.filter_bus.valid := true.B
    io.axis_tready := true.B
    io.filter_bus.bits.goodFrame := true.B
    //State for when the frame supports the filter requirements
    when(io.axis_tlast) {
      io.axis_tready := false.B
      frameSize := cntFrame + 1.U // Possibly wrong
      stateBuffer := bufferAddHeader
    }
  }

  when(stateBuffer === bufferAddHeader) {
    // Add length of the frame to the header
    io.filter_bus.valid := true.B
    io.axis_tready := false.B
    io.filter_bus.bits.addHeader := true.B
    io.filter_bus.bits.tdata := frameSize

    cntFrame := 0.U
    stateBuffer := bufferIdle
  }


  when(stateBuffer === bufferBadFrame) {
    // Interrupt filling of the circular buffer
    io.axis_tready := true.B
    io.filter_bus.valid := false.B
    when(io.axis_tlast) {
      frameSize := cntFrame + 1.U // Possibly wrong
      stateBuffer := bufferFlushFrame
    }
  }
  when(stateBuffer === bufferFlushFrame) {
    io.axis_tready := false.B
    // Flush the unwanted frame from the buffer
    io.filter_bus.bits.tdata := frameSize
    io.filter_bus.bits.flushFrame := true.B

    cntFrame := 0.U
    stateBuffer := bufferIdle
  }

}
