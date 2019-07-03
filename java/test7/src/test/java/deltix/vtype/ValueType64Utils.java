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

package deltix.vtype;

public class ValueType64Utils {

    public static final long NULL=Long.MIN_VALUE;

    // Test class. Only works properly for positive numbers!
    public static String toString(long value) {
        //return String.valueOf(value >> 32) + "." + String.valueOf(value & 0xFFFFFFFFL);
        return String.valueOf((double)value / (double)(1L << 32));
    }

    public static boolean equals(long a, long b) {
        return a == b;
    }

    public static boolean equals(long a, Object b) {
        return null == b ? (NULL == a ? true : false)
                : b instanceof ValueType64 ? equals(a, ((ValueType64)b).value) : false;
    }

    public static boolean isNull(long x) {
        return NULL == x;
    }

    public static long identity(long x) {
        return x;
    }
}
