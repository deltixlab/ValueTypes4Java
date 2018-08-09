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
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import deltix.vtype.annotations.ValueTypeTrace;
import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class VariableFrameTest {
    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    @Test
    public void testUninitializedVars1() {
        int a, b;
        int c = 3;
        DateTime.now();
    }

    @Test
    public void testUninitializedVars2() {
        int a, b;
        int c = 3;

        while (c < 10) {
            DateTime dt1 = DateTime.now();
            String s = "1234";
            ++c;
        }

        String s1, s2;
        DateTime dt = DateTime.now();
        // AssertTrue(c == 10);
    }

    @Test
    public void testUninitialized64BitVars1() {
        double d;
        int a, b;
        Object obj1 = null;
        long l;
        int c = 3;
        Object obj2 = "2";

        if (c < 10) {
            d = 123;
            l = 321;
            obj2 = 42;
            ++c;
        } else {
            d = 321;
            l = 123;
            obj2 = 42;
            ++c;
        }

        String s1, s2;
        DateTime dt = DateTime.now(); // Should have addr = 12
        assertTrue(123 == d);
        assertTrue(321 == l);
        assertTrue(obj2.equals(42));
    }

    @Test
    public void testUninitialized64BitVars2() {
        double d;
        int a, b;
        Object obj1 = null;
        long l;
        int c = 3;
        Object obj2 = "2";

        if (c < 10) {
            obj2 = 42;
            ++c;
        } else {
            obj2 = 42;
            ++c;
        }

        d = 123;
        l = 321;

        String s1, s2;
        DateTime dt = DateTime.now();
        assertTrue(123 == d);
        assertTrue(321 == l);
        assertTrue(obj2.equals(42));
    }


    @Test
    public void testUninitialized64BitVars3() {

        int c = 3;
        DateTime dt = DateTime.now();
        double d;
        long l;
        double d2 = 123;
        DateTime dt2 = dt.addNanos(123);

        while(--c != 0) {
            d = c + 1;
        }

        if (c != 0) {
            l = 321;
        }

        assertTrue(d2 == 123);
    }


    @Test
    @ValueTypeSuppressWarnings({"uninitialized", "refAssign"})
    public void testUninitializedValueType1() {
        int a, b;
        DateTime dt;
        int c = 3;

        if (c < 10) {
            dt = DateTime.now();
            String s = "1234";
            ++c;
        } else {
            dt = DateTime.now();
            String s = "4321";
            ++c;
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"uninitialized", "refAssign"})
    public void testUninitializedValueType2() {
        int a, b;
        DateTime dt1;
        //DateTime dt1 = null; // TODO: Check this variant
        String str;
        DateTime dt2;
        DateTime dt3 = DateTime.now();
        int c = 3;

        if (c < 10) {
            dt2 = DateTime.now();
            String s = "1234";
            ++c;
        } else {
            dt2 = DateTime.now();
            String s = "4321";
            ++c;
        }

        dt1 = DateTime.now();
        str = dt1.toString();
        str = "42";
    }



    protected Object getVariant(long src, boolean getDecimal)
    {
        // This test checks possibility to auto-box ValueType into  Object
        return getDecimal ? Decimal64.fromUnderlying(src) : DateTime.create(src);
    }


    protected Object[] getVariantArray(boolean getDecimal)
    {
        // This test checks possibility to auto-box ValueType[] into Object[]
        return getDecimal ? new Decimal64[4] : new DateTime[4];
    }


    @Test
    public void testVariant() {

        // This test checks possibility to auto-box ValueType into  Object
        assertTrue(getVariant(123456789, false) instanceof DateTime);
        assertTrue(getVariant(123456789, true) instanceof Decimal64);
    }


    @Test
    public void testVariantArray() {

        // This test checks possibility to auto-box ValueType[] into Object[]
        Object[] objArray1 = new DateTime[3];
        Object[] objArray2 = new Decimal64[3];
        Object[] objArray3;
        Object[] objArray4;

        // Both Value Types are transformed to long
        assertTrue(objArray1 instanceof DateTime[]);
        assertTrue(objArray2 instanceof Decimal64[]);
        assertTrue(objArray2 instanceof DateTime[]);
        assertTrue(objArray1 instanceof Decimal64[]);

        // This is not Object[] anymore
        assertFalse(objArray1 instanceof Object[]);
        assertFalse(objArray2 instanceof Object[]);

        // Directly test returned type without assignment
        assertTrue(getVariantArray( false) instanceof DateTime[]);
        assertTrue(getVariantArray( true) instanceof Decimal64[]);

        // Assign to uninitialized
        objArray3 = getVariantArray( false);
        objArray4 = getVariantArray( true);
        assertTrue(objArray3 instanceof Object[]);
        assertTrue(objArray4 instanceof Object[]);
        assertTrue(objArray3 instanceof DateTime[]);
        assertTrue(objArray4 instanceof Decimal64[]);
        assertFalse(objArray4 instanceof DateTime[]);
        assertFalse(objArray3 instanceof Decimal64[]);

        // Assign same type to previously initialized
        objArray1 = getVariantArray( false);
        objArray2 = getVariantArray( true);
//        assertTrue(objArray1 instanceof DateTime[]);
//        assertTrue(objArray2 instanceof Decimal64[]);

        assertTrue(objArray1 instanceof Object[]);
        assertTrue(objArray2 instanceof Object[]);
        assertTrue(objArray1 instanceof DateTime[]);
        assertTrue(objArray2 instanceof Decimal64[]);
        assertFalse(objArray2 instanceof DateTime[]);
        assertFalse(objArray1 instanceof Decimal64[]);

        // Assign different type to previously assigned
        objArray4 = getVariantArray( false);
        objArray3 = getVariantArray( true);
        assertTrue(objArray4 instanceof DateTime[]);
        assertTrue(objArray3 instanceof Decimal64[]);

        // Assign different type to previously initialized and assigned
        objArray2 = getVariantArray( false);
        objArray1 = getVariantArray( true);
        assertTrue(objArray2 instanceof DateTime[]);
        assertTrue(objArray1 instanceof Decimal64[]);
    }


    static class DummyList  extends ArrayList<Object> {
        DateTime[] dataDt;
        Decimal64[] dataDecimal;

        public DummyList(DateTime[] dtArray) {
            this.dataDt = null == dtArray ? null : dtArray;
        }


        public DummyList(DateTime[] dtArray, Decimal64[] decimalArray) {

            // This code caused failure of stack frame update methods once
            this.dataDt = null == dtArray ? null : dtArray;
            this.dataDecimal = null == decimalArray ? null : decimalArray;
        }

//        @Override
//        public  <T> T[] toArray(T[] a) {
//            // This version will box "dataDt" array, which is quite expensive operation, by the way
//            //SafeArrayCopy.safeArrayCopy(a instanceof Decimal64[] ? (Object [])dataDecimal : (Object[])dataDt, 0, a, 0, 42);
//            SafeArrayCopy.safeArrayCopy(a instanceof Decimal64[] ? dataDecimal : dataDt, 0, a, 0, 42);
//            return a;
//        }
    }


    @Test
    public void testConstructorWithArrays() {

        DummyList dl1 = new DummyList(new DateTime[11]);
        DummyList dl2 = new DummyList(new DateTime[11], new Decimal64[111]);
    }
}
