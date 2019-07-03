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

package deltix.vtype.test;

import deltix.dt.DateTime;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TypeCastTest {

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    static DateTime dummyDouble(double d) {
        return DateTime.now();
    }

    static DateTime dummyFloat(float d) {
        return DateTime.now();
    }

    static DateTime dummyLong(long x) {
        return DateTime.now();
    }

    static DateTime dummyInt(int x) {
        return DateTime.now();
    }

    static DateTime dummyShort(short x) {
        return DateTime.now();
    }

    static DateTime dummyByte(byte x) {
        return DateTime.now();
    }

    static DateTime dummyChar(char x) {
        return DateTime.now();
    }

    static DateTime dummyBool(boolean x) {
        return DateTime.now();
    }

    static DateTime dummyMixedTypes(boolean a, char b, short c, int d, long e, double f) {
        return DateTime.now();
    }

    @Test
    public void testCastsBasic() {

        double  d = 42.0;
        float   f = 42.0f;
        long    l = 42;
        int     i = 42;
        short   s = 42;
        byte    b = 42;
        char    c = 'A';

        dummyDouble(d);
        dummyDouble(f);
        dummyDouble(l);
        dummyDouble(i);
        dummyDouble(s);
        dummyDouble(b);
        dummyDouble(c);

        dummyFloat((float)d);
        dummyFloat(f);
        dummyFloat(l);
        dummyFloat(i);
        dummyFloat(s);
        dummyFloat(b);
        dummyFloat(c);

        dummyLong((long)d);
        dummyLong((long)f);
        dummyLong(l);
        dummyLong(i);
        dummyLong(s);
        dummyLong(b);
        dummyLong(c);

        dummyInt((int)d);
        dummyInt((int)f);
        dummyInt((int)l);
        dummyInt(i);
        dummyInt(s);
        dummyInt(b);
        dummyInt(c);


        dummyShort((short)d);
        dummyShort((short)f);
        dummyShort((short)l);
        dummyShort((short)i);
        dummyShort(s);
        dummyShort(b);
        dummyShort((short)c);

        dummyChar((char)d);
        dummyChar((char)f);
        dummyChar((char)l);
        dummyChar((char)i);
        dummyChar((char)s);
        dummyChar((char)b);
        dummyChar(c);

        dummyByte((byte)d);
        dummyByte((byte)f);
        dummyByte((byte)l);
        dummyByte((byte)i);
        dummyByte((byte)s);
        dummyByte(b);
        dummyByte((byte)c);
    }
}
