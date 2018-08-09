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

import java.util.Arrays;
import java.util.Objects;

public class Warnings {
    static final int REF_CMP            = 0;
    static final int VALUE_CMP          = 1;
    static final int UNINIT_TRANSFORM   = 2; // This is also related to "frame transform" problems below
    static final int REF_RETURN         = 3;
    static final int REF_ASSIGN         = 4;
    static final int REF_ARGS           = 5;
    static final int REF_CAST           = 6;
    static final int BOXING_UNKNOWN     = 7;
    static final int UNBOXING_ASSIGN    = 8;
    static final int UNBOXING_UNKNOWN   = 9;
    static final int ANEWARRAY          = 10;
    static final int ANEWARRAYFILL      = 11;
    static final int ANEWMULTIARRAY     = 12;
    static final int ANEWMULTIARRAYFILL = 13;
    static final int DIFFERENT_TYPES_CMP = 14;
    static final int INDY_VT            = 15;
    static final int LAMBDA_VT          = 16;
    static final int FRAME_TRANSFORM    = 17;
    static final int FRAME_UNBOXING     = 18;
    static final int FRAME_BOXING       = 19;
    static final int UNKNOWN            = 20;

    static final int WARNINGS_COUNT;

    // NOTE: TODO: Some of these warnings could become outdated/redundant/misleading. May need cleanup.

    static final String[] descs = {
            "==/!= used with Value Types, ref. comparison used",
            "==/!= used with Value Types, value comparison used",
            "extra code transformation due to using uninitialized Value Type local variables",
            "returning Value Type as reference",
            "boxing Value Type via assignment",
            "boxing Value Type method argument",
            "boxing Value Type via type casting",
            "boxing Value Type by unknown cause",
            "unboxing Value Type via assignment",
            "unboxing Value Type by unknown cause",
            "new ValueType[] won't be initialized with ValueType.NULL value",
            "new ValueType[] is filled with Array.fill()",
            "new ValueType[]..[] won't be initialized with ValueType.NULL value",
            "new ValueType[]..[] is filled with ???",
            "==/!= used to compare DIFFERENT Value Types, returning false",
            "transforming unknown INVOKEDYNAMIC that references a Value Type. Probably will fail.",
            "transforming a lambda that references a Value Type",
            "Stack Frame synchronization occurred",
            "unboxing Value Type on Stack Frame synchronization",
            "boxing Value Type on Stack Frame synchronization",
            "something suspicious is going on"
    };

    static final String[] names = {
            "refCompare",
            "refValueCompare",
            "uninitialized",
            "refReturn",
            "refAssign",
            "refArgs",
            "refCast",
            "boxingByUnknown",
            "unboxingAssign",
            "unboxingByUnknown",
            "newArrayOld",      // Deprecated
            "newArray",
            "newMultiArray",
            "newMultiArrayNew", // Not yet used
            "refCmpDiffType",
            "indyVt",
            "lambda",
            "frameSync",
            "frameSyncUnboxing",
            "frameSyncBoxing",
            "genericWarning"
    };

    static final String[] hints = {
            "Avoid using reference comparison operators with Value Types",
            "Avoid using reference comparison operators with Value Types",
            "Initialize ValueType variables in declaration with constant of appropriate type",
            "Change return type of the method",
            "Avoid implicit casts of Value Types to Object",
            "Change argument types, avoid implicit casts of Value Types to Object args",
            "Do not cast Value Types to Object",
            "Do something",
            "Not a problem, ignore",
            "Not a problem, ignore",
            "don't forget to initialize array members",
            "performance degradation possible, ignore",
            "don't forget to initialize array members",
            "performance degradation possible, ignore",
            "Comparing obviously different types is probably an error, fix",
            "do not use Value Types with unrecognized modern Java features",
            "Be careful when using Value Types in lambdas, support is still experimental",
            "initialize Value Types with ValueType constants, do not store ValueTypes to Objects",
            "initialize Value Types with ValueType constants, do not store ValueTypes to Objects",
            "initialize Value Types with ValueType constants, do not store ValueTypes to Objects",
            "Do something or complain to the developer",
    };



    static {
        WARNINGS_COUNT = descs.length;
    }

    private long ignoreMask;

    int numAllocated, numTotal;
    final int[] n        = new int[WARNINGS_COUNT];
    final int[] first    = new int[WARNINGS_COUNT];
    final int[] last     = new int[WARNINGS_COUNT];
    //final boolean[] suppressed = new boolean[WARNINGS_COUNT];
    int[] next;
    int[] lines;
    Object[] data;

    static {
        assert(descs.length == names.length);
        assert(names.length == hints.length);
    }


    public Warnings() {
        this.numAllocated   = 16;
        this.next           = new int[numAllocated];
        this.lines          = new int[numAllocated];
        this.data           = new Object[numAllocated];
        this.numTotal       = 0;
        this.ignoreMask     = 0;
        assert (WARNINGS_COUNT <= Long.SIZE);
    }


    private void realloc(int numReserve) {

        if (0 == numAllocated) {
            numAllocated = 16;
            next = new int[numAllocated];
            lines = new int[numAllocated];
            data = new Object[numAllocated];
        } else {
            numAllocated *= 2;
            next = Arrays.copyOf(next, numAllocated);
            lines = Arrays.copyOf(lines, numAllocated);
            data = Arrays.copyOf(data, numAllocated);
        }
    }


    private void checkRealloc(int numReserve) {

        if (numReserve > numAllocated) {
            realloc(numReserve);
        }
    }


    public void clear() {

        Arrays.fill(data, 0, numTotal, null);
        numTotal = 0;
        Arrays.fill(n, 0);
        Arrays.fill(first, 0);
        Arrays.fill(last, 0);
    }


    public int numTotal() {
        return numTotal;
    }


    public int numOf(int id) {
        return n[id];
    }

    void add(int id, int line, Object data) {

        int prev = last[id];
        // Ignore repeating warnings
        if (isSupressed(id) || 0 != prev && this.lines[prev] == line && Objects.equals(this.data[prev], data))
            return;

        int i = ++numTotal;
        checkRealloc(i + 1); // 1st entry is guard entry
        this.lines[i] = line;
        this.data[i] = data;

        if (0 == prev) {
            assert(0 == first[id]);
            first[id] = i;
        } else {
            next[prev] = i;
        }

        last[id] = i;
        ++n[id];
    }


    public void suppress(String name) {
        for (int i = 0; i < WARNINGS_COUNT; i++) {
            if (Objects.equals(name, names[i])) {
                ignoreMask |= 1L << i;
                break;
            }
        }
    }


    public boolean isSupressed(int id) {
        return 0 != (ignoreMask & (1L << id));
    }


    public long getIgnoreMask() {
        return ignoreMask;
    }


    public void setIgnoreMask(long ignoreMask) {
        this.ignoreMask = ignoreMask;
    }
}
