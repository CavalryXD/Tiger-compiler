package ast;

import java.util.LinkedList;

public class Ast {

    // ///////////////////////////////////////////////////////////
    // type
    public static class Type {
        public static abstract class T implements ast.Acceptable {
            // boolean: -1
            // int: 0
            // int[]: 1
            // class: 2
            // Such that one can easily tell who is who
            public abstract int getNum();

            public int lineNum;   // 新添加lineNum, 为了以后AST中错误定位
        }

        // boolean
        public static class Boolean extends T {
            public Boolean() {
            }

            @Override
            public String toString() {
                return "@boolean";
            }

            @Override
            public int getNum() {
                return -1;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

        // class
        public static class ClassType extends T {
            public String id;

            public ClassType(String id) {
                this.id = id;
            }
            // Type类型为class的才有lineNum
            public ClassType(String id, int line) {
                this.id = id;
                this.lineNum = line;
            }

            @Override
            public String toString() {
                return this.id;
            }

            @Override
            public int getNum() {
                return 2;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

        // int
        public static class Int extends T {
            public Int() {
            }

            @Override
            public String toString() {
                return "@int";
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }

            @Override
            public int getNum() {
                return 0;
            }
        }

        // int[]
        public static class IntArray extends T {
            public IntArray() {
            }

            @Override
            public String toString() {
                return "@int[]";
            }

            @Override
            public int getNum() {
                return 1;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }

    }

    // ///////////////////////////////////////////////////
    // dec
    public static class Dec {
        public static abstract class T implements ast.Acceptable {
            public int lineNum;
        }
        // Dec这里不需要构造lineNum是因为如果Dec出错一定会在parser阶段就报错了, 且AST不会生成
        // 而type的不合法会在语义分析阶段根据id的token类中的lineNum报错

        public static class DecSingle extends T {
            public Type.T type;
            public String id;
            public boolean isField;    // id是否是类变量
            public boolean isUsed;      // 记录在方法中是否被使用过

            public DecSingle(Type.T type, String id) {
                this.type = type;
                this.id = id;
                isUsed = true;
            }
            public DecSingle(Type.T type, String id, int line) {
                this.type = type;
                this.id = id;
                isUsed = true;
                this.lineNum = line;
            }
            public DecSingle(Type.T type, String id, boolean isField, int line) {
                this.type = type;
                this.id = id;
                this.isField = isField;
                isUsed = true;
                this.lineNum = line;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }
    }

    // /////////////////////////////////////////////////////////
    // expression
    public static class Exp {
        public static abstract class T implements ast.Acceptable {
            public int lineNum;
        }

        // +
        public static class Add extends T {
            public T left;
            public T right;

            public Add(T left, T right) {
                this.left = left;
                this.right = right;
            }

            public Add(T left, T right, int line) {
                this.left = left;
                this.right = right;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // and
        public static class And extends T {
            public T left;
            public T right;

            public And(T left, T right) {
                this.left = left;
                this.right = right;
            }

            public And(T left, T right, int line) {
                this.left = left;
                this.right = right;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // ArraySelect
        public static class ArraySelect extends T {
            public T array;
            public T index;

            public ArraySelect(T array, T index) {
                this.array = array;
                this.index = index;
            }

            public ArraySelect(T array, T index, int line) {
                this.array = array;
                this.index = index;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // Call
        public static class Call extends T {
          public T exp;   // 是哪个对象调用的
          public String id;   // 方法名
          public java.util.LinkedList<T> args;  // 函数调用参数列表
          public String type; // type of first field "exp" 即调用方法的对象的类型, 在Elab中用到
          public java.util.LinkedList<Type.T> at; // arg's type
          public Type.T rt;   // 返回值类型
          public Call(T exp, String id, java.util.LinkedList<T> args) {
              this.exp = exp;
              this.id = id;
              this.args = args;
              this.type = null;
          }

          public Call(T exp, String id, java.util.LinkedList<T> args, int line) {
              this.exp = exp;
              this.id = id;
              this.args = args;
              this.type = null;
              this.lineNum = line;
          }

          public Call(
            // 必须要有，在opt时会用到
            T exp, String id, java.util.LinkedList<T> args,
            String type, java.util.LinkedList<Type.T> at, Type.T rt)
            {
                this.exp = exp;
                this.id = id;
                this.args = args;
                this.type = type;
                this.at = at;
                this.rt = rt;
            }
            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // False
        public static class False extends T {
            public False() {
            }

            public False(int line) {
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // Id
        public static class Id extends T {
            public String id; // name of the id
            public Type.T type; // type of the id
            public boolean isField; // whether or not this is a class field, elab确定
            // isField 表示该变量是方法内的id还是类内成员
            public Id(String id) {
                this.id = id;
                this.type = null;
                this.isField = false;
            }

            public Id(String id, int line) {
                this.id = id;
                this.type = null;
                this.isField = false;
                this.lineNum = line;
            }
            public Id(String id, Type.T type, boolean isField) {
                this.id = id;
                this.type = type;
                this.isField = isField;
            }
            public Id(String id, Type.T type, boolean isField,int line)
            {
              this.id = id;
              this.type = type;
              this.isField = isField;
              this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // length
        public static class Length extends T {
            public T array;

            public Length(T array) {
                this.array = array;
            }

            public Length(T array, int line) {
                this.array = array;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // <
        public static class Lt extends T {
            public T left;
            public T right;

            public Lt(T left, T right) {
                this.left = left;
                this.right = right;
            }

            public Lt(T left, T right, int line) {
                this.left = left;
                this.right = right;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // new int [e]
        public static class NewIntArray extends T {
            public T exp;

            public NewIntArray(T exp) {
                this.exp = exp;
            }

            public NewIntArray(T exp, int line) {
                this.exp = exp;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // new A();
        public static class NewObject extends T {
            public String id;

            public NewObject(String id) {
                this.id = id;
            }

            public NewObject(String id, int line) {
                this.id = id;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // !
        public static class Not extends T {
            public T exp;

            public Not(T exp) {
                this.exp = exp;
            }

            public Not(T exp, int line) {
                this.exp = exp;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // number
        public static class Num extends T {
            public int num;

            public Num(int num) {
                this.num = num;
            }

            public Num(int num, int line) {
                this.num = num;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // -
        public static class Sub extends T {
            public T left;
            public T right;

            public Sub(T left, T right) {
                this.left = left;
                this.right = right;
            }

            public Sub(T left, T right, int line) {
                this.left = left;
                this.right = right;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // this
        public static class This extends T {
            public This() {
            }

            public This(int line) {
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // *
        public static class Times extends T {
            public T left;
            public T right;

            public Times(T left, T right) {
                this.left = left;
                this.right = right;
            }

            public Times(T left, T right, int line) {
                this.left = left;
                this.right = right;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

        // True
        public static class True extends T {
            public True() {
            }

            public True(int line) {
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
                return;
            }
        }

    }// end of expression

    // /////////////////////////////////////////////////////////
    // statement
    public static class Stm {
        public static abstract class T implements ast.Acceptable {
            public int lineNum;
        }

        // assign
        public static class Assign extends T {
            public String id;
            public Exp.T exp;
            public Type.T type; // type of the id
            public boolean isField;     // elab确定
            
            public Assign(String id, Exp.T exp) {
                this.id = id;
                this.exp = exp;
                this.type = null;
                isField = false;
            }

            public Assign(String id, Exp.T exp, int line) {
                this.id = id;
                this.exp = exp;
                this.type = null;
                this.lineNum = line;
                isField = false;
            }

            // 在opt中会用
			public Assign(String id, ast.Ast.Exp.T exp, ast.Ast.Type.T type,
            boolean isField, int line)
            {
                this.id = id;
                this.exp = exp;
                this.type = type;
                this.isField = isField;
                this.lineNum = line;
            }
            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

        // assign-array
        public static class AssignArray extends T {
            public String id;
            public Exp.T index;
            public Exp.T exp;
            public Type.T type; // type of the id
            public boolean isField;

            public AssignArray(String id, Exp.T index, Exp.T exp) {
                this.id = id;
                this.index = index;
                this.exp = exp;
                isField = false;
                this.type = new ast.Ast.Type.IntArray();
            }

            public AssignArray(String id, Exp.T index, Exp.T exp, int line) {
                this.id = id;
                this.index = index;
                this.exp = exp;
                this.lineNum = line;
                isField = false;
                this.type = new ast.Ast.Type.IntArray();
            }
			// 在opt中会用
			public AssignArray(String id, ast.Ast.Exp.T index,
					ast.Ast.Exp.T exp, ast.Ast.Type.T type, boolean isField,
					int line)
			{
				this.id = id;
				this.index = index;
				this.exp = exp;
				this.type = type;
				this.isField = isField;
				this.lineNum = line;
			}

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

        // block
        public static class Block extends T {
            public java.util.LinkedList<T> stms;

            public Block(java.util.LinkedList<T> stms) {
                this.stms = stms;
            }
            public Block(java.util.LinkedList<T> stms, int line) {
                this.stms = stms;
                this.lineNum = line;
            }
            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

        // if
        public static class If extends T {
            public Exp.T condition;
            public T thenn;
            public T elsee;

            public If(Exp.T condition, T thenn, T elsee) {
                this.condition = condition;
                this.thenn = thenn;
                this.elsee = elsee;
            }

            public If(Exp.T condition, T thenn, T elsee, int line) {
                this.condition = condition;
                this.thenn = thenn;
                this.elsee = elsee;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

        // Print
        public static class Print extends T {
            public Exp.T exp;

            public Print(Exp.T exp) {
                this.exp = exp;
            }

            public Print(Exp.T exp, int line) {
                this.exp = exp;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

        // while
        public static class While extends T {
            public Exp.T condition;
            public T body;

            public While(Exp.T condition, T body) {
                this.condition = condition;
                this.body = body;
            }

            public While(Exp.T condition, T body, int line) {
                this.condition = condition;
                this.body = body;
                this.lineNum = line;
            }

            @Override
            public void accept(ast.Visitor v) {
                v.visit(this);
            }
        }

    }// end of statement

    // /////////////////////////////////////////////////////////
    // method
    public static class Method {
        public static abstract class T implements ast.Acceptable {
            public int lineNum;
        }

        public static class MethodSingle extends T {
            public Type.T retType;
            public String id;
            public LinkedList<Dec.T> formals;
            public LinkedList<Dec.T> locals;
            public LinkedList<Stm.T> stms;
            public Exp.T retExp;

            public MethodSingle(Type.T retType, String id,
                                LinkedList<Dec.T> formals, LinkedList<Dec.T> locals,
                                LinkedList<Stm.T> stms, Exp.T retExp) {
                this.retType = retType;
                this.id = id;
                this.formals = formals;
                this.locals = locals;
                this.stms = stms;
                this.retExp = retExp;
            }

            public MethodSingle(Type.T retType, String id,
                                LinkedList<Dec.T> formals, LinkedList<Dec.T> locals,
                                LinkedList<Stm.T> stms, Exp.T retExp, int line) {
                this.retType = retType;
                this.id = id;
                this.formals = formals;
                this.locals = locals;
                this.stms = stms;
                this.retExp = retExp;
                this.lineNum = line;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }
    }

    // class
    public static class Class {
        public static abstract class T implements ast.Acceptable {
        }
        // 类声明不需要lineNum, 是因为类名错误会在parser阶段识别出来
        public static class ClassSingle extends T {
            public String id;
            public String extendss; // null for non-existing "extends"
            public java.util.LinkedList<Dec.T> decs;
            public java.util.LinkedList<ast.Ast.Method.T> methods;

            public ClassSingle(String id, String extendss,
                               java.util.LinkedList<Dec.T> decs,
                               java.util.LinkedList<ast.Ast.Method.T> methods) {
                this.id = id;
                this.extendss = extendss;
                this.decs = decs;
                this.methods = methods;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
            }
        }
    }

    // main class
    public static class MainClass {
        public static abstract class T implements ast.Acceptable {
        }

        public static class MainClassSingle extends T {
            public String id;
            public String arg;
            public Stm.T stm;

            public MainClassSingle(String id, String arg, Stm.T stm) {
                this.id = id;
                this.arg = arg;
                this.stm = stm;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
                return;
            }
        }

    }

    // whole program
    public static class Program {
        public static abstract class T implements ast.Acceptable {
        }

        public static class ProgramSingle extends T {
            public MainClass.T mainClass;
            public LinkedList<Class.T> classes;

            public ProgramSingle(MainClass.T mainClass, LinkedList<Class.T> classes) {
                this.mainClass = mainClass;
                this.classes = classes;
            }

            @Override
            public void accept(Visitor v) {
                v.visit(this);
                return;
            }
        }

    }
}
