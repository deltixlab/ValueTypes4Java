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

package deltix.dfp;

/**
 * Stub for Deltix Decimal Floating Poing library.
 * Contents of this class in no way represent the actual dfp library and are present only to stop tests from failing.
 */
public class Decimal64Utils {
    /// region Constants

    public static final long NULL = Long.MIN_VALUE;

    public static final long NaN = NULL - 1;

    public static final long POSITIVE_INFINITY = NULL - 2;

    public static final long NEGATIVE_INFINITY = NULL - 3;

    public static final long ZERO = 0;

    public static final long ONE = fromLong(1);
    public static final long TWO = fromLong(2);
    public static final long TEN = fromLong(10);
    public static final long HUNDRED = fromLong(100);
    public static final long THOUSAND = fromLong(1000);
    public static final long MILLION = fromLong(1);

    public static final long ONE_TENTH = make(0, 10000000);
    public static final long ONE_HUNDREDTH = make(0, 1000000);


    /// endregion

    /// region Object Implementation

    /**
     * Hash code of binary representation of given decimal.
     *
     * @param value Given decimal.
     * @return HashCode of given decimal.
     */
    public static int identityHashCode(final long value) {
        return (int) (value ^ (value >>> 32));
    }

    /**
     * Return hash code of arithmetic value of given decimal.
     *
     * We consider that all POSITIVE_INFINITYs have equal hashCode,
     * all NEGATIVE_INFINITYs have equal hashCode,
     * all NaNs have equal hashCode.
     *
     * @param value Given decimal.
     * @return HashCode of given decimal.
     */
    public static int hashCode(final long value) {
        return (int) (value ^ (value >>> 32));
    }


    public static String toString(final long value) {
        return String.valueOf(toDouble(value));
    }

    /**
     * Return true if two decimals represents the same arithmetic value.
     *
     * We consider that all POSITIVE_INFINITYs is equal to another POSITIVE_INFINITY,
     * all NEGATIVE_INFINITYs is equal to another NEGATIVE_INFINITY,
     * all NaNs is equal to another NaN.
     *
     * @param a First argument
     * @param b Second argument
     * @return True if two decimals represents the same arithmetic value.
     */
    public static boolean equals(final long a, final long b) {
        long canonizedA = canonize(a);
        long canonizedB = canonize(b);
        return canonizedA == canonizedB;
    }

    /**
     * Return true if two decimals have the same binary representation.
     *
     * @param a First argument.
     * @param b Second argument.
     * @return True if two decimals have the same binary representation.
     */
    public static boolean isIdentical(final long a, final long b) {
        return a == b;
    }

    public static boolean isIdentical(final long a, final Object b) {
        return (b == null && NULL == a) || (b instanceof Decimal64 && (a == ((Decimal64) b).value));
    }

    public static boolean equals(final long a, final Object b) {
        return (b == null && NULL == a)
            || (b instanceof Decimal64 && equals(a, ((Decimal64) b).value));
    }

    public static boolean isNull(final long value) {
        return NULL == value;
    }

    /// endregion

    /// region Conversion & Rounding

    /**
     * Returns canonical representation of Decimal.
     * We consider that all binary representations of one arithmetic value have the same canonical binary representation.
     * Canonical representation of zeros = {@link #ZERO ZERO}
     * Canonical representation of NaNs = {@link #NaN NaN}
     * Canonical representation of POSITIVE_INFINITYs = {@link #POSITIVE_INFINITY POSITIVE_INFINITY}
     * Canonical representation of NEGATIVE_INFINITYs = {@link #NEGATIVE_INFINITY NEGATIVE_INFINITY}
     *
     * @param value Decimal argument.
     * @return Canonical representation of decimal argument.
     */
    public static long canonize(final long value) {
        if (Decimal64Utils.isNaN(value)) {
            return NaN;
        }
        if (Decimal64Utils.isInfinity(value)) {
            if (Decimal64Utils.isPositiveInfinity(value)) {
                return POSITIVE_INFINITY;
            } else {
                return NEGATIVE_INFINITY;
            }
        }
        return value;
    }

    public static long fromUnderlying(final long value) {
        return value;
    }

    public static long toUnderlying(final long value) {
        return value;
    }


    public static long fromDouble(final double value) {
        return make((long)value, (long)((value - (long)value) * 1E8));
    }

    private static long make(long intPart, long fracPart) {
        return (intPart << 32) | fracPart & 0xFFFFFFFFFL;
    }

    public static double toDouble(final long value) {
        return (double)(int)(value >>> 32) + 1E-8 * (int)value;
    }

    public static long fromLong(final long value) {
        return make(value, 0);
    }

    public static long toLong(final long value) {
        return value >>> 32;
    }



    /// endregion

    /// region Classification

    public static boolean isNaN(final long value) {
        return NaN == value;
    }

    public static boolean isInfinity(final long value) {
        return POSITIVE_INFINITY == value || NEGATIVE_INFINITY == value;
    }

    public static boolean isPositiveInfinity(final long value) {
        return POSITIVE_INFINITY == value;
    }

    public static boolean isNegativeInfinity(final long value) {
        return NEGATIVE_INFINITY == value;
    }

    public static boolean isFinite(final long value) {
        return !isInfinity(value) && !isNaN(value);
    }



    /// endregion

    /// region Comparison

    public static int compareTo(final long a, final long b) {
        return Double.compare(toDouble(a), toDouble(b));
    }

    public static boolean isEqual(final long a, final long b) {
        return a == b;
    }

    public static boolean isNotEqual(final long a, final long b) {
        return a != b;
    }


    public static boolean isZero(final long value) {
        return ZERO == value;
    }

    /// endregion

    /// region Minimum & Maximum


    /// endregion

    /// region Arithmetic

    public static long negate(final long value) {
        return fromDouble(-toDouble(value));
    }

    public static long abs(final long value) {
        return fromDouble(Math.abs(toDouble(value)));
    }

    public static long add(final long a, final long b) {
        return fromDouble(toDouble(a) + toDouble(b));
    }


    public static long subtract(final long a, final long b) {
        return fromDouble(toDouble(a) - toDouble(b));
    }

    public static long divide(final long a, final long b) {
        return fromDouble(toDouble(a) / toDouble(b));
    }



    /// endregion

    /// region Null-checking wrappers for non-static methods

    static protected void checkNull(final long value) {
        if (isNull(value)) {
            throw new NullPointerException();
        }
    }

    static protected void checkNull(final long a, final long b) {
        if (isNull(a) || isNull(b)) {
            throw new NullPointerException();
        }
    }


    public static double toDoubleChecked(final long value) {
        checkNull(value);
        return toDouble(value);
    }

    public static long fromLongChecked(final long value) {
        checkNull(value);
        return fromLong(value);
    }

    public static long toLongChecked(final long value) {
        checkNull(value);
        return toLong(value);
    }

    public static long negateChecked(final long value) {
        checkNull(value);
        return negate(value);
    }

    public static long absChecked(final long value) {
        checkNull(value);
        return abs(value);
    }


    public static boolean isNaNChecked(final long value) {
        checkNull(value);
        return isNaN(value);
    }

    public static boolean isInfinityChecked(final long value) {
        checkNull(value);
        return isInfinity(value);
    }

    public static boolean isPositiveInfinityChecked(final long value) {
        checkNull(value);
        return isPositiveInfinity(value);
    }

    public static boolean isNegativeInfinityChecked(final long value) {
        checkNull(value);
        return isNegativeInfinity(value);
    }

    public static boolean isFiniteChecked(final long value) {
        checkNull(value);
        return isFinite(value);
    }


    public static boolean isZeroChecked(final long value) {
        checkNull(value);
        return isZero(value);
    }

    public static boolean isEqualChecked(final long a, final long b) {
        checkNull(a, b);
        return isEqual(a, b);
    }

    public static boolean isNotEqualChecked(final long a, final long b) {
        checkNull(a, b);
        return isNotEqual(a, b);
    }


    public static long addChecked(final long a, final long b) {
        checkNull(a, b);
        return add(a, b);
    }


    public static long subtractChecked(final long a, final long b) {
        checkNull(a, b);
        return subtract(a, b);
    }


    public static long divideChecked(final long a, final long b) {
        checkNull(a, b);
        return divide(a, b);
    }


    public static int identityHashCodeChecked(final long value) {
        checkNull(value);
        return identityHashCode(value);
    }


    public static int hashCodeChecked(final long value) {
        checkNull(value);
        return hashCode(value);
    }

    public static String toStringChecked(final long value) {
        checkNull(value);
        return toString(value);
    }

    public static boolean equalsChecked(final long a, final long b) {
        checkNull(a, b);
        return equals(a, b);
    }

    public static boolean equalsChecked(final long a, Object b) {
        checkNull(a);
        return equals(a, ((Decimal64)b).value);
    }

    public static boolean isIdenticalChecked(final long a, final long b) {
        checkNull(a, b);
        return a == b;
    }

    public static boolean isIdenticalChecked(final long value, Object obj) {
        checkNull(value);
        return obj instanceof Decimal64 && value == ((Decimal64)obj).value;
    }


    public static int intValueChecked(final long value) {
        checkNull(value);
        return (int) toLong(value);
    }

    public static long longValueChecked(final long value) {
        checkNull(value);
        return toLong(value);
    }

    public static float floatValueChecked(final long value) {
        checkNull(value);
        return (float) toDouble(value);
    }

    public static double doubleValueChecked(final long value) {
        checkNull(value);
        return toDouble(value);
    }

    public static int compareToChecked(final long a, final long b) {
        checkNull(a, b);
        return compareTo(a, b);
    }

    // Required by Comparable<>
    public static int compareToChecked(final long a, Object b) {
        checkNull(a);
        return compareTo(a, ((Decimal64)b).value);
    }

    /// endregion

    /// region Array boxing/unboxing (array conversions from long[] / to long[])

    public static Decimal64[] fromUnderlyingLongArray(long[] src, int srcOffset, Decimal64[] dst, int dstOffset, int length) {

        // NOTE: no bounds checks
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = Decimal64.fromUnderlying(src[srcOffset + i]);
        }

        return dst;
    }


    public static long[] toUnderlyingLongArray(Decimal64[] src, int srcOffset, long[] dst, int dstOffset, int length) {

        // NOTE: no bounds checks
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = Decimal64.toUnderlying(src[srcOffset + i]);
        }

        return dst;
    }

    public static long[] toUnderlyingLongArray(Decimal64[] src) {
        return null == src ? null : toUnderlyingLongArray(src, 0, new long[src.length], 0, src.length);
    }

    public static Decimal64[] fromUnderlyingLongArray(long[] src) {
        return null == src ? null : fromUnderlyingLongArray(src, 0, new Decimal64[src.length], 0, src.length);
    }

    public static long parse(String s) {
        return fromDouble(Double.valueOf(s));
    }

    /// endregion
}
