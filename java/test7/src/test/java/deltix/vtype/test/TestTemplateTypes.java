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

import org.junit.Test;

/**
 * Temporary test for checking the code generated from Java Generics
 */
public class TestTemplateTypes {

    @Test
    public void test() {

        Template<Long> t = new Template<Long>();
        Object x = (Long)12345L;
        Long y;

        //assignLong(t);
        y = t.print((Long)x);
        Template tt = t;
        //y = tt.print(x);
        //assignString(t);

        //t.print(12345L);
        //t.print((Long)(Object)"String");
        //((Template) t).print("String");
    }

    void assignString(Template t) {
        t.print("Test");
    }

    void assignLong(Template t) {
        t.print(1L);
    }


    static class Template<T> {
        T print(T value) {
            System.out.println("Value: [" + value.toString() + "], This = " + this);
            return value;
        }
    }
}
