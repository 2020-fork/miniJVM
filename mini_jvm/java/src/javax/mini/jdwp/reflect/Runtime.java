/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javax.mini.jdwp.reflect;

/**
 *
 * @author gust
 */
public class Runtime {

    public long runtimeId;
    public long classId;
    public long sonId;
    public long pc;
    public long byteCode;
    public long methodId;

    public Runtime son;
    public Runtime parent;

    public Runtime(long rid) {
        this(rid, null);
    }

    public Runtime(long rid, Runtime parent) {
        this.runtimeId = rid;
        this.parent = parent;
        mapRuntime(runtimeId);
        if (sonId != 0) {
            son = new Runtime(sonId, this);
            System.out.println("parent:" + runtimeId + ", son:" + sonId);
        }
    }

    public Runtime getLastSon() {
        return son == null ? this : son.getLastSon();
    }

    public int getDeepth() {
        int deep = 0;
        Runtime r = this;
        while (r != null) {
            r = r.son;
            deep++;
        }
        deep--;//顶层
        return deep;
    }

    native void mapRuntime(long runtimeId);
}
//typedef struct _Runtime {
//    MethodInfo *methodInfo;
//    Class *clazz;
//    u8 *pc;
//    CodeAttribute *bytecode;//method bytecode
//    JavaThreadInfo *threadInfo;
//    Runtime *son;//sub method's runtime
//    StackFrame *stack;
//    LocalVarItem *localVariables;
//    s32 localvar_count;
//    u8 wideMode;
//} Runtime;
