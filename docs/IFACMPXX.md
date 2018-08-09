# Implementation of IF_ACMPEQ/IF_ACMPNE opcodes:

### Simplified pseudocode
* If none of the 2 types are recognized as Scalar Value Type, standard reference comparison is used. This also covers comparing VT arrays or vt array and an object.
* If both sides are a Value Type and their Value Type class is different, comparison will be hardcoded as "not equal"
* If any of the sides is recognized as null (normal null or VT null constant), then the comparison is converted into explicit null check of the non-null argument. Method `ValueTypeUtils.isNull(long)` is called for a scalar, non-boxed Value Type argument, otherwise normal null comparison is used (IFNULL/IFNONNULL opcode)
* If none of the cases above is chosen, issue a warning about reference comparison changed to value comparison
* If none of the 2 types is a non-boxed Value Type, replace the comparison with call to `bool ValueType.isIdentical(ValueType, Object)`.
* If one of the 2 types is a non-boxed Value Type, try to unbox the other side object as the same ValueType by calling the unboxing method.
* Finally, replace the comparison with `ValueTypeUtils.isIdentical(long, long)` or `ValueTypeUtils.isIdentical(long, Object)` depending on the type of the other argument

### Following hardcoded method names are expected to be defined by the user
* `static bool ValueTypeUtils.isNull(long)` - Compares ValueType Value with `null`, used if one of the arguments is identified as null constant and the opcode is processed as ISNULL/IFNONNULL
* `static bool ValueTypeUtils.isIdentical(long, long)` - Compares a nullable ValueType Value to another ValueType Value
* `static bool ValueTypeUtils.isIdentical(long, Object)` - Compares a nullable ValueType Value to an Object (possibly a boxed Value Type)
* `static bool ValueType.isIdentical(ValueType, Object)` - Compares a boxed nullable ValueType Object with arbitrary Object (possibly another boxed ValueType)

`ValueType` stands for user-defined Value Type class, `ValueTypeUtils` stands for user-defined utility class for `ValueType`.
User is responsible for the methods described above having the same semantics, and being exception-safe, regardless of if the ValueType appearing as value or as reference in any given context.
Comparison is assumed to be commutative, argument order may be swapped as necessary.