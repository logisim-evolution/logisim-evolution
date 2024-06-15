package com.cburch.logisim.riscv;

public class ArithmeticInstruction {


    public static void executeImmediate(rv32imData hartData) {
        InstructionRegister ir = hartData.getIR();
        switch (ir.func3()) {
            case 0x0:   // addi rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) + ir.imm_I());
                break;
            case 0x4:   // xori rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) ^ ir.imm_I());
                break;
        }
    }
}
