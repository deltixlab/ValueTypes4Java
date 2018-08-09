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

public class Utils {

    public static boolean hasVtArgs(int[] args, int ofs, int n) {

        for (int i = 0; i < n; ++i) {
            if (TypeId.isVt(args[ofs + i]))
                return true;
        }

        return false;
    }


    public static boolean hasVtScalarArgs(int[] args, int ofs, int n) {

        for (int i = 0; i < n; ++i) {
            if (TypeId.isVtNonArray(args[ofs + i]))
                return true;
        }

        return false;
    }


    public static int compareMethodArgs(int[] argsA, int ofsA, int[] argsB, int ofsB, int n) {
        if (0 == n)
            return TypeIdCast.SUCCESS;

        int firstFailedDepth = -0x100;
        int cmpResult = 0;

        for (int i = 0; i < n; ++i) {
            int argA = argsA[ofsA + i];
            int argB = argsB[ofsB + i];
            cmpResult |= TypeIdCast.checkArg(argA, argB);
            if (TypeIdCast.isFailure(cmpResult)) {
                return cmpResult | ((ofsA + i) << 8);
            } else if (TypeIdCast.SUCCESS != cmpResult && firstFailedDepth < 0) {
                firstFailedDepth = (ofsA + i) << 8;
            }
        }

        return cmpResult | (firstFailedDepth & 0xFF);
    }
}
