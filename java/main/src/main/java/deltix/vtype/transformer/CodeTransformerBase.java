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

import deltix.vtype.mapping.*;
import deltix.vtype.type.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashMap;

import static deltix.vtype.transformer.AsmUtil.loadOpcodeForTypeId;
import static deltix.vtype.transformer.AsmUtil.stackFrameToString;
import static deltix.vtype.transformer.AsmUtil.storeOpcodeForTypeId;
import static deltix.vtype.type.DescriptorParser.getTransformedDesc;
import static deltix.vtype.type.TypeId.*;
import static deltix.vtype.type.TypeId.isDst32;
import static deltix.vtype.type.TypeIdCast.*;
import static org.objectweb.asm.Opcodes.*;


public abstract class CodeTransformerBase extends StackWalkHandler implements VariableNameFormatter {
    private final TranslationState state;
    protected InsnList instructions;
    protected final Mapping mapping;
    protected final MethodNameTransformer nameConverter;
    protected MethodNode parentMethod;
    private Warnings warnings;

    protected int returnTypeId;
    protected final VarListTransformer varTransformer;

    private boolean isConstructor;
    private boolean isStaticConstructor;
    private boolean needAutoInitInjectionPoint;

    // VT field initialization code will be inserted AFTER this node, if defined.
    // Best location is before the 1st putfield or 1st branch. Inserting before RETURN is problematic because there may be many.
    // Inserting after putfield is obviously wrong unless we track _all_ fields initialized by putfield.
    private AbstractInsnNode initializationPatchNode;
    protected LabelNode firstLabel;
    private boolean suppressAutoInitInjection;

    // Scratchpad TypeId arrays
    final int[] indyBodyArgs        = new int[0x100];
    final int[] indyInterfaceArgs   = new int[0x100];
    final int[] indyImplArgs        = new int[0x100];

    CodeTransformerBase(TranslationState state) {

        super(state.stack, state.instructionIterator, state.logger);
        this.state          = state;
        this.mapping        = state.mapping;
        this.nameConverter  = state.methodNameConverter;
        this.varTransformer = state.debugVarsListTransformer;
    }

    protected void init(MethodNode method) {

        super.init();
        this.parentMethod           = method;
        this.instructions           = method.instructions;
        this.warnings               = this.method().warnings;
        String name                 = method.name;
        this.isStaticConstructor    = name.equals("<clinit>");
        this.isConstructor          = name.equals("<init>") || this.isStaticConstructor;

        this.initializationPatchNode = null;
        this.firstLabel             = null;

        // TODO: only set if there are VT fields
        this.needAutoInitInjectionPoint = isConstructor;
        DescriptorParser.parseMethod(methodArgs, 0, method().originalDesc, mapping);
        returnTypeId = methodArgs[0];
    }


    protected MethodNode method() {
        return (MethodNode)this.parentMethod;
    }


//    void addWarning(int id, int line, Object data) {
//        warnings.add(id, line, data);
//    }
//
//    void addWarning(int id, int line) {
//        warnings.add(id, line, null);
//    }

    protected String dbgStr(int typeId) {
        return typeId + " : " + mapping.typeIdToShortPrintableString(typeId);
    }

    protected String dbgMethodOldStr() {
        return method().className + "." + method().originalName + method().originalDesc;
    }

    protected String dbgMethodNewStr() {
        return method().className + "." + method().name + method().desc;
    }

    protected void dbgBreakAt(int lineNum) {
        if (currentLine() == lineNum) {
            dbgBreak();
        }
    }

    protected void dbgBreakAt(String methodName, int lineNum) {
        if (method().name.equals(methodName) && currentLine() == lineNum) {
            dbgBreak();
        }
    }

    protected void dbgBreakAt(String methodName) {
        if (method().name.equals(methodName)) {
            dbgBreak();
        }
    }

    protected void dbgBreakAtClass(String className) {
        if (method().className.equals(className)) {
            dbgBreak();
        }
    }


    protected void dbgBreakAt(String methodName, int lineNum1, int lineNum2) {
        if (method().originalName.equals(methodName) && currentLine() >= lineNum1 && currentLine() <= lineNum2) {
            dbgBreak();
        }
    }


    protected void addWarning(int id, Object data) {

        warnings.add(id & 0xFFFF, currentLine(),
                id >= 0x10000 ? (null == data ? "" : (String)data) + " arg" + ((id >> 16) - 1) : data);
    }

    protected void addWarning(int id) {
        addWarning(id, null);
    }

    protected void addWarningWithType(int id, int typeId) {
        String typeStr = toPrintableString(typeId);
        addWarning(id, typeStr);
    }

    // TODO: Add counters and warnings for these 3 cases!!!
    protected void onVTypeVarValueAutoInitialization() {
    }

    protected void onVTypeVarValueAutoDiscard() {
    }

    protected void onVTypeVarAutoUnboxing() {
    }

    protected void onVTypeVarAutoBoxing() {
    }

    protected void onVTypeVarNewArray(int typeId) {
        addWarningWithType(Warnings.ANEWARRAYFILL, typeId);
    }

    protected void onVTypeVarNewMultiArray(int typeId) {
        addWarningWithType(Warnings.ANEWMULTIARRAY, typeId);
    }

    protected void onVTypeVarUninitializedBoxing(int stackType, int srcIndex) {
//        System.out.printf("WARNING: Boxing new ValueType %s into uninitialized var[%d] at %s line: %d%n",
//                TypeId.toString(stackType, mapping), dstIndex, method.name, currentLine());

        if (warnings.isSupressed(Warnings.UNINIT_TRANSFORM))
            return;

        addWarning(Warnings.UNINIT_TRANSFORM, format(stackType, null, srcIndex));
    }

    protected String toPrintableString(int typeId) {
        return mapping.typeIdToShortPrintableString(typeId);
    }

    protected void onVTypeRefCmp(int typeId0, int typeId1, boolean isEquality, boolean isRefOnly) {

        String typeStr0 = toPrintableString(typeId0);
        String typeStr1 = toPrintableString(typeId1);
        addWarning(isRefOnly ? Warnings.REF_CMP : Warnings.VALUE_CMP,
                typeStr0 + (isEquality ? " == " : " != ") + typeStr1);
    }


    protected InsnNode replaceWithBasic(AbstractInsnNode node, int opcode) {
        InsnNode newnode;
        instructions.insertBefore(node, newnode = new InsnNode(opcode));
        instructions.remove(node);
        return newnode;
    }

    protected int getDescTypeId(final String desc) {
        return DescriptorParser.getDescTypeId(desc, mapping);
    }

    protected int getClassTypeId(final String desc) {
        return DescriptorParser.getClassTypeId(desc, mapping);
    }

    protected ClassDef getClassFromTypeId(int typeId) {
        return mapping.getClassDef(TypeId.getVtClassIndex(typeId));
    }

    protected ClassDef getClassFromIndex(int index) {
        return mapping.getClassDef(index);
    }

    protected void remove(AbstractInsnNode node) {
        instructions.remove(node);
    }

    /**
     * Insert a node before the specified node. Return the inserted node
     * @param node newnode will be inserted before this one
     * @param newnode node to insert
     * @param <T> Type of the node being inserted
     * @return
     */
    protected <T extends AbstractInsnNode> T insertBefore(AbstractInsnNode node, T newnode) {
        instructions.insertBefore(node, newnode);
        return newnode;
    }

    /**
     * Insert a basic InsnNode before the specified node. Return the inserted node
     * @param node new node will be inserted before this one
     * @param opcode opcode of the new node
     * @return
     */
    protected AbstractInsnNode insertBasic(AbstractInsnNode node, int opcode) {
        return insertBefore(node,  new InsnNode(opcode));
    }

    /**
     * Insert several basic instruction nodes before the specified node
     * @param node new nodes will be inserted before this one
     * @param opcode1 opcode for the 1st node
     * @param opcode2 opcode of the 2nd node
     */
    protected void insertBasic(AbstractInsnNode node, int opcode1, int opcode2) {
        insertBasic(node, opcode1);
        insertBasic(node, opcode2);
    }

    /**
     * Insert several basic instruction nodes before the specified node
     * @param node new nodes will be inserted before this one
     * @param opcode1 opcode for the 1st node
     * @param opcode2 opcode of the 2nd node
     * @param opcode3 opcode of the 2nd node
     */
    protected void insertBasic(AbstractInsnNode node, int opcode1, int opcode2, int opcode3) {
        insertBasic(node, opcode1);
        insertBasic(node, opcode2);
        insertBasic(node, opcode3);
    }

    protected InsnNode replaceWithNop(AbstractInsnNode node) {
        return replaceWithBasic(node, 0);
    }

    protected AbstractInsnNode insertLoadThis(AbstractInsnNode node) {
        return insertBefore(node,  new VarInsnNode(ALOAD, 0));
    }

    protected AbstractInsnNode insertLoadVar(AbstractInsnNode node, int typeId, int aDst) {
        return insertBefore(node, new VarInsnNode(loadOpcodeForTypeId(typeId), aDst));
    }

    protected AbstractInsnNode insertStoreVar(AbstractInsnNode node, int typeId, int aDst) {
        return insertBefore(node, new VarInsnNode(storeOpcodeForTypeId(typeId), aDst));
    }

    protected AbstractInsnNode insertLoadNull(AbstractInsnNode node) {
        return insertBasic(node, ACONST_NULL);
    }


    protected AbstractInsnNode insertLoadVtNull(AbstractInsnNode node, ClassDef cl) {

        return insertBefore(node,  new LdcInsnNode(cl.getNullValue()));
    }


    protected AbstractInsnNode insertLoadVtNull(AbstractInsnNode node, int vtClassIndex) {

        return insertLoadVtNull(node, getClassFromIndex(vtClassIndex));
    }


    protected void insertPop(AbstractInsnNode node, int typeId) {

        insertBasic(node, isDst64(typeId) ? POP2 : POP);
    }


    /**
     * Insert code for popping second value on the stack, while leaving 1st one intact
     * @param node insert before this node
     * @param typeId0 Type at depth 0
     * @param typeId1 Type at depth 1
     * @return
     */
    protected AbstractInsnNode insertPop2nd(AbstractInsnNode node, int typeId0, int typeId1) {

        if (isDst32(typeId0)) {
            if (isDst32(typeId1)) {
                insertBasic(node, SWAP);
                insertBasic(node, POP);
            } else {
                insertBasic(node, DUP_X2);
                insertBasic(node, POP);
                insertBasic(node, POP2);
            }
        } else {
            assert(isDst64(typeId0));
            if (isDst32(typeId1)) {
                insertBasic(node, DUP2_X1);
                insertBasic(node, POP2);
                insertBasic(node, POP);
            } else {
                insertBasic(node, DUP2_X2);
                insertBasic(node, POP2);
                insertBasic(node, POP2);
            }
        }

        return node;
    }


    protected MethodInsnNode insertMethodStatic(AbstractInsnNode node, String owner, String name, String desc) {

        return insertBefore(node,  new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
    }

    /**
     * Insert code for filling 1-dimensional ValueType Array on the stack
     * @param node
     * @param typeId
     * @return
     */
    protected AbstractInsnNode insertArrayFill(AbstractInsnNode node, int typeId) {

        /* TODO: This should work well, but from perf. standpoint is better to force user to create array initialization method
           Later may use this as fallback if user did create said method
           OR auto-create the method in user's utility class */
        assert(vm.typeIdAt(0) == TypeId.arrayFrom(typeId, 1)); // typeIdAtDepth0 is only passed for performance reasons
        assert(isVtA1Ref(vm.typeIdAt(0)));

        insertBasic(node, DUP);
        insertLoadVtNull(node, getClassFromTypeId(typeId));
        return insertMethodStatic(node, "java/util/Arrays", "fill", "([JJ)V");
    }

    /**
     * Replace signature of MethodInsnNode with new owner/name/desc
     * @param methodNode method node being updated
     * @param methodDef definition of the new method to use
     */
    protected void replaceMethodSignature(MethodInsnNode methodNode, MethodDef methodDef) {

        methodNode.owner = methodDef.getNewOwner();
        methodNode.name  = methodDef.getNewName();
        methodNode.desc  = methodDef.getNewDesc();

        int op = methodNode.getOpcode();
        // Just for safety
        switch (op) {
            case INVOKEVIRTUAL:
                // ValueType implementation methods are always static
                methodNode.setOpcode(INVOKESTATIC);
                // fall through
            case INVOKESTATIC:
                break;
            default:
                log.err("replaceMethodSignature(): Trying to replace unexpected opcode %d at line: %d", op, currentLine());
        }
    }

    /**
     * Insert static method call
     * @param node insert before this node
     * @param method method to insert
     * @return
     */
    protected MethodInsnNode insertImplMethod(AbstractInsnNode node, MethodDef method) {

        return insertMethodStatic(node, method.getNewOwner(), method.getNewName(), method.getNewDesc());
    }

    /**
     * Insert static method call with additional null check
     * @param node insert before this node
     * @param method method to insert
     * @param debugName arbitrary text name of the method to use for error output
     * @return
     */
    protected void insertImplMethod(AbstractInsnNode node, MethodDef method, String debugName) {
        if (null == method) {
            log.err("%s method is not defined in the configuration for VT: %s", debugName, method.getOwner().getSrcClassPath());
        }

        insertImplMethod(node, method);
    }

    /**
     * Insert boxing method for a given VT class and register a warning with the specified cause code, if the cause is != -1
     * @param node insert before this node
     * @param classDef insert boxing for this Value Type class
     * @param cause warning cause code, will be registered if != -1
     */
    protected void insertBoxingFrom(AbstractInsnNode node, ClassDef classDef, int cause) {

        log.dbg("Boxing..");
        if (-1 != cause) {
            addWarning(cause);
        }

        insertImplMethod(node, classDef.boxingMethod, "VT boxing");
    }

    /**
     * Insert UNboxing method for a given VT class and register a warning with the specified cause code, if the cause is != -1
     * @param node insert before this node
     * @param classDef insert UNboxing for this Value Type class
     * @param cause warning cause code, will be registered if != -1
     */
    protected void insertUnboxingTo(AbstractInsnNode node, ClassDef classDef, int cause) {

        // TODO: We don't check cause for unboxing here, for now
        log.dbg("Unboxing..");
        // Using static method that also does null check
        insertImplMethod(node, classDef.unboxingMethod, "VT unboxing");
    }


    protected void insertArrayBoxingFrom(AbstractInsnNode node, ClassDef classDef, int cause) {

        log.wrn("Array Boxing..");
        if (-1 != cause) {
            addWarning(cause);
        }

        insertImplMethod(node, classDef.arrayBoxingMethod, "array boxing");
    }


    protected void insertArrayUnboxingTo(AbstractInsnNode node, ClassDef classDef, int cause) {

        log.wrn("Array Unboxing..");
        if (-1 != cause) {
            addWarning(cause);
        }

        insertImplMethod(node, classDef.arrayUnboxingMethod, "array unboxing");
    }

    /**
     * Insert the method that checks if a given scalar ValueType is null and returns a boolean
     * @param node
     * @param classDef
     */
    protected void insertVtValueNullCheck(AbstractInsnNode node, ClassDef classDef) {

        insertImplMethod(node, classDef.isNullMethod);
    }


    protected void insertTypecastTo(AbstractInsnNode node, String classPath) {

        insertBefore(node, new TypeInsnNode(CHECKCAST, classPath));
    }


    protected void insertTypecastTo(AbstractInsnNode node, ClassDef classDef) {

        insertTypecastTo(node, classDef.getSrcClassPath());
    }


    protected void insertBoxingFrom(AbstractInsnNode node, int srcTypeId, int cause) {

        if (-1 != cause) {
            addWarningWithType(cause, srcTypeId);
        }

        int arrayDepth = TypeId.getArrayDepth(srcTypeId);

        if (0 == arrayDepth) {
            insertBoxingFrom(node, getClassFromTypeId(srcTypeId), -1);
            return;
        }

        if (isNull(srcTypeId)) {
            // Null value can be "boxed" to any array, though this may be impossible due to impossiblity of having null VT[]
            return;
        }

        if (1 == arrayDepth) {
            insertArrayBoxingFrom(node, getClassFromTypeId(srcTypeId), -1);
            return;
        }

        log.err("Can't box VT array with %d dimensions", arrayDepth);
    }


    protected void insertUnboxing(AbstractInsnNode node, int srcTypeId, int dstTypeId, boolean withTypecast, int cause) {

        if (!isVt(dstTypeId)) {
            log.wrn("Can't unbox to non-VT: %x", dstTypeId);
        }

        ClassDef cl = getClassFromTypeId(dstTypeId);
        if (withTypecast) {
            insertTypecastTo(node, cl);
        }

        int arrayDepth = TypeId.getArrayDepth(dstTypeId);

        if (0 == arrayDepth) {
            insertUnboxingTo(node, cl, cause);
            return;
        }

        if (isNull(srcTypeId)) {
            // Null value can be "unboxed" to any array
            return;
        }

        if (1 == arrayDepth) {
            insertArrayUnboxingTo(node, cl, cause);
            return;
        }

        log.err("Can't unbox to VT array with %d dimensions", arrayDepth);
    }

    protected void insertUnboxingWithTypecast(AbstractInsnNode node, int srcTypeId, int dstTypeId, int cause) {
        insertUnboxing(node, srcTypeId, dstTypeId, TypeId.isVt(dstTypeId), cause);
    }

    protected void insertUnboxing(AbstractInsnNode node, int srcTypeId, int dstTypeId, int cause) {
        insertUnboxing(node, srcTypeId, dstTypeId, false, cause);
    }


    protected void insertVtValueNullCheck(AbstractInsnNode node, int typeId) {
        insertVtValueNullCheck(node, getClassFromTypeId(typeId));
    }

    protected void toIfVtNull(JumpInsnNode node, int typeId, boolean isNull) {
        if (isVtValue(typeId)) {
            // If isNull == true, -> IFNE(if != 0), If isNull == false, -> IFEQ(if == 0)
            insertVtValueNullCheck(node, typeId);
            node.setOpcode(isNull ? IFNE : IFEQ);
            // We expect isNull method presence. TODO: Check before loading VT class implementation
            // TODO: Replace isNull with hardcoded constant comparison
            //        insertBasic(node, LCMP);
        } else {
            log.err("toIfNonNull: typeId must be VType: %x", typeId);
        }
    }

    protected void toIfNull(JumpInsnNode node, int typeId, boolean isNull) {

        if (isRefDst(typeId)) {
            node.setOpcode(isNull ? IFNULL : IFNONNULL);
        } else {
            toIfVtNull(node, typeId, isNull);
        }
    }

    /**
     * Load VType null value (currently by using unboxing method)
     */
    protected void insertVTypeInitialization(AbstractInsnNode node, int typeId) {

        insertLoadVtNull(node, TypeId.getVtClassIndex(typeId));
    }


    protected void popReferenceWithOptionalBoxing(AbstractInsnNode node, int cause) {

        int stackTypeId = vm.typeIdAt(0);
        if (isVtValue(stackTypeId)) {
            insertBoxingFrom(node, stackTypeId, cause);
            vm.popVType64();
        } else {
            vm.pop32();
        }
    }


    protected void popReferenceWithOptionalUnboxingTo(AbstractInsnNode node, int dstTypeId, int cause) {

        int typeId = vm.typeIdAt(0);
        if (isVtValue(typeId)) {
            vm.popVType64();
        } else {
            insertUnboxing(node, typeId, dstTypeId, cause);
            vm.pop32();
        }
    }


    protected void boxOrUnboxIfNeeded(int checkResult, int srcType, int dstType, AbstractInsnNode node, int cause) {

        if (TypeIdCast.isFailure(checkResult)) {
            log.err("Unable to transform types: %x -> %x in src line: %s", srcType, dstType, currentLine());
        }
        // TODO: maybe add "box to null constant" shortcut

        switch (checkResult & ~HAS_VTYPE) {
            case TypeIdCast.NEED_BOXING:
                // Need to transform Value Type to its Reference class representation
                insertBoxingFrom(node, srcType, cause);
                break;

            case TypeIdCast.NEED_UNBOXING:
                // Need to create Value Type from its Reference class representation
                insertUnboxing(node, srcType, dstType, cause);
                break;

            case TypeIdCast.SUCCESS:
                // Do nothing, we shouldn't reach this label anyway
                break;

            default:
                log.err("Logic Error: Failed to transform types %x -> %x in src line: %s", srcType, dstType, currentLine());
        }
    }

    /**
     * Almost same as boxOrUnboxIfNeeded, but supports uninitialized values and has separate logging
     * @param checkResult
     * @param srcType
     * @param dstType
     * @param node
     */
    protected void convertVariableIfNeeded(AbstractInsnNode node, int checkResult, int srcType, int dstType, String name, int cause) {

        if (TypeIdCast.isFailure(checkResult)) {
            log.err("Unable to transform var types: %d -> %d in src line: %s", srcType, dstType, currentLine());
        }

        int dbgType = 0;
        switch (checkResult & ~HAS_VTYPE) {
            case TypeIdCast.NEED_BOXING:
                // Need to transform Value Type to its Reference class representation
                onVTypeVarAutoBoxing();
                cause = Warnings.FRAME_TRANSFORM == cause ? Warnings.FRAME_BOXING : cause;
                insertBoxingFrom(node, srcType, -1);
                dbgType = srcType;
                break;

            case TypeIdCast.NEED_UNBOXING:
                // Need to create Value Type from its Reference class representation
                onVTypeVarAutoUnboxing();
                cause = Warnings.FRAME_TRANSFORM == cause ? Warnings.FRAME_UNBOXING : cause;
                insertUnboxing(node, srcType, dstType, -1);
                dbgType = dstType;
                break;

            case TypeIdCast.UNINITIALIZED_VTYPE:
                // Need to put Value Type in place of previously uninitialized space that could only hold reference type
                onVTypeVarValueAutoInitialization();
                insertVTypeInitialization(node, dstType);
                dbgType = dstType;
                break;

            case TypeIdCast.DISCARD_VTYPE:
                // Need to discard Value Type that was taking a place of previously uninitialized space that could only hold reference type
                onVTypeVarValueAutoDiscard();
                insertBasic(node, POP2);
                // Not removing the value from stack, it will be discarded by store instruction TODO: maybe discard here
                //vm.popVType64();
                dbgType = srcType;
                break;

            case TypeIdCast.SUCCESS:
                // Do nothing, we shouldn't reach this label anyway
                break;

            default:
                log.err("Logic Error: Failed to transform var types %d -> %d in src line: %s", srcType, dstType, currentLine());
        }

        if (-1 != cause) {
            cause = Warnings.FRAME_TRANSFORM == cause ? Warnings.UNINIT_TRANSFORM : cause;
            addWarning(cause, null != name ? (toPrintableString(dbgType) + " " + name) : toPrintableString(dbgType));
        }
    }


    void boxOrUnboxIfNeeded(AbstractInsnNode node, int srcType, int dstType, int cause) {
        if (srcType != dstType) {
            boxOrUnboxIfNeeded(TypeIdCast.check(srcType, dstType), srcType, dstType, node, cause);
        }
    }

    void convertVariableIfNeeded(AbstractInsnNode node, int srcType, int dstType, String name, int cause) {
        convertVariableIfNeeded(node, TypeIdCast.checkCastFrame(srcType, dstType), srcType, dstType, name, cause);
    }


    protected void applyReturnValue(int typeId) {

        if(TypeId.VOID != typeId) {
            vm.pushTypeId(typeId);
        }
    }


    protected static int argRefWarningCause(int argIndex) {
        assert(argIndex > 0); // arg indices start from 1, but displayed starting from 0
        return Warnings.REF_ARGS | ((argIndex) << 16);
    }

    /**
     * Remove some args from stack, box/unbox and put back
     * @param node method call node whose args are going to be transformed between boxed/unboxed state
     * @param methodArgs array containing TypeIds correspnding to method signature
     * @param nMethodArgs number of args
     * @param depthCorrected starting stack depth for argument transformation
     */
    protected void transformMethodArgs(AbstractInsnNode node, int[] methodArgs, int nMethodArgs, int depthCorrected) {
        int depthTotal = nMethodArgs - 1;
        assert (depthCorrected >= 0);

        // First, unload "depth" vars into temporary variables
        // TODO: OPT: Inefficient, all vars take 2 slots
        int varTop = varTop();
        for (int i = 0; i < depthCorrected; ++i) {
            insertBefore(node, new VarInsnNode(storeOpcodeForTypeId(vm.typeIdAt(i)), varTop + i * 2));
        }

        int stackType = vm.typeIdAt(depthCorrected);
        int argType = methodArgs[nMethodArgs - depthCorrected];
        boxOrUnboxIfNeeded(node, stackType, argType, argRefWarningCause(1));

        for (int i = depthCorrected - 1; i >= 0; --i) {
            stackType = vm.typeIdAt(i);
            argType = methodArgs[nMethodArgs - i];
            insertBefore(node, new VarInsnNode(loadOpcodeForTypeId(stackType), varTop + i * 2));
            boxOrUnboxIfNeeded(node, stackType, argType, argRefWarningCause(nMethodArgs - i));
        }
    }


    /**
     * Update VM stack according to method's signature, transform stack args, if required by checkResult descriptor
     * @param node
     * @param methodArgs
     * @param nMethodArgs
     * @param checkResult
     */
    protected void applyMethodArgs(AbstractInsnNode node, int[] methodArgs, int nMethodArgs, int checkResult) {

        if (nMethodArgs > 0) {
            assert (!TypeIdCast.isFailure(checkResult));
            if (TypeIdCast.SUCCESS != (checkResult & ~HAS_VTYPE)) {
                // TODO: Suspicious assert. Still needed/valid?
                assert (0 != ((TypeIdCast.NEED_BOXING | TypeIdCast.NEED_UNBOXING) | checkResult));
                transformMethodArgs(node, methodArgs, nMethodArgs, (checkResult >> 8) & 0xFF);
            }

            vm.popMany(nMethodArgs);
        }

        applyReturnValue(methodArgs[0]);
    }


    protected int transformMethodCall(MethodInsnNode node, MethodDef methodDef) {
        log.trace(methodDef.getDebugName());

        int[] methodArgs = methodDef.args;
        int nMethodArgs = methodDef.numArgs;
        int checkResult = verifyMethodArgs(methodArgs, nMethodArgs);

        // If unable to substitute VType for at least one arg
        if (0 != (NO_SUBSTITUTION & checkResult))
            return TypeIdCast.NO_SUBSTITUTION;

        if (0 != (checkResult & NEED_SUBSTITUTION)) {
            assert(0 != (HAS_VTYPE & checkResult));

            methodArgs = Arrays.copyOf(methodArgs, nMethodArgs + 1);
            int result = substituteWildcardArgs(methodArgs, nMethodArgs);
            if (TypeIdCast.isFailure(result)) {
                log.err("Wildcard ValueType substitution failed at position %d (more than 1 vtype used?)", checkResult >> 8);
            }

            // Retry after substitution
            checkResult = verifyMethodArgs(methodArgs, nMethodArgs);
            if (0 != (checkResult & TypeIdCast.NO_SUBSTITUTION)) {
                log.err("Value Type substitution incomplete: %s.%s%s", node.owner, node.name, node.desc);
            }

            // Should not have substitution-related flags anymore
            if (0 != (checkResult & (NO_SUBSTITUTION | NEED_SUBSTITUTION))) {
                return TypeIdCast.NO_SUBSTITUTION;
            }
        }

        if (TypeIdCast.isFailure(checkResult)) {
            log.err("Value Type implementation method argument verification failure for: %s.%s argument#: %d", node.owner, node.name, nMethodArgs - ((checkResult >> 8) & 0xFF));
            log.err("at line: %d", currentLine());
            return checkResult;
        }

        applyMethodArgs(node, methodArgs, nMethodArgs, checkResult);
        replaceMethodSignature(node, methodDef);
        return checkResult;
    }


    boolean tryTransformAsVTypeMethodCall(MethodInsnNode node, String name, String owner, String desc) {
        // Is this method a registered ValueType method?
        HashMap<Integer, MethodDef> vtypeMethodMap = mapping.getMethodMap(owner);
        if (null != vtypeMethodMap) {
            // NOTE: Names of ValueType class methods or VT utility methods are not themselves transformed
            MethodDef m = Mapping.getMethod(vtypeMethodMap, name, desc);
            if (null == m) {
                if (mapping.isMappedSrcClass(owner)) {
                    log.err("VType method not found: %d %s %s%s", node.getOpcode(), owner, name, desc);
                    return true;
                } else {
                    // Class is an automethod owner, but this method is not mapped
                    return false;
                }
            } else {
                if (m.isAutoMethod()) {
                    log.wrn("AutoMethod: %s.%s%s -> %s", owner, name, desc, m.getNewDesc());
                    if (TypeIdCast.NO_SUBSTITUTION != transformMethodCall(node, m))
                        return true;

                    // Fall through to processing this method as normal
                    return false;
                } else {
                    transformMethodCall(node, m);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Pre-transform Object and Array methods that Value Types are expected to support
     * Current implementation call this _before_ parsing the method signature
     * method will be further transformed as usual
     * this is not optimal from performance standpoint but more straightforward to implement
     */

    protected boolean tryPreTransformObjectMethods(MethodInsnNode node) {

        // TODO: Check for presence of equals method and produce a warning if absent
        if (node.getOpcode() != INVOKEVIRTUAL)
            return false;

        if (!node.owner.equals("java/lang/Object"))
            return false;

        // TODO: Automate this type of autoreplacement also, low priority
        vm.checkSrcUnderflow32(node, 1);
        int stackType0 = vm.typeIdAt(0);
        if (!isVt(stackType0))
            return false;

        String vtClassPath = mapping.getClassDefById(stackType0).getSrcClassPath();
        if (isVtValue(stackType0)) {
            String name = node.name, desc = node.desc;
            if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
                vm.checkSrcUnderflow32(node, 2);
                node.owner = vtClassPath;
                // choose between ValueType.equals(ValueType) and ValueType.equals(Object)
                if (vm.typeIdAt(1) == stackType0) {
                    node.desc = "(L" + vtClassPath + ";)Z";
                }

                return true;
            } else if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
                node.owner = vtClassPath;
                return true;
            }

            log.wrn("Unsupported VT Object method: %s%s in line %d", node.name, node.desc, currentLine());
        }

        return false;
    }


    abstract int varTop();

    /**
     * Transform owner of field access instruction, if necessary
     * Converts ValueType field access to ValueTypeUtils field access
     * @param node
     */
    void checkForOwnerTransformation(FieldInsnNode node) {

        String owner = node.owner;
        if (mapping.isMappedSrcClass(owner)) {
            ClassDef def = mapping.getClassDef(owner);
            // TODO: Recheck this code
//            if ((ACC_STATIC | ACC_FINAL) == (access & (ACC_STATIC | ACC_FINAL))) {
//            }
            node.owner = def.getDstClassPath();
            log.dbg("Changed field .%s owner: %s -> %s", node.name, owner, node.owner);
        }
    }


    void tryAddFieldInitInjectionPoint(AbstractInsnNode node) {
        if (needAutoInitInjectionPoint) {
            // Suitable location for init code is before the first branch and before the first PUTXX instruction
            if (null == initializationPatchNode) {
                initializationPatchNode = node.getPrevious();
                assert(null != initializationPatchNode);
                if (node instanceof FieldInsnNode) {
                    log.dbg("VT Init location chosen. Before field access node %s %s", node, ((FieldInsnNode)node).name);
                } else {
                    // So, we arrived at a branch, without executing a single PUTFIELD/PUTSTATIC,
                    // so we don't yet have a suitable location for field init code
                    // cant just remember the branch node itself because we may yet insert more code before it
                    log.dbg("VT Init location chosen. Before flow control node %s", node);
                }
            }

            needAutoInitInjectionPoint = false;
        }
    }


    public void transformGetField(FieldInsnNode node) {

        String desc = node.desc;
        int typeId = getDescTypeId(desc);

        if (isVt(typeId)) {
            tryAddFieldInitInjectionPoint(node);
            // Fields of VT classes are not renamed on GET (and not supposed to be PUT)
            if (!mapping.isMappedClass(node.owner)) {
                // Field names are not modified anymore
                //node.name = nameConverter.transform(node.name, desc);
            }

            node.desc = mapping.typeIdToDstTypeDesc(typeId);

            if (!isVtValue(typeId) && !isVtArray(typeId)) {
                log.err("GetField: Unexpected VType field: %x|%s at line: %d", typeId, desc, currentLine());
            }

            checkForOwnerTransformation(node); // ValueType fields will get new owner
        }

        vm.pushTypeId(typeId);
    }


    public void transformPutField(FieldInsnNode node) {

        String desc = node.desc;
        int dstType = getDescTypeId(desc);
        int stackType = vm.typeIdAt(0);

        tryAddFieldInitInjectionPoint(node);
        // TODO: Rewrite
        if (isVt(dstType)) {
            // Field names are not modified anymore
            //node.name = nameConverter.transform(node.name, desc);
            node.desc = mapping.typeIdToDstTypeDesc(dstType);
            if (isVtValue(dstType)) {
                if (dstType == stackType) {
                    vm.popVType64();
                } else if (isDst32(stackType)) {
                    // TODO: Stricter type comparison
                    insertUnboxing(node, stackType, dstType, Warnings.REF_ASSIGN);
                    vm.pop32();
                } else {
                    log.err("Putfield: Incompatible value on the stack at line: %d", currentLine());
                }
            } else if (isVtArray(dstType) && isDst32(stackType)) {
                // TODO: Stricter type comparison (test if different value types/arrays not assignable to each other)
                vm.pop32();
            } else {
                log.err("Putfield: Unexpected destination field type: %x|%s %s at line: %d", dstType, desc, node.name, currentLine());
            }

            checkForOwnerTransformation(node); // ValueType fields will get new owner
        } else if (isVtValue(stackType)) {
            insertBoxingFrom(node, stackType, Warnings.REF_ASSIGN);
            vm.popVType64();
        } else {
            super.onPutStatic(node);
        }
    }

    @Override
    public void onStart(Object context) {
        init((MethodNode)context);
    }

    @Override
    public void onGetStatic(FieldInsnNode node) {
        transformGetField(node);
    }

    @Override
    public void onGetField(FieldInsnNode node) {

        vm.pop32();
        this.onGetStatic(node);
    }

    @Override
    public void onPutField(FieldInsnNode node) {
        transformPutField(node);
        vm.pop32(); // Pop object ptr
    }

    @Override
    public void onPutStatic(FieldInsnNode node) {
        transformPutField(node);
    }

    @Override
    public void onReturn(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-return.html
        tryAddFieldInitInjectionPoint(node);
        super.onReturn(node);
    }

    @Override
    public void onReturn32(InsnNode node) {
        tryAddFieldInitInjectionPoint(node);
        super.onReturn32(node);

    }

    @Override
    public void onReturn64(InsnNode node) {
        tryAddFieldInitInjectionPoint(node);
        super.onReturn64(node);
    }


    @Override
    public void onAReturn(InsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        int typeId = vm.typeIdAt(0);
        vm.checkReturnStack(node, 1, 1, isVtValue(typeId) ? 2 : 1);
        if (isVt(returnTypeId)) {
            boxOrUnboxIfNeeded(node, typeId, returnTypeId, Warnings.REF_RETURN);
            if (isVtValue(returnTypeId)) {
               replaceWithBasic(node, LRETURN);
            }
        } else {
            if (TypeId.isVt(typeId)) {
                boxOrUnboxIfNeeded(node, typeId, returnTypeId, Warnings.REF_RETURN);
            }
        }

        vm.invalidateStack();
    }


    // TypeInsnNode
    // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/TypeInsnNode.html
    // NEW, ANEWARRAY, CHECKCAST or INSTANCEOF.

    @Override
    public void onNew(TypeInsnNode node) {

        // Just in case... We disabled constructors though
        String desc = node.desc;
        if (isVt(getClassTypeId(desc))) {
            log.err("Constructor calls are not allowed for value type: %s at line: %d", desc, currentLine());
        } else {
            vm.push32ref(desc);
        }
    }

    @Override
    public void onANewArray(TypeInsnNode node) {

        String desc = node.desc;
        vm.checkSrcUnderflow32(node, 1);
        vm.pop32();
        int typeId = getClassTypeId(desc);
        if (isVt(typeId)) {
            if (isVtValue(typeId)) {
                insertBefore(node, new IntInsnNode(NEWARRAY, T_LONG));
                log.dbg("new X[]");
                int newTypeId = TypeId.arrayFrom(typeId, 1);
                vm.pushTypeId(newTypeId);
                insertArrayFill(node, typeId);
                remove(node);
                onVTypeVarNewArray(newTypeId);
            } else if (isVtArray(typeId)) {
                node.desc = mapping.typeIdToDstTypeDesc(typeId);
                vm.pushTypeId(TypeId.arrayIncrementDepth(typeId));
                // No warning if creating an array of VType arrays. It is just a normal Java array of Objects with normal nulls
            } else {
                log.err("ANEWARRAY: Strange Array of Value Type: %s (0x%x) at line: %d", desc, typeId, currentLine());
            }
        } else {
            // Fall through
            vm.pushTypeId(TypeId.arrayIncrementDepth(typeId), desc);
            //vm.pushTypeId(typeId, "A[" + desc + "]");
        }
    }

    @Override
    public void onMultiANewArray(MultiANewArrayInsnNode node) {

        String desc = node.desc;
        vm.checkSrcUnderflow32(node, node.dims);
        for (int i = 0; i < node.dims; ++i) {
            vm.pop32();
        }

        int typeId = getDescTypeId(desc);
        if (isVt(typeId)) {
            if (isVtArray(typeId)) {
                insertBefore(node, new MultiANewArrayInsnNode(mapping.typeIdToDstTypeDesc(typeId), node.dims));
                remove(node);
                log.dbg("new X..[]");
                //vm.pushVTypeArrayRef(typeId);
                vm.pushTypeId(typeId);
                onVTypeVarNewMultiArray(typeId);
            } else {
                log.err("MULTIANEWARRAY: Unexpected type: %s (0x%x) at line: %d", desc, typeId, currentLine());
            }
        } else {
            // Non - VType Object
            vm.pushTypeId(typeId,  desc);
        }
    }

    @Override
    public void onCheckCast(TypeInsnNode node) {

        String desc = node.desc;
        vm.checkSrcUnderflow32(node, 1);
        int stackTypeId = vm.typeIdAt(0);
        int newTypeId = getClassTypeId(desc);

        int castResult = TypeIdCast.check(stackTypeId, newTypeId);

        if (TypeIdCast.isFailure(castResult)) {
            log.err("CHECKCAST: Failed to cast type %x|%s to %s", stackTypeId, format(stackTypeId, vm.nameAt(0), -1), desc, newTypeId);
            return;
        }

        assert(0 == (NEED_SUBSTITUTION & castResult));

        if (isVtValue(stackTypeId)) {
            assert(0 == (NEED_BOXING & castResult)); // Impossible to require boxing in checkcast
            if ((SUCCESS | HAS_VTYPE) == castResult) {
                replaceWithBasic(node, NOP);
                return;
            }

            vm.popVType64();
            // We check cast _before_ unboxing
            boxOrUnboxIfNeeded(castResult, stackTypeId, newTypeId, node.getNext(), Warnings.REF_CAST);
            vm.pushTypeId(newTypeId, desc);
            if (desc.equals("java/lang/Object")) {
                remove(node);
            }
        } else {
            vm.pop32();

            if (0 != (HAS_VTYPE & castResult)) {
                // Transform expected destination type
                if (isVtArray(newTypeId)) {
                    if (0 == (NEED_UNBOXING & castResult) || isNull(stackTypeId)) {
                        node.desc = mapping.typeIdToDstTypeDesc(newTypeId);
                    }

                    vm.pushTypeId(newTypeId);
                } else {
                    // Since no value type on the stack, we push ref VT
                    vm.pushTypeId(TypeId.vtRefFrom(newTypeId));
                }
            } else {
                vm.pushTypeId(newTypeId, desc);
            }

            if (SUCCESS == (castResult & ~HAS_VTYPE) || (isVtNonArray(newTypeId) && NEED_UNBOXING == (castResult & ~HAS_VTYPE))) {
                // No further conversion required. Scalar Value Type is not unboxed
                return;
            }

            // Null check is correct?
            if (!isNull(stackTypeId)) {
                // Boxing is done _before_ typecast, unboxing _after_ the typecast
                boxOrUnboxIfNeeded(castResult, stackTypeId, newTypeId, NEED_BOXING == castResult ? node : node.getNext(), Warnings.REF_CAST);
            }

            // TODO: rearrange this code for greater comprehension. Current implementation is a mess.
        }
    }


    @Override
    public void onInstanceOf(TypeInsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref--31.html
        String desc = node.desc;
        vm.checkSrcUnderflow32(node, 1);
        int stackTypeId = vm.typeIdAt(0);
        int otherTypeId = getClassTypeId(desc);

        if (isVt(stackTypeId)) {
            if (isVtValue(stackTypeId)) {
                log.dbg("VT instanceof X?");

                if (otherTypeId == stackTypeId || desc.equals("java/lang/Object")) {
                    // Null check if same VT class or Object
                    insertVtValueNullCheck(node, stackTypeId);
                    // Negate boolean
                    insertBasic(node, ICONST_1);
                    insertBasic(node, IXOR);
                    insertBasic(node, ICONST_1);
                    insertBasic(node, IAND);
                    remove(node);
                } else {
                    // false otherwise
                    insertBasic(node, POP2);
                    replaceWithBasic(node, ICONST_0);
                }

                vm.popVType64();
            } else {
                // If comparing known VType with known VT Array, transform signature of the VT Array
                if (isVtArray(otherTypeId)) {
                    node.desc = mapping.typeIdToDstTypeDesc(otherTypeId);
                }

                vm.pop32();
            }

            vm.push32i("boolIsInstance");
            return;
        }

        // Object on the stack is not a known VType. In such case we also don't transform class type signature,
        // even if it belongs to a VType. Otherwise this will return true: (new long[123] instanceof ValueType[])
        vm.pop32();
        vm.push32i("boolIsInstance");
    }

    abstract int adjustIincInsnIndex(IincInsnNode node);

    @Override
    public void onIInc(IincInsnNode node) {
        adjustIincInsnIndex(node);
    }

    abstract void transformFramesBeforeBranch(AbstractInsnNode node, LabelNode label, int stackOffset, boolean isUnconditional);



    @Override
    public void onGoto(JumpInsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.label, 0, true);
        super.onGoto(node);
    }

    @Override
    public void onIfICmpXX(JumpInsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.label, 2, false);
        super.onIfICmpXX(node);
    }

    @Override
    public void onIfXX(JumpInsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.label, 1, false);
        super.onIfXX(node);
    }

    @Override
    public void onLookupSwitch(LookupSwitchInsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.dflt, 1, false);
        vm.pop32();
    }

    @Override
    public void onTableSwitch(TableSwitchInsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.dflt, 1, false);
        vm.pop32();
    }

    @Override
    public void onAThrow(InsnNode node) {

        tryAddFieldInitInjectionPoint(node);
        super.onAThrow(node);
    }

    /**
     * transform IFNULL/IFNONNULL opcode
     * @param node Instruction node being transformed
     * @param isNull true if jumping at null, false if jumping at not null
     */
    @Override
    public void onIfNull(JumpInsnNode node, boolean isNull) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.label, 1, false);
        vm.checkSrcUnderflow32(node, 1);
        int typeId = vm.typeIdAt(0);
        if (TypeId.isVtValue(typeId)) {
            toIfVtNull(node, typeId, isNull);
            log.trace(isNull ? "IFNULL" : "IFNONNULL");
            vm.popVType64();
        } else {
            vm.pop32();
        }
    }


    protected void unboxForComparisonWith(AbstractInsnNode node, int thisId, int otherId) {

        if (isVtRef(thisId)) {
            // Warning already present in the calling method
            insertUnboxing(node, thisId, thisId, -1);
        } else {
            assert(isRefSrc(thisId));
            if (isVtNonArray(otherId)) {
                insertUnboxingWithTypecast(node, thisId, otherId, -1);
            }
        }
    }

    /**
     * Transform IFACMPXX opcode
     * @param node Instruction node being transformed
     * @param isEquality true = compared for equality, false = compared for inequality
     */
    @Override
    public void onIfACmpEq(JumpInsnNode node, boolean isEquality) {

        tryAddFieldInitInjectionPoint(node);
        transformFramesBeforeBranch(node, node.label,2, false);
        vm.checkSrcUnderflow32(node, 2);

        // NOTE: Equality check, unlike null check, is turned into value comparison

        int typeId0 = vm.typeIdAt(0);
        int typeId1 = vm.typeIdAt(1);
        // If neither is detected as VType, don't care
        boolean isVtScalar0 = isVtNonArray(typeId0);
        boolean isVtScalar1 = isVtNonArray(typeId1);

        if (!(isVtScalar0 || isVtScalar1)) {
            super.onIfACmpEq(node, isEquality);
            return;
        }

        int nVtScalar = isVtScalar0 ? 1 : 0;
        nVtScalar += isVtScalar1 ? 1 : 0;

        assert(isVtNonArray(typeId0) || isVtNonArray(typeId1));
        assert(nVtScalar > 0);
        assert(!(2 == nVtScalar) || (getVtClassIndex(typeId0) == getVtClassIndex(typeId1)));

        if (isVt(typeId0) && isVt(typeId1)) {
            // Probably impossible, but..
            if (!isSameVtClass(typeId0, typeId1) /*|| getArrayDepth(typeId0) != getArrayDepth(typeId1)*/) {
                addWarning(Warnings.DIFFERENT_TYPES_CMP, toPrintableString(typeId1) + " != " + toPrintableString(typeId1));
                popAny(node, 2); // This also cleans stack

                // Obviously different types, must return false
                /// TODO: test this
                if (isEquality) {
                    remove(node);
                } else {
                    node.setOpcode(GOTO);
                }

                return;
            }
        } else if(isNull(typeId0) || isNull(typeId1)) {
            log.trace(isEquality ? "->IFNULL" : "->IFNONNULL");

            if (isNull(typeId0)) {
                insertPop(node, typeId0);
                toIfNull(node, typeId1, isEquality);
            } else {
                insertPop2nd(node, typeId0, typeId1);
                toIfNull(node, typeId0, isEquality);
            }

            vm.popMany(2);
            return;
        }
        else {
            // Issue a warning
            onVTypeRefCmp(typeId0, typeId1, !isEquality, false);
        }

        // Change the type of the conditional jump node to use the result of equals() call
        node.setOpcode(isEquality ? IFNE : IFEQ);
        ClassDef classDef = getClassFromTypeId(isVtScalar0 ? typeId0 : typeId1);
        vm.popMany(2);

        // Possible variants at this point:
        // ref     VtRef
        // ref     VtValue
        // VtRef   ref
        // VtRef   VtRef
        // VtRef   VtValue
        // VtValue ref
        // VtValue VtRef
        // VtValue VtValue

        int nVtValue = isVtValue(typeId0) ? 1 : 0;
        nVtValue += isVtValue(typeId1) ? 1 : 0;
        assert(nVtScalar >= nVtValue);
        assert(typeId0 == typeId1 || !(2 == nVtValue));

        if (0 == nVtValue) {
            // Following cases are handled here:
            // ref   VtRef
            // VtRef ref
            // VtRef VtRef
            // Both are reference, use Object.equals() call, but ensure left side of the comparison is VType!!!
            if (!isVtScalar1) {
                insertBasic(node, SWAP);
            }

            // TODO: Should not be hardcoded
            insertMethodStatic(node, classDef.getSrcClassPath(), "isIdentical",
                    "(" + classDef.getSrcClassDesc() + "Ljava/lang/Object;)Z");
            return;
        }

        assert(nVtValue > 0);
        // Possible variants at this point:
        // ref     VtValue -> Swap
        // VtRef   VtValue -> Swap, Unbox
        // VtValue ref
        // VtValue VtRef   -> Unbox
        // VtValue VtValue

        if (isRefDst(typeId1)) {
            assert(isVtValue(typeId0));
            insertBasic(node, DUP2_X1);
            insertBasic(node, POP2);
            int tmp = typeId0;
            typeId0 = typeId1;
            typeId1 = tmp;
        }

        // Possible variants at this point:
        // VtValue ref
        // VtValue VtRef   -> Unbox
        // VtValue VtValue

        // unbox stack[0] if needed, pop from simulated stack
        if (isVtRef(typeId0)) {
            log.dbg("Unbox 1 side of the comparison");
            insertUnboxing(node, typeId0, typeId0, -1);
            typeId0 = vtValueFrom(typeId0); // Just in case
        }

        // Possible variants at this point:
        // VtValue ref
        // VtValue VtValue
        assert(isVtValue(typeId1));
        assert(isVtValue(typeId0) || !isVtNonArray(typeId0));
        insertMethodStatic(node, classDef.getDstClassPath(), "isIdentical",
                isVtValue(typeId0) ? "(JJ)Z" : "(JLjava/lang/Object;)Z");

    }

    protected void popAny(AbstractInsnNode node, int n) {

        for (; n != 0; --n) {
            int typeId0 = vm.typeIdAt(0);
            if (n > 1 && !isDst64(typeId0)&& !isDst64(vm.typeIdAt(1))) {
                insertBasic(node, POP2);
                --n;
                vm.popMany(2);
            } else {
                insertPop(node, typeId0);
                vm.popMany(1);
            }
        }
    }

    protected void popAny(AbstractInsnNode node) {
        popAny(node, 1);
    }


    protected void resetStackFrame() {
        vm.resetStack();
    }


    protected void saveStackFrame() {
        vm.saveStack(savedStack);
    }


    protected void resetAndSaveStackFrame() {

        resetStackFrame();
        saveStackFrame();
    }

    @Override
    public void onFrame(FrameNode node) {

        if (logDbg) {
            log.dbg("Stack Frame: %s", stackFrameToString(node));
        }

        switch (node.type) {
            case F_SAME:
                // We don't process loval var frame differences here
                resetAndSaveStackFrame();
                break;

            case F_APPEND:
            case F_CHOP:
                resetAndSaveStackFrame();
                break;

            case F_SAME1:

                //System.out.print(node.stack.get(0).toString());
                resetStackFrame();
                vm.pushAsmFrameObject(node.stack.get(0), mapping);
                saveStackFrame();
                break;

            case F_NEW:
            case F_FULL:
                //System.out.print(node.stack.get(0).toString());
                resetStackFrame();
                for (Object o : node.stack) {
                    vm.pushAsmFrameObject(o, mapping);
                }

                saveStackFrame();
                break;
        }
    }


    public void onPop32(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_pop.html

        int stackType = vm.typeIdAt(0);
        vm.checkUnderflow(node, 1);
        if (isVtValue(stackType)) {
            replaceWithBasic(node, POP2);
            vm.pop64();
        } else {
            vm.pop32();
        }
    }


    @Override
    public void onPop64(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_pop.html

        vm.checkSrcUnderflow32(node, 2);
        int stackType0 = vm.typeIdAt(0);

        if (isSrc64(stackType0)) {
            vm.pop64();
            return;
        }

        int stackType1 = vm.typeIdAt(1);

        // We have [32 32] or [64 32] on the stack. Latter situation is an error.
        if (isSrc64(stackType1)) {
            throw new RuntimeException("POP2: Illegal stack contents, partial access to a 64-bit value");
        }

        // We have [32 32] on the stack. One of which (or both) can be a VType.

        if (isVtValue(stackType0)) {
            vm.pop64();
            insertBasic(node, POP2);
            if (isVtValue(stackType1)) {
                // Do not change the instruction, it will now pop 2nd VType value
                vm.pop64();
            } else {
                replaceWithBasic(node, POP);
                vm.pop32();
            }
        } else {
            vm.pop32();
            if (isVtValue(stackType1)) {
                insertBasic(node, POP);
                // Do not change the instruction, it will now pop the VType value
                vm.pop64();
            } else {
                // Do not change the instruction. Remove 2 values from the stack
                vm.pop32();
            }
        }
    }

    @Override
    public void onDup32(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html

        int stackType = vm.typeIdAt(0);
        vm.checkUnderflow(node, 1);
        if (isVtValue(stackType)) {
            replaceWithBasic(node, DUP2);
            vm.dup64();
        } else {
            vm.dup32();
        }
    }


    @Override
    public void onDup32x(InsnNode node, int depth) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html

        assert(depth != 0);
        int stackType = vm.typeIdAt(0);
        if (isVtValue(stackType)) {
            // TODO: Finish other versions of the instruction!!!!
            throw new UnsupportedOperationException("DUP_* (ValueType)");
        } else {
            vm.dup32x(depth);
        }
    }


    @Override
    public void onDup64(InsnNode node) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_pop.html

        vm.checkSrcUnderflow32(node, 2);
        int stackType0 = vm.typeIdAt(0);

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
        if (isVtValue(stackType0)) {
            throw new UnsupportedOperationException("DUP2");
        } else {
            if (isVtValue(stackType1)) {

                throw new UnsupportedOperationException("DUP2");
            } else {
                vm.dup2();
            }
        }
    }


    @Override
    public void onDup64x(InsnNode node, int depth) {
        // https://cs.au.dk/~mis/dOvs/jvmspec/ref-_dup.html
        vm.checkSrcUnderflow32(node, depth + 2);

        // TODO: Finish other versions of the instruction!!!!
        throw new UnsupportedOperationException("DUP_*");
    }


    @Override
    public void onSwap32(InsnNode node) {

        vm.checkSrcUnderflow32(node, 2);
        int stackType0 = vm.typeIdAt(0);
        int stackType1 = vm.typeIdAt(1);

        if (isVtValue(stackType0)) {
            int varTop = varTop();
            insertBefore(node, new VarInsnNode(LSTORE, varTop));
            if (isVtValue(stackType1)) {
                insertBefore(node, new VarInsnNode(LSTORE, varTop + 2));
                insertBefore(node, new VarInsnNode(LLOAD, varTop));
                insertBefore(node, new VarInsnNode(LLOAD, varTop + 2));
            } else {
                insertBefore(node, new VarInsnNode(storeOpcodeForTypeId(stackType1), varTop + 2));
                insertBefore(node, new VarInsnNode(LLOAD, varTop));
                insertBefore(node, new VarInsnNode(loadOpcodeForTypeId(stackType1), varTop + 2));
            }

            remove(node);
        } else {
            if (isVtValue(stackType1)) {
                int varTop = varTop();
                insertBefore(node, new VarInsnNode(storeOpcodeForTypeId(stackType0), varTop));
                insertBefore(node, new VarInsnNode(LSTORE, varTop + 2));
                insertBefore(node, new VarInsnNode(loadOpcodeForTypeId(stackType0), varTop));
                insertBefore(node, new VarInsnNode(LLOAD, varTop + 2));
                remove(node);
            } else {
                // Just normal swap, no nothing
            }
        }

        vm.swap();
    }


    void injectInitializersFor(AbstractInsnNode node, int index, int opcode) {

        String[] names = state.scalarVtFieldNames;
        int[] nextField = state.prevVtField;
        for (int i = state.firstVtField[isStaticConstructor ? 1 : 0][index]; i >= 0; i = nextField[i]) {
            if (PUTFIELD == opcode) {
                insertLoadThis(node);
            }

            insertLoadVtNull(node, index);
            // NOTE: Hardcoded description for VT64
            insertBefore(node, new FieldInsnNode(opcode, state.classPath, names[i], "J"));
            if (logDbg) {
                log.dbg("Autoinit:   %s := NULL", names[i]);
            }
        }
    }

    void injectInitializers(AbstractInsnNode node) {

        for (int i = 0, n = mapping.numClasses(); i < n; ++i) {
            injectInitializersFor(node, i, isStaticConstructor ? PUTSTATIC : PUTFIELD);
        }
    }


    void onPossibleConstructorMethodCall(int opcode, String name, String owner) {
         if(name.equals(this.method().name) && owner.equals(state.classPath)) {
            assert(isConstructor);
            if ((INVOKESPECIAL != opcode) == isStaticConstructor) {
                // This constructor seem to call another constructor of the same class, so don't initialize Value Type fields AFTER it
                // I don't track "this" so I don't know for sure if it is call to
                if (null == initializationPatchNode) {
                    log.dbg("Initializer injection will be suppressed");
                    needAutoInitInjectionPoint = false;
                    suppressAutoInitInjection = true;
                }
            }
        }
    }



//    void transformMethod(AbstractInsnNode node, String name, String owner, String desc) {
//    }


    @Override
    public void onMethod(MethodInsnNode node) {

        //dbgBreakAt("testDtBasicUnboxing2", 50,54);
        //dbgBreakAt("testScalarToString", 43,46);

        // TODO: debug
        if (node.name.contains("lambda")) {
            node.name = node.name;
        }

        // Possibly change node signature to match Value Type signature on stack
        tryPreTransformObjectMethods(node);

        int nMethodArgs = AsmUtil.parseMethod(methodArgs, node.getOpcode(), node.owner, node.desc, mapping);

        if (needAutoInitInjectionPoint) {
            onPossibleConstructorMethodCall(node.getOpcode(), node.name, node.owner);
        }

        if (tryTransformAsVTypeMethodCall(node, node.name, node.owner, node.desc))
            return;

        int checkResult = verifyMethodArgs(methodArgs, nMethodArgs);
        if (TypeIdCast.isFailure(checkResult)) {
            onMethodVerificationError(methodArgs, nMethodArgs, checkResult, node.name, node.owner, node.desc);
        }

        if (0 != (checkResult & TypeIdCast.HAS_VTYPE) || isVt(methodArgs[0])) {
            node.name = nameConverter.transform(node.name, node.desc);
            node.desc = getTransformedDesc(node.desc, false, mapping);
        }

        // Process Array built-in class methods
        if (node.owner.charAt(0) == '[') {
            int ownerTypeId = DescriptorParser.getDescTypeId(node.owner, mapping);
            // Value Type Array method?
            if (isVt(ownerTypeId)) {
                if (!isVtArray(ownerTypeId))
                    throw new VerifyError("ValueType Array expected as node.owner: " + node.owner);

                node.owner = mapping.typeIdToDstTypeDesc(ownerTypeId);
                // NOTE: DO NOTHING WITH RETURN TYPE
                // Description is not transformed, as clone method is supposed to return scalar Object type
                // Subsequent CheckCast instruction is responsible for casting to ValueType Array destination type
                // CheckCast should not invoke array unboxing when casting from scalar object to ValueType array

                // if (node.name.equals("clone") && node.desc.equals("()Ljava/lang/Object;")) { }
            }
        }

        applyMethodArgs(node, this.methodArgs, nMethodArgs, checkResult);
    }


//    private void transformNonLambdaIndy(InvokeDynamicInsnNode node) {
//
//    }

    @Override
    public void onInvokeDynamic(InvokeDynamicInsnNode node) {
        // TODO: Incomplete implementation
        boolean vtInDesc = false, vtInInterface = false, vtInBody = false, vtInImplArgs = false, vtInBsm = false;
        String name = node.name, desc = node.desc;
        final int[] methodArgs = this.methodArgs;

        // TODO: Check, if we already properly support non-static lambda invocations
        int nMethodArgs = DescriptorParser.parseMethod(methodArgs, 0, desc, mapping);
        int checkResult = verifyMethodArgs(methodArgs, nMethodArgs);
        if (TypeIdCast.isFailure(checkResult)) {
            onMethodVerificationError(methodArgs, nMethodArgs, checkResult, name, null, desc);
        }

        if (0 != (checkResult & TypeIdCast.HAS_VTYPE) || isVt(methodArgs[0])) {
            // Still try to transform the signature
            desc = node.desc = getTransformedDesc(node.desc, false, mapping);
            vtInDesc = true;
        }

        Object[] bsmArgs = node.bsmArgs;
        boolean isLambda = node.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") && bsmArgs.length == 3 && bsmArgs[0] instanceof Type && bsmArgs[1] instanceof Handle && bsmArgs[2] instanceof Type;

        if (!isLambda) {
            // If not recognized lambda format. Still assume it is lambda.
            // Check for the presence of VT signature in bootstrap method arguments.
            int n = bsmArgs.length;
            for (int i = 0; i < n; i++) {
                String bsmDesc;
                Object arg = bsmArgs[i];
                if (arg instanceof Type) {
                    bsmDesc = arg.toString();
                } else if (arg instanceof Handle) {
                    bsmDesc = ((Handle) arg).getDesc();
                } else
                    continue;

                if (null == bsmDesc || bsmDesc.charAt(0) != '(')
                    continue;

                if (DescriptorParser.findVtInMethodDesc(bsmDesc, mapping)) {
                    vtInBsm = true;
                }
            }

            if (vtInDesc || vtInBsm) {
                addWarning(Warnings.INDY_VT);
            }

            applyMethodArgs(node, this.methodArgs, nMethodArgs, checkResult);
            return;
        }
        // Get type descriptors for: implemented functional interface, dynamic args, lambda implementation body
        String interfaceTypeDesc = ((Type)bsmArgs[0]).toString();
        String implTypeDesc = ((Type)bsmArgs[2]).toString();
        Handle bodyHandle = (Handle) bsmArgs[1];
        String bodyTypeDesc = bodyHandle.getDesc();

        // Search for Value Type in all 3 type descriptors
        vtInInterface = DescriptorParser.findVtInMethodDesc(interfaceTypeDesc, mapping);
        vtInImplArgs = DescriptorParser.findVtInMethodDesc(implTypeDesc, mapping);
        vtInBody = DescriptorParser.findVtInMethodDesc(bodyTypeDesc, mapping);

        vtInBsm = vtInInterface || vtInBody || vtInImplArgs;
        // No Value Types found in Lambda anywhere?
        if (!vtInDesc && !vtInBsm) {
            applyMethodArgs(node, methodArgs, nMethodArgs, checkResult);
            return;
        }

        // We have lambda and it references Value Types somewhere
        boolean isInstanceMethod = H_INVOKESTATIC != bodyHandle.getTag();

        final int[] bodyArgs = this.indyBodyArgs;
        final int[] interfaceArgs = this.indyInterfaceArgs;
        final int[] implArgs = this.indyImplArgs;

        int nBodyArgs = DescriptorParser.parseMethod(bodyArgs, isInstanceMethod, bodyHandle.getOwner(), bodyTypeDesc, mapping);
        int nInterfaceArgs = DescriptorParser.parseMethod(interfaceArgs, 0, interfaceTypeDesc, mapping);
        int nImplArgs = DescriptorParser.parseMethod(implArgs, 0, implTypeDesc, mapping);
        int nDynaArgs = nBodyArgs - nImplArgs;  // Not counting possible 'this' which hopefully can't be a Value Type
        // TODO: Low priority check that lambda body owner is not Value Type

        assert(nImplArgs == nInterfaceArgs);
        assert(nBodyArgs >= nImplArgs);
        assert(vtInBody || !vtInImplArgs);

        if (!vtInDesc && vtInInterface && vtInBody && vtInImplArgs && nInterfaceArgs == nImplArgs) do {
            // Check if types used in functional interface and interface implementation match
            int checkResult2 = Utils.compareMethodArgs(interfaceArgs, 0, implArgs, 0, nInterfaceArgs);
            if (TypeIdCast.SUCCESS != (checkResult2 & ~HAS_VTYPE))
                break;

            // Check if types used in interface implementation match last args of body
            checkResult2 = Utils.compareMethodArgs(bodyArgs, nBodyArgs - nImplArgs + 1, implArgs, 1, nImplArgs);
            if (TypeIdCast.SUCCESS != (checkResult2 & ~HAS_VTYPE))
                break;

            checkResult2 = Utils.compareMethodArgs(bodyArgs, 0, implArgs, 0, 1);
            if (TypeIdCast.SUCCESS != (checkResult2 & ~HAS_VTYPE))
                break;

            node.name = nameConverter.transform(name, interfaceTypeDesc);
            bsmArgs[0] = Type.getType(getTransformedDesc(interfaceTypeDesc, false, mapping));
            bsmArgs[1] = new Handle(bodyHandle.getTag(),
                    bodyHandle.getOwner(),
                    nameConverter.transform(bodyHandle.getName(), bodyTypeDesc),
                    getTransformedDesc(bodyTypeDesc, false, mapping),
                    bodyHandle.isInterface());

            bsmArgs[2] = Type.getType(getTransformedDesc(implTypeDesc, false, mapping));
                    //new Type(Type.METHOD,
        } while(false);

        applyMethodArgs(node, methodArgs, nMethodArgs, checkResult);
    }


    @Override
    public void onLabel(LabelNode node) {

        if (null == firstLabel) {
            firstLabel = node;
        }

        super.onLabel(node);
    }

    @Override
    public void onEnd() {
        if (isConstructor) {

            if (suppressAutoInitInjection) {
                // This method calls another constructor, so don't initialize Value Type fields
                log.dbg("Initializer injection was suppressed");
                return;
            }

            AbstractInsnNode node = initializationPatchNode;
            if (null == node) {
                if (null != firstLabel) {
                    node = firstLabel;
                    log.wrn("VT Init location is after the first label: %s", node);
                }
            }

            if (null != node)  {
                node = node.getNext();
            }

            if (null == node) {
                log.err("NO SUITABLE VT INIT LOCATION FOUND!");
            }

            assert(null != node);
            log.dbg("Injecting initializers", node);
            injectInitializers(node);
        }
    }


    /**
     * VariableNameFormatter methods
     */

    @Override
    public String format(int typeId, String originalName, int aSrc) {
        String typeStr;
        if (null != originalName) {
            typeStr = originalName;
        } else {
            typeStr = mapping.typeIdToShortPrintableString(typeId);
        }

        int pos = typeStr.lastIndexOf('/');
        if (-1 != pos) {
            typeStr = typeStr.substring(pos + 1, typeStr.length());
        }

        String varName = varTransformer.getVarNameBySrcIndex(aSrc);
        return null != varName ? typeStr + "_" + varName : typeStr;
    }
}
