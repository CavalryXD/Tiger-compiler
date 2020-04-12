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

