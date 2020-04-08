/*------------------------------------------------------------------*/
/* Copyright (C) SSE-USTC, 2014-2015                                */
/*                                                                  */
/*  FILE NAME             :  AlgSimp.java                           */
/*  PRINCIPAL AUTHOR      :  qcLiu                                  */
/*  LANGUAGE              :  Java                                   */
/*  TARGET ENVIRONMENT    :  ANY                                    */
/*  DATE OF FIRST RELEASE :  2014/10/05                             */
/*  DESCRIPTION           :  the tiger compiler                     */
/*------------------------------------------------------------------*/

/*
 * Revision log:
 *
 * 
 *
 */
package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class;
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
import ast.Ast.Method;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm;
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

// Algebraic simplification optimizations on an AST.

public class AlgSimp implements ast.Visitor
{
	private Class.T newClass;
	private MainClass.T mainClass;
	public Program.T program;

	public LinkedList<Class.T> classes;
	public Stm.T stm;
	public LinkedList<Method.T> methods;
	public ast.Ast.Method.MethodSingle method;
	public LinkedList<Stm.T> stms;
	public ast.Ast.Exp.T exp;

	public boolean leftneedopti = false;
	public boolean rightneedopti = false;
	public boolean is0 = false;
	public boolean leftis0 = false;
	public boolean rightis0 = false;

	public AlgSimp()
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
		// need opti
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;
		e.left.accept(this);
		if (this.is0)
			this.leftis0 = true;
		else
			this.leftis0 = false;
		left = this.exp;

		e.right.accept(this);
		if (this.is0)
			this.rightis0 = true;
		else
			this.rightis0 = false;
		right = this.exp;

		if (this.leftis0 && this.rightis0){
			this.exp = new ast.Ast.Exp.Num(0, e.lineNum);
			ast.optimizations.Main.modified();
		}
		else if (this.leftis0){
			this.exp = right;
			ast.optimizations.Main.modified();		// 只有单边为0, 也算修改
		}
		else if (this.rightis0){
			this.exp = left;
			ast.optimizations.Main.modified();
		}
		else
			this.exp = e;
	}

	@Override
	public void visit(And e)
	{
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;
		this.is0 = false;
		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;

		this.exp = new ast.Ast.Exp.And(left, right, e.lineNum);
	}

	@Override
	public void visit(ArraySelect e)
	{
		ast.Ast.Exp.T index;
		e.index.accept(this);
		index = this.exp;

		this.exp = new ast.Ast.Exp.ArraySelect(e.array, index, e.lineNum);
	}

	@Override
	public void visit(Call e)
	{
		this.is0 = false;
		ast.Ast.Exp.T exp;
		LinkedList<ast.Ast.Exp.T> args = new LinkedList<ast.Ast.Exp.T>();
		e.exp.accept(this);
		exp = this.exp;
		for (ast.Ast.Exp.T arg : e.args)
		{
			arg.accept(this);
			args.add(this.exp);
		}
		/*
		 * 在这个位置要格外的注意。因为Elab时给ast.Ast的成员添加了字段，所以有必要写一个新的Call构造函数
		 * 否则生成的C代码会出现在声明的最后补上的调用时的声明类型为null，因为e.type是Elab是添加的
     	 * 所以需要手动添加type字段
		 */
		this.exp = new ast.Ast.Exp.Call(exp, e.id, args, e.type, e.at, e.rt);
		return;
	}

	@Override
	public void visit(False e)
	{
		this.is0 = false;
		this.exp = e;
	}

	@Override
	public void visit(Id e)
	{
		this.is0 = false;
		this.exp = e;
		return;
	}

	@Override
	public void visit(Length e)
	{
		this.is0 = false;
		ast.Ast.Exp.T array;
		e.array.accept(this);
		array = this.exp;
		this.exp = new ast.Ast.Exp.Length(array, e.lineNum);
		return;
	}

	@Override
	public void visit(Lt e)
	{
		// need??
		this.is0 = false;
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;

		e.left.accept(this);
		left = this.exp;
		e.right.accept(this);
		right = this.exp;

		this.exp = new ast.Ast.Exp.Lt(left, right, e.lineNum);
		return;
	}

	@Override
	public void visit(NewIntArray e)
	{
		this.is0 = false;
		ast.Ast.Exp.T exp;
		e.exp.accept(this);
		exp = this.exp;

		this.exp = new ast.Ast.Exp.NewIntArray(exp, e.lineNum);
		return;
	}

	@Override
	public void visit(NewObject e)
	{
		this.is0 = false;
		this.exp = e;
		return;
	}

	@Override
	public void visit(Not e)
	{
		// need??
		this.is0 = false;
		ast.Ast.Exp.T exp;
		e.exp.accept(this);
		exp = this.exp;

		this.exp = new ast.Ast.Exp.Not(exp, e.lineNum);
		return;
	}

	@Override
	public void visit(Num e)
	{
		this.is0 = false;
		if (e.num == 0)
			this.is0 = true;
		else
			this.is0 = false;
		this.exp = e;
		return;
	}

	@Override
	public void visit(Sub e)
	{
		// need opti
		this.is0 = false;
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;

		e.left.accept(this);
		if (this.is0)
			this.leftis0 = true;
		else
			this.leftis0 = false;
		left = this.exp;

		e.right.accept(this);
		if (this.is0)
			this.rightis0 = true;
		else
			this.rightis0 = false;
		right = this.exp;

		if (this.leftis0 && this.rightis0){
			this.exp = new ast.Ast.Exp.Num(0, e.lineNum);
			ast.optimizations.Main.modified();
		}
		else if (this.rightis0)			// 因为token没有包含负号, 所以当左边为0时暂不处理
			this.exp = left;
		else
			this.exp = e;
		return;
	}

	@Override
	public void visit(This e)
	{
		this.is0 = false;
		this.exp = e;
		return;
	}

	@Override
	public void visit(Times e)
	{
		// need opti
		this.is0 = false;
		ast.Ast.Exp.T left;
		ast.Ast.Exp.T right;

		e.left.accept(this);
		if (this.is0)
			this.leftis0 = true;
		else
			this.leftis0 = false;
		left = this.exp;

		e.right.accept(this);
		if (this.is0)
			this.rightis0 = true;
		else
			this.rightis0 = false;
		right = this.exp;

		if (this.leftis0 || this.rightis0){
			this.exp = new ast.Ast.Exp.Num(0, e.lineNum);
			ast.optimizations.Main.modified();
		}
		else
			this.exp = e;
		return;
	}

	@Override
	public void visit(True e)
	{
		this.is0 = false;
		this.exp = e;
		return;
	}

	// ///////////////////////////////////////
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
		s.exp.accept(this);
		ast.Ast.Exp.T expp = this.exp;
		s.index.accept(this);
		ast.Ast.Exp.T index = this.exp;
		this.stm = new ast.Ast.Stm.AssignArray(s.id, index, expp, s.type,
				s.isField, s.lineNum);
		return;
	}

	@Override
	public void visit(Block s)
	{
		LinkedList<Stm.T> stms = new LinkedList<Stm.T>();
		for (ast.Ast.Stm.T ss : s.stms)
		{
			ss.accept(this);
			stms.add(this.stm);
		}
		this.stm = new ast.Ast.Stm.Block(stms, s.lineNum);
		return;
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
		s.condition.accept(this);
		condition = this.exp;
		ast.Ast.Stm.T body;
		s.body.accept(this);
		body = this.stm;

		this.stm = new ast.Ast.Stm.While(condition, body, s.lineNum);
		return;
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
		for (ast.Ast.Stm.T s : m.stms)
		{
			s.accept(this);
			this.stms.add(this.stm);
		}
		m.retExp.accept(this);
		this.method = new ast.Ast.Method.MethodSingle(m.retType, m.id,
				m.formals, m.locals, this.stms, this.exp);
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c)
	{
		this.methods = new LinkedList<Method.T>();
		for (ast.Ast.Method.T m : c.methods)
		{
			ast.Ast.Method.MethodSingle mm = (ast.Ast.Method.MethodSingle) m;
			mm.accept(this);
			methods.add(method);
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
		// this.program = p;
		p.mainClass.accept(this);

		this.classes = new LinkedList<Class.T>();
		for (ast.Ast.Class.T c : p.classes)
		{
			c.accept(this);
			this.classes.add(this.newClass);
		}
		this.program = new ast.Ast.Program.ProgramSingle(mainClass, classes);

		if (control.Control.isTracing("ast.AlgSimp"))
		{
			System.out.println("before Algebraic Simplification optimization:");
			ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
			p.accept(pp);
			System.out.println("after Algebraic Simplification optimization:");
			this.program.accept(pp);
		}
		return;
	}
}
