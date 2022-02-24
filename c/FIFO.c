#include <machine/patmos.h>
#include <stdio.h>

int main(){
	volatile _IODEV int *io_ptr = (volatile _IODEV int *) 0xf00b0000;
	int val;
	int val1;
	int val2;
	int val3;
	val = *io_ptr;
	val1 = *io_ptr;
	val2 = *io_ptr;
	val3 = *io_ptr;
	printf("%c, %c, %c, %c\n",val,val1,val2,val3);
}
	
	
