#include <machine/patmos.h>
#include <stdio.h>

int main() {

	volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xf00b0000;
	int val1;
	int val2;
	
	val1 = *io_ptr;
	printf("Counting\n");
	val2 = *io_ptr;
	printf("Execution time is %d\n", val2-val1);
}
