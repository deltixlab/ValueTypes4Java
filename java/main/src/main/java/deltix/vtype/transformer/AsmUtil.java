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

import deltix.vtype.mapping.Mapping;
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;
import deltix.vtype.type.VariableMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static deltix.vtype.type.DescriptorParser.findVtInMethodDesc;
import static deltix.vtype.type.DescriptorParser.getTransformedDesc;
import static deltix.vtype.type.DescriptorParser.isPossibleTransformedVtSetter;
import static deltix.vtype.type.TypeId.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Contains various helpers functions related to class type processing and to asm framework
 */
public class AsmUtil {
    private static final int FRAME_TOP                  = 0;  // Opcodes.TOP etc.
    private static final int FRAME_INTEGER              = 1;
    private static final int FRAME_FLOAT                = 2;
    private static final int FRAME_DOUBLE               = 3;
    private static final int FRAME_LONG                 = 4;
    private static final int FRAME_NULL                 = 5;
    private static final int FRAME_UNINITIALIZED_THIS   = 6;
    private static final int FRAME_OBJECT               = 7;

    private static int newArrayLut[] = {TypeId.I32, TypeId.I32, TypeId.F32, TypeId.F64, TypeId.I32,TypeId.I32,TypeId.I32, TypeId.I64 };


    static public String makeTypeDesc(String prefix, String className) {
        return new StringBuffer(prefix).append("L").append(className).append(";").toString();
    }

    static public String makeTypeDesc(String className) {
        return makeTypeDesc("", className);
    }

    public static String classNameToPath(String className) {
        return className.trim().replace('.', '/');
    }

    public static String classPathToName(String classPath) {
        return classPath.replace('/', '.');
    }

    private static int classNameStartIndex(String path) {
        return path.lastIndexOf('/') + 1;
        //return max(path.lastIndexOf('/'), path.lastIndexOf('.')) + 1;
    }

    /**
     * return class path without class name
     * @param path
     * @return
     */
    public static String extractClassPath(String path) {
        int i = classNameStartIndex(path);
        return 0 == i ? null : path.substring(0, i);
    }

    /**
     * return class name without path part
     * @param path
     * @return
     */
    public static String extractClassName(String path) {

        int i = classNameStartIndex(path);
        return path.substring(i, path.length());
    }

    public static String replaceClassPath(String path, String newPath) {

        int i = classNameStartIndex(newPath);
        return newPath.substring(0, i) + extractClassName(path);
    }


    private static String frameNodeTypeToString(int type) {
        switch(type) {
            case F_NEW:     return "F_NEW";
            case F_FULL:    return "F_FULL";
            case F_APPEND:  return "F_APPEND";
            case F_CHOP:    return "F_CHOP";
            case F_SAME:    return "F_SAME";
            case F_SAME1:   return "F_SAME1";
            default:        return "???";
        }
    }


    public static Object typeIdToFrameEntry(int typeId, String typeIdDesc) {

        if (isVtRefOrVtArray(typeId))
            return typeIdDesc;

        if (isVtValue(typeId))
            return 4;

        throw new UnsupportedOperationException("Non-VType values not supported in this method");
    }


    public static int frameEntryEnumToTypeId(Integer o) {

        switch (o) {
            case FRAME_TOP:
                return TypeId.VOID;

            case FRAME_INTEGER:
                return TypeId.I32;

            case FRAME_FLOAT:
                return TypeId.F32;

            case FRAME_DOUBLE:
                return TypeId.F64;

            case FRAME_LONG:
                return TypeId.I64;

            case FRAME_NULL:
                return TypeId.NULL_REF;

            case FRAME_UNINITIALIZED_THIS:
                // We don't track 'new this' state.
                return TypeId.OBJ_REF; //, "NewRef");

            // NOTE: Other object types appear as String desc
            default:
                throw new IllegalArgumentException(o.toString());
        }

        // TODO: Minor optimization. May replace with array.
    }


    public static String frameEntryEnumToString(Integer o) {
        switch (o) {
            case FRAME_TOP:
                return "<top>";

            case FRAME_INTEGER:
                return "int";

            case FRAME_FLOAT:
                return "float";

            case FRAME_DOUBLE:
                return "double";

            case FRAME_LONG:
                return "long";

            case FRAME_NULL: // NULL
                return "null";

            case FRAME_UNINITIALIZED_THIS: // New THIS
                return "new_this";

            default:
                throw new IllegalArgumentException(o.toString());
        }
    }


    static String stackFrameEntryToString(Object o) {

        if (null == o)
            return "<nul>";

        if (o instanceof Integer)
            return frameEntryEnumToString((Integer)o);

        return o.toString();
    }


    public static String stackFrameToString(FrameNode node) {

        StringBuffer s = new StringBuffer();
        s.append(frameNodeTypeToString(node.type));

        if(node.local != null) {
            s.append("   Vars:[");
            for (Object var : node.local) {
                s.append(' ').append(stackFrameEntryToString(var));
            }

            s.append("]");
        }

        if(node.stack != null) {
            s.append("   Stack:[");
            for (Object entry : node.stack) {
                s.append(' ').append(stackFrameEntryToString(entry));
            }

            s.append("]");
        }

        return s.toString();
    }


    public static int newArrayTypeToTypeId(int newArrayType) {

        assert (T_LONG - T_BOOLEAN == 7 && newArrayLut.length == 8);
        return newArrayLut[newArrayType - T_BOOLEAN];
    }


    static class VarIndexComparator implements Comparator<LocalVariableNode> {
        @Override
        public int compare(LocalVariableNode var1, LocalVariableNode var2) {
            return Integer.compare(var1.index,var2.index);
        }
    }

    public static void sortVars(List<LocalVariableNode> vars) {
        Collections.sort(vars, new VarIndexComparator());
    }

    private static int loadStoreOpcodeOffsetForTypeId(int typeId) {

        if (TypeId.isVt(typeId)) {
                return TypeId.isVtValue(typeId) ? LSTORE - ISTORE : ASTORE - ISTORE;
        }

        // TODO: Not all types are checked?
        if (typeId == TypeId.I32) return ISTORE - ISTORE;
        if (typeId == TypeId.I64) return LSTORE - ISTORE;
        if (typeId == TypeId.F32) return FSTORE - ISTORE;
        if (typeId == TypeId.F64) return DSTORE - ISTORE;
        if (isRefDst(typeId))     return ASTORE - ISTORE;

        // Return sentinel value
        return -0x10000;
    }

    public static int storeOpcodeForTypeId(int typeId) {

        return loadStoreOpcodeOffsetForTypeId(typeId) + ISTORE;
    }

    public static int loadOpcodeForTypeId(int typeId) {

        return loadStoreOpcodeOffsetForTypeId(typeId) + ILOAD;
    }

    public static int addVarFromFrame(final VariableMap vars, final Mapping mapping, Object o) {

        int typeId;
        if (o instanceof Integer) {
            vars.add(typeId = AsmUtil.frameEntryEnumToTypeId((Integer)o), null);
        } else
        if (o instanceof String) {
            String name = (String)o;
            typeId = mapping.getVTypeIdFromDesc(name);
            vars.add(typeId, TypeId.isVt(typeId) ? null : name);
        } else
        if (o instanceof LabelNode || o instanceof Label) {
            vars.add(typeId = TypeId.OBJ_REF, "NewRef");
        }
        else
            throw new IllegalArgumentException(o.toString());

        return typeId;
    }

    /**
     * Initialize vars with arguments from method type signature
     * @param vars variable list to initialize
     * @param methodNode VType Method Node
     * @param mapping
     */
    public static void initializeMethodArguments(final VariableMap vars, final org.objectweb.asm.tree.MethodNode methodNode, final Mapping mapping) {

        String name, desc;
        vars.clear();
        if (methodNode instanceof MethodNode) {
            MethodNode mn = (MethodNode) methodNode;
            name = mn.className;
            desc = mn.originalDesc;
        } else {
            name = null;
            desc = methodNode.desc;
        }

        if (0 == (ACC_STATIC & methodNode.access)) {
            vars.add(TypeId.OBJ_REF, "this");
        }

        if (!desc.startsWith("()")) {
            int[] methodArgs = new int[0x100];
            int nMethodArgs = DescriptorParser.parseMethod(methodArgs, false, name, desc, mapping);
            vars.add(methodArgs, null, 1, nMethodArgs);
        }
    }


    /**
     * Find closest LabelNode in backward direction, skipping line number nodes
     * @param node node to start search from
     * @return found node or null
     */
    static LabelNode findNearPrevLabel(AbstractInsnNode node) {

        if (isLabelNode(node))
            return (LabelNode) node;

        for (node = node.getPrevious(); null != node; node = node.getPrevious()) {
            if (isLabelNode(node))
                return (LabelNode) node;

            if (!isLineNode(node))
                break;
        }

        return null;
    }

    /**
     * Find closest LabelNode in forward direction, skipping line number nodes
     * @param node node to start search from
     * @return found node or null
     */
    static LabelNode findNearNextLabel(AbstractInsnNode node) {

        if (isLabelNode(node))
            return (LabelNode) node;

        for (node = node.getNext(); null != node; node = node.getNext()) {
            if (isLabelNode(node))
                return (LabelNode) node;

            if (!isLineNode(node))
                break;
        }

        return null;
    }

    /**
     * return previous node, don't throw on null
     * @param node
     * @return previous node or null
     */
    public static AbstractInsnNode prev(AbstractInsnNode node) {
        return null != node ? node.getPrevious() : null;
    }

    /**
     * return next node, don't throw on null
     * @param node
     * @return next node or null
     */
    public static AbstractInsnNode next(AbstractInsnNode node) {
        return null != node ? node.getNext() : null;
    }

    // Is a label node?
    public static boolean isLabelNode(AbstractInsnNode node) {
        return node instanceof LabelNode;
    }

    // Is a line node?
    public static boolean isLineNode(AbstractInsnNode node) {
        return node instanceof LineNumberNode;
    }

    /**
     * Print ASM variable list to console. For debugging.
     * @param vars
     */
    public static void dumpVarNodes(List vars) {

        int n = 0;
        if (null == vars)
            return;

        for(Object i : vars) {
            LocalVariableNode v = (LocalVariableNode)i;
            System.out.printf(" %s:(%s :%s #%s) ",
                    n++, v.name, v.desc, v.index);
        }

        System.out.println("");
    }


    public static int parseMethod(int[] methodArgs, int op, String owner, String desc, Mapping mapping) {
        boolean isNonStatic = false;
        switch (op) {
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE:
                isNonStatic = true;
                break;

            case INVOKESTATIC:
                break;

            default:
                throw new java.lang.IllegalArgumentException(String.format("Unknown opcode for method node: %s", op));
        }

        return DescriptorParser.parseMethod(methodArgs, isNonStatic, owner, desc, mapping);
    }


    public static int parseMethod(int[] methodArgs, MethodInsnNode node, Mapping mapping) {

        return parseMethod(methodArgs, node.getOpcode(), node.owner, node.desc, mapping);
    }

    public static boolean shouldBeRenamed(String name, String desc, Mapping mapping) {
        return !DescriptorParser.hasNoArgs(desc) && !appearsToBeVtSetter(name, desc, mapping);
    }

    public static boolean isSetterName(String name) {
        return name.length() > 3 && name.startsWith("set")
                && (!Character.isAlphabetic(name.codePointAt(3)) || Character.isUpperCase(name.codePointAt(3)));
    }

    public static boolean isSetterFlags(int access) {
        return 0 == (access & (ACC_STATIC | ACC_BRIDGE));
    }

    public static boolean appearsToBeVtSetter(int access, String name, String desc, Mapping mapping) {

        return isSetterFlags(access) && appearsToBeVtSetter(name, desc, mapping);
    }

    public static boolean possibleTransformedVtSetter(int access, String name, String desc, Mapping mapping) {
        return isSetterFlags(access) && isSetterName(name)
                && isPossibleTransformedVtSetter(desc) && !findVtInMethodDesc(desc, mapping);
    }

    public static boolean appearsToBeVtSetter(String name, String desc, Mapping mapping) {

        return isSetterName(name)
                && findVtInMethodDesc(desc, mapping)
                //&& !newDesc.equals(desc)
                && isPossibleTransformedVtSetter(getTransformedDesc(desc, false, mapping));
    }
}
