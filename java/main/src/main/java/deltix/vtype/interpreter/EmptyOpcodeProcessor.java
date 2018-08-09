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

package deltix.vtype.interpreter;

import deltix.vtype.type.JvmStack;
import org.objectweb.asm.tree.*;

import java.util.List;

public class EmptyOpcodeProcessor implements OpcodeProcessor {
    protected JvmStack vm;

    @Override
    public void onStart(Object context) {
    }

    @Override
    public void onUnderflow(AbstractInsnNode node) {
    }

    @Override
    public void onReturnStackMismatch(InsnNode node) {
    }

    @Override
    public void onStackTypeMismatch() {

    }

    @Override
    public void onVar(LocalVariableNode variable) {

    }

    @Override
    public void onVarProcessingEnd(List vars) {

    }

    @Override
    public boolean shouldProcessMethodCode() {
        return true;
    }

    @Override
    public void onMonitorEnter(InsnNode node) {

    }

    @Override
    public void onMonitorExit(InsnNode node) {

    }

    @Override
    public void onConstF64(InsnNode node) {

    }

    @Override
    public void onConstF32(InsnNode node) {

    }

    @Override
    public void onAF64Load(InsnNode node) {

    }

    @Override
    public void onAF32Load(InsnNode node) {

    }

    @Override
    public void onLoadF32(VarInsnNode node) {

    }

    @Override
    public void onLoadF64(VarInsnNode node) {

    }

    @Override
    public void onDup32(InsnNode node) {

    }

    @Override
    public void onDup32x(InsnNode node, int depth) {

    }

    @Override
    public void onDup64(InsnNode node) {

    }

    @Override
    public void onDup64x(InsnNode node, int depth) {

    }



    @Override
    public void onSwap32(InsnNode node) {

    }

    @Override
    public void onPop32(InsnNode node) {

    }

    @Override
    public void onPop64(InsnNode node) {

    }

    @Override
    public void onMethod(MethodInsnNode node) {

    }

    @Override
    public void onInvokeDynamic(InvokeDynamicInsnNode node) {

    }

    @Override
    public void onGetStatic(FieldInsnNode node) {

    }

    @Override
    public void onGetField(FieldInsnNode node) {

    }

    @Override
    public void onPutStatic(FieldInsnNode node) {

    }

    @Override
    public void onPutField(FieldInsnNode node) {

    }

    @Override
    public void onConstI32(InsnNode node) {

    }

    @Override
    public void onConstI64(InsnNode node) {
    }

    @Override
    public void onAConstNull(InsnNode node) {
    }

    @Override
    public void onAI32Load(InsnNode node) {

    }

    @Override
    public void onA32Store(InsnNode node) {

    }

    @Override
    public void onAI64Load(InsnNode node) {

    }

    @Override
    public void onA64Store(InsnNode node) {

    }

    @Override
    public void onArrayLength(InsnNode node) {

    }

    @Override
    public void onANewArray(TypeInsnNode node) {
    }

    @Override
    public void onAAStore(InsnNode node) {

    }

    @Override
    public void onALoad(VarInsnNode node) {
    }

    @Override
    public void onAStore(VarInsnNode node) {

    }

    @Override
    public void onNew(TypeInsnNode node) {
    }

    @Override
    public void onCheckCast(TypeInsnNode node) {}

    @Override
    public void onInstanceOf(TypeInsnNode node) {
    }

    @Override
    public void onFrame(FrameNode node) {

    }

    @Override
    public void onLine(LineNumberNode node) {

    }

    @Override
    public void onLdc(LdcInsnNode node) {

    }

    @Override
    public void onIInc(IincInsnNode node) {

    }

    @Override
    public void onReturn(InsnNode node) {

    }

    @Override
    public void onReturn32(InsnNode node) {

    }

    @Override
    public void onReturn64(InsnNode node) {

    }

    @Override
    public void onAReturn(InsnNode node) {

    }

    @Override
    public void onAALoad(InsnNode node) {

    }

    @Override
    public void onLCmp(InsnNode node) {

    }

    @Override
    public void onDCmp(InsnNode node) {

    }

    @Override
    public void onFCmp(InsnNode node) {

    }

    @Override
    public void onAluI32(InsnNode node) {

    }

    @Override
    public void onAluI64(InsnNode node) {

    }

    @Override
    public void onAluF32(InsnNode node) {

    }

    @Override
    public void onAluF64(InsnNode node) {

    }

    @Override
    public void onShift64(InsnNode node) {

    }

    @Override
    public void onAluToI32Unary(InsnNode node) {

    }

    @Override
    public void onAluToI64Unary(InsnNode node) {

    }

    @Override
    public void onAluToF64Unary(InsnNode node) {

    }

    @Override
    public void onAluToF32Unary(InsnNode node) {

    }

    @Override
    public void onCast32toI64(InsnNode node) {

    }

    @Override
    public void onCast64toI32(InsnNode node) {

    }

    @Override
    public void onCast64toF32(InsnNode node) {

    }

    @Override
    public void onCast32toF64(InsnNode node) {

    }

    @Override
    public void onCastF32I32(InsnNode node) {

    }

    @Override
    public void onCastI32F32(InsnNode node) {

    }

    @Override
    public void onCastF64I64(InsnNode node) {

    }

    @Override
    public void onCastI64F64(InsnNode node) {

    }

    @Override
    public void onNewArray(IntInsnNode node) {

    }

    @Override
    public void onIPush(IntInsnNode node) {

    }

    @Override
    public void onLoadI32(VarInsnNode node) {

    }

    @Override
    public void onLoadI64(VarInsnNode node) {

    }

    @Override
    public void onStore32(VarInsnNode node) {

    }

    @Override
    public void onStore64(VarInsnNode node) {

    }

    @Override
    public void onMultiANewArray(MultiANewArrayInsnNode node) {

    }

    @Override
    public void onLookupSwitch(LookupSwitchInsnNode node) {

    }

    @Override
    public void onTableSwitch(TableSwitchInsnNode node) {

    }

    @Override
    public void onLabel(LabelNode node) {

    }

    @Override
    public void onIfICmpXX(JumpInsnNode node) {

    }

    @Override
    public void onIfXX(JumpInsnNode node) {

    }

    @Override
    public void onIfNull(JumpInsnNode node, boolean isNull) {

    }

    @Override
    public void onIfACmpEq(JumpInsnNode node, boolean isEqual) {

    }

    @Override
    public void onGoto(JumpInsnNode node) {

    }

    @Override
    public void onAThrow(InsnNode node) {

    }

    @Override
    public void onJsr(JumpInsnNode node) {

    }

    @Override
    public void onRet(VarInsnNode node) {

    }

    @Override
    public void onEnd() {
        
    }
}
