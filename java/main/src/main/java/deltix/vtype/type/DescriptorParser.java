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

import deltix.vtype.mapping.Mapping;
import deltix.vtype.common.CrudeLogger;

public class DescriptorParser {
    final static CrudeLogger log = new CrudeLogger(CrudeLogger.DBG);

    public void setLogLevel(int level) {
        this.log.setLogLevel(level);
    }


    protected static int getTypeId(final Mapping mapping, final String classPath, final int arrayDepth) {
        return 0 == arrayDepth ? mapping.getClassTypeId(classPath)
                : mapping.getClassArrayTypeId(classPath, arrayDepth);
    }

    protected static int getWildcardTypeId() {
        return TypeId.vtValueFromIndex(TypeId.VT_WILDCARD_INDEX);
    }


    // TODO: OPT: hash substring, return by range
//    protected static int getTypeId(final Mapping mapping, final String classPath, final int arrayDepth) {
//        return null == mapping ? TypeId.refOrArrayFrom(arrayDepth)
//                : 0 == arrayDepth ? mapping.getClassTypeId(classPath)
//                : mapping.getClassArrayTypeId(classPath, arrayDepth);
//    }

    public static boolean isField64(String descr) {
        if (1 == descr.length()) {
            switch (descr.charAt(0)) {
                case 'J':
                case 'D':
                    return true;
            }
        }

        return false;
    }


    public static int getClassTypeId(final String classPath, final Mapping mapping) {
        return classPath.charAt(0) == '['  ? getDescTypeId(classPath, mapping) : mapping.getClassTypeId(classPath);
    }

    public static int getDescTypeId(final String desc, final Mapping mapping) {

        long tmp = parseType(desc, 0, mapping);
        int argType = (int)tmp;
        if (-1 == argType) {
            log.err("Unable to parse type signature: %s at %d", desc, (int)(tmp >>> 32));
        }

        return argType;
    }


    /**
     *
     * @param desc method descriptor
     * @param mapping defines class mapping
     * @return true, if a type or array of types from 'mapping' is found in the whole method signature
     */
    public static boolean isVt(String desc, int i, final Mapping mapping) {

        assert(null != mapping);
        char ch;
        for (; '[' == (ch = desc.charAt(i)); ++i) ;
        switch (ch) {
            case 'L': {
                // TODO: Optimization possible here (substring -> range)
                int end = desc.indexOf(';', i + 1);
                if (end > 0)
                    desc = desc.substring(i, end);
                }
                break;

            case 'Z': case 'B': case 'C': case 'S': case 'I': case 'F':
            case 'J': case 'D':
            case 'V':
                if (i + 1 == desc.length())
                    return false;
        }

        return TypeId.isVt(mapping.getClassTypeId(desc));
    }

    public static boolean isVt(String desc, Mapping mapping) {
        return isVt(desc, 0, mapping);
    }

    /**
     *
     * @param desc method descriptor
     * @param mapping defines class mapping
     * @return true, if a type or array of types from 'mapping' is found in the whole method signature
     */
    public static boolean findVtInMethodDesc(final String desc, final Mapping mapping) {

        int i = 0;
        boolean parsingRv = false;
        assert(null != mapping);

        if(desc.charAt(0) == '(') {
            for (i = 1;; ++i) {
                char ch = desc.charAt(i);

                if (')' == ch) {
                    parsingRv = true;
                    ch = desc.charAt(++i);
                }

                while ('[' == ch) {
                    ch = desc.charAt(++i);
                }

                switch (ch) {
                    case 'L': {
                        int iend = desc.indexOf(';', ++i);
                        if (TypeId.isVt(mapping.getClassTypeId(desc.substring(i, iend))))
                            return true;

                        i = iend;
                        break;
                    }

                    case 'Z': case 'B': case 'C': case 'S': case 'I': case 'F':
                    case 'J': case 'D':
                    case 'V':
                        break;

                    default:
                        log.err("Unable to parse method signature: %s at %d", desc, i);
                }

                if (parsingRv)
                    return false;
            }
        } else {
            log.err("Not a method signature: %s", desc);
        }

        return false;
    }


    /**
     *
     * @param desc method descriptor
     * @return true, if the method takes long or long array
     */
    public static boolean isPossibleTransformedVtSetter(final String desc) {

        int i = 0, n = desc.length();

        if(n < 4 || desc.charAt(i++) != '(')
            return false;

        for (; i < n && '[' == desc.charAt(i); ++i);
        if (i > n - 3 || desc.charAt(i) != 'J' || desc.charAt(i + 1) != ')')
            return false;

        return -1 == desc.indexOf(')', i + 2);
    }


    public static String getTransformedDesc(final String desc, boolean isNonStaticVTypeMethod, final Mapping mapping) {

        StringBuffer newDesc = new StringBuffer(32);
        int i = 0, n = desc.length();

        if (desc.charAt(0) != '(') {
            log.err("Not a method signature: %s", desc);
            return null;
        }

        newDesc.append('(');
        if (isNonStaticVTypeMethod) {
            newDesc.append('J');
        }

        for (i = 1; i < n; ++i) {
            char ch = desc.charAt(i);
            switch (ch) {
                case 'L': {
                    int iend = desc.indexOf(';', i + 1);
//                   // Do not add ++i!
                    int typeId = mapping.getClassTypeId(desc.substring(i + 1, iend));
                    //newDesc.append('L').append(mapping.getClassDefById(typeId).getDstClassPath()).append(";");

                    if (TypeId.isVt(typeId)) {
                        newDesc.append('J');
                    } else {
                        newDesc.append(desc.substring(i, iend + 1));
                    }
                    i = iend;
                    break;
                }

                case 'Z': case 'B': case 'C': case 'S': case 'I': case 'F':
                case 'J': case 'D':
                case 'V':
                case '[':  case ')':
                    newDesc.append(ch);
                    break;

                default:
                    log.err("Unable to transform type signature: %s at %d", desc, i);
            }
        }

        return newDesc.toString();
    }


    static long parseType(final String desc, int i, final Mapping mapping) {
        int arrayDepth = 0, argType = 0;
        char ch = desc.charAt(i++);

        if ('[' == ch) {
            do {
                ++arrayDepth;
                ch = desc.charAt(i++);
            } while ('[' == ch);

            assert(arrayDepth <= TypeId.VT_ADEPTH_MASK);
        }

        switch (ch) {
            case 'L': {
                int iend = desc.indexOf(';', i);
                String argClassPath = desc.substring(i, iend);
                argType = null == mapping ?
                        (argClassPath.equals("ValueType") ? getWildcardTypeId() : TypeId.OBJ_REF)
                        : mapping.getClassTypeId(argClassPath);
                i = iend + 1;
                break;
            }

            case 'Z': case 'B': case 'C': case 'S': case 'I':
                argType = TypeId.I32;
                break;

            case 'F':
                argType = TypeId.F32;
                break;

            case 'J':
                argType = TypeId.I64;
                break;

            case 'D':
                argType = TypeId.F64;
                break;

            case 'V':
                argType = TypeId.VOID;
                break;

            default:
                log.err("Unable to parse method signature: %s at %d", desc, i);
                return -1;
        }

        if (0 != arrayDepth) {
            argType = TypeId.arrayFrom(argType, arrayDepth);
        }

        return ((long)argType & 0xFFFFFFFFL) | ((long)i << 32);
    }


    /**
     * Parse argument types into parsedArgs[] and return the number of parsed values.
     * parsedArgs[0] contains the parsed return value type
     * parsedArgs[1..returnValue] contain parsed arguments
     * @param nUsedArgs number of already filled entries in parsedArgs array, applied as offset, usually = 0
     * @param desc input method signature to parse
     * @param mapping optional mapping that allows identification of Value Type classes
     */
    public static int parseMethod(int[] parsedArgs, int nUsedArgs, final String desc, final Mapping mapping) {

        if(desc.charAt(0) == '(') {
            for (int i = 1, j = nUsedArgs, nParms = 0;;) {
                int argType;
                char ch = desc.charAt(i);

                if (')' == ch) {
                    nParms = j;
                    j = 0;
                    ++i;
                } else {
                    ++j;
                }

                long tmp = parseType(desc, i, mapping);
                i = (int)(tmp >>> 32);
                parsedArgs[j] = argType = (int) tmp;
                if (-1 == argType) {
                    log.err("Unable to parse method signature: %s at %d", desc, i);
                    return -1;
                }

                if (0 == j)
                    return nParms;
            }
        } else {
            log.err("Not a method signature: %s", desc);
            return -1;
        }
    }

    public static int parseMethod(int[] parsedArgs, boolean isNotStatic, final String owner, final String desc, final Mapping mapping) {
        int nThisArgs =  0;
        if (isNotStatic) {
            // Add "this" argument
            parsedArgs[nThisArgs = 1] = null != mapping ? mapping.getClassTypeId(owner) : TypeId.OBJ_REF;
        }

        return parseMethod(parsedArgs, nThisArgs, desc, mapping);
    }

    public static boolean hasNoArgs(String desc) {
        return desc.startsWith("()");
    }
}
