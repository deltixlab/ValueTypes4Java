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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccessorsTest {
    static class A {
        DateTime dt;

        public DateTime getDt() {
            return dt;
        }

        public void setDt(DateTime dt) {
            this.dt = dt;
        }

        public void setdt(DateTime dt) {
            this.dt = dt;
        }

        public A setDt2(DateTime dt) {
            this.dt = dt;
            return this;
        }

        public DateTime setAndReturnDt(DateTime dt) {
            this.dt = dt;
            return dt;
        }

        public DateTime dt() {
            return dt;
        }


        public DateTime now() {
            return DateTime.now();
        }
    }

    static class AUser {
        A x = new A();
        public void testGet() {
            DateTime tmp = x.dt = DateTime.now();
            x.getDt();
            assertEquals(tmp, x.getDt());
        }

        public void testSet() {
            x.setDt(DateTime.now());
        }

        public void testBoth() {
            x.setDt(x.getDt());
        }

        public void testManyAccessors() {
            x.setdt(x.now());
            x.setDt2(x.getDt()).setdt(x.setAndReturnDt(x.dt()));
        }
    }


    private static class B {
        DateTime _dt;

        DateTime[][] _dtArray;

        public void setDt(DateTime dt) {
            _dt = dt;
        }

        public DateTime[][] dtArray() {
            return _dtArray;
        }

        public void setDt(long dt) {
            _dt = DateTime.create(dt);
        }

        public void setDt(Long dt) {
            _dt = DateTime.create(dt);
        }

        public void setDtArray(DateTime dt) {
            _dtArray = new DateTime[1][1];
            _dtArray[0][0] = dt;
        }

        public void setDtArray(Long dt) {
            setDtArray(DateTime.create(dt));
        }

        public void setDtArray(DateTime[] dt) {
            _dtArray = new DateTime[1][0];
            _dtArray[0] = dt;
        }

        public void setDtArray(DateTime[][] dt) {
            _dtArray = dt;
        }

        public void setDtArray(DateTime[][][] dt) {
            _dtArray = dt[0];
        }

        public void setDtArray(long[] dt) {
        }

        public void setDtArray(Long[] dt) {
        }

        public void setDtArray(long[][] dt) {
        }

        public void setDtArray(Long[][] dt) {
        }

        public void setDtArray(long[][][] dt) {
        }

        public void setDtArray(Long[][][] dt) {
        }
    }


    private static class BUser {
        B x = new B();

        public void testOverloads() {
            DateTime now = DateTime.now();
            x.setDt(now);
            x.setDt(now.getLong());
            x.setDt((Long)now.getLong());
        }

        public void testArrayOverloads() {
            DateTime now = DateTime.now();
            x.setDtArray(now);
            x.setDtArray(x.dtArray()[0]);
            x.setDtArray(x.dtArray());
        }
    }

    @Test
    public void testBasicAccessors() {
        new AUser().testGet();
        new AUser().testSet();
        new AUser().testBoth();
    }

    @Test
    public void testOtherAccessorNames() {
        new AUser().testManyAccessors();
    }

    @Test
    public void testOverloads() {
        new BUser().testOverloads();
    }

    @Test
    public void testArrayOverloads() {
        new BUser().testArrayOverloads();
    }
}
