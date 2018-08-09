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

package deltix.vtype.mapping;

import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import static deltix.vtype.transformer.AsmUtil.classPathToName;
import static deltix.vtype.mapping.ClassDefFlags.*;

/**
 * Defined a definition of the class whose methods are possibly being transformed is being transformed by this
 */
public class ClassDef {
    Mapping mapping;

    final int classIndex; // Index of ValueType class associated with this Class Definition. -1 for non-VT classes

    final int typeId; // TypeId of ValueType class associated with this Class Definition.

    int flags = 0;

    // Should be a static method that takes reference and returns long. Can take null reference.
    public MethodDef unboxingMethod;

    // method that will unbox an array: VT[] -> long[]
    public MethodDef arrayUnboxingMethod;

    // Should be a static method that takes long and returns reference. Basically, a static constructor.
    // Returns null if given a null constant, specific for a VT class
    public MethodDef boxingMethod;

    // method that will box an array: long[] -> VT[]
    public MethodDef arrayBoxingMethod;

    // Method that takes long, returns true if it equals null constant
    public MethodDef isNullMethod;

    // Maps src method name+signature to valuetype implementation. Argument is a hash created from method name+signature
    HashMap<Integer, MethodDef> methodMap = new HashMap<>(4);

    // Maps method names only
    HashMap<String, NameMapping> nameMappings = new HashMap<>(4);


    // class path for source ValueType class
    private String srcClassPath;

    // class path for implementation class that operates on transformed values
    private String dstClassPath;

    // "Laaa/bbb/ValueType;"
    private String srcClassDesc;

    // NULL value constant, specific for this Value Type. Used in vasrious NULL checks, for arrays fills
    // Obtained by calling VT.unboxingMethod(null)
    private long nullValue;

    // Suffix that should be appended to the name of implementation method implementing non-static source method
    private String methodSuffix;
    // Suffix that should be appended to the name of implementation method implementing static source method
    private String staticMethodSuffix;

    // Only contains ValueType class name, without full path
    private String srcShortClassPath;
    // Contains ValueType class name, with '&' prepended. Used for debug/warning/error output mostly
    private String srcShortRefClassPath;

    public void setAllSrcMethodsScanned() {
        setFlag(F_SRC_ALL_METHODS_DEFINED);
    }

    public void setAllDstMethodsScanned() {
        setFlag(F_DST_ALL_METHODS_DEFINED);
    }

    public void setMethodSuffix(String methodSuffix) {
        this.methodSuffix = methodSuffix;
    }

    public void setStaticMethodSuffix(String staticMethodSuffix) {
        this.staticMethodSuffix = staticMethodSuffix;
    }

    public long getNullValue() {
        return nullValue;
    }

    public String getSrcShortClassPath() {
        return srcShortClassPath;
    }

    public String getSrcShortRefClassPath() {
        return srcShortRefClassPath;
    }

    public String getSrcClassDesc() {
        return srcClassDesc;
    }

    // Simple linked list entry for name mapping pairs
    static class NameMapping {
        NameMapping next = null;
        final String src;
        final MethodDef dst;

        NameMapping(String src, MethodDef dst, NameMapping next) {

            assert(null != src && null != dst);
            this.src = src;
            this.dst = dst;
            this.next = next;
        }
    }

    ClassDef(Mapping mapping, int vtClassIndex, String srcClassPath, String dstClassPath) {

        this.mapping = mapping;
        this.classIndex = vtClassIndex;
        this.typeId = TypeId.vtValueFromIndex(vtClassIndex);
        this.srcClassPath = srcClassPath;
        this.dstClassPath = dstClassPath;
        this.srcClassDesc = "L" + srcClassPath + ';';
        int i = srcClassPath.lastIndexOf('/');
        this.srcShortClassPath = i >= 0 ? srcClassPath.substring(i + 1, srcClassPath.length()) : srcClassPath;
        this.srcShortRefClassPath = "&" + this.srcShortClassPath;
    }

    public ClassDef setFlag(int flag) {

        assert (0 == (flags & ~F_ALL_MAPPING_FLAGS));
        flags |= flag;
        return this;
    }

    public boolean isInitialized() {
        return ClassDefFlags.allInitialized(flags);
    }

    public boolean allMethodsMapped() {
        return ClassDefFlags.allMethodsMapped(flags);
    }

    public int getTypeId() {
        return typeId;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public String getSrcClassPath() {
        return srcClassPath;
    }

    public String getDstClassPath() {
        return dstClassPath;
    }

    void addMethod(MethodDef method) {
        Mapping.addMethod(methodMap, method);
    }

    public int getFlags() {
        return flags;
    }

    public void mapClassMethods() {

        if (!allSet(flags, F_SRC_BOX_METHOD_DEFINED | F_SRC_UNBOX_METHOD_DEFINED |
                F_DST_BOX_METHOD_DEFINED | F_DST_UNBOX_METHOD_DEFINED | F_SRC_ALL_METHODS_DEFINED | F_SRC_CLASS_PROCESSED)) {
            throw new IllegalStateException(String.format("Failed to finalize initialization of the ValueType class: %s flags: 0x%x ", srcClassPath, flags));
        }

        int nFailed = 0;
        StringBuffer failedMethods = new StringBuffer();
        int numMethods = 0;

        for (MethodDef mdef : methodMap.values()) {
            ++numMethods;
            if (!mdef.isInitialized()) {
                if (0 != nFailed++) {
                    failedMethods.append(", ");
                }

                failedMethods.append(mdef.getSrcString());
            }
        }

        if (0 != nFailed)
            throw new IllegalStateException(String.format("Failed to finalize initialization of the ValueType class: %s flags: 0x%x failed to map methods(%d/%d): %s ",
                    srcClassPath, flags, nFailed, methodMap.size(), failedMethods.toString()));

        flags |= F_DST_ALL_METHODS_DEFINED;
        if (!allMethodsMapped())
            throw new IllegalStateException(String.format("Failed to finalize initialization of the ValueType class: %s flags: 0x%x",
                srcClassPath, flags));

        mapping.mapClassMethods(this);
    }

    /**
     *
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void loadNullValue() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        Method unboxMethod = null;
        try {
            Class<?> cl = Class.forName(classPathToName(unboxingMethod.newOwner));
            unboxMethod = cl.getMethod(unboxingMethod.newName, cl);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format("Unable to load unboxing method of ValueType class: %s", srcClassPath));
        }

        try {
            nullValue =  (Long)unboxMethod.invoke(null, new Object[]{null});
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
            if (e.getTargetException() instanceof NullPointerException) {
                throw new NullPointerException(String.format("Unboxing method is expected to accept NULL - ValueType class: %s", srcClassPath));
            }
        }

        flags |= F_NULL_CONSTANT_DEFINED;
    }

    static String getPath(String path) {

        int i = path.lastIndexOf('/');
        return i < 0 ? null : path.substring(0, i + 1);
    }


    /**
     * Add method definition whose implementation is not yet found
     * @param methodName
     * @param desc
     * @param isStatic
     * @param preferredName
     * @throws Exception
     */
    public void addPartialMethod(String methodName, String desc, boolean isStatic, String preferredName) throws Exception {
        MethodDef mdef = MethodDef.createPartial(this, methodName, desc, isStatic, methodName);
        addMethod(mdef);

        String newDesc = DescriptorParser.getTransformedDesc(desc, !isStatic, mapping);

        // Do nothing if a method does not make use of its own class (static utility methods etc.)

        if (newDesc.equals(desc)) {
            if (null != preferredName)
                throw new Exception("Method " + methodName + desc + " is not supposed to have Value Type annotations");

            return;
        }

        if (null != preferredName) {
            // Not changing method path at this point
//            if (null == getPath(preferredName)) {
//                preferredName = getPath(dstClassPath) + preferredName;
//            }

            methodName = preferredName;
        }


        addNameMapping(mdef, methodName + (!isStatic ? methodSuffix : staticMethodSuffix) + newDesc);
    }

    private void addNameMapping(MethodDef mdef, String dstMethodSignature) {

        NameMapping prev = nameMappings.get(dstMethodSignature);
        nameMappings.put(dstMethodSignature, new NameMapping(dstMethodSignature, mdef, prev));
    }

    public void tryAddDestinationMethod(String name, String desc, boolean isCommutative) {

        // TODO: No overload support for destination class
        // for source class limited overload support - overloads are supposed to only point to a single implementation method
        NameMapping m = nameMappings.get(name + desc);
        if (null == m)
            return;

        for (;null != m; m = m.next) {
            if (!m.dst.isInitialized()) {
                m.dst.setDestinationMethod(dstClassPath, name, desc, isCommutative);
            }
        }
    }
}
