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

package deltix.vtype.transformer;

import deltix.vtype.type.Util;
import deltix.vtype.type.VariableMap;
import deltix.vtype.type.VariableNameFormatter;
import org.junit.Test;

import static deltix.vtype.type.Util.parseItemList;
import static deltix.vtype.type.Util.parseType;
import static deltix.vtype.type.Util.toStr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VariableMapTest {
    VariableMap vars;
    private static final VariableNameFormatter formatter = VariableNameDefaultFormatter.get();
    private boolean use2x = false;


    VariableMap make(String str) {

        VariableMap v = new VariableMapV2(formatter, use2x, 0);
        String[] items = parseItemList(str);

        for (String item : items) {
            int typeId = parseType(item);
            v.add(typeId, item + "-Name");
        }

        return v;
    }


    boolean test(VariableMap v1, String str, boolean failExpected) {

        VariableMap v2 = make(str);

        if (v1.numVars() != v2.numVars()) {
            if (failExpected)
                return true;

            System.err.printf("Stack item count mismatch: Expected: %s%nActual : %s%n", Util.toStr(v2), Util.toStr(v1));
            assertTrue(false);
        }

//        for (int i = 0; i < v1.top(); i++) {
//            if (v1.typeIdAt(i) != v2.typeIdAt(i) || !v1.nameAt(i).equals(v2.nameAt(i))) {
//                if (failExpected)
//                    return true;
//
//                System.err.printf("Stack mismatch at depth %d: Expected: %s%nActual : %s%n", i, toStr(v2, i), toStr(v1, i));
//                assertTrue(false);
//            }
//        }
//
//        if (v1.top32Src() != v2.top32Src() || v1.top32Dst() != v2.top32Dst()) {
//            if (failExpected)
//                return true;
//
//            System.err.printf("Stack simulated depth mismatch: Expected: %d -> %d%nActual : %d -> %d%n",
//                    v2.top32Src(), v2.top32Dst(), v1.top32Src(), v1.top32Dst());
//            assertTrue(false);
//        }

        if (failExpected) {
            System.err.printf("Stack contents equality not expected: Expected: %s%nActual : %s%n", Util.toStr(v2), Util.toStr(v1));
            assertTrue(false);
        }

        return true;
    }


    boolean test(VariableMap vm, String str) {
        return test(vm, str, false);
    }

    boolean testFail(VariableMap vm, String str) {
        return test(vm, str, true);
    }

    boolean expect(String str) {
        return test(vars, str, false);
    }


    protected void init(String str) {

        vars = make(str);
    }

    protected VariableMapTest add(String what) {

        if (what.equals("*")) {
            vars.dbgAddHole(1, 1);
            return this;
        }

        int typeId = parseType(what);
        vars.add(typeId, what + "-Name");

        return this;
    }

    @Test
    public void testIdentity() {

        init("3 2 1");
        expect("3I 2I 1I");
    }

    @Test
    public void testBasic() {

        do {
            init("3 2 1");
            expect("3 2 1");

            assertEquals(3, vars.numVars());
            assertEquals(3, vars.topSrcAddr());
            assertEquals(use2x ? 6 : 3, vars.topDstAddr());

            init("3 2J 1");
            expect("3 2J 1");
            assertEquals(3, vars.numVars());
            assertEquals(4, vars.topSrcAddr());
            assertEquals(use2x ? 6 : 4, vars.topDstAddr());

            init("4J 3D 2J 1D");
            expect("4J 3D 2J 1D");
            assertEquals(4, vars.numVars());
            assertEquals(8, vars.topSrcAddr());
            assertEquals(8, vars.topDstAddr());
        } while(use2x = !use2x);
    }


    @Test
    public void testVTypeBasic() {

        do {
            init("3 2X 1X");
            expect("3 2X 1X");

            assertEquals(3, vars.numVars());
            assertEquals(3, vars.topSrcAddr());
            assertEquals(use2x ? 6 : 5, vars.topDstAddr());

            init("3X 2J 1");
            expect("3X 2J 1");
            assertEquals(3, vars.numVars());
            assertEquals(4, vars.topSrcAddr());
            assertEquals(use2x ? 6 : 5, vars.topDstAddr());

            init("4J 3X 2X 1D");
            expect("4J 3X 2X 1D");
            assertEquals(4, vars.numVars());
            assertEquals(6, vars.topSrcAddr());
            assertEquals(8, vars.topDstAddr());
        } while(use2x = !use2x);
    }

//    @Test
//    public void testVTypeReplacement() {
//
//        do {
//            init("3 2X 1X");
//            expect("3 2X 1X");
//
//            vars.replaceVarUnsafe();
//        } while(use2x = !use2x);
//    }
}
