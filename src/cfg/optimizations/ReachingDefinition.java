package cfg.optimizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Graph;
import util.Graph.dfs;
import cfg.Cfg.Block;
import cfg.Cfg.Method;
import cfg.Cfg.Block.BlockSingle;
import cfg.Cfg.Class.ClassSingle;
import cfg.Cfg.Dec.DecSingle;
import cfg.Cfg.MainMethod.MainMethodSingle;
import cfg.Cfg.Method.MethodSingle;
import cfg.Cfg.Operand.Int;
import cfg.Cfg.Operand.Var;
import cfg.Cfg.Program.ProgramSingle;
import cfg.Cfg.Stm;
import cfg.Cfg.Stm.Add;
import cfg.Cfg.Stm.And;
import cfg.Cfg.Stm.ArraySelect;
import cfg.Cfg.Stm.AssignArray;
import cfg.Cfg.Stm.InvokeVirtual;
import cfg.Cfg.Stm.Length;
import cfg.Cfg.Stm.Lt;
import cfg.Cfg.Stm.Move;
import cfg.Cfg.Stm.NewIntArray;
import cfg.Cfg.Stm.NewObject;
import cfg.Cfg.Stm.Not;
import cfg.Cfg.Stm.Print;
import cfg.Cfg.Stm.Sub;
import cfg.Cfg.Stm.Times;
import cfg.Cfg.Transfer;
import cfg.Cfg.Transfer.Goto;
import cfg.Cfg.Transfer.If;
import cfg.Cfg.Transfer.Return;
import cfg.Cfg.Type.ClassType;
import cfg.Cfg.Type.IntArrayType;
import cfg.Cfg.Type.IntType;
import cfg.Cfg.Vtable.VtableSingle;

public class ReachingDefinition implements cfg.Visitor
{
	private ArrayList<Graph<Block.T>.Node> top = new ArrayList<Graph<Block.T>.Node>();
	// 原始图
	private util.Graph<Block.T> graph;
	//
	private ArrayList<Graph<Block.T>.Node> pred;
	// gen, kill for one statement
	private HashSet<Stm.T> oneStmGen;
	private HashSet<Stm.T> oneStmKill;

	// gen, kill for one transfer
	private HashSet<Stm.T> oneTransferGen;
	private HashSet<Stm.T> oneTransferKill;
	// gen, kill for one block
	private HashSet<Stm.T> oneBlockGen;
	private HashSet<Stm.T> oneBlockKill;

	// gen, kill for statements
	private HashMap<Stm.T, HashSet<Stm.T>> stmGen;
	private HashMap<Stm.T, HashSet<Stm.T>> stmKill;

	// gen, kill for transfers
	private HashMap<Transfer.T, HashSet<Stm.T>> transferGen;
	private HashMap<Transfer.T, HashSet<Stm.T>> transferKill;

	// gen, kill for blocks
	private HashMap<Block.T, HashSet<Stm.T>> blockGen;
	private HashMap<Block.T, HashSet<Stm.T>> blockKill;

	
	// in, out for blocks
	int blockInSize;
	int blockOutSize;
	private boolean blockIsChanged;
	private HashMap<Block.T, HashSet<Stm.T>> blockIn;
	private HashMap<Block.T, HashSet<Stm.T>> blockOut;
	
	// in, out for statements
	public HashMap<Stm.T, HashSet<Stm.T>> stmIn;
	public HashMap<Stm.T, HashSet<Stm.T>> stmOut;

	// liveIn, liveOut for transfer
	public HashMap<Transfer.T, HashSet<Stm.T>> transferIn;
	public HashMap<Transfer.T, HashSet<Stm.T>> transferOut;

	private HashMap<String, HashSet<Stm.T>> defs;

	enum visitorKind {
		None, init, initDefs, calculateGenAndKill
	};

	enum blockKind {
		None, calculateStmGenAndKill, calculateBlockGenAndKill, 
		calculateBlockInAndOut, calculateStmInAndOut
	};

	private visitorKind vkind;
	private blockKind bkind;

	public ReachingDefinition()
	{
		this.oneStmGen = new HashSet<>();
		this.oneStmKill = new HashSet<>();

		this.oneTransferGen = new HashSet<>();
		this.oneTransferKill = new HashSet<>();

		this.oneBlockGen = new HashSet<>();
		this.oneBlockKill = new HashSet<>();

		this.stmGen = new HashMap<>();
		this.stmKill = new HashMap<>();

		this.transferGen = new HashMap<>();
		this.transferKill = new HashMap<>();

		this.blockGen = new HashMap<>();
		this.blockKill = new HashMap<>();

		this.blockIn = new HashMap<>();
		this.blockOut = new HashMap<>();

		this.stmIn = new HashMap<>();
		this.stmOut = new HashMap<>();

		this.transferIn = new HashMap<>();
		this.transferOut = new HashMap<>();

		this.defs = new HashMap<String, HashSet<Stm.T>>();

		this.vkind = visitorKind.None;
		this.bkind = blockKind.None;
		
		this.blockInSize = 0;
		this.blockOutSize = 0;
		this.blockIsChanged = true;
	}

	// /////////////////////////////////////////////////////
	// utilities
	HashSet<Stm.T> getNewStmSet()
	{
		return new HashSet<Stm.T>();
	}

	// /////////////////////////////////////////////////////
	// operand
	@Override
	public void visit(Int operand)
	{
	}

	@Override
	public void visit(Var operand)
	{
	}

	// statements
	@Override
	public void visit(Add s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// s.left.accept(this);
		// s.right.accept(this);
	}

	@Override
	public void visit(InvokeVirtual s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// for (Operand.T arg : s.args)
		// {
		// arg.accept(this);
		// }
	}

	@Override
	public void visit(Lt s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// s.left.accept(this);
		// s.right.accept(this);
	}

	@Override
	public void visit(Move s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// s.src.accept(this);
	}

	@Override
	public void visit(NewObject s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
	}

	@Override
	public void visit(Print s)
	{
		switch (this.vkind)
		{
		case initDefs:
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;
		}
		// s.arg.accept(this);
	}

	@Override
	public void visit(Sub s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;
		}
		// s.left.accept(this);
		// s.right.accept(this);
	}

	@Override
	public void visit(Times s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);

			break;
		default:
			break;
		}
		// s.left.accept(this);
		// s.right.accept(this);
	}

	// transfer
	@Override
	public void visit(If s)
	{
	}

	@Override
	public void visit(Goto s)
	{
		return;
	}

	@Override
	public void visit(Return s)
	{
	}

	// type
	@Override
	public void visit(ClassType t)
	{
	}

	@Override
	public void visit(IntType t)
	{
	}

	@Override
	public void visit(IntArrayType t)
	{
	}

	// dec
	@Override
	public void visit(DecSingle d)
	{
	}

	@Override
	public void visit(ArraySelect s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.id);
			if (def == null)
			{
				this.defs.put(s.id, getNewStmSet());
				def = this.defs.get(s.id);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.id));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// s.array.accept(this);
		// s.index.accept(this);

	}

	@Override
	public void visit(Length s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}

	}

	@Override
	public void visit(NewIntArray s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}

	}

	@Override
	public void visit(Not s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}

	}

	@Override
	public void visit(AssignArray s)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(s.dst);
			if (def == null)
			{
				this.defs.put(s.dst, getNewStmSet());
				def = this.defs.get(s.dst);
			}
			def.add(s);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(s);
			this.stmGen.put(s, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(s.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(s, this.oneStmKill);
			break;
		default:
			break;

		}
		// s.exp.accept(this);
		// s.index.accept(this);

	}

	@Override
	public void visit(And m)
	{
		switch (this.vkind)
		{
		case initDefs:
			HashSet<Stm.T> def = this.defs.get(m.dst);
			if (def == null)
			{
				this.defs.put(m.dst, getNewStmSet());
				def = this.defs.get(m.dst);
			}
			def.add(m);
			break;
		case calculateGenAndKill:
			this.oneStmGen = getNewStmSet();
			this.oneStmGen.add(m);
			this.stmGen.put(m, this.oneStmGen);

			this.oneStmKill = getNewStmSet();
			this.oneStmKill.addAll(this.defs.get(m.dst));
			this.oneStmKill.removeAll(this.oneStmGen);
			this.stmKill.put(m, this.oneStmKill);
			break;
		default:
			break;

		}
		// m.left.accept(this);
		// m.right.accept(this);

	}

	private void calculateStmGenAndKill(BlockSingle b)
	{

		for (Stm.T stm : b.stms)
		{
			stm.accept(this);
		}
		cfg.Cfg.Transfer.T t = b.transfer;
		this.oneTransferGen = getNewStmSet();
		this.oneTransferKill = getNewStmSet();
		this.transferGen.put(t, this.oneTransferGen);
		this.transferKill.put(t, this.oneTransferKill);
	}

	private void calculateBlockGenAndKill(BlockSingle b)
	{
		this.oneBlockGen = getNewStmSet();
		this.oneBlockKill = getNewStmSet();
		for (Stm.T stm : b.stms)
		{
			HashSet<Stm.T> stmGen = this.stmGen.get(stm);
			HashSet<Stm.T> stmKill = this.stmKill.get(stm);
			this.oneBlockGen.addAll(stmGen);
			this.oneBlockKill.addAll(stmKill);
		}
		this.oneBlockGen.addAll(this.transferGen.get(b.transfer));
		this.oneBlockKill.addAll(this.transferKill.get(b.transfer));

		this.blockGen.put(b, this.oneBlockGen);
		this.blockKill.put(b, this.oneBlockKill);
	}
	
	private void calculateBlockInAndOut(BlockSingle b)
	{
		HashSet<Stm.T> oneBlockGen = this.blockGen.get(b);
		HashSet<Stm.T> oneBlockKill = this.blockKill.get(b);
		
		HashSet<Stm.T> oneBlockIn = getNewStmSet();
		HashSet<Stm.T> oneBlockOut = getNewStmSet();
		HashSet<Stm.T> oneBlockInTemp = getNewStmSet();
		if (this.blockIn.get(b) != null)
			blockInSize = this.blockIn.get(b).size();
		else
			blockInSize = 0;
		if (this.blockOut.get(b) != null)
			blockOutSize = this.blockOut.get(b).size();
		else
			blockOutSize = 0;
		
		boolean isChanged = false;
		for(Graph<Block.T>.Node node : this.pred)
		{
			BlockSingle bs = (BlockSingle)node.data;
			if(this.blockOut.get(bs) != null)
				oneBlockIn.addAll(this.blockOut.get(bs));
		}
		//assert the In
		this.blockIn.put(b, oneBlockIn);
		
		oneBlockInTemp.addAll(oneBlockIn);
		oneBlockInTemp.removeAll(oneBlockKill);
		oneBlockGen.addAll(oneBlockInTemp);
		oneBlockOut.addAll(oneBlockGen);
		//assert the Out
		this.blockOut.put(b, oneBlockOut);
		
		//is fix-point?
		if((this.blockInSize != oneBlockIn.size()) ||
				(this.blockOutSize != oneBlockOut.size()))
		{
			this.blockInSize = oneBlockIn.size();
			this.blockOutSize = oneBlockOut.size();
			isChanged = true;
		}
		else
		{
			this.blockInSize = oneBlockIn.size();
			this.blockOutSize = oneBlockOut.size();
			isChanged = false;
		}
		
		this.blockIsChanged = this.blockIsChanged | isChanged;
	}
	
	private void calculateStmInAndOut(BlockSingle b)
	{
		HashSet<Stm.T> oneBlockIn = getNewStmSet();
		HashSet<Stm.T> oneBlockOut = getNewStmSet();
		oneBlockIn.addAll(this.blockIn.get(b));
		oneBlockOut.addAll(this.blockOut.get(b));
		//上一个Stm的out是下一个Stm的in。block的in是第一条Stm的in，block的out是transfer的out
		HashSet<Stm.T> preOut = getNewStmSet();
		preOut.addAll(oneBlockIn);
		
		for(Stm.T stm : b.stms)
		{
			HashSet<Stm.T> oneStmIn = getNewStmSet();
			HashSet<Stm.T> oneStmOut = getNewStmSet();
			HashSet<Stm.T> oneStmGen = this.stmGen.get(stm);
			HashSet<Stm.T> oneStmKill = this.stmKill.get(stm);
			HashSet<Stm.T> temp =getNewStmSet();
			
			oneStmIn.addAll(preOut);
			this.stmIn.put(stm, oneStmIn);
			
			temp.addAll(oneStmIn);
			temp.removeAll(oneStmKill);
			temp.addAll(oneStmGen);
			oneStmOut.addAll(temp);
			this.stmOut.put(stm, oneStmOut);
			
			preOut = getNewStmSet();
			preOut.addAll(oneStmOut);
		}
		
		HashSet<Stm.T> oneTransferIn = getNewStmSet();
		oneTransferIn.addAll(preOut);
		this.transferIn.put(b.transfer, oneTransferIn);
		this.transferOut.put(b.transfer, oneBlockOut);
		
	}

	// block
	@Override
	public void visit(BlockSingle b)
	{
		//TODO BlockSingle
		switch (this.bkind)
		{
		case calculateStmGenAndKill:
			calculateStmGenAndKill(b);
			break;
		case calculateBlockGenAndKill:
			calculateBlockGenAndKill(b);
			break;
		case calculateBlockInAndOut:
			calculateBlockInAndOut(b);
			break;
		case calculateStmInAndOut:
			calculateStmInAndOut(b);
			break;
		default:
			break;

		}
	}

	private void initDefset(MethodSingle m)
	{
		// initial
		this.vkind = visitorKind.initDefs;
		for (Block.T block : m.blocks)
		{
			BlockSingle bs = (BlockSingle) block;
			for (Stm.T stm : bs.stms)
			{
				stm.accept(this);
			}
		}
	}

	// method
	@Override
	public void visit(MethodSingle m)
	{
		// Five steps:
		// Step 0: for each argument or local variable "x" in the
		// method m, calculate x's definition site set def(x).
		// Your code here:
		initDefset(m);

		// Step 1: calculate the "gen" and "kill" sets for each
		// statement and transfer
		this.vkind = visitorKind.calculateGenAndKill;
		this.bkind = blockKind.calculateStmGenAndKill;
		for (Block.T block : m.blocks)
		{
			block.accept(this);
		}
		System.out.println("finished StmGenAndKill");

		// Step 2: calculate the "gen" and "kill" sets for each block.
		// For this, you should visit statements and transfers in a
		// block sequentially.
		// Your code here:
		this.bkind = blockKind.calculateBlockGenAndKill;
		for (Block.T block : m.blocks)
		{
			block.accept(this);
		}
		System.out.println("finished BlockGenAndKill");

		// Step 3: calculate the "in" and "out" sets for each block
		// Note that to speed up the calculation, you should use
		// a topo-sort order of the CFG blocks, and
		// crawl through the blocks in that order.
		// And also you should loop until a fix-point is reached.
		// Your code here:

		this.bkind = blockKind.calculateBlockInAndOut;
		// init graph
		util.Graph<Block.T> graph = new util.Graph<Block.T>(m.id + "_1");
		//use the grawGraph() 
		LivenessVisitor.drawGraph(graph, m);
		this.graph = graph;
		// 得到quasi-top
		Graph<Block.T>.Node start = this.graph.graph.getFirst();
		this.top = this.graph.dfs((Block.T) start.data, dfs.top);
		//do real work
		this.blockIn = new HashMap<Block.T,HashSet<Stm.T>>();
		this.blockOut = new HashMap<Block.T, HashSet<Stm.T>>();
		int fix_point = 0;
		this.blockIsChanged = true;
		while (blockIsChanged)
		{
			this.blockIsChanged = false;
			fix_point++;
			System.out.println("this is the " + fix_point+" time fix-point");
			
			for (Graph<Block.T>.Node node : this.top)
			{//according  to the quasi-sort
				this.pred = node.pred;
				
				BlockSingle b = (BlockSingle)node.data;
				b.accept(this);
			}
		}
		
		// Step 4: calculate the "in" and "out" sets for each
		// statement and transfer
		// Your code here:
		this.bkind = blockKind.calculateStmInAndOut;
		for(Block.T b : m.blocks)
		{
			BlockSingle bs = (BlockSingle)b;
			bs.accept(this);
		}

	}

	@Override
	public void visit(MainMethodSingle m)
	{
		// Five steps:
		// Step 0: for each argument or local variable "x" in the
		// method m, calculate x's definition site set def(x).
		// Your code here:

		// Step 1: calculate the "gen" and "kill" sets for each
		// statement and transfer

		// Step 2: calculate the "gen" and "kill" sets for each block.
		// For this, you should visit statements and transfers in a
		// block sequentially.
		// Your code here:

		// Step 3: calculate the "in" and "out" sets for each block
		// Note that to speed up the calculation, you should use
		// a topo-sort order of the CFG blocks, and
		// crawl through the blocks in that order.
		// And also you should loop until a fix-point is reached.
		// Your code here:

		// Step 4: calculate the "in" and "out" sets for each
		// statement and transfer
		// Your code here:
	}

	// vtables
	@Override
	public void visit(VtableSingle v)
	{
	}

	// class
	@Override
	public void visit(ClassSingle c)
	{
	}

	// program
	@Override
	public void visit(ProgramSingle p)
	{
		for (Method.T mth : p.methods)
		{
			mth.accept(this);
		}
		return;
	}

}
