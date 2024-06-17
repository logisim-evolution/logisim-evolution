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

    public static void executeRegister(rv32imData hartData){
        InstructionRegister ir = hartData.getIR();
        switch (ir.func3()) {
            case 0x0:
                switch (ir.func7()) {
                    case 0x00:  // add rd,rs1,rs2
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) + hartData.getX(ir.rs2()));
                        break;
                    case 0x20:  // sub rd,rs1,rs2
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) - hartData.getX(ir.rs2()));
                        break;
                }
                break;
            case 0x1:   // sll rd,rs1,rs2
                hartData.setX(ir.rd(), hartData.getX(ir.rs1()) << (hartData.getX(ir.rs2()) & 0x1f));
                break;
            case 0x2:   // slt rd,rs1,rs2
                hartData.setX(ir.rd(), (hartData.getX(ir.rs1()) < hartData.getX(ir.rs2())) ? 1 : 0);
                break;
            case 0x3:   // sltu rd,rs1,rs2
                hartData.setX(ir.rd(),
                        (Long.compareUnsigned(hartData.getX(ir.rs1()),hartData.getX(ir.rs2())) < 0) ? 1 : 0);
                break;
            case 0x4:   // xor rd,rs1,rs2
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) ^ hartData.getX(ir.rs2()));
                break;
            case 0x5:
                switch (ir.func7()) {
                    case 0x00:  // srl rd,rs1,rs2
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) >>> (hartData.getX(ir.rs2()) & 0x1f));
                        break;
                    case 0x20:  // sra rd,rs1,rs2
                        hartData.setX(ir.rd(), hartData.getX(ir.rs1()) >> (hartData.getX(ir.rs2()) & 0x1f));
                        break;
                }
                break;
            case 0x6:   // or rd,rs1,rs2
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) | hartData.getX(ir.rs2()));
                break;
            case 0x7:   // and rd,rs1,rs2
                hartData.setX(ir.rd(),hartData.getX(ir.rs1()) & hartData.getX(ir.rs2()));
                break;
        }
    }
}
