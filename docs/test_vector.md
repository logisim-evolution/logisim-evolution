# Test Vector Feature

The Test Vector feature allows you to verify your circuits by running automated tests defined in text files.
This document describes the enhanced Test Vector functionality, including sequential testing and special values.

## Overview

The Test Vector window allows you to load a test vector from a file, and Logisim will automatically run tests on the current circuit.
The Test Vector module runs a separate copy of the circuit simulator, so it does not interfere with the simulation in the project window.

Any incorrect outputs will be flagged in red. Hover the mouse over the red box to see what the output should have been,
according to the test vector. Rows with incorrect outputs are sorted to the top of the window.

## Basic File Format

The file format is simple. You can use the Logging module (with "Include Header Line" selected in the file output tab) to get started,
since in most cases the Logging module outputs the same format as used by the Test Vector module.

Here is an example test vector file:

```txt
# my test vector for add32
A[32] B[32] C[32] Cin Cout
00000000000000000000000000000000        00000000000000000000000000000000        00000000000000000000000000000000        0       0
-2       0x00000005    3       0       0
0        0o0003        3       0       0
```

**Format Rules:**

- Blank lines are ignored
- Anything following a '#' character is a comment
- The first non-blank, non-comment line lists the name of each circuit input/output pin and its width (if > 1), separated by whitespace
- The remaining lines list each value separated by whitespace

**Value Formats:**

- Values can be in hex, octal, binary, or signed decimal
- Hex values must have a '0x' prefix
- Octal values must have a '0o' prefix
- Binary and decimal are distinguished by the number of digits:
  - Binary values must always have exactly as many digits as the width of the column
  - Decimal values must always have fewer digits, should not have leading zeros, and may have a negative sign
- **Underscores for readability**: You can use underscores (`_`) anywhere in numeric values to improve readability.
  Underscores are ignored during parsing. Examples:
  - `0x0000_1111` (hex)
  - `0o1234_5670` (octal)
  - `1111_0000` (binary)
  - `1_234` or `-5_000` (decimal)

**Don't Care Bits:**

- For hex, octal, and binary values, a digit of 'x' specifies four, three, or one "don't care" bits
- Example: `101xx` is a five bit binary value with the last two bits unspecified
- Example: `0x1ax5` is a hex value with four unspecified bits
- Such "don't cares" cannot be used in decimal notation

## Sequential Testing

The Test Vector feature supports both **combinational** and **sequential** circuit testing.

### Combinational Tests (Default)

By default, tests are combinational: the circuit is reset before each test, ensuring each test is independent.
This is the traditional behavior and works for all circuits.

### Sequential Tests

For sequential circuits (like counters, state machines, etc.), you can specify test sequences using special header columns:

- **`<set>`**: Defines the **sequence ID** - tests with the same set number belong to the same sequence and run together.
The circuit state is preserved between steps in the same set. Tests default to set 0 if not specified.
- **`<seq>`**: Defines the **step number** within a set - this determines the execution order of tests within the same set.
Tests are executed in order of their `<seq>` value within each `<set>`.
Tests with `<seq>` of 0 or missing are treated as combinational (circuit resets between tests, even if they share the same set).

**Example of Sequential Test:**

```txt
# Sequential test for a counter
Clock Reset Count <set> <seq>
0     0     0     1     1
1     0     0     1     2
0     0     1     1     3
1     0     1     1     4
0     0     2     1     5
1     0     2     1     6
0     1     0     2     1
```

In this example:

- The first six tests all have `<set> 1` with `<seq>` values 1-6,
so they form one sequence that runs in order (seq 1, then 2, then 3, etc.) without resetting between steps
- The last test has `<set> 2`, so it starts a new sequence and the circuit is reset before it runs

**Sequence Execution Rules:**

- Tests with the same `<set>` number belong to the same sequence
- Within each set, tests are executed in order of their `<seq>` value (seq 1, then 2, then 3, etc.)
- The circuit state is maintained between tests in the same set (same sequence ID)
- The circuit is reset when starting a new set (different `<set>` number)
- Tests with `<seq>` = 0 or missing are always combinational (reset between tests, even within the same set)

## Special Values

The Test Vector format supports two special values for more flexible testing:

### Don't Care (`<DC>`)

When used for an **output pin**, this value always passes regardless of the actual output.
This is useful when you don't care about certain outputs during a test.

- Case-insensitive: `<DC>`, `<dc>`, `<Dc>` all work
- Only applies to output pins (inputs still need explicit values)

**Example:**

```txt
Input Enable Output <set> <seq>
1      1      <DC>   0     0
```

In this test, any output value will pass because it's marked as don't care.

### Floating (`<float>`)

When used for an **input pin**, this drives a floating (high-impedance) value.
When used for an **output pin**, this expects the output to be floating (UNKNOWN value).

- Case-insensitive: `<float>`, `<FLOAT>`, `<Float>` all work
- For inputs: drives Value.UNKNOWN (high-impedance state)
- For outputs: expects Value.UNKNOWN (high-impedance state)

**Example:**

```txt
Input Enable Output <set> <seq>
<float> 0      <float> 0     0
```

In this test:

- The input is driven as floating (high-impedance)
- The output is expected to be floating (high-impedance)

## Complete Example

Here is a complete example combining all features:

```txt
# Mixed combinational and sequential tests
A B C Out <set> <seq>
0 0 0 0   0     0
0 0 1 1   0     0
1 1 0 1   0     0
0 1 0 0   0     1
1 1 1 1   0     1
0 0 0 <DC> 0     1
1 0 1 <float> 0     2
```

**Explanation:**

- The first three tests are combinational (seq=0), so the circuit resets between each test
- The next three tests form sequence 1, so they run sequentially without reset
- The last test starts sequence 2, so the circuit resets before it runs
- The sixth test uses `<DC>` for the output, so any output value passes
- The last test uses `<float>` for the output, expecting a floating value

## Command Line Usage

To facilitate automated testing, the test vector feature can be run from the command line:

```bash
logisim --test-vector <circuit_name> <test_vector_file> <project.circ>
```

Or using the JAR file:

```bash
java -jar logisim-evolution.jar --test-vector <circuit_name> <test_vector_file> <project.circ>
```

**Arguments:**

- `<circuit_name>`: The name of the circuit to test (must match a circuit in the project file)
- `<test_vector_file>`: Path to the test vector file (e.g., `TestsDLatch.txt`)
- `<project.circ>`: Path to the Logisim project file containing the circuit

**Example:**

```bash
java -jar logisim-evolution.jar --test-vector dlatch TestsRegisterFile.txt /home/user/Computer.circ
```

The command will:

1. Load the specified circuit from the project file
2. Load and parse the test vector file
3. Run all tests (respecting sequential execution rules)
4. Print results showing passed and failed tests
5. Exit with status code 0 on success, non-zero on failure

## Backward Compatibility

All existing test vector files continue to work without modification. The new features are optional:

- If `<set>` and `<seq>` columns are not present, all tests are combinational (default behavior)
- If special values `<DC>` and `<float>` are not used, normal value comparison applies
- The original file format is fully supported

## Tips and Best Practices

1. **Use sequences for stateful circuits**: If your circuit has memory (flip-flops, registers, counters),
use sequential tests to verify state transitions
2. **Use don't care for partial verification**: When testing complex circuits, use `<DC>` for outputs you're not currently verifying
3. **Use floating for tri-state testing**: Use `<float>` to test circuits with tri-state outputs or high-impedance states
4. **Organize with sets**: Use the `<set>` column to group related tests, even though it doesn't affect execution
5. **Mix combinational and sequential**: You can mix combinational tests (seq=0) with sequential tests in the same file

## See Also

- [User's Guide](../src/main/resources/doc/en/html/guide/log/_test.html) - HTML documentation with additional examples
- [Test Vector Window](../src/main/resources/doc/en/html/guide/log/_test.html) - Detailed usage instructions
