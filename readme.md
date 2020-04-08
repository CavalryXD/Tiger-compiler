## Tiger Compiler 

The Tiger compiler. Copyright (C) 2014-2020, SSE of USTC.

Tiger compiler is a miniJava compiler which use Recursive Descent to parse program source code to AST and can generate C code and Bytecode. And there is also a grabage collector embedded in compiler which uses Copying algorithm.

#### Getting started

type this command to get information in detail

```java
java -cp bin Tiger -help
```

For example, you can use this to compile miniJava to C code

```java
java -cp bin Tiger ./test/Sum-Infinite.java -codegen C
```