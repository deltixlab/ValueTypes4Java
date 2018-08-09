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

import deltix.vtype.type.TypeId;
import deltix.vtype.type.TypeIdDefaultFormatter;
import deltix.vtype.type.TypeIdFormatter;

import static deltix.vtype.type.TypeId.*;
import static deltix.vtype.mapping.ClassDefFlags.*;
import static deltix.vtype.transformer.AsmUtil.classPathToName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;


public class Mapping implements TypeIdFormatter {
    private static final int initialClassCapacity = 2;

    HashMap<String, HashMap<Integer, MethodDef>> methodMaps = new HashMap<>(initialClassCapacity);
    ArrayList<ClassDef> classes                             = new ArrayList<>(initialClassCapacity);
    // TODO: Optimization: access by hash taken from char range
    HashMap<String, ClassDef> classesMap                    = new HashMap<>(initialClassCapacity);
    HashSet<String> mappedClasses                           = new HashSet<>(initialClassCapacity * 2);
    HashSet<String> ignoredClasses                          = new HashSet<>(initialClassCapacity * 2);
    HashSet<String> loggedClasses                           = new HashSet<>(initialClassCapacity * 2);

    // Contains full method name including class path, excluding signature
    HashSet<String> loggedMethods                          = new HashSet<>(initialClassCapacity * 2);

    // Configuration flags, not directly related to Mapping, but reside here for a while
    // Later may be moved to separate config class if we succesfully avoid adding extra arg to the methods that use it
    public boolean ignoreByDefault;
    public boolean verifyAllMethods;
    public boolean logAllMethods;
    public boolean logEveryClass;
    public boolean logSuccesses;

    private int numLoggedMethods;
    public boolean extraVerification;
    public boolean useQuickScan;
    public long ignoredWarnings;            // Bit set for the list of ignored warnings
    public boolean noDebugData;             // Delete debug data for transformed classes instead of trying to process it
    public boolean deleteAllDebugData;      // Delete debug data for processed an umprocessed classes
    public String classDumpPath;

    public HashMap<Integer, MethodDef> getMethodMap(String className) {
        return methodMaps.get(className);
    }

    static public MethodDef getMethod(HashMap<Integer, MethodDef> map, String name, String desc) {
        return map.get(combineKeys(name, desc));
    }

    static int combineKeys(String a, String b) {
        return 0x1B3 * a.hashCode() + b.hashCode();
    }

    static void addMethod(HashMap<Integer, MethodDef> methodMap, MethodDef method) {
        methodMap.put(combineKeys(method.oldName, method.oldDesc), method);
    }

    public ClassDef getClassDef(int index) {
        return classes.get(index);
    }

    public ClassDef getClassDefById(int typeId) {

        return TypeId.isVt(typeId) ? classes.get(TypeId.getVtClassIndex(typeId)) : null;
    }

    public ClassDef getClassDef(String classPath) {
        return classesMap.get(classPath);
    }

    public int getClassTypeId(String classPathSrc, int from, int to) {
        throw new UnsupportedOperationException("Not implemented yet");
        // TODO: OPT: Implement range-based allocation-free class search. Just an extra perf optimization to be considered
        //ClassDef c = classesMap.get(classPath);
        //return null == c ? TypeId.REF : c.getTypeId();
        //return TypeId.VOID;
    }

    /**
     * Return typeId from a class path (no prefixes or array notation expected)
     * TypeId.REF is returned for unregistered classes
     * @param classPath
     * @return
     */
    public int getClassTypeId(String classPath) {
        ClassDef c = classesMap.get(classPath);
        return null == c ? TypeId.OBJ_REF : c.getTypeId();
    }

    /**
     * Return typeId for an array of a class for a class path (no prefixes or array notation expected in the specified string)
     * TypeId.REF is returned for unregistered classes
     * @param classPath
     * @return
     */
//    public int getClassArrayTypeId(String classPath) {
//        ClassDef c = classesMap.get(classPath);
//        return null == c ? TypeId.REF : c.getArrayTypeId();
//    }

    public int getClassArrayTypeId(String classPath, int arrayDepth) {
        ClassDef c = classesMap.get(classPath);
        return null == c ? refOrArrayFrom(arrayDepth) : arrayFrom(c.getTypeId(), arrayDepth);
    }

    // TODO: This amount of parsing methods looks redundant and misplaced, need cleanup

    // Returns 0=VOID for basic types, TypeId.REF for non-VType objects and any non-VType arrays
    public int getVTypeIdFromDesc(String desc) {

        int arrayDepth = 0;
        int i = 0;
        int n = desc.length();

        // Basic type? We don't expect these and return VOID
        if (n < 2)
            return TypeId.VOID;

        if ('[' != desc.charAt(0)) {
            // Not array, just a class name
            return getClassTypeId(desc);
        }

        // Array!
        do {
            ++arrayDepth;
            ++i;
        } while (i < n && desc.charAt(i) == '[');

        if (desc.charAt(i) != 'L')
            return TypeId.OBJ_REF;

        //--n; // Remove ';' at the end
        //++i; // Remove 'L' at the beginning
        return getClassArrayTypeId(desc.substring(i + 1, n - 1), arrayDepth);
    }

    public boolean isMappedSrcClass(String classPath) {
        return mappedClasses.contains(classPath) && null != classesMap.get(classPath);
    }

    public boolean isMappedDstClass(String classPath) {
        return mappedClasses.contains(classPath) && null == classesMap.get(classPath);
    }

    public boolean isMappedClass(String classPath) {
        return mappedClasses.contains(classPath);
    }

    public boolean isIgnoredClass(String classPath) {
        return ignoredClasses.contains(classPath);
    }

    public boolean isLoggedClass(String classPath) {
        return loggedClasses.contains(classPath);
    }

    Mapping() {

    }


    public void loadClasses(AtomicReference<String> currentlyLoadedClass, AtomicReference<ClassDef> currentClassDef) throws Exception {


        for(ClassDef classDef : classes) {
            if (classDef.isInitialized())
                continue;

            String classPath = classDef.getSrcClassPath();
            currentlyLoadedClass.set(classPath);
            currentClassDef.set(classDef);
            Class.forName(classPathToName(classPath));
            classDef.setFlag(F_SRC_CLASS_PROCESSED);
        }

        for(ClassDef classDef : classes) {
            if (classDef.isInitialized())
                continue;

            String classPath = classDef.getDstClassPath();
            currentlyLoadedClass.set(classPath);
            currentClassDef.set(classDef);
            Class.forName(classPathToName(classPath));
            classDef.setFlag(F_DST_CLASS_PROCESSED);

            // We assume all declared methods are read already
            classDef.mapClassMethods();
            classDef.loadNullValue();
            if (!classDef.isInitialized())
                throw new IllegalStateException("Failed to completely initialize ValueType class: " + classDef.getSrcClassPath());
        }

        currentlyLoadedClass.set(null);
        currentClassDef.set(null);
    }

    public int numLoadedClasses() {

        int n = 0;
        for(ClassDef classDef : classes) {
            if (classDef.isInitialized())
                ++n;
        }

        return n;
    }


    public int numClasses() {

        return classes.size();
    }


    public void addMethodMap(String ownerClassPath, HashMap<Integer, MethodDef> methodMap) {
        methodMaps.put(ownerClassPath, methodMap);
    }


    public void mapClassMethods(ClassDef cl) {
        addMethodMap(cl.getSrcClassPath(), cl.methodMap);
    }


    public void addClass(ClassDef cl) {

        if (cl.getClassIndex() != classes.size())
            throw new InvalidParameterException("classIndex must match collection end index");

        classes.add(cl);
        classesMap.put(cl.getSrcClassPath(), cl);
    }


    public void addLoggedMethod(String name) {

        ++numLoggedMethods;
        loggedMethods.add(name);
    }

    public boolean isMetodLogged(String classPath) {
        return loggedMethods.contains(classPath);
    }

    public int getNumLoggedMethods() {
        return numLoggedMethods;
    }

    @Override
    public String typeIdToShortPrintableString(int typeId) {
        String out;
        if (isVt(typeId)) {
            ClassDef cl = getClassDef(TypeId.getVtClassIndex(typeId));
            assert(null != cl);
            out = isVtRef(typeId) ? cl.getSrcShortRefClassPath() : cl.getSrcShortClassPath();
        } else {
            out = TypeIdDefaultFormatter.makeBasicPrintableString(typeId);
        }

        if (isArray(typeId)) {
            out = TypeIdDefaultFormatter.appendReadableArrayDepth(new StringBuffer(out), getArrayDepth(typeId)).toString();
        }

        return out;
    }

    @Override
    public String typeIdToSrcTypeDesc(int typeId) {

        String out;
        if (isVt(typeId)) {
            ClassDef cl = getClassDef(TypeId.getVtClassIndex(typeId));
            assert(null != cl);
            out = cl.getSrcClassDesc();
        } else {
            out = TypeIdDefaultFormatter.makeBasicTypeDescriptor(TypeId.getArrayBaseElement(typeId));
        }

        if (isArray(typeId)) {
            int depth = getArrayDepth(typeId);
            out = TypeIdDefaultFormatter.prependArrayDepth(out, depth).toString();
        }

        return out;
    }

    @Override
    public String typeIdToDstTypeDesc(int typeId) {

        if (isVtRef(typeId)) {
            return typeIdToSrcTypeDesc(typeId);
        }

        return TypeIdDefaultFormatter.makeDstDescriptor(typeId);
    }
}
