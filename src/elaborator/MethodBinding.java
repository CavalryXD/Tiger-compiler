package elaborator;

import ast.Ast;
import ast.Ast.Dec;
import ast.Ast.Type;

import java.util.Hashtable;
import java.util.LinkedList;

public class MethodBinding {
    // 局部变量
    public java.util.Hashtable<String, Type.T> fields;
    public java.util.Hashtable<String, Boolean> localsTable; // 查询局部变量是否被使用, Lab5 OPT中使用
    
    public MethodBinding() {
        this.fields = new Hashtable<>();
        this.localsTable = new java.util.Hashtable<String, Boolean>();
    }

    // Duplication is not allowed
    public void put(LinkedList<Dec.T> formals,
                    LinkedList<Dec.T> locals) {
        for (Dec.T dec : formals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.fields.get(decc.id) != null) {
                System.out.println("duplicated parameter: " + decc.id);
                System.exit(1);
            }
            this.localsTable.put(decc.id, true);        // Lab5
            this.fields.put(decc.id, decc.type);
        }

        for (Dec.T dec : locals) {
            Dec.DecSingle decc = (Dec.DecSingle) dec;
            if (this.fields.get(decc.id) != null) {
                System.out.println("duplicated variable: " + decc.id);
                System.exit(1);
            }
            this.localsTable.put(decc.id, false);      // Lab5
            this.fields.put(decc.id, decc.type);
        }

    }

    public Type.T get(String id) {
        return this.fields.get(id);
    }

    // localsTable相关
    public void set(String id) {
        if(this.localsTable.get(id) == false) {
            this.localsTable.remove(id);
            this.localsTable.put(id, true);
        }
        else if(this.localsTable.get(id) == true)
            return ;
        else this.localsTable.put(id, true);
    }

    public boolean query(String id) {
        return this.localsTable.get(id);
    }

    @Override
    public String toString() {
        return this.fields.toString();
    }
}
