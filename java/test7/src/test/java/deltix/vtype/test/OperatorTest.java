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
import deltix.vtype.annotations.ValueTypeTrace;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OperatorTest {

    private static final DateTime NOW = DateTime.now();
    private static final DateTime TOMORROW = NOW.addDays(1);

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    @Test
    public void testNull() {
        final DateTime now = null;
        assertTrue(now == null);
        assertFalse(now != null);
    }

    @Test
    public void testNonNull() {
        final DateTime now = NOW;
        assertTrue(now != null);
        assertFalse(now == null);
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testEqual() {
        assertTrue(NOW == NOW);
        assertFalse(NOW != NOW);

        assertTrue(TOMORROW == TOMORROW);
        assertFalse(TOMORROW != TOMORROW);

        assertTrue(NOW != TOMORROW);
        assertFalse(NOW == TOMORROW);
    }

    @ValueTypeSuppressWarnings({"refCompare", "refAssign"})
    public void testCastImpl() {
        final Object object = NOW;
        final DateTime now = (DateTime) object;

        assertTrue(now == NOW);
        assertFalse(now != NOW);
    }

    @Test
    public void testCast() {

        //AllocationDetector.uninstall();
        // Boxing is expected to occur when casting to Object, disabling allocation detector
        testCastImpl();
        //AllocationDetector.install();
    }

    @Test
    public void testInstanceOf1() {

        DateTime dt = DateTime.now();

        assertTrue("dt instanceof DateTime", dt instanceof DateTime);
        assertTrue("dt instanceof Object", dt instanceof Object);
        assertFalse("!(dt instanceof DateTime)", !(dt instanceof DateTime));
        assertFalse("!(dt instanceof Object)", !(dt instanceof Object));
    }

    @Test
    public void testInstanceOf2() {

        DateTime dt = null;

        // null instanceof * == false
        assertFalse("dt instanceof DateTime", dt instanceof DateTime);
        assertFalse("dt instanceof Object", dt instanceof Object);
        assertTrue("!(dt instanceof DateTime)", !(dt instanceof DateTime));
        assertTrue("!(dt instanceof Object)", !(dt instanceof Object));

        dt = DateTime.now();

        assertTrue("dt instanceof DateTime", dt instanceof DateTime);
        assertTrue("dt instanceof Object", dt instanceof Object);
        assertFalse("!(dt instanceof DateTime)", !(dt instanceof DateTime));
        assertFalse("!(dt instanceof Object)", !(dt instanceof Object));
    }

    @Test
    public void testInstanceOf3() {

        DateTime dt = DateTime.now();

        // null instanceof * == false
        assertTrue(dt instanceof DateTime);
        assertTrue(dt instanceof Object);
        assertFalse(!(dt instanceof DateTime));
        assertFalse(!(dt instanceof Object));

        dt = null;

        assertFalse(dt instanceof DateTime);
        assertFalse(dt instanceof Object);
        assertTrue(!(dt instanceof DateTime));
        assertTrue(!(dt instanceof Object));
    }

    @Test
    @ValueTypeSuppressWarnings({"refAssign"})
    public void testInstanceOf4() {

        DateTime dt0 = null;                // Initialized as non-vtype due to null assignment
        DateTime dt1 = DateTime.NULL;       // Initialized as vtype
        Object objDt = null;                // The same as the dt0
        Object objStr = null;
        DateTime dt2 = DateTime.now();

        // null instanceof * == false
        assertFalse(dt0 instanceof DateTime);
        assertFalse(dt0 instanceof Object);
        assertTrue(!(dt0 instanceof DateTime));
        assertTrue(!(dt0 instanceof Object));
        assertFalse(objDt instanceof Object);
        assertFalse(objStr instanceof DateTime);

        assertFalse(dt1 instanceof DateTime);
        assertFalse(dt1 instanceof Object);
        assertTrue(!(dt1 instanceof DateTime));
        assertTrue(!(dt1 instanceof Object));


        dt0 = (DateTime)null;
        dt0 = DateTime.now();    // Actually causes boxing due to assigning null
        assertTrue(null == dt1);    // Verify there is no overrun due to wrongly selected opcode

        dt1 = (DateTime)null;
        dt1 = DateTime.now();
        assertTrue(null == objDt);  // Verify there is no overrun due to wrongly selected opcode

        objDt = dt2;
        objStr = "1337";

        assertTrue(dt0 instanceof DateTime);
        assertTrue(dt0 instanceof Object);
        assertFalse(!(dt0 instanceof DateTime));
        assertFalse(!(dt0 instanceof Object));

        assertTrue(dt1 instanceof DateTime);
        assertTrue(dt1 instanceof Object);
        assertFalse(!(dt1 instanceof DateTime));
        assertFalse(!(dt1 instanceof Object));

        assertTrue(objDt instanceof Object);
        assertTrue(objDt instanceof DateTime);
        assertTrue(objStr instanceof Object);
        assertFalse(objStr instanceof DateTime);
        // TODO: Split into few more tests
    }

}
