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


// TODO: This class still may be a subject to optimizations and encoding improvements
// TODO: Later may need separately track uninitialized refs
// Question: Do I use F_REF flag for VType arrays?

public abstract class TypeId {

    static final TypeIdFormatter defaultFormatter = new TypeIdDefaultFormatter();

    static final int F_SRC64        = 1;    // 1 means variable takes 2 cells before transformation
    static final int F_DST64        = 2;    // 1 means variable takes 2 cells after the transformation
    static final int F_REF          = 4;

    static final int F_TYPE_MASK    = 0xF;
    static final int F_TYPE_POS     = 4;

    static final int E_TYPE_NULL    = 0;  // Reference, obtained with ACONST_NULL
    static final int E_TYPE_NEW     = 1;  // Unused, new ref tracking is not technically feasible at this point
    static final int E_TYPE_OBJ     = 3;
    static final int E_TYPE_I32     = 4;
    static final int E_TYPE_I64     = 5;
    static final int E_TYPE_F32     = 6;
    static final int E_TYPE_F64     = 7;

    static final int F_TYPE_NULL    = E_TYPE_NULL << F_TYPE_POS;
    static final int F_TYPE_NEW     = E_TYPE_NEW << F_TYPE_POS;
    static final int F_TYPE_OBJ     = E_TYPE_OBJ << F_TYPE_POS;
    static final int F_TYPE_I32     = E_TYPE_I32 << F_TYPE_POS;
    static final int F_TYPE_I64     = E_TYPE_I64 << F_TYPE_POS;
    static final int F_TYPE_F32     = E_TYPE_F32 << F_TYPE_POS;
    static final int F_TYPE_F64     = E_TYPE_F64 << F_TYPE_POS;
    static final int F_TYPE_BITS    = F_TYPE_MASK << F_TYPE_POS;

    static final int F_VTYPE        = -0x80000000;

    static final int VT_ID_POS      = 16;
    static final int VT_ID_MASK     = 0xF;
    static final int F_ID_BITS      = VT_ID_MASK << VT_ID_POS;

    public static final int VT_WILDCARD_INDEX = VT_ID_MASK;

    static final int VT_ADEPTH_POS  = 8;
    static final int VT_ADEPTH_MASK = 0xFF;
    static final int F_ARRAY_BITS   = VT_ADEPTH_MASK << VT_ADEPTH_POS;

    static {
        // Some operations rely on this due to optimizations
        assert(F_SRC64 << 1 == F_DST64);
        assert(F_SRC64 == 1);
    }

    public static final int VOID        = 0;
    public static final int I32         = F_TYPE_I32;
    public static final int I64         = F_TYPE_I64 | F_SRC64 | F_DST64;
    public static final int F32         = F_TYPE_F32;
    public static final int F64         = F_TYPE_F64 | F_SRC64 | F_DST64;
    public static final int OBJ_REF     = F_TYPE_OBJ | F_REF;
    //public static final int ARRAY_REF   = F_TYPE_ARRAY | F_REF;
    public static final int NULL_REF    = F_TYPE_NULL | F_REF;
    public static final int VT64        = F_VTYPE | F_TYPE_OBJ | F_DST64;
    protected static final int VT_REF   = F_VTYPE | F_TYPE_OBJ | F_REF;

    protected static final int typeEnum2Value[] = { NULL_REF, 0, 0, OBJ_REF, I32, I64, F32, F64 };


    static final int VT_TYPE_CHAR_POS   = 20;
    static final int VT_TYPE_CHAR_MASK  = 0xFF;

    static int getTypeEnum(int typeId) {
        return (typeId >>> F_TYPE_POS) & F_TYPE_MASK;
    }

    /**
     * @return true for all basic types represented by int, references, ValueType32 and not transformed ValueType64
     */
    public static boolean isDst32(int typeId) {
        return F_DST64 != (typeId & (F_DST64 | F_ARRAY_BITS | F_REF));
    }

    /**
     * @return true for long or double type
     */
    public static boolean isSrc64(int typeId) {
        return F_SRC64 == (typeId & (F_SRC64 | F_ARRAY_BITS | F_REF)); // TODO: Can simplify after recent changes
    }

    /**
     * @return true for long, double or transformed ValueType64 type
     */
    public static boolean isDst64(int typeId) {
        return F_DST64 == (typeId & (F_DST64 | F_ARRAY_BITS | F_REF));
    }

    /**
     *
     * @param typeId
     * @return Size in 32-bit cells for a type BEFORE the transformation
     */
    public static int size32Src(int typeId) {
        return 1 + (typeId & TypeId.F_SRC64);
    }

    /**
     *
     * @param typeId
     * @return Size in 32-bit cells for a type AFTER the transformation. May be extended later when some Value Types start taking more than 8 bytes
     */
    public static int size32Dst(int typeId) {
        return 1 + (typeId & TypeId.F_DST64) / 2;
    }

    public static int vtValueDstSize(int typeId) {
        assert(typeId < 0);
        return 2; // 64 bits
    }

    /**
     * @return true for non-64-bit scalar (basic types except long &amp; double, not objects)
     */
    public static boolean isScalar32(int typeId) {
        return typeId != VOID && 0 == (typeId & ~F_TYPE_BITS);
    }

    /**
     * @return true for 64-bit types: long or double
     */
    public static boolean isScalar64(int typeId) {
        return (F_SRC64 | F_DST64) == (typeId & ~F_TYPE_BITS);
    }

    public static boolean isVt(int typeId) {
        return typeId < 0;
    }

    public static boolean isVtValue(int typeId) {
        return (typeId & ~F_ID_BITS) == VT64;
    }

    public static boolean isVtRefOrVtArray(int typeId) {
        return isVt(typeId) && isRefDst(typeId);
    }

    public static boolean isRefSrc(int typeId) {

        return 0 != (typeId & (F_VTYPE | F_ARRAY_BITS | F_REF));
    }

    public static boolean isRefDst(int typeId) {

        return 0 != (typeId & (F_ARRAY_BITS | F_REF));
    }

    public static boolean isRefScalarDst(int typeId) {

        return F_REF == (typeId & (F_ARRAY_BITS | F_REF));
    }

    // Reference to scalar Value Type
    public static boolean isVtRef(int typeId) {
        return (typeId & ~F_ID_BITS) == VT_REF;
    }

    public static boolean isVtNonArray(int typeId) {
        return (typeId & (F_VTYPE | F_ARRAY_BITS)) == (F_VTYPE);
    }

    public static boolean isSameVtClass(int typeIdA, int typeIdB) {
        assert(isVt(typeIdA & typeIdB));
        return 0 == ((typeIdA ^ typeIdB) & F_ID_BITS);
    }


    public static boolean isSameArrayDepth(int typeIdA, int typeIdB) {
        return 0 == ((typeIdA ^ typeIdB) & F_ARRAY_BITS);
    }

    public static int getArrayDepth(int typeId) {
        return (typeId >> VT_ADEPTH_POS) & VT_ADEPTH_MASK;
    }

    public static boolean isNonVtScalarRef(int typeId) {
        return F_REF == (typeId & (F_REF | F_VTYPE | F_ARRAY_BITS));
    }

    public static boolean isNonVtArray(int typeId) {
        return !isVt(typeId) && (0 != (typeId & F_ARRAY_BITS));
    }

    public static boolean isVtArray(int typeId) {
        return isVt(typeId) && (0 != (typeId & F_ARRAY_BITS));
    }

    public static boolean isArray(int typeId) {
        return 0 != (typeId & F_ARRAY_BITS);
    }

    public static boolean isVtA1Ref(int typeId) {
        return (typeId & ~F_ID_BITS) == ((1 << VT_ADEPTH_POS) | F_VTYPE | F_REF | F_TYPE_OBJ);
    }

    public static int getVtClassIndex(int typeId) {

        assert(isVt(typeId));
        return (typeId >> VT_ID_POS) & VT_ID_MASK;
    }

    public static boolean isVtWildcard(int typeId) {
        return (VT_WILDCARD_INDEX << VT_ID_POS) == (typeId & (VT_ID_MASK << VT_ID_POS));
    }

    // Get underlying value for array of any depth (or VT ref). Should only be applied to arrays of vtypes or vtype refs
    public static int vtValueFrom(int typeId) {

        assert(isVt(typeId));
        return typeId & ~(F_REF | F_ARRAY_BITS) | F_DST64;
    }

    /**
     * Get innermost type of array of any depth
     */
    public static int getArrayBaseElement(int typeId) {
        // Will set 64-bit type flag if array underlying type is 64-bit
        assert(0 != typeId);
        assert(getTypeEnum(typeId) >= E_TYPE_OBJ);
        return isVt(typeId) ? typeId & ~(F_REF | F_ARRAY_BITS) | F_DST64 : typeEnum2Value[getTypeEnum(typeId)];
    }


    public static int getArrayElement(int typeId) {
        assert(0 != (typeId & F_ARRAY_BITS));
        typeId -= (1 << VT_ADEPTH_POS);
        return 0 != (typeId & F_ARRAY_BITS) ? typeId : getArrayBaseElement(typeId);
    }


    public static int arrayFrom(int typeId, int depth) {

        assert(depth > 0 && depth <= VT_ADEPTH_MASK);
        assert(0 != typeId && getTypeEnum(typeId) >= E_TYPE_OBJ);
        assert(!isArray(typeId));

        return typeId & ~(F_ARRAY_BITS | F_DST64 | F_SRC64) | (depth << VT_ADEPTH_POS) | F_REF;
    }

    /**
     * Convert Any value typeId to array of the same type, or increment depth of an array
     */
    public static int arrayIncrementDepth(int typeId) {
        assert(0 != typeId && getTypeEnum(typeId) >= E_TYPE_OBJ);
        assert((typeId & F_ARRAY_BITS) != F_ARRAY_BITS);
        return typeId & ~(F_ARRAY_BITS | F_DST64 | F_SRC64) | ((typeId + (1 << VT_ADEPTH_POS)) & F_ARRAY_BITS) | F_REF;
    }


    public static int refOrArrayFrom(int depth) {

        assert(depth >= 0 && depth <= VT_ADEPTH_MASK);
        return OBJ_REF | (depth << VT_ADEPTH_POS);
    }

    /**
     * Convert null object type to nonnull object type
     * @param typeId
     * @return
     */
    public static int toNonNull(int typeId) {

        return (typeId & F_TYPE_BITS) == F_TYPE_NULL ? typeId & ~F_TYPE_BITS | F_TYPE_OBJ : typeId;
    }


    public static boolean isNull(int typeId) {

        return F_TYPE_NULL == (F_TYPE_BITS & typeId);
    }


    public static int vtSubstituteTypeTo(int from, int to) {

        return from & ~(VT_ID_MASK << VT_ID_POS) | (to & (VT_ID_MASK << VT_ID_POS));
    }


    public static int vtValueFromIndex(int classIndex) {

        assert(classIndex >=0 && classIndex <= VT_ID_MASK);
        return ((classIndex & VT_ID_MASK) << VT_ID_POS) | VT64;
    }

    public static int vtArrayFromIndex(int classIndex, int depth) {

        return ((classIndex & VT_ID_MASK) << VT_ID_POS) | (depth << VT_ADEPTH_POS) | VT_REF;
    }


    public static int vtRefFrom(int typeId) {
        assert (isVt(typeId));
        return typeId & ~(F_DST64 | F_SRC64 | F_ARRAY_BITS) | F_REF;
    }


    public static String toString(int typeId) {
        return defaultFormatter.typeIdToShortPrintableString(typeId);
    }


    public static String toString(int typeId, TypeIdFormatter formatter) {

        return formatter.typeIdToShortPrintableString(typeId);
    }


    /**
     * Format validation methods. To be used in asserts to ensure TypeIds are not malformed
     */
    boolean isValidVt(int typeId) {
        return isVtValue(typeId) || isVtRef(typeId) || isVtArray(typeId) && isVtValue(vtValueFrom(typeId));
    }


    boolean isValidNonVtScalar(int typeId) {
        // TODO: Needed for debug validation etc.
        return true;
    }


    private boolean isValidNonVtScalarRef(int typeId) {
        return OBJ_REF == typeId || NULL_REF == typeId;
    }


    private boolean isValidRefType(int typeId, String desc) {

        if (isVt(typeId)) {
            if (isValidVt(typeId))
                return true;
        } else if (TypeId.isArray(typeId)) {
            if (isValidNonVtScalar(getArrayBaseElement(typeId)))
                return true;
        } else if (isValidNonVtScalarRef(typeId))
            return true;

        return false;
    }
}


