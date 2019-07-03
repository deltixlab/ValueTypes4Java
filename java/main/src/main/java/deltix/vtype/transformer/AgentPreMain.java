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

import java.lang.instrument.Instrumentation;

public final class AgentPreMain {

    public static void premain(String args, Instrumentation instrumentation) throws Exception { // magic

        System.out.printf("VT Agent: Version %s started. premain() BEGIN%n", Version.version);
        initAgent(args, instrumentation);
        System.out.println("VT Agent: premain() END");
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception { // magic

        System.out.printf("VT Agent: Version %s started. agentmain() BEGIN%n", Version.version);
        initAgent(args, instrumentation);
        System.out.println("VT Agent: agentmain() END");
    }

    private static void initAgent(String args, Instrumentation instrumentation) {
        try {
            // TODO: Low priority. Remove extra args if any, sanitize.
            ClassFileTransformer transformer = new ClassFileTransformer(args);
            // NOTE: Since re-transform shouldn't change method and field signatures, we can't do it,
            // as our translation does change signatures
            instrumentation.addTransformer(transformer, false);
            // Load Value Type classes before any other classes that may use them, analyse their contents.
            transformer.readClasses();
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("VT Agent initialization failed, instrumentation aborted");
        }
    }
}
