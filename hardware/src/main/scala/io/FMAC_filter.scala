package io
import chisel3._
import chisel3.util._




class FMAC_filter extends  Module{
      val io = IO(new Bundle{
        val in = Module(new FMAC)
      })
  // Counter to count the amount of bytes in each frame recieved
  val cntFrame = RegInit(0.U(32.W))
  val filteredFrame = WireInit(true.B)

 // If ctl is high, frame is recieved, increase counter at each
  // rising edge, as a byte is transmitted each rising edge
  when(io.in.io.pins.rgmii_rx_ctl){
    cntFrame := cntFrame + io.in.io.pins.rgmii_rx_clk
  }

  // When t_last is high the frame has reached the end. reset counter
  when(io.in.ethmac1g.io.rx_axis_tlast){
    cntFrame := 0.U
  }

  // Check if frame[18] is the correct value otherwise filter it out
  when(cntFrame === 18.U){
    when(io.in.ethmac1g.io.rx_axis_tdata === 0x18.U){
      filteredFrame := false.B
    }
  }
  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferFlush :: bufferOkay :: Nil = Enum(3)
  val stateBuffer = RegInit(bufferIdle)

  when(stateBuffer === bufferIdle){
    when(!filteredFrame) {
      stateBuffer := bufferFlush
    }.otherwise{
      stateBuffer := bufferOkay
    }
  }
  when(stateBuffer === bufferFlush){
    // Flush the buffer
    filteredFrame := true.B
  }
  when(stateBuffer === bufferOkay){
    // Keep writing to buffer
  }

}
