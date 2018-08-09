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
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class ArrayConversionTest {

    private Decimal64[] decimalArray;
    private int[] intArray;

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    @Test
    public void testAutoMethods1() {

        DateTime[] dta = new DateTime[42];
        dta[12] = DateTime.now();
        DateTime[] dta2 = Arrays.copyOf(dta, 24);
        assertTrue(dta[12] == dta2[12]);
        assertTrue(dta[12].equals(dta2[12]));

        String[] str = new String[42];
        str[12] = "314";
        String[] str2 = Arrays.copyOf(str, 24);
        assertTrue(str[12].equals(str2[12]));
    }

    // TODO: This test stopped working with Decimal64 stub!
    @Test
    @Ignore
    public void testAutoMethods2() {

        DateTime[] dta = new DateTime[42];
        dta[12] = DateTime.now();
        DateTime[] dta2 = Arrays.copyOf(dta, 142);

        SafeArrayCopy.safeArrayCopy (dta, 0, dta2, 100, 24);
        assertTrue(dta[12] == dta2[112]);
        assertTrue(dta[12].equals(dta2[112]));
    }


    static class DummyList1  extends ArrayList<Object> {
        DateTime[] dataDt;
        Decimal64[] dataDecimal;


        public DummyList1(DateTime[] dtArray) {

            this.dataDt = null == dtArray ? null : dtArray;
        }


        public DummyList1(DateTime[] dtArray, Decimal64[] decimalArray) {

            this.dataDt = null == dtArray ? null : dtArray;
            this.dataDecimal = null == decimalArray ? null : decimalArray;
        }
    }


    static class DummyList  extends ArrayList<Object>  {
        final DateTime[] dataDt;
        final Decimal64[] dataDecimal;
        final boolean hasDecimal;

        public DummyList(DateTime[] dta, Decimal64[] decimal) {

            this.dataDt = null == dta ? null : Arrays.copyOf(dta, dta.length);
            this.dataDecimal = null == decimal ? null : Arrays.copyOf(decimal, decimal.length);
            this.hasDecimal = decimal != null;
        }


        public static DummyList fromDecimal(Decimal64[] decimal) {
            return new DummyList(null, decimal);
        }


        public static DummyList fromDateTime(DateTime[] dt) {
            return new DummyList(dt, null);
        }


        @Override
        public <T> T[] toArray(T[] a) {
            // This version will box "dataDt" array, which is quite expensive operation, by the way
            SafeArrayCopy.safeArrayCopy (hasDecimal ? dataDecimal : dataDt, 0, a, 0, 42);
            return a;
        }


        public <T> T[] toArrayDt4(T[] a) {
            int size = dataDt.length;
            if (a.length < size) {
                //return (T[]) Arrays.copyOf(dataDt, size, a.getClass());
                return (T[]) Arrays.copyOf(dataDt, size);
            }

            SafeArrayCopy.safeArrayCopy (dataDt, 0, a, 0, size);

            if (a.length > size) {
                a[size] = null;
            }

            return a;
        }

//        @Override
//        public  <T> T[] toArray(T[] a) {
//            // This version will box "dataDt" array, which is quite expensive operation, by the way
//            if (hasDecimal)
//                return Decimal64UtilsWrapper2.fromUnderlyingLongArray((long[])dataDecimal, 0, a, 0, 42);
//            //SafeArrayCopy.safeArrayCopy (dataDt, 0, a, 0, 42);
//            //return a;
//        }

//        @Override
//        public Object[] toArray() {
//            return hasDecimal ? (Object[])dataDecimal : (Object[])dataDt;
//        }

        @Override
        public Object get(int index) {
            return hasDecimal ? dataDecimal[index] : dataDt[index];
        }
    }


    public static DateTime[] arrayHalfVtVt(DateTime[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // No array boxing
    }

    @ValueTypeSuppressWarnings({"refReturn"})
    public static Object[] arrayHalfDtObj(DateTime[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // Array boxing
    }


    public static Object arrayHalfDtObj2(DateTime[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // No array boxing
    }

    public static Object[] arrayObjHalfObjObj(Object[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // No array boxing, arg boxed on call
    }

    public static <T> T[] arrayHalfTemplateObjObj(T[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // No array boxing, arg boxed on call
    }

    public static Object arrayObjHalf2(Object[] arr) {
        return Arrays.copyOf(arr, arr.length / 2); // No array boxing, arg boxed on call
    }

    public static Object arrayCast1(DateTime[] dta) {
        // VT array directly cast to object
        return (Object)dta;
    }

    @ValueTypeSuppressWarnings({"refCast"})
    public static Object[] arrayCast2(DateTime[] dta) {
        // VT array is boxed by typecast
        return (Object[])dta;
    }

    @Test
    public void testArrayCast() {

        DateTime dta[] = new DateTime[7];

        for (int i = 0; i < dta.length; i++) {
            dta[i] = DateTime.now().addNanos(i);
        }

        // First, try calling and then ignoring result
        arrayCast1(dta);
        arrayCast2(dta);

        // Note that when ValueType arrays are cast to Object, they remain long[]
        long[] a1 = (long[]) arrayCast1(dta);
        assertTrue(a1 != null);
        assertTrue(a1[2] % 10 == 2);

        // Note that when ValueType arrays are cast to Object, they can be cast back to ValueType Arrays directly
        DateTime[] a2 = (DateTime[]) arrayCast1(dta);
        assertTrue(a2 != null);
        assertTrue(a2[2].getLong() % 10 == 2);


        Object[] a3 = arrayCast2(dta);  // Should be boxed by the explicit cast within arrayCast2() method
        assertTrue(a3 != null);
        assertTrue(a3[2] != null);
        assertTrue(((DateTime)a3[2]).getLong() % 10 == 2);
        assertTrue(((DateTime[])a3)[2].getLong() % 10 == 2);
    }


    @Test
    public void testArrayBoxing() {

        DateTime dta[] = new DateTime[7];

        for (int i = 0; i < dta.length; i++) {
            dta[i] = DateTime.now().addNanos(i);
        }

        DateTime[] a1 = arrayHalfVtVt(dta);
        assertTrue(a1[2].getLong() % 10 == 2);

        Object[] a2 = arrayObjHalfObjObj(dta);
        assertTrue(((DateTime)a2[2]).getLong() % 10 == 2);

        DateTime[] a3 = arrayHalfTemplateObjObj(dta);
        assertTrue(a3[2].getLong() % 10 == 2);

        DateTime[] a4 = (DateTime[]) arrayObjHalfObjObj(dta);
        assertTrue(a4[2].getLong() % 10 == 2);
    }

    @Test
    public void testArrayBoxing2() {

        DateTime dta[] = new DateTime[7];

        for (int i = 0; i < dta.length; i++) {
            dta[i] = DateTime.now().addNanos(i);
        }

        // First, try calling and then ignoring result
        arrayHalfDtObj(dta);
        arrayHalfDtObj2(dta);

        Object[] a1 = arrayHalfDtObj(dta);
        assertTrue(a1 != null);
        assertTrue(a1[2] != null);
        assertTrue(((DateTime)a1[2]).getLong() % 10 == 2);
        assertTrue(((DateTime[])a1)[2].getLong() % 10 == 2);

        // Note that when ValueType arrays are cast to Object, they remain long[]
        long[] a2 = (long[]) arrayHalfDtObj2(dta);
        assertTrue(a2 != null);
        assertTrue(a2[2] % 10 == 2);

        // Note that when ValueType arrays are cast to Object, they can be cast back to ValueType Arrays directly
        DateTime[] a3 = (DateTime[]) arrayHalfDtObj2(dta);
        assertTrue(a3 != null);
        assertTrue(a3[2].getLong() % 10 == 2);
    }


    @Test
    @ValueTypeSuppressWarnings({"refValueCompare"})
    public void testArrayTemplatesDt() {

        DateTime[] dta = new DateTime[42];
        dta[12] = DateTime.now();
        DateTime[] dta2 = Arrays.copyOf(dta, 142);

        DummyList dl = DummyList.fromDateTime(dta);
        DateTime[] dta3 = dl.toArray(dta2);
        assertTrue(dta[12] == dl.get(12));
        assertTrue(dta[12].equals(dl.get(12)));
        assertTrue(dta3[12] == dl.get(12));
        assertTrue(dta3[12].equals(dl.get(12)));
    }

    @Test
    @ValueTypeSuppressWarnings({"refValueCompare"})
    public void testArrayTemplatesDecimal() {

        Decimal64[] dec = new Decimal64[42];
        dec[12] = Decimal64.fromDouble(0.42);
        Decimal64[] dec2 = Arrays.copyOf(dec, 142);

        DummyList dl = DummyList.fromDecimal(dec);
        Decimal64[] dta3 = dl.toArray(dec2);
        assertTrue(dec[12] == dl.get(12));
        assertTrue(dec[12].equals(dl.get(12)));
        assertTrue(dta3[12] == dl.get(12));
        assertTrue(dta3[12].equals(dl.get(12)));
    }


    private void resize1Array1(int size) {
        decimalArray = Arrays.copyOf(decimalArray, size);
    }

    private void resize1Array2(int size) {
        Decimal64 dummy = Decimal64.fromDouble(2525);
        intArray = Arrays.copyOf(new int[123], size);
    }


    private void resize1Array3(int size) {
        Decimal64 dummy = Decimal64.fromDouble(2525);
        intArray = Arrays.copyOf(intArray, size);
    }


    private void resize2Arrays(int size) {
        intArray = Arrays.copyOf(intArray, size);
        decimalArray = Arrays.copyOf(decimalArray, size);
    }

    @Test
    public void testArrayTemplatesWithoutValueType() {

        Decimal64 dec = Decimal64.fromDouble(1.234);
        int[] intArray = Arrays.copyOf(new int[123], 111);
        intArray[12] = 42;
        intArray[10] = 1;
        int[] intArray2 = Arrays.copyOf(intArray, 13);
        int[] intArray3 = null;


        assertTrue(intArray[14] == intArray[14]);
        assertTrue(intArray2[12] == intArray[12] && 42 == intArray2[12]);
        assertTrue(intArray2[10] == intArray[10] && 1 == intArray2[10]);
        assertTrue(null == intArray3);
    }
}
