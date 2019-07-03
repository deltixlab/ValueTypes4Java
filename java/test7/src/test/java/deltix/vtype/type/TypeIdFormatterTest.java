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

import static deltix.vtype.type.TypeIdDefaultFormatter.makeBasicTypeDescriptor;
import static deltix.vtype.type.TypeIdDefaultFormatter.makeDstDescriptor;
import static deltix.vtype.type.TypeIdDefaultFormatter.makeSrcDescriptor;
import static org.junit.Assert.assertEquals;

public class TypeIdFormatterTest {

    @Test
    public void testDefaultFormatter() {

        assertEquals("I", makeBasicTypeDescriptor(TypeId.I32));
        assertEquals("J", makeBasicTypeDescriptor(TypeId.I64));
        assertEquals("F", makeBasicTypeDescriptor(TypeId.F32));
        assertEquals("D", makeBasicTypeDescriptor(TypeId.F64));
        assertEquals("V", makeBasicTypeDescriptor(TypeId.VOID));
        assertEquals("Ljava/lang/Object;", makeBasicTypeDescriptor(TypeId.OBJ_REF));

        assertEquals("I", makeDstDescriptor(TypeId.I32));
        assertEquals("J", makeDstDescriptor(TypeId.I64));
        assertEquals("F", makeDstDescriptor(TypeId.F32));
        assertEquals("D", makeDstDescriptor(TypeId.F64));
        assertEquals("V", makeDstDescriptor(TypeId.VOID));
        assertEquals("Ljava/lang/Object;", makeDstDescriptor(TypeId.OBJ_REF));

        assertEquals("[I", makeDstDescriptor(TypeId.arrayFrom(TypeId.I32, 1)));
        assertEquals("[[J", makeDstDescriptor(TypeId.arrayFrom(TypeId.I64, 2)));
        assertEquals("[[[F", makeDstDescriptor(TypeId.arrayFrom(TypeId.F32, 3)));
        assertEquals("[[[[D", makeDstDescriptor(TypeId.arrayFrom(TypeId.F64, 4)));
        assertEquals("[[[[[Ljava/lang/Object;", makeDstDescriptor(TypeId.arrayFrom(TypeId.OBJ_REF, 5)));

        assertEquals("I", makeSrcDescriptor(TypeId.I32));
        assertEquals("J", makeSrcDescriptor(TypeId.I64));
        assertEquals("F", makeSrcDescriptor(TypeId.F32));
        assertEquals("D", makeSrcDescriptor(TypeId.F64));
        assertEquals("V", makeSrcDescriptor(TypeId.VOID));
        assertEquals("Ljava/lang/Object;", makeSrcDescriptor(TypeId.OBJ_REF));

        assertEquals("[I", makeSrcDescriptor(TypeId.arrayFrom(TypeId.I32, 1)));
        assertEquals("[[J", makeSrcDescriptor(TypeId.arrayFrom(TypeId.I64, 2)));
        assertEquals("[[[F", makeSrcDescriptor(TypeId.arrayFrom(TypeId.F32, 3)));
        assertEquals("[[[[D", makeSrcDescriptor(TypeId.arrayFrom(TypeId.F64, 4)));
        assertEquals("[[[[[Ljava/lang/Object;", makeSrcDescriptor(TypeId.arrayFrom(TypeId.OBJ_REF, 5)));

        assertEquals("J", makeDstDescriptor(TypeId.vtValueFromIndex(13)));
        assertEquals("[J", makeDstDescriptor(TypeId.arrayFrom(TypeId.vtValueFromIndex(0), 1)));
        assertEquals("[[[J", makeDstDescriptor(TypeId.arrayFrom(TypeId.vtValueFromIndex(10), 3)));
    }
}
