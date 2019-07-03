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

package deltix.dt;

import deltix.vtype.annotations.ValueType;

import java.text.ParseException;

public class DateTime {
    public static final DateTime NULL = null;
    long dt;

    private DateTime(long dt) {
        this.dt = dt;
        //System.out.printf("DT.new(%s) %n", dt);
    }

    @ValueType(impl="copyArray")
    public static DateTime[] fromLongArray(long[] src, int srcOffset, int length, DateTime[] dst, int dstOffset) {

        int srcLength = src.length;
        int srcEndOffset = srcOffset + length;

        // NOTE: no bounds checks, just a sample
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = DateTime.create(src[srcOffset + i]);
        }

        return dst;
    }

    @ValueType(impl="copyArray")
    public static long[] toLongArray(DateTime[] src, int srcOffset, int length, long[] dst, int dstOffset) {

        int srcLength = src.length;
        int srcEndOffset = srcOffset + length;

        // NOTE: no bounds checks, just a sample
        for (int i = 0; i < length; ++i) {
            dst[dstOffset + i] = DateTime.getLong(src[srcOffset + i]);
        }

        return dst;
    }

    @ValueType(impl="identity")
    public static long[] toLongArray(DateTime[] src) {
        return null == src ? null : toLongArray(src, 0, src.length, new long[src.length], 0);
    }

    @ValueType(impl="identity")
    public static DateTime[] fromLongArray(long[] src) {
        return null == src ? null : fromLongArray(src, 0, src.length, new DateTime[src.length], 0);
    }

    @ValueType(impl="identity")
    public static DateTime create(long dt) {
        return new DateTime(dt);
    }


    @ValueType(impl="fromString")
    public static DateTime create(String str) throws ParseException {
        return new DateTime(Utils.fromString(str));
    }

    @ValueType(impl="identity")
    public static long getLong(DateTime dt) {
        return null == dt ? Utils.NULL : dt.dt;
    }


    public static DateTime fromString(String str) throws ParseException {
        return create(str);
    }

    @ValueType(impl="identity")
    public long getLong() {
        return this.dt;
    }

    @ValueType(impl="equals")
    public boolean equals(DateTime other) {
        return this.dt == other.dt;
    }

    public static boolean isIdentical(DateTime dt1, Object o) {
        return (o == dt1) || (o instanceof DateTime && dt1.dt == ((DateTime)o).dt);
    }

    public boolean equals(Object other) {
        return this == other || Utils.equals(dt, other);
    }

    static public boolean equals(DateTime a, Object b) {
        return a == b || null != a && a.equals(b);
    }

    public static DateTime now() {
        return new DateTime(Utils.now());
    }

    public DateTime addNanos(long nanos) {
        return create(Utils.addNanos(dt, nanos));
    }

    public DateTime addDays(long days) {
        return create(Utils.addDays(dt, days));
    }

    @Override
    public String toString() {
        return Utils.toString(dt);
    }

    @ValueType(impl="avgRenamed")
    public DateTime avg(DateTime b) {
        return create(Utils.avgRenamed(dt, b.dt));
    }

}