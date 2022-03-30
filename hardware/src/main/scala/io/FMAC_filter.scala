package io
import chisel3._
import chisel3.util._




class FMAC_filter extends  Module{
      val io = IO(new Bundle{
        val in = Module(new FMAC)
      })

}
