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

package deltix.vtype.transformer;

import deltix.vtype.interpreter.InstructionIterator;
import deltix.vtype.interpreter.OpcodeProcessor;
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.JvmStack;
import deltix.vtype.type.TypeId;
import deltix.vtype.type.TypeIdCast;
import deltix.vtype.mapping.Mapping;
import deltix.vtype.common.CrudeLogger;
import org.objectweb.asm.tree.*;

import java.util.List;

import static deltix.vtype.type.DescriptorParser.isField64;
import static org.objectweb.asm.Opcodes.*;

public class StackWalkHandler implements OpcodeProcessor {
    // Helper vars for simplifying conditional logging
    protected boolean logTrace;
    protected boolean logDbg;

    protected final JvmStack vm;
    private final InstructionIterator instructionIterator;
    protected JvmStack.SavedStack savedStack;

    protected final CrudeLogger log;

    // Pre-allocated array for storing method arguments for verification
    protected final int[] methodArgs          = new int[0x100];

    public StackWalkHandler(JvmStack vm, InstructionIterator instructionIterator, CrudeLogger logger) {

        this.vm = vm;
        this.instructionIterator = instructionIterator;
        this.log = logger;
        setLogLevel(logger.getLevel());
    }


    // Dummy method
    protected void dbgBreak() {
        log.debuggerBreak();
    }

    @Override
    public void onStart(Object context) {
        init();
    }

    protected void init() {
        // Just reset stack as we are not tracking variable state
        vm.resetStack();
    }

    @Override
    public void onUnderflow(AbstractInsnNode node) {
        log.err("Stack Underflow while processing opcode: %d. Simulated VM Stack: %s", node.getOpcode(), vm.toString());
    }

    @Override
    public void onReturnStackMismatch(InsnNode node) {

        log.err("Return stack mismatch for Return opcode: %d, Stack: %s", node.getOpcode(), vm.toString());
    }

    @Override
    public void onStackTypeMismatch() {
        log.err("Stack Type mismatch. Simulated VM Stack: %s%n Types: ", vm.toString(), vm.typesToString());
    }

    @Override
    public void onConstI32(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref--21.html

        vm.push32i("ConstI32");
    }

    @Override
    public void onConstI64(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref--39.html

        vm.push64i("ConstI64");
    }

    @Override
    public void onConstF32(InsnNode node) {
        vm.push32f("ConstF32");
    }

    @Override
    public void onConstF64(InsnNode node) {
        vm.push64f("ConstF64");
    }

    @Override
    public void onAConstNull(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-aconst_n.html

        vm.pushNull();
    }

    @Override
    public void onPop32(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_pop.html

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }

    @Override
    public void onPop64(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-pop2.html

        vm.checkSrcUnderflow32(node, 2);
        vm.pop2();
    }

    @Override
    public void onDup32(InsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.dup32();
    }


    @Override
    public void onDup64(InsnNode node) {
        onDup64x(node, 0);
    }


    @Override
    public void onDup32x(InsnNode node, int depth) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html

        throw new UnsupportedOperationException("DUP_*");

    }

    @Override
    public void onDup64x(InsnNode node, int depth) {
        vm.checkSrcUnderflow32(node, 2);
        if (0 != depth) {
            throw new UnsupportedOperationException("DUP2_*");
        }

        vm.dup2();
    }

    @Override
    public void onSwap32(InsnNode node) {
        throw new UnsupportedOperationException("Swap32");
    }

    @Override
    public void onAI32Load(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_iaload.html

        vm.checkSrcUnderflow32(node, 2);

        vm.pop32();
        vm.pop32();
        vm.push32i();
    }

    @Override
    public void onAF32Load(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_iaload.html

        vm.checkSrcUnderflow32(node, 2);

        vm.pop32();
        vm.pop32();
        vm.push32f();
    }

    @Override
    public void onA32Store(InsnNode node) {
        vm.checkSrcUnderflow32(node, 3);

        vm.pop32();
        vm.pop32();
        vm.pop32();
    }

    @Override
    public void onAI64Load(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_laload.html

        vm.checkSrcUnderflow32(node, 2);

        vm.pop32();
        vm.pop32();
        vm.push64i();
    }

    @Override
    public void onAF64Load(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_laload.html

        vm.checkSrcUnderflow32(node, 2);

        vm.pop32();
        vm.pop32();
        vm.push64f();
    }

    @Override
    public void onA64Store(InsnNode node) {
        vm.checkSrcUnderflow32(node, 4);

        vm.pop64();
        vm.pop32();
        vm.pop32();
    }

    @Override
    public void onArrayLength(InsnNode node) {
        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.push32i("ALength");
    }

    protected int parseMethod(MethodInsnNode node, Mapping mapping) {
        return AsmUtil.parseMethod(methodArgs, node, mapping);
    }


    private void applyReturnValue(int typeId) {
        vm.pushTypeId(typeId);
    }





    private void applyMethodArgs(int[] methodArgs, int nMethodArgs, int checkResult, AbstractInsnNode node) {

        assert (TypeIdCast.SUCCESS == (checkResult & 0xFF));
        if (0 != nMethodArgs) {
            vm.popMany(nMethodArgs);
        }

        applyReturnValue(methodArgs[0]);
    }


    protected void onMethodVerificationError(int[] methodArgs, int nMethodArgs, int checkResult, String name, String owner, String desc) {

        String err = String.format("Argument type verification failure for method call:%n  %s.%s%s, arg[%d], result: %x, %nStack:%s",
                owner, name, desc,
                nMethodArgs - ((checkResult >> 8) & 0xFF), checkResult, vm);

        log.err(err);
    }


    /**
     *
     * @param methodArgs  contains nMethodArgs+1 values, methodArgs[0] is the return value type
     * @param nMethodArgs
     * @return Argument verification result in bits 0..7 and 31, the first different argument stack depth in bits 8..15
     */
    protected int verifyMethodArgs(int[] methodArgs, int nMethodArgs) {
        if (0 == nMethodArgs)
            return TypeIdCast.SUCCESS;

        if (vm.top() < nMethodArgs) {
            log.err("Stack underflow while parsing method parameter list. Need: %d, actual: %d", nMethodArgs, vm.top());
            return TypeIdCast.FAILURE;
        }

        int firstFailedDepth = 0;
        int cmpResult = 0;

        for (int depth = nMethodArgs - 1; depth >= 0; --depth) {
            int argType = methodArgs[nMethodArgs - depth];
            int stackType = vm.typeIdAt(depth);
            cmpResult |= TypeIdCast.checkArg(stackType, argType);

            if (TypeIdCast.isFailure(cmpResult)) {
                return cmpResult | (depth << 8);
            } else if (TypeIdCast.SUCCESS != cmpResult && 0 == firstFailedDepth) {
                firstFailedDepth = (depth << 8);
            }
        }

        return cmpResult | firstFailedDepth;
    }


    protected int substituteWildcardArgs(int[] methodArgs, int nMethodArgs) {
        if (0 == nMethodArgs)
            return TypeIdCast.SUCCESS;

        int cmpResult = 0;
        int dstType = 0;
        for (int depth = nMethodArgs - 1; depth >= 0; --depth) {
            int argType = methodArgs[nMethodArgs - depth];
            int stackType = vm.typeIdAt(depth);
            cmpResult = TypeIdCast.checkArg(stackType, argType);
            if (0 != (TypeIdCast.NEED_SUBSTITUTION & cmpResult)) {
                if (0 == dstType) {
                    dstType = stackType;
                } else {
                    if (!TypeId.isSameVtClass(dstType, stackType)) {
                        log.wrn("More than 1 ValueType used with wildcard VT substitution at position %d", depth);
                        return TypeIdCast.FAILURE | (depth << 8);
                    }
                }
                methodArgs[nMethodArgs - depth] = TypeId.vtSubstituteTypeTo(argType, dstType);
            }

            if (TypeIdCast.isFailure(cmpResult)) {
                return cmpResult | (depth << 8);
            }
        }

        if (TypeId.isVt(methodArgs[0])) {
            if (0 == dstType)
                return TypeIdCast.FAILURE;

            methodArgs[0] = TypeId.vtSubstituteTypeTo(methodArgs[0], dstType);
        }

        return TypeIdCast.SUCCESS;
    }


    @Override
    public void onMethod(MethodInsnNode node) {

        int nMethodArgs = parseMethod(node, null);
        int checkResult = verifyMethodArgs(methodArgs, nMethodArgs);
        if (TypeIdCast.isFailure(checkResult)) {
            onMethodVerificationError(methodArgs, nMethodArgs, checkResult, node.name, node.owner, node.desc);
        }

        applyMethodArgs(this.methodArgs, nMethodArgs, checkResult, node);
    }

    @Override
    public void onInvokeDynamic(InvokeDynamicInsnNode node) {
        // TODO: Possibly incomplete implementation
        String name = node.name, desc = node.desc;
        int nMethodArgs = DescriptorParser.parseMethod(methodArgs, 0, desc, null);
        int checkResult = verifyMethodArgs(methodArgs, nMethodArgs);
        if (TypeIdCast.isFailure(checkResult)) {
            onMethodVerificationError(methodArgs, nMethodArgs, checkResult, name, null, desc);
        }

        applyMethodArgs(this.methodArgs, nMethodArgs, checkResult, node);
    }


    @Override
    public void onFrame(FrameNode node) {

        switch (node.type) {
            case F_SAME:
                // TODO: Wrong?
                vm.loadStack(savedStack);
                break;

            case F_APPEND:
            case F_CHOP:
                vm.resetStack();
                vm.saveStack(savedStack);
                break;

            case F_SAME1:

                //System.out.print(node.stack.get(0).toString());
                vm.resetStack();
                vm.pushAsmFrameObject(node.stack.get(0), null);
                vm.saveStack(savedStack);
                break;

            case F_FULL:
                //System.out.print(node.stack.get(0).toString());
                vm.resetStack();
                for (Object o : node.stack) {
                    vm.pushAsmFrameObject(o, null);
                }

                vm.saveStack(savedStack);
                break;
        }
    }

    @Override
    public void onLine(LineNumberNode node) {

    }

    @Override
    public void onLdc(LdcInsnNode node) {

        vm.pushType(node.cst.getClass());
    }

    @Override
    public void onIInc(IincInsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-iinc.html
        // Do nothing in the default implementation
    }

    @Override
    public void onReturn(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-return.html
        vm.checkReturnStack(node, 0, 0, 0);
        vm.invalidateStack();
    }

    @Override
    public void onReturn32(InsnNode node) {
        vm.checkReturnStack(node, 1, 1, 1);
        vm.invalidateStack();

    }

    @Override
    public void onReturn64(InsnNode node) {
        vm.checkReturnStack(node, 1, 2, 2);
        vm.invalidateStack();
    }

    @Override
    public void onAReturn(InsnNode node) {
        vm.checkReturnStack(node, 1, 1, 1);
        vm.invalidateStack();
    }

    @Override
    public void onAALoad(InsnNode node) {
        vm.checkSrcUnderflow32(node,2);
        vm.pop32();
        vm.pop32();
        vm.push32ref();
    }

    @Override
    public void onAAStore(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-aastore.html

        vm.checkSrcUnderflow32(node, 3);
        // Check if boxing is needed
        //log_err("AASTORE: Stack underflow!!");

        vm.pop32();
        vm.pop32();
        vm.pop32();
    }


    protected void consumeStackValue32(InsnNode node, int mask, int value) {
        assertScalar32(node);
        vm.pop32();
    }


    protected void assertScalar32(InsnNode node) {
        vm.checkSrcUnderflow32(node, 1);
        if (!TypeId.isScalar32(vm.typeIdAt(0))) {
            onStackTypeMismatch();
        }

    }


    protected void assertScalar64(InsnNode node) {
        vm.checkSrcUnderflow32(node, 2);
        if (!TypeId.isScalar64(vm.typeIdAt(0))) {
            onStackTypeMismatch();
        }
    }


    protected void consumeScalar32(InsnNode node) {
        assertScalar32(node);
        vm.pop32();
    }


    protected void consumeScalar64(InsnNode node) {
        assertScalar64(node);
        vm.pop64();
    }


    @Override
    public void onLCmp(InsnNode node) {

        vm.checkSrcUnderflow32(node, 4);

        vm.pop64();
        vm.pop64();
        vm.push32i("LCmpResult");
    }

    @Override
    public void onDCmp(InsnNode node) {

        vm.checkSrcUnderflow32(node, 4);
        vm.pop64();
        vm.pop64();
        vm.push32i("DCmpResult");
    }

    @Override
    public void onFCmp(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop32();
        vm.pop32();
        vm.push32i("FCmpResult");
    }

    @Override
    public void onAluI32(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop32();
        vm.pop32();
        vm.push32i();
    }

    @Override
    public void onAluI64(InsnNode node) {

        vm.checkSrcUnderflow32(node, 4);
        vm.pop64();
        vm.pop64();
        vm.push64i();
    }

    @Override
    public void onAluF32(InsnNode node) {
        vm.checkSrcUnderflow32(node, 2);
        vm.pop32();
        vm.pop32();
        vm.push32f();
    }

    @Override
    public void onAluF64(InsnNode node) {
        vm.checkSrcUnderflow32(node, 4);
        vm.pop64();
        vm.pop64();
        vm.push64f();
    }

    @Override
    public void onShift64(InsnNode node) {

        vm.checkSrcUnderflow32(node, 3);
        vm.pop32();
        vm.pop64();
        vm.push64i();
    }

    @Override
    public void onAluToI32Unary(InsnNode node) {

        consumeScalar32(node);
        vm.push32i();
    }

    @Override
    public void onAluToI64Unary(InsnNode node) {

        consumeScalar64(node);
        vm.push64i();
    }

    @Override
    public void onAluToF32Unary(InsnNode node) {

        consumeScalar32(node);
        vm.push32i();
    }

    @Override
    public void onAluToF64Unary(InsnNode node) {

        consumeScalar64(node);
        vm.push64f();
    }

    @Override
    public void onCast32toI64(InsnNode node) {

        consumeScalar32(node);
        vm.push64i();
    }

    @Override
    public void onCast64toI32(InsnNode node) {

        consumeScalar64(node);
        vm.push32i();
    }

    @Override
    public void onCast64toF32(InsnNode node) {

        consumeScalar64(node);
        vm.push32f();
    }

    @Override
    public void onCast32toF64(InsnNode node) {

        consumeScalar32(node);
        vm.push64f();
    }

    @Override
    public void onCastF32I32(InsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        if (TypeId.F32 != vm.typeIdAt(0)) {
            onStackTypeMismatch();
        }

        vm.pop32();
        vm.push32i();
    }

    @Override
    public void onCastI32F32(InsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        if (TypeId.I32 != vm.typeIdAt(0)) {
            onStackTypeMismatch();
        }

        vm.pop32();
        vm.push32f();
    }

    @Override
    public void onCastF64I64(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        if (TypeId.F64 != vm.typeIdAt(0)) {
            onStackTypeMismatch();
        }

        vm.pop64();
        vm.push64i();
    }

    @Override
    public void onCastI64F64(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        if (TypeId.I64 != vm.typeIdAt(0)) {
            onStackTypeMismatch();
        }

        vm.pop64();
        vm.push64f();
    }

    @Override
    public void onNewArray(IntInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.pushTypeId(TypeId.arrayIncrementDepth(AsmUtil.newArrayTypeToTypeId(node.operand)));
    }

    @Override
    public void onIPush(IntInsnNode node) {

        vm.push32i("ConstI32");
    }

    @Override
    public void onLoadI32(VarInsnNode node) {

        vm.push32i();
    }

    @Override
    public void onLoadI64(VarInsnNode node) {

        vm.push64i();
    }

    @Override
    public void onLoadF32(VarInsnNode node) {

        vm.push32f();
    }

    @Override
    public void onLoadF64(VarInsnNode node) {

        vm.push64f();
    }

    @Override
    public void onStore32(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }

    @Override
    public void onStore64(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop64();
    }

    @Override
    public void onALoad(VarInsnNode node) {

        vm.push32ref();
    }

    @Override
    public void onAStore(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }


    // Type Instruction Node
    // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/TypeInsnNode.html

    @Override
    public void onANewArray(TypeInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.push32ref("A[" + node.desc + "]");
    }

    @Override
    public void onNew(TypeInsnNode node) {

        vm.push32ref("A[" + node.desc + "]");
    }

    @Override
    public void onCheckCast(TypeInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.push32ref();
    }

    @Override
    public void onInstanceOf(TypeInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.push32i("bool");
    }

    // Field Instructuion
    // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/FieldInsnNode.html
    @Override
    public void onGetStatic(FieldInsnNode node) {
        vm.pushTypeId(DescriptorParser.getDescTypeId(node.desc, null));
    }

    @Override
    public void onGetField(FieldInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.pushTypeId(DescriptorParser.getDescTypeId(node.desc, null));
    }

    @Override
    public void onPutStatic(FieldInsnNode node) {

        if (isField64(node.desc)) {
            vm.checkSrcUnderflow32(node, 2);
            vm.pop64();
        } else {
            vm.checkSrcUnderflow32(node, 1);
            vm.pop32();
        }
    }

    @Override
    public void onPutField(FieldInsnNode node) {

        if (isField64(node.desc)) {
            vm.checkSrcUnderflow32(node, 2);
            vm.pop64();
        } else {
            vm.checkSrcUnderflow32(node, 1);
            vm.pop32();
        }

        vm.pop32();
    }

    @Override
    public void onMultiANewArray(MultiANewArrayInsnNode node) {

        String desc = node.desc;
        int n = node.dims;
        vm.checkSrcUnderflow32(node, n);
        for (int i = 0; i < n; ++i) {
            vm.pop32();
        }

        vm.pushTypeId(DescriptorParser.getDescTypeId(desc, null),  desc);
    }

    @Override
    public void onLookupSwitch(LookupSwitchInsnNode node) {
        vm.pop32(); // TODO: Verify
    }

    @Override
    public void onTableSwitch(TableSwitchInsnNode node) {
        vm.pop32(); // TODO: Verify
    }

    @Override
    public void onLabel(LabelNode node) {

    }

    @Override
    public void onIfICmpXX(JumpInsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop32();
        vm.pop32();
    }

    @Override
    public void onIfXX(JumpInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }

    @Override
    public void onIfNull(JumpInsnNode node, boolean isNull) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }

    @Override
    public void onIfACmpEq(JumpInsnNode node, boolean isEqual) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop32();
        vm.pop32();
    }

    @Override
    public void onGoto(JumpInsnNode node) {
        vm.invalidateStack();
    }

    @Override
    public void onAThrow(InsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        vm.invalidateStack();
    }

    @Override
    public void onJsr(JumpInsnNode node) {
        throw new UnsupportedOperationException("Jsr");
    }

    @Override
    public void onRet(VarInsnNode node) {
        throw new UnsupportedOperationException("Ret");
    }

    @Override
    public void onVar(LocalVariableNode variable) {

    }

    @Override
    public void onVarProcessingEnd(List vars) {

        savedStack = vm.saveStack(savedStack);
    }

    @Override
    public boolean shouldProcessMethodCode() {
        return true;
    }

    @Override
    public void onMonitorEnter(InsnNode node) {
        vm.checkSrcUnderflow32(node ,1);
        vm.pop32();
    }

    @Override
    public void onMonitorExit(InsnNode node) {
        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
    }

    @Override
    public void onEnd() {
    }


    public void setLogLevel(int logLevel) {

        this.log.setLogLevel(logLevel);
        logTrace = log.on(log.TRACE);
        logDbg = log.on(log.DBG);
    }


    protected int currentLine() {
        return instructionIterator.currentLine();
    }
}
