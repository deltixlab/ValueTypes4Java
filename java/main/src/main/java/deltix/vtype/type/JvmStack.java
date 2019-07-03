/*
 * Copyright 2017-2018 Deltix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package deltix.vtype.type;

import deltix.vtype.interpreter.OpcodeProcessor;
import deltix.vtype.mapping.Mapping;
import deltix.vtype.transformer.VariableNameDefaultFormatter;
import deltix.vtype.common.CrudeLogger;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import static deltix.vtype.transformer.AsmUtil.frameEntryEnumToTypeId;

/**
 * Simulates JVM stack.
 * TODO: Need major cleanup. Simplify SavedStack class.
 */
public class JvmStack {

    private boolean logDbg;
    private boolean logTrace;
    OpcodeProcessor handler;
    private final CrudeLogger log;
    private final VariableNameFormatter formatter;

    final static int maxStackDepth = 0x10000;

    private String names[] = null;   // Just for debugging
    private int types[] = null;

    private int top;            // Actual stack pointer
    private int top32Src;       // Virtual size in 32-bit cells     (Actual size before value substitution)
    private int top32Dst;       // Real size in 32-bit cells         (Actual size after value substitution)


    public class SavedStack {
        int[] types;
        String[] names;
        int top, top32Dst, top32Src;

        SavedStack() {
            this.names = new String[32];
            this.types = new int[32];
        }

        SavedStack(int top, int top32Dst, int top32Src, int[] types, String[] names) {
            this.top  = top;
            this.top32Dst = top32Dst;
            this.top32Src = top32Src;
            this.names = new String[top];
            this.types = new int[top];
            System.arraycopy(types, 1, this.types, 0, top);
            System.arraycopy(names, 1, this.names, 0, top);
        }
    }


    public JvmStack(CrudeLogger logger, VariableNameFormatter formatter) {

        this.log = logger;
        setLogLevel(logger.getLevel());

        this.formatter = null != formatter ? formatter : VariableNameDefaultFormatter.get();
        types = new int[maxStackDepth];
        names = new String[maxStackDepth];
    }

    public void setLogLevel(int level) {
        this.log.setLogLevel(level);
        logDbg = log.on(log.DBG);
        logTrace = log.on(log.TRACE);
    }

    public boolean isStackReset() {
        return Integer.MIN_VALUE == top ;
    }

    // Safety method. After calling it we expect loadStack or a similar method to be called
    public void invalidateStack() {
        top = Integer.MIN_VALUE;
    }

    void resetStackIfInvalidated() {

        if (top < 999) {
            resetStack();
        }
    }


    public void resetStack() {
        top = top32Src = top32Dst = 0;
    }


    public SavedStack saveStack() {
        SavedStack saved = new SavedStack();
        saveStack(saved);
        return saved;
    }


    public SavedStack saveStack(SavedStack saved) {

        if (null == saved) {
            saved = new SavedStack();
        }

        int top = saved.top = this.top;
        saved.top32Dst = this.top32Dst;
        saved.top32Src = this.top32Src;

        if (saved.types.length < top) {
            saved.types = new int[top * 2];
            saved.names = new String[top * 2];
        }

        if (0 != top) {
            System.arraycopy(this.types, 1, saved.types, 0, top);
            System.arraycopy(this.names, 1, saved.names, 0, top);
        }

        //log.trace("SaveStack:" + this.toString());
        return saved;
    }


    public void loadStack(SavedStack saved) {

        int top = this.top  = saved.top;
        this.top32Dst = saved.top32Dst;
        this.top32Src = saved.top32Src;
        if (0 != top) {
            System.arraycopy(saved.types, 0, this.types, 1, top);
            System.arraycopy(saved.names, 0, this.names, 1, top);
        }

        if (logTrace) {
            log.trace("LoadStack: " + this.toString());
        }
    }


    public int pushAsmFrameObject(Object o, Mapping mapping) {

        int typeId;
        String name = null;
        if (o instanceof Integer) {
            typeId = frameEntryEnumToTypeId((Integer)o);
        } else if (o instanceof String) {
            typeId = null != mapping ? mapping.getVTypeIdFromDesc((String)o) : TypeId.OBJ_REF;
            name = (String)o;
        }
        else if (o instanceof LabelNode || o instanceof Label) {
            typeId = TypeId.OBJ_REF;
            name = "NewRef";
        }
        else
            throw new IllegalArgumentException(o.getClass().toString() + " " + o.toString());

        _push(typeId, name);
        return typeId;
    }

    public void renameTop(String name) {
        names[top] = name;
    }


    private void incrementSimulatedTop(int typeId) {
        top32Dst += TypeId.size32Dst(typeId);
        top32Src += TypeId.size32Src(typeId);
    }

    private void decrementSimulatedTop(int typeId) {
        top32Dst -= TypeId.size32Dst(typeId);
        top32Src -= TypeId.size32Src(typeId);
    }


    void _push(int typeId, String desc) {

        incrementSimulatedTop(typeId);
        ++top;
        types[top] = typeId;
        this.names[top] = desc;
    }


    void _pop() {

        assert (top >= 0);
        int typeId = types[top];
        --top;
        decrementSimulatedTop(typeId);
    }


    void _swap() {
        assert(top > 1);
        String name = names[top];
        names[top] = names[top - 1];
        names[top - 1] = name;
        int v = types[top];
        types[top] = types[top - 1];
        types[top - 1] = v;
    }


    void _dupX(int depth, int step) {
        // Depth is how many items to skip before the copied one
        // This is _not_ necessarily the same depth param from DUP_X/DUP2_X
        // Step will == 1 for DUP_X, 1 or 2 for DUP2_X, depending on the value on the top of the stack
        //assert (depth > 0);
        assert (step > 0);
        assert(top > depth);    // There must be at least depth + 1 items on the stack

        int src = top - depth - step + 1;
        int dst = src + step;
        for (int i = top - src; i >= 0; --i) {
            types[dst + i] = types[src + i];
            names[dst + i] = names[src + i];
        }

        dst = src;
        src = top + 1;
        top += step;
        for (int i = 0; i < step; ++i) {
            names[dst + i] = names[src + i];
            int x = types[dst + i] = types[src + i];
            incrementSimulatedTop(x);
        }
    }

    public void popMany(int n) {

        for (int i = n; i > 0; --i) {
            _pop();
        }

        if (logTrace) {
            log.trace("Pop %d item(s), result: %s", n, this);
        }
    }

    public void popAny() {

        if (log.on(log.TRACE)) {
            int typeId = types[top];
            String name = names[top];
            _pop();
            log.trace("Pop '%s', result: %s", formatter.format(typeId, name, -1), this);
        } else {
            _pop();
        }
    }

    public int top() {
        return this.top;
    }

    public int[] ids() {
        return this.types;
    }

    public int top32Src() {
        return top32Src;
    }

    public int top32Dst() {
        return top32Dst;
    }

    public int typeIdAt(int depth) {
        return types[top - depth];
    }

    public String nameAt(int depth) {
        return names[top - depth];
    }


    public boolean isTypeDst32at(int depth) {
        return TypeId.isDst32(types[top - depth]);
    }

    boolean isTypeSrc64at(int depth) {
        return TypeId.isSrc64(types[top - depth]);
    }

    boolean isTypeSrc32at(int depth) {
        return !TypeId.isSrc64(types[top - depth]);
    }

    boolean isTypeDst64at(int depth) {
        return TypeId.isDst64(types[top - depth]);
    }

    public boolean isVType64ValueAt(int depth) {
        return TypeId.isVtValue(types[top - depth]);
    }

    public void swap() {
        assert (top > 1);
        if (isTypeSrc32at(0) && isTypeSrc32at(1)) {
            _swap();
            trace("SWAP");
        } else {
            handler.onStackTypeMismatch();
        }

        // Dump stack state
        if (logTrace) {
            trace(this);
        }
    }

    public void pop32() {
        //assert(0 == (types[top] & 0x40));
        assert (top > 0);
        if (isTypeDst32at(0)) {
            _pop();
            if (logTrace) {
                trace("POP 32: stack: ", this);
            }
        } else {
            handler.onStackTypeMismatch();
        }
    }


    public void pop64() {
        //assert(top >= 0);
        //assert(0 != (types[top] & 0x40));
        if (isTypeDst64at(0)) {
            _pop();
            trace("POP 64: stack: ", this);
        } else {
            handler.onStackTypeMismatch();
        }
    }


    public void pop2() {
        assert (top > 0);
        if (isTypeDst64at(0)) {
            trace("POP2(64)");
            _pop();
        } else if (isTypeDst32at(1)) {
            trace("POP2(2x32)");
            _pop();
            _pop();
        } else {
            handler.onStackTypeMismatch();
        }

        // Dump stack state
        if (logTrace) {
            trace(this);
        }
    }


    public void dup32() {
        assert (top > 0);

        if (isTypeDst32at(0)) {
            trace("DUP32");
            _push(types[top], names[top]);
        } else {
            handler.onStackTypeMismatch();
        }

        // Dump stack state
        trace(this);
    }


    public void dup32x(int depth) {
        assert (top > depth);
        throw new UnsupportedOperationException();
// depth is how many 32-bit cells to skip before the copied one


//        if (isTypeDst32at(depth)) {
//            log.trace("DUP32_%d", depth);
//            _push(types[top - depth], names[top - depth]);
//        } else {
//            handler.onStackTypeMismatch();
//        }
//
//        // Dump stack state
//        log.trace(this.toString());
    }


    public void dup64() {
        assert (top > 0);

        if (isTypeDst64at(0)) {
            trace("DUP64");
            _push(types[top], names[top]);
        } else {
            handler.onStackTypeMismatch();
        }

        if (logTrace) {
            log.trace("\t\tstack: %s", this.toString());
        }
    }


    public void dup2() {
        assert (top > 0);
        if (isTypeSrc64at(0)) {
            log.trace("DUP2(64)");
            _push(types[top], names[top]);

        } else if (isTypeSrc32at(1)) {
            log.trace("DUP2(2x32)");
            _push(types[top - 1], names[top - 1]);
            _push(types[top - 1], names[top - 1]);
        } else {
            handler.onStackTypeMismatch();
        }

        if (logTrace) {
            log.trace("\t\tstack: %s", this);
        }
    }


    public void popVType64() {
        assert (top >= 0);
        //assert(0xC0 == (types[top] & 0xC0));
        //check(isVType64at(0));
        _pop();

        // Dump stack state
        if (logTrace) {
            trace("POP VT64: stack: %s", this);
        }
    }

    public void pushTypeId(int typeId) {

        pushTypeId(typeId, null);
    }

    public void pushTypeId(int typeId, String name) {

        assert(typeId != 0);
        _push(typeId, name);
        if (logTrace) {
            log.trace("\t\tPUSH typeId: %x|%s, stack: %s", typeId, formatter.format(typeId, name, -1), this);
        }
    }


    public void pushType(Class<?> type) {

        if (type == int.class) {push32i(); return; }
        if (type == long.class)   {push64i(); return; }
        if (type == float.class)  {push32f(); return; }
        if (type == double.class) {push64f(); return; }
        if (type == Integer.class) {push32i(); return; }
        if (type == Long.class)   {push64i(); return; }
        if (type == Float.class)  {push32f(); return; }
        if (type == Double.class) {push64f(); return; }
        //if (type == Object.class) {push32ref(); return; }
        if (type == String.class) {push32ref("String"); return; }
        push32ref(type.getName());
    }


    public void push32i(String desc) {

        _push(TypeId.I32, desc);
        trace("PUSH I32: stack: ", this);
    }


    public void push32i() {
        push32i(null);
    }


    public void push64i(String desc) {

        _push(TypeId.I64, desc);
        trace("PUSH I64: stack: ", this);
    }


    public void push64i() {
        push64i(null);
    }


    public void push32f(String desc) {

        _push(TypeId.F32, desc);
        trace("PUSH F32: stack: ", this);
    }


    public void push64f(String desc) {

        _push(TypeId.F64, desc);
        trace("PUSH F64: stack: ", this);
    }


    public void push64f() {
        push64f(null);
    }


    public void push32f() {
        push32f(null);
    }


    public void push32ref(String desc) {
        // TODO: Probably remove this operation

        _push(TypeId.OBJ_REF, desc);
        if (logTrace) {
            log.trace("\t\tPUSH Ref(%s): stack: %s", desc, this.toString());
        }
    }

    public void push32ref() {
        push32ref();
    }

    public void pushNull() {

        _push(TypeId.NULL_REF, null);
        trace("\t\tPUSH null: stack: ", this);
    }


    public void pushVType64(int arg) {
        log.trace("\t\tPUSH VT64(0x%x)", arg);
        // TODO: Generate "stack name" for Value types and Value Type arrays
        assert(TypeId.isVtValue(arg));
        _push(arg, null);
        if (logTrace) {
            log.trace("\t\tstack: %s", this);
        }
    }

    public void pushVTypeRef(int arg) {
        log.trace("PUSH \t\tVTref(0x%x)", arg);
        assert(TypeId.isVtRef(arg));
        _push(arg, null);
        if (logTrace) {
            log.trace("\t\tstack: %s", this);
        }
    }

//    public void pushVTypeArrayRef(int arg) {
//        log.trace("PUSH VT64[] (0x%x)", arg);
//        assert(TypeId.isVtArray(arg));
//        _push(arg, "X[]");
//    }


    public void dupX(int depth, int step) {
        log.trace("\t\tdupX(%d, %d) ", depth, step);
        _dupX(depth, step);

        if (logTrace) {
            log.trace("\t\tstack: %s", this);
        }
    }


    /**
     * @param depth parameter of DUP_X: [1..2]
     * @return true if succesfully applied the operation, false if VType is found and more complex conversion is required
     */
    public boolean tryDupX(int depth) {

        checkSrcUnderflow32(null, depth + 1);

        if (depth < 1 || depth > 2)
            throw new UnsupportedOperationException("Logic Error: DUP_X size invalid");

        int t0 = typeIdAt(0);
        int t1 = typeIdAt(1);

        if (TypeId.isVtValue(t0) || TypeId.isVtValue(t1))
            return false;

        switch (depth) {
            case 1:
                if (TypeId.isSrc64(t0) || TypeId.isSrc64(t1))
                    break;

                _dupX(1, 1);
                return true;

            case 2:
                if (TypeId.isSrc64(t0))
                    break;

                if (TypeId.isSrc64(t1)) {
                    _dupX(1, 1);
                    return true;
                }

                int t2 = typeIdAt(2);

                if (TypeId.isVtValue(t2))
                    return false;

                if (TypeId.isSrc64(t2))
                    break;

                _dupX(2, 1);
                return true;
        }

        handler.onStackTypeMismatch();
        log.err("DUP_X%d failed, Stack: ", depth, this);
        throw new RuntimeException("DUP_X failed");
    }



    private boolean dup2Fail(int depth) {
        handler.onStackTypeMismatch();
        log.err("DUP_X%d failed, Stack: ", depth, this);
        throw new RuntimeException("DUP_X failed");
    }

    /**
     * @param depth parameter of DUP_X: [1..2]
     * @return true if succesfully applied the operation, false if VType is found and more complex conversion is required
     */
    public boolean tryDupX2(int depth) {

        checkSrcUnderflow32(null, depth + 1);

        if (depth < 1 || depth > 2)
            throw new UnsupportedOperationException("Logic Error: DUP_X size invalid");

        int t0 = typeIdAt(0);
        int t1 = typeIdAt(1);

        if (TypeId.isVtValue(t0) || TypeId.isVtValue(t1))
            return false;

        int step;
        if (TypeId.isSrc64(t0)) {
            step = 1;
        } else {
            if (TypeId.isSrc64(t1)) {
                return dup2Fail(depth);
            }

            step = 2;
            t1 = typeIdAt(2);
        }

        switch (depth) {
            case 1:
                if (TypeId.isSrc64(t1))
                    break;

                _dupX(1, step);
                return true;

            case 2:
                if (TypeId.isSrc64(t1)) {
                    _dupX(1, 2);
                    return true;
                }

                int t2 = typeIdAt(2);

                if (TypeId.isVtValue(t2))
                    return false;

                if (TypeId.isSrc64(t2))
                    break;

                _dupX(2, 1);
                return true;
        }

        return dup2Fail(depth);
    }

    /**
     * Assert stack size (in arbitrary values, where longs, vtype, 32-bit types are equal)
     *
     * @param node
     * @param required amount of "virtual" 32-bit cells required
     */
    public void checkUnderflow(AbstractInsnNode node, int required) {
        if (top < required) {
            handler.onUnderflow(node);
        }
    }

    /**
     * Assert virtual stack size (in 32-bit units, where value type is still seen as 32-bit ref)
     *
     * @param node
     * @param required amount of "virtual" 32-bit cells required
     */
    public void checkSrcUnderflow32(AbstractInsnNode node, int required) {
        if (top32Src < required) {
            handler.onUnderflow(node);
        }
    }


    public void checkReturnStack(InsnNode node, int top, int top32Src, int top32Dst) {
        if (this.top != top || this.top32Src != top32Src || this.top32Dst != top32Dst) {
            handler.onReturnStackMismatch(node);
        }
    }


    private String getName(int typeId, String name) {
        return formatter.format(typeId, name, -1);
    }


    public String toString() {
        StringBuffer s = new StringBuffer("{");

        for (int i = 1; i <= top; ++i) {
            char ch = 0;
            if (i > 1) s.append(',');
            s.append(getName(types[i], names[i]));
        }

        return s.append('}').toString();
    }

    public String typesToString() {
        StringBuffer s = new StringBuffer("{");

        for (int i = 1; i <= top; ++i) {
            char ch = 0;
            if (i > 1) s.append(',');
            s.append(types[i]).append(TypeId.toString(types[i]));
        }

        return s.append('}').toString();
    }


    private void trace(Object p) {
        if (logTrace) {
            log.trace("\t\t%s", p);
        }
    }

    private void trace(String p0, Object p1) {
        if (logTrace) {
            log.trace("\t\t%s%s", p0, p1);
        }
    }
}
