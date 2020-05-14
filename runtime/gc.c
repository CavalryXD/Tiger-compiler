#include <stdio.h>
#include <stdlib.h>
#include <string.h>


static void Tiger_gc();
// The Gimple Garbage Collector.

// ===============================================================// 
// The Java Heap data structure.

/*   
      ----------------------------------------------------
      |                        |                         |
      ----------------------------------------------------
      ^\                      /^
      | \<~~~~~~~ size ~~~~~>/ |
    from                       to
 */
struct JavaHeap
{
  int size;         // in bytes, note that this is for semi-heap size
  char *from;       // the "from" space pointer
  char *fromFree;   // the next "free" space in the from space
  char *to;         // the "to" space pointer
  char *toStart;    // "start" address in the "to" space
  char *toNext;     // "next" free space pointer in the to space  "to"中当前空闲的地址
};

// The Java heap, which is initialized by the following
// "heap_init" function.
struct JavaHeap heap;

// Lab 4, exercise 10:
// Given the heap size (in bytes), allocate a Java heap
// in the C heap, initialize the relevant fields.
void Tiger_heap_init (int heapSize)
{
  // You should write 7 statement here:
  // #1: allocate a chunk of memory of size "heapSize" using "malloc"
  char * ptr = (char *)malloc(heapSize * sizeof(char));
  // #2: initialize the "size" field, note that "size" field
  // is for semi-heap, but "heapSize" is for the whole heap.
  heap.size = heapSize / 2;
  // #3: initialize the "from" field (with what value?)
  heap.from = ptr;
  // #4: initialize the "fromFree" field (with what value?)
  heap.fromFree = ptr;
  // #5: initialize the "to" field (with what value?)
  heap.to = ptr + heap.size;
  // #6: initizlize the "toStart" field with NULL;
  heap.toStart = (char*)heap.to;
  // #7: initialize the "toNext" field with NULL;
  heap.toNext = (char*)heap.to;
  printf("Java Initial finished...\n");
  printf("Heap size     : %d\n", heap.size);
  printf("Heap from     : %#x\n", heap.from);
  printf("Heap fromFree : %#x\n", heap.fromFree);
  printf("Heap to       : %#x\n", heap.to);
  printf("Heap toStart  : %#x\n", heap.toStart);
  printf("Heap toNext   : %#x\n", heap.toNext);  
  return;
}

// The "prev" pointer, pointing to the top frame on the GC stack. prev == %ebp
// (see part A of Lab 4)
// extern void* prev;
void *prev = 0;

// forward之后, 会修改GC后原来引用变量的指针, 使其指向新的对象

// ===============================================================// 
// Object Model And allocation


// Lab 4: exercise 11:
// "new" a new object, do necessary initializations, and
// return the pointer (reference).
/*    ----------------
      | vptr      ---|----> (points to the virtual method table)
      |--------------|
      | isObjOrArray | (0: for normal objects)
      |--------------|
      | length       | (this field should be empty for normal objects)
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| v_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | v_{size-1}   | /e
      ----------------/
*/
// Try to allocate an object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new (void *vtable, int size)
{
  // Your code here:
  if(heap.to - heap.fromFree < size)
  {
    printf("There is %d byte remained, but you need:%d\n", heap.to - heap.fromFree, size);
    Tiger_gc();
    if(heap.to - heap.fromFree < size)
    {
      printf("Error!! OutOfMemory\n");
      printf("After Tiger_GC there have %d byte, but expected : %d\n", heap.to - heap.fromFree, size);
      exit(1);
    }
  }
  printf("\n-------------- This is Tiger_new --------------\n");
  printf("malloc size : %d\n", size);
  char* ptr = heap.fromFree;
  memset(ptr, 0, size);
  *(ptr + 4) = 0;     // 代表是object
  *(ptr + 8) = size;     // 这里要置为size, 置0会在cheney()死循环
  *(ptr + 12) = 0;    // forwarding置为0 表示当前obj在From区域中

  heap.fromFree += size;
  *((int* )ptr) = (int*)vtable;
  printf("vtable's address is   =: %#x\n", (int*)(ptr));
  printf("isObj   = %d, address =: %#x\n", *(ptr + 4), ptr + 4);
  printf("length  = %d, address =: %#x\n", *(ptr + 8), ptr + 8);
  printf("forward = %d, address =: %#x\n", *(ptr + 12), ptr + 12);
  printf("\n-------------- malloc finished ------------------\n");
  return ptr;
}

// "new" an array of size "length", do necessary
// initializations. And each array comes with an
// extra "header" storing the array length and other information.
/*    ----------------
      | vptr         | (this field should be empty for an array)
      |--------------|
      | isObjOrArray | (1: for array)
      |--------------|
      | length       |
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| e_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | e_{length-1} | /e
      ----------------/
*/
// Try to allocate an array object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this array object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new_array (int length)
{
  // Your code here:
  int expectBytes = length * sizeof(int) + 16;
  if(heap.to - heap.fromFree < expectBytes)    // 16为头部长度
  {
    printf("There is %d byte remained, but you need:%d\n", heap.to - heap.fromFree, expectBytes);
    Tiger_gc();
    if(heap.to - heap.fromFree < expectBytes)
    {
      printf("Error!! OutOfMemory---Tiger_gc can not collecte enough space...\n");
      // GC后交换from和to指针
      printf("After Tiger_GC there have %d bytes, but expected : %d\n", (int*)(heap.from + heap.size - heap.fromFree), expectBytes);
      exit(1);
    }
  }
  printf("\n-------------- This is Tiger_new_array --------------\n");
  printf("malloc size : %d\n", expectBytes);
  char* ptr = heap.fromFree;
  memset(ptr, 0, expectBytes);
  *ptr = NULL;    // vptr置为NULL
  *(ptr + 4) = 1;
  *((int*)(ptr + 8)) = length;
  *(ptr + 12) = 0;
  heap.fromFree += expectBytes;

  printf("vtable's address is   =: %#x\n", ptr);
  printf("isObj   = %d, address =: %#x\n", *(ptr + 4), ptr + 4);
  printf("length  = %d, address =: %#x\n", *(ptr + 8), ptr + 8);
  printf("forward = %d, address =: %#x\n", *(ptr + 12), ptr + 12);
  printf("\n-------------- malloc finished --------------\n");
  return ptr;   
}

// ===============================================================// 
// The Gimple Garbage Collector

// Lab 4, exercise 12:
// A forwarding collector based-on Cheney's algorithm.

void Exchange()
{
  // 交换后from和to的打小关系从而就发生了变化
  char* swap = heap.from;
	heap.from = heap.toStart;
	//heap.to = (char*)heap.from + heap.size;
  heap.to = swap;   // change

	heap.fromFree = heap.toNext;
	heap.toStart = swap;
	heap.toNext = swap;
	printf("\n-------------- New Heap Info -----------\n");
	printf("heap from      : %#x\n", (int*)heap.from);
	printf("heap to        : %#x\n", (int*)heap.to);
	printf("heap fromFree  : %#x\n", (int*)heap.fromFree);
	printf("heap toNext    : %#x\n", (int*)heap.toNext);
	printf("\n");
}


int CalculateSize(void* temp)
{
  int size = 0;
  int* ObjAddr = *(int*)temp;
  printf("\n-------------- CalculateSize --------------\n");
  int isArray = *((char*)ObjAddr + 4);
  printf("isAarray is : %d\n", isArray);
  if(isArray)
  {
    // Array
    size = (*((char*)ObjAddr + 8)) * sizeof(int) + 16;
    printf("Alloc size is : %d\n", size);
  }
  else
  {
    // Obj
    int len = 0;
    int* vtable = *(int*)ObjAddr;
    char* classMap = *vtable;
    // GC_stack_frames 中的 第一个成员为local_gc_map的字符串, 其长度代表了有几个local fields
    len = strlen(classMap);
    printf("the number of reference type in class is %d\n", len);
    // 这里的size就代表了要给类的struct分配多少空间
    size = len * 4 + 16;
    printf("Alloc size is : %d\n", size);
    }
  printf("\n-------------- CalculateSize finished --------------\n");
  return size;
}

// 接口要求 temp指向对象模型的第一个字节
void* Forward(void *temp)
{
  // ObjAddr 所在位置就是类的struct结构体变量
  int* ObjAddr = *(int*)temp;
  void* newAdd = temp;
  printf("\n-------------- Forward start --------------\n");
  printf("heap toNext   : %#x\n", heap.toNext);
  printf("heap fromFree : %#x\n", heap.fromFree);
  if(ObjAddr < (heap.from + heap.size) && ObjAddr >= heap.from)
  {
    // 当目的地址在from区间里
    char* test = ObjAddr;
    printf("\n-------------- This is Forward() --------------\n");
    printf("ObjAddr is %#x\n", test);
    printf("isObj      = %d\n", *(test + 4));
    printf("length     = %d\n", *(test + 8));
    printf("forwarding = %#x\n", *(int*)(test + 12));

    void *forwarding = ((char *)ObjAddr + 12);

    if(*(int*)forwarding < (heap.to + heap.size) && *(int*)forwarding >= heap.to)
    {
      // forwarding在to区间里, 说明已经Forward
      printf("forwarding is already in tospace !!! : %#x\n", *(int*)forwarding);
      return forwarding;
    }
    else if((*(int*)forwarding < (heap.from + heap.size) && *(int*)forwarding >= heap.from) || *((int*)forwarding) == 0)
    {
      // forwarding在from区间里面 或者 forwarding为0代表不存在forwarding指针
      printf("forwarding is in From Space or forwarding == 0\n");
      // 因为没有给forwarding赋值，所以先用char*好输出0，不然是一大长串数
      newAdd = heap.toNext;
      *(int*)forwarding = (int*)newAdd;
      printf("new forwarding is : %#x\n", *(int*)forwarding);
      printf("Forward !!!!\n");

      // 得到size。有obj的size与array的size两种情况
      int size = CalculateSize(temp);
      // 开始forward 按字节forward
      for(int i = 0; i < size; i++)
      {
        *(char*)heap.toNext = *((char*)ObjAddr + i);
        heap.toNext = (char*)heap.toNext + 1;
      }
      printf("in Foward heap.toNext is : %#x\n", heap.toNext);
      return newAdd;
    }
    // forward finished...
    else
    {
      printf("Obj not exist !!!\n");
      return 0;
    }
  }
  else
  {
    // 当前obj在To区域中
    printf("No need forward !!!\n");
  }
  return temp;
}

void Cheney()
{
  printf("\n-------------- Cheney Start! --------------\n");
  char* toStart_temp = heap.toStart;
  // while(scan < next)  具体见<<虎书 13.3>>
  // 将 [ heap.toStart, heap.toNext ] 之间的所有的记录作为BFS的队列
  while(toStart_temp < heap.toNext)
  {
    printf("toStart_temp : %#x\n", toStart_temp);
    printf("heap.toNext  : %#x\n", heap.toNext);
    int* obj = (int*)toStart_temp;    // 重大BUG
    // 判断对象是什么类型。
    int isObj = (int)*((char*)obj + 4);
    int size = (int)*((char*)obj + 8);
    printf("szie = %d\n", size);
    if(isObj == 1)// is Array
    {
      printf("sizeof(Array) = %d\n", size * sizeof(int) + 16);
      toStart_temp = (char*)toStart_temp + size * sizeof(int) + 16;
    }
    else
    {
      printf("sizeof(Obj) = %d\n", size);    
      // 是Obj的话需要处理一下
      void* vptr_arg = *(int*)toStart_temp;
      char* class_gcMap = *(int*)vptr_arg;
      printf("map is : %s\n", (char*)class_gcMap);
      int classLocalCount = strlen(class_gcMap);
      if(classLocalCount > 0)
      {
        int* localAddress = (int*)((char*)toStart_temp + 16);
        for(int i = 0; i < classLocalCount; i++)
        {
            if(class_gcMap[i] == '1')
              Forward(localAddress);
            localAddress = (char*)localAddress + 4;
        }
      }
      toStart_temp = (char*)toStart_temp + size;
    }
  }
  printf("\n-------------- Cheney End! --------------\n");
}


void Tiger_gc ()
{
  // Your code here:
  // 这里的设计哲学是将父子关系的Obj尽可能在To空间挨得近一些
  printf("\n-------------- Tiger_GC start! --------------\n");

  // prev是针对Stack Frame进行的, 回溯所有函数调用栈
  while(prev != 0)
  {
    printf("\n-------------- This is a Frame --------------\n");
    char* arguments_gc_map = *((int*)((char *)prev + 4));   // 得到地址
    // 注意!!!!  这里的arguments_address是对函数中this指针再取地址后得到的指针
    int* arguments_address = (int*)((char*)prev + 8);   
    int locals_gc_map = *((char*)prev + 12);
    // prev -> arguments_address -> this -> Obj
    // 指针指向关系 : prev + 8 == arguments_address, *arguments_address = &this, **arguments_address = this
    printf("arguments_gc_map is         : \"%s\"\n", arguments_gc_map);
    printf("arguments_gc_map address is : %#x\n", *arguments_address);
    printf("locals_gc_map : there are %d local value\n", locals_gc_map);

    void* temp = 0;
    // arguments
    if(arguments_gc_map != 0)   // 0 = '\0'
    {
      printf("\n-------------- This is a arguments_gc_map --------------\n");
      int* addr = arguments_address;
      int len = strlen(arguments_gc_map);
      printf("arguments_gc_map length is : %d\n", len);

      for(int i = 0; i < len; i++)
      {
        if(arguments_gc_map[i] == '1')
        {
            temp = *((int*)addr);   // 得到第一个参数的指针
            printf("\nin arguments Obj address is : %#x\n", *(int*)temp);     
            // Forward
            temp = Forward(temp);
            printf("\n-------------- Forward finished --------------\n");
            addr = (char*)addr + 4;
        }
      }
    }
     // locals
    if(locals_gc_map != 0)
    {
      printf("\n-------------- This is a locals_gc_map --------------\n");
      int* localStart = (int*)((char*)prev + 16);
      int* localTemp = localStart;
      printf("there are %d locals\n", locals_gc_map);
      for(int j = 0; j < locals_gc_map; j++)
      {
        temp = localTemp;
        printf("\nLocals_Obj address is : %#x with %d bytes size\n", *(int*)temp, CalculateSize(temp));   
        // Forward
        temp = Forward(temp);
        printf("\n-------------- Forward finished -------------- \n");
        // 每次向下移动一次
        localTemp = (char*)localTemp + 4;     // 指针也是四个字节
      }
    }
    // 继续遍历上一个函数栈
    prev = (*(int*)prev);
    printf("\n-------------- frame finished --------------\n");
  }
  // prev遍历结束
  // 修改对象内部的指针
  Cheney();
  // 交换heap和Free
  Exchange();
  printf("now %d bytes heap is used\n", heap.fromFree - heap.from);   // debug
	return;
}

