package io
import chisel3._
import chisel3.util._



// Needs fixin
class FMAC_filter extends  Module{
      val io = IO(new Bundle{
        val in = Module(new FMAC)
        val flushBuffer = Output(Bool())
        val intrDataStream = Output(Bool())
        val frameSize = Output(UInt())
        val incrPntr = Output(Bool())
      })
  // Counter to count the amount of bytes in each frame recieved
  val cntFrame = RegInit(0.U(12.W))
  val GoodFrame = WireInit(false.B)
  val BadFrame = WireInit(false.B)

  // Initialize signals
  io.flushBuffer := false.B
  io.intrDataStream := false.B
  io.incrPntr := false.B
  io.frameSize := 0.U

 // If ctl is high, frame is recieved, increase counter at each
  // rising edge, as a byte is transmitted each rising edge
  when(io.in.ethmac1g.io.rx_axis_tvalid){
    when(io.in.ethmac1g.io.rx_axis_tkeep === 0.U){ // If tkeep is low, it means that a byte is invalid
      cntFrame := cntFrame + 1.U // thus increment by one
    }.otherwise{
    cntFrame := cntFrame + 2.U // Increment by two as bus width is 16 bit = 2 bytes
    }
  }

  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferEvaluate:: bufferBadFrame :: bufferGoodFrame :: bufferIntr :: bufferAddHeader :: bufferFrameSent :: Nil = Enum(7)
  val stateBuffer = RegInit(bufferIdle)

  when(stateBuffer === bufferIdle){
    when(io.in.ethmac1g.io.rx_axis_tvalid){
      io.in.ethmac1g.io.rx_axis_tready := true.B
      stateBuffer := bufferEvaluate
    }.otherwise{
      stateBuffer := bufferIdle
    }
  }
  when(stateBuffer === bufferEvaluate){
    io.incrPntr := true.B
    // Check if frame[18] is the correct value otherwise filter it out
    when(cntFrame === 18.U){
      when(io.in.ethmac1g.io.rx_axis_tdata === 0x18.U){
        GoodFrame := true.B
        BadFrame := false.B
      }
    }
    // Further filter logic for increasing complexity

    // Check frame whether it is good or bad
    when(GoodFrame === true.B && BadFrame === false.B){
      stateBuffer := bufferGoodFrame
    }
    when(GoodFrame === false.B && BadFrame === true.B){
      stateBuffer := bufferBadFrame
    }
  }

  when(stateBuffer === bufferGoodFrame){


    when(io.in.ethmac1g.io.rx_axis_tlast){
      // When t_last is high the frame has reached the end. reset counter
      cntFrame := 0.U
      stateBuffer := bufferAddHeader
    }
  }

  when(stateBuffer === bufferAddHeader){
    io.in.ethmac1g.io.rx_axis_tready := false.B

  }

  when(stateBuffer === bufferFrameSent){
    io.in.ethmac1g.io.rx_axis_tready := false.B
  }

  when(stateBuffer === bufferBadFrame){
    io.flushBuffer := true.B
    io.frameSize := cntFrame
    stateBuffer := bufferIntr
  }
  when(stateBuffer === bufferIntr){
    io.intrDataStream := true.B
    when(io.in.ethmac1g.io.rx_axis_tlast){
      // When t_last is high the frame has reached the end. reset counter
      cntFrame := 0.U
      stateBuffer := bufferIdle
    }
  }
}
