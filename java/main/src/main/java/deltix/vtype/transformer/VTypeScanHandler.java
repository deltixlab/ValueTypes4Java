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

import deltix.vtype.interpreter.EmptyOpcodeProcessor;
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;
import deltix.vtype.mapping.ClassDef;
import deltix.vtype.mapping.Mapping;
import org.objectweb.asm.tree.*;

/**
 * Detect Value Type usage in a method
 * Can be replaced with a simple ClassVisitor, but still used in the current version.
 */
public class VTypeScanHandler extends EmptyOpcodeProcessor {
    private final Mapping mapping;

    public boolean hasVTypeMethodCall;
    public boolean hasVTypeMethodArg;
    public boolean hasVTypeInVars;
    public boolean hasVTypeInNew;
    public boolean hasVTypeFieldAccess;

    VTypeScanHandler(Mapping mapping) {
        this.mapping = mapping;
    }

    private int getDescTypeId(final String desc) {
        return DescriptorParser.getDescTypeId(desc, mapping);
    }

    private int getClassTypeId(final String desc) {
        return DescriptorParser.getClassTypeId(desc, mapping);
    }

    public boolean hasVType() {
        boolean result = hasVTypeInVars || hasVTypeMethodArg || hasVTypeMethodCall || hasVTypeInNew || hasVTypeFieldAccess;
        if (result && mapping.logAllMethods) {
            System.out.printf("hasVTypeInVars:%s hasVTypeMethodArg:%s hasVTypeMethodCall:%s hasVTypeInNew:%s hasVTypeFieldAccess:%s%n",
                    hasVTypeInVars , hasVTypeMethodArg , hasVTypeMethodCall , hasVTypeInNew , hasVTypeFieldAccess);
        }

        return result;
    }

    int checkTypeId(int typeId) {

        if (!TypeId.isVt(typeId))
            throw new IllegalStateException("Not a Value Type Id");

        ClassDef classDef = mapping.getClassDefById(typeId);
        // TODO: Maybe only check if methods are declared in the source class
        if (null == classDef || !classDef.allMethodsMapped())
            throw new IllegalStateException("Class Definition object is not initialized (unable to load transformed classes?)");

        return typeId;
    }

    @Override
    public void onMethod(MethodInsnNode node) {
        int typeId;

        if (TypeId.isVt(typeId = getClassTypeId(node.owner))) {
            // owner is Value Type
            hasVTypeMethodCall = true;
            checkTypeId(typeId);
        } else {
            if (DescriptorParser.findVtInMethodDesc(node.desc, mapping)) {
                hasVTypeMethodArg = true;
            }
        }
    }

    @Override
    public void onVar(LocalVariableNode variable) {
        int typeId;

        if (TypeId.isVt(typeId = getDescTypeId(variable.desc))) {
            hasVTypeInVars = true;
            checkTypeId(typeId);
        }
    }

    private void onFIELD(FieldInsnNode node) {
        int typeId;

        if (TypeId.isVt(typeId = getDescTypeId(node.desc))) {
            hasVTypeFieldAccess = true;
            checkTypeId(typeId);
        }
    }

    @Override
    public void onPutField(FieldInsnNode node) {
        onFIELD(node);
    }

    @Override
    public void onPutStatic(FieldInsnNode node) {
        onFIELD(node);
    }

    @Override
    public void onGetField(FieldInsnNode node) {
        onFIELD(node);
    }

    @Override
    public void onGetStatic(FieldInsnNode node) {
        onFIELD(node);
    }

    @Override
    public void onNew(TypeInsnNode node) {
        int typeId;

        if (TypeId.isVt(typeId = getClassTypeId(node.desc))) {
            hasVTypeInNew = true;
            checkTypeId(typeId);
        }
    }

    @Override
    public void onANewArray(TypeInsnNode node) {
        this.onNew(node);
    }

    @Override
    public void onCheckCast(TypeInsnNode node) {
        this.onNew(node);
    }

    @Override
    public void onInstanceOf(TypeInsnNode node) {
        this.onNew(node);
    }

    @Override
    public void onMultiANewArray(MultiANewArrayInsnNode node) {
        int typeId;
        if (TypeId.isVt(typeId = getDescTypeId(node.desc))) {
            hasVTypeInNew = true;
            checkTypeId(typeId);
        }
    }

    @Override
    public boolean shouldProcessMethodCode() {
        return mapping.verifyAllMethods || hasVTypeInVars;
    }
}
