package edu.cornell.cs3410;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

import com.cburch.logisim.util.GraphicsUtil;

// import org.graalvm.compiler.core.common.type.ArithmeticOpTable.IntegerConvertOp.SignExtend;

public class ALU2 extends InstanceFactory {

    String[] insts = new String[] {  "ADD",     "ADDI",     "SUB",      "MUL",      "AND",      "ANDI",     "OR",       "ORI",      "XOR",      "XORI",     "SLT",      "SLTI",     "SLTU",     "SLTIU",    "SRA",      "SRAI",     "SRL",      "SRLI",     "SLL",      "SLLI",     "LUI",      "AUIPC",    "LW",       "SW",       "JAL",      "JALR",     "BEQ",      "BNE",      "BLT",      "BGE",      "BLTU",     "BGEU",     "LB",       "SB"};
    int[] instOpCodes = new int[] {  0b0110011, 0b0010011,  0b0110011,  0b0110011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110011,  0b0010011,  0b0110111,  0b0010111,  0b0000011,  0b0100011,  0b1101111,  0b1100111,  0b1100011,  0b1100011,  0b1100011,  0b1100011,  0b1100011,  0b1100011,  0b0000011,  0b0100011};
    int[] instOpCodes2 = new int[] { 0b000,     0b000,      0b000,      0b000,      0b111,      0b111,      0b110,      0b110,      0b100,      0b100,      0b010,      0b010,      0b011,      0b011,      0b101,      0b101,      0b101,      0b101,      0b001,      0b001,      -1,         -1,         0b010,      0b010,      -1,         0b000,      0b000,      0b001,      0b100,      0b101,      0b110,      0b111,      0b000,      0b000};
    int[] instOpCodes3 = new int[] { 0b0000000, -1,         0b0100000,  0b0000001,  0b0000000,  -1,         0b0000000,  -1,         0b0000000,  -1,         0b0000000,  -1,         0b0000000,  -1,         0b0100000,  0b0100000,  0b0000000,  0b0000000,  0b0000000,  0b0000000,  -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1,         -1};
    char[] instType = new char[] {   'R',       'I',        'R',        'R',        'R',        'I',        'R',        'I',        'R',        'I',        'R',        'I',        'R',        'I',        'R',        'I',        'R',        'I',        'R',        'I',        'U',        'U',        'I',        'W',        'U',        'I',        'B',        'B',        'B',        'B',        'B',        'B',        'I',        'S'};
    int[] ALUOp = new int[] {        0b1110,    0b1110,     0b1100,     0b0000,     0b0000,     0b0000,     0b0001,     0b0001,     0b0100,     0b0100,     0b0000,      0b0000,     0b0000,     0b0000,     0b0111,     0b0111,     0b0110,     0b0110,     0b0010,     0b0010,     0b1110,     0b1110,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000,     0b0000};
    int numInsts = insts.length;

    public ALU2() {
        super("Decode Black Box");
        assert insts.length == instOpCodes.length;
        setOffsetBounds(Bounds.create(-30, -50, 60, 410));
        Port[] ports = new Port[numInsts + 7];
        ports[0] = new Port(-20, -50, Port.OUTPUT, 5);
        ports[1] = new Port(0, -50, Port.OUTPUT, 5);
        ports[2] = new Port(-30, 40, Port.INPUT, 32);
        ports[3] = new Port(20, -50, Port.INPUT, 5);
        ports[4] = new Port(30, -20, Port.OUTPUT, 32);
        ports[5] = new Port(30, -10, Port.OUTPUT, 1);
        ports[6] = new Port(30, 0, Port.OUTPUT, 4); // ALUOp

        for (int i = 0; i < numInsts; i++) {
            ports[i + 7] = new Port(30, i * 10 + 20, Port.OUTPUT, 1);
        }
        setPorts(ports);
    }
    
    public static int signExtend(int val, int bits) {
        int shift = 32 - bits;  // int always has 32 bits in Java
        int s = val << shift;
        return s >> shift;
    }

    // Shifts A for risc-v not B
    @Override
    public void propagate(InstanceState state) {
        int A = state.getPortValue(0).toIntValue() & 127;
        int B = state.getPortValue(1).toIntValue();
        int op = state.getPortValue(2).toIntValue();

        // opcode
        int op1 = op & 127;
        // funct3
        int op2 = (op >>> 12) & 0b111;
        // funct7
        int op3 = (op >>> 25);
        // rs1
        Value out = Value.createKnown(BitWidth.create(32), (op >>> 15) & 0b11111);
        state.setPort(0, out, 32);
        
        // rs2
        out = Value.createKnown(BitWidth.create(32), (op >>> 7) & 0b11111);
        state.setPort(3, out, 32);
        // int shift = state.getPortValue(3).toIntValue();
        int[] instOut = new int[numInsts];
        for (int i=0; i<numInsts; i++) {
        	//TODO: refactor
        	instOut[i] = (op1 == instOpCodes[i]) && (op2 == instOpCodes2[i] || instOpCodes2[i] == -1) && (op3 == instOpCodes3[i] || instOpCodes3[i] == -1) ? 1 : 0;
            if (instOut[i] == 1) { //Imm
                // rs2
                out = Value.createKnown(BitWidth.create(32), ((insts[i] == "LUI" || insts[i] == "AUIPC") ? ALUOp[i] : (op >>> 20) & 0b11111) );
                state.setPort(1, out, 32);
        		int useImmOut = 0;
        		switch (instType[i]) {
                case 'I': out = (insts[i] == "ADDI" || insts[i] == "ANDI" || insts[i] == "ORI" || insts[i] == "XORI" || insts[i] == "SLTI" || insts[i] == "SLTIU") 
                    ? Value.createKnown(BitWidth.create(32), signExtend((op >>> 20), 12) )
                    : Value.createKnown(BitWidth.create(32), (op >>> 20) ); 
                    useImmOut=1; break;
                case 'R': out = Value.createKnown(BitWidth.create(32), 0 ); useImmOut=0; break;
        		case 'S': out = Value.createKnown(BitWidth.create(32), ((op >>> 7) & 0b11111) | ((op>>>20) & 0b111111100000) ); useImmOut=1; break;
        		case 'B': out = Value.createKnown(BitWidth.create(32), ((op >>> 7)&0b11110) | ((op>>>20) & 0b10111111100000) | (((op>>>7)&0b1)<<12)    ); useImmOut=1;break;
        		case 'U': out = Value.createKnown(BitWidth.create(32), (op & 0xFFFFF000)); useImmOut=1; break;
                }

                state.setPort(4, out, 32);
                
        		out = Value.createKnown(BitWidth.create(32), useImmOut );
        		state.setPort(5, out, 32);
        		out = Value.createKnown(BitWidth.create(4), ALUOp[i] );
        		state.setPort(6, out, 32);
            }
            
        }

        for (int i = 0; i < numInsts; i++) {
            out = Value.createKnown(BitWidth.create(32), instOut[i]);
            state.setPort(i + 7, out, 32);
        }
        // Value out = Value.createKnown(BitWidth.create(32), ans);
        // Value out = Value.createKnown(BitWidth.create(1), op != 0 ? 1:0);
        // Eh, delay of 32? Sure...
        // state.setPort(ans+5, out, 32);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Bounds bounds = painter.getBounds();
        int x0 = bounds.getX();
        int x1 = x0 + bounds.getWidth();
        int y0 = bounds.getY();
        int y1 = y0 + bounds.getHeight();
        int xp[] = { x0, x1, x1, x0// , x0, x0 + 20, x0
        };
        int yp[] = { y0, y0, y1, y1// , y1 - 40, y1 - 50, y1 - 60
        };
        GraphicsUtil.switchToWidth(painter.getGraphics(), 2);
        painter.getGraphics().drawPolygon(xp, yp, 4);
        painter.drawPort(0, "rs1", Direction.NORTH);
        painter.drawPort(1, "rs2", Direction.NORTH);
        painter.drawPort(2, "Inst", Direction.EAST);
        painter.drawPort(3, "rd", Direction.NORTH);
        painter.drawPort(4, "Imm", Direction.WEST);
        painter.drawPort(5, "useImm", Direction.WEST);
        painter.drawPort(6, "ALUOp", Direction.WEST);

        for (int i = 0; i < numInsts; i++) {
            painter.drawPort(i + 7, insts[i], Direction.WEST);
        }
    }

    /*
     * @Override public void paintIcon(InstancePainter painter) { Graphics g =
     * painter.getGraphics(); Font old = g.getFont();
     * g.setFont(old.deriveFont(9.0f)); GraphicsUtil.drawCenteredText(g, "RAM", 10,
     * 9); g.setFont(old); g.drawRect(0, 4, 19, 12); for(int dx = 2; dx < 20; dx +=
     * 5) { g.drawLine(dx, 2, dx, 4); g.drawLine(dx, 16, dx, 18); } }
     */
}
