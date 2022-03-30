transcript on
if {[file exists rtl_work]} {
	vdel -lib rtl_work -all
}
vlib rtl_work
vmap work rtl_work

vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/build {/home/patmos/t-crest/patmos/hardware/build/Patmos.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/build {/home/patmos/t-crest/patmos/hardware/build/BlackBoxRom.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/eth_mac_1g_rgmii_fifo.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/eth_mac_1g_rgmii.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/eth_mac_1g.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/axis_gmii_rx.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/lfsr.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/axis_gmii_tx.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/rgmii_phy_if.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/oddr.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/ssio_ddr_in.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/rtl/iddr.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/lib/axis/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/lib/axis/rtl/axis_async_fifo_adapter.v}
vlog -vlog01compat -work work +incdir+/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/lib/axis/rtl {/home/patmos/t-crest/patmos/hardware/WiresharkMAC/fpga/lib/eth/lib/axis/rtl/axis_async_fifo.v}
vcom -93 -work work {/home/patmos/t-crest/patmos/hardware/vhdl/altera/cyc2_pll.vhd}
vcom -93 -work work {/home/patmos/t-crest/patmos/hardware/vhdl/patmos_de2-115.vhdl}

