#include <machine/patmos.h>
#include <machine/spm.h>
//#include "include/bootable.h"

int main(){
	volatile _SPM int *uart_status = (volatile _SPM int *) 0xF0080000;
	volatile _SPM int *uart_data = (volatile _SPM int *) 0xF0080004;
	volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xF00b0000;
  	int val;
	int i,j;
	
	
	for (;;) {
		*uart_data = *io_ptr;
		 
  		while ((*uart_status & 0x01) == 0) {
  		;
  		}	
	}
}
	
	
