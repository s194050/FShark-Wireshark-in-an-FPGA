#include <machine/patmos.h>
#include <machine/spm.h>
#include "include/bootable.h"

int main(){
	volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xf00b0000;
	volatile _SPM int *uart_ptr = (volatile _IODEV int *) 0xf0080004;
	int i,j;
	
	
	for (;;) {
		for (i=2000; i!=0; --i)
			for (j=2000; j!=0; --j)
				*uart_ptr = *io_ptr;
	}
}
	
	
