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

import deltix.vtype.mapping.Mapping;
import deltix.vtype.mapping.MappingReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class MappingParserTest {
    private final String classPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    private final String path = classPath.substring(0, classPath.indexOf("/java/test") + 1) + "cfg/";
    private final String configFileName = "valuetype-tests.json";

    @Test
    public void parseMappingTest() throws IOException, URISyntaxException {
        //System.out.println(classPath);
        //System.out.println(path);
        Mapping mapping = MappingReader.parse(path + configFileName);
    }
}
