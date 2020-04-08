package codegen.bytecode;

import java.util.Hashtable;
import java.util.LinkedList;

import codegen.C.Ast.Exp;
import codegen.bytecode.Ast.Class;
import codegen.bytecode.Ast.Class.ClassSingle;
import codegen.bytecode.Ast.Dec;
import codegen.bytecode.Ast.Dec.DecSingle;
import codegen.bytecode.Ast.MainClass;
import codegen.bytecode.Ast.MainClass.MainClassSingle;
import codegen.bytecode.Ast.Method;
import codegen.bytecode.Ast.Method.MethodSingle;
import codegen.bytecode.Ast.Program;
import codegen.bytecode.Ast.Program.ProgramSingle;
import codegen.bytecode.Ast.Stm;
import codegen.bytecode.Ast.Type;
import codegen.bytecode.Ast.Type.Int;
import codegen.bytecode.Ast.Stm.*;
import elaborator.ElaboratorVisitor;
import elaborator.MethodType;
import util.Label;

// Given a Java ast, translate it into Java bytecode.

public class TranslateVisitor implements ast.Visitor
{
  private String classId;// 记录正在Trans的类名
  private int index;
  private Hashtable<String, Integer> indexTable;// 传说中的frame
  // 0号位置不存放数据，保留。
  // 这个HashTable的内容与JVM中的frame是一一对应
  
  private Type.T type; // type after translation
  private Dec.T dec;
  private LinkedList<Stm.T> stms;
  private Method.T method;// 一个method拥有一个Stm链
  
  private Class.T classs;
  private MainClass.T mainClass;
  
  public Program.T program;

  public TranslateVisitor()
  {
    this.classId = null;
    this.indexTable = null;
    this.type = null;
    this.dec = null;
    this.stms = new LinkedList<Stm.T>();
    this.method = null;
    this.classs = null;
    this.mainClass = null;
    this.program = null;
  }

  private void emit(Stm.T s)
  {
    this.stms.add(s);
  }

  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(ast.Ast.Exp.Add e)
  {
	e.left.accept(this);
	e.right.accept(this);
	emit(new Iadd());
	return;
	  
  }

  @Override
  public void visit(ast.Ast.Exp.And e)
  {
	  e.left.accept(this);
	  e.right.accept(this);
	  emit(new Iand());
	  return;
	  
  }

  @Override//array[index]
  public void visit(ast.Ast.Exp.ArraySelect e)
  {
	  e.array.accept(this);//arrayref这一步会执行Aload或Iload把Id进栈
	  e.index.accept(this);//index
	  emit(new IAload());//retrieve integer from array
	  return;
  }

  @Override
  public void visit(ast.Ast.Exp.Call e)
  {
    e.exp.accept(this);
    for (ast.Ast.Exp.T x : e.args) {
      x.accept(this);
    }
    e.rt.accept(this);
    Type.T rt = this.type;
    // 参数的类型列表
    LinkedList<Type.T> at = new LinkedList<Type.T>();
    // 在Elab时将这里的类型全部变为了函数原型，因为在Tans C时不需要at这个字段
    for (ast.Ast.Type.T t : e.at) {
      t.accept(this);
      at.add(this.type);
    }
    emit(new Invokevirtual(e.id, e.type, at, rt));
    return;
  }

  @Override
  public void visit(ast.Ast.Exp.False e)
  {
	  emit(new Ldc(0));   // 强制转换
	  return;
  }

  @Override
  public void visit(ast.Ast.Exp.Id e)
  {
	  if(!e.isField)
	  {
      int index = this.indexTable.get(e.id);
      ast.Ast.Type.T type = e.type;
      if (type.getNum() > 0)// a reference 数组
        emit(new Aload(index));   // retrieve object reference from local variable
      else
        emit(new Iload(index));   // integer
      // but what about this is a field?
      return;
	  }
	  else
	  {
		  emit(new Aload(0));   // 当前类的this引用会在0号位置
		  // 这一步是为了在栈里给Getfield创造需要的数据
		  e.type.accept(this);
		  emit(new codegen.bytecode.Ast.Stm.Getfield(
				  this.classId,e.id,this.type));
	  }
  }
    /*----------------------
     * Before    |  After   |
     * ---------------------
     * arrayref  |  length  |
     * ---------------------*/
    @Override //array.length
    public void visit(ast.Ast.Exp.Length e)
    {
      e.array.accept(this);
      emit(new ArrayLength());
    }

    /*
    * 
    */
    // condition一般是 Lt, 当结果为true时, 栈中会压入1, 反之0
    @Override
    public void visit(ast.Ast.Exp.Lt e)
    {
      Label tl = new Label();   // true
      Label fl = new Label();   // false
      Label el = new Label();   // exit
      e.left.accept(this);
      e.right.accept(this);     // 两个exp入栈
      emit(new Ificmplt(tl));   // 比较，成功时goto t1，不成功时返回继续执行，也就是执行了f1
      emit(new LabelJ(fl));
      emit(new Ldc(0));         // 0 == false  ?
      emit(new Goto(el));
      emit(new LabelJ(tl));
      emit(new Ldc(1));         // 1 == true
      emit(new Goto(el));
      emit(new LabelJ(el));     //作为退出的Lable,一定要在最后
      return;
    }

  @Override // new int[index]
  public void visit(ast.Ast.Exp.NewIntArray e)
  {
	  e.exp.accept(this);
	  emit(new NewIntArray());
	  return;
  }

  @Override
  public void visit(ast.Ast.Exp.NewObject e)
  {
    emit(new New(e.id));
    return;
  }

  // 
  @Override
  public void visit(ast.Ast.Exp.Not e)
  {
	  Label tl = new Label(), el = new Label();
		e.exp.accept(this);
		this.emit(new Ifne(tl));
		
		this.emit(new Ldc(1));      
		this.emit(new Goto(el));
		
		this.emit(new LabelJ(tl));
		this.emit(new Ldc(0));      // 当Ifne为true时, 说明原来的表达式为0
		
		this.emit(new LabelJ(el));
		return;
  }

  @Override
  public void visit(ast.Ast.Exp.Num e)
  {
    emit(new Ldc(e.num));
    return;
  }

  @Override
  public void visit(ast.Ast.Exp.Sub e)
  {
    e.left.accept(this);
    e.right.accept(this);
    emit(new Isub());
    return;
  }

  @Override
  public void visit(ast.Ast.Exp.This e)
  {
    emit(new Aload(0));     // 第0号变量即为this指针
    return;
  }

  @Override
  public void visit(ast.Ast.Exp.Times e)
  {
    e.left.accept(this);
    e.right.accept(this);
    emit(new Imul());
    return;
  }

  @Override
  public void visit(ast.Ast.Exp.True e)
  {
	  emit(new Ldc(1));
	  return;
  }

  // ///////////////////////////////////////////////////
  // statements
  @Override
  public void visit(ast.Ast.Stm.Assign s)
  {
	  // 同样要特殊处理id
	  ast.Ast.Exp.Id id = new ast.Ast.Exp.Id(s.id, s.type,s.isField);
	  if(!id.isField)
	  {
      s.exp.accept(this);
		  int index = this.indexTable.get(s.id);
		  ast.Ast.Type.T type = s.type;
		  if (type.getNum() > 0)
			  emit(new Astore(index));
		  else
        emit(new Istore(index));  // store in integer array
	  }
	  else
	  { 
		  emit(new Aload(0));     // 赋值的目标先进栈 
		  s.exp.accept(this);     // value再进栈
		  s.type.accept(this);
		  emit(new Putfield(this.classId,s.id,this.type));
	  }
	  return ;
  }

  @Override // id[exp]=exp
  public void visit(ast.Ast.Stm.AssignArray s)
  {
    // 需要特殊对待s.id
	  ast.Ast.Exp.Id id = new ast.Ast.Exp.Id(s.id, s.type, s.isField);
	  
	  id.accept(this);        // objref,index,value依次进栈
	  s.index.accept(this);
	  s.exp.accept(this);
    emit(new IAstore());    // store in integer array
    return ;
  }

  @Override
  public void visit(ast.Ast.Stm.Block s)
  {
	  for(ast.Ast.Stm.T t : s.stms)
	  {
		  t.accept(this);
	  }
  }

  @Override
  public void visit(ast.Ast.Stm.If s)
  {
    // true, false, end
    Label tl = new Label(), fl = new Label(), el = new Label();
    s.condition.accept(this);   // 现将condition的结果进栈

    // condition一般是 Lt, 当结果为true时, 栈中会压入1, 反之0
    emit(new Ifne(tl));
    emit(new LabelJ(fl));
    s.elsee.accept(this);
    emit(new Goto(el));
    emit(new LabelJ(tl));
    s.thenn.accept(this);
    emit(new Goto(el));
    emit(new LabelJ(el));
    return;
  }

  @Override
  public void visit(ast.Ast.Stm.Print s)
  {
    s.exp.accept(this);
    emit(new Print());
    return;
  }

  @Override
  public void visit(ast.Ast.Stm.While s)
  {
	  Label start = new Label();
	  Label t1 = new Label();
	  Label f1 = new Label();
	  Label e1 = new Label();
	  
	  emit(new LabelJ(start));
	  s.condition.accept(this);
	  emit(new Ifne(t1));
	  
	  emit(new LabelJ(f1));
	  emit(new Goto(e1));
	  
	  emit(new LabelJ(t1));
	  s.body.accept(this);
	  emit(new Goto(start));
	  
	  emit(new LabelJ(e1));
	  return ;
  }

  // type
  @Override
  public void visit(ast.Ast.Type.Boolean t)
  {
	  this.type = new codegen.bytecode.Ast.Type.Int();
	  return;
  }

  @Override
  public void visit(ast.Ast.Type.ClassType t)
  {
	  this.type = new codegen.bytecode.Ast.Type.ClassType(t.id);
	  return;
  }

  @Override
  public void visit(ast.Ast.Type.Int t)
  {
    this.type = new Int();
  }

  @Override
  public void visit(ast.Ast.Type.IntArray t)
  {
	  this.type = new codegen.bytecode.Ast.Type.IntArray();
	  return;
  }

  // dec
  @Override
  public void visit(ast.Ast.Dec.DecSingle d)
  {
    d.type.accept(this);
    this.dec = new DecSingle(this.type, d.id);
    // 将所有声明都放到indexTable当中,除了Class里面的声明
    if(d.isField)
      return;
    this.indexTable.put(d.id, index++);
    return;
  }

  // method
  @Override
  public void visit(ast.Ast.Method.MethodSingle m)
  {
    // record, in a hash table, each var's index
    // this index will be used in the load store operation
    this.index = 1;
    // 每个方法拥有一个HashTable, 每当调用一个新方法, 就开辟一段新栈帧, 从而局部变量表清空
    this.indexTable = new Hashtable<String, Integer>();
    // 返回类型
    m.retType.accept(this);
    Type.T newRetType = this.type;
    // 参数列表声明
    LinkedList<Dec.T> newFormals = new LinkedList<Dec.T>();
    for (ast.Ast.Dec.T d : m.formals) {
      d.accept(this);
      newFormals.add(this.dec);
    }
    // 局部变量声明
    LinkedList<Dec.T> locals = new java.util.LinkedList<Dec.T>();
    for (ast.Ast.Dec.T d : m.locals) {
      d.accept(this);
      locals.add(this.dec);
    }
    // 语句
    this.stms = new LinkedList<Stm.T>();
    for (ast.Ast.Stm.T s : m.stms) {
      s.accept(this);
    }

    // return statement is specially treated
    m.retExp.accept(this);    // 栈中压入返回值

    if (m.retType.getNum() > 0)// is reference
      emit(new Areturn());    // return from method with object reference result
    else
      emit(new Ireturn());    // return from method with integer result

    this.method = new MethodSingle(newRetType, m.id, this.classId, newFormals,
        locals, this.stms, 0, this.index);
    return;
  }

  // class
  @Override
  public void visit(ast.Ast.Class.ClassSingle c)
  {
    this.classId = c.id;
    // 遍历javaAST的一个类的decsList，显然，每一个类都应该构造一个新的decsList
    LinkedList<Dec.T> newDecs = new LinkedList<Dec.T>();
    for (ast.Ast.Dec.T dec : c.decs) {
      ast.Ast.Dec.DecSingle decc = (ast.Ast.Dec.DecSingle) dec;
      decc.accept(this);   // 不会将id放入HashTable！典型的为了副作用而调用。class不需要HashTable
      newDecs.add(this.dec);
    }
    // 遍历methodList
    LinkedList<Method.T> newMethods = new LinkedList<Method.T>();
    for (ast.Ast.Method.T m : c.methods) {
      m.accept(this);   // 在此之后this.method会变为new MethodSingle
      newMethods.add(this.method);
    }
    this.classs = new ClassSingle(c.id, c.extendss, newDecs, newMethods);
    return;
  }

  // main class
  @Override
  public void visit(ast.Ast.MainClass.MainClassSingle c)
  {
    c.stm.accept(this);
    this.mainClass = new MainClassSingle(c.id, c.arg, this.stms);
    //当处理完mainClass时，初始化一个新的Stm链
    this.stms = new LinkedList<Stm.T>();
    return;
  }

  // program
  @Override
  public void visit(ast.Ast.Program.ProgramSingle p)
  {
    // do translations
    p.mainClass.accept(this);

    LinkedList<Class.T> newClasses = new LinkedList<Class.T>();
    for (ast.Ast.Class.T classes : p.classes) {
      classes.accept(this);
      newClasses.add(this.classs);
    }
    this.program = new ProgramSingle(this.mainClass, newClasses);
    return;
  }
}
