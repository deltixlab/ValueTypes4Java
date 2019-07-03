## ValueTypes4Java installation/quick start

#### Location of the Value Type project
https://github.com/deltixlab/ValueTypes4Java

#### Obtaining Value Type Agent files
You need two files, .jar file, containing VT agent and a JSON config file, containing description of 0 or more classes, representing value types.

JAR file is built by `jar` Gradle task:
`gradlew jar`


Minimal configuration file for working with deltix Decimal Floating Point package (based on Intel DFP):
```
{
	"logSuccesses"          : false,

	"mappings" :
	[
		{
			"name" 			: "deltix/dfp/Decimal64",
			"implementation"	: "deltix/dfp/Decimal64Utils",
			"box"			: "deltix/dfp/Decimal64 deltix/decimal/Decimal64.fromUnderlying(J)",
			"unbox"			: "J deltix/dfp/Decimal64.toUnderlying(Ldeltix/dfp/Decimal64;)",
			"boxArray"		: "[Ldeltix/dfp/Decimal64; deltix/dfp/Decimal64Utils.fromUnderlyingLongArray([J)",
			"unboxArray"		: "[J deltix/dfp/Decimal64Utils.toUnderlyingLongArray([Ldeltix/dfp/Decimal64;)",

			"methodSuffix"		: "Checked",
			"staticMethodSuffix"	: ""
		}
	]
}
```

#### Adding Value Type Agent to your project
JAR file, containing the VT agent, is specified as a Java VM option, using `-javaagent` argument, and its config is given after `=` sign. Both paths can be absolute or relative. The path is usually relative to the project's root path.

##### Example for Idea(add to 'VM options' in Run/Debug configuration):
`-javaagent:lib\deltix-value-types-0.9.1-SNAPSHOT.jar=cfg/vt-decimal-min.json`

##### Example for Gradle:

`jvmArgs += '-javaagent:' + vt_agent_jar_path + '=' + vt_agent_config_path`

#### Logging and Diagnostics
Value Type Agent will log information and warnings to `System.out`/`System.err`. The sample configuration file specified above generates minimal amount of messages.

In the case of unrecoverable error exception trace will be printed to `System.err`. Exceptions are thrown on per-class basis and the corresponding class will be left untransformed.
If an exception is thrown, correct execution of user's application is usually not possible.