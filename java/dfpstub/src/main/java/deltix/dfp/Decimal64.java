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
 * <p>
 * This class is immutable.
 * Can be instantiated only through static constructors.
 */
public class Decimal64 extends Number implements Comparable<Decimal64> {
    /// region Constants

    // Use NULL or NaN to initialize Decimal64 variables, improving compatibility with Value Type Agent
    public static final Decimal64 NULL = null;
    public static final Decimal64 NaN = new Decimal64(Decimal64Utils.NaN);

    public static final Decimal64 POSITIVE_INFINITY = new Decimal64(Decimal64Utils.POSITIVE_INFINITY);
    public static final Decimal64 NEGATIVE_INFINITY = new Decimal64(Decimal64Utils.NEGATIVE_INFINITY);

    public static final Decimal64 ZERO = new Decimal64(Decimal64Utils.ZERO);

    public static final Decimal64 ONE = new Decimal64(Decimal64Utils.ONE);
    public static final Decimal64 TWO = new Decimal64(Decimal64Utils.TWO);
    public static final Decimal64 TEN = new Decimal64(Decimal64Utils.TEN);
    public static final Decimal64 HUNDRED = new Decimal64(Decimal64Utils.HUNDRED);
    public static final Decimal64 THOUSAND = new Decimal64(Decimal64Utils.THOUSAND);
    public static final Decimal64 MILLION = new Decimal64(Decimal64Utils.MILLION);

    public static final Decimal64 ONE_TENTH = new Decimal64(Decimal64Utils.ONE_TENTH);
    public static final Decimal64 ONE_HUNDREDTH = new Decimal64(Decimal64Utils.ONE_HUNDREDTH);

    /// endregion

    /// region Object Implementation

    final long value;

    private Decimal64(long value) {
        this.value = value;
    }

    /**
     * Return true if this decimal and given decimal represents the same arithmetic value.
     *
     * We consider that all POSITIVE_INFINITYs is equal to another POSITIVE_INFINITY,
     * all NEGATIVE_INFINITYs is equal to another NEGATIVE_INFINITY,
     * all NaNs is equal to another NaN.
     *
     * @param other value to compare
     * @return True if two decimals represents the same arithmetic value.
     */
    public boolean equals(Decimal64 other) {
        return this == other || other != null && Decimal64Utils.equals(this.value, other.value);
    }

    @Override
    /**
     * Return true if this decimal and given decimal represents the same arithmetic value.
     *
     * We consider that all POSITIVE_INFINITYs is equal to another POSITIVE_INFINITY,
     * all NEGATIVE_INFINITYs is equal to another NEGATIVE_INFINITY,
     * all NaNs is equal to another NaN.
     *
     * @param other value to compare
     * @return True if two decimals represents the same arithmetic value.
     */
    public boolean equals(Object other) {
        return other != null && other instanceof Decimal64 && Decimal64Utils.equals(value, ((Decimal64) other).value);
    }


    /**
     * Compares this instance with another one.
     * <p>
     * This method returns {@code true} if and only if {@param obj} is not of type {@see Decimal64} and their
     * underlying values match. This means that {@code Decimal64.NaN.equals(Decimal64.NaN)} evaluates to
     * {@code true}, while on the same time two different representation of real values might be not equal according
     * to this method. E.g. various representation of 0 are not considered the same.
     *
     * @param other Other instance to compareTo to.
     * @return {@code true} if this instance equals the {@param obj}; otherwise - {@code false}.
     */
    public boolean isIdentical(Decimal64 other) {
        return this == other || other != null && value == other.value;
    }

    /**
     * Compares this instance with another one.
     * <p>
     * This method returns {@code true} if and only if {@param other} is not of type {@see Decimal64} and their
     * underlying values match. This means that {@code Decimal64.NaN.equals(Decimal64.NaN)} evaluates to
     * {@code true}, while on the same time two different representation of real values might be not equal according
     * to this method. E.g. various representation of 0 are not considered the same.
     *
     * @param other Other instance to compareTo to.
     * @return {@code true} if this instance equals the {@param other}; otherwise - {@code false}.
     */
    public boolean isIdentical(Object other) {
        return other != null && other instanceof Decimal64 && value == ((Decimal64) other).value;
    }

    /**
     * Return true if two decimals represents the same arithmetic value.
     * <p>
     * We consider that all POSITIVE_INFINITYs is equal to another POSITIVE_INFINITY,
     * all NEGATIVE_INFINITYs is equal to another NEGATIVE_INFINITY,
     * all NaNs is equal to another NaN.
     *
     * @param a First argument
     * @param b Second argument
     * @return True if two decimals represents the same arithmetic value.
     */
    public static boolean equals(Decimal64 a, Decimal64 b) {
        return a == b || a != null && b != null && Decimal64Utils.equals(a.value, b.value);
    }

    /**
     * Compares two instances of {@see Decimal64}
     * <p>
     * This method returns {@code true} if and only if the underlying values of both objects match. This means that
     * {@code Decimal64.NaN.equals(Decimal64.NaN)} evaluates to {@code true}, while on the same time two different
     * representation of real values might be not equal according to this method. E.g. various representation of 0 are
     * not considered the same.
     *
     * @param a First value to compareTo.
     * @param b Second value to compareTo.
     * @return {@code true} if this instance equals the {@param obj}; otherwise - {@code false}.
     */
    public static boolean isIdentical(Decimal64 a, Decimal64 b) {
        return a == b || a != null && b != null && a.value == b.value;
    }

    /**
     * Return true if two decimals represents the same arithmetic value.
     * <p>
     * We consider that all POSITIVE_INFINITYs is equal to another POSITIVE_INFINITY,
     * all NEGATIVE_INFINITYs is equal to another NEGATIVE_INFINITY,
     * all NaNs is equal to another NaN.
     *
     * @param a First argument
     * @param b Second argument
     * @return True if two decimals represents the same arithmetic value.
     */
    public static boolean equals(Decimal64 a, Object b) {
        return a == b || a != null && b instanceof Decimal64 && Decimal64Utils.equals(a.value, ((Decimal64) b).value);
    }

    /**
     * Compares an instance of {@see Decimal64} with an object.
     * <p>
     * This method returns {@code true} if and only if {@param b} is of type {@see Decimal64} and the underlying values
     * of both objects match. This means that {@code Decimal64.NaN.equals(Decimal64.NaN)} evaluates to {@code true},
     * while on the same time two different representation of real values might be not equal according to this method.
     * E.g. various representation of 0 are not considered the same.
     *
     * @param a First value to compareTo.
     * @param b Second value to compareTo.
     * @return {@code true} if this instance equals the {@param obj}; otherwise - {@code false}.
     */

    public static boolean isIdentical(Decimal64 a, Object b) {
        return a == b || a != null && b instanceof Decimal64 && a.value == ((Decimal64) b).value;
    }

    /**
     * Hash code of binary representation of given decimal.
     *
     * @return HashCode of given decimal.
     */
    public int identityHashCode() {
        return Decimal64Utils.identityHashCode(value);
    }

    @Override
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
    public int hashCode() {
        return Decimal64Utils.hashCode(value);
    }

    @Override
    public String toString() {
        return Decimal64Utils.toString(value);
    }

    /// endregion

    /// Number Implementation

    @Override
    public int intValue() {
        return (int) toLong();
    }

    @Override
    public long longValue() {
        return toLong();
    }

    @Override
    public float floatValue() {
        return (float) toDouble();
    }

    @Override
    public double doubleValue() {
        return toDouble();
    }

    /// endregion

    /// Comparable<T> Implementation

    @Override
    public int compareTo(final Decimal64 o) {
        return Decimal64Utils.compareTo(value, o.value);
    }

    // endregion

    /// region Conversion

    public static Decimal64 fromUnderlying(long value) {
        return Decimal64Utils.NULL == value ? null : new Decimal64(value);
    }

    public static long toUnderlying(Decimal64 obj) {
        return null == obj ? Decimal64Utils.NULL : obj.value;
    }



    public static Decimal64 fromDouble(double d) {
        return new Decimal64(Decimal64Utils.fromDouble(d));
    }

    public double toDouble() {
        return Decimal64Utils.toDouble(value);
    }

//    public Decimal64 canonize() {
//        return new Decimal64(Decimal64Utils.canonize(value));
//    }

    public static Decimal64 fromLong(long l) {
        return new Decimal64(Decimal64Utils.fromLong(l));
    }

    public long toLong() {
        return Decimal64Utils.toLong(value);
    }


    /// endregion

    /// region Classification

    public boolean isNaN() {
        return Decimal64Utils.isNaN(value);
    }

    public boolean isInfinity() {
        return Decimal64Utils.isInfinity(value);
    }

    public boolean isPositiveInfinity() {
        return Decimal64Utils.isPositiveInfinity(value);
    }

    public boolean isNegativeInfinity() {
        return Decimal64Utils.isNegativeInfinity(value);
    }

    public boolean isFinite() {
        return Decimal64Utils.isFinite(value);
    }


    /// endregion

    /// region Comparison

    public boolean isZero() {
        return Decimal64Utils.isZero(value);
    }

    public boolean isEqual(Decimal64 other) {
        return Decimal64Utils.isEqual(value, other.value);
    }

    public boolean isNotEqual(Decimal64 other) {
        return Decimal64Utils.isNotEqual(value, other.value);
    }

    /// endregion


    /// endregion

    /// region Arithmetic

    public Decimal64 negate() {
        return new Decimal64(Decimal64Utils.negate(value));
    }

    public Decimal64 add(Decimal64 other) {
        return new Decimal64(Decimal64Utils.add(value, other.value));
    }

    public Decimal64 subtract(Decimal64 other) {
        return new Decimal64(Decimal64Utils.subtract(value, other.value));
    }

    public Decimal64 divide(Decimal64 other) {
        return new Decimal64(Decimal64Utils.divide(value, other.value));
    }

    public Decimal64 abs() {
        return new Decimal64(Decimal64Utils.abs(value));
    }

    /// endregion

    public static String toString(final Decimal64 decimal64) {
        return Decimal64Utils.toString(decimal64.value);
    }

    public static Decimal64 parse(String s) {
        return new Decimal64(Decimal64Utils.parse(s));
    }
}
