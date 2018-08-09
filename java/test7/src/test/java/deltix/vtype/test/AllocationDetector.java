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

import com.google.monitoring.runtime.instrumentation.ConstructorCallback;
import com.google.monitoring.runtime.instrumentation.ConstructorInstrumenter;
import deltix.dt.DateTime;
import org.junit.Assert;

import java.lang.instrument.UnmodifiableClassException;

// Not thread safe
public class AllocationDetector {

    private static final boolean DETECT_ALLOCATION = !Boolean.getBoolean("disable.allocation.detector");

    private static final AllocationSpyCallback callback = new AllocationSpyCallback();

    static {
        if (DETECT_ALLOCATION) {
            try {
                ConstructorInstrumenter.instrumentClass(DateTime.class, callback);
            } catch (UnmodifiableClassException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void install() {
        callback.enable();
    }

    public static void uninstall() {
        callback.disable();
    }

    private static class AllocationSpyCallback implements ConstructorCallback<DateTime> {

        private boolean enabled = true;

        private void enable() {
            this.enabled = true;
        }

        private void disable() {
            this.enabled = false;
        }

        @Override
        public void sample(DateTime t) {
            if (enabled) {
                Assert.fail("Detected allocation: " + t);
            }
        }
    }

}
