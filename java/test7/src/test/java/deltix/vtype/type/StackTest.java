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

import deltix.vtype.common.CrudeLogger;
import deltix.vtype.type.JvmStack;
import org.junit.Test;

import static deltix.vtype.type.Util.*;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class StackTest {
    final static CrudeLogger dummy = new CrudeLogger();

    JvmStack vm;

    public StackTest() {
        dummy.setLogLevel(CrudeLogger.ERR);
    }


    static JvmStack make(String str) {

        JvmStack vm = new JvmStack(dummy, null);
        String[] items = parseItemList(str);

        for (String item : items) {
            int typeId = parseType(item);
            vm.pushTypeId(typeId, item + "-Name");
        }

        return vm;
    }


    static boolean test(JvmStack vm, String str, boolean failExpected) {

        JvmStack vm0 = make(str);

        if (vm.top() != vm0.top()) {
            if (failExpected)
                return true;

            System.err.printf("Stack item count mismatch: Expected: %s%nActual : %s%n", toStr(vm0), toStr(vm));
            assertTrue(false);
        }

        for (int i = 0; i < vm.top(); i++) {
            if (vm.typeIdAt(i) != vm0.typeIdAt(i) || !vm.nameAt(i).equals(vm0.nameAt(i))) {
                if (failExpected)
                    return true;

                System.err.printf("Stack mismatch at depth %d: Expected: %s%nActual : %s%n", i, toStr(vm0, i), toStr(vm, i));
                assertTrue(false);
            }
        }

        if (vm.top32Src() != vm0.top32Src() || vm.top32Dst() != vm0.top32Dst()) {
            if (failExpected)
                return true;

            System.err.printf("Stack simulated depth mismatch: Expected: %d -> %d%nActual : %d -> %d%n",
                    vm0.top32Src(), vm0.top32Dst(), vm.top32Src(), vm.top32Dst());
            assertTrue(false);
        }

        if (failExpected) {
            System.err.printf("Stack contents equality not expected: Expected: %s%nActual : %s%n", toStr(vm0), toStr(vm));
            assertTrue(false);
        }

        return true;
    }

    static boolean test(JvmStack vm, String str) {
        return test(vm, str, false);
    }

    static boolean testFail(JvmStack vm, String str) {
        return test(vm, str, true);
    }

    boolean expect(String str) {
        return test(vm, str, false);
    }

    protected void init(String str) {

        vm = make(str);
    }

    @Test
    public void testInitialization() {
        String a = "";
        String b = "3 2 1";
        String c = "5 4 3 2 1";
        String d = "5 4J 3J 2 1";

        test(make(a), a);
        test(make(b), b);
        test(make(c), c);
        test(make(d), d);

        testFail(make(c), a);
        testFail(make(b), a);
        testFail(make(d), a);

        testFail(make(a), b);
        testFail(make(c), b);
        testFail(make(d), b);

        testFail(make(a), c);
        testFail(make(b), c);
        testFail(make(d), c);

        testFail(make(a), d);
        testFail(make(b), d);
        testFail(make(c), d);

    }

    @Test
    public void testSwap() {

        init("3 2 1");
        vm.swap();
        expect( "3 1 2");
    }

    @Test
    public void testDup() {

        init("3 2 1");
        vm.dup32();
        expect( "3 2 1 1");
    }


    private void pushTypeId(int typeId) {
        vm.pushTypeId(typeId, nameFor(typeId));
    }

    @Test
    public void testPushPop() {

        init("1");

        pushTypeId(t32id(2));
        expect( "1 2");

        pushTypeId(t64id(3));
        expect( "1 2 3J");

        vm.popMany(2);
        expect( "1");


        pushTypeId(t64id(3));
        pushTypeId(t32id(4));
        pushTypeId(t32id(5));
        pushTypeId(t64id(6));
        expect( "1 3J 4 5 6J");

        vm.pop2();
        expect( "1 3J 4 5");

        vm.pop2();
        expect( "1 3J");

        vm.pop64();
        expect( "1");


        pushTypeId(t32id(2));
        vm.pop32();
        vm.pop32();
        expect( "");
    }

    @Test
    public void testDup2() {

        init("3 2 1J");
        vm.dup64();
        expect( "3 2 1J 1J");

        init("3 2 1D");
        vm.dup2();
        expect( "3 2 1D 1D");

        init("3 2 1");
        vm.dup2();
        expect( "3 2 1 2 1");
    }

    @Test
    public void testInternalDupX() {

        // Internal dupx doesn't care about item types
        init("3 2 1J");
        vm._dupX(0, 1);
        expect( "3 2 1J 1J");

        init("3 2 1J");
        vm._dupX(1, 1);
        expect( "3 1J 2 1J");

        init("4 3 2 1D");
        vm._dupX(2, 1);
        expect( "4 1D 3 2 1D");


        init("3D 2X 1X");
        vm._dupX(0, 1);
        expect( "3D 2X 1X 1X");

        init("3D 2D 1");
        vm._dupX(1, 1);
        expect( "3D 1 2D 1");

        init("4 3X 2J 1");
        vm._dupX(2, 1);
        expect( "4 1 3X 2J 1");

        init("4 3D 2D 1");
        vm._dupX(1, 2);
        expect( "4 2D 1 3D 2D 1");

        init("4 3X 2J 1");
        vm._dupX(2, 2);
        expect( "2J 1 4 3X 2J 1");
    }

    @Test
    public void testTryDupX() {

        init("3 2 1");
        assertTrue(vm.tryDupX(1));
        expect( "3 1 2 1");

        init("3 2 1X");
        assertFalse(vm.tryDupX(1));
        expect( "3 2 1X");

        init("3 2X 1");
        assertFalse(vm.tryDupX(1));
        expect( "3 2X 1");

        init("3X 2 1");
        assertTrue(vm.tryDupX(1));
        expect( "3X 1 2 1");

        init("3J 2 1");
        assertTrue(vm.tryDupX(1));
        expect( "3J 1 2 1");



        init("3 2J 1");
        assertTrue(vm.tryDupX(2));
        expect( "3 1 2J 1");

        init("3 2 1");
        assertTrue(vm.tryDupX(2));
        expect( "1 3 2 1");

        init("3 2 1X");
        assertFalse(vm.tryDupX(2));
        expect( "3 2 1X");

        init("3 2X 1");
        assertFalse(vm.tryDupX(2));
        expect( "3 2X 1");

        init("3X 2 1");
        assertFalse(vm.tryDupX(2));
        expect( "3X 2 1");

        init("3X 2D 1");
        assertTrue(vm.tryDupX(2));
        expect( "3X 1 2D 1");
    }
}
