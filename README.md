This hardware uses Patmos
============

Patmos is a time-predictable VLIW processor.
Patmos is the processor for the T-CREST project.
See also: http://www.t-crest.org/ and http://patmos.compute.dtu.dk/

The Patmos [Reference Handbook](http://patmos.compute.dtu.dk/patmos_handbook.pdf)
contains build instructions in Section 5.

For questions and discussions join the Patmos mailing list at:
https://groups.yahoo.com/group/patmos-processor/

Link to T-CREST Patmos: https://github.com/t-crest/patmos

What is FShark?
===============

FShark is an open-source hardware implementation of Wireshark. This repository contains, all the specific files to making FShark work with Patmos, as well as all functionallity.

How to Install
==============
Follow the Patmos reference manual to set up the whole Patmos framework.
Afterwards proceed to navigate to the branch in this repostoriy named:
```
alpha-version
```
This branch is set up such that by copying all the files contained in the repository they replace and add the necessary files to run and implement FShark.

Running the Simulation
======================
After installing the first step is to run:
```
make tools
```
Which ensures that Patemu is updated with the correct simulation functionalities.
Afterwards run:
```
make hwsim BOOTAPP=basic
```
Which compiles and generates the Chisel Verilog code and compiles the emulation.
Afterwards run:
```
make comp APP=VeriMAC
```
Which compiles the program that allows for Linux terminal viewing of the good frames.
Finally to run the simulation run one of the following commands:
The first one loads a custom Ethernet frame for the RGMII simulation
```
patemu tmp/VeriMAC.elf -L filename.txt
```
The second one runs the default counter to 59 RGMII simulation
```
patemu tmp/VeriMAC.elf
```
Adding:
```
-v
```
After the .elf will enable Waveform generation which then can be viewed in GTKWave or a similar waveform viewer.
