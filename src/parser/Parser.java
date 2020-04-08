package parser;

import ast.Ast;
import ast.Visitor;
import javax.xml.namespace.QName;

import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;
import java.util.LinkedList;

public class Parser
{
  Lexer lexer;
  Token current;
  // 加入回溯相关的数据
  boolean rollback;
  Token rollbackToken;
  // Parser创建好之后就先读一个token进来
  public Parser(String fname, java.io.InputStream fstream)
  {
    lexer = new Lexer(fname, fstream);
    current = lexer.nextToken();
    rollback = false;
    rollbackToken = new Token(null, -1, null);
  }

  // /////////////////////////////////////////////
  // utility methods to connect the lexer
  // and the parser.

  private void advance()
  {
    current = lexer.nextToken();
  }

  private void eatToken(Kind kind)
  {
    if (kind == current.kind)
      advance();
    else {
      System.out.println("Expects: " + kind.toString());
      System.out.println("But got: " + current.kind.toString() + " " + current.lexeme);
      System.out.println("At line " + current.lineNum);
      System.exit(1);
    }
  }
  // todo : 恢复错误以报告所有的错误
  private void error()
  {
    System.out.println("Syntax error: compilation aborting...");
    System.out.println("Current Token: " + current.toString());
    System.out.println("At line " + current.lineNum);
    System.exit(1);
    return;
  }

  // ////////////////////////////////////////////////////////////
  // below are method for parsing.

  // A bunch of parsing methods to parse expressions. The messy
  // parts are to deal with precedence and associativity. 优先级和关联性
  // 经过对Exp各个子类的处理, 当前文法已经是CFG

  // ExpList -> Exp ExpRest*
  // ->
  // ExpRest -> , Exp
  private LinkedList<Ast.Exp.T> parseExpList()
  {
    LinkedList<Ast.Exp.T> expList = new LinkedList<>();
    if (current.kind == Kind.TOKEN_RPAREN)    // 啥意思？
      return expList;
    expList.add(parseExp());
    while (current.kind == Kind.TOKEN_COMMER) { // 逗号 ','
      advance();    // 跳过逗号
      expList.add(parseExp());
    }
    return expList;
  }

  // AtomExp -> (exp)
  // -> INTEGER_LITERAL    也即NUM
  // -> true
  // -> false
  // -> this
  // -> id
  // -> new int [exp]
  // -> new id ()
  private Ast.Exp.T parseAtomExp()
  {
    String id;
    int line;
    switch (current.kind) {
    case TOKEN_LPAREN:
      advance();
      Ast.Exp.T e = parseExp();
      eatToken(Kind.TOKEN_RPAREN);    // 在这里可以报错
      return e;
    case TOKEN_NUM:
      line = current.lineNum;
      int num = Integer.parseInt(current.lexeme);
      advance();
      return new Ast.Exp.Num(num, line);
    case TOKEN_TRUE:
      line = current.lineNum;
      advance();
      return new Ast.Exp.True(line);
    case TOKEN_FALSE:
      line = current.lineNum;
      advance();
      return new Ast.Exp.False(line);
    case TOKEN_THIS:
      line = current.lineNum;
      advance();
      return new Ast.Exp.This(line);
    case TOKEN_ID:
      id = current.lexeme;
      line = current.lineNum;
      advance();
      return new Ast.Exp.Id(id, line);
    case TOKEN_NEW: {
      advance();
      switch (current.kind) {
      case TOKEN_INT:
        advance();
        eatToken(Kind.TOKEN_LBRACK);
        line = current.lineNum;
        Ast.Exp.T ee = parseExp();
        eatToken(Kind.TOKEN_RBRACK);
        return new Ast.Exp.NewIntArray(ee, line);
      case TOKEN_ID:
        id = current.lexeme;
        line = current.lineNum;
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        eatToken(Kind.TOKEN_RPAREN);
        return new Ast.Exp.NewObject(id, line);
      default:
        System.out.println("Func: parseAtomExp, Keyword : new");
        error();
        return null;
      }
    }
    default:
      System.out.println("Func: parseAtomExp"); 
      error();
      return null;
    }
  }
  // 最高运算符优先级
  // NotExp -> AtomExp
  // -> AtomExp .id (expList)   使用成员函数
  // -> AtomExp [exp]   数组操作
  // -> AtomExp .length  得到数组长度
  private Ast.Exp.T parseNotExp()
  {
    int line = current.lineNum;
    Ast.Exp.T atomExp = parseAtomExp();
    while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
      if (current.kind == Kind.TOKEN_DOT) {
        advance();
        if (current.kind == Kind.TOKEN_LENGTH) {
          advance();
          return new Ast.Exp.Length(atomExp, line);
        }
        String id = current.lexeme;
        line = current.lineNum;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        LinkedList<Ast.Exp.T> args = parseExpList();
        eatToken(Kind.TOKEN_RPAREN);
        return new Ast.Exp.Call(atomExp, id, args, line);
      } else {
        advance();    // eatToken(Kind.TOKEN_LBRACK)
        line = current.lineNum;
        Ast.Exp.T index = parseExp();  // Explist ?
        eatToken(Kind.TOKEN_RBRACK);
        return new Ast.Exp.ArraySelect(atomExp, index, line);
      }
    }
    return atomExp;
  }

  // TimesExp -> ! TimesExp
  // -> NotExp
  private Ast.Exp.T parseTimesExp()
  {
    while (current.kind == Kind.TOKEN_NOT) {
      int line = current.lineNum;
      advance();
      // 递归调用
      return new Ast.Exp.Not(parseTimesExp(), line);
    }
    return parseNotExp();
  }

  // AddSubExp -> TimesExp * TimesExp
  // -> TimesExp
  private Ast.Exp.T parseAddSubExp()
  {
    // 这里不支持 eg : 1 * 2 * 3 * 4 这样不带括号的连乘的语法特性
    Ast.Exp.T left = parseTimesExp();
    while (current.kind == Kind.TOKEN_TIMES) {
        int line = current.lineNum;
        advance();
        Ast.Exp.T right = parseTimesExp();
        return new Ast.Exp.Times(left, right, line);
    }
    return left;
  }

  // LtExp -> AddSubExp + AddSubExp
  // -> AddSubExp - AddSubExp
  // -> AddSubExp
  private Ast.Exp.T parseLtExp()
  {
    Ast.Exp.T left = parseAddSubExp();
    while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
        int line = current.lineNum;
        if (current.kind == Kind.TOKEN_ADD) {
            advance();
            Ast.Exp.T right = parseAddSubExp();
            return new Ast.Exp.Add(left, right, line);
        } else {
            advance();
            Ast.Exp.T right = parseAddSubExp();
            return new Ast.Exp.Sub(left, right, line);
        }
    }
    return left;
  }

  // AndExp -> LtExp < LtExp
  // -> LtExp
  private Ast.Exp.T parseAndExp()
  {
    Ast.Exp.T left = parseLtExp();
    while (current.kind == Kind.TOKEN_LT) {
        int line = current.lineNum;
        advance();
        Ast.Exp.T right = parseLtExp();
        return new Ast.Exp.Lt(left, right, line);
    }
    return left;
  }

  // Exp -> AndExp && AndExp
  // -> AndExp
  private Ast.Exp.T parseExp()
  {
    Ast.Exp.T andExp = parseAndExp();
    while (current.kind == Kind.TOKEN_AND) {
        int line = current.lineNum;
        advance();
        Ast.Exp.T andExpp = parseAndExp();
        return new Ast.Exp.And(andExp, andExpp, line);
    }
    return andExp;
  }

  // Statement不包含return语句
  // Statement -> { Statement* }
  // -> if ( Exp ) Statement else Statement
  // -> while ( Exp ) Statement
  // -> System.out.println ( Exp ) ;
  // -> id = Exp ;
  // -> id [ Exp ]= Exp ;
  private Ast.Stm.T parseStatement()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a statement.
    // new util.Todo();
    int line;
    switch(current.kind)
    {
      case TOKEN_LBRACE:
        advance();
        LinkedList<Ast.Stm.T> stms = parseStatements();
        eatToken(Kind.TOKEN_RBRACE);
        return new Ast.Stm.Block(stms);
      case TOKEN_IF:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        line = current.lineNum;   
        Ast.Exp.T condition = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        Ast.Stm.T thenn = parseStatement();
        eatToken(Kind.TOKEN_ELSE);
        Ast.Stm.T elsee = parseStatement();
        return new Ast.Stm.If(condition, thenn, elsee, line);
      case TOKEN_WHILE:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        line = current.lineNum;
        Ast.Exp.T cond = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        Ast.Stm.T body = parseStatement();
        return new Ast.Stm.While(cond, body, line);
      case TOKEN_SYSTEM:
        advance();
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_OUT);
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_PRINTLN);
        eatToken(Kind.TOKEN_LPAREN);
        line = current.lineNum;
        Ast.Exp.T exp = parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_SEMI);
        return new Ast.Stm.Print(exp, line);
      case TOKEN_ID:
        String id;
        if(rollback)
        {
          rollback = false;
          // 恢复current的token类型
          current.kind = rollbackToken.kind;
          line = rollbackToken.lineNum;
          id = rollbackToken.lexeme;
        }
        else 
        {
          line = current.lineNum;
          id = current.lexeme;
          advance();
        }
        switch(current.kind)
        {
          case TOKEN_ASSIGN:
            advance();
            Ast.Exp.T exp1 = parseExp();
            eatToken(Kind.TOKEN_SEMI); 
            return new Ast.Stm.Assign(id, exp1, line);
          case TOKEN_LBRACK:
            advance();
            Ast.Exp.T exp2 = parseExp();
            eatToken(Kind.TOKEN_RBRACK);
            eatToken(Kind.TOKEN_ASSIGN);
            Ast.Exp.T exp3 = parseExp();
            eatToken(Kind.TOKEN_SEMI);
            return new Ast.Stm.AssignArray(id, exp2, exp3, line);
          default:
            System.out.println("Func: parseStm, KeywordType: " + current.kind);
            error();
            return null;
        }
      default:
        System.out.println("Func: parseStm");
        error();
        return null;
    }
  }

  // Statements -> Statement Statements
  // ->

  // 闭包
  private LinkedList<Ast.Stm.T> parseStatements()
  {
    LinkedList<Ast.Stm.T> stms = new LinkedList<>();
    while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
        || current.kind == Kind.TOKEN_WHILE
        || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
      stms.add(parseStatement());
    }
    return stms;
  }

  // Type -> int []
  // -> boolean
  // -> int
  // -> id
  // MiniJava只有四种变量类型: 整形, 布尔, 数组, 对象
  private Ast.Type.T parseType()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a type.
    // new util.Todo();
    switch(current.kind)
    {
      case TOKEN_INT:
        advance();
        if(current.kind == Kind.TOKEN_LBRACK)
        {
          eatToken(Kind.TOKEN_LBRACK);
          eatToken(Kind.TOKEN_RBRACK);
          return new Ast.Type.IntArray();
        }
        return new Ast.Type.Int();
      case TOKEN_BOOLEAN:
        advance();
        return new Ast.Type.Boolean();
      case TOKEN_ID:
        String id = current.lexeme;
        int line = current.lineNum;
        advance();
        return new Ast.Type.ClassType(id, line);
      default:
        // debug
        System.out.println(current.toString());
        System.out.println("Func: parseType");
        error();
        return null;
    }
  }

  // VarDecl -> Type id ;
  private Ast.Dec.T parseVarDecl()
  {
    // to parse the "Type" nonterminal in this method, instead of writing
    // a fresh one.
    // BUG1 : 如果在声明之后接上  i = 1;便会出现eatToken(Kind.TOKEN_ID)错误， 究其原因是因为parseType时
    // advance了id
    // 
    rollbackToken = current;
    Ast.Type.T type = parseType();
    if(current.kind == Kind.TOKEN_ID)
    {
      String id = current.lexeme;
      advance();
      eatToken(Kind.TOKEN_SEMI);
      return new Ast.Dec.DecSingle(type, id);
    }
    else if(current.kind == Kind.TOKEN_ASSIGN)
    {
      // deal with the following situation
      // int i;
      // i = 0;
      // prevent backtracking
      rollback = true;
      // 为了parseStm可以正常执行, 需要将current的token类型设置为id
      rollbackToken.kind = current.kind;
      current.kind = Kind.TOKEN_ID;
      System.out.println("backtrack for id: " + rollbackToken.lexeme);
      return null;
    }
    return null;
  }

  // VarDecls -> VarDecl VarDecls
  // ->
  // 闭包
  private LinkedList<Ast.Dec.T> parseVarDecls()
  {
    LinkedList<Ast.Dec.T> decs = new LinkedList<>();
    while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      if(rollback) return decs;
      Ast.Dec.T dec = parseVarDecl();
      if (dec != null) decs.add(dec);
    }
    return decs;
  }

  // FormalList -> Type id FormalRest*
  // ->
  // FormalRest -> , Type id

  // int a, int b, int c  函数传参
  // 消除左递归
  private LinkedList<Ast.Dec.T> parseFormalList()
  {
    LinkedList<Ast.Dec.T> formals = new LinkedList<>();
    if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      Ast.Type.T type = parseType();
      String id = current.lexeme;
      eatToken(Kind.TOKEN_ID);
      formals.add(new Ast.Dec.DecSingle(type, id));
      while (current.kind == Kind.TOKEN_COMMER) {
        advance();
        type = parseType();
        id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        formals.add(new Ast.Dec.DecSingle(type, id));
      }
    }
    return formals;
  }

  // Method -> public Type id ( FormalList )
  // { VarDecl* Statement* return Exp ;}
  // 方法内先有声明, 后有语句执行
  // 这就规定了变量声明必须放在方法最前面?
  private Ast.Method.T parseMethod()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a method.
    // new util.Todo();
    eatToken(Kind.TOKEN_PUBLIC);
    Ast.Type.T retType = parseType();
    String id = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_LPAREN);
    LinkedList<Ast.Dec.T> formals = parseFormalList();
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);
    LinkedList<Ast.Dec.T> locals = parseVarDecls();
    LinkedList<Ast.Stm.T> stms = parseStatements();
    eatToken(Kind.TOKEN_RETURN);
    int line = current.lineNum;
    Ast.Exp.T retExp = parseExp();
    eatToken(Kind.TOKEN_SEMI);
    eatToken(Kind.TOKEN_RBRACE);
    return new Ast.Method.MethodSingle(retType, id, formals, locals, stms, retExp, line);
  }

  // MethodDecls -> MethodDecl MethodDecls
  // ->

  // 闭包
  private LinkedList<Ast.Method.T> parseMethodDecls()
  {
    // 方法只能是public来修饰
    LinkedList<Ast.Method.T> methods = new LinkedList<>();
    while (current.kind == Kind.TOKEN_PUBLIC) {
      Ast.Method.T method = parseMethod();
      methods.add(method);
    }
    return methods;
  }

  // ClassDecl -> class id { VarDecl* MethodDecl* }
  // -> class id extends id { VarDecl* MethodDecl* }
  private Ast.Class.T parseClassDecl()
  {
    eatToken(Kind.TOKEN_CLASS);
    String id = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    String extendss = null;
    if (current.kind == Kind.TOKEN_EXTENDS) {
      eatToken(Kind.TOKEN_EXTENDS);
      extendss = current.lexeme;
      eatToken(Kind.TOKEN_ID);
    }
    eatToken(Kind.TOKEN_LBRACE);
    LinkedList<Ast.Dec.T> decs = parseVarDecls();
    LinkedList<Ast.Method.T> methods = parseMethodDecls();
    eatToken(Kind.TOKEN_RBRACE);
    return new Ast.Class.ClassSingle(id, extendss, decs, methods);
  }

  // ClassDecls -> ClassDecl ClassDecls
  // ->
  private LinkedList<Ast.Class.T> parseClassDecls()
  {
    LinkedList<Ast.Class.T> classes = new LinkedList<>();
    while (current.kind == Kind.TOKEN_CLASS) {
      classes.add(parseClassDecl());
    }
    return classes;
  }

  // MainClass -> class id
  // {
  // public static void main ( String [] id )
  // {
  // Statement
  // }
  // }
  private Ast.MainClass.T parseMainClass()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a main class as described by the
    // grammar above.
    // new util.Todo();
    eatToken(Kind.TOKEN_CLASS);
    String id = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_LBRACE);
    eatToken(Kind.TOKEN_PUBLIC);
    eatToken(Kind.TOKEN_STATIC);
    eatToken(Kind.TOKEN_VOID);
    eatToken(Kind.TOKEN_MAIN);
    eatToken(Kind.TOKEN_LPAREN);
    eatToken(Kind.TOKEN_STRING);
    eatToken(Kind.TOKEN_LBRACK);
    eatToken(Kind.TOKEN_RBRACK);
    String args = current.lexeme;
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);
    // 这里只能是单语句
    Ast.Stm.T stm = parseStatement();
    eatToken(Kind.TOKEN_RBRACE);
    eatToken(Kind.TOKEN_RBRACE);
    return new Ast.MainClass.MainClassSingle(id, args, stm);
  }

  // Program -> MainClass ClassDecl*
  // 文件尾部会打印两次EOF, 是因为parseClassDecls()后nextToken()就已经是EOF
  // eatToken(Kind.TOKEN_EOF)还会再打印一次
  private Ast.Program.T parseProgram()
  {
    Ast.MainClass.T main = parseMainClass();
    LinkedList<Ast.Class.T> classes = parseClassDecls();
    eatToken(Kind.TOKEN_EOF);
    return new Ast.Program.ProgramSingle(main, classes);
  }

  public ast.Ast.Program.T parse()
  {
    return parseProgram();
    // return null;
  }
}
