# ValueTypes4Java.
Java Agent classloader to implement transformation of operations on specific classes into operations on long integer values.

### Basic principles of operation
User supplies Value Type classes that contain a single `long` field and support few mandatory and an arbitrary amount of user-defined methods. User also supplies a corresponding utility class that contains static methods operating on `long` values.
With the help of the configuration file, Value Type Agent finds the correspondence between 'source' and 'implementation' class methods.
Value Type Agent attempts to transparently convert all operations on the source class instances into operations on `long` values using utility class methods, removing allocations while retaining type safety and support for overloading.
Transformed classes operate in a way similar to C# structs, but somewhat limited. They are immutable, passed by value, copied on assignment, compared only by value. They can be passed as arguments, returned, stored in fields and arrays. When necessary, Value Type is automatically boxed and unboxed to/from its reference implementation class. Arrays of Value Types are transformed into `long[]` and there is also limited support for Java 8 lambdas.
Warnings are generated if any redundant operations are generated during code translation, usually to support automatic boxing/unboxing in a multitude of contexts. Warnings try to dive a detailed description of the problem and offer the advice about how it may be avoided.
If the advice given in the warnings is followed, the generated code will be mostly the same as the manually written code that operates on `long`s. User should not expect Value Types to operate efficiently with Generic collections or with Java reflection facilities.

Current version of the Value Type Agent (0.9.2+) is considered to be in beta stage. It performs the work it is intended to do, but some corners were cut, significant amount of cruft has accumulated during the experimental phase of the development.
ASM framework is used to perform the code transformation. Efforts were made to ensure that class loading is not significantly slowed down by the VT Agent and the amount of generated garbage is reduced, but many optimization opportunities still remain.

### Installing and using the ValueType agent
* [Installation/Quick Start](docs/INSTALL.md)

###### Add agent jar and the JSON config file path to JVM options

`-javaagent:java/build/libs/deltix-vtype-0.9.1.jar=cfg/valuetypes.json`

Example configuration files are supplied with the project as reference.

For each class need to specify class name, implementation class name and the box/unbox methods.

Unboxing method can be static or not. If static, can expect null to be passed sometimes. Processing of null argument is user-defined. Should either throw or return long.

Boxing method should be a static method that takes long and returns an instance of ValueType class.

Methods can be individually described in the config file, in which case the implementation method can be individually chosen for each source method.
All used implementation methods should be static.

Mapping will be also deduced automatically via reflection from loaded ValueType and implementation classes. In that case all valuetype arguments turn into longs, and in come cases several methods may be mapped to a single implementation method. If such behavior is not desired, annotation should be used to change the implementation method name.

Extra methods in the implementation class that do not mirror source class methods are ignored during mapping process.

After processing, all classes referred in the config file exception may be thrown if unable to find implementation for all mapped methods or if another error occurs, such as inability to load class.


### JSON Configuration file format

##### Global settings (all are optional)

* `ignoreByDefault` : boolean, true - exclude all classes from transformation by default. When true, only classes marked with @ValueTypeTest are processed
           default : false

* `useQuickScan` : boolean, true - quickly scan classes for the presence of Value Types before executing the main pass. More efficient, no real downsides, so turned on by default and deprecated. Less intrusive, so when turned off, can uncover more bugs.
* `verifyAllMethods` : boolean, false - try mapping (but not modifying) all loaded methods/classes, including classes that don't use Value Types. This is a debug option.
* `logEveryClass` : boolean, false - log every class processed by the agent
* `logAllMethods` : boolean, false - log all transformed / verified methods of all loaded classes
           default : false
* `logSuccesses` : boolean, false - log the name of every succesfully transformed class that uses Value Types
* `extraVerification` : boolean, false - additional, more verbose class verification after transformation. For debugging, slower loading.
* `skipDebugData` : boolean, false - delete variable names debug data from transformed methods instead of transforming it as well
* `deleteAllDebugData` : boolean, false - delete variable names debug data from all methods. Not implemented.
* `logClasses` : array of strings - names of the listed classes wil be logged when transformed.
* `excludedClasses` : array of strings - listed classes will be excluded from transformation (ignored by the agent). They are not guaranteed to execute correctly, if call other, transformed, classes. Same as putting `@ValueTypeIgnore`
* `ignoreWarnings` : array of strings - list of globally ignored Value Type Agent warnings. Same as listing these warnings before _every_ method: `@ValueTypeSuppressWarnings({"Aaa", "Bbb", ...})`
* `logMethods` : array of strings - full names of methods, whose instructions will be logged during transformation. Same as putting `@ValueTypeTrace` before each
* `autoMethods` : array of strings - list of external methods that have overloaded versions that operate on both `Object` and `long`. Example: `"[LValueType; java/util/Arrays.copyOf([LValueType;I)"`, which means that `long[] copyOf(long[],int)` is called instead of `Object[] copyOf(Object[],int)` for any ValueType array. `LValueType;` is a "wildcard" ValueType class name
* `classDumpPath` : string - classpath for logging transformed classes to disk. Can be relative to the default dir. Only transformed classes are logged. Decompilers may fail to generate valid Java code from these even if it is actually valid for JVM.

##### Class mappings

* `mappings` : array of structures - describes every transformed Value Type class. May contain none.

##### Class mapping description structure
* `name`  : string - source/reference class path that implements ValueType without the agent.
* `implementation`  : string - implementation class path. Methods from this class are invoked after the transformation
* `box`  : string - a static method that converts a long (64-bit int) value to an instance of "Source" class. Reaction to invalid long value is user-defined
* `unbox`  : string - a static method that converts an instance of "Source" class to long (64-bit int).  Reaction to null argument is user-defined.
* `unboxArray`  : string - a static method that unboxes an array, converting `ValueType[]` to `long[]`
* `boxArray`  : string - a static method that boxes an array, converting `long[]` to `ValueType[]`
* `methodSuffix`  : string, optional - text suffix to append to all transformed non-static method names
* `staticMethodSuffix`  : string, optional - text suffix to append to all transformed static method names
* `methods`  : array of structures, optional - explicitly declares mapping for each transformed method. Deprecated. Current version is able to auto-match method names between source and destination class.


### Annotations

Prototypes for the annotations are declared in the package: `deltix.vtype` within VT javaagent jar.

Annotations only checked by name, so *no need to include any ValueType agent packages*, can declare anywhere, many times, if necessary.


##### Class annotations

* `@ValueTypeTest` - enable transformation for this class (if classes are _not_ transformed by default due to `ignoreByDefault : true`). This annotation enables transformation for a specific class.
* `@ValueTypeIgnore` - exclude this whole class from the transformation (can also be applied to individual methods)


##### Method annotations

* `@ValueType(impl="XX")`  - set the name of the implementation method for source class method. By default it matches source method's name. Can be used to assign a single trivial implementation method to several source methods.
* `@ValueTypeCommutative`  - mark implementation method as commutative to optimize stack operations usage. Optional&Currently unused/deprecated.
* `@ValueTypeSuppressWarnings({"Aaa", "Bbb", ...})` - list of ignored warnings for the method. names for the warnings are given in the warning texts.
* `@ValueTypeIgnore`  - can be used to ignore methods, excluding them from the transformation. Mostly for debugging.
* `@ValueTypeTrace`  - detailed logging of the chosen method during processing. For debugging. Same effect as adding the method name/path to "logMethods" array
* `@ValueTypeDebug`  - less detailed debug logging of the chosen method

### Additional implementation information

Value Type Agent
* [Implementation of IF_ACMPxx opcodes](docs/IFACMPXX.md)


### Version history:

* V0.9.2 OSS
  - Upgraded to use ASM 7.2.
  - Builds and tests with Java 11. Java 7 support remains, but needs Java 8+ to compile.
  - No substantial transformation code changes since 0.8.18 (yet)
  - Tests improved
* V0.9.1 OSS
  - will not try to transform already transformed class if a (broken?)classloader sends it again
  - Better support for using 2 or more different Value Types in same method signature
  - Method name suffixes not added to methods whose implementation names were overridden with `@ValueType(impl="XX")`
  - Suspected accessor methods are not renamed anymore. ValueType setter overrides Long setter by default.
  - All instance methods that return ValueType/VT array and take no args are considered getters and not renamed.
  - All instance methods that start with "set" followed by uppercase letter, take exactly 1 ValueType/VT array and return _anything_ are considered setters and are not renamed
  - In the case of transformed setter signature collision with setter that takes long/long array, first found method takes priority.
  - Warning is displayed when setter name collision occurs and redundant setter is deleted.
  - Bugfixes in local variable frame tracking
  - Bugfixes for Java 8 lambdas.
  - Cleanup

* V0.9 OSS
  - Released as Open Source project under Apache 2.0 license
