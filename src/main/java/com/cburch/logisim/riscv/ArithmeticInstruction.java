package com.cburch.logisim.riscv;

public class ArithmeticInstruction {

    public static void executeImmediate(rv32imData hartData) {
        InstructionRegister ir = hartData.getIR();
        switch (ir.func3()) {
            case 0x0:   // addi rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) + ir.imm_I());
                break;
            case 0x1:   // slli rd,rs1,shamt_I
                hartData.setX(ir.rd(), hartData.getX(ir.rs1()) << ir.rs2());
                break;
            case 0x2:   // slti rd,rs1,imm_I
                hartData.setX(ir.rd(), (hartData.getX(ir.rs1()) < ir.imm_I()) ? 1 : 0);
                break;
            case 0x3:   // sltiu rd,rs1,imm_I
                hartData.setX(ir.rd(), (Long.compareUnsigned(hartData.getX(ir.rs1()),ir.imm_I()) < 0) ? 1 : 0);
                break;
            case 0x4:   // xori rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) ^ ir.imm_I());
                break;
            case 0x5:
                switch(ir.func7()) {
                    case 0x00: // srli rd,rs1,shamt_I
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) >>> ir.rs2());
                        break;
                    case 0x20: // srai rd,rs1,shamt_I
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) >> ir.rs2());
                        break;
                }
                break;
            case 0x6:   // ori rd,rs1,imm_I
                hartData.setX(ir.rd(), hartData.getX(ir.rs1()) | ir.imm_I());
                break;
            case 0x7:   // andi rd,rs1,imm_I
                hartData.setX(ir.rd(), hartData.getX(ir.rs1()) & ir.imm_I());
                break;
        }
    }
}
