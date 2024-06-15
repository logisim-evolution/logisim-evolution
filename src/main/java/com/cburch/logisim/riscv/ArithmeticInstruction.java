package com.cburch.logisim.riscv;

public class ArithmeticInstruction {


    public static void executeImmediate(rv32imData hartData) {
        InstructionRegister ir = hartData.getIR();
        switch (ir.func3()) {
            case 0x0:   // addi rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) + ir.imm_I());
                break;
            case 0x1:   // slli rd,rs1,imm_U
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) << ir.imm_U());
                break;
            case 0x2:   // slti rd,rs1,imm_I
                hartData.setX(ir.rd(), (hartData.getX(ir.rs1()) < ir.imm_I() ? 1 : 0) );
                break;
            case 0x3:   // sltiu rd,rs1,imm_I
                hartData.setX(ir.rd(), (hartData.getX(ir.rs1()) < ir.imm_I() ? 1 : 0));
                break;
            case 0x4:   // xori rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) ^ ir.imm_I());
                break;
            case 0x5:
                // srli rd,rs1,imm_U
                if (ir.func7() == 0x0) {
                    hartData.setX(ir.rd(),hartData.getX(ir.rs1()) >> ir.imm_U());
                }
                // srai rd,rs1,imm_U
                else if (ir.func7() == (1 << 5)) {
                    hartData.setX(ir.rd(),hartData.getX(ir.rs1()) >> ir.imm_U());
                }
                break;
            case 0x6:   // ori rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) | ir.imm_I());
                break;
            case 0x7:   // andi rd,rs1,imm_I
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) & ir.imm_I());
                break;
        }
    }
}
