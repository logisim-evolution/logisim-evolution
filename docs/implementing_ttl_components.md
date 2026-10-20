[![Logisim-evolution](img/logisim-evolution-logo.png)](https://github.com/logisim-evolution/logisim-evolution)

---

# Implementing TTL components #

* [« Go back](developers.md)
* **Implementing TTL components**
  * [Before writing code](#before-writing-code)
  * [Choose current examples](#choose-current-examples)
  * [Define the component](#define-the-component)
  * [Implement simulation and painting](#implement-simulation-and-painting)
  * [Register and document the component](#register-and-document-the-component)
  * [HDL support](#hdl-support)
  * [Test the implementation](#test-the-implementation)
  * [Validate the change](#validate-the-change)

---

This guide describes the repository-specific steps for adding a 74-series TTL component. It complements the general
[contribution rules](../.github/CONTRIBUTING.md) and does not replace the device manufacturer's data sheet.

## Before writing code ##

* Open an issue and agree on the component and expected scope before implementing it.
* Create the work on a branch from the current [`main`](https://github.com/logisim-evolution/logisim-evolution/tree/main).
* Select an authoritative manufacturer data sheet for the exact device variant being modelled. Verify the package pinout,
  truth table, active levels, clock edge, reset behavior, output type, and any unusual power-pin positions.
* Cite the data sheet in the component class documentation. If sources disagree, document which source and variant the
  simulation follows instead of silently combining them.

TTL simulation in Logisim-evolution is a digital functional model. Do not claim analog characteristics, propagation times,
fan-out limits, or electrical compatibility unless the simulator explicitly models them.

## Choose current examples ##

Use nearby implementations as patterns, but check every detail against the selected data sheet:

* [`Ttl7400`](../src/main/java/com/cburch/logisim/std/ttl/Ttl7400.java) is a compact stateless gate example.
* [`Ttl7493`](../src/main/java/com/cburch/logisim/std/ttl/Ttl7493.java) demonstrates a stateful counter, non-standard power
  pins, named logical port indexes, and clock metadata.
* [`Ttl74173`](../src/main/java/com/cburch/logisim/std/ttl/Ttl74173.java) demonstrates instance-owned register state,
  active-low controls, and three-state outputs.
* [`Ttl7493Test`](../src/test/java/com/cburch/logisim/std/ttl/Ttl7493Test.java) shows focused port-layout, power-pin,
  state-transition, reset, and unknown-output tests.

Prefer the smallest example with matching behavior. Older TTL classes may predate current conventions and should not be
copied without review.

## Define the component ##

Place the component in `com.cburch.logisim.std.ttl` and extend
[`AbstractTtlGate`](../src/main/java/com/cburch/logisim/std/ttl/AbstractTtlGate.java).

### Stable identifier ###

Declare a public `_ID` whose value is the established device number, for example `"7493"`. The identifier is serialized in
project files, so it must be unique within the library and must never be changed after release.

### Physical pins and logical ports ###

Pass physical, one-based data-sheet pin numbers to the `AbstractTtlGate` constructor:

* identify every output and input/output pin;
* identify unused pins so they do not become Logisim ports;
* provide tooltips in the same order as the resulting logical ports; and
* use the constructor overload with explicit VCC and GND numbers when the package does not use the conventional last-pin
  VCC and midpoint GND positions.

Logical ports are zero-based and follow physical pin order after unused and power pins are omitted. When the
**Enable Vcc and Gnd ports** attribute is enabled, GND and VCC are appended as the last two logical ports. Define named
logical-port constants or a reviewed physical-pin conversion helper; do not pass a physical pin number directly to
`InstanceState.getPortValue()` or `InstanceState.setPort()`.

The power-pin attribute defaults to hidden for compatibility. `AbstractTtlGate` verifies exposed power inputs and drives
declared output pins unknown when they are invalid, so focused tests must cover both hidden and exposed power modes.

## Implement simulation and painting ##

Implement the component's digital behavior in `propagateTtl(InstanceState)`:

* read all relevant control inputs before deciding the operation;
* preserve `Value.UNKNOWN`, error, and released-output behavior where required by the component contract;
* use `state.setPort(port, value, delay)` for outputs and keep delays consistent with comparable TTL models; and
* test active-low inputs, reset priority, enable/hold behavior, and clock edges explicitly rather than relying on names.

State belongs to the circuit instance, never to fields on the component factory. Retrieve or initialize an `InstanceData`
object through `InstanceState.getData()` and `InstanceState.setData()`. `TtlRegisterData` and `ClockState` provide common
storage and edge-detection behavior for sequential devices.

Implement `paintInternal()` only for the internal-structure view. Reuse `paintBase()` and the helpers in `Drawgates` where
they match the data sheet. Painting must not contain simulation state shared across component instances.

## Register and document the component ##

Complete all of these integration points in the same change:

1. Add a `FactoryDescription` in
   [`TtlLibrary`](../src/main/java/com/cburch/logisim/std/ttl/TtlLibrary.java). Keep the component order deliberate.
2. Add the `TTL<device-number>` display key and description to
   [`std.properties`](../src/main/resources/resources/logisim/strings/std/std.properties), then use the localization tools to
   add the corresponding key or untranslated placeholder to every `std_*.properties` bundle.
3. Add the device and its short description to the
   [TTL online-help overview](../src/main/resources/doc/en/html/libs/ttl/index.html). Refer users to the manufacturer's data
   sheet for detailed behavior and electrical information.
4. Add a concise user-visible entry under the current development section in [`CHANGES.md`](../CHANGES.md).

Translate only languages you can review. The English resource bundle remains the fallback for untranslated keys.

## HDL support ##

HDL generation is useful but is not required for every TTL simulator contribution. Pass `null` as the generator when HDL
support is outside the agreed issue scope.

When HDL support is included, keep it reviewable alongside the simulator contract, support the repository's applicable
VHDL and Verilog paths, and test generated ports and behavior. Sequential components may also need accurate
`checkForGatedClocks()` and `clockPinIndex()` metadata. Java tests of generated text do not by themselves prove that an
external synthesis tool accepts the result.

## Test the implementation ##

Add a focused test class under `src/test/java/com/cburch/logisim/std/ttl`. Cover the behaviors that apply to the device:

* logical port count, type, location, and data-sheet pin mapping;
* hidden and exposed VCC/GND modes, including non-standard power pins;
* truth-table rows or representative input combinations for combinational logic;
* clock edges, state transitions, reset priority, enables, loading, and hold behavior for sequential logic;
* unknown, error, open-collector, or three-state output behavior; and
* HDL metadata and generated behavior when HDL support is included.

Test through the public component and `InstanceState` paths. Avoid tests that merely repeat a private helper's
implementation.

## Validate the change ##

Run the focused test first, replacing the class name with the new test:

```bash
./gradlew test --tests com.cburch.logisim.std.ttl.Ttl7493Test
```

Then run compilation, Checkstyle, and the complete check suite:

```bash
./gradlew compileJava checkstyleMain checkstyleTest
./gradlew check
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`; in Command Prompt, use `gradlew.bat`. Also run
`git diff --check`, verify the new component manually in both power-pin modes, and confirm that the links and online-help
entry open correctly.
