package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import cfg.Cfg.Block;
import cfg.Cfg.Block.T;

public class Graph<X>
{
	public enum dfs {retop,top};
	public dfs dfsKind = dfs.retop;
	// graph node
	public class Node
	{
		public X data;
		public LinkedList<Edge> edges;
		public Integer indegree;
		public Integer outdegree;

		public ArrayList<Node> pred;	//这个节点的前驱
		public ArrayList<Node> succ;	//这个节点的后继

		public Node()
		{
			this.data = null;
			this.edges = null;
			this.indegree = 0;
			this.outdegree = 0;
			this.pred = new ArrayList<Node>();
			this.succ = new ArrayList<Node>();
		}

		public Node(X data)
		{
			this.data = data;// 放block
			this.edges = new LinkedList<Edge>();
			this.indegree = 0;
			this.outdegree = 0;
			this.pred = new ArrayList<Node>();
			this.succ = new ArrayList<Node>();
		}

		@Override
		public String toString()
		{
			return data.toString();
		}

		public HashSet<Node> getNeighbors()
		{
			HashSet<Node> nei = new HashSet<Node>();
			for (Edge e : this.edges)
			{
				nei.add(e.to);
			}
			return nei;
		}
	}

	// graph edge
	public class Edge
	{

		public Node from;
		public Node to;

		public Edge(Node from, Node to)
		{
			this.from = from;
			this.to = to;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == null)
				return false;
			if (!(o instanceof Graph.Edge))
				return false;

			return (this == o);
		}

		@Override
		public String toString()
		{
			return (this.from.toString() + "->" + this.to.toString());
		}
	}

	// the graph
	public LinkedList<Node> graph;	// block的链表
	public String gname;

	public Graph(String name)
	{
		this.gname = name;
		this.graph = new LinkedList<Node>();
	}

	public void delNode(X data)
	{
		Node node = this.lookupNode(data);
		if (node == null)
			new util.Bug();
		this.delNode(node);
	}

	private void delNode(Node node)
	{
		// 先处理边
		for (Edge edge : node.edges)
		{
			if (edge.to != null)
				edge.to.indegree--;
			if (edge.from != null)		// ??? 当前node的edges中from只可能为自身
				edge.from.outdegree--;
		}
		this.graph.remove(node);
	}

	private void addNode(Node node)
	{// 在最后加入node，主要是给下面的方法调用
		this.graph.addLast(node);
	}

	public void addNode(X data)
	{
		for (Node n : this.graph)
			if (n.data.equals(data))
				new util.Bug();

		Node node = new Node(data);
		this.addNode(node);
	}

	public Node lookupNode(X data)
	{// 查找node
		for (Node node : this.graph)
		{
			if (node.data.equals(data))
				return node;
		}
		return null;
	}

	private void addEdge(Node from, Node to)
	{
		from.outdegree ++;	// from的出度++
		to.indegree  ++;	// to的入度++
		from.edges.addLast(new Edge(from, to));
		if(!from.succ.contains(to))
			from.succ.add(to);
		if(!to.pred.contains(from))
			to.pred.add(from);
	}

	public void addEdge(X from, X to)
	{// 加入边
		Node f = this.lookupNode(from);
		Node t = this.lookupNode(to);
		if (f == null || t == null)
			new util.Bug();
		this.addEdge(f, t);
	}

	private ArrayList<Node> dfsDoit(Node start, HashSet<Node> visited, ArrayList<Node> retop)
	{
		// visited.add(start);
		for (Edge edge : start.edges)
		{
			// 这个节点的edge指向的节点没有在visited里面，就从指向的那个节点开始,继续DFS
			// 如果已经在，那就换一条边继续
			if (!visited.contains(edge.to))
			{
				visited.add(edge.to);
				retop = dfsDoit(edge.to, visited, retop);
			}
		}
		retop.add(start);
		return retop;
	}
	
	private ArrayList<Node> dfsDoitTop(Node start, HashSet<Node> visited, ArrayList<Node> retop)
	{
		retop.add(start);
		for (Edge edge : start.edges)
		{
			// 这个节点的edge指向的节点没有在visited里面，就从指向的那个节点开始,继续DFS
			// 如果已经在，那就换一条边继续
			if (!visited.contains(edge.to))
			{
				visited.add(edge.to);
				retop = dfsDoitTop(edge.to, visited, retop);
			}
		}
		return retop;
	}

	/**
	 * 深度优先遍历
	 */
	public ArrayList<Node> dfs(X start, dfs dfsKind)
	{

		// 先查找node
		Node startNode = this.lookupNode(start);
		if (startNode == null)
			new util.Bug();

		// 用Set保存访问过的节点。
		java.util.HashSet<Node> visited = new java.util.HashSet<Node>();
		java.util.ArrayList<Node> retop = new java.util.ArrayList<Node>();
		// 开始DFS
		switch (dfsKind)
		{
		case retop:
			retop = this.dfsDoit(startNode, visited, retop);
			break;
		case top:
			retop = this.dfsDoitTop(startNode, visited, retop);
			break;
		default:
				break;
		}
		return retop;
	}

	public void visualize()
	{
		Dot dot = new Dot();
		String fname;

		fname = this.gname;

		for (Node node : this.graph)
		{
			for (Edge edge : node.edges)
			{
				dot.insert(edge.from.toString(), edge.to.toString());
			}
		}

		try
		{
			dot.visualize(fname);
		} catch (Exception e)
		{
			e.printStackTrace();
			new util.Bug();
		}
	}

	HashSet<Graph<T>.Node> visited = new HashSet<Graph<T>.Node>();
	HashMap<Block.T, ArrayList<Block.T>> cycle = new HashMap<Block.T, ArrayList<Block.T>>();

	/**
	 * 拓扑排序
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<X> topSort()
	{
		ArrayList<X> top = new ArrayList<X>();
		Queue<Node> q = new LinkedList<Node>();
		HashSet<Node> neighbors = new HashSet<Node>();
		util.Graph<T> g = null;
		try
		{
			g = (Graph<T>) Clone.clone(this);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		for (Graph<T>.Node n : g.graph)
		{
			if (n.indegree == 0)
			{
				q.add((Graph<X>.Node) n);
				top.add((X) n.data);
			}

		}
		while (!q.isEmpty())
		{
			Node qn = q.poll();
			// System.out.println(qn);

			for (Edge edge : qn.edges)
			{
				if (g.graph.contains(edge.to))
					neighbors.add(edge.to);
			}
			// 这个getNeighbors方法有问题。返回的还是原图的nei
			// neighbors = qn.getNeighbors();
			g.graph.remove(qn);
			for (Node nn : neighbors)
			{
				nn.indegree--;
				if (nn.indegree == 0)
				{
					q.add(nn);
					top.add(nn.data);
					g.graph.remove(nn);
				}
			}
		}

		if (q.size() != g.graph.size())
		{
			throw new IllegalStateException("存在环");
		}
		return top;
	}
}
