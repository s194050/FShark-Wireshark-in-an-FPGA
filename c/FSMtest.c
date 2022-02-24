#include <machine/patmos.h>
#include <stdio.h>

int main(){
	volatile _IODEV int *event_ptr = (volatile _IODEV int *) 0xf00c0000;
	char val;
	char val2;
	char val3;
	val = *event_ptr;
	
	//printf("Current ASCII value is: %c \n",val);
	
	*event_ptr = 1;
	//printf("-------\n");
	val2 = *event_ptr;
	//printf("Current ASCII value is: %c \n",val2);
	
	*event_ptr = 1;
	//printf("-------\n");
	val3 = *event_ptr;
	//printf("Current ASCII value is: %c \n",val3);
}
	
