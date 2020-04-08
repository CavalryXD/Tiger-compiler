package cfg;

import java.util.ArrayList;
import java.util.LinkedList;

import util.Bug;
import cfg.Cfg.Block;
import cfg.Cfg.Block.BlockSingle;
import cfg.Cfg.Class;
import cfg.Cfg.Class.ClassSingle;
import cfg.Cfg.Dec;
import cfg.Cfg.Dec.DecSingle;
import cfg.Cfg.MainMethod;
import cfg.Cfg.MainMethod.MainMethodSingle;
import cfg.Cfg.Method;
import cfg.Cfg.Method.MethodSingle;
import cfg.Cfg.Operand;
import cfg.Cfg.Operand.Int;
import cfg.Cfg.Operand.Var;
import cfg.Cfg.Program;
import cfg.Cfg.Program.ProgramSingle;
import cfg.Cfg.Stm;
import cfg.Cfg.Stm.InvokeVirtual;
import cfg.Cfg.Stm.Lt;
import cfg.Cfg.Stm.Move;
import cfg.Cfg.Stm.NewObject;
import cfg.Cfg.Stm.Print;
import cfg.Cfg.Stm.Sub;
import cfg.Cfg.Stm.Times;
import cfg.Cfg.Transfer;
import cfg.Cfg.Transfer.Goto;
import cfg.Cfg.Transfer.If;
import cfg.Cfg.Transfer.Return;
import cfg.Cfg.Type;
import cfg.Cfg.Type.ClassType;
import cfg.Cfg.Type.IntType;
import cfg.Cfg.Vtable;
import cfg.Cfg.Vtable.VtableSingle;

// Traverse the C AST, and generate
// a control-flow graph.
public class TranslateVisitor implements codegen.C.Visitor
{
	private String classId;
	private Type.T type; 			// type after translation
	private Operand.T operand;		// 当前的操作数
	private Dec.T dec;
	// A dirty hack. Can hold stm, transfer, or label.
	private ArrayList<Object> stmOrTransfer;
	private util.Label entry;
	private LinkedList<Dec.T> newLocals;	// 相当于tempVars
	private Method.T method;
	private Class.T classs;
	private Vtable.T vtable;
	private MainMethod.T mainMethod;
	public Program.T program;

	public TranslateVisitor()
	{
		this.classId = null;
		this.type = null;
		this.dec = null;
		this.stmOrTransfer = new java.util.ArrayList<Object>();
		this.newLocals = new LinkedList<Dec.T>();
		this.method = null;
		this.classs = null;
		this.vtable = null;
		this.mainMethod = null;
		this.program = null;
	}

	// /////////////////////////////////////////////////////
	// utility functions
	private java.util.LinkedList<Block.T> cookBlocks()
	{
		// block里面存放的是一个一个的block
		java.util.LinkedList<Block.T> blocks = new java.util.LinkedList<Block.T>();

		int i = 0;
		int size = this.stmOrTransfer.size();
		while (i < size)
		{
			util.Label label;
			BlockSingle b;
			LinkedList<Stm.T> stms = new LinkedList<Stm.T>();
			Transfer.T transfer;

			// 小block的第一个语句必须是Label
			if (!(this.stmOrTransfer.get(i) instanceof util.Label))
			{
				System.out.println("First statement must be Label !");
				new util.Bug();
			}
			label = (util.Label) this.stmOrTransfer.get(i++);
			// 将语句放入stm链表
			while (i < size && this.stmOrTransfer.get(i) instanceof Stm.T)
			{
				stms.add((Stm.T) this.stmOrTransfer.get(i++));
			}// 不再是Stm
				// 可能是tansfer或者是lable
				// tansfer是blockSingle的结尾
			if (this.stmOrTransfer.get(i) instanceof Transfer.T)
			{
				transfer = (Transfer.T) this.stmOrTransfer.get(i++);
				b = new BlockSingle(label, stms, transfer);
				blocks.add(b);
			}
			else
			{
				System.out.println("Last statement must be Transfer !");
				new util.Bug();
			}

		}
		// 每次执行的最后刷新stmOrTransfer
		this.stmOrTransfer = new java.util.ArrayList<Object>();
		return blocks;
	}

	/*
	 * emit()的作用是将stm加到链表中
	 */
	private void emit(Object obj)
	{
		this.stmOrTransfer.add(obj);
	}

	/*
	 * genVar()的作用是返回一个x_编号 另外会将Exp 变为一个4元组 并且将这个编号放到tempVar声明列表
	 */
	private String genVar()
	{
		String fresh = util.Temp.next();
		DecSingle dec = new DecSingle(new IntType(), fresh);
		this.newLocals.add(dec);
		return fresh;
	}

	private String genVar(Type.T ty)
	{
		String fresh = util.Temp.next();
		DecSingle dec = new DecSingle(ty, fresh);
		this.newLocals.add(dec);
		return fresh;
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(codegen.C.Ast.Exp.Add e)
	{
		String dst = genVar();
		e.left.accept(this);
		Operand.T left = this.operand;
		e.right.accept(this);
		Operand.T right = this.operand;
		emit(new cfg.Cfg.Stm.Add(dst, null, left, right));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.And e)
	{
		String dst = genVar();
		e.left.accept(this);
		Operand.T left = this.operand;
		e.right.accept(this);
		Operand.T right = this.operand;
		emit(new cfg.Cfg.Stm.And(dst, left, right));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.ArraySelect e)
	{
		String dst = genVar();
		e.array.accept(this);
		Operand.T array = this.operand;
		e.index.accept(this);
		Operand.T index = this.operand;
		emit(new cfg.Cfg.Stm.ArraySelect(dst, array, index));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Call e)
	{
		e.retType.accept(this);
		String dst = genVar(this.type);		// 对类型用genVar处理，得到一个x_的编号
		String obj = null;
		e.exp.accept(this);
		Operand.T objOp = this.operand;
		if (objOp instanceof Var)			// 三地址码必须将函数调用值返回给一个Var
		{
			Var var = (Var) objOp;
			if (var.isField == false)
				obj = var.id;
			else
				obj = "this->" + var.id;	// 在此判断是否为父类型
		}
		else
			new util.Bug();

		LinkedList<Operand.T> newArgs = new LinkedList<Operand.T>();
		for (codegen.C.Ast.Exp.T x : e.args)
		{
			x.accept(this);
			newArgs.add(this.operand);
		}
		emit(new InvokeVirtual(dst, obj, e.id, newArgs));
		this.operand = new Var(dst);		// 将dst变为一个Var对象
		// 这个Var可能被放到println里面,也可能在 = 的右边
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Id e)
	{
		this.operand = new Var(e.id, e.isField);
		// 在处理Id时，给Var加一个isField
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Length e)
	{
		String dst = genVar();
		e.array.accept(this);
		Operand.T array = this.operand;
		emit(new cfg.Cfg.Stm.Length(dst, array));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Lt e)
	{
		String dst = genVar();// 产生了des
		e.left.accept(this);
		Operand.T left = this.operand;// 左操作数,this.operand是在上一句里面改变的
		e.right.accept(this);// 右操作数
		emit(new Lt(dst, null, left, this.operand));
		this.operand = new Var(dst);
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.NewIntArray e)
	{
		e.exp.accept(this);
		Operand.T exp = this.operand;
		/*
		 * NewIntArray也是一个Exp 比如 array=new int[10-1];
		 * 
		 * a=10-1; int* b=new int[a];(b会提前声明) array=b;
		 */
		String dst = genVar(new cfg.Cfg.Type.IntArrayType());
		/*
		 * 这一条语句就做到了提前声明int* b；且把dst变为b
		 */
		emit(new cfg.Cfg.Stm.NewIntArray(dst, exp));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.NewObject e)
	{
		// x_7 = ((struct Fac*)(Tiger_new (&Fac_vtable_, sizeof(struct Fac))));
		String dst = genVar(new ClassType(e.id));	// 为了产生一个提前的声明
		emit(new NewObject(dst, e.id));
		this.operand = new Var(dst);
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Not e)
	{
		String dst = genVar();
		e.exp.accept(this);
		Operand.T exp = this.operand;
		emit(new cfg.Cfg.Stm.Not(dst, exp));
		this.operand = new Var(dst);
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Num e)
	{
		this.operand = new Int(e.num);	// e.exp.accept(this)的终点
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Sub e)
	{
		String dst = genVar();			// 产生des
		e.left.accept(this);
		Operand.T left = this.operand;
		e.right.accept(this);			// 左右操作数
		emit(new Sub(dst, null, left, this.operand));
		this.operand = new Var(dst);	// 将这个Exp变为一个Var
		// eg. x = 3 - 1现在会变为x1 = 3 - 1; x = x1;
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.This e)
	{
		this.operand = new Var("this");
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Exp.Times e)
	{
		String dst = genVar();
		e.left.accept(this);
		Operand.T left = this.operand;
		e.right.accept(this);
		emit(new Times(dst, null, left, this.operand));
		this.operand = new Var(dst);	// 将这个Exp变为一个Var
		return;
	}

	// statements
	@Override
	public void visit(codegen.C.Ast.Stm.Assign s)
	{
		s.exp.accept(this);
		emit(new Move(s.id, null, this.operand, s.isField));// 赋值语句
		// exp先计算好值, eg : x_0 = 1 + 3;  s.id = x_0;
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Stm.AssignArray s)
	{
		s.index.accept(this);	// 已经将index的语句emit
		Operand.T index = this.operand;
		s.exp.accept(this);
		Operand.T exp = this.operand;
		emit(new cfg.Cfg.Stm.AssignArray(s.id, index, exp, s.isField));
	}

	@Override
	public void visit(codegen.C.Ast.Stm.Block s)
	{
		for (codegen.C.Ast.Stm.T ss : s.stms)
			ss.accept(this);
	}

	@Override
	public void visit(codegen.C.Ast.Stm.If s)
	{
		// true, false, end
		util.Label tl = new util.Label(), fl = new util.Label(), el = new util.Label();
		s.condition.accept(this);
		emit(new If(this.operand, tl, fl));
		emit(fl);				// 如果false
		s.elsee.accept(this);	// 把false的语句 emit
		emit(new Goto(el));		// 跳转到结束
		emit(tl);				// 如果为true
		s.thenn.accept(this);	// 把true的语句emit
		emit(new Goto(el));		// 结束//////同时，Goto还是block的结束标志。
		emit(el);				// 结束标签
		// 注意If语句中每个分支最后都要有一个Goto(el)
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Stm.Print s)
	{
		s.exp.accept(this);
		emit(new Print(this.operand));// print里面只有一个操作数。
		return;
	}

	@Override
	public void visit(codegen.C.Ast.Stm.While s)
	{
		util.Label start = new util.Label();
		util.Label end = new util.Label();
		util.Label body = new util.Label();

		emit(new cfg.Cfg.Transfer.Goto(start));	// 加这一个Goto是为了让cookBlock通过, 小trick
		/*
		 * 一个小block会以lable开头，以transfor结尾 但是while必须在一开始加一个label，到时候gaoto回来，所以就出现了
		 * 两个label之间没有transfor的情况
		 */
		emit(start);
		s.condition.accept(this);
		emit(new cfg.Cfg.Transfer.If(this.operand, body, end));
		emit(body);
		s.body.accept(this);
		emit(new cfg.Cfg.Transfer.Goto(start));

		emit(end);
	}

	// type
	@Override
	public void visit(codegen.C.Ast.Type.ClassType t)
	{
		this.type = new ClassType(t.id);
	}

	@Override
	public void visit(codegen.C.Ast.Type.Int t)
	{
		this.type = new IntType();
	}

	@Override
	public void visit(codegen.C.Ast.Type.IntArray t)
	{
		this.type = new cfg.Cfg.Type.IntArrayType();
	}

	// dec
	@Override
	public void visit(codegen.C.Ast.Dec.DecSingle d)
	{
		d.type.accept(this);
		this.dec = new DecSingle(this.type, d.id);
		return;
	}

	// vtable
	@Override
	public void visit(codegen.C.Ast.Vtable.VtableSingle v)
	{
		// 方法信息列表
		java.util.LinkedList<cfg.Ftuple> newTuples = new java.util.LinkedList<cfg.Ftuple>();
		for (codegen.C.Ftuple t : v.ms)
		{
			t.ret.accept(this);// 方法返回值
			Type.T ret = this.type;
			// 刷新声明列表
			java.util.LinkedList<Dec.T> args = new java.util.LinkedList<>();
			for (codegen.C.Ast.Dec.T dec : t.args)
			{
				dec.accept(this);
				args.add(this.dec);
			}
			newTuples.add(new cfg.Ftuple(t.classs, ret, args, t.id));
		}
		// 建立新的虚方法对象
		this.vtable = new VtableSingle(v.id, newTuples);
		return;
	}

	// class
	@Override
	public void visit(codegen.C.Ast.Class.ClassSingle c)
	{
		java.util.LinkedList<cfg.Tuple> newTuples = new java.util.LinkedList<cfg.Tuple>();

		// 遍历C的class表，对class的类型做一下修改,然后生成新的cfg的class
		for (codegen.C.Tuple t : c.decs)
		{
			t.type.accept(this);
			newTuples.add(new cfg.Tuple(t.classs, this.type, t.id));
		}
		this.classs = new ClassSingle(c.id, newTuples);
		return;
	}

	// method
	@Override
	public void visit(codegen.C.Ast.Method.MethodSingle m)
	{
		this.newLocals = new java.util.LinkedList<>();

		m.retType.accept(this);
		Type.T retType = this.type;

		LinkedList<Dec.T> newFormals = new LinkedList<Dec.T>();
		for (codegen.C.Ast.Dec.T c : m.formals)
		{// 参数
			c.accept(this);
			newFormals.add(this.dec);
		}

		LinkedList<Dec.T> locals = new LinkedList<Dec.T>();// 声明
		for (codegen.C.Ast.Dec.T c : m.locals)
		{
			c.accept(this);
			locals.add(this.dec);
		}

		// a junk label在block之前加的第一个标签, 也是为了cookblock
		util.Label entry = new util.Label();
		this.entry = entry;
		emit(entry);

		for (codegen.C.Ast.Stm.T s : m.stms)
			s.accept(this);					// 期间emit执行的若干次

		m.retExp.accept(this);
		Operand.T retVal = this.operand;
		emit(new Return(this.operand));		// 最后加入return!!!!

		//
		LinkedList<Block.T> blocks = cookBlocks();

		for (Dec.T d : this.newLocals)
			locals.add(d); // newLocals就是C里面的temVars

		this.method = new MethodSingle(retType, m.id, m.classId, newFormals,
				locals, blocks, entry, null, retVal);		// todo : exit设置
		return;
	}

	// main method
	@Override
	public void visit(codegen.C.Ast.MainMethod.MainMethodSingle m)
	{
		this.newLocals = new java.util.LinkedList<>();	// 刷新tempVars

		// 局部变量
		java.util.LinkedList<Dec.T> locals = new LinkedList<Dec.T>();
		for (codegen.C.Ast.Dec.T c : m.locals)
		{
			c.accept(this);
			locals.add(this.dec);
		}

		util.Label entry = new util.Label();
		emit(entry);

		m.stm.accept(this);

		emit(new Transfer.Return(new Operand.Int(0)));

		java.util.LinkedList<Block.T> blocks = cookBlocks();
		for (Dec.T d : this.newLocals)
			locals.add(d);// 将tempVars加入局部变量表
		this.mainMethod = new MainMethodSingle(locals, blocks);
		return;
	}

	// program
	@Override
	public void visit(codegen.C.Ast.Program.ProgramSingle p)
	{
		// 遍历class表
		java.util.LinkedList<Class.T> newClasses = new LinkedList<Class.T>();
		for (codegen.C.Ast.Class.T c : p.classes)
		{
			c.accept(this);
			newClasses.add(this.classs);
		}

		// 遍历虚方法表
		java.util.LinkedList<Vtable.T> newVtable = new LinkedList<Vtable.T>();
		for (codegen.C.Ast.Vtable.T v : p.vtables)
		{
			v.accept(this);// 执行完成后this.vtable会更新
			newVtable.add(this.vtable);
		}

		// 遍历方法
		LinkedList<Method.T> newMethods = new LinkedList<Method.T>();
		for (codegen.C.Ast.Method.T m : p.methods)
		{
			m.accept(this);
			newMethods.add(this.method);
		}

		// 处理mainMethod
		p.mainMethod.accept(this);
		MainMethod.T newMainMethod = this.mainMethod;

		this.program = new ProgramSingle(newClasses, newVtable, newMethods,
				newMainMethod);
		return;
	}
}
