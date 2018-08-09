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

import static deltix.vtype.type.TypeId.*;

/**
 * This class contains the logic to check the possiblity of converting a type (possibly a transformed VType) into another
 */

public abstract class TypeIdCast {
    // Same or fully compatible type
    public static final int SUCCESS            = 0;

    // Destination type is a VType
    public static final int HAS_VTYPE          = 1;

    // Need VType boxing operation. It is possible to return NEED_BOXING without setting HAS_VTYPE
    // Elements in this case. (????) TODO: Do I still need this behavior?
    public static final int NEED_BOXING        = 2;

    // Need VType unboxing. HAS_VTYPE is assumed
    public static final int NEED_UNBOXING      = 4;

    // Conversion is not possible. Type incompatibility encountered probably due to a logic error,
    // invalid bytecode or unsupported case
    public static final int FAILURE            = -0x80000000;

        // Need destination type initialization, for Value Type at least
    public static final int UNINITIALIZED_VTYPE= 8;
    public static final int DISCARD_VTYPE      = 0x10;      // Need to discard VType at destination
    public static final int NEED_SUBSTITUTION  = 0x20;
    public static final int NO_SUBSTITUTION    = 0x40;
    public static final int DEPTH_MASK         = 0xFF00;
    // TODO: Maybe: Add array type?

    public static boolean isFailure(int checkResult) {
        return checkResult < 0;
    }


    public static int checkCastBase(int from, int to) {

        assert(from != to);
        assert(!isVtWildcard(to));
        if (isVt(to)) {
            if (isVt(from)) {

                if (!isSameArrayDepth(from, to))
                    return FAILURE; // We are tracking array types and mismatched number of dimensions equals failure

                if (isSameVtClass(from, to)) {
                    if (isVtRef(from) && isVtValue(to))
                        return NEED_UNBOXING | HAS_VTYPE;

                    if (isVtValue(from) && isVtRef(to))
                        return NEED_BOXING | HAS_VTYPE;

                    if (isVtArray(from) && isVtArray(to))
                        return HAS_VTYPE;
                }

                assert("This code must be unreachable" == null);
                return FAILURE;
            }

            // "from" is not ValueType, "to" is some ValueType

            if (isVtValue(to)) {
                if (isRefScalarDst(from)) {
                    return NEED_UNBOXING | HAS_VTYPE;
                }
            } else {
                if (isVtArray(to)) {
                    // TODO: Uncomment
                    //if (!isSameArrayDepth(from, to))
                        //return FAILURE;

                    // TODO: Remove this
                    if (isNonVtScalarRef(from))
                        return HAS_VTYPE;   // Assume cast from Object to VT Array

                    return NEED_UNBOXING | HAS_VTYPE;
                }

                // "to" is ValueType but neither Value or Array
                assert(!isVtWildcard(to));
                // Destination is VT Ref, do nothing  (TODO: Can we even reach this line?)
                return SUCCESS;
            }

            return FAILURE;
        } else if (isRefDst(to)) {
            assert(!isVt(to));
            // 'to' is Non-VType reference (or array)

            if (isNonVtScalarRef(to)) {
                if (isVtValue(from))
                    return NEED_BOXING;

                // Assume any ref type can be assigned to a ref (we can't afford full inheritance checks,
                // and there's no point in checking inheritance partially)
                if (isRefDst(from))
                    return SUCCESS;

                // Assuming from != to we should report failure
                return FAILURE;
            }

            assert(isArray(to));
            if (isVt(from)) {
                // Can turn VT array reference into Long array!!!
                //if (isArray(from) && isInt64Array(to) && isSameArrayDepth(from, to))
                    //return SUCCESS;
                if (isSameArrayDepth(from, to))
                    return NEED_BOXING;

                // Can't cast any ValueType to array of different depth
                return FAILURE;
            }

            assert(isNonVtArray(to) && !isVt(from));
            // We are not tracking /java/lang/Object as a separate type, like null
            // so we are not responsible for actually checking if this scalar type can be cast to Java array
            return SUCCESS;
        }

        return FAILURE;
    }


    public static int check(int from, int to) {

        if (from == to) return isVt(to) ? HAS_VTYPE | SUCCESS : SUCCESS;

        assert(!isVtWildcard(from) && !isVtWildcard(to));   // Check for misuse
        assert(TypeId.VOID != from && TypeId.VOID != to);   // Check for misuse / 2

        return checkCastBase(from, to);
    }


    // This check adds support for conversions from/to "uninitialized" stack frame cells
    public static int checkArg(int from, int to) {

        if (from == to) {
            return isVt(to) ? (HAS_VTYPE | SUCCESS) : SUCCESS;
        }

        assert(!isVtWildcard(from));                        // stack arg can never be wildcard
        assert(TypeId.VOID != from && TypeId.VOID != to);   // Args can't have uninitialized type
        if (isVtWildcard(to)) {
            if (isVt(from)) {
                if (!isSameArrayDepth(from, to))
                    return FAILURE;

                if (isVtValue(from) && isVtValue(to))
                    return NEED_SUBSTITUTION | HAS_VTYPE;

                if (isVtRef(from) && isVtValue(to))
                    return NEED_SUBSTITUTION | NEED_UNBOXING | HAS_VTYPE;

                if (isVtArray(from) && isVtArray(to))
                    return NEED_SUBSTITUTION | HAS_VTYPE;

                assert("This code must be unreachable" == null);
                return FAILURE;
            } else {
                return NO_SUBSTITUTION;
            }
        }

        return checkCastBase(from, to);
    }

    // This check adds support for conversions from/to "uninitialized" stack frame cells
    // Only supposed to be called during stack frame conversion and with 64-bit <-> uninitialized case handled separately
    public static int checkCastFrame(int from, int to) {

        if (from == to) {
            return isVt(to) ? (HAS_VTYPE | SUCCESS) : SUCCESS;
        }

        assert(!isVtWildcard(from) && !isVtWildcard(to));   // Check for misuse

        if (TypeId.VOID == from) {
            return isVt(to) ? (HAS_VTYPE | UNINITIALIZED_VTYPE) : SUCCESS;
            //return isVt(to) ? (HAS_VTYPE | UNINITIALIZED_VTYPE) : (!isSrc64(to) ? SUCCESS : FAILURE);
        }

        if (TypeId.VOID == to) {
            return isVtValue(from) ? (HAS_VTYPE | DISCARD_VTYPE) : SUCCESS;
        }

        return checkCastBase(from, to);
    }

}
