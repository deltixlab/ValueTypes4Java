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

import deltix.vtype.type.*;
import org.objectweb.asm.tree.*;

import java.util.List;

import static deltix.vtype.transformer.AsmUtil.*;
import static deltix.vtype.transformer.Warnings.REF_ASSIGN;
import static deltix.vtype.transformer.Warnings.UNBOXING_ASSIGN;
import static deltix.vtype.type.TypeId.*;
import static org.objectweb.asm.Opcodes.*;

public class SinglePassCodeTransformer extends CodeTransformerBase {

    private final VariableMap vars;
    private final FrameMap frameMap;
    private TypeArray savedLocalVarFrame;

    SinglePassCodeTransformer(TranslationState state) {

        super(state);
        savedLocalVarFrame = new TypeArray(16, this);
        this.frameMap = state.frameMap;
        this.vars = state.variableMap;
    }

    protected void init(MethodNode method) {

        if (logTrace) dbgBreak();
        super.init(method);
        initializeMethodArguments(vars, method, mapping);
        log.trace("Initial variable frame: %s", vars);
        saveVarFrame();
    }

    @Override
    public void onStart(Object context) {
        init((MethodNode)context);
    }

    @Override
    public void onVar(LocalVariableNode variable) {
        super.onVar(variable);
    }

    @Override
    public void onVarProcessingEnd(List varsUnused) {
        super.onVarProcessingEnd(null);
    }


    int adjustVarInsnIndex(VarInsnNode node) {

        return node.var = vars.adjustAddressSrc2Dst(node.var);
    }


    int adjustVarStoreInsnIndex(VarInsnNode node, int typeId, String name) {

        int aSrc = node.var;
        int aDst = node.var = vars.put(aSrc, typeId, name);
        if (logTrace) {
            log.trace("STORE %x|%s -> [%d]", typeId, format(typeId, name, aSrc), aSrc);
        }

        varTransformer.remapIndex(findNearNextLabel(node), typeId, aSrc, aDst);
        return aDst;
    }


    int adjustVarStoreInsnIndex(VarInsnNode node, int typeId) {

        return adjustVarStoreInsnIndex(node, typeId, null);
    }

    @Override
    int adjustIincInsnIndex(IincInsnNode node) {

        return node.var = vars.adjustAddressSrc2Dst(node.var);
    }

    // Processing variable access opcodes
    @Override
    public void onALoad(VarInsnNode node) {

        int aSrc = node.var;
        int aDst = adjustVarInsnIndex(node);
        int varType = vars.typeBySrcAddr(aSrc);

        if (isVt(varType)) {
            String varName = vars.dbgNameBySrcAddr(aSrc);

            if (isVtValue(varType)) {
                node.setOpcode(LLOAD);
                if(logDbg) log.dbg("LOAD VT %s <- [ %d ]", varName, aDst);
                vm.pushVType64(varType);
            } else if (isVtArray(varType)) {
                // We need separate processing to track usage of array type
                if(logDbg) log.dbg("LOAD VT[] %s <- [ %d ]", varName, aDst);
                vm.pushTypeId(varType);
            } else if (TypeId.isVtRef(varType)) {
                // This is unexpected!!
                log.wrn("LOAD VT ref %s / 0x%x <- [ %d ]", varName, varType, aDst);
                vm.pushVTypeRef(varType);
            } else {
                log.err("LOAD VT ??? %s / 0x%x <- [ %d ]", varName, varType, aDst);
                log.err(" at line: %d", currentLine());
                vm.pushTypeId(varType, vars.nameBySrcAddr(aSrc));
            }
        } else {
            vm.pushTypeId(varType, vars.nameBySrcAddr(aSrc));
        }
    }

    @Override
    public void onAStore(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        int storedType = vm.typeIdAt(0);
        String storedName = vm.nameAt(0);
        int aSrc = node.var;
        int aDst;
        int nSlots = vars.getAvailableDstSlotsAt(aSrc);

        if (isVtNonArray(storedType)) {
            if (TypeId.vtValueDstSize(storedType) <= nSlots) {
                node.setOpcode(LSTORE);
                int valueTypeId = TypeId.vtValueFrom(storedType);
                aDst = vars.put(aSrc, valueTypeId, storedName);

                if (!isVtValue(storedType)) {
                    insertUnboxing(node, storedType, valueTypeId, UNBOXING_ASSIGN);
                }

                storedType = valueTypeId;
            } else {
                // No space for value type, store as reference
                aDst = vars.put(aSrc, TypeId.vtRefFrom(storedType), storedName);
                if (isVtValue(storedType)) {
                    insertBoxingFrom(node, storedType, REF_ASSIGN);
                    storedType = TypeId.vtRefFrom(storedType);
                }
            }

            if(logDbg) {
                log.dbg("STORE VT %s -> [%d] vars: %s", format(vars.typeBySrcAddr(aSrc), vars.nameBySrcAddr(aSrc), aSrc), aSrc, vars);
            }

            vm.popAny();
        } else {
            aDst = vars.put(aSrc, storedType, storedName);
            if (logTrace) {
                log.trace("STORE %s -> [%d]", format(storedType, storedName, aSrc), aSrc);
            }

            vm.pop32();
        }

        node.var = aDst;
        varTransformer.remapIndex(findNearNextLabel(node), storedType, aSrc, aDst);
    }


    @Override
    public void onLoadI32(VarInsnNode node) {

        adjustVarInsnIndex(node);
        vm.push32i();
    }

    @Override
    public void onLoadI64(VarInsnNode node) {

        adjustVarInsnIndex(node);
        vm.push64i();
    }

    @Override
    public void onLoadF32(VarInsnNode node) {

        adjustVarInsnIndex(node);
        vm.push32f();
    }

    @Override
    public void onLoadF64(VarInsnNode node) {

        adjustVarInsnIndex(node);
        vm.push64f();
    }

    @Override
    public void onStore32(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 1);
        adjustVarStoreInsnIndex(node, node.getOpcode() == FSTORE ? TypeId.F32 : TypeId.I32);
        vm.pop32();
    }

    @Override
    public void onStore64(VarInsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        adjustVarStoreInsnIndex(node, node.getOpcode() == DSTORE ? TypeId.F64 : TypeId.I64);
        vm.pop64();
    }


    @Override
    public void onAALoad(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        vm.pop32(); // Pop array index
        int arrayTypeId = vm.typeIdAt(0);
        if (isVt(arrayTypeId)) {
            if (isVtArray(arrayTypeId)) {
                if (isVtA1Ref(arrayTypeId)) {
                    log.dbg("AALOAD -> LALOAD X[]");
                    replaceWithBasic(node, LALOAD);
                    vm.pop32();
                    vm.pushVType64(TypeId.vtValueFrom(arrayTypeId));
                } else {
                    log.dbg("AALOAD X[]..[]");
                    vm.pop32();
                    vm.pushTypeId(arrayFrom(vtValueFrom(arrayTypeId), getArrayDepth(arrayTypeId) - 1));
                    // TODO: Arraydecrementdepth
                }
            } else {
                log.err("AALOAD: unsupported type on the stack: %x/%s at line: %d", vm.typeIdAt(0),  vm.nameAt(0), currentLine());
            }
        } else {
            vm.pop32();
            vm.pushTypeId(getArrayElement(arrayTypeId));
        }
    }

    @Override
    public void onAAStore(InsnNode node) {

        vm.checkSrcUnderflow32(node, 3);
        int arrayTypeId = vm.typeIdAt(2);
        // TODO: ..

        if (isVt(arrayTypeId)) {
            if (isVtArray(arrayTypeId)) {
                if (isVtA1Ref(arrayTypeId)) {
                    log.dbg("AASTORE -> LSTORE X[]");
                    popReferenceWithOptionalUnboxingTo(node, TypeId.vtValueFrom(arrayTypeId), Warnings.UNBOXING_UNKNOWN);
                    replaceWithBasic(node, LASTORE);
                } else {
                    int valueTypeId = vm.typeIdAt(0);
                    if (isDst32(valueTypeId)) {
                        log.dbg("AASTORE X[]..[]");
                        vm.pop32();
                    } else {
                        log.err("AASTORE: unsupported type on the stack[0]: %s at line: %d", valueTypeId, currentLine());
                    }
                }
            } else {
                log.err("AASTORE: unsupported type on the stack: %x/%s[2] at line: %d", arrayTypeId, vm.nameAt(2), currentLine());
            }
        } else {
            popReferenceWithOptionalBoxing(node, Warnings.REF_ASSIGN);
        }

        vm.pop32();
        vm.pop32();
    }


    @Override
    public void onDup64(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_pop.html

        vm.checkSrcUnderflow32(node, 2);
        int stackType0 = vm.typeIdAt(0);

        // Basic 64-bit type?
        if (isSrc64(stackType0)) {
            vm.dup64();
            return;
        }

        int stackType1 = vm.typeIdAt(1);

        // We have [32 32] or [64 32] on the stack. Latter situation is an error.
        if (isSrc64(stackType1)) {
            throw new RuntimeException("DUP2: Illegal stack contents, partial access to a 64-bit value");
        }

        // We have [32 32] on the stack. One of which (or both) can be a VType.
        if (!isVtValue(stackType0) && !isVtValue(stackType1)) {
            vm.dup2();
        } else {
            int aDst0 = vars.topDstAddr();
            generateLocalVarPush(node);         //  [B A]   -> [B]
            int aDst1 = vars.topDstAddr();
            generateLocalVarPush(node);         //  [B]     -> []

            generateLocalVarLoad_(node, aDst1); //   []      -> [B]
            generateLocalVarLoad_(node, aDst0); //   [B]     -> [B A]
            generateLocalVarLoad_(node, aDst1); //   [B A]   -> [B A B]
            generateLocalVarLoad_(node, aDst0); //   [B A B] -> [B A B A]
            vars.removeLast(2);
            remove(node);
        }
    }


    @Override
    public void onDup32x(InsnNode node, int depth) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html
        vm.checkSrcUnderflow32(node, depth + 1);

        // This call will also validate basic stack state etc.
        if (vm.tryDupX(depth))
            return;

        int t0 = vm.typeIdAt(0);
        int t1 = vm.typeIdAt(1);
        assert(isVtValue(t0) || isVtValue(t1));

        if (1 == depth) {
            if (isVtValue(t0)) {
                // VT]
                if (isVtValue(t1)) {
                    // VT VT]
                    replaceWithBasic(node, DUP2_X2);
                } else {
                    // 32? VT]
                    replaceWithBasic(node, DUP2_X1);

                }
            } else {
                if (isVtValue(t1)) {
                    // VT 32]
                    replaceWithBasic(node, DUP_X2);
                } else {
                    log.err("Logic Error in DUP_X1 [VT]");
                    return;
                }
            }

            vm.dupX(1, 1);
        } else {
            int t2 = vm.typeIdAt(2);
            // Fuck this
            throw new UnsupportedOperationException("DUP_X2 (VT)");
            /*if (isVtValue(t0)) {
                // VT]
                if (isVtValue(t1)) {
                    // VT VT]
                    throw new UnsupportedOperationException("DUP_X*");
                } else {
                    // 32 VT]
                    if (isVtValue(t2)) {
                        // VT 32 VT]
                        throw new UnsupportedOperationException("DUP_X*");
                    }
                        // VT 32 VT]
                        throw new UnsupportedOperationException("DUP_X*");
                    } else {
                        // 32 32 VT]
                        replaceWithBasic(node, DUP2_X2);
                        vm.dupX(2, 1);
                    }
                }
            } else {
                // 32]
                if (isVtValue(t1)) {
                    throw new UnsupportedOperationException("DUP_X*");
                } else {
                    if (isVtValue(t2)) {
                        // Need custom impl here
                    } else {
                        replaceWithBasic(node, DUP2_X2);
                        vm.dupX(2, 1);
                    }
                }

            }
            */
        }
    }


    @Override
    public void onDup64x(InsnNode node, int depth) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html
        vm.checkSrcUnderflow32(node, depth + 2);

        // This call will also validate basic stack state etc.
        if (vm.tryDupX2(depth))
            return;

        // TODO: Finish other versions of the instruction!!!!
        throw new UnsupportedOperationException("DUP2_X*");
    }


    @Override
    public int varTop() {
        return vars.topDstAddr();
    }

    void restoreVarFrame() {
        vars.restoreFrom(savedLocalVarFrame);
    }

    void saveVarFrame() {
        vars.saveTo(savedLocalVarFrame);
    }

    @Override
    public void onLabel(LabelNode node) {
        FrameEntry localFrame = frameMap.getFrames(node);
        // no need to logger, already logged in trace mode
        varTransformer.onLabel(node);
        if (null != localFrame) {
            transformLocalFrame(node, localFrame, 0, true);
        }

        super.onLabel(node);
    }


    protected void applyFrameVars(List vars) {

        if (null != vars) {
            int i = 0;
            for (Object o : vars) {
                int typeId = addVarFromFrame(o);
                if (isVt(typeId)) {
                    vars.set(i, typeIdToFrameEntry(typeId, mapping.typeIdToDstTypeDesc(typeId)));
                }

                ++i;
            }
        }
    }


    protected void applyFrameStack(List stack, int count) {

        if (null != stack) {
            int i = 0;
            for (Object o : stack) {
                int typeId = vm.pushAsmFrameObject(o, mapping);
                if (isVt(typeId)) {
                    stack.set(i, typeIdToFrameEntry(typeId, mapping.typeIdToDstTypeDesc(typeId)));
                }

                if (++i >= count)
                    break;
            }
        }
    }

    @Override
    public void onFrame(FrameNode node) {

        // This code now performs frame node contents synchronization.
        // The state of the stack/vars should be already compatible with the contents of this frame node
        // after processing the LabelNode that precedes it. (see onLabel() )
        // So we can get away with just basic Value Type substitution without actually checking the current interpreter state
        log.dbg("Stack Frame: " + stackFrameToString(node));
        switch (node.type) {
            case F_SAME:
                restoreVarFrame();
                log.trace("Reset vars to previous frame: %s", vars);
                resetAndSaveStackFrame();
                break;

            case F_SAME1:
                restoreVarFrame();
                //System.out.print(node.stack.get(0).toString());
                log.trace("Reset vars to previous frame: %s", vars);
                resetStackFrame();
                applyFrameStack(node.stack, 1);
                saveStackFrame();
                break;

            case F_APPEND:
                restoreVarFrame();
                applyFrameVars(node.local);
                saveVarFrame();
                log.trace("Add %d vars. Vars: %s", node.local.size(), vars);
                resetAndSaveStackFrame();
                break;

            case F_CHOP:
                restoreVarFrame();
                vars.removeLast(node.local.size());
                saveVarFrame();
                log.trace("Chopped %d vars. Vars left: %s", node.local.size(), vars);
                resetAndSaveStackFrame();
                break;

            case F_NEW:
            case F_FULL:
                //System.out.print(node.stack.get(0).toString());
                vars.clear();
                applyFrameVars(node.local);
                saveVarFrame();

                log.trace("Reloaded full vars/stack frame: %s", vars);
                resetStackFrame();
                applyFrameStack(node.stack, Integer.MAX_VALUE);
                saveStackFrame();
                break;
        }
    }


    private int addVarFromFrame(Object o) {

        return AsmUtil.addVarFromFrame(vars, mapping, o);
    }

    /**
     * Generate an instruction that reads variable contents (by source address) and moves them to stack,
     * adjusting stack state accordingly
     */
    private int generateLocalVarLoad_(AbstractInsnNode node, int aSrc) {

        int typeId = vars.typeBySrcAddr(aSrc);
        String name = vars.nameBySrcAddr(aSrc);
        if (TypeId.VOID != typeId) {
            vm.pushTypeId(typeId, name);
            insertLoadVar(node, typeId, vars.src2dstAddr(aSrc));
        }

        return typeId;
    }

    /**
     * Generate an instruction that moves last frame variable to operand stack,
     * adjusting variable frame and stack state accordingly. You are supposed to know its typeId already
     */
    private int generateLocalVarPop(AbstractInsnNode node, int typeId, String name) {

        if (TypeId.VOID != typeId) {
            vm.pushTypeId(typeId, name);
        }

        int oldTypeId = vars.removeLast(1);
        if (TypeId.VOID != typeId) {
            insertLoadVar(node, typeId, vars.topDstAddr());
        }

        return oldTypeId;
    }


    /**
     * Generate an instruction that moves last frame variable to operand stack,
     * adjusting variable frame and stack state accordingly
     */

    private int generateLocalVarPop(AbstractInsnNode node) {

        int typeId = generateLocalVarLoad_(node, vars.getLastVarSrcAddr());
        vars.removeLast(1);
        return typeId;
    }

    /**
     * Generate an instruction that adds top of operand stack to local variable frame,
     * adjusting variable frame and stack state accordingly
     */
    void generateLocalVarPush(AbstractInsnNode node, int typeIdSrc, int typeIdDst, String name) {

        int aDst = vars.topDstAddr();
        // Try to add variable
        vars.add(typeIdDst, null == name && TypeId.VOID != typeIdSrc ? vm.nameAt(0) : name);
        if (TypeId.VOID != typeIdDst) {
            insertStoreVar(node, typeIdDst, aDst);
        }

        if (TypeId.VOID != typeIdSrc) {
            vm.popMany(1);
        }
    }


    void generateLocalVarPush(AbstractInsnNode node) {

        int typeId = vm.typeIdAt(0);
        int aDst = vars.topDstAddr();
        if (TypeId.VOID == typeId) {
            log.err("Stack can't contain <void> value");
        }

        vars.add(typeId, vm.nameAt(0));
        insertStoreVar(node, typeId, aDst);
        vm.popMany(1);
    }


    private void transformLocalVarFrame(AbstractInsnNode node, int varAddr, TypeArray frame, int frameAddr, int count) {

        // Will overwrite part of the variable frame, leaving vars before and after the overwritten area intact
        // may cause variable frame to grow
        // this is not insert/delete operation

        if (count <= 0)
            return;

        // Skip vars with matching types than don't need conversion. This operation is possibly redundant.
        int topSrc = vars.topSrcAddr();
        while (varAddr < topSrc && vars.typeBySrcAddr(varAddr) == frame.getId(frameAddr)) {

            varAddr = vars.nextSrcAddr(varAddr);
            ++frameAddr;
            if (0 == --count)
                return;
        }

        // Index of the 1st var after the ones that will be transformed
        int firstPreserved = varAddr;
        for (int i = 0; i < count; i++) {
            firstPreserved = vars.nextSrcAddr(firstPreserved);
        }

        assert(firstPreserved <= topSrc);
        TypeArray tailVars = null;
        TypeArray replacedVars = vars.copyOfRange(varAddr, firstPreserved);

        if (firstPreserved < topSrc) {
            // We are doing copy because "void" vars can't go to stack, but we must still remember their position
            tailVars = vars.copyOfRange(firstPreserved, topSrc);
            for (int i = tailVars.length() - 1; i >= 0; --i) {
                int removedType = generateLocalVarPop(node, tailVars.getId(i), tailVars.getName(i));
                assert(tailVars.getId(i) == removedType);
            }
        }

        assert(firstPreserved == vars.topSrcAddr());

        for (int i = count - 1; i >= 0; --i) {
            int removedType = generateLocalVarPop(node, replacedVars.getId(i), replacedVars.getName(i));
            assert(replacedVars.getId(i) == removedType);
        }

        assert(varAddr == vars.topSrcAddr());

        for (int i = 0; i < count; ++i) {
            int typeIdSrc = replacedVars.getId(i);
            int j = frameAddr + i;
            int typeIdDst = frame.getId(j);
            String name = frame.getName(j);
            convertVariableIfNeeded(node, typeIdSrc, typeIdDst, name, Warnings.FRAME_TRANSFORM);
            generateLocalVarPush(node, typeIdSrc, typeIdDst, name);
        }

        if (null != tailVars) {
            assert(tailVars != null);
            for (int i = 0, n = tailVars.length(); i < n; ++i) {
                int id = tailVars.getId(i);
                generateLocalVarPush(node, id, id, tailVars.getName(i));
            }
        }

        //assert(vars.numVars() == numVarsNew);
    }


    private void transformLocalStackFrame(AbstractInsnNode node, int dstOffset, TypeArray src, int srcOffset, int count) {

        if (count < 1)
            throw new IllegalArgumentException();

        //logger.trace("Vars before stack transform: %s", vars);
        vm.checkUnderflow(node, dstOffset + count);

        int nSavedVars = dstOffset + count - 1;
        int base = vars.numVars();

        log.trace("numSaved: %s", nSavedVars);

        // Transfer stack values to local var frame
        for (int i = 0; i < nSavedVars; ++i) {
            log.trace("%x -> vars", vm.typeIdAt(0));
            generateLocalVarPush(node);

        }

        int typeIdSrc0 = vm.typeIdAt(0);
        int typeIdDst0 = src.getId(srcOffset + count - 1);

        //logger.trace("Stack before unboxing deepest(%x -> %x): %s", typeIdSrc0, typeIdDst0, vm);
        boxOrUnboxIfNeeded(node, typeIdSrc0, typeIdDst0, Warnings.UNKNOWN);
        vm.popMany(1);
        vm.pushTypeId(typeIdDst0, src.getName(srcOffset + count - 1));

        //logger.trace("Stack after unboxing deepest: %s", vm);

        // We already processed item at depth count - 1, so start from count - 2
        for (int i = count - 2; i >= 0; --i) {
            int j = srcOffset + i;
            int typeIdDst = src.getId(j);
            String nameDst = src.getName(j);
            int typeIdSrc = generateLocalVarPop(node, typeIdDst, null);
            vm.renameTop(nameDst);
            log.trace("Stack before unboxing (%x -> %x): %s", typeIdSrc, typeIdDst, vm);
            boxOrUnboxIfNeeded(node, typeIdSrc, typeIdDst, Warnings.UNKNOWN);
            log.trace("Stack after unboxing: %s", vm);
            log.trace("%x <- vars", vm.typeIdAt(0));
        }


        for (int i = dstOffset; i > 0; --i) {
            generateLocalVarPop(node);
            log.trace("%x <- vars", vm.typeIdAt(0));
        }

        //logger.trace("Vars after stack transform: %s", vars);
        log.trace("Stack after transform: %s", vm);
    }


    void reloadLocalFrame(AbstractInsnNode node, FrameEntry frame) {

        vars.clear();
        int numVars = frame.getNumVars();
        for (int i = 0; i < numVars; ++i) {
            vars.add(frame.getVarTypes()[i], frame.getVarNames()[i]);
        }

        vm.resetStack();
        int numStack = frame.getNumStackEntries();
        for (int i = numStack - 1; i >= 0; --i) {
            vm.pushTypeId(frame.getStackTypes()[i], frame.getStackNames()[i]);
        }

        log.dbg("Reloaded all. vars: %s stack: %s", vars, vm);
    }


    void onVarFrameTransformFailure(String msg, int index, FrameEntry frame, int stackOffset, int typeIdSrc, int typeIdDst) {

        StringBuilder warningMsg = addWarningMsg (null,
                String.format("transformFrames(): %s at index [%d]: %x -> %x%n", msg, index, typeIdSrc, typeIdDst, currentLine()));
        printWarningIfExists(warningMsg, frame, stackOffset);
        log.err("Stack Frame conversion failed:%n%s", warningMsg);
    }

    /**
     *
     * @param node
     * @param frame contains a snapshot of the stack/locals that are expected at the current point, generated by JavaC
     * @param stackOffset number of extra values on the top of the stack that are ignored during verification
     *                    and preserved during transformation
     * @param discardExtraVars discard extra Locals after the specified stack frame (temporaries that won't be used anymore)
     *                         it is curently not used due to reliability concerns. It is just an optimization.
     */
    void transformLocalFrame(AbstractInsnNode node, FrameEntry frame, int stackOffset, boolean discardExtraVars) {
        int nSrc = vars.numVars();
        int topSrc = vars.topSrcAddr();
        int nDst = frame.getNumVars();

        int[] typesDst = frame.getVarTypes();

        int firstFailedSrcAddr = -1;
        int firstFailedFrameIndex = -1;
        int cmpResultTotal = 0;

        StringBuilder warningMsg = null;

        //dbgBreakAt("testUninitialized64BitVars1", 67);
        if (vm.isStackReset()) {
            assert(0 == stackOffset);
            reloadLocalFrame(node, frame);
            return;
        }

        boolean needs64BitTypeInsertionOrDeletion = false;
        for (int iSrc = 0, iDst = 0, nextSrc; iSrc < topSrc && iDst < nDst; iSrc = nextSrc, ++iDst) {
            int typeIdSrc = vars.typeBySrcAddr(iSrc);
            int typeIdDst = typesDst[iDst];
            nextSrc = iSrc + TypeId.size32Src(typeIdSrc);

            if (TypeId.VOID != typeIdSrc && TypeId.VOID == typeIdDst) {

                if (logDbg) {
                    // Initialized 64-bit type becomes uninitialized (replaced with 2 "void" cells)?
                    log.dbg("Will uninitialize var[%d]: %x:%s , ", iSrc, typeIdSrc, vars.dbgNameBySrcAddr(iSrc));
                }

                if (isSrc64(typeIdSrc)) {
                    // "Uninitialize" 64-bit type
                    if (iDst + 1 == nDst || TypeId.VOID != typesDst[iDst + 1]) {
                        onVarFrameTransformFailure("Invalid bytecode: Variable can't be UNinitialized as 64-bit type",
                                iSrc, frame, stackOffset, typeIdSrc, typeIdDst);
                        return;
                    }

                    needs64BitTypeInsertionOrDeletion = true;
                    ++iDst;
                    continue;
                }

                // Uninitialize 32-bit type
                if (!isDst64(typeIdSrc)) {
                    vars.replaceVarBySrcAddr(iSrc, TypeId.VOID, null);
                    continue;
                }

                // Otherwise, need more complex conversion
            }

            // Uninitialized 64-bit type becomes initialized?
            if (TypeId.VOID == typeIdSrc && TypeId.isSrc64(typeIdDst)) {

                if (logDbg) {
                    log.dbg("Will initialize var[%d] <- %x:%s , ", iSrc, typeIdDst, frame.getVarNames()[iDst]);
                }

                if (iSrc + 1 == topSrc || TypeId.VOID != vars.typeBySrcAddr(iSrc + 1)) {
                    onVarFrameTransformFailure("Invalid bytecode: Variable can't be initialized as 64-bit type",
                            iSrc, frame, stackOffset, typeIdSrc, typeIdDst);
                    return;
                }

                needs64BitTypeInsertionOrDeletion = true;
                nextSrc = iSrc + 2;
                continue;
            }

            int cmpResult = TypeIdCast.checkCastFrame(typeIdSrc, typeIdDst) & ~TypeIdCast.HAS_VTYPE;
            cmpResultTotal |= cmpResult;

            if (TypeIdCast.isFailure(cmpResult)) {
                onVarFrameTransformFailure("Variable types incompatible", iSrc, frame, stackOffset, typeIdSrc, typeIdDst);
                return;
            } else if (TypeIdCast.SUCCESS != cmpResult) {
                if (-1 == firstFailedSrcAddr) {
                    firstFailedSrcAddr = iSrc;
                    firstFailedFrameIndex = iDst;
                }

                if (logDbg) {
                    log.dbg("var[%d] %d:%s: %x -> %x", iSrc, typeIdSrc, vars.dbgNameBySrcAddr(iSrc), typeIdSrc, typeIdDst);
                }
            }
        }

        // This operation does not change variable indices, therefore does not participate in var. frame debug info synchronization
        // It only replaces 2 <top> slots with one <64-bit>, or replaces one <64-bit> with 2 <top>, possibly in several places
        if (needs64BitTypeInsertionOrDeletion) {

            log.trace("Will init/deinit vars: %s%n", vars);
            for (int iSrc = 0, iDst = 0, nextSrc; iSrc < topSrc && iDst < nDst; iSrc = nextSrc, ++iDst) {
                int typeIdSrc = vars.typeBySrcAddr(iSrc);
                int typeIdDst = typesDst[iDst];
                nextSrc = iSrc + TypeId.size32Src(typeIdSrc);

                if (isSrc64(typeIdSrc) && TypeId.VOID == typeIdDst) {
                    assert(TypeId.VOID == typesDst[iDst + 1]);
                    ++iDst;
                    vars.replaceVarBySrcAddr(iSrc, TypeId.VOID, null);
                    log.dbg("var[%d] %s: %x -> * *", iSrc, vars.dbgNameBySrcAddr(iSrc), typeIdSrc);
                    //vars.replaceVarUnsafe(iSrc, TypeId.VOID, "*");
                    //vars.insertUnsafe(iSrc + 1, TypeId.VOID, "*");
                }

                // Uninitialized 64-bit type becomes initialized?
                else if (TypeId.VOID == typeIdSrc && TypeId.isSrc64(typeIdDst)) {
                    //vars.replaceEmptyByIndex(iSrc, typeIdDst, frame.getVarNames()[iDst]);
                    //vars.replaceVarUnsafe(iSrc, typeIdDst, frame.getVarNames()[iDst]);
                    //vars.removeUnsafe(iSrc + 1);
                    log.dbg("var[%d] %s: * * -> %x", iSrc, frame.getVarDbgName(iDst), typeIdDst);
                    vars.replaceVarBySrcAddr(iSrc, typeIdDst, frame.getVarNames()[iDst]);
                    nextSrc = iSrc + 2;
                }
            }

            nSrc = vars.numVars(); // Update the number of vars.
        }

        if (nSrc < nDst) {
            onVarFrameTransformFailure(String.format("More local variables expected! actual: %d < expected: %d", nSrc, nDst),
                    0, frame, stackOffset, 0, 0);
            return;
        }

        if (TypeIdCast.SUCCESS != cmpResultTotal && !TypeIdCast.isFailure(cmpResultTotal)) {
            //logger.wrn("Need to transform vars: %s", vars);
            if (logDbg) {
                if (logTrace) {
                    log.trace("Will transform vars: %s%n", vars);
                    log.trace("To                 : %s%n", frame);
                    log.trace("Starting from [%d / %d] , cmpResult: %x", firstFailedSrcAddr, firstFailedFrameIndex, cmpResultTotal);
                } else {
                    log.dbg("transformFrames(): cmpResult: %x", cmpResultTotal);
                }
            }

            TypeArray saved = vars.copyAll();

            transformLocalVarFrame(node, firstFailedSrcAddr, frame.copyOfVarRange(firstFailedFrameIndex, frame.getNumVars()),
                    0, frame.getNumVars() - firstFailedFrameIndex);

            if (logDbg) {
                log.trace("Vars transformed to: %s", vars);
            }

            if (saved.length() != vars.numVars() || !saved.equalTypes(vars.copyAll())) {
                if (logDbg) {
                    log.dbg("Synchronizing debug information: %s", vars);
                }

                // If the current node is not label then add a new label
                LabelNode label = node instanceof LabelNode ? (LabelNode) node : insertBefore(node, new LabelNode());

                for (int iSrc = 0, n = vars.topSrcAddr(); iSrc < n; iSrc = vars.nextSrcAddr(iSrc)) {
                    varTransformer.remapIndex(label, vars.typeBySrcAddr(iSrc), iSrc, vars.src2dstAddr(iSrc));
                }
            }
        }

        nDst = frame.getNumStackEntries();
        vm.checkSrcUnderflow32(node, nDst);

        boolean stackConversionFail = false;

        if (nDst != vm.top() - stackOffset) {
            warningMsg = addWarningMsg(warningMsg,
                    String.format("Stack depth mismatch! expected: %d+%d, actual: %d%n", nDst, stackOffset, vm.top()));
            stackConversionFail = true;
        }

        typesDst = frame.getStackTypes();
        cmpResultTotal = 0;
        int lastFailedDepth = -1;

        // Iterate and compare stack entries from lower to higher depth
        for (int i = 0; i < nDst; ++i) {
            int typeIdSrc = vm.typeIdAt(i + stackOffset);
            int typeIdDst = typesDst[i];

            int cmpResult = TypeIdCast.checkCastFrame(typeIdSrc, typeIdDst) & ~TypeIdCast.HAS_VTYPE;
            cmpResultTotal |= cmpResult;

            if (TypeIdCast.isFailure(cmpResult)) {
                warningMsg = addWarningMsg(warningMsg,
                        String.format("Failed to convert stack[]: %s: %x -> %x\n", i, vm.nameAt(i + stackOffset), typeIdSrc, typeIdDst));
                stackConversionFail = true;
            }
            else if (TypeIdCast.SUCCESS != cmpResult) {
                lastFailedDepth = i;
                log.dbg("stack %s: %x -> %x\n", vm.nameAt(i + stackOffset), typeIdSrc, typeIdDst);
                //logger.dbg("stack %s: %x -> %x", vars.getNames()[i], typeIdSrc, typeIdDst);
            }
        }

        if (TypeIdCast.SUCCESS != cmpResultTotal) {
            log.trace("Need to transform stack: [%d..%d](+%d)",
                    lastFailedDepth + stackOffset, stackOffset, stackOffset);
            log.trace("Stack: %s%ncmpResult was: %x", vm, cmpResultTotal);
            TypeArray to = frame.copyOfStackRange(0, lastFailedDepth + 1);
            log.trace("Transform to: [%d..0] %s", lastFailedDepth, to);

            transformLocalStackFrame(node, stackOffset, to,0, lastFailedDepth + 1);

            log.trace("Stack transformed to: %s", vm);
        }

        printWarningIfExists(warningMsg, frame, stackOffset);
    }


    @Override
    void transformFramesBeforeBranch(AbstractInsnNode node, LabelNode label, int stackOffset, boolean isUnconditional) {
        // TODO: Add branch side merging!
        // TODO: Discard extra vars after the branch side merging
        FrameEntry localFrame = frameMap.getFrames(label);
        if  (null == localFrame) {
            log.err("transformFrames(): frame Map not found for label: %s when jumping from line: %d",
                    label.getLabel(), currentLine());
            return;
        }

        transformLocalFrame(node, localFrame, stackOffset, isUnconditional);
    }


    /**
     * Some utility methods
     */

    /**
     *
     * @param warningMsg will print this if not null
     * @param frame will logger this stack frame
     * @param stackOffset will logger this stack offset
     */

    private void printWarningIfExists(StringBuilder warningMsg, FrameEntry frame, int stackOffset) {
        if (null != warningMsg) {
            warningMsg.append("Frame: ").append(frame)
                    .append("\nStack (+").append(stackOffset).append("): ").append(vm)
                    .append("\nVars: ").append(vars);

            System.out.println(warningMsg);
            method().setShouldLogMethodName(true);
        }
    }

    private StringBuilder addWarningMsg(StringBuilder warningMsg, String str) {

        if (null == warningMsg) {
            warningMsg = new StringBuilder("WARNING!(@line: ").append(currentLine()).append("): ");
            //throw new RuntimeException();
        }

        if (null != str) {
            warningMsg.append(str);
        }

        return warningMsg;
    }
}
