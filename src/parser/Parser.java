package parser;

import javax.xml.namespace.QName;

import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

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

  // ExpList -> Exp ExpRest*
  // ->
  // ExpRest -> , Exp
  private void parseExpList()
  {
    if (current.kind == Kind.TOKEN_RPAREN)    // 啥意思？
      return;
    parseExp();
    while (current.kind == Kind.TOKEN_COMMER) { // 逗号 ','
      advance();    // 跳过逗号
      parseExp();
    }
    return;
  }

  // AtomExp -> (exp)
  // -> INTEGER_LITERAL    也即NUM
  // -> true
  // -> false
  // -> this
  // -> id
  // -> new int [exp]
  // -> new id ()
  private void parseAtomExp()
  {
    switch (current.kind) {
    case TOKEN_LPAREN:
      advance();
      parseExp();
      eatToken(Kind.TOKEN_RPAREN);    // 在这里可以报错
      return;
    case TOKEN_NUM:
      advance();
      return;
    case TOKEN_TRUE:
      advance();
      return;
    case TOKEN_FALSE:
      advance();
      return;
    case TOKEN_THIS:
      advance();
      return;
    case TOKEN_ID:
      advance();
      return;
    case TOKEN_NEW: {
      advance();
      switch (current.kind) {
      case TOKEN_INT:
        advance();
        eatToken(Kind.TOKEN_LBRACK);
        parseExp();
        eatToken(Kind.TOKEN_RBRACK);
        return;
      case TOKEN_ID:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        eatToken(Kind.TOKEN_RPAREN);
        return;
      default:
        System.out.println("Func: parseAtomExp, Keyword : new");
        error();
        return;
      }
    }
    default:
      System.out.println("Func: parseAtomExp"); 
      error();
      return;
    }
  }

  // NotExp -> AtomExp
  // -> AtomExp .id (expList)   使用成员函数
  // -> AtomExp [exp]   数组操作
  // -> AtomExp .length  得到数组长度
  private void parseNotExp()
  {
    parseAtomExp();
    while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
      if (current.kind == Kind.TOKEN_DOT) {
        advance();
        if (current.kind == Kind.TOKEN_LENGTH) {
          advance();
          return;
        }
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        parseExpList();
        eatToken(Kind.TOKEN_RPAREN);
      } else {
        advance();    // eatToken(Kind.TOKEN_LBRACK)
        parseExp();   // Explist ?
        eatToken(Kind.TOKEN_RBRACK);
      }
    }
    return;
  }

  // TimesExp -> ! TimesExp
  // -> NotExp
  private void parseTimesExp()
  {
    while (current.kind == Kind.TOKEN_NOT) {
      advance();
    }
    parseNotExp();
    return;
  }

  // AddSubExp -> TimesExp * TimesExp
  // -> TimesExp
  private void parseAddSubExp()
  {
    parseTimesExp();
    while (current.kind == Kind.TOKEN_TIMES) {
      advance();
      parseTimesExp();
    }
    return;
  }

  // LtExp -> AddSubExp + AddSubExp
  // -> AddSubExp - AddSubExp
  // -> AddSubExp
  private void parseLtExp()
  {
    parseAddSubExp();
    while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
      advance();
      parseAddSubExp();
    }
    return;
  }

  // AndExp -> LtExp < LtExp
  // -> LtExp
  private void parseAndExp()
  {
    parseLtExp();
    while (current.kind == Kind.TOKEN_LT) {
      advance();
      parseLtExp();
    }
    return;
  }

  // Exp -> AndExp && AndExp
  // -> AndExp
  private void parseExp()
  {
    parseAndExp();
    while (current.kind == Kind.TOKEN_AND) {
      advance();
      parseAndExp();
    }
    return;
  }

  // Statement -> { Statement* }
  // -> if ( Exp ) Statement else Statement
  // -> while ( Exp ) Statement
  // -> System.out.println ( Exp ) ;
  // -> id = Exp ;
  // -> id [ Exp ]= Exp ;
  private void parseStatement()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a statement.
    // new util.Todo();
    switch(current.kind)
    {
      case TOKEN_LBRACE:
        advance();
        parseStatements();
        eatToken(Kind.TOKEN_RBRACE);
        return ;
      case TOKEN_IF:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        parseStatement();
        eatToken(Kind.TOKEN_ELSE);
        parseStatement();
        return ;
      case TOKEN_WHILE:
        advance();
        eatToken(Kind.TOKEN_LPAREN);
        parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        parseStatement();
        return ;
      case TOKEN_SYSTEM:
        advance();
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_OUT);
        eatToken(Kind.TOKEN_DOT);
        eatToken(Kind.TOKEN_PRINTLN);
        eatToken(Kind.TOKEN_LPAREN);
        parseExp();
        eatToken(Kind.TOKEN_RPAREN);
        eatToken(Kind.TOKEN_SEMI);
        return ;
      case TOKEN_ID:
        if(rollback)
        {
          rollback = false;
          // 恢复current的token类型
          current.kind = rollbackToken.kind;
        }
        else advance();
        switch(current.kind)
        {
          case TOKEN_ASSIGN:
            advance();
            parseExp();
            eatToken(Kind.TOKEN_SEMI);
            return ;
          case TOKEN_LBRACK:
            advance();
            parseExp();
            eatToken(Kind.TOKEN_RBRACK);
            eatToken(Kind.TOKEN_ASSIGN);
            parseExp();
            eatToken(Kind.TOKEN_SEMI);
            return ;
          default:
            System.out.println("Func: parseStm, KeywordType: " + current.kind);
            error();
            return ;
        }
      default:
        System.out.println("Func: parseStm");
        error();
        return ;
    }
  }

  // Statements -> Statement Statements
  // ->

  // 闭包
  private void parseStatements()
  {
    while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
        || current.kind == Kind.TOKEN_WHILE
        || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
      parseStatement();
    }
    return;
  }

  // Type -> int []
  // -> boolean
  // -> int
  // -> id
  // MiniJava只有四种变量类型: 整形, 布尔, 数组, 对象
  private void parseType()
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
        }
        return ;
      case TOKEN_BOOLEAN:
        advance();
        return ;
      case TOKEN_ID:
        advance();
        return ;
      default:
        System.out.println("Func: parseType");
        error();
        return ;
    }
  }

  // VarDecl -> Type id ;
  private void parseVarDecl()
  {
    // to parse the "Type" nonterminal in this method, instead of writing
    // a fresh one.
    // BUG1 : 如果在声明之后接上  i = 1;便会出现eatToken(Kind.TOKEN_ID)错误， 究其原因是因为parseType时
    // advance了id
    // 
    rollbackToken = current;
    parseType();
    if(current.kind == Kind.TOKEN_ID)
    {
      advance();
      eatToken(Kind.TOKEN_SEMI);
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
    }
    return;
  }

  // VarDecls -> VarDecl VarDecls
  // ->
  // 闭包
  private void parseVarDecls()
  {
    while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      if(rollback) return ;
      parseVarDecl();
    }
    return;
  }

  // FormalList -> Type id FormalRest*
  // ->
  // FormalRest -> , Type id

  // int a, int b, int c  函数传参
  // 消除左递归
  private void parseFormalList()
  {
    if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      parseType();
      eatToken(Kind.TOKEN_ID);
      while (current.kind == Kind.TOKEN_COMMER) {
        advance();
        parseType();
        eatToken(Kind.TOKEN_ID);
      }
    }
    return;
  }

  // Method -> public Type id ( FormalList )
  // { VarDecl* Statement* return Exp ;}
  // 方法内先有声明, 后有语句执行
  // 这就规定了变量声明必须放在方法最前面?
  private void parseMethod()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a method.
    // new util.Todo();
    eatToken(Kind.TOKEN_PUBLIC);
    parseType();
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_LPAREN);
    parseFormalList();
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);
    parseVarDecls();
    parseStatements();
    eatToken(Kind.TOKEN_RETURN);
    parseExp();
    eatToken(Kind.TOKEN_SEMI);
    eatToken(Kind.TOKEN_RBRACE);
    return;
  }

  // MethodDecls -> MethodDecl MethodDecls
  // ->

  // 闭包
  private void parseMethodDecls()
  {
    // 方法只能是public来修饰
    while (current.kind == Kind.TOKEN_PUBLIC) {
      parseMethod();
    }
    return;
  }

  // ClassDecl -> class id { VarDecl* MethodDecl* }
  // -> class id extends id { VarDecl* MethodDecl* }
  private void parseClassDecl()
  {
    eatToken(Kind.TOKEN_CLASS);
    eatToken(Kind.TOKEN_ID);
    if (current.kind == Kind.TOKEN_EXTENDS) {
      eatToken(Kind.TOKEN_EXTENDS);
      eatToken(Kind.TOKEN_ID);
    }
    eatToken(Kind.TOKEN_LBRACE);
    parseVarDecls();
    parseMethodDecls();
    eatToken(Kind.TOKEN_RBRACE);
    return;
  }

  // ClassDecls -> ClassDecl ClassDecls
  // ->
  private void parseClassDecls()
  {
    while (current.kind == Kind.TOKEN_CLASS) {
      parseClassDecl();
    }
    return;
  }

  // MainClass -> class id
  // {
  // public static void main ( String [] id )
  // {
  // Statement
  // }
  // }
  private void parseMainClass()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a main class as described by the
    // grammar above.
    // new util.Todo();
    eatToken(Kind.TOKEN_CLASS);
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
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_RPAREN);
    eatToken(Kind.TOKEN_LBRACE);
    parseStatements();
    eatToken(Kind.TOKEN_RBRACE);
    eatToken(Kind.TOKEN_RBRACE);
  }

  // Program -> MainClass ClassDecl*
  // 文件尾部会打印两次EOF, 是因为parseClassDecls()后nextToken()就已经是EOF
  // eatToken(Kind.TOKEN_EOF)还会再打印一次
  private void parseProgram()
  {
    parseMainClass();
    parseClassDecls();
    eatToken(Kind.TOKEN_EOF);
    return;
  }

  public void parse()
  {
    parseProgram();
    return;
  }
}
