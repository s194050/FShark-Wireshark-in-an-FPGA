#include <machine/spm.h>

int main() {
	volatile _SPM int *uart_status = (volatile _SPM int *) 0xF0080000;
	volatile _SPM int *uart_data = (volatile _SPM int *) 0xF0080004;
	int i,j;
	for (;;) {
		
		*uart_data = '1';
		
		for (i=10000; i!=0; --i)
			for (j=10000; j!=0; --j){}
			
		for (i=10000; i!=0; --i)
			for (j=10000; j!=0; --j){}
		if(*uart_status == 0){
			*uart_data = '0';
		}
	}
}
