package io
import chisel3._
import chisel3.util._



// Needs fixin
class FMAC_filter extends  Module{
      val io = IO(new Bundle{
        val in = Module(new FMAC)
        val flushBuffer = Output(Bool())
      })
  // Counter to count the amount of bytes in each frame recieved
  val cntFrame = RegInit(0.U(32.W))
  val GoodFrame = WireInit(false.B)
  val BadFrame = WireInit(false.B)
 // If ctl is high, frame is recieved, increase counter at each
  // rising edge, as a byte is transmitted each rising edge
  when(io.in.ethmac1g.io.rx_axis_tvalid){
    cntFrame := cntFrame
  }

  // When t_last is high the frame has reached the end. reset counter
  when(io.in.ethmac1g.io.rx_axis_tlast){
    cntFrame := 0.U
  }

  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferEvaluate:: bufferBadFrame :: bufferGoodFrame :: bufferIntr :: Nil = Enum(5)
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

  }
  when(stateBuffer === bufferBadFrame){

  }
  when(stateBuffer === bufferIntr){

  }
}
