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

public class TypeIdDefaultFormatter implements TypeIdFormatter {

    @Override
    public String typeIdToShortPrintableString(int typeId) {

        String out;
        if (isVt(typeId)) {
            out = isVtRef(typeId) ? "&VT" : "VT";
        } else {
            out = makeBasicPrintableString(typeId);
        }

        if (isArray(typeId)) {
            out = appendReadableArrayDepth(new StringBuffer(out), getArrayDepth(typeId)).toString();
        }

        return out;
    }

    @Override
    public String typeIdToSrcTypeDesc(int typeId) {
        return makeSrcDescriptor(typeId);
    }

    @Override
    public String typeIdToDstTypeDesc(int typeId) {
        return !isVt(typeId) ? null : makeDstDescriptor(typeId);
    }

    /**
     * Append user-friendly array depth to the stringbuffer
     * @param typeName
     * @param depth array depth, starting from 0
     * @return typeName argument
     */
    public static StringBuffer appendReadableArrayDepth(StringBuffer typeName, int depth) {

        depth *= 2;
        for (int i = 0; i < depth; i++) {
            typeName.append(0 == (i & 1) ? '[' : ']');
        }

        return typeName;
    }

//    public static String makeDstArrayDescriptorFromDepth(int arrayDepth) {
//
//        assert(arrayDepth > 0 && arrayDepth <= VT_ADEPTH_MASK);
//        char[] chars = new char[arrayDepth + 1];
//        Arrays.fill(chars, '[');
//        chars[arrayDepth] = 'J';
//        return new String(chars);
//    }

//    public static String makeDstArrayDescriptor(int typeId) {
//
//        assert (TypeId.isVt(typeId));
//        return makeDstArrayDescriptorFromDepth(TypeId.getArrayDepth(typeId));
//    }

    public static String makeDstDescriptor(int typeId) {

        String out;
        assert(!isVtRef(typeId));
        if (TypeId.VOID == typeId) {
            out = "V";
        } else if (isVt(typeId)) {
            out = "J";
        } else {
            out = makeBasicTypeDescriptor(TypeId.getArrayBaseElement(typeId));
        }

        if (isArray(typeId)) {
            int depth = getArrayDepth(typeId);
            out = prependArrayDepth(out, depth).toString();
        }

        return out;
    }


    public static String makeSrcDescriptor(int typeId) {

        assert(!isVt(typeId));
        String out = TypeId.VOID == typeId ? "V" : makeBasicTypeDescriptor(TypeId.getArrayBaseElement(typeId));
        if (isArray(typeId)) {
            int depth = getArrayDepth(typeId);
            out = prependArrayDepth(out, depth).toString();
        }

        return out;
    }


    public static String makeBasicPrintableString(int typeId) {

        assert(!isVt(typeId));

        if (TypeId.getTypeEnum(typeId) > E_TYPE_OBJ) {
            typeId = TypeId.getArrayBaseElement(typeId);
        }
        switch (typeId) {
            case TypeId.I32:
                return "I32";

            case TypeId.I64:
                return "I64";

            case TypeId.F32:
                return "F32";

            case TypeId.F64:
                //case F_FLOAT:   // When taken from array
                return "F64";

            case TypeId.OBJ_REF:
                return "Ref";

            case TypeId.NULL_REF:
                return "Null";

            case TypeId.VOID:
                return "*";

            default:
                return "?"; // throw?
        }
    }

    public static String makeBasicTypeDescriptor(int typeId) {

        assert(!isVt(typeId));
        switch (typeId) {
            case TypeId.I32:
                return "I";

            case TypeId.I64:
                return "J";

            case TypeId.F32:
                return "F";

            case TypeId.F64:
                //case F_FLOAT:   // When taken from array
                return "D";

            case TypeId.OBJ_REF:
                return "Ljava/lang/Object;";

            case TypeId.NULL_REF:
                return "Ljava/lang/Object;"; // ?

            case TypeId.VOID:
                return "V";

            default:
                throw new IllegalArgumentException(Integer.toHexString(typeId));
        }
    }

    public static StringBuffer prependArrayDepth(String typeName, int arrayDepth) {

        int nameLength = typeName.length();
        StringBuffer out = new StringBuffer(arrayDepth + nameLength + 4);
        for (int i = 0; i < arrayDepth; i++) {
            out.append('[');
        }

        if (1 == nameLength || typeName.charAt(0) == 'L') {
            out.append(typeName);
        } else {
            out.append('L').append(typeName).append(';');
        }

        return out;
    }
}
