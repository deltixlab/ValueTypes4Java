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

import org.junit.Test;

import static deltix.vtype.type.TypeIdCast.*;
import static org.junit.Assert.*;

public class TypeIdCastTest {

    int basicTypes[] = { TypeId.VOID, TypeId.I32, TypeId.I64, TypeId.F32, TypeId.F64, TypeId.OBJ_REF };

    @Test
    public void basicTypeCasts() {

        for (int i = basicTypes.length - 1; i >= 0; --i) {
            for (int j = basicTypes.length - 1; j >= 0; --j) {
                for (int castType = 0; castType < 3; ++castType) {
                    int result0 = -1, result1 = -2;
                    int t0 = basicTypes[i], t1 = basicTypes[j];
                    int expected = i == j ? SUCCESS : FAILURE;
                    switch (castType) {
                        case 0:
                            if (TypeId.VOID == t0 || TypeId.VOID == t1)
                                continue;

                            result0 = check(t0, t1);
                            result1 = check(t1, t0);
                            break;
                        case 1:
                            if (TypeId.VOID == t0 || TypeId.VOID == t1)
                                continue;

                            result0 = checkArg(t0, t1);
                            result1 = checkArg(t1, t0);
                            break;
                        case 2:
                            // TODO: Remove
                            if (TypeId.VOID == t0 || TypeId.VOID == t1)
                                continue;

                            if (TypeId.VOID == t0) {
                                expected = SUCCESS;
                            }

                            if (TypeId.VOID == t0) {
                                continue;
                                //expected = SUCCESS;
                            }

                            result0 = checkCastFrame(t0, t1);
                            result1 = checkCastFrame(t1, t0);
                            break;
                    }



                    if (result0 != result1 || result0 != expected) {
                        checkCastFrame(t0, t1);
                        fail(String.format("%x %s %x /%d & unexpected result: [%x, %x]", t0, (i == j ? "==" : "!="), t1, castType, result0, result1));
                    }
                }
            }
        }

//        assertEquals(SUCCESS, check(TypeId.I32, TypeId.I32));
//        assertEquals(SUCCESS, check(TypeId.I64, TypeId.I64));
//        assertEquals(SUCCESS, check(TypeId.F32, TypeId.F32));
//        assertEquals(SUCCESS, check(TypeId.I64, TypeId.I64));
//        assertEquals(SUCCESS, check(TypeId.I64, TypeId.I64));

    }

    public void cast1() {
        Object a = new byte[2][4];
    }
}
