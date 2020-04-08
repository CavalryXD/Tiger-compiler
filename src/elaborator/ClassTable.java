package elaborator;

import ast.Ast.Type;
import util.Todo;
import java.util.Map;

public class ClassTable
{
  // map each class name (a string), to the class bindings.
  // 所有类的名字和其类里面继承类, 变量和方法的对应表
  private java.util.Hashtable<String, ClassBinding> table;
  /* 
      table : id --> ClassBinding --> 指向两个表
              |           |
           fields      methods   
        id --> Type   id --> MethodType  
          局部变量    函数返回值类型和形参
  */
  public ClassTable()
  {
    this.table = new java.util.Hashtable<String, ClassBinding>();
  }

  // put一个类进来 
  // Duplication is not allowed
  public void put(String c, ClassBinding cb)
  {
    if (this.table.get(c) != null) {
      System.out.println("duplicated class: " + c);
      System.exit(1);
    }
    this.table.put(c, cb);
  }

  // put某个类的一个变量
  // put a field into this table
  // Duplication is not allowed
  public void put(String c, String id, Type.T type)
  {
    ClassBinding cb = this.table.get(c);
    cb.put(id, type);
    return;
  }

  // put某个类的方法
  // put a method into this table
  // Duplication is not allowed.
  // Also note that MiniJava does NOT allow overloading.
  public void put(String c, String id, MethodType type)
  {
    ClassBinding cb = this.table.get(c);
    cb.put(id, type);
    return;
  }

  // return null for non-existing class
  public ClassBinding get(String className)
  {
    return this.table.get(className);
  }

  // get type of some field
  // return null for non-existing field.
  // 在当前类和所有父类中寻找变量的类型
  public Type.T get(String className, String xid)
  {
    ClassBinding cb = this.table.get(className);
    Type.T type = cb.fields.get(xid);
    while (type == null) { // search all parent classes until found or fail
      if (cb.extendss == null)
        return type;

      cb = this.table.get(cb.extendss);
      type = cb.fields.get(xid);
    }
    return type;
  }

  // get type of some method
  // return null for non-existing method
  public MethodType getm(String className, String mid)
  {
    ClassBinding cb = this.table.get(className);
    MethodType type = cb.methods.get(mid);
    while (type == null) { // search all parent classes until found or fail
      if (cb.extendss == null)
        return type;

      cb = this.table.get(cb.extendss);
      type = cb.methods.get(mid);
    }
    return type;
  }

  public void dump()
  {
    // new Todo();
    System.out.println("ClassTable");
    for(Map.Entry<String, ClassBinding> entry : this.table.entrySet()) 
    {
      System.out.println(entry.getKey());
      entry.getValue().toString();
      System.out.println("\n");
    }
  }

  @Override
  public String toString()
  {
    return this.table.toString();
  }
}
