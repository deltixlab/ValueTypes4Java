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

import deltix.vtype.mapping.ClassDef;
import deltix.vtype.mapping.Mapping;
import deltix.vtype.mapping.MappingReader;
import org.objectweb.asm.*;

import java.io.*;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ASM6;


class ClassFileTransformer implements java.lang.instrument.ClassFileTransformer {
    // Limit processing to a single class is this field is not null

//    public void setCurrentlyLoadedClass(String currentlyLoadedClass) {
//        this.currentlyLoadedClass.set(currentlyLoadedClass);
//    }

    AtomicReference<String> currentlyLoadedClass = new AtomicReference<>();
    AtomicReference<ClassDef> currentClassDef = new AtomicReference<>();


    private static final String defaultConfigFilePath = "valuetypes.json";
    static private Mapping mapping;
    private boolean initialized;
    private int numFailedClasses;
    private int numFailedMethods;
    private PrintWriter fileLogger;
    static private final Object lockObj = new Integer(1);
    //static private final Semaphore firstFullTransform = new Semaphore(1);
    //private volatile boolean firstFullTransformTransformOccured = false;

    ClassFileTransformer(String configPath) throws ClassNotFoundException, IOException {

        initialized = false;
        numFailedClasses = numFailedMethods = 0;
        configPath = null != configPath ? configPath : defaultConfigFilePath;
        System.out.printf("VT Agent: Begin reading transformation config: %s\n", configPath);
        synchronized (lockObj) {
            if (null == mapping) {
                mapping = MappingReader.parse(configPath);
            }
        }

        System.out.println("VT Agent: End reading transformation config");
    }

    PrintWriter getLogFile() {

        PrintWriter out = this.fileLogger;
        if (null == out) {
            try {
                out = this.fileLogger =  new PrintWriter("valueTypeAgent.log");
            } catch (Throwable e) {
                System.out.println("Value Type agent is unable to open log file:");
                e.printStackTrace();
                return null;
            }
        }

        return out;
    }


    synchronized void onFailedClass(final String className) {

        ++numFailedClasses;
        PrintWriter out = getLogFile();
        if (null == out)
            return;

        out.println("FAILED class # " + numFailedClasses + ": " + className);
        out.flush();
    }


    synchronized void onFailedMethod(final String methodName) {

        ++numFailedMethods;
        PrintWriter out = getLogFile();
        if (null == out)
            return;

        out.println("FAILED method # " + numFailedMethods + ": " + methodName);
        out.flush();
    }


    @Override
    public byte[] transform(ClassLoader loader, final String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        // classDef of ValueType class currently being transformed, if transforming a ValueType class
        ClassDef classDef = null;
        try {
            if (null == mapping) {
                System.err.print("VT Agent: ERROR: mapping is NULL!! ");
                return null;
            }

            if (mapping.logEveryClass) {
                System.out.print("VT Agent: Asked to transform class: " + className);
            }

            synchronized (lockObj) {
                classDef = currentClassDef.get();

                if (null != classDef && (null == className || !(className.equals(classDef.getSrcClassPath()) || className.equals(classDef.getDstClassPath())))) {
                    classDef = null;
                }

                if (!initialized && null == classDef) {
                    if (mapping.logEveryClass) {
                        System.out.print(", Not Initialized - Will Ignore ");

                    }
                    else if (mapping.isLoggedClass(className)) {
                        System.out.println("VT Agent: Not Initialized - Will ignore class: " + className);
                    }

                    return null;
                }

//                if (!firstFullTransformTransformOccured) {
//                    if (!firstFullTransform.tryAcquire()) {
//                        System.out.println();
//                        System.out.println("Blocked on another thread doing transformation");
//                        firstFullTransform.acquire();
//                        firstFullTransform.release();
//
//                        assert(firstFullTransformTransformOccured == true);
//                        //if (mapping.logEveryClass) {
//                            System.out.print("Waited for another thread doing 1st transformation");
//                        //}
//                    }
//                }
            }

            // TODO: lambdas not processed
            if (null == className) {
                if (mapping.logEveryClass) {
                    System.out.print("Processing anonymous class..");
                }
            }

            if (mapping.isLoggedClass(className)) {
                System.out.printf("VT Agent: Loading class: %s%n", className);
            }

            // Skip some classes we are obviously not going to process
            if (null != className && (className.startsWith("java/")
                    || className.startsWith("sun/")
                    || className.startsWith("org/gradle/")
                    || className.startsWith("com/google/")
                    || className.startsWith("com/sun/")
                    || className.startsWith("deltix/vtype/transformer/")
                    || mapping.isIgnoredClass(className))
                    )
                return null;

            if (null == classDef && mapping.isMappedClass(className)) {
                if (mapping.logEveryClass) {
                    System.out.println(", VType class already defined, ignored(not rescanned)");
                }

                return null;
            }

            if (mapping.logEveryClass) {
                System.out.print(", Creating ClassReader ");
            }

            final ClassReader cr = new ClassReader(classfileBuffer);

            if (mapping.useQuickScan && null == classDef/* Use QuickScan only if not src/dst class */) {
                QuickScanClassVisitor scanner = QuickScanClassVisitor.findVt(cr, mapping);
                if (!scanner.foundVType()) {
                    if (mapping.logEveryClass) {
                        System.out.print(", VType NOT Found! ");
                    }

//                    if (null == className) {
//                        // Log all anon classes for research
//                        dumpClassData("$anon$/", className, classfileBuffer);
//                    }

                    return null;
                }

                // Debug code
//            if (scanner.foundVType()) {
//                if (mapping.logEveryClass) {
//                    System.out.printf("VT Agent: Skipping (for debug) class: %s%n", classPath);
//                }
//
//                return null;
//            }
            }

            if (mapping.logEveryClass) {
                System.out.println(", VType Found !");
            }


            // Will recompute local var & stack sizes
            // Our Class Visitor will remap frames itself, because ASM Frame remapping is not always able to pull class
            // inheritance tree leading to skipped classes. Also it is slower due to being more general.
            ClassWriter cw = new ClassWriter(cr, COMPUTE_MAXS);

            // TODO: Finish implementation of additional class verification
            //ClassVisitor prev = mapping.extraVerification ? new CheckClassAdapter(cw, true) : cw;
            org.objectweb.asm.ClassVisitor prev = cw;

            try {
                TranslationState state = new TranslationState(className, classDef, mapping);

                if (mapping.logEveryClass) {
                    System.out.printf("Start ClassReader for class: %s%n", className);
                }

                org.objectweb.asm.ClassVisitor cv = new ClassVisitor(ASM6, prev, state);
                cr.accept(cv, 0);

                boolean transformed = state.classWasTransformed;
                if (mapping.logEveryClass || mapping.logSuccesses && transformed) {
                    System.out.printf("VT Agent: Will %srewrite class: %s%n", transformed ? "" : "NOT ", className);
                }

                // Debug code that tries to avoid parallel processing of classes
//                if (transformed) {
//                    if (!firstFullTransformTransformOccured) {
//                        assert(firstFullTransform.availablePermits() == 0);
//                        firstFullTransformTransformOccured = true;
//                        firstFullTransform.release();
//                        System.out.println();
//                        System.out.println("VT Agent: First transformation succeeded for: " + classPath);
//                    }
//                }

                if (transformed) {
                    byte[] data = cw.toByteArray();
                    if (null != mapping.classDumpPath) {
                        dumpClassData(className, data);
                    }

                    return data;
                }

                return null;
            } catch (Throwable e) {

                if (e instanceof MethodException) {
                    MethodException mex = (MethodException) e;
                    String methodName = mex.getMethodFullName();
                    onFailedMethod(methodName);
                    System.err.printf("VT Agent: Exception occured while processing method: %s line %d%n", methodName, mex.getLine());
                    e = e.getCause();
                }

                onFailedClass(className);
                System.err.println("VT Agent: FAILED to transform class: " + className);

                e.printStackTrace();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

        } catch (Throwable e) {
            System.err.println("VT Agent: Exception!");
            e.printStackTrace();
        }

        finally {

//            if (!firstFullTransformTransformOccured) {
//                assert(firstFullTransform.availablePermits() == 0);
//                firstFullTransform.release();
//            }

            if (mapping.logEveryClass) {
                System.out.println(", Exited After Processing: " + className);
            }
        }

        return null;
    }

    private void dumpClassData(String className, byte[] data) {
        dumpClassData("", className, data);
    }

    private void dumpClassData(String pathPrefix, String className, byte[] data) {

        if (null == mapping.classDumpPath)
            return;

        if (null == className) {
            className = "$$lambda$$_" + Integer.toHexString(data.hashCode());

//            for (int i = 1; i < Integer.MAX_VALUE; ++i) {
//                File file = new File(String.format("%s/%s$$lambda$$_%d.class", mapping.classDumpPath, pathPrefix, i));
//                if (!file.exists()) {
//                    className = "$$lambda$$_" + i;
//                    break;
//                }
//            }
        }

        boolean again = false;
        String filePath = mapping.classDumpPath + "/" + (pathPrefix != null ? pathPrefix : "") + className;

        while(true) {
            try {
                File file = new File(filePath + ".class");
                FileOutputStream out = new FileOutputStream(file.getAbsoluteFile());
                out.write(data);
                out.close();
                break;
            } catch (Exception e) {
                if (again || !(e instanceof IOException || e instanceof  FileNotFoundException)) {
                    e.printStackTrace();
                    break;
                }
            }

            again = true;
            File dirPath = new File(filePath.substring(0, filePath.lastIndexOf('/')));
            if (!dirPath.exists()) {
                dirPath.mkdirs();
            }
        }
    }

    // Initialize loading of our Value Type classes before everything else
    // This will call our own transform() method for every such class, so we can scan it, extracting any data we need,
    // verify its signatures, etc.
    public void readClasses() {

        try {
            System.out.println("Begin scanning transformed classes (and loading dependencies)");
            mapping.loadClasses(currentlyLoadedClass, currentClassDef);
            System.out.printf("End scanning transformed classes. %d classes will be transformed.%n", mapping.numLoadedClasses());
        } catch (Exception e) {
            e.printStackTrace();
        }

        synchronized (lockObj) {
            initialized = true;
        }
    }
}
