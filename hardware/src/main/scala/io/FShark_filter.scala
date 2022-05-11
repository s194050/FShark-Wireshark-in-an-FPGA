package io
import chisel3._
import chisel3.util._
/*
  Hardware filter for use on the DE2-115 board, for filtering frames at 1G speeds, this is done due to the proce-
  ssor used, is clocked slower than the utilised MAC.
 */

class FShark_filter(datawidth: Int = 16) extends  Module{
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
      val addHeader = Output(Bool())
      val tdata = Output(UInt((datawidth).W))
    })

    val filterIndex = Input(UInt(datawidth.W))
    val filterValue = Input(UInt((datawidth/2).W))
    val filterSet = Input(Bool())
  })
  // Initialize counter register
  val cntFrame = RegInit(0.U(datawidth.W))
  // Initialize booleans
  io.axis_tready := WireInit(false.B)
  io.filter_bus.valid := WireInit(false.B)
  io.filter_bus.bits.flushFrame := WireInit(false.B)
  io.filter_bus.bits.addHeader := WireInit(false.B)
  io.filter_bus.bits.tdata := WireInit(0.U(datawidth.W))


  when(!io.filterSet){ // Stall eveything until filter values are set
    io.axis_tready := false.B
  }

  // Counter
  when(io.axis_tvalid && io.filterSet) {
    when(io.axis_tkeep =/= 3.U){ // Increment by one, when byte uneven.S
      cntFrame := cntFrame + 1.U
    }.otherwise {
      cntFrame := cntFrame + 2.U // Increment by two as bus width is 16 bit = 2 bytes
    }
  }

  // FSM to determine whether frame data in buffer is kept or flushed
  val bufferIdle :: bufferEvaluate:: bufferBadFrame :: bufferGoodFrame :: bufferFlushFrame :: bufferAddHeader :: Nil = Enum(6)
  val stateBuffer = RegInit(bufferIdle)

when(io.filter_bus.ready && io.filterSet) { // Only give data to buffer if it is not full
  switch(stateBuffer) {
    is(bufferIdle) {
      // Reset booleans
      io.filter_bus.valid := false.B
      io.filter_bus.bits.flushFrame := false.B
      io.filter_bus.bits.addHeader := false.B
      io.axis_tready := false.B
      cntFrame := 0.U

      when(io.axis_tvalid) {
        stateBuffer := bufferEvaluate
      }
    }

    is(bufferEvaluate) {
      // Check if the current frame is wanted or unwanted
      // Set status
      io.axis_tready := true.B
      io.filter_bus.valid := true.B
      io.filter_bus.bits.tdata := io.axis_tdata
      /*
       when(filterIndex(0) == 1)
          when(io.axis_tdata(15,8) === filtervalue{
       when(filterIndex(0) == 0){
          when(io.axis_tdata(7,0) === filterValue{
       */
      when(cntFrame === io.filterIndex) {
        when(io.axis_tdata(7,0) === io.filterValue) {
          stateBuffer := bufferGoodFrame
        }.otherwise {
          stateBuffer := bufferBadFrame
        }
      }
    }

    is(bufferGoodFrame) {
      // When the frame is good we can begin reading the frame in the buffer
      io.filter_bus.valid := true.B
      io.axis_tready := true.B

      io.filter_bus.bits.tdata := io.axis_tdata

      when(io.axis_tlast) {
        cntFrame := cntFrame // Set the counter to the current value at next rising edge
        io.axis_tready := false.B // Stop recieving from the MAC FIFO
        stateBuffer := bufferAddHeader
      }
    }

    is(bufferAddHeader) {
      // Add header which is the length of the frame for usage in the buffer
      io.axis_tready := true.B
      io.filter_bus.valid := true.B
      io.filter_bus.bits.addHeader := true.B
      io.filter_bus.bits.tdata := cntFrame //(cntFrame + 1.U) >> 1 // Divide by two and round up


      stateBuffer := bufferIdle

    }

    is(bufferBadFrame) {
      // When the frame is bad, such that it is unwanted prepare to flush it from the buffer, and stop sending it
      // to the buffer.
      cntFrame := cntFrame // Set the counter constant, as we have stopped sending the frame to the buffer
      io.axis_tready := true.B
      io.filter_bus.valid := false.B

      when(io.axis_tlast) { // Receive data from MAC until tlast is high
        cntFrame := cntFrame // Set the counter to the current value at next rising edge
        io.axis_tready := false.B
        io.filter_bus.valid := true.B
        stateBuffer := bufferFlushFrame
      }
    }

    is(bufferFlushFrame) {
      cntFrame := cntFrame // This is unnecessary in this state but is done for continuity
      io.axis_tready := true.B
      io.filter_bus.valid := true.B
      io.filter_bus.bits.flushFrame := true.B

      io.filter_bus.bits.tdata := cntFrame //(cntFrame + 1.U) >> 1 // Divide by two, as we receive 16 bits

      stateBuffer := bufferIdle
    }
  }
}
}
