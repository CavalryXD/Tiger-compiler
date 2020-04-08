#include <stdio.h>
#include <stdlib.h>

int main()
{
	// int a[10];
	int  a[];
	//  a = (int*)malloc(10 * sizeof (int));
	printf("%d\n", sizeof(a));
	return 0;
}