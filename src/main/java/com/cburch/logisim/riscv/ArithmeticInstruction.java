package com.cburch.logisim.riscv;

public class ArithmeticInstruction {


    public static void executeImmediate(rv32imData hartData) {
        InstructionRegister ir = hartData.getIR();
        switch (ir.func3()) {
            case 0x0:   // addi rd,rs1,imm_I
                System.out.println("opcode: " + ir.opcode());
                System.out.println("destination register: " + ir.rd());
                System.out.println("function: " + ir.func3());
                System.out.println("source register 1: " + ir.rs1());
                System.out.println("immediate value: " + ir.imm_I());
                System.out.println("Test 0x300093 instruction: ");
                System.out.println("BEFORE: x0 = " + hartData.getX(0) + ", x1 = " + hartData.getX(1));
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) + ir.imm_I());
                System.out.println("AFTER: x0 = " + hartData.getX(0) + ", x1 = " + hartData.getX(1));
                System.out.println("Should be x1 = x0 + 3.");
                break;
            case 0x4:   // xori rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) ^ ir.imm_I());
                break;
        }
    }
}
