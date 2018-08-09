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

import org.objectweb.asm.tree.*;

import java.util.List;


public interface OpcodeProcessor {

    void onStart(Object context);

    void onVarProcessingEnd(List vars);

    void onEnd();

    boolean shouldProcessMethodCode();

    void onUnderflow(AbstractInsnNode node);

    void onReturnStackMismatch(InsnNode node);

    void onStackTypeMismatch();

    /**
     *  Generic stack operations
     */

    void onPop32(InsnNode node);

    void onPop64(InsnNode node);

    void onDup32(InsnNode node);

    /**
     * @param depth in 32-bit cells
     */
    void onDup32x(InsnNode node, int depth);

    void onDup64(InsnNode node);

    /**
     * @param depth in 32-bit cells
     */
    void onDup64x(InsnNode node, int depth);

    void onSwap32(InsnNode node);

    /**
     * ALU
     */

    void onAluI32(InsnNode node);

    void onAluI64(InsnNode node);

    void onAluF32(InsnNode node);

    void onAluF64(InsnNode node);

    void onShift64(InsnNode node);

    void onAluToI32Unary(InsnNode node);

    void onAluToI64Unary(InsnNode node);

    void onAluToF64Unary(InsnNode node);

    void onAluToF32Unary(InsnNode node);

    void onCast32toI64(InsnNode node);

    void onCast64toI32(InsnNode node);

    void onCast64toF32(InsnNode node);

    void onCast32toF64(InsnNode node);

    void onCastF32I32(InsnNode node);

    void onCastI32F32(InsnNode node);

    void onCastF64I64(InsnNode node);

    void onCastI64F64(InsnNode node);


    /**
     * Comparisons and tests
     */

    void onLCmp(InsnNode node);

    void onDCmp(InsnNode node);

    void onFCmp(InsnNode node);


    /**
     * Method Invocations and Field Access
     */

    void onGetStatic(FieldInsnNode node);

    void onGetField(FieldInsnNode node);

    void onPutStatic(FieldInsnNode node);

    void onPutField(FieldInsnNode node);

    void onMethod(MethodInsnNode node);

    void onInvokeDynamic(InvokeDynamicInsnNode node);


    /**
     * Constant loading
     */

    void onConstI32(InsnNode node);

    void onConstI64(InsnNode node);

    void onConstF32(InsnNode node);

    void onConstF64(InsnNode node);

    void onAConstNull(InsnNode node);

    void onLdc(LdcInsnNode node);


    /**
     * Variable access
     */

    void onALoad(VarInsnNode node);

    void onAStore(VarInsnNode node);

    void onLoadI32(VarInsnNode node);

    void onLoadI64(VarInsnNode node);

    void onLoadF32(VarInsnNode node);

    void onLoadF64(VarInsnNode node);

    void onStore32(VarInsnNode node);

    void onStore64(VarInsnNode node);

    void onIInc(IincInsnNode node);


    /**
     * Array access
     */

    void onAALoad(InsnNode node);

    void onAAStore(InsnNode node);

    void onAI32Load(InsnNode node);

    void onAI64Load(InsnNode node);

    void onAF32Load(InsnNode node);

    void onAF64Load(InsnNode node);

    void onA32Store(InsnNode node);

    void onA64Store(InsnNode node);

    void onArrayLength(InsnNode node);


    /**
     * Type creation/conversion
     */

    void onNew(TypeInsnNode node);

    void onANewArray(TypeInsnNode node);

    void onCheckCast(TypeInsnNode node);

    void onInstanceOf(TypeInsnNode node);

    void onMultiANewArray(MultiANewArrayInsnNode node);

    void onNewArray(IntInsnNode node);


    /**
     * Jumps
     */

    void onGoto(JumpInsnNode node);
    void onAThrow(InsnNode node);
    void onJsr(JumpInsnNode node);
    void onRet(VarInsnNode node);
    void onIfICmpXX(JumpInsnNode node);
    void onIfXX(JumpInsnNode node);
    void onIfNull(JumpInsnNode node, boolean isNull);
    void onIfACmpEq(JumpInsnNode node, boolean isEqual);
    void onLookupSwitch(LookupSwitchInsnNode node);
    void onTableSwitch(TableSwitchInsnNode node);


    /**
     * Return
     */

    // No args
    void onReturn(InsnNode node);

    // 32-bit non-ref RV
    void onReturn32(InsnNode node);

    // 64-bit RV
    void onReturn64(InsnNode node);

    // ref RV
    void onAReturn(InsnNode node);


    /**
     * Metadata
     */

    void onFrame(FrameNode node);

    void onLine(LineNumberNode node);

    void onVar(LocalVariableNode variable);

    void onLabel(LabelNode node);


    /**
     * Other
     */

    void onMonitorEnter(InsnNode node);

    void onMonitorExit(InsnNode node);

    void onIPush(IntInsnNode node);
}
