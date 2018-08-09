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

package deltix.vtype.common;

import java.util.HashMap;

public abstract class CachedStringConverter {
    private static boolean logCacheUsage = false;
    // Cache
    HashMap<String, String> map = new HashMap<>(0x20);
    private int numHits;
    private int numMisses;

    protected String getCached(String desc) {

        String v = map.get(desc);
        if (null != v) {
            ++numHits;
            return v;
        } else {
            ++numMisses;
            return add(desc);
        }
    }

    private String add(String desc) {

        assert(null == map.get(desc));
        String value = getValue(desc);
        map.put(desc, value);
        return value;
    }

    protected abstract String getValue(String desc);

    @Override protected void finalize() throws Throwable {
        if (logCacheUsage && 0 != numMisses) {
            System.out.printf("SignatureAppender cache: %d hits, %d misses%n", numHits, numMisses);
        }

        super.finalize();
    }
}
