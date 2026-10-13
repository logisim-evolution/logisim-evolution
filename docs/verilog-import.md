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
include addition, subtraction, multiplication, Boolean logic, reductions,
comparisons, shifts, multiplexers, slices, concatenation, registers, latches,
and the supported reset/enable forms. Parameterized modules are elaborated by
Yosys before conversion.

Memory policies:

| Policy | Behavior |
| --- | --- |
| Native where compatible, otherwise logic | Uses RAM/ROM components for compatible memories and asks Yosys to expand the remaining memories. |
| Require native memories | Rejects a memory that cannot be represented directly. |
| Expand memories into logic | Asks Yosys to expand all memories into registers and logic. |

Native memories support asynchronous reads, one clocked write port, write masks,
multiple read ports, and initialized ROMs. Supported address offsets and bounds
are checked so an out-of-range access does not wrap around. Native data widths
are 1–64 bits and depths are at most 2^24 words. Large designs can still consume
substantial memory or produce large schematics.

The default undefined-value policy rejects explicit `x` and `z` values that need
mapping. Selecting zero or one deliberately gives those values a fixed binary
meaning. This is a two-state circuit translation; it is not Verilog four-state
simulation. Logisim registers and RAM start at zero. Nonzero initial register
values, including those produced by memory expansion, require a reset-based
design and are rejected.

Unsupported cells report the module, instance and cell type. Current exclusions
include tri-state/inout ports, black boxes, recursive hierarchy,
and other cells outside the supported mappings. Memory fallback is limited by
Yosys: for example, incompatible write clocks can remain unmapped and are then
rejected. SystemVerilog support is limited to the installed Yosys frontend.
There is no HDL co-simulation or automatic resynthesis when source files change.

## Implementation and tests

The Java implementation is in `com.cburch.logisim.verilog`. `YosysRunner`
elaborates and normalizes sources, `WordNetlist` and `NativeMemory` lower cells,
and `CircuitLayout` builds circuits and verifies connectivity against native
component endpoints. Routing uses bounded blocks connected by circuit-local
tunnels. `VerilogImporter` commits the detached circuits through an undoable
project action; `VerilogImportDialog` provides the UI.

`VerilogImporterTest` runs the reference fixtures under JUnit without needing
Yosys or Python. It checks 16 designs and 928 simulation vectors, including
simulation after save/reload, and also checks failure atomicity and repeated
imports. The standalone `ImportChecks` entry point runs the same assertions.
`SynthesisChecks` additionally exercises the original converter workspace's
Verilog examples and actual Yosys execution, paths, options and cancellation.
