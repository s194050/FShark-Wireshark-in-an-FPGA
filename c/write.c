#include <sys/socket.h>
#include <linux/if_ether.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <linux/if_packet.h>
#include <stdio.h>

int main() {
  printf("Transmitting...");
  int sockfd = socket (AF_PACKET, SOCK_RAW, htons (ETH_P_ALL));

  struct sockaddr_ll sll;
  sll.sll_family = AF_PACKET;
  sll.sll_protocol = htons (ETH_P_ALL);
  sll.sll_ifindex = 2;
  bind (sockfd, (struct sockaddr *) &sll, sizeof (sll));

  char header[] =
    { "\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0A\x0B\x0C\x0D\x0E\x0F\x10\x11\x12\x13\x14\x15\x16" };
  write (sockfd, (unsigned char *) &header, 26);

  sleep (10);

  return 0;
}