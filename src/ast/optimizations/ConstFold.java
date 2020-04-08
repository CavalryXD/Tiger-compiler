package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class;
import ast.Ast.Method;
import ast.Ast.Stm;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.Exp.Add;
import ast.Ast.Exp.And;
import ast.Ast.Exp.ArraySelect;
import ast.Ast.Exp.Call;
import ast.Ast.Exp.False;
import ast.Ast.Exp.Id;
import ast.Ast.Exp.Length;
import ast.Ast.Exp.Lt;
import ast.Ast.Exp.NewIntArray;
import ast.Ast.Exp.NewObject;
import ast.Ast.Exp.Not;
import ast.Ast.Exp.Num;
import ast.Ast.Exp.Sub;
import ast.Ast.Exp.This;
import ast.Ast.Exp.Times;
import ast.Ast.Exp.True;
import ast.Ast.MainClass;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.AssignArray;
import ast.Ast.Stm.Block;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;
import ast.Ast.Stm.While;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;

// Constant folding optimizations on an AST.

public class ConstFold implements ast.Visitor
{
	private Class.T newClass;
	private MainClass.T mainClass;
	public Program.T program;

	private LinkedList<Class.T> classes;
	private ast.Ast.Stm.T stm;
	private LinkedList<ast.Ast.Stm.T> stms;
	private ast.Ast.Method.MethodSingle method;
	private LinkedList<ast.Ast.Method.T> methods;
	private ast.Ast.Exp.T exp;

	public ConstFold()
	{
		this.newClass = null;
		this.mainClass = null;
		this.program = null;
	}

	// //////////////////////////////////////////////////////
	//
	public String genId()
	{
		return util.Temp.next();
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e)
	{
		ast.Ast.Exp.T left, right;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
    right = this.exp;
    // 直接计算好Num
		if (left instanceof ast.Ast.Exp.Num && right instanceof ast.Ast.Exp.Num)
		{
			ast.Ast.Exp.Num leftt = (ast.Ast.Exp.Num) e.left;
			ast.Ast.Exp.Num rightt = (ast.Ast.Exp.Num) e.right;
			int num = leftt.num + rightt.num;
      this.exp = new ast.Ast.Exp.Num(num, e.lineNum);
      ast.optimizations.Main.modified();    // 修改tag
			return;
		}
		else
			this.exp = new ast.Ast.Exp.Add(left, right, e.lineNum);
	}

	@Override
	public void visit(And e)
	{
		boolean leftt, rightt;
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;
		if ((left instanceof ast.Ast.Exp.True || left instanceof ast.Ast.Exp.False)
				&& (right instanceof ast.Ast.Exp.True || right instanceof ast.Ast.Exp.False))
		{
			if (left instanceof ast.Ast.Exp.True)
				leftt = true;
			else
				leftt = false;
			if (right instanceof ast.Ast.Exp.True)
				rightt = true;
			else
				rightt = false;

			if (leftt && rightt){
        this.exp = new ast.Ast.Exp.True(e.lineNum);
        ast.optimizations.Main.modified();
      }
			else{
        this.exp = new ast.Ast.Exp.False(e.lineNum);
        ast.optimizations.Main.modified();
      }
		}
		else
			this.exp = new ast.Ast.Exp.And(left, right, e.lineNum);
		return;
	}

	@Override
	public void visit(ArraySelect e)
	{
		ast.Ast.Exp.T index;
		e.index.accept(this);
		index = this.exp;
		this.exp = new ast.Ast.Exp.ArraySelect(e.array, index, e.lineNum);
		return;
	}

	@Override
	public void visit(Call e)
	{
		LinkedList<ast.Ast.Exp.T> args = new LinkedList<ast.Ast.Exp.T>();
		ast.Ast.Exp.T exp;
		e.exp.accept(this);
		exp = this.exp;
		for (ast.Ast.Exp.T arg : e.args)
		{
			arg.accept(this);
			args.add(this.exp);
		}
		this.exp = new ast.Ast.Exp.Call(exp, e.id, args, e.type, e.at, e.rt);
		return;
	}

	@Override
	public void visit(False e)
	{
		this.exp = e;
		return;
	}

	@Override
	public void visit(Id e)
	{
		this.exp = e;
		return;
	}

	@Override
	public void visit(Length e)
	{
		ast.Ast.Exp.T array;
		e.array.accept(this);
		array = this.exp;
		this.exp = new ast.Ast.Exp.Length(array, e.lineNum);
		return;
	}

	@Override
	public void visit(Lt e)
	{
		ast.Ast.Exp.T left, right;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;
		if ((left instanceof ast.Ast.Exp.Num) && (right instanceof ast.Ast.Exp.Num))
		{
			ast.Ast.Exp.Num leftt = (ast.Ast.Exp.Num) left;
			ast.Ast.Exp.Num rightt = (ast.Ast.Exp.Num) right;
			if (leftt.num < rightt.num){
        		this.exp = new ast.Ast.Exp.True(e.lineNum);
        		ast.optimizations.Main.modified();
      		}
		else{
        	this.exp = new ast.Ast.Exp.False(e.lineNum);
        	ast.optimizations.Main.modified();
      	}
		}
		else
			this.exp = new ast.Ast.Exp.Lt(left, right, e.lineNum);
		return;
	}

	@Override
	public void visit(NewIntArray e)
	{
		ast.Ast.Exp.T exp;
		e.exp.accept(this);
		exp = this.exp;
		this.exp = new ast.Ast.Exp.NewIntArray(exp, e.lineNum);
		return;
	}

	@Override
	public void visit(NewObject e)
	{
		this.exp = e;
		return;
	}

	@Override
	public void visit(Not e)
	{
		ast.Ast.Exp.T exp;
		e.exp.accept(this);
		exp = this.exp;
		if (exp instanceof ast.Ast.Exp.True){
      this.exp = new ast.Ast.Exp.False(e.lineNum);
      ast.optimizations.Main.modified();
    }
		else if (exp instanceof ast.Ast.Exp.False){
      this.exp = new ast.Ast.Exp.True(e.lineNum);
      ast.optimizations.Main.modified();
    }
		else
			this.exp = new ast.Ast.Exp.Not(exp, e.lineNum);
		return;
	}

	@Override
	public void visit(Num e)
	{
		this.exp = e;
		return;
	}

	@Override
	public void visit(Sub e)
	{
		ast.Ast.Exp.T left, right;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;

		if ((left instanceof ast.Ast.Exp.Num)
				&& (right instanceof ast.Ast.Exp.Num))
		{
			ast.Ast.Exp.Num leftt, rightt;
			leftt = (ast.Ast.Exp.Num) left;
			rightt = (ast.Ast.Exp.Num) right;
			int num = leftt.num - rightt.num;

      this.exp = new ast.Ast.Exp.Num(num, e.lineNum);
      ast.optimizations.Main.modified();
		}
		else
			this.exp = new ast.Ast.Exp.Sub(left, right, e.lineNum);
		return;
	}

	@Override
	public void visit(This e)
	{
		this.exp = e;
		return;
	}

	@Override
	public void visit(Times e)
	{
		ast.Ast.Exp.T left, right;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;

		if ((left instanceof ast.Ast.Exp.Num)
				&& (right instanceof ast.Ast.Exp.Num))
		{
			ast.Ast.Exp.Num leftt, rightt;
			leftt = (ast.Ast.Exp.Num) left;
			rightt = (ast.Ast.Exp.Num) right;
			int num = leftt.num * rightt.num;

      this.exp = new ast.Ast.Exp.Num(num, e.lineNum);
      ast.optimizations.Main.modified();
		}
		else
			this.exp = new ast.Ast.Exp.Times(left, right, e.lineNum);
		return;
	}

	@Override
	public void visit(True e)
	{
		this.exp = e;
		return;
	}

	// statements
	@Override
	public void visit(Assign s)
	{
		s.exp.accept(this);
		this.stm = new ast.Ast.Stm.Assign(s.id, this.exp, s.type, s.isField,
				s.lineNum);
		return;
	}

	@Override
	public void visit(AssignArray s)
	{
		ast.Ast.Exp.T index, exp;
		s.index.accept(this);
		index = this.exp;
		s.exp.accept(this);
		exp = this.exp;

		this.stm = new ast.Ast.Stm.AssignArray(s.id, index, exp, s.type,
				s.isField, s.lineNum);

	}

	@Override
	public void visit(Block s)
	{
		LinkedList<Stm.T> bstms = new LinkedList<Stm.T>();
		for (ast.Ast.Stm.T ss : s.stms)
		{
			ss.accept(this);
			bstms.add(this.stm);
		}
		this.stm = new ast.Ast.Stm.Block(bstms, s.lineNum);
	}

	@Override
	public void visit(If s)
	{
		ast.Ast.Exp.T condition;
		ast.Ast.Stm.T thenn;
		ast.Ast.Stm.T elsee;

		s.condition.accept(this);
		condition = this.exp;
		s.thenn.accept(this);
		thenn = this.stm;
		s.elsee.accept(this);
		elsee = this.stm;

		this.stm = new ast.Ast.Stm.If(condition, thenn, elsee, s.lineNum);

		return;
	}

	@Override
	public void visit(Print s)
	{
		s.exp.accept(this);

		this.stm = new ast.Ast.Stm.Print(this.exp, s.lineNum);
		return;
	}

	@Override
	public void visit(While s)
	{
		ast.Ast.Exp.T condition;
		ast.Ast.Stm.T body;
		s.condition.accept(this);
		condition = this.exp;
		s.body.accept(this);
		body = this.stm;

		this.stm = new ast.Ast.Stm.While(condition, body, s.lineNum);
	}

	// type
	@Override
	public void visit(Boolean t)
	{
		return;
	}

	@Override
	public void visit(ClassType t)
	{
		return;
	}

	@Override
	public void visit(Int t)
	{
		return;
	}

	@Override
	public void visit(IntArray t)
	{
		return;
	}

	// dec
	@Override
	public void visit(DecSingle d)
	{
		return;
	}

	// method
	@Override
	public void visit(MethodSingle m)
	{
		this.stms = new LinkedList<Stm.T>();
		ast.Ast.Exp.T retExp;
		for (ast.Ast.Stm.T s : m.stms)
		{
			s.accept(this);
			this.stms.add(this.stm);
		}

		m.retExp.accept(this);
		retExp = this.exp;
		this.method = new ast.Ast.Method.MethodSingle(m.retType, m.id,
				m.formals, m.locals, stms, retExp);
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c)
	{
		this.methods = new LinkedList<ast.Ast.Method.T>();
		for (ast.Ast.Method.T m : c.methods)
		{
			ast.Ast.Method.MethodSingle mm = (ast.Ast.Method.MethodSingle) m;
			mm.accept(this);
			this.methods.add(this.method);
		}
		this.newClass = new ast.Ast.Class.ClassSingle(c.id, c.extendss, c.decs,
				this.methods);
		return;
	}

	// main class
	@Override
	public void visit(MainClassSingle c)
	{
		c.stm.accept(this);
		this.mainClass = new ast.Ast.MainClass.MainClassSingle(c.id, c.arg,
				this.stm);
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p)
	{
		// You should comment out this line of code:
		p.mainClass.accept(this);

		classes = new LinkedList<Class.T>();
		for (ast.Ast.Class.T c : p.classes)
		{
			ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
			cc.accept(this);
			classes.add(this.newClass);
		}
		this.program = new ast.Ast.Program.ProgramSingle(mainClass, classes);

		if (control.Control.isTracing("ast.ConstFold")){
      System.out.println("before ConstFold optimization:");
      ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
      p.accept(pp);
      System.out.println("after ConstFold optimization:");
      this.program.accept(pp);
		}
		return;
	}
}
