#include <machine/patmos.h>
#include <machine/spm.h>

int main(){
    volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xF00b0000;
	volatile _IODEV int *filter_index = (volatile _IODEV int *) 0xF00b0008;
	volatile _IODEV int *filter_value = (volatile _IODEV int *) 0xF00b000C;
    // Pointer to the deadline device
    volatile _IODEV int *dead_ptr = (volatile _IODEV int *) PATMOS_IO_DEADLINE;
    int val;
    int packet = 0;

    *filter_index = 18;
	*filter_value = 18;
	
    *dead_ptr = 5000;
    val = *dead_ptr;

    *io_ptr = 1;


    for (;;) {
        packet = *io_ptr;
        }
}