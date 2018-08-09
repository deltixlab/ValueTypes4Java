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

import deltix.vtype.annotations.ValueType;

public class ValueType64 {
    long value;

    ValueType64(long value) {
        this.value = value;
        System.out.printf("VT64.new(%s) %n", value);
    }

    @ValueType(impl="identity")
    public static ValueType64 fromValue(long l) {
        return
                new ValueType64(l);
    }

    @ValueType(impl="identity")
    public long getValue() {
        return value;
    }

    @ValueType(impl="identity")
    public static long getValue(ValueType64 vt) {
        return null == vt ? ValueType64Utils.NULL : vt.value;
    }

    @Override
    public String toString() {
        return ValueType64Utils.toString(value);
    }

    @Override
    public boolean equals(Object other) {
        return ValueType64Utils.equals(value, other);
    }
}
