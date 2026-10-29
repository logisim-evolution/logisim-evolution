# Importing Verilog

Choose **Project > Import Verilog...** to synthesize Verilog into editable
Logisim-evolution circuits. The importer invokes an installed Yosys executable
and creates native gates, arithmetic components, registers, memories, splitters,
wires and subcircuits.

## Requirements and use

Install Yosys with Tcl support (`yosys -c`); the integration has been tested with
Yosys 0.66. Select its executable in the dialog, or use `yosys` if it is on PATH.
The executable setting is remembered.

1. Select one or more `.v` files. For `.sv` files, enable the SystemVerilog subset
   option. Source order is preserved; put packages and dependencies first.
2. Enter the top module, or leave it blank for Yosys to choose automatically.
3. Add include directories and simple macro definitions as needed, one per line.
   Definitions accept `NAME` or `NAME=VALUE`; complex string macros should be
   placed in a Verilog header instead. Relative source/include paths resolve
   against the chosen working directory. Relative `$readmemh`/`$readmemb` files
   also use that working directory.
4. Choose the undefined-value and memory policies, then click **Import**.
5. Close the dialog after success. The imported top circuit is selected; save
   the project normally. **Edit > Undo** removes the entire import in one step.

Synthesis runs in the background. The dialog reports each stage and recent
Yosys output. **Cancel import** stops the worker and its Yosys process. Conversion
errors and cancellation add no circuits to the project. A repeated import
creates uniquely named circuits instead of replacing existing circuits.

## Supported circuit behavior

The importer preserves module hierarchy, including independent state in each
instance. Ports and component buses must be 1–64 bits wide. Supported operations
include addition, subtraction, multiplication, division, modulo, Boolean logic, reductions,
comparisons, shifts, multiplexers, slices, concatenation, registers, latches,
and the supported reset/enable forms. Parameterized modules are elaborated by
Yosys before conversion.

Memory policies:

| Policy | Behavior |
| --- | --- |
| Native where compatible, otherwise logic | Uses native RAM/ROM where possible; expands remaining memories through Yosys. |
| Require native memories | Rejects a memory that cannot be represented directly. |
| Expand memories into logic | Asks Yosys to expand all memories into registers and logic. |

Native memories support asynchronous reads, one clocked write port, write masks,
multiple read ports, and initialized ROMs. Supported address offsets and bounds
are checked so an out-of-range access does not wrap around. Native data widths
are 1–64 bits and depths are at most 2^24 words. Large designs can still consume
substantial memory or produce large schematics.

The default undefined-value policy rejects explicit `x` values that need mapping.
Selecting zero or one gives those values a fixed binary meaning. Signal `z`
constants and disabled tri-state drivers remain high impedance under every policy.
This uses Logisim's floating and error states, rather than full Verilog four-state
and drive-strength semantics. Logisim registers and RAM start at zero. Nonzero initial register
values, including those produced by memory expansion, require a reset-based
design and are rejected.

Unsupported cells report the module, instance and cell type. Current exclusions
include black boxes, recursive hierarchy,
and other cells outside the supported mappings. Memory fallback is limited by
Yosys: for example, incompatible write clocks can remain unmapped and are then
rejected. SystemVerilog support is limited to the installed Yosys frontend.
There is no HDL co-simulation or automatic resynthesis when source files change.

## Tri-state and bidirectional signals

Conditional assignments such as `assign io = enable ? data : 'bz;` become native
Controlled Buffers. Multiple drivers share the same electrical net, including
overlapping bus slices. Disabled drivers release it; conflicting active values
produce Logisim's error value (`E`). Floating bits appear as unknown/floating in
Logisim's display. Explicit `z` signal constants are preserved, including mixed
vectors such as `4'b10zz`.

Logisim subcircuit pins have separate input and output directions. Each Verilog
`inout io` therefore becomes two pins: `io_in` samples the shared net, and
`io_out` carries only that module's drive contribution. Both have the original
bus width. When using the imported circuit as a subcircuit, connect both pins to
the shared external bus. Imported hierarchy makes these connections automatically.
When viewing the top circuit directly, its `io_in` pin is a tri-state input that
starts floating and can be poked or driven with an external value.

The resolved input is kept separate from the exported drive contribution so
releasing the last driver also releases the bus across nested instances.
Separate declared inout ports must not alias the same HDL bit; those designs are
rejected to avoid feedback across paired interfaces. Verilog drive strengths,
`tran`/`rtran`, and wired-AND/wired-OR net semantics are not implemented.

## Implementation and tests

The Java implementation is in `com.cburch.logisim.verilog`. `YosysRunner`
elaborates and normalizes sources, `WordNetlist` and `NativeMemory` lower cells,
and `CircuitLayout` builds circuits and verifies connectivity against native
component endpoints. Routing uses bounded blocks connected by circuit-local
tunnels. `VerilogImporter` commits the detached circuits through an undoable
project action; `VerilogImportDialog` provides the UI.

`VerilogImporterTest` runs the reference fixtures under JUnit without needing
Yosys. It checks 16 designs and 928 simulation vectors, including
simulation after save/reload, and also checks failure atomicity and repeated
imports. The standalone `ImportChecks` entry point runs the same assertions.
`TristateChecks` tests high impedance, contention, repeated release, overlapping
slices, nested inout/output interfaces, open-drain assignments and 64-bit buses.
The JSON tri-state tests run without Yosys; synthesized cases use the optional
external synthesis test.
`SynthesisChecks` additionally exercises the included Verilog source fixtures
and actual Yosys execution, paths, options and cancellation. The optional JUnit
synthesis test runs when the `YOSYS` environment variable names an executable.

From the repository root in PowerShell:

```powershell
$env:YOSYS = 'C:\path\to\yosys.exe'
.\gradlew.bat test --tests com.cburch.logisim.verilog.VerilogImporterTest
```

On Linux or macOS:

```sh
YOSYS=/path/to/yosys ./gradlew test --tests com.cburch.logisim.verilog.VerilogImporterTest
```

Without `YOSYS`, the reference-vector test still runs and the external synthesis
test is skipped. Both tests use temporary output directories and all source
fixtures are included in the repository.
