"""Demonstrates how to construct and send raw Ethernet packets on the
network.
You probably need root privs to be able to bind to the network interface,
e.g.:
    $ sudo python sendeth.py
"""

from socket import *

def sendeth(src, dst, eth_type, payload, interface = "ens33"):
  """Send raw Ethernet packet on interface."""

  assert(len(src) == len(dst) == 6) # 48-bit ethernet addresses
  assert(len(eth_type) == 2) # 16-bit ethernet type

  s = socket(AF_PACKET, SOCK_RAW)

  # From the docs: "For raw packet
  # sockets the address is a tuple (ifname, proto [,pkttype [,hatype]])"
  s.bind((interface, 0))
  return s.send(src + dst + eth_type + payload)

if __name__ == "__main__":
  print("Sent %d-byte Ethernet packet on eth0" %
    sendeth("\x00\x0C\x29\x85\xA4\x2B",
            "\x00\x50\x56\xEF\x56\x57",
            "\x7A\x05",
            "\x00\x10\x11\x12\x13\x14\x15"))