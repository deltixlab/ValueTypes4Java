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

import static java.lang.Integer.parseInt;

public class Util {

    public static int parseType(String item) {

        int type;
        int iLastChar = item.length() - 1;
        String item2;

        if (!Character.isDigit(item.charAt(iLastChar))) {
            type = item.endsWith("J") ? TypeId.I64
                    : item.endsWith("X") ? TypeId.VT64
                    : item.endsWith("F") ? TypeId.F32
                    : item.endsWith("D") ? TypeId.F64
                    : item.endsWith("I") ? TypeId.I32
                    : TypeId.VOID;

            item2 = item.substring(0, iLastChar); // Remove last char
        } else {
            type = TypeId.I32;
            item2 = item;
        }

        if (TypeId.VOID == type) {
            throw new IllegalArgumentException(item);
        }

        assert(TypeId.VOID != type);
        type |= parseInt(item2) << TypeId.VT_ID_POS;
        return type;
    }


    public static String[] parseItemList(String str) {

        String[] items = str.split("\\s+");
        if (items.length > 0 && items[0].equals("")) items = new String[0];
        return items;
    }

    public static int t32id(int i) {
        return TypeId.I32 + (i << TypeId.VT_ID_POS);
    }

    public static int t64id(int i) {
        return TypeId.I64 + (i << TypeId.VT_ID_POS);
    }


    static String type2Str(int type) {
        return String.valueOf((type >> TypeId.VT_ID_POS) & 0xFF) + (
                TypeId.isSrc64(type) ? (TypeId.F64 == type ? "D" : "J")
                : TypeId.isDst64(type) ? "X" : "");
    }

    public static String nameFor(int type) {
        return type2Str(type) + "-Name";
    }

    static String toStr(int[] types, int failDepth) {
        StringBuffer sb = new StringBuffer("{");
        for (int i = types.length - 1; i >=0; --i) {
            int type = types[i];

            sb  .append(i == failDepth ? "<":"")
                    .append(type2Str(type))
                    .append(i == failDepth ? ">":"")
                    .append(i > 0 ? " " : "");
        }

        return sb.append('}').toString();
    }


    public static String toStr(JvmStack vm, int failDepth) {
        return toStr(vm.ids(), failDepth);
    }


    static String toStr(VariableMap vars, int failDepth) {
        TypeArray t = vars.copyAll();
        return toStr(t.types, failDepth);
    }

    public static String toStr(JvmStack vm) {
        return toStr(vm, -1);
    }

    public static String toStr(VariableMap vm) {
        return toStr(vm, -1);
    }
}
