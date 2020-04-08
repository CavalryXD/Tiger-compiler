package slp;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashSet;

import slp.Slp.Exp;
import slp.Slp.Exp.Eseq;
import slp.Slp.Exp.Id;
import slp.Slp.Exp.Num;
import slp.Slp.Exp.Op;
import slp.Slp.ExpList;
import slp.Slp.Stm;
import util.Bug;
import util.Todo;
import control.Control;

public class Main
{
  // ///////////////////////////////////////////
  // maximum number of args
  private int maxArgsExp(Exp.T exp) {
      // new Todo();
      // 常规运算符返回0
      if (exp instanceof Exp.Num) {
          return 0;
      } else if (exp instanceof Exp.Id) {
          return 0;
      } else if (exp instanceof Exp.Op) {
          int n1 = maxArgsExp(((Op) exp).left);
          int n2 = maxArgsExp(((Op) exp).right);
          return n1 > n2 ? n1 : n2;
      } else if (exp instanceof Exp.Eseq) {
          int n1 = maxArgsStm(((Eseq) exp).stm);
          int n2 = maxArgsExp(((Eseq) exp).exp);
          return n1 > n2 ? n1 : n2;
      }
      return 0;
  }
  
  // 核心在于print的参数为ExpList, 所以要从这一点下手, 每多一个Exp时, 参数数量便加一
  // 也就相当于Exp最多为1
  private int maxArgsExpList(ExpList.T expList, int count) {
    if (expList instanceof ExpList.Pair) {
        return maxArgsExpList(((ExpList.Pair) expList).list, count + 1);
    } else if (expList instanceof ExpList.Last) {
        return count + 1;
    }
    return 0;
  }

  private int maxArgsStm(Stm.T stm)
  {
    if (stm instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) stm;
      int n1 = maxArgsStm(s.s1);
      int n2 = maxArgsStm(s.s2);

      return n1 >= n2 ? n1 : n2;
    } else if (stm instanceof Stm.Assign) {
      // new Todo();
      Stm.Assign a = (Stm.Assign) stm;
      Exp.T e = a.exp;
      return maxArgsExp(e);
    } else if (stm instanceof Stm.Print) {
      // new Todo();
      // 找出print的explist
      Stm.Print print = (Stm.Print) stm;
      return maxArgsExpList(print.explist, 0);
    } else
      new Bug();
    return 0;
  }

  // ////////////////////////////////////////
  // interpreter

  private int interpExp(Exp.T exp)
  {
    // new Todo();
    if(exp instanceof Exp.Id){
      Exp.Id exp_id = (Exp.Id) exp; 
      return Table.lookup(exp_id.id);
    }
    else if(exp instanceof Exp.Num){
      Exp.Num exp_num = (Exp.Num) exp; 
      return exp_num.num;
    }
    else if(exp instanceof Exp.Op){
      Exp.Op exp_op = (Exp.Op) exp;
      int a = interpExp(exp_op.left);
      int b = interpExp(exp_op.right);
      Exp.OP_T op = exp_op.op;

      switch(op)
      {
        case ADD: return a + b;
        case SUB: return a - b;
        case TIMES: return a * b;
        case DIVIDE: return a / b;
      }
    }
    else  // Eseq = (stm, exp)
    {
        Exp.Eseq exp_eseq = (Exp.Eseq) exp;
        interpStm(exp_eseq.stm);
        return interpExp(exp_eseq.exp);
    }
    return -1;
  }

  private void interpPrint(ExpList.T exp_list){
    if(exp_list instanceof ExpList.Pair){
        ExpList.Pair pair = (ExpList.Pair) exp_list;
        int val1 = interpExp(pair.exp);
        System.out.print(val1 + " ");
        interpPrint(pair.list);
    }
    else  // Last
    {
      ExpList.Last last = (ExpList.Last) exp_list;
      int val1 = interpExp(last.exp);
      System.out.println(val1);
    }
  }

  private void interpStm(Stm.T prog)
  {
    if (prog instanceof Stm.Compound) {
      // new Todo();
      Stm.Compound compound = (Stm.Compound) prog;
      interpStm(compound.s1);
      interpStm(compound.s2);
    } else if (prog instanceof Stm.Assign) {
      // new Todo();
      // Aiisgn文法 ->  id := exp
      Stm.Assign assign = (Stm.Assign) prog;
      int val = interpExp(assign.exp);
      Table.update(assign.id, val);
    } else if (prog instanceof Stm.Print) {
      // new Todo();
      Stm.Print print = (Stm.Print) prog;
      interpPrint(print.explist);
    } else
      new Bug();
  }

  // ////////////////////////////////////////
  // compile
  HashSet<String> ids;
  StringBuffer buf;

  private void emit(String s)
  {
    buf.append(s);
  }

  private void compileExp(Exp.T exp)
  {
    if (exp instanceof Id) {
      Exp.Id e = (Exp.Id) exp;
      String id = e.id;

      emit("\tmovl\t" + id + ", %eax\n");
    } else if (exp instanceof Num) {
      Exp.Num e = (Exp.Num) exp;
      int num = e.num;

      emit("\tmovl\t$" + num + ", %eax\n");
    } else if (exp instanceof Op) {
      Exp.Op e = (Exp.Op) exp;
      Exp.T left = e.left;
      Exp.T right = e.right;
      Exp.OP_T op = e.op;

      switch (op) {
      case ADD:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\taddl\t%edx, %eax\n");
        break;
      case SUB:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\tsubl\t%eax, %edx\n"); // edx -= eax
        emit("\tmovl\t%edx, %eax\n");
        break;
      case TIMES:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n");
        emit("\timul\t%edx\n");
        break;
      case DIVIDE:
        compileExp(left);
        emit("\tpushl\t%eax\n");
        compileExp(right);
        emit("\tpopl\t%edx\n"); // edx:eax 被除数
        emit("\tmovl\t%eax, %ecx\n");
        emit("\tmovl\t%edx, %eax\n");
        emit("\tcltd\n");   // 作用是把eax的32位整数扩展为64位，高32位用eax的符号位填充保存到edx
        emit("\tdiv\t%ecx\n");
        break;
      default:
        new Bug();
      }
    } else if (exp instanceof Eseq) {
      Eseq e = (Eseq) exp;
      Stm.T stm = e.stm;
      Exp.T ee = e.exp;

      compileStm(stm);
      compileExp(ee);
    } else
      new Bug();
  }

  private void compileExpList(ExpList.T explist)
  {
    if (explist instanceof ExpList.Pair) {
      ExpList.Pair pair = (ExpList.Pair) explist;
      Exp.T exp = pair.exp;
      ExpList.T list = pair.list;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
      compileExpList(list);
    } else if (explist instanceof ExpList.Last) {
      ExpList.Last last = (ExpList.Last) explist;
      Exp.T exp = last.exp;

      compileExp(exp);
      emit("\tpushl\t%eax\n");
      emit("\tpushl\t$slp_format\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  private void compileStm(Stm.T prog)
  {
    if (prog instanceof Stm.Compound) {
      Stm.Compound s = (Stm.Compound) prog;
      Stm.T s1 = s.s1;
      Stm.T s2 = s.s2;

      compileStm(s1);
      compileStm(s2);
    } else if (prog instanceof Stm.Assign) {
      Stm.Assign s = (Stm.Assign) prog;
      String id = s.id;
      Exp.T exp = s.exp;

      ids.add(id);
      compileExp(exp);
      emit("\tmovl\t%eax, " + id + "\n");
    } else if (prog instanceof Stm.Print) {
      Stm.Print s = (Stm.Print) prog;
      ExpList.T explist = s.explist;

      compileExpList(explist);
      emit("\tpushl\t$newline\n");
      emit("\tcall\tprintf\n");
      emit("\taddl\t$4, %esp\n");
    } else
      new Bug();
  }

  // ////////////////////////////////////////
  public void doit(Stm.T prog)
  {
    // return the maximum number of arguments
    if (Control.ConSlp.action == Control.ConSlp.T.ARGS) {
      int numArgs = maxArgsStm(prog);
      System.out.println(numArgs);
    }

    // interpret a given program
    if (Control.ConSlp.action == Control.ConSlp.T.INTERP) {
      interpStm(prog);
    }

    // compile a given SLP program to x86
    if (Control.ConSlp.action == Control.ConSlp.T.COMPILE) {
      ids = new HashSet<String>();
      buf = new StringBuffer();

      compileStm(prog);
      try {
        // FileOutputStream out = new FileOutputStream();
        FileWriter writer = new FileWriter("slp_gen.s");
        writer.write("// Automatically generated by the Tiger compiler, do NOT edit.\n\n");
        writer.write("\t.data\n");
        writer.write("slp_format:\n");
        writer.write("\t.string \"%d \"\n");
        writer.write("newline:\n");
        writer.write("\t.string \"\\n\"\n");
        for (String s : this.ids) {
          writer.write(s + ":\n");
          writer.write("\t.int 0\n");
        }
        writer.write("\n\n\t.text\n");
        writer.write("\t.globl main\n");
        writer.write("main:\n");
        writer.write("\tpushl\t%ebp\n");    // 保存栈帧
        writer.write("\tmovl\t%esp, %ebp\n");
        writer.write(buf.toString());
        writer.write("\tleave\n\tret\n\n");
        writer.close();
        Process child = Runtime.getRuntime().exec("gcc slp_gen.s");
        child.waitFor();
        if (!Control.ConSlp.keepasm)
          Runtime.getRuntime().exec("rm -rf slp_gen.s");
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
      // System.out.println(buf.toString());
    }
  }
}

// 使用一个数据结构来存储变量名以及对应的值(符号表)
class Table{
  // 使用一个静态对象来避免多传一个参数
  private static Table cur;

  String id;
  int value;
  Table tail;

  Table(){}
  Table(String id, int value, Table tail){
    this.id = id;
    this.value = value;
    this.tail = tail;
  }

  public static int lookup(String id){
    Table tmp;
    tmp = cur;
    while(tmp.id != id) tmp = tmp.tail;
    return tmp.value;
  }

  public static void update(String id, int value){
    Table table_new = new Table(id, value, cur);
    cur = table_new;
  }
}
