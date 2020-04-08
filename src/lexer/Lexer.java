package lexer;

import static control.Control.ConLexer.dump;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lexer.Token.Kind;
import util.Todo;

public class Lexer
{
  String fname; // the input file name to be compiled
  InputStream fstream; // input stream for the above file


  private Map<String, Kind> keywords = new HashMap<>();  // 关键字集合
  // line number
  private int lineNum;

  // next char
  private int lookahead;

  public Lexer(String fname, InputStream fstream)
  {
    this.fname = fname;
    this.fstream = fstream;
    this.lineNum = 1;
    this.lookahead = 0;   // 0代表了EOF
    // initialize keywords
    keywords.put("boolean", Kind.TOKEN_BOOLEAN);
    keywords.put("class", Kind.TOKEN_CLASS);
    keywords.put("else", Kind.TOKEN_ELSE);
    keywords.put("extends", Kind.TOKEN_EXTENDS);
    keywords.put("false", Kind.TOKEN_FALSE);
    keywords.put("if", Kind.TOKEN_IF);
    keywords.put("int", Kind.TOKEN_INT);
    keywords.put("length", Kind.TOKEN_LENGTH);
    keywords.put("main", Kind.TOKEN_MAIN);
    keywords.put("new", Kind.TOKEN_NEW);
    keywords.put("out", Kind.TOKEN_OUT);
    keywords.put("println", Kind.TOKEN_PRINTLN);
    keywords.put("public", Kind.TOKEN_PUBLIC);
    keywords.put("return", Kind.TOKEN_RETURN);
    keywords.put("static", Kind.TOKEN_STATIC);
    keywords.put("String", Kind.TOKEN_STRING);
    keywords.put("System", Kind.TOKEN_SYSTEM);
    keywords.put("this", Kind.TOKEN_THIS);
    keywords.put("true", Kind.TOKEN_TRUE);
    keywords.put("void", Kind.TOKEN_VOID);
    keywords.put("while", Kind.TOKEN_WHILE);
  }

  // When called, return the next token (refer to the code "Token.java")
  // from the input stream.
  // Return TOKEN_EOF when reaching the end of the input stream.
  private Token nextTokenInternal() throws Exception
  {
    // 一次读一个字节
    int c;
    if (lookahead == 0) {
        c = this.fstream.read();
    } else {
        c = lookahead;
        //lookahead = this.fstream.read();
    }

    if (-1 == c)
      // The value for "lineNum" is now "null",
      // you should modify this to an appropriate
      // line number for the "EOF" token.
      return new Token(Kind.TOKEN_EOF, this.lineNum);

    // skip all kinds of "blanks"
    while (' ' == c || '\t' == c || '\r' == c || c == '\n' || c == '/') {
      if(c == '\n') // \n 和 \r 连在一起？
        this.lineNum ++;
      int tmp = c;
      c = this.fstream.read();
      // 在这里处理注释的情况, 凡是注释全部都忽略掉
      if(tmp == '/' && c == '/'){
        while(c != '\n') c = this.fstream.read();
      }
      if(tmp == '/' && c == '*'){
        c = this.fstream.read();
        while(true){
          tmp = c;
          c = this.fstream.read();
          if(tmp == '*' && c == '/'){
            c = this.fstream.read();
            break;
          }
        }
      }
      
    }
    lookahead = this.fstream.read();

    if (-1 == c)
      return new Token(Kind.TOKEN_EOF, this.lineNum);

    // 先处理字符的情况
    switch (c) {
    case '+':
      return new Token(Kind.TOKEN_ADD, this.lineNum);
    case '&':
      if(lookahead == '&') return new Token(Kind.TOKEN_AND, this.lineNum);
      //else return null; // 只有一个&报错
    case '=':
        return new Token(Kind.TOKEN_ASSIGN, this.lineNum);
    case ',':
        return new Token(Kind.TOKEN_COMMER, this.lineNum);
    case '.':
        return new Token(Kind.TOKEN_DOT, this.lineNum);
    case '{':
        return new Token(Kind.TOKEN_LBRACE, this.lineNum);
    case '[':
        return new Token(Kind.TOKEN_LBRACK, this.lineNum);
    case '(':
        return new Token(Kind.TOKEN_LPAREN, this.lineNum);
    case '<':
        return new Token(Kind.TOKEN_LT, this.lineNum);
    case '!':
        return new Token(Kind.TOKEN_NOT, this.lineNum);
    case '}':
        return new Token(Kind.TOKEN_RBRACE, this.lineNum);
    case ']':
        return new Token(Kind.TOKEN_RBRACK, this.lineNum);
    case ')':
        return new Token(Kind.TOKEN_RPAREN, this.lineNum);
    case ';':
        return new Token(Kind.TOKEN_SEMI, this.lineNum);
    case '-':
        return new Token(Kind.TOKEN_SUB, this.lineNum);
    case '*':
        return new Token(Kind.TOKEN_TIMES, this.lineNum);
    default:
      // Lab 1, exercise 2: supply missing code to
      // lex other kinds of tokens.
      // Hint: think carefully about the basic
      // data structure and algorithms. The code
      // is not that much and may be less than 50 lines. If you
      // find you are writing a lot of code, you
      // are on the wrong way.

      // new Todo();
      // 接下来才处理字符串的情况
      String str = "";
      Token resToken = null; // result token
      // 正则表达式, 标识符-IDENTIFIER 必须以字母开头
      if (Character.isLetter(c)) {
          str += Character.toString((char) c);
          while(Character.isLetter(lookahead) || Character.isDigit(lookahead) || (char) lookahead == '_') {
              str += Character.toString((char) lookahead);
              lookahead = this.fstream.read();
          }
          
          Kind k = keywords.get(str);
          if (k != null) {
              resToken = new Token(k, this.lineNum, str);    // 关键字
          } else {
          	// todo 符号表来判别是否是标识符
              resToken = new Token(Kind.TOKEN_ID, this.lineNum, str);  // 标识符
          }
      } else if (Character.isDigit(c)) {
          str = "";
          str += Character.toString((char) c);
          while(Character.isDigit(lookahead)) {
              str += Character.toString((char) lookahead);
              lookahead = this.fstream.read();
          }
          resToken =  new Token(Kind.TOKEN_NUM, this.lineNum, str);
      }
      return resToken;
    }
  }

  public Token nextToken()
  {
    Token t = null;

    try {
      t = this.nextTokenInternal();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    if (dump)
      System.out.println(t.toString());
    return t;
  }
}
