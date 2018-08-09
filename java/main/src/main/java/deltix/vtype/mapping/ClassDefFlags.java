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

public class ClassDefFlags {
    public final static int F_SRC_CLASS_PROCESSED       = 0x0001;
    public final static int F_DST_CLASS_PROCESSED       = 0x0100;

    final static int F_SRC_BOX_METHOD_DEFINED           = 0x0002;
    final static int F_DST_BOX_METHOD_DEFINED           = 0x0200;

    final static int F_SRC_UNBOX_METHOD_DEFINED         = 0x0004;
    final static int F_DST_UNBOX_METHOD_DEFINED         = 0x0400;

    public final static int F_SRC_ALL_METHODS_DEFINED   = 0x0008;
    final static int F_DST_ALL_METHODS_DEFINED          = 0x0800;

    final static int F_NULL_CONSTANT_DEFINED            = 0x1010;

    final static int F_ALL_INITIALIZED                  = 0x1F1F;

    final static int F_ALL_MAPPING_FLAGS                = 0x1F1F;

    public static boolean allInitialized(int flags) {
        return allSet(flags, F_ALL_MAPPING_FLAGS);
    }

    public static boolean allMethodsMapped(int flags) {
        return allSet(flags, F_ALL_MAPPING_FLAGS ^ F_NULL_CONSTANT_DEFINED);
    }

    public static boolean allSet(int flags, int mask) {
        return mask == (flags & mask);
    }
}
