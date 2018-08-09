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

package deltix.vtype.mapping;

import deltix.vtype.transformer.Warnings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import static deltix.vtype.transformer.AsmUtil.classNameToPath;
import static deltix.vtype.transformer.AsmUtil.makeTypeDesc;
import static deltix.vtype.mapping.ClassDefFlags.*;

public class MappingReader {

    private String name;
    private String implementation;

    private MappingReader() {}

    class ParsedMethod {
        public String returnType;
        public String owner;
        public String name;
        public String descBody;
        public String desc;
        public String oldOwner;
        public String fieldName;

        ParsedMethod(String oldOwner) { this.oldOwner = oldOwner; }

        IOException fail(String fmt, Object ... args) {
            return new IOException(String.format(fmt, args));
        }

        IOException fail(String fmt, Object arg) {
            return fail(fmt, arg);
        }

        private int dotCount(String s) {
            int c = 0;
            for (int i = 0, n = s.length(); i < n; ++i) {
                if ('.' == s.charAt(i))
                    ++c;
            }

            return c;
        }

        void parse(String from) throws IOException {

            // https://stackoverflow.com/questions/225337/how-do-i-split-a-string-with-any-whitespace-chars-as-delimiters
            String[] parts1 = from.split("\\s+", 3);
            // parts1[0] == return value

            if (parts1.length != 2)
                throw fail("Return value separator not found: %s \nmust be delimited by whitespace", from);

            String body = parts1[1];
            int p0Pos = body.indexOf('(');
            int p1Pos = body.indexOf(')');
            int dotPos = body.lastIndexOf('.', p0Pos);
            if ((p0Pos | p1Pos) < 0 || p1Pos < p0Pos)
                throw fail("Unable to find '(' / ')' in the method definition: %s", body);

            if (p1Pos + 1 != body.length())
                throw fail("Method definition must end with ')': %s", body);

            if (dotPos > p0Pos)
                dotPos = -1;

            if (dotPos != -1) {
                this.owner = classNameToPath(body.substring(0, dotPos));
                this.name = body.substring(dotPos + 1, p0Pos);
            } else {
                this.owner = null;
                this.name = body.substring(0, p0Pos);
            }

            this.returnType = classNameToPath(parts1[0]); // Will also accept basic type names
            this.descBody = body.substring(p0Pos, p1Pos + 1);
            this.desc = descBody + (returnType.length() > 1 && !returnType.endsWith(";") && !returnType.startsWith("[")
                    ? makeTypeDesc(returnType) : returnType);
        }

        public MethodDef createSpecialMethodDef(ClassDef parent, String fieldName) {
            MethodDef mdef = new MethodDef(parent, owner, name, desc, fieldName.toUpperCase() + " " +  oldOwner);
            return mdef;
        }
    }

    private ParsedMethod parseMethod(final String str) throws IOException {
        ParsedMethod p = new ParsedMethod(this.name);
        p.parse(str);
        return null != p.returnType && null != p.name && null != p.descBody ? p : null;
    }

    private ParsedMethod parseMethod(final JSONObject json, String fieldName) throws IOException {

        String name = getOptionalString(json, fieldName, null);
        if (null == name)
            return null;

        ParsedMethod p = parseMethod(name);
        if(null == p)
            throw new IOException(fieldName + " method not defined properly");

        return p;
    }

    private void parseDebugOptions(Mapping mapping, JSONObject json) {

        JSONArray logMethods = getOptionalArray(json,"logMethods");
        if (isNonEmptyStringArray(logMethods)) {
            for (Object i : logMethods) {
                mapping.addLoggedMethod((String)i);
            }
        }

        JSONArray logClasses = getOptionalArray(json,"logClasses");
        if (isNonEmptyStringArray(logClasses)) {
            for (Object i : logClasses) {
                mapping.loggedClasses.add(((String)i).replace('.', '/'));
            }
        }

        JSONArray excludedClasses = getOptionalArray(json,"excludedClasses");
        if (isNonEmptyStringArray(excludedClasses)) {
            for (Object i : excludedClasses) {
                mapping.ignoredClasses.add((String)i);
            }
        }

        JSONArray ignoreWarnings = getOptionalArray(json,"ignoreWarnings");
        mapping.ignoredWarnings  = 0;
        if (isNonEmptyStringArray(ignoreWarnings)) {
            Warnings w = new Warnings();
            for (Object i : ignoreWarnings) {
                w.suppress((String)i);
            }

            mapping.ignoredWarnings = w.getIgnoreMask();
        }

        mapping.ignoreByDefault     = getOptionalBool(json, "ignoreByDefault", false);
        mapping.verifyAllMethods    = getOptionalBool(json, "verifyAllMethods", false);
        mapping.logAllMethods       = getOptionalBool(json, "logAllMethods", false);
        mapping.logEveryClass       = getOptionalBool(json, "logEveryClass", false);
        mapping.logSuccesses        = getOptionalBool(json, "logSuccesses", true) || mapping.logEveryClass || mapping.logAllMethods;
        mapping.extraVerification   = getOptionalBool(json, "extraVerification", false);
        mapping.useQuickScan        = getOptionalBool(json, "useQuickScan", true);
        mapping.noDebugData         = getOptionalBool(json, "skipDebugData", false);
        mapping.deleteAllDebugData  = getOptionalBool(json, "deleteAllDebugData", false);
        mapping.classDumpPath       = getOptionalString(json, "classDumpPath", null);
    }


    private Mapping parseFile(String configFilePath) throws IOException {

        Mapping m = new Mapping();
        FileReader reader = new FileReader(configFilePath);
        JSONTokener jsonTokener = new JSONTokener(reader);
        JSONObject json = new JSONObject(jsonTokener);
        parseDebugOptions(m, json);

        JSONArray mappings = json.getJSONArray("mappings");
        int length = mappings.length();
        for(int i = 0; i < length; ++i) {
            ClassDef c = parseClass(mappings.getJSONObject(i), i, m);
        }

        JSONArray autoMethods = getOptionalArray(json,"autoMethods");
        if (isNonEmptyStringArray(autoMethods)) {
            for (Object i : autoMethods) {
                addAutoMethod(m, (String)i);
            }
        }

        return m;
    }

    private void addAutoMethod(Mapping mapping, String methodSignature) throws IOException {

        ParsedMethod method = parseMethod(methodSignature);
        HashMap<Integer, MethodDef> mm = mapping.getMethodMap(method.owner);
        if (null == mm) {
            mm = new HashMap<>();
            mapping.addMethodMap(method.owner, mm);
        }

        // Currently automethods can only be static and have same name/owner before/after transform.
        // TODO: Would be nice to add more flexible template definition later
        MethodDef methodDef = new MethodDef(method.name, method.desc, method.owner, true, method.name,
                method.desc.replace("LValueType;", "J"), method.name);

        Mapping.addMethod(mm, methodDef);
    }


    public static Mapping parse(String configFilePath) throws IOException {
        MappingReader reader = new MappingReader();
        return reader.parseFile(configFilePath);
    }

    private ClassDef parseClass(final JSONObject json, int classIndex, final Mapping m) throws IOException {

        assert(null != json);
        name =  classNameToPath(json.getString("name"));
        implementation = classNameToPath(json.getString("implementation"));

        final ClassDef cl = new ClassDef(m, classIndex, name, implementation);

        cl.setMethodSuffix(getOptionalString(json, "methodSuffix", ""));
        cl.setStaticMethodSuffix(getOptionalString(json, "staticMethodSuffix", ""));

        // Exclude our classes from Value Type transformations
        // TODO: Maybe we will need one Value Type class to use another in its implementation
        m.mappedClasses.add(name);
        m.mappedClasses.add(implementation);

        m.addClass(cl);

        ParsedMethod box = parseMethod(json, "box");
        ParsedMethod unbox = parseMethod(json, "unbox");
        ParsedMethod boxArray = parseMethod(json, "boxArray");
        ParsedMethod unboxArray = parseMethod(json, "unboxArray");

        cl.setFlag(F_SRC_BOX_METHOD_DEFINED | F_DST_BOX_METHOD_DEFINED
                | F_SRC_UNBOX_METHOD_DEFINED | F_DST_UNBOX_METHOD_DEFINED);

        
        MethodDef boxMethod = cl.boxingMethod = box.createSpecialMethodDef(cl, "BOX");
        MethodDef unboxMethod = cl.unboxingMethod = unbox.createSpecialMethodDef(cl, "UNBOX");
        MethodDef isNullMethod = cl.isNullMethod = new MethodDef(cl, implementation, "isNull", "(J)Z", "ISNULL" + " " +  box.oldOwner);

        if (null != boxArray) {
            cl.arrayBoxingMethod = boxArray.createSpecialMethodDef(cl, "BOXARRAY");
        }

        if (null != unboxArray) {
            cl.arrayUnboxingMethod = unboxArray.createSpecialMethodDef(cl, "UNBOXARRAY");
        }

        // TODO: Verify signatures that box/unbox methods are expected to have

        JSONArray methods = null;

        try {
            methods = json.getJSONArray("methods");
        }
        catch (JSONException e) {
            if (!e.getMessage().endsWith("not found."))
                throw e;
        }


        if (null != methods) {
            int length = methods.length();

            m.mapClassMethods(cl);
            for (int i = 0; i < length; ++i) {
                parseMethodMapping(methods.getJSONObject(i), cl);
            }

            cl.setFlag(F_SRC_ALL_METHODS_DEFINED | F_DST_ALL_METHODS_DEFINED);
        }

        return cl;
    }

    static String getOptionalString(JSONObject json, String name, String defaultValue) {
        try {
            return json.getString(name);
        }
        catch (JSONException e) {
            if (!e.getMessage().endsWith("not found."))
                throw e;

            return defaultValue;
        }
    }

    static int getOptionalInt(JSONObject json, String name, int defaultValue) {
        try {
            return json.getInt(name);
        }
        catch (JSONException e) {
            if (!e.getMessage().endsWith("not found."))
                throw e;

            return defaultValue;
        }
    }

    static boolean getOptionalBool(JSONObject json, String name, boolean defaultValue) {
        try {
            return json.getBoolean(name);
        }
        catch (JSONException e) {
            if (!e.getMessage().endsWith("not found."))
                throw e;

            return defaultValue;
        }
    }

    static JSONArray getOptionalArray(JSONObject json, String name) {
        try {
            return json.getJSONArray(name);
        }
        catch (JSONException e) {
            if (!e.getMessage().endsWith("not found."))
                throw e;

            return null;
        }
    }

    boolean isNonEmptyStringArray(JSONArray array) {

        int n;
        if (null == array || 0 == (n = array.length()))
            return false;

        for (int i = 0; i < n; i++) {
            if (!(array.get(i) instanceof String))
                return false;
        }

        return true;
    }

    private MethodDef parseMethodMapping(JSONObject json, ClassDef owner) throws IOException {
        ParsedMethod from = parseMethod(json, "from");
        ParsedMethod to = parseMethod(json, "to");
        boolean isStatic = 0 != json.getInt("static");
        boolean isCommutative = 0 != getOptionalInt(json, "commutative", 0);

        MethodDef mdef = new MethodDef(owner, from.name, from.desc, isStatic, isCommutative,
                owner.getDstClassPath(), to.name, to.desc, from.name + from.desc);

        owner.addMethod(mdef);
        return mdef;
    }
}
