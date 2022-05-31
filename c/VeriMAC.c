#include <machine/patmos.h>
#include <machine/spm.h>
#include "include/bootable.h"

void tohex(int in, char * out)
{
    int pin = in;
    const char * hex = "0123456789ABCDEF";
    char * byte = out;
	byte[0] = hex[(pin>>12) & 0xF];
	byte[1] = hex[(pin>>8) & 0xF];
	byte[2] = hex[(pin>>4) & 0xF];
	byte[3] = hex[ pin     & 0xF];
	byte[4] = 32;
}


int main(){
	volatile _SPM int *uart_status = (volatile _SPM int *) 0xF0080000;
	volatile _SPM int *uart_data = (volatile _SPM int *) 0xF0080004;
	volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xF00b0000;
	volatile _IODEV int *filter_index = (volatile _IODEV int *) 0xF00b0008;
	volatile _IODEV int *filter_value = (volatile _IODEV int *) 0xF00b000C;
	// Pointer to the deadline device
    	volatile _IODEV int *dead_ptr = (volatile _IODEV int *) PATMOS_IO_DEADLINE;
    	int val;
	int packet;
	int frameLength = 0;
	char str[5];

	*filter_index = 18;
	*filter_value = 18;
	
	*dead_ptr = 50000;
    	val = *dead_ptr;
	
	*io_ptr = 1;
	
	for (;;) {
		
		frameLength = *io_ptr/2;
		//printf("Printing a frame of length: %d\n", frameLength );
		//*uart_data = ("Printing a frame of length: %d\n", frameLength);
		/*
		while ((*uart_status & 0x01) == 0) {
			;
		}
		*/

		for(int i = 0; i <= frameLength; i++){
			packet = *io_ptr;

			tohex(packet,str);

			*uart_data = str[2];
			*uart_data = str[3];
			*uart_data = str[4];
			*uart_data = str[0];
			*uart_data = str[1];
			*uart_data = str[4];
			while ((*uart_status & 0x01) == 0) {
			;
			}
		}
	}
}
