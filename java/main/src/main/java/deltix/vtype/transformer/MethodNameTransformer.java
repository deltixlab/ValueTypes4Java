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

import deltix.vtype.common.CachedStringConverter;

public class MethodNameTransformer extends CachedStringConverter {

    private boolean isIgnored(String name) {
        // We can't change constructor names due to weird errors in the test with inner classes
        // We will also ignore lambdas
        return      name.hashCode() == "<init>".hashCode()
                ||  name.indexOf('$') > 0 && name.contains("lambda$");
    }

    public String transform(String name, String desc) {

        return isIgnored(name) ? name : name + super.getCached(desc);
    }

    @Override
    protected String getValue(String desc) {
        return "$VT$" + Integer.toHexString(desc.hashCode());
    }

    // Just a helper
    public String transformIf(boolean b, String name, String desc) {
        return !b ? name : transform(name, desc);
    }
}
