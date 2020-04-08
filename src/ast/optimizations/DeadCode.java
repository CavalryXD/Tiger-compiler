package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class;
import ast.Ast.Method;
import ast.Ast.Dec;
import ast.Ast.Stm;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;
import ast.Ast.Exp.*;
import ast.Ast.Stm.*;
import ast.Ast.Type.*;

// Dead code elimination optimizations on an AST.

public class DeadCode implements ast.Visitor
{
	private ast.Ast.Class.T newClass;
	private ast.Ast.MainClass.T mainClass;
	public ast.Ast.Program.T program;
	// 新增
	public ast.Ast.Stm.T stm;       // 指示当前opt后的stm, 可能为null
  public ast.Ast.Method.T method;
  public ast.Ast.Dec.T localDec;

	public boolean isTypeBoolean = false;
	public boolean isNot = false;
	public boolean isTrue = false;

	public DeadCode()
	{
		this.newClass = null;
		this.mainClass = null;
    this.program = null;
    this.localDec = null;
    this.method = null;
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
		this.isTypeBoolean = false;
	}

	@Override
	public void visit(And e)
	{
		e.left.accept(this);
		boolean left = this.isTypeBoolean;
		boolean leftTrue = this.isTrue;
		e.right.accept(this);
		boolean right = this.isTypeBoolean;
		boolean rightTrue = this.isTrue;
		if (left && right)
		{
			this.isTypeBoolean = true;    // 为了支持递归
			if (leftTrue && rightTrue)
				this.isTrue = true;
			else
				this.isTrue = false;
		}
		return;
	}

	@Override
	public void visit(ArraySelect e)
	{
		this.isTypeBoolean = false;
	}

	@Override
	public void visit(Call e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(False e)
	{
		this.isTypeBoolean = true;
		this.isTrue = false;
		return;
	}

	@Override
	public void visit(Id e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(Length e)
	{
		this.isTypeBoolean = false;
	}

	@Override
	public void visit(Lt e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(NewIntArray e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(NewObject e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(Not e)
	{
		this.isTypeBoolean = true;
		e.exp.accept(this);
		if (this.isTypeBoolean)
			this.isTrue = !this.isTrue;
		else
			return;
	}

	@Override
	public void visit(Num e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(Sub e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(This e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(Times e)
	{
		this.isTypeBoolean = false;
		return;
	}

	@Override
	public void visit(True e)
	{
		this.isTypeBoolean = true;
		this.isTrue = true;
		return;
	}

	// statements
	@Override
	public void visit(Assign s)
	{
		this.stm = s;
		return;
	}

	@Override
	public void visit(AssignArray s)
	{
		this.stm = s;
	}

	@Override
	public void visit(Block s)
	{
    java.util.LinkedList<ast.Ast.Stm.T> newStms = new java.util.LinkedList<ast.Ast.Stm.T>();
    for (ast.Ast.Stm.T t : s.stms) {
        t.accept(this);
        if (this.stm != null)
            newStms.add(this.stm);
    }
    this.stm = new ast.Ast.Stm.Block(newStms);
	}

	@Override
	public void visit(If s)
	{
		s.condition.accept(this);
		if (this.isTypeBoolean)
		{
			if (this.isTrue){
				this.stm = s.thenn;     // 即消除了if语句, 变成了普通语句
				ast.optimizations.Main.modified();
			}
			else{
				this.stm = s.elsee;
				ast.optimizations.Main.modified();
			}
		}
		else
			this.stm = s;
		return;
	}

	@Override
	public void visit(Print s)
	{
		this.stm = s;
		return;
	}

	@Override
	public void visit(While s)
	{
		s.condition.accept(this);
		if (this.isTypeBoolean && !this.isTrue){
			  this.stm = null;
			  ast.optimizations.Main.modified();
		}
		else
			this.stm = s;
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
        if (d.isUsed)
            this.localDec = d;
        else {
            ast.optimizations.Main.modified();
            this.localDec = null;
        }
        return;
	}

	// method
	@Override
	public void visit(MethodSingle m)
	{
		java.util.LinkedList<Dec.T> newLocals = new java.util.LinkedList<Dec.T>();
		java.util.LinkedList<Stm.T> newStms = new java.util.LinkedList<Stm.T>();
        for (Dec.T local : m.locals) {
            local.accept(this);
            if (this.localDec != null)
                newLocals.add(this.localDec);
        }
		for (ast.Ast.Stm.T s : m.stms)
		{
			s.accept(this);
			if (this.stm != null)   // While可能返回空。
        		newStms.add(this.stm);
		}
		this.method = new ast.Ast.Method.MethodSingle(m.retType, m.id,
				m.formals, newLocals, newStms, m.retExp);
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c)
	{
		java.util.LinkedList<Method.T> newMethods = new java.util.LinkedList<Method.T>();
		java.util.LinkedList<Dec.T> newfields = new java.util.LinkedList<Dec.T>();
		// fields
		for(ast.Ast.Dec.T dec : c.decs) {
			dec.accept(this);
			if (this.localDec != null)
			newfields.add(this.localDec);
		}
		// methods
		for (ast.Ast.Method.T m : c.methods)
		{
			ast.Ast.Method.MethodSingle mm = (ast.Ast.Method.MethodSingle) m;
			mm.accept(this);
			newMethods.add(this.method);
		}
		this.newClass = new ast.Ast.Class.ClassSingle(c.id, c.extendss, newfields,
    newMethods);
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

		java.util.LinkedList<ast.Ast.Class.T> newClasses = new java.util.LinkedList<ast.Ast.Class.T>();
		for (ast.Ast.Class.T c : p.classes)
		{
			ast.Ast.Class.ClassSingle cc = (ast.Ast.Class.ClassSingle) c;
			cc.accept(this);
			newClasses.add(newClass);
		}
		this.program = new ast.Ast.Program.ProgramSingle(mainClass, newClasses);
		// this.program=p;
		if (control.Control.isTracing("ast.DeadCode"))
		{
			System.out.println("before DeadCode optimization:");
			ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
			p.accept(pp);
			System.out.println("after DeadCode optimization:");
			this.program.accept(pp);
		}
		return;
	}
}
