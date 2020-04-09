## Tiger Compiler 

The Tiger compiler. Copyright (C) 2014-2020, SSE of USTC [Compiler Fall 2014 Lab](http://staff.ustc.edu.cn/~bjhua/courses/compiler/2014/)

Tiger compiler is a miniJava compiler which use Recursive Descent to parse program source code to AST and can generate C code and Bytecode. And there is also a Grabage Collector embedded in compiler which uses Copying algorithm.

#### Getting started

type this command to get information in detail

```java
java -cp bin Tiger -help
```

For example, you can use this to compile miniJava to Bytecode

```java
java -cp bin Tiger ./test/Sum-Infinite.java -codegen C
```

and you can use Jasmin to assemble Bytecode running on Java Virtual Machine

```java
jasmin -jar jasmin.jar (your filename).j
java (your filename)
```

#### Obejct Model

![call stack](https://https://github.com/CavalryXD/Tiger-compiler/raw/master/image/image-20200404130726398.png)

This figure is the typical memory layout of C call stack, unlike arguments, you cannot determine the layout of locals  variable on the call stack. In fact, GCC is free to determine any layout for all the method locals and it may put some or all of the locals into registers instead of on the call stack.

Thus, we have the following **object model**

1, represent locals in function

```c++
struct f_gc_frame{
void *prev;              		// dynamic chain, pointing to f's caller's GC frame
char *arguments_gc_map;  		//should be assigned the value of"f_arguments_gc_map"
int *arguments_base_address;     // address of the first argument
char *locals_gc_map;     		// should be assigned the value of "f_locals_gc_map"
struct A *local1;        		// remaining fields are method locals
int local2;			 
struct C *local3;
};
```

2, every function body will rewrite for the convenience of GC

```c++
Type function (D this, int arg1, A arg2, B arg3){
// put the GC stack frame onto the call stack
// note that this frame contains the three original locals in f
struct f_gc_frame frame;   
// push this frame onto the GC stack by setting up "prev"
frame.prev = prev;
prev = &frame; 
// setting up memory GC maps and corresponding base addresses
frame.arguments_gc_map = f_arguments_gc_map;
frame.arguments_base_address = &this;
frame.locals_gc_map = f_locals_gc_map;
// initialize locals of this method 
// statements should be rewritten apporpriately
prev = frame.prev	// 恢复现场
}
```

3, also change virtual table for OO language

```
struct A_virtual_table{
char *A_gc_map;   // class GC map
...               // virtual methods as before
};
```

4, unify the representations of array and object

```c++
struct A_class{
void *vptr;       // virtual method table pointer
int isObjOrArray; // is this a normal object or an (integer) array object?
unsigned length;  // array length
void *forwarding; // forwarding pointer, will be used by your Gimple GC
...;              // remainings are normal class or array fields
};
```



