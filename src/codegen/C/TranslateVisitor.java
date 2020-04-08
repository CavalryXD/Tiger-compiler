package codegen.C;

import java.util.LinkedList;

import codegen.C.Ast.Class;
import codegen.C.Ast.Class.ClassSingle;
import codegen.C.Ast.Dec;
import codegen.C.Ast.Exp;
import codegen.C.Ast.Exp.Call;
import codegen.C.Ast.Exp.Id;
import codegen.C.Ast.Exp.Lt;
import codegen.C.Ast.Exp.NewObject;
import codegen.C.Ast.Exp.Num;
import codegen.C.Ast.Exp.Sub;
import codegen.C.Ast.Exp.This;
import codegen.C.Ast.Exp.Times;
import codegen.C.Ast.MainMethod;
import codegen.C.Ast.MainMethod.MainMethodSingle;
import codegen.C.Ast.Method;
import codegen.C.Ast.Method.MethodSingle;
import codegen.C.Ast.Program;
import codegen.C.Ast.Program.ProgramSingle;
import codegen.C.Ast.Stm;
import codegen.C.Ast.Stm.Assign;
import codegen.C.Ast.Stm.If;
import codegen.C.Ast.Stm.Print;
import codegen.C.Ast.Type;
import codegen.C.Ast.Type.ClassType;
import codegen.C.Ast.Vtable;
import codegen.C.Ast.Vtable.VtableSingle;

// Given a Java ast, translate it into a C ast and outputs it.
// 仔细体会不同codegenerator中的visitor不同 细品
// ClassTable用于保存vtable和fields, 流程是:先根据java的ast来构造号table, 再对每个类查询table来构造好vtable和fields
public class TranslateVisitor implements ast.Visitor {
  public Program.T program;
  private ClassTable table;     // <class_name, class_binding>
  private String classId;       // 在Visit(Program p)中每visit一个class就会切换classid
  private Type.T type;          // type after translation
  private Dec.T dec;
  private Stm.T stm;
  private Exp.T exp;
  private Method.T method;
  private LinkedList<Dec.T> tmpVars;        // 指向class的struct指针集合, 局部变量local
  private LinkedList<Class.T> classes;
  private LinkedList<Vtable.T> vtables;
  private LinkedList<Method.T> methods;
  private MainMethod.T mainMethod;

  public TranslateVisitor() {
      this.table = new ClassTable();
      this.classId = null;
      this.type = null;
      this.dec = null;
      this.stm = null;
      this.exp = null;
      this.method = null;
      this.classes = new LinkedList<Class.T>();
      this.vtables = new LinkedList<Vtable.T>();
      this.methods = new LinkedList<Method.T>();
      this.mainMethod = null;
      this.program = null;
  }

  // //////////////////////////////////////////////////////
  //
  public String genId() {
      return util.Temp.next();
  }

  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(ast.Ast.Exp.Add e) {
      e.left.accept(this);
      Exp.T left = this.exp;
      e.right.accept(this);
      Exp.T right = this.exp;
      this.exp = new Exp.Add(left, right);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.And e) {
      e.left.accept(this);
      Exp.T left = this.exp;
      e.right.accept(this);
      Exp.T right = this.exp;
      this.exp = new Exp.And(left, right);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.ArraySelect e) {
      e.array.accept(this);
      Exp.T array = this.exp;
      e.index.accept(this);
      Exp.T index = this.exp;
      this.exp = new Exp.ArraySelect(array, index);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Call e) {
      e.exp.accept(this);
      String newid = this.genId();
      // 这里不同于Java, 可以直接利用对象来调用成员函数
      // C语言没有对象, 所以需要先声明一个指向struct ClassId的指针, 再根据结构体中指向Vtable的指针进行函数调用
      this.tmpVars.add(new Dec.DecSingle(new Type.ClassType(e.type), newid));   // todo
      Exp.T exp = this.exp;
      LinkedList<Exp.T> args = new LinkedList<Exp.T>();
      for (ast.Ast.Exp.T x : e.args) {
          x.accept(this);
          args.add(this.exp);
      }
      this.exp = new Call(newid, exp, e.id, args);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.False e) {
      this.exp = new Num(0);    // false == 0
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Id e) {
      this.exp = new Id(e.id);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Length e) {
      e.array.accept(this);
      Exp.T array = this.exp;
      this.exp = new Exp.Length(array);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Lt e) {
      e.left.accept(this);
      Exp.T left = this.exp;
      e.right.accept(this);
      Exp.T right = this.exp;
      this.exp = new Lt(left, right);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.NewIntArray e) {
      // new int [e]
      e.exp.accept(this);
      Exp.T tmp = this.exp;
      this.exp = new Exp.NewIntArray(tmp);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.NewObject e) {
      this.exp = new NewObject(e.id);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Not e) {
      e.exp.accept(this);
      Exp.T tmp = this.exp;
      this.exp = new Exp.Not(tmp);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Num e) {
      this.exp = new Num(e.num);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Sub e) {
      e.left.accept(this);
      Exp.T left = this.exp;
      e.right.accept(this);
      Exp.T right = this.exp;
      this.exp = new Sub(left, right);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.This e) {
      this.exp = new This();
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.Times e) {
      e.left.accept(this);
      Exp.T left = this.exp;
      e.right.accept(this);
      Exp.T right = this.exp;
      this.exp = new Times(left, right);
      return;
  }

  @Override
  public void visit(ast.Ast.Exp.True e) {
      this.exp = new Num(1);
  }

  // //////////////////////////////////////////////
  // statements
  @Override
  public void visit(ast.Ast.Stm.Assign s) {
      s.exp.accept(this);
      this.stm = new Assign(s.id, this.exp);
      return;
  }

  @Override
  public void visit(ast.Ast.Stm.AssignArray s) {
      s.index.accept(this);
      Exp.T index = this.exp;
      s.exp.accept(this);
      this.stm = new Stm.AssignArray(s.id, index, this.exp);
      return;
  }

  @Override
  public void visit(ast.Ast.Stm.Block s) {
      LinkedList<Stm.T> tmp = new LinkedList<>();
      for (ast.Ast.Stm.T stm : s.stms) {
          stm.accept(this);
          tmp.add(this.stm);
      }
      this.stm = new Stm.Block(tmp);
      return;
  }

  @Override
  public void visit(ast.Ast.Stm.If s) {
      s.condition.accept(this);
      Exp.T condition = this.exp;
      s.thenn.accept(this);
      Stm.T thenn = this.stm;
      s.elsee.accept(this);
      Stm.T elsee = this.stm;
      this.stm = new If(condition, thenn, elsee);
      return;
  }

  @Override
  public void visit(ast.Ast.Stm.Print s) {
      s.exp.accept(this);
      this.stm = new Print(this.exp);
      return;
  }

  @Override
  public void visit(ast.Ast.Stm.While s) {
      s.condition.accept(this);
      Exp.T codition = this.exp;
      s.body.accept(this);
      Stm.T body = this.stm;
      this.stm = new Stm.While(codition, body);
      return;
  }

  // ///////////////////////////////////////////
  // type
  @Override
  public void visit(ast.Ast.Type.Boolean t) {
      this.type = new Type.Int();   // C语言中bool == int
  }

  @Override
  public void visit(ast.Ast.Type.ClassType t) {
      this.type = new Type.ClassType(t.id);
  }

  @Override
  public void visit(ast.Ast.Type.Int t) {
      this.type = new Type.Int();
  }

  @Override
  public void visit(ast.Ast.Type.IntArray t) {
      this.type = new Type.IntArray();
  }

  // ////////////////////////////////////////////////
  // dec
  @Override
  public void visit(ast.Ast.Dec.DecSingle d) {
      d.type.accept(this);
      this.dec = new codegen.C.Ast.Dec.DecSingle(this.type, d.id);
      return;
  }

  // method
  @Override
  public void visit(ast.Ast.Method.MethodSingle m) {
      this.tmpVars = new LinkedList<Dec.T>();
      m.retType.accept(this);
      Type.T newRetType = this.type;
      LinkedList<Dec.T> newFormals = new LinkedList<Dec.T>();
      newFormals.add(new Dec.DecSingle(
              new ClassType(this.classId), "this"));    // todo 为了在C中实现Java中的this, 所以在参数中添加this声明
      for (ast.Ast.Dec.T d : m.formals) {
          d.accept(this);
          newFormals.add(this.dec);
      }
      LinkedList<Dec.T> locals = new LinkedList<Dec.T>();
      for (ast.Ast.Dec.T d : m.locals) {
          d.accept(this);
          locals.add(this.dec);
      }
      LinkedList<Stm.T> newStm = new LinkedList<Stm.T>();
      for (ast.Ast.Stm.T s : m.stms) {
          s.accept(this);
          newStm.add(this.stm);
      }
      m.retExp.accept(this);
      Exp.T retExp = this.exp;
      // 将对自己的this的声明加入到变量声明  
      // 这里不同于Java, 可以直接利用对象来调用成员函数
      // C语言没有对象, 所以需要先声明一个指向struct ClassId的指针, 再根据结构体中指向Vtable的指针进行函数调用
      for (Dec.T dec : this.tmpVars) {
          locals.add(dec);//在声明的最后，补上需要的声明，比如call调用产生的类名。
      }
      this.method = new MethodSingle(newRetType, this.classId, m.id,
              newFormals, locals, newStm, retExp);
      return;
  }

  // class
  @Override
  public void visit(ast.Ast.Class.ClassSingle c) {
      ClassBinding cb = this.table.get(c.id);
      this.classes.add(new ClassSingle(c.id, cb.fields));
      this.vtables.add(new VtableSingle(c.id, cb.methods));
      this.classId = c.id;
      for (ast.Ast.Method.T m : c.methods) {
          m.accept(this);
          this.methods.add(this.method);
      }
      return;
  }

  // main class
  @Override
  public void visit(ast.Ast.MainClass.MainClassSingle c) {
      ClassBinding cb = this.table.get(c.id);
      Class.T newc = new ClassSingle(c.id, cb.fields);
      this.classes.add(newc);
      this.vtables.add(new VtableSingle(c.id, cb.methods));

      this.tmpVars = new LinkedList<Dec.T>();

      c.stm.accept(this);
      // todo
      MainMethod.T mthd = new MainMethodSingle(
              this.tmpVars, this.stm);
      this.mainMethod = mthd;
      return;
  }

  // /////////////////////////////////////////////////////
  // the first pass
  public void scanMain(ast.Ast.MainClass.T m) {
      this.table.init(((ast.Ast.MainClass.MainClassSingle) m).id, null);
      // this is a special hacking in that we don't want to
      // enter "main" into the table.
      return;
  }

  public void scanClasses(java.util.LinkedList<ast.Ast.Class.T> cs) {
      // put empty chuncks into the table
      // 初始化classTable，只填入extends信息。
      for (ast.Ast.Class.T c : cs) {
          ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
          this.table.init(cc.id, cc.extendss);
      }

      // put class fields and methods into the table
      // 再次遍历java的class表
      for (ast.Ast.Class.T c : cs) {
          ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
          LinkedList<Dec.T> newDecs = new LinkedList<Dec.T>();
          for (ast.Ast.Dec.T dec : cc.decs) {
              dec.accept(this);
              newDecs.add(this.dec);
          }
          this.table.initDecs(cc.id, newDecs);

          // all methods
          java.util.LinkedList<ast.Ast.Method.T> methods = cc.methods;
          for (ast.Ast.Method.T mthd : methods) {
              ast.Ast.Method.MethodSingle m = (ast.Ast.Method.MethodSingle) mthd;
              LinkedList<Dec.T> newArgs = new LinkedList<Dec.T>();
              newArgs.add(new Dec.DecSingle(
                new ClassType(cc.id), "this"));     // 重点！！也是放了一个自己所在类的类型
              for (ast.Ast.Dec.T arg : m.formals) {
                  arg.accept(this);
                  newArgs.add(this.dec);
              }
              m.retType.accept(this);
              Type.T newRet = this.type;
              this.table.initMethod(cc.id, newRet, newArgs, m.id);
          }
      }

      // calculate all inheritance information
      for (ast.Ast.Class.T c : cs) {
          ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
          this.table.inherit(cc.id);
      }
  }

  public void scanProgram(ast.Ast.Program.T p) {
      ast.Ast.Program.ProgramSingle pp = (ast.Ast.Program.ProgramSingle) p;
      scanMain(pp.mainClass);
      scanClasses(pp.classes);
      return;
  }

  // end of the first pass
  // ////////////////////////////////////////////////////

  // program
  @Override
  public void visit(ast.Ast.Program.ProgramSingle p) {
      // The first pass is to scan the whole program "p", and
      // to collect all information of inheritance.
      scanProgram(p);

      // do translations
      p.mainClass.accept(this);
      for (ast.Ast.Class.T classs : p.classes) {
          classs.accept(this);
      }
      this.program = new ProgramSingle(this.classes, this.vtables,
              this.methods, this.mainMethod);
      return;
  }
}
