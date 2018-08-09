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

import deltix.vtype.type.TypeId;

import static deltix.vtype.type.DescriptorParser.parseMethod;
import static deltix.vtype.type.TypeId.*;

public class MethodDef {

    final ClassDef owner;
    public int[] args = new int [0x100];
    private boolean[] isVTypeArg = new boolean[0x100];
    public int numArgs;
    private int numVTypeArgs;

    boolean isInitialized = false;
    boolean isCommutative = false;
    boolean isNonStaticSrc;
    boolean returnsVType;

    String oldName;
    String oldDesc;

    String newOwner;
    String newName;
    String newDesc;
    String debugName;

    /*
     * Conversion rules:
     * Static:
     * Target.(ABCX)X->AsmUtil.(ABCL)L
     * Normal:
     * Target.(ABCX)X->AsmUtil.(LABCL)L
     *
     */


    MethodDef(final ClassDef owner) {
        this.owner = owner;
    }


    /**
     * Special constructor for autoType methods.
     * @param oldName
     * @param oldDesc
     * @param newOwner
     * @param newName
     * @param newDesc
     * @param debugName
     */
    public MethodDef(String oldName, String oldDesc, String newOwner, boolean isStatic, String newName, String newDesc, String debugName) {
        this(null, oldName, oldDesc, isStatic, false, newOwner, newName, newDesc, debugName);
    }

    /**
     * Constructor for "special" ValueType methods, such as box/unbox/isNull
     * @param owner
     * @param newOwner
     * @param newName
     * @param newDesc
     * @param debugName
     */
    public MethodDef(final ClassDef owner, String newOwner, String newName, String newDesc, String debugName) {

        this(owner);
        this.newOwner = newOwner;
        this.newName = newName;
        this.newDesc = newDesc;
        this.debugName = debugName;

        try {
            numArgs = parseMethod(args, false, null, newDesc, owner.mapping);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("Unable to parse boxing/unboxing method: %s.%s%s", newOwner, newName, newDesc), e);
        }

        if (-1 == numArgs)
            throw new IllegalArgumentException(String.format("Unable to parse boxing/unboxing method: %s.%s%s", newOwner, newName, newDesc));
    }

    /**
     * Static constructor for ValueType class methods. Performs 1st part of the initialization
     * @param owner
     * @param oldName
     * @param oldDesc
     * @param isStatic
     * @param debugName
     * @return
     */
    public static MethodDef createPartial(final ClassDef owner, String oldName, String oldDesc, boolean isStatic, String debugName) {
        MethodDef mdef = new MethodDef(owner, oldName, oldDesc, isStatic, false,
                null, null, null, debugName);

        return mdef;
    }

    /**
     * Constructor for ValueType class methods
     *
     * @param owner
     * @param oldName
     * @param oldDesc
     * @param isStatic
     * @param isCommutative
     * @param newOwner
     * @param newName
     * @param newDesc
     * @param debugName
     */
    public MethodDef(final ClassDef owner, String oldName, String oldDesc, boolean isStatic, boolean isCommutative,
                     String newOwner, String newName, String newDesc, String debugName) {

        this(owner);
        this.newOwner = newOwner;
        this.newName = newName;
        this.newDesc = newDesc;
        this.debugName = debugName;

        this.isNonStaticSrc = !isStatic;
        this.isCommutative = isCommutative;
        this.oldName = oldName;
        this.oldDesc = oldDesc;

        if (null != newOwner && null != newName && null != newDesc) {
            initialize();
        }
    }

    /**
     * Performs 2nd part of the initialization of the ValueType class method, typically after finding and binding utility method that implements it
     * @param newOwner
     * @param newName
     * @param newDesc
     * @param isCommutative
     */
    void setDestinationMethod(String newOwner, String newName, String newDesc, boolean isCommutative) {

        this.newOwner = newOwner;
        this.newName = newName;
        this.newDesc = newDesc;
        this.isCommutative = isCommutative;

        if (null != newOwner && null != newName && null != newDesc) {
            initialize();
        }
    }

    void initialize() {
        // Parse and verify arguments
        int[] args2 = new int [0x100];
        int numArgs2;
        Mapping mapping = null != owner ? owner.mapping : null;
        int ownerIndex = null != owner ? getVtClassIndex(owner.typeId) : TypeId.VT_WILDCARD_INDEX;

        numArgs = parseMethod(args, isNonStaticSrc, null != mapping ? owner.getSrcClassPath() : null, oldDesc, mapping);
        if (numArgs == -1)
            throw new IllegalArgumentException(String.format("Unable to parse src: %s.%s", oldName, oldDesc));

        numArgs2 = parseMethod(args2, false, null, newDesc, mapping);
        if (numArgs2 == -1)
            throw new IllegalArgumentException(String.format("Unable to parse dst: %s.%s", newName, newDesc));

        if (numArgs != numArgs2)
            throw new IllegalArgumentException(String.format("number of args mismatch between %s.%s and %s.%s", oldName, oldDesc, newName, newDesc));

        if (args[0] != args2[0]) {
            if (!(isVt(args[0]) && ownerIndex == getVtClassIndex(args[0]) && newDesc.endsWith("J") && getArrayDepth(args[0]) == getArrayDepth(args2[0]))) {
                throw new IllegalArgumentException(String.format("return value mismatch between %s.%s and %s.%s", oldName, oldDesc, newName, newDesc));
            } else {
                // Returns compatible integer representation or array of integers
                isVTypeArg[0] = returnsVType = true;
            }
        }

        int numVTypeArgs = 0;
        // TODO: Currently no support for using other value types in value type methods
        for (int i = 1; i <= numArgs; ++i) {
            if (args[i] != args2[i]) {
                if (!(isVt(args[i]) && ownerIndex == getVtClassIndex(args[i]) && getArrayDepth(args[i]) == getArrayDepth(args2[i]))) {
                    throw new IllegalArgumentException(String.format("arg %d mismatch 0x%x <=> 0x%x between %s.%s and %s.%s",
                            i, args[i], args2[i], oldName, oldDesc, newName, newDesc));
                } else {
                    isVTypeArg[i] = true;
                    ++numVTypeArgs;
                }
            }
        }

        this.numVTypeArgs = numVTypeArgs;

        if (TypeId.VT_WILDCARD_INDEX == ownerIndex) {
            oldDesc = oldDesc.replace("LValueType;", "Ljava/lang/Object;");
        }

        isInitialized = true;
    }

    public boolean isAutoMethod() {
        return null == owner;
    }

    public ClassDef getOwner() {
        return owner;
    }

    public String getDebugName() {
        return debugName;
    }

    public String getNewName() {
        return newName;
    }

    public String getNewDesc() {
        return newDesc;
    }

    public String getNewOwner() {
        return newOwner;
    }

    public String getSrcString() {
        return new StringBuffer(isNonStaticSrc ? "" : "static ").append(oldName).append(oldDesc).toString();
    }

    public void setCommutative(boolean commutative) {
        isCommutative = commutative;
    }

    public boolean isInitialized() {
        if (null == newDesc || null == newOwner)
            return false;

        return this.isInitialized;
    }
}
