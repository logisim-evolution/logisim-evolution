# Reference fixtures

The Java tests import each design, exercise undo/redo, save and reload the
resulting `.circ`, and simulate all vectors. Coverage includes hierarchy with
independent state, signed arithmetic, shifts, bus permutations, reset priority,
ROMs, masked RAM writes, memory bounds and multiple read ports. The final
fixture contains a Yosys-synthesized 64 KiB RAM and ROM design. These are targeted
regressions, not an exhaustive Verilog equivalence proof.
