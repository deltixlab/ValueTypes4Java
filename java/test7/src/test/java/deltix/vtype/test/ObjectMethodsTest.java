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

import deltix.dfp.Decimal64;
import deltix.dt.DateTime;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests usage of methods defined for Object class and for Array classes, that are supposed to be inherited
 * and implemented by all ValueTypes
 */
public class ObjectMethodsTest {

    @Test
    public void testScalarEquals() {

        // None of these methods should cause boxing
        DateTime t1 = DateTime.now();
        Decimal64 d1 = Decimal64.fromDouble(123.45);
        Object t2 = DateTime.now();
        Object d2 = Decimal64.fromDouble(42);

        assertTrue(((Object)t1).equals(t1));
        assertTrue(t2.equals(t2));
        assertTrue(((Object)d1).equals(d1));
        assertTrue(d2.equals(d2));
    }

    @Test
    public void testScalarToString() {

        // None of these methods should cause boxing
        DateTime t1 = DateTime.now();
        Decimal64 d1 = Decimal64.fromDouble(123.45);
        Object t2 = DateTime.now();
        Object d2 = Decimal64.fromDouble(42);

        assertNotEquals(0, ((Object)t1).toString().length());
        assertNotEquals(0, t2.toString().length());
        assertNotEquals(0, ((Object)d1).toString().length());
        assertNotEquals(0, d2.toString().length());
    }

    @Test
    //@ValueTypeSuppressWarnings({"frameSyncBoxing"}) // IF opcode added by comparison operator causes boxing back to Object
    public void testScalarToString2() {

        // None of these methods should cause boxing
        DateTime t1 = DateTime.now();
        Decimal64 d1 = Decimal64.fromDouble(123.45);
        Object t2 = DateTime.now();
        Object d2 = Decimal64.fromDouble(42);


        assertTrue(((Object)t1).toString().length() > 1);
        assertTrue(t2.toString().length() > 1);
        assertTrue(((Object)d1).toString().length() > 1);
        assertTrue(d2.toString().length() > 1);
    }

    @Test
    public void testArrayEquals() {

        // None of these methods should cause boxing
        DateTime[] t1 = new DateTime[42];
        Decimal64[] d1 = new Decimal64[42];
        Object t2 = new DateTime[42];
        Object d2 = new Decimal64[42];

        assertTrue(((Object)t1).equals(t1));
        assertTrue(t2.equals(t2));
        assertTrue(((Object)d1).equals(d1));
        assertTrue(d2.equals(d2));
    }

    @Test
    public void testArrayToString() {

        // None of these methods should cause boxing
        DateTime[] t1 = new DateTime[42];
        Decimal64[] d1 = new Decimal64[42];
        Object t2 = new DateTime[42];
        Object d2 = new Decimal64[42];

        assertTrue(((Object)t1).toString().length() > 1);
        assertTrue(t2.toString().length() > 1);
        assertTrue(((Object)d1).toString().length() > 1);
        assertTrue(d2.toString().length() > 1);
    }


    @Test
    public void testArrayClone() {

        // None of these methods should cause boxing
        DateTime[] t1 = new DateTime[42];
        Decimal64[] d1 = new Decimal64[42];
        Object t2 = new DateTime[42];
        Object d2 = new Decimal64[42];

        assertTrue(t1.clone().length == 42);
        assertTrue(((DateTime[])t2).clone().length == 42);
        assertTrue(d1.clone().length == 42);
        assertTrue(((Decimal64[])d2).clone().length == 42);

        assertTrue((d1.clone()).length == d1.length);
        assertTrue(((Decimal64[]) d2).clone().length ==((Decimal64[]) d2).length);
    }
}
