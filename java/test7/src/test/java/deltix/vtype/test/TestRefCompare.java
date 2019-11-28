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

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRefCompare {

    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testCreateRef() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
    }

    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testGetRef() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        dtAtomicRef1.get().addNanos(1);
    }

    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testGetAssignRef1() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        // No 'boxing' warning because this "Object" var actually turned into DateTime! This is mostly unavoidable side-effect.
        // Actual boxing will only occur if more complex execution flow is encountered or the variable is actually used as an Object
        Object a = dtAtomicRef1.get().addNanos(1);      // Implicit typecast, then unboxed on method call, assigned as valuetype, not Object
        DateTime b = dtAtomicRef1.get().addNanos(1);    // Implicit typecast, then unboxed on method call
    }


    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testGetAssignRef2() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        Object a = dtAtomicRef1.get();              // Assign reference directly
        Object b = (DateTime)dtAtomicRef1.get();    // Explicit typecast, then unboxed on assignment
        DateTime c = dtAtomicRef1.get();            // Implicit typecast, then unboxed on assignment
    }

    @Test
    @ValueTypeSuppressWarnings({"refValueCompare", "uninitialized", "frameSyncUnboxing", "frameSyncBoxing"})
    public void testNullCompare1() {
        DateTime aNull = null;
        DateTime bNull = DateTime.NULL;
        DateTime a = DateTime.now();
        Object b = DateTime.now();
        assertTrue(aNull == null);
        assertFalse(aNull != null);
        assertTrue(bNull == null);
        assertFalse(bNull != null);
        assertTrue(a != null);
        assertFalse(a == null);
        assertTrue(b != null);
        assertFalse(b == null);
    }

    @Test
    @ValueTypeSuppressWarnings({"refValueCompare", "uninitialized", "frameSyncUnboxing", "frameSyncBoxing"})
    public void testNullCompare2() {
        Object oNull = null;
        DateTime aNull = null;
        DateTime a = DateTime.now();

        assertTrue(aNull != a);
        assertFalse(a == aNull);
        assertTrue(oNull != a);
        assertFalse(a == oNull);
        aNull = DateTime.create(a.getLong());
        oNull = "42";
        assertTrue(aNull == a);
        assertFalse(a != aNull);
        assertTrue(oNull != a);
        assertFalse(a == oNull);
        aNull = null;
        oNull = null;
        assertTrue(aNull != a);
        assertFalse(a == aNull);
        assertTrue(oNull != a);
        assertFalse(a == oNull);
    }

    @Test
    @ValueTypeSuppressWarnings({"refValueCompare", "refArs", "frameSyncUnboxing", "frameSyncBoxing"})
    public void testNullCompare3() throws ParseException {

        DateTime a = DateTime.now();

        if (true) {
            DateTime aNull = null;
            assertTrue(aNull != a);
            assertFalse(a == aNull);
            aNull = DateTime.fromString(a.toString());
            assertTrue(aNull == a);
            assertFalse(a != aNull);
        }

        if (true) {
            Object oNull = null;
            Object o2;
            assertTrue(oNull != a);
            assertFalse(a == oNull);
            o2 = oNull;
        }
    }


    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refArgs", "refCompare"})
    public void testRefCompare1() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        boolean a = false;
        // DO NOT remove these typecasts. They are NOT redundant in this test
        if (DateTime.create(12345) == (DateTime)dtAtomicRef1.get()) {
            a = true;
        }

        assertTrue(a);
    }

    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refArgs", "refCompare"})
    public void testRefCompare2() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        AtomicReference<DateTime> dtAtomicRef2 = new AtomicReference<>(DateTime.create(12345));
        boolean a = false;
        // NOTE: DO NOT remove these typecasts. They are NOT redundant in this test
        if ((DateTime)dtAtomicRef1.get() == (DateTime)dtAtomicRef2.get()) {
            a = true;
        }

        assertTrue(a);
    }


    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refCompare"})
    public void testRefCompare3() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        boolean a = false;
        if (DateTime.create(12345) == dtAtomicRef1.get()) {
            a = true;
        }

        assertTrue(a);
    }

    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refCompare"})
    public void testRefCompare4() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));

        boolean a = DateTime.create(12345) == dtAtomicRef1.get();
        boolean b = dtAtomicRef1.get() == DateTime.create(12345);
    }

    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refCompare"})
    public void testRefCompare5() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        AtomicReference<DateTime> dtAtomicRef2 = new AtomicReference<>(DateTime.create(12345));

        assertTrue(dtAtomicRef1.get() == DateTime.create(12345));
        assertTrue( DateTime.create(12345) == dtAtomicRef1.get());
        assertFalse( dtAtomicRef1.get() == dtAtomicRef2.get());
        assertTrue( (DateTime)dtAtomicRef1.get() == dtAtomicRef2.get());
        assertTrue( dtAtomicRef2.get() == (DateTime)dtAtomicRef1.get());

        if (dtAtomicRef1.get() == DateTime.create(12345)) {
            assertTrue(true);
        } else {
            assertTrue(false);
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"refAssign", "refCompare"})
    public void testRefCompare6() {
        AtomicReference<DateTime> dtAtomicRef1 = new AtomicReference<>(null);
        AtomicReference<DateTime> dtAtomicRef2 = new AtomicReference<>(DateTime.create(12345));

        assertTrue(dtAtomicRef1.get() != DateTime.create(12345));
        assertTrue( DateTime.create(12345) != dtAtomicRef1.get());
        assertTrue( dtAtomicRef1.get() != dtAtomicRef2.get());

        if (dtAtomicRef1.get() != DateTime.create(12345)) {
            assertTrue(true);
        } else {
            assertTrue(false);
        }
    }

    @Test
    public void testRefCompareDifferentTypes1() {
        AtomicReference dtAtomicRef1 = new AtomicReference<>(DateTime.create(12345));
        AtomicReference dtAtomicRef2 = new AtomicReference<>(DateTime.create(12345));
        AtomicReference decAtomicRef = new AtomicReference<>(Decimal64.fromDouble(12345));

        AtomicReference dtAtomicZeroRef1 = new AtomicReference<>(DateTime.create(0));
        AtomicReference dtAtomicZeroRef2 = new AtomicReference<>(DateTime.create(0));
        AtomicReference decAtomicZeroRef = new AtomicReference<>(Decimal64.fromUnderlying(0));

        // Comparison of different VTypes is a special case, with instruction being deleted or replaced with conditional branch

        assertTrue(dtAtomicRef1.get() != decAtomicRef.get());
        assertFalse(dtAtomicRef1.get() == decAtomicRef.get());

        assertTrue(dtAtomicRef1.get() == dtAtomicRef1.get());
        assertFalse(dtAtomicRef1.get() != dtAtomicRef1.get());
        assertTrue(dtAtomicRef1.get() != dtAtomicRef2.get());
        assertFalse(dtAtomicRef1.get() == dtAtomicRef2.get());
        assertTrue((DateTime)(dtAtomicRef1.get()) == dtAtomicRef2.get());
        assertFalse((DateTime)dtAtomicRef1.get() != dtAtomicRef2.get());
        assertTrue((DateTime)dtAtomicRef1.get() == (DateTime)dtAtomicRef2.get());
        assertFalse((DateTime)dtAtomicRef1.get() != (DateTime)dtAtomicRef2.get());
        assertTrue(dtAtomicRef1.get() == (DateTime)dtAtomicRef2.get());
        assertFalse(dtAtomicRef1.get() != (DateTime)dtAtomicRef2.get());
        assertTrue(dtAtomicRef2.get() == (DateTime)dtAtomicRef1.get());
        assertFalse(dtAtomicRef2.get() != (DateTime)dtAtomicRef1.get());
        assertTrue((dtAtomicRef1.get()) != dtAtomicRef2.get());
        assertFalse(dtAtomicRef1.get() == dtAtomicRef2.get());
        assertTrue(dtAtomicRef1.get() != dtAtomicRef2.get());
        assertFalse(dtAtomicRef1.get() == dtAtomicRef2.get());

        assertTrue(dtAtomicZeroRef1.get() != decAtomicZeroRef.get());
        assertFalse(dtAtomicZeroRef1.get() == decAtomicZeroRef.get());


        assertTrue(dtAtomicZeroRef1.get() == dtAtomicZeroRef1.get());
        assertFalse(dtAtomicZeroRef1.get() != dtAtomicZeroRef1.get());
        assertTrue(dtAtomicZeroRef1.get() == (DateTime)dtAtomicZeroRef2.get());
        assertFalse((DateTime)dtAtomicZeroRef1.get() != dtAtomicZeroRef2.get());
        assertTrue((DateTime)dtAtomicZeroRef2.get() == dtAtomicZeroRef1.get());
        assertFalse((DateTime)dtAtomicZeroRef2.get() != dtAtomicZeroRef1.get());
    }
}
