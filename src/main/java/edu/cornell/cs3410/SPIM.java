/* 
 * CS3410 - Simulated MIPS core
 * Based on code from http://community.vcsu.edu/facultypages/curt.hill/My_Webpage/mips.html
 * NOTE:
 *  -- added more mips instructions
 *  -- fixed various bugs
 *  -- modified to work with existing cs3410 library components, such as ROM and RAM
 * 
 * Caveat: 
 *   The instruction after jump/branch delay slot is fetched and flushed, which 
 *   should not be fetched in the first place.
 *
 * Author: hwang@cs.cornell.edu 
 */

package edu.cornell.cs3410;

import java.awt.Font;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.NoSuchElementException;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

import static edu.cornell.cs3410.SPIMUtils.*;
import edu.cornell.cs3410.ProgramAssembler.Listing;

public class SPIM extends InstanceFactory {

  public static Fetch fetch;
  public static Decode decode;
  public static Execute execute;
  public static Memory memory;
  public static WriteBack wb;
  public static Staller staller;
  public static ExceptionUnit exceptionUnit;
  private static ClockState clockState;

  private static final Attribute<Integer> ATTR_BUFFER = Attributes.forIntegerRange("buflen",
      Strings.getter("SPIMBufferLengthAttr"), 1, 256);
  private static Pattern pat1 = Pattern.compile("\\s*,\\s*");
  private static HashMap<String, InstType> m;

  public SPIM() {
    super("SPIM", Strings.getter("Core with Exception Support"));
    setAttributes(new Attribute[] { ATTR_BUFFER, StdAttr.EDGE_TRIGGER },
        new Object[] { Integer.valueOf(32), StdAttr.TRIG_RISING });
    setOffsetBounds(Bounds.create(0, 0, WIDTH, HEIGHT));
    setIconName("SPIM.gif");
    // setInstancePoker(Poker.class);

    m = new HashMap<String, InstType>();
    set_support_mips_inst(m);

    clockState = new ClockState();
    fetch = new Fetch();
    decode = new Decode();
    execute = new Execute();
    memory = new Memory();
    wb = new WriteBack();
    staller = new Staller();
    exceptionUnit = new ExceptionUnit();

    Port[] ps = new Port[10];
    ps[CLK] = new Port(0, HEIGHT - 20, Port.INPUT, 1);
    ps[OP] = new Port(20, HEIGHT, Port.INPUT, 32);
    ps[PC] = new Port(40, HEIGHT, Port.OUTPUT, 32);
    ps[ADDR] = new Port(WIDTH, 20, Port.OUTPUT, 24);
    ps[DOUT] = new Port(WIDTH, 40, Port.OUTPUT, 32);
    ps[DIN] = new Port(WIDTH, 60, Port.INPUT, 32);
    ps[STR] = new Port(WIDTH, 80, Port.OUTPUT, 1);
    ps[SEL] = new Port(WIDTH, 100, Port.OUTPUT, 4);
    ps[LD] = new Port(WIDTH, 120, Port.OUTPUT, 1);
    ps[IRQ_IN] = new Port(80, HEIGHT, Port.INPUT, 1);
    setPorts(ps);
  }

  @Override
  public void propagate(InstanceState state) {
    SPIMData data = SPIMData.get(state);
    int op = state.getPortValue(OP).toIntValue();

    boolean rising_triggered = clockState.updateClock(state.getPortValue(CLK), StdAttr.TRIG_RISING);
    boolean level_triggered = clockState.updateClock(state.getPortValue(CLK), StdAttr.TRIG_LOW);
    boolean allNOPs = false;

    if (rising_triggered) {
      String line = ProgramAssembler.disassemble(op, fetch.PC);
      line = pat1.matcher(line).replaceAll(" ");
      line.replaceAll(",", " ");
      Instruction inst = new Instruction(line);

      p("==========================");
      p("+++++++Rising Edge++++++++");
      p("fetched: " + Integer.toHexString(fetch.PC) + " " + line);
      pf("PC      %20x%20x%20x%20x%20x\n", fetch.PC, decode.PC, execute.PC, memory.PC, wb.PC);

      // move instructions through pipeline by one stage
      wb.inst = memory.inst;
      memory.inst = execute.inst;
      execute.inst = decode.inst;
      decode.inst = fetch.inst;

      wb.PC = memory.PC;
      memory.PC = execute.PC;
      execute.PC = decode.PC;
      if (!fetch.stall) {
        decode.PC = fetch.PC;
      }
      staller.step(fetch, decode, execute, memory);
      // load next Instruction
      fetch.step(inst, data, decode);
      decode.step(data, memory, wb, fetch, execute);
      execute.step(data, fetch, decode);
      memory.step(state);

      print_mips_debug_info(fetch, decode, execute, memory, wb);

      // output current PC
      state.setPort(PC, Value.createKnown(BitWidth.create(32), fetch.PC), 1);

      // update Cause register
      if (state.getPortValue(IRQ_IN).toIntValue() == 1) {
        int cause = data.regs[CAUSE].toIntValue();
        cause |= (E_CODE_HW << 2);
        data.regs[CAUSE] = Value.createKnown(BitWidth.create(32), cause);
        int status = data.regs[STATUS].toIntValue();
        status |= 1 << KEYBOARD_IRQ;
        data.regs[STATUS] = Value.createKnown(BitWidth.create(32), status);
      } else {

      }

      // update Status register
      int status = data.regs[STATUS].toIntValue();
      status |= 1 << 4; // always in use mode
      data.regs[STATUS] = Value.createKnown(BitWidth.create(32), status);

      // update EPC regsiter
      exceptionUnit.step(fetch, decode, execute, memory, wb, state, data);
    }

    if (state.getPortValue(CLK).toIntValue() == 0) {
      p(".....falling edge.....");
      allNOPs = (fetch.inst.opcode == 0) && (decode.inst.opcode == 0) && (execute.inst.opcode == 0)
          && (memory.inst.opcode == 0);
      ;
      wb.step(data, allNOPs);
    }

    if (level_triggered) {
      decode.step_level(data, memory, wb, fetch, execute);
      memory.step_level(state);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bounds = painter.getBounds();
    Font font = g.getFont().deriveFont(9f);
    ;
    SPIMData data = SPIMData.get(painter);
    boolean showState = painter.getShowState();

    painter.drawBounds();
    painter.drawClock(CLK, Direction.EAST);
    painter.drawPort(OP, "OP", Direction.SOUTH);
    painter.drawPort(PC, "PC", Direction.SOUTH);
    painter.drawPort(ADDR, "ADDR", Direction.WEST);
    painter.drawPort(DOUT, "DOUT", Direction.WEST);
    painter.drawPort(DIN, "DIN", Direction.WEST);
    painter.drawPort(STR, "STR", Direction.WEST);
    painter.drawPort(SEL, "SEL", Direction.WEST);
    painter.drawPort(LD, "LD", Direction.WEST);
    painter.drawPort(IRQ_IN, "IRQ_IN", Direction.SOUTH);

    // draw some rectangles
    for (int i = 0; i < NUM_REGISTERS; i++) {
      drawBox(g, bounds, Color.GRAY, i);
    }

    // draw register labels
    for (int i = 0; i < NUM_REGISTERS; i++) {
      GraphicsUtil.drawText(g, font, "$" + i, bounds.getX() + boxX(i) - 1,
          bounds.getY() + boxY(i) + (BOX_HEIGHT - 1) / 2, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
    }

    if (!painter.getShowState()) {
      return;
    }

    // draw state
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(bounds.getX() + boxX(0) + 1, bounds.getY() + boxY(0) + 1, BOX_WIDTH - 1, BOX_HEIGHT - 1);
    g.setColor(Color.BLACK);

    for (int i = 0; i < NUM_REGISTERS; i++) {
      String s = (data.regs[i].isFullyDefined() ? data.regs[i].toHexString() : "?");
      GraphicsUtil.drawText(g, font, s, bounds.getX() + boxX(i) + BOX_WIDTH / 2,
          bounds.getY() + boxY(i) + (BOX_HEIGHT - 1) / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    }

    // draw boundary for mem Port
    g.drawRect(bounds.getX() + bounds.getWidth() / 4 * 3 + 10, bounds.getY() + 10, bounds.getWidth() / 4 - 10,
        bounds.getHeight() / 2);
    GraphicsUtil.drawCenteredText(g, "CPU + RAM CNTL", bounds.getX() + bounds.getWidth() / 4 * 3,
        bounds.getY() + bounds.getHeight() - 40);

    // draw CP0 registers
    GraphicsUtil.drawText(g, font, "BadVAddr ", bounds.getX() + cp_x(BADVADDR) + BOX_WIDTH / 2,
        bounds.getY() + cp_y(BADVADDR) + BOX_HEIGHT / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, font, "Status ", bounds.getX() + cp_x(STATUS) + BOX_WIDTH / 2,
        bounds.getY() + cp_y(STATUS) + BOX_HEIGHT / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, font, "Cause ", bounds.getX() + cp_x(CAUSE) + BOX_WIDTH / 2,
        bounds.getY() + cp_y(CAUSE) + BOX_HEIGHT / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, font, "EPC ", bounds.getX() + cp_x(EPC) + BOX_WIDTH / 2,
        bounds.getY() + cp_y(EPC) + BOX_HEIGHT / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);

    for (int i = 34; i < 38; i++) {
      String s = (data.regs[i].isFullyDefined() ? data.regs[i].toHexString() : "?");
      GraphicsUtil.drawText(g, font, s, bounds.getX() + cp_x(i) + BOX_WIDTH * 3 / 2,
          bounds.getY() + cp_y(i) + (BOX_HEIGHT - 1) / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
      drawCPBox(g, bounds, Color.GRAY, i);
    }
  }

  // Uncomment to debug
  private static <printabletostring> void p(printabletostring... args) {
    for (printabletostring pts : args)
      System.out.print(pts);
    System.out.println();
  }

  private static <printabletostring> void pf(String fmt, printabletostring... args) {
    System.out.format(fmt, args);
  }

  private void print_mips_debug_info(Fetch f, Decode d, Execute e, Memory m, WriteBack w) {
    pf("Inst    %20s%20s%20s%20s%20s\n", f.inst, d.inst, e.inst, m.inst, w.inst);
    pf("flush   %20b%20b%20b%20b%20b\n", f.inst.flush, d.inst.flush, e.inst.flush, m.inst.flush, w.inst.flush);
    pf("Rs      %20d%20d%20d%20d%20d\n", f.inst.rs, d.inst.rs, e.inst.rs, m.inst.rs, w.inst.rs);
    pf("Rt      %20d%20d%20d%20d%20d\n", f.inst.rt, d.inst.rt, e.inst.rt, m.inst.rt, w.inst.rt);
    pf("Rd      %20d%20d%20d%20d%20d\n", f.inst.rd, d.inst.rd, e.inst.rd, m.inst.rd, w.inst.rd);
    pf("RsV(hex)%20x%20x%20x%20x%20x\n", f.inst.rsValue, d.inst.rsValue, e.inst.rsValue, m.inst.rsValue,
        w.inst.rsValue);
    pf("RtV(hex)%20x%20x%20x%20x%20x\n", f.inst.rtValue, d.inst.rtValue, e.inst.rtValue, m.inst.rtValue,
        w.inst.rtValue);
    pf("RdV(hex)%20x%20x%20x%20x%20x\n", f.inst.rdValue, d.inst.rdValue, e.inst.rdValue, m.inst.rdValue,
        w.inst.rdValue);
    p("..........................");
  }

  // type:
  // 0 - Rtype, cmd rd, rs, rt
  // 1 - Itype, cmd rt, rs, immediate
  // 2 - ld/st, cmd rt, offset(base)
  // 3 - branch, cmd offset
  // 4 - b+link, cmd rs, offset
  // 5 - beq, cmd rs, rt, offset
  // 6 - div, cmd rs, rt
  // 7 - cop0, cmd rt, rd
  // 8 - syscall, break, eret, nop
  // 9 - jalr rd, rs or jr rs
  // 10 - jr, cmd rs
  // 11 - lui, cmd rt, immediate
  // 12 - sll, cmd rd, rt, immediate
  // 13 - sllv, cmd rd, rt, rs
  // opcode: internal opcode is assigned according to MIPS_Vol2.pdf page number.
  // NONE of the COP1 instructions are supported.
  private void set_support_mips_inst(HashMap<String, InstType> m) {
    m.put("add", new InstType(0, 31, 0));
    m.put("addi", new InstType(1, 34, 0));
    m.put("addiu", new InstType(1, 35, 0));
    m.put("addu", new InstType(0, 36, 0));
    m.put("and", new InstType(0, 37, 0));
    m.put("andi", new InstType(1, 38, 0));
    m.put("b", new InstType(3, 39, 0));
    m.put("bal", new InstType(4, 40, 0));
    m.put("beq", new InstType(5, 55, 0));
    m.put("bgez", new InstType(4, 58, 0));
    m.put("bgezal", new InstType(4, 59, 0));
    m.put("bgtz", new InstType(4, 64, 0));
    m.put("blez", new InstType(4, 67, 0));
    m.put("bltz", new InstType(4, 70, 0));
    m.put("bltzal", new InstType(4, 71, 0));
    m.put("bne", new InstType(5, 76, 0));
    m.put("break", new InstType(12, 79, 0));
    m.put("div", new InstType(6, 109, 0));
    m.put("divu", new InstType(6, 112, 0));
    m.put("eret", new InstType(8, 113, 0));
    m.put("j", new InstType(3, 115, 0));
    m.put("jal", new InstType(3, 116, 0));
    m.put("jalr", new InstType(9, 117, 0));
    m.put("jr", new InstType(10, 119, 0));
    m.put("lb", new InstType(2, 121, 0));
    m.put("lbu", new InstType(2, 122, 0));
    m.put("lh", new InstType(2, 125, 0));
    m.put("lhu", new InstType(2, 126, 0));
    m.put("ll", new InstType(2, 127, 0));
    m.put("lui", new InstType(11, 129, 0));
    m.put("lw", new InstType(2, 130, 0));
    m.put("mfc0", new InstType(7, 143, 0));
    m.put("movn", new InstType(0, 152, 0));
    m.put("movz", new InstType(0, 158, 0));
    m.put("mtc0", new InstType(7, 163, 0));
    m.put("mul", new InstType(0, 169, 0));
    m.put("nop", new InstType(8, 174, 0));
    m.put("nor", new InstType(0, 175, 0));
    m.put("or", new InstType(0, 176, 0));
    m.put("ori", new InstType(1, 177, 0));
    m.put("sb", new InstType(2, 185, 0));
    m.put("sh", new InstType(2, 192, 0));
    m.put("sll", new InstType(12, 193, 0));
    m.put("sllv", new InstType(13, 194, 0));
    m.put("slt", new InstType(0, 195, 0));
    m.put("slti", new InstType(1, 196, 0));
    m.put("sltiu", new InstType(1, 197, 0));
    m.put("sltu", new InstType(0, 198, 0));
    m.put("sra", new InstType(12, 200, 0));
    m.put("srav", new InstType(13, 201, 0));
    m.put("srl", new InstType(12, 202, 0));
    m.put("srlv", new InstType(13, 203, 0));
    m.put("sub", new InstType(0, 205, 0));
    m.put("subu", new InstType(0, 207, 0));
    m.put("sw", new InstType(2, 208, 0));
    m.put("syscall", new InstType(8, 219, 0));
    m.put("teq", new InstType(6, 220, 0));
    m.put("teqi", new InstType(4, 221, 0));
    m.put("tge", new InstType(6, 222, 0));
    m.put("tgei", new InstType(4, 223, 0));
    m.put("tgeiu", new InstType(4, 224, 0));
    m.put("tgeu", new InstType(6, 225, 0));
    m.put("tlt", new InstType(6, 233, 0));
    m.put("tlti", new InstType(4, 234, 0));
    m.put("tltiu", new InstType(4, 235, 0));
    m.put("tltu", new InstType(6, 236, 0));
    m.put("tne", new InstType(6, 237, 0));
    m.put("tnei", new InstType(4, 238, 0));
    m.put("xor", new InstType(0, 243, 0));
    m.put("xori", new InstType(1, 244, 0));
  }

  private class InstType {
    public int type;
    public int opcode;
    public int supported;

    InstType(int t, int n, int s) {
      type = t;
      opcode = n;
      supported = s;
    }
  }

  private int get_inst_type(String s) {
    InstType t = m.get(s.toLowerCase());
    return (t == null) ? -1 : t.type;
  }

  private int get_inst_opcode(String s) {
    InstType t = m.get(s.toLowerCase());
    return (t == null) ? -1 : t.opcode;
  }

  private int sign_extend(int v, int bit) {
    v <<= 32 - bit;
    v >>= 32 - bit;
    return v;
  }

  private class Instruction {
    public int opcode;
    public int optype;
    public int rs;
    public int rt;
    public int rd;
    public int immediate;
    public long rdValue;
    public long rsValue;
    public long rtValue;
    public String instructionString;

    private boolean ok; // check validity of Instruction
    public boolean flush; // indicate if the instruction has been flushed.
    public boolean wbFlush; // indicate when instruction is in writeback and fetch.
    public boolean forwardRsFlag; // indicate Rs value of prev inst is need.
    public boolean forwardRtFlag; // indicate Rt value of prev inst is need.

    private boolean isImmediateInstruction;

    Instruction(String temp) {
      StringTokenizer tokens = new StringTokenizer(temp, " ");
      String op = "", t1 = "", t2 = "", t3 = "";

      rdValue = 0;
      rsValue = 0;
      rtValue = 0;
      isImmediateInstruction = false;
      ok = true;
      instructionString = temp;
      flush = false;
      forwardRsFlag = false;
      forwardRtFlag = false;

      /* aka 'Stall' */
      if (temp.equals("NOP")) {
        instructionString = "NOP";
        rd = 0;
        rt = 0;
        rs = 0;
        immediate = 0;
        opcode = 0;
        return;
      }

      trying: try {
        op = tokens.nextToken();
        optype = get_inst_type(op);

        if (tokens.hasMoreTokens())
          t1 = tokens.nextToken();
        if (tokens.hasMoreTokens())
          t2 = tokens.nextToken();
        if (tokens.hasMoreTokens())
          t3 = tokens.nextToken();

        switch (optype) {
        case 0:
          rd = Integer.parseInt(t1.substring(1));
          rs = Integer.parseInt(t2.substring(1));
          rt = Integer.parseInt(t3.substring(1));
          break;
        case 1:
          rt = Integer.parseInt(t1.substring(1));
          rs = Integer.parseInt(t2.substring(1));
          rd = rt;
          isImmediateInstruction = true;
          immediate = Integer.parseInt(t3.substring(2), 16);
          break;
        case 2:
          rt = Integer.parseInt(t1.substring(1));
          rs = Integer.parseInt(t2.substring(t2.indexOf("$") + 1, t2.indexOf(")")));
          immediate = Integer.parseInt(t2.substring(2, t2.indexOf("(")), 16);
          rd = rt;
          break;
        case 3:
          immediate = Integer.parseInt(t1.substring(2), 16);
          break;
        case 4:
          rs = Integer.parseInt(t1.substring(1));
          immediate = Integer.parseInt(t2.substring(2), 16);
          opcode = get_inst_opcode(op);
          break;
        case 5:
          rt = Integer.parseInt(t1.substring(1));
          rs = Integer.parseInt(t2.substring(1));
          immediate = Integer.parseInt(t3.substring(2), 16);
          break;
        case 6:
          rs = Integer.parseInt(t1.substring(1));
          rt = Integer.parseInt(t2.substring(1));
          break;
        case 7:
          rt = Integer.parseInt(t1.substring(1));
          rd = Integer.parseInt(t2.substring(1));
          break;
        case 8: // syscall
          break;
        case 9:
          if (t2 != "") {
            rd = Integer.parseInt(t1.substring(1));
            rs = Integer.parseInt(t2.substring(1));
          } else {
            rs = Integer.parseInt(t1.substring(1));
            rd = 31;
          }
          break;
        case 10:
          rs = Integer.parseInt(t1.substring(1));
          break;
        case 11: // lui rt, immediate
          rt = Integer.parseInt(t1.substring(1));
          rd = rt;
          isImmediateInstruction = true;
          immediate = Integer.parseInt(t2.substring(2), 16);
          break;
        case 12:
          rd = Integer.parseInt(t1.substring(1));
          rt = Integer.parseInt(t2.substring(1));
          immediate = Integer.parseInt(t3);
          break;
        case 13:
          rd = Integer.parseInt(t1.substring(1));
          rt = Integer.parseInt(t2.substring(1));
          rs = Integer.parseInt(t3.substring(1));
          break;
        default:
          ok = false;
          break trying;
        }
        opcode = get_inst_opcode(op);
      } catch (NumberFormatException e) {
        System.out.println(e.toString());
        ok = false;
      } catch (NoSuchElementException e) {
        System.out.println(e.toString());
        ok = false;
      } catch (StringIndexOutOfBoundsException e) {
        System.out.println(e.toString());
        ok = false;
      }
    }

    public boolean valid() {
      return ok;
    }

    public boolean is_immediate() {
      return isImmediateInstruction;
    }

    public boolean is_load() {
      return (opcode == 121 || opcode == 122 || opcode == 125 || opcode == 126 || opcode == 130);
    }

    public boolean is_ji() {
      return (opcode == 115 || opcode == 116);
    }

    public boolean is_jr() {
      return (opcode == 117 || opcode == 119);
    }

    public String toString() {
      return (instructionString);
    }
  }

  // Stall unit for load-use stall
  private class Staller {
    private boolean _id_ex_load_use;
    private boolean id_ex_load_use;
    private boolean id_mem_load_use;

    public Staller() {
      _id_ex_load_use = false;
      id_ex_load_use = false;
      id_mem_load_use = false;
    }

    public void step(Fetch fetch, Decode decode, Execute execute, Memory mem) {
      id_ex_load_use = _id_ex_load_use;

      if (((decode.inst.rs == execute.inst.rd)
          || (decode.inst.rt == execute.inst.rd) && (decode.inst.is_immediate() == false)) && (execute.inst.is_load())
          && (execute.inst.rd != 0)) {
        _id_ex_load_use = true;
        p("staller: id_ex_load_use TRUE!");
      } else {
        _id_ex_load_use = false;
      }

      if (((decode.inst.rs == mem.inst.rd) || (decode.inst.rt == mem.inst.rd) && (decode.inst.is_immediate() == false))
          && (mem.inst.is_load()) && (mem.inst.rd != 0)) {
        id_mem_load_use = true;
      } else {
        id_mem_load_use = false;
      }

      if (id_ex_load_use || id_mem_load_use || _id_ex_load_use) {
        p("staller:" + id_ex_load_use + " " + _id_ex_load_use + " " + id_mem_load_use);
        fetch.stall = true;
        decode.nop = true;
      } else {
        fetch.stall = false;
        decode.nop = false;
      }
    }
  }

  // handles exception.
  private class ExceptionUnit {
    private boolean handle_exception;

    public ExceptionUnit() {
      handle_exception = false;
    }

    public void step(Fetch f, Decode d, Execute e, Memory m, WriteBack w, InstanceState state, SPIMData data) {
      int status = data.regs[STATUS].toIntValue();
      int irq_enable = (status &= 0x1);
      if (irq_enable > 0) {
        handle_exception = ((state.getPortValue(IRQ_IN).toIntValue() == 1) || false); // more
      } else {
        handle_exception = false;
      }

      if (handle_exception) {
        data.regs[EPC] = Value.createKnown(BitWidth.create(32), f.PC); // Save current PC.
        // disable hardware interrupt
        p("Status =" + Integer.toHexString(status));
        status &= -2; //
        p("Status =" + Integer.toHexString(status));
        data.regs[STATUS] = Value.createKnown(BitWidth.create(32), status);

        // execute exception handler
        f.PC = 0x800000;
      }
    }
  }

  // Write back stage
  private class WriteBack extends Stage {
    private boolean done;

    public WriteBack() {
      super();
      done = false;
    }

    public void step(SPIMData data, boolean allNOPs) {
      switch (inst.optype) {
      case 0:
      case 3:
      case 9:
      case 12:
      case 13:
        if (inst.rd != 0) {
          if (inst.flush == false) {
            if (inst.rd < NUM_REGISTERS) {
              Value v = Value.createKnown(BitWidth.create(32), (int) (inst.rdValue));
              data.regs[inst.rd] = v;
              p("Reg[" + inst.rd + "]=" + Long.toHexString(inst.rdValue & 0xFFFFFFFFL));
            } else {
              throw new IllegalArgumentException("Write address invalid: email hwang@cs and tell him!");
            }
          }
        }
        break;
      case 1:
      case 2:
      case 11:
        if (inst.rt != 0) {
          if (inst.flush == false) {
            if (inst.rt < NUM_REGISTERS) {
              Value v = Value.createKnown(BitWidth.create(32), (int) (inst.rtValue));
              data.regs[inst.rt] = v;
              p("Reg[" + inst.rt + "]=" + Long.toHexString(inst.rtValue & 0xFFFFFFFFL));
            }
          }
        }
        break;
      case 7: // MTC, MFC
        if (inst.opcode == 163) {
          if ((inst.rt != 0) && (inst.flush == false)) {
            Value v = Value.createKnown(BitWidth.create(32), (int) (inst.rtValue));
            switch (inst.rd) {
            case 8:
              data.regs[BADVADDR] = v;
              break;
            case 12:
              data.regs[STATUS] = v;
              break;
            case 13:
              data.regs[CAUSE] = v;
              break;
            default:
              p("nothing");
              break;
            }
          }
        } else if (inst.opcode == 143) {
          if ((inst.rd != 0) && (inst.flush == false)) {
            Value v = Value.createKnown(BitWidth.create(32), (int) (inst.rdValue));
            data.regs[inst.rt] = v;
          }
        }
        break;
      default:
        break;
      }
    }
  }

  private class Memory extends Stage {
    private int memAddr;

    public Memory() {
      super();
      memAddr = -1;
    }

    public void step(InstanceState state) {
      int sel = 0xF;
      int str = 0;
      int ld = 0;
      long val_to_mem = 0;

      if (inst.flush == true) {
        inst.wbFlush = true;
        return;
      } else {
        inst.wbFlush = false;
      }

      if (is_mem_write()) {
        memAddr = (int) ((inst.rsValue & 0xFFFFFFFFL) + inst.immediate);
        if (is_byte_access()) {
          p("byte access");
          val_to_mem = ((inst.rtValue & 0xFFL) << (8 * (memAddr & 3)));
          sel = 1 << (memAddr & 3);
        } else if (is_half_word_access()) {
          p("half word access");
          val_to_mem = ((inst.rtValue & 0xFFFFL) << (16 * (memAddr & 3)));
          sel = 3 << (memAddr & 3); // FIX
        } else { // word access
          p("word access");
          val_to_mem = inst.rtValue & 0xFFFFFFFFL;
          sel = 0xF;
        }
        p("tomem: " + Long.toHexString(val_to_mem));

        if (is_byte_access()) {
          memAddr >>= 2;
        } else if (is_half_word_access()) {
          memAddr >>= 1;
        }

        state.setPort(ADDR, Value.createKnown(BitWidth.create(24), memAddr), 1);
        state.setPort(DOUT, Value.createKnown(BitWidth.create(32), (int) val_to_mem), 1);
        state.setPort(STR, Value.createKnown(BitWidth.create(1), 1), 20);
        state.setPort(LD, Value.createKnown(BitWidth.create(1), 0), 20);
        state.setPort(SEL, Value.createKnown(BitWidth.create(4), sel), 10);
      } else if (is_mem_read()) {
        sel = 0xF;

        p("read memory!");
        if (is_byte_access()) {
          memAddr = (int) (inst.rsValue + (((inst.immediate & 0xFFFFFFFCL) << 48) >> 48));
        } else if (is_half_word_access()) {
          memAddr = (int) (inst.rsValue + (((inst.immediate & 0xFFFFFFFEL) << 48) >> 48));
        } else {
          memAddr = (int) (inst.rsValue + (((inst.immediate) << 48) >> 48));
          p("word access: " + Integer.toHexString(memAddr));
        }

        state.setPort(ADDR, Value.createKnown(BitWidth.create(24), memAddr), 1);
        state.setPort(STR, Value.createKnown(BitWidth.create(1), 0), 1);
        state.setPort(LD, Value.createKnown(BitWidth.create(1), 1), 1);
        state.setPort(SEL, Value.createKnown(BitWidth.create(4), sel), 1);
      } else {
        state.setPort(STR, Value.createKnown(BitWidth.create(1), 0), 0);
        state.setPort(LD, Value.createKnown(BitWidth.create(1), 0), 0);
        state.setPort(SEL, Value.createKnown(BitWidth.create(4), 0), 0);
      }
    }

    public void step_level(InstanceState state) {
      boolean forwardflag = false;
      long val_from_mem = 0;

      if ((memAddr == wb.inst.rsValue) && (wb.inst.opcode == 208)) {
        forwardflag = true;
      }

      if (is_mem_read()) {
        if (forwardflag) {
          val_from_mem = wb.inst.rtValue;
        } else {
          val_from_mem = state.getPortValue(DIN).toIntValue();
        }
        if (is_byte_access()) { // byte access
          val_from_mem = val_from_mem & (0xFF << (8 * (inst.immediate & 3)));
          if (is_zero_extend()) { // zero-extend?
            val_from_mem >>= (8 * (inst.immediate & 3));
          } else { // sign-extend?
            val_from_mem <<= (56 - (8 * (inst.immediate & 3)));
            val_from_mem >>= (56 - (8 * (inst.immediate & 3)));
          }
        } else if (is_half_word_access()) {
          val_from_mem = val_from_mem & (0xFFFF << (16 * (inst.immediate & 3)));
          if (is_zero_extend()) {
            val_from_mem >>= (16 * ((inst.immediate & 3) >> 1));
          } else {
            val_from_mem <<= (48 - (16 * ((inst.immediate & 3) >> 1)));
            val_from_mem >>= (48 - (16 * ((inst.immediate & 3) >> 1)));
          }
        }
        inst.rtValue = val_from_mem;
      }
    }

    private boolean is_mem_write() {
      if (inst.opcode == 185 || inst.opcode == 192 || inst.opcode == 208)
        return true;
      return false;
    }

    private boolean is_mem_read() {
      if (inst.opcode == 121 || inst.opcode == 122 || inst.opcode == 125 || inst.opcode == 126 || inst.opcode == 130)
        return true;
      return false;
    }

    private boolean is_byte_access() {
      if (inst.opcode == 121 || inst.opcode == 122 || inst.opcode == 185)
        return true;
      return false;
    }

    private boolean is_half_word_access() {
      if (inst.opcode == 125 || inst.opcode == 126 || inst.opcode == 192)
        return true;
      return false;
    }

    private boolean is_zero_extend() {
      if (inst.opcode == 122 || inst.opcode == 126)
        return true;
      return false;
    }
  }

  private class Execute extends Stage {

    public Execute() {
      super();
    }

    public void step(SPIMData data, Fetch fetch, Decode decode) {
      switch (inst.opcode) {
      case 31:
        inst.rdValue = inst.rsValue + inst.rtValue;
        break;
      case 34:
        inst.immediate <<= 16;
        inst.immediate >>= 16;
        inst.rtValue = inst.rsValue + inst.immediate; // exception
        inst.rdValue = inst.rtValue;
        break;
      case 35:
        inst.immediate <<= 16;
        inst.immediate >>= 16;
        inst.rtValue = inst.rsValue + inst.immediate;
        inst.rdValue = inst.rtValue;
        break;
      case 36:
        inst.rdValue = inst.rsValue + inst.rtValue;
        break;
      case 37:
        inst.rdValue = inst.rsValue & inst.rtValue;
        break;
      case 38:
        inst.rtValue = inst.rsValue & inst.immediate;
        inst.rdValue = inst.rtValue;
        break;
      case 39: // B
        break;
      case 40: // BAL
        break;
      case 55: // BEQ
        break;
      case 59: // BGEZAL
        break;
      case 64:
        break;
      case 67:
        break;
      case 70:
        break;
      case 71: // BLTZAL
        break;
      case 76:
        fetch.inst.flush = true;
        decode.inst.flush = false;
        break;
      case 79: // BREAK
        break;
      case 95: // CFC2
        break;
      case 99: // COP2
        break;
      case 103: // CTC2
        break;
      case 109:
        inst.rdValue = (inst.rsValue & 0xFF) / (inst.rtValue & 0xFF);
        break;
      case 112:
        inst.rdValue = (inst.rsValue & 0xFF) / (inst.rtValue & 0xFF);
        break;
      case 113: // ERET
        fetch.PC = data.regs[EPC].toIntValue();
        fetch.inst.flush = true;
        decode.inst.flush = true;
        break;
      case 115: // J
        fetch.PC = (int) (inst.immediate & 0xFFFFFFFFL);
        fetch.inst.flush = true;
        decode.inst.flush = false;
        break;
      case 116: // JAL
        inst.rd = 31;
        inst.rdValue = PC + 8;
        fetch.PC = (int) (inst.immediate & 0xFFFFFFFFL);
        fetch.inst.flush = true;
        decode.inst.flush = false;
        break;
      case 117: // JALR
        inst.rdValue = PC + 8;
        fetch.PC = (int) (inst.rsValue & 0xFFFFFFFFL);
        fetch.inst.flush = true;
        decode.inst.flush = false;
        break;
      case 119: // JR
        fetch.PC = (int) (inst.rsValue & 0xFFFFFFFFL);
        fetch.inst.flush = true;
        decode.inst.flush = false;
        break;
      case 121: // LB
        break;
      case 122: // LBU
        break;
      case 125: // LH
        break;
      case 126: // LHU
        break;
      case 127: // LL
        break;
      case 129:
        inst.rtValue = inst.immediate << 16;
        inst.rdValue = inst.rtValue;
        break;
      case 130: // LW
        break;
      case 143: // MFC0
        p("handle MFC0");
        break;
      case 152:
        if (inst.rtValue != 0)
          inst.rdValue = inst.rsValue;
        break;
      case 158:
        if (inst.rtValue == 0)
          inst.rdValue = inst.rsValue;
        inst.rdValue = inst.rdValue;
        break;
      case 163: // MTC0
        break;
      case 169:
        inst.rdValue = (inst.rsValue & 0xFF) * (inst.rtValue & 0xFF);
        break;
      case 174: // NOP
        break;
      case 175:
        inst.rdValue = ~(inst.rsValue | inst.rtValue);
        break;
      case 176:
        inst.rdValue = inst.rsValue | inst.rtValue;
        break;
      case 177:
        inst.rtValue = inst.rsValue | inst.immediate;
        inst.rdValue = inst.rtValue;
        break;
      case 185: // SB
        break;
      case 192: // SH
        break;
      case 193:
        inst.rdValue = inst.rtValue << inst.immediate;
        break;
      case 194:
        inst.rdValue = inst.rtValue << (inst.rsValue & 0x1F);
        break;
      case 195:
        inst.rdValue = (inst.rsValue < inst.rtValue) ? 1 : 0;
        break;
      case 196:
        inst.immediate <<= 16;
        inst.immediate >>= 16;
        inst.rtValue = (inst.rsValue < inst.immediate) ? 1 : 0;
        inst.rdValue = inst.rtValue;
        break;
      case 197:
        inst.immediate <<= 16;
        inst.immediate >>= 16;
        inst.rtValue = ((inst.rsValue & 0xFFFFFFFFL) < (inst.immediate & 0xFFFFFFFFL)) ? 1 : 0;
        inst.rdValue = inst.rtValue;
        break;
      case 198:
        inst.rdValue = ((inst.rsValue & 0xFFFFFFFFL) < (inst.rtValue & 0xFFFFFFFFL)) ? 1 : 0;
        break;
      case 200:
        inst.rdValue = inst.rtValue >> inst.immediate;
        break;
      case 201:
        inst.rdValue = inst.rtValue >> (inst.rsValue & 0x1F);
        break;
      case 202:
        inst.rtValue <<= 32;
        inst.rtValue >>>= inst.immediate;
        inst.rtValue >>= 32;
        inst.rdValue = inst.rtValue;
        break;
      case 203:
        inst.rdValue = (inst.rtValue & 0xFFFFFFFFL) >>> (inst.rsValue & 0x1F);
        break;
      case 205:
        inst.rdValue = inst.rsValue - inst.rtValue; // Exception?
        break;
      case 207:
        inst.rdValue = inst.rsValue - inst.rtValue;
        break;
      case 208:
        inst.rdValue = inst.rtValue; // SW
        break;
      case 219: // syscall

        break;
      case 220: // trap
        break;
      case 221:
        break;
      case 222:
        break;
      case 223:
        break;
      case 224:
        break;
      case 225: // all traps
        break;
      case 233: // more traps
        break;
      case 234:
        break;
      case 235:
        break;
      case 236:
        break;
      case 237:
        break;
      case 238:
        break;
      case 243:
        inst.rdValue = inst.rsValue ^ inst.rtValue;
        break;
      case 244:
        inst.rtValue = inst.rsValue ^ inst.immediate;
        inst.rdValue = inst.rtValue;
      }
    }
  }

  private class Decode extends Stage {
    private Vector<Integer> hazardList;
    private Instruction instructionSave;
    private int savePC;
    private boolean isStall;
    public boolean nop;

    public int btarget;
    public int jitarget;
    public int jrtarget;
    public boolean branch_taken;
    public boolean jump;

    private boolean rs_ex_hazard;
    private boolean rt_ex_hazard;
    private boolean rs_mem_hazard;
    private boolean rt_mem_hazard;
    private boolean rs_wb_hazard;
    private boolean rt_wb_hazard;

    private boolean ld_ex_load_use;
    private boolean ld_mem_load_use;

    public Decode() {
      super();
      instructionSave = new Instruction("NOP");
      hazardList = new Vector<Integer>(3);
      hazardList.addElement(new Integer(0));
      hazardList.addElement(new Integer(0));
      hazardList.addElement(new Integer(0));
    }

    public void step(SPIMData data, Memory mem, WriteBack wb, Fetch fetch, Execute execute) {

      rs_ex_hazard = false;
      rt_ex_hazard = false;
      rs_mem_hazard = false;
      rt_mem_hazard = false;
      rs_wb_hazard = false;
      rt_wb_hazard = false;

      /* forwarding */
      if ((execute.inst.flush == false) && inst.rs == (hazardList.elementAt(0)).intValue()
          && (hazardList.elementAt(0)).intValue() != 0) {
        rs_ex_hazard = true;
      }

      if ((execute.inst.flush == false) && inst.rt == (hazardList.elementAt(0)).intValue()
          && (hazardList.elementAt(0)).intValue() != 0) {
        rt_ex_hazard = true;
      }

      if ((mem.inst.flush == false) && inst.rs == (hazardList.elementAt(1)).intValue()
          && (hazardList.elementAt(1)).intValue() != 0) {
        rs_mem_hazard = true;
      }

      if ((mem.inst.flush == false) && inst.rt == (hazardList.elementAt(1)).intValue()
          && (hazardList.elementAt(1)).intValue() != 0) {
        rt_mem_hazard = true;
      }

      if ((wb.inst.flush == false) && inst.rs == (hazardList.elementAt(2)).intValue()
          && (hazardList.elementAt(2)).intValue() != 0) {
        rs_wb_hazard = true;
      }

      if ((wb.inst.flush == false) && inst.rt == (hazardList.elementAt(2)).intValue()
          && (hazardList.elementAt(2)).intValue() != 0) {
        rt_wb_hazard = true;
      }

      hazardList.setElementAt(hazardList.elementAt(1), 2);
      hazardList.setElementAt(hazardList.elementAt(0), 1);
      hazardList.setElementAt(new Integer(inst.rd), 0);

      if (fetch.stall) {
        instructionSave = inst;
        savePC = PC;
        isStall = true;
        inst = new Instruction("NOP");
        inst.rsValue = 0;
        inst.rtValue = 0;
        inst.rdValue = 0;
      } else {
        if (isStall) {
          isStall = false;
          inst = instructionSave;
          PC = savePC;
        }
      }
    }

    // execute when clock is low, to propagate value from staller and forwarder to
    // fetch stage.
    public void step_level(SPIMData data, Memory mem, WriteBack wb, Fetch fetch, Execute execute) {

      p("---step level---");
      if (decode.nop) {
        inst = new Instruction("NOP");
        inst.rsValue = 0;
        inst.rtValue = 0;
      }

      if (inst.opcode == 143) {
        switch (inst.rd) {
        case 8:
          inst.rdValue = data.regs[BADVADDR].toIntValue();
          break;
        case 12:
          inst.rdValue = data.regs[STATUS].toIntValue();
          break;
        case 13:
          inst.rdValue = data.regs[CAUSE].toIntValue();
          break;
        case 14:
          inst.rdValue = data.regs[EPC].toIntValue();
          break;
        }
        return;
      }

      if (rs_ex_hazard) {
        inst.rsValue = execute.inst.rdValue;
      } else if (rs_mem_hazard) {
        inst.rsValue = mem.inst.rdValue;
        p("forwarded rsV with rdValue 0x" + Long.toHexString(mem.inst.rdValue));
      } else if (rs_wb_hazard) {
        inst.rsValue = wb.inst.rdValue;
      } else {
        Value _vrs = data.regs[inst.rs];
        inst.rsValue = _vrs.toIntValue();
        p("load rsV=" + Long.toHexString(inst.rsValue));
      }

      if (rt_ex_hazard) {
        inst.rtValue = execute.inst.rdValue;
      } else if (rt_mem_hazard) {
        inst.rtValue = mem.inst.rdValue;
      } else if (rt_wb_hazard) {
        inst.rtValue = wb.inst.rdValue;
      } else {
        if (inst.is_immediate()) {
          inst.rtValue = inst.immediate;
        } else {
          Value _vrt = data.regs[inst.rt];
          inst.rtValue = _vrt.toIntValue();
        }
        p("load rtV=" + Long.toHexString(inst.rtValue));
      }

      Value _vrd = data.regs[inst.rd];
      inst.rdValue = _vrd.toIntValue();

      p("hazard: rs - ex, mem, wb: " + rs_ex_hazard + " " + rs_mem_hazard + " " + rs_wb_hazard);
      p("hazard: rt - ex, mem, wb: " + rt_ex_hazard + " " + rt_mem_hazard + " " + rt_wb_hazard);

      branch_calc();
      jump_calc();
    }

    private void branch_calc() {
      int offset;
      offset = sign_extend(inst.immediate, 18);
      btarget = PC + offset;

      switch (inst.opcode) {
      case 39: // B
        branch_taken = true;
        break;
      case 40: // BAL
        branch_taken = true;
        inst.rd = 31;
        inst.rdValue = PC + 8;
        break;
      case 55: // BEQ
        branch_taken = (inst.rsValue == inst.rtValue);
        break;
      case 58: // BGEZ
        branch_taken = (inst.rsValue >= 0);
        break;
      case 64: // BGTZ
        branch_taken = (inst.rsValue > 0);
        break;
      case 67: // BLEZ
        branch_taken = (inst.rsValue <= 0);
        break;
      case 70: // BLTZ
        branch_taken = (inst.rsValue < 0);
        break;
      case 76: // BNE
        branch_taken = (inst.rsValue != inst.rtValue);
        break;
      default:
        branch_taken = false;
        btarget = 0;
        break;
      }

      p("branch_taken: " + branch_taken + " " + Integer.toHexString(btarget));
    }

    private void jump_calc() { // FIX
      switch (inst.opcode) {
      case 115: // j
        jitarget = ((PC & 0xF0000000) | inst.immediate);
        jump = true;
        p("jitarget: " + Integer.toHexString(jitarget));
        break;
      case 116: // jal
        jitarget = (PC & 0xF0000000) | (inst.immediate << 2);
        inst.rd = 31;
        inst.rdValue = PC + 8;
        jump = true;
        p("jitarget: " + Integer.toHexString(jitarget));
        break;
      case 117: // jalr
        jrtarget = (int) (inst.rsValue & 0xFFFFFFFFL);
        inst.rdValue = PC + 8;
        jump = true;
        p("jrtarget: " + Integer.toHexString(jrtarget));
        break;
      case 119: // jr
        jrtarget = (int) (inst.rsValue & 0xFFFFFFFFL);
        jump = true;
        p("jrtarget: " + Integer.toHexString(jrtarget));
        break;
      default:
        jump = false;
        jitarget = 0;
        jrtarget = 0;
        break;
      }
    }
  }

  private class Fetch extends Stage {
    public boolean stall;

    public Fetch() {
      super();
      stall = false;
      PC = -4;
    }

    public void step(Instruction inst, SPIMData data, Decode decode) {

      if (!stall) {
        if (decode.branch_taken) {
          p("branch_taken!");
          PC = decode.btarget;
          // Value v = Value.createKnown(BitWidth.create(32), decode.btarget);
          // data.regs[32] = v;
          // PC = data.regs[32].toIntValue();
        } else {
          if (decode.jump) {
            if (execute.inst.is_ji()) {
              PC = decode.jitarget;
              // Value v = Value.createKnown(BitWidth.create(32), decode.jitarget);
              // data.regs[32] = v;
              // PC = data.regs[32].toIntValue();
              p("branch to jitarget:" + Integer.toHexString(decode.jitarget));
            } else if (execute.inst.is_jr()) {
              PC = decode.jrtarget;
              // Value v = Value.createKnown(BitWidth.create(32), decode.jrtarget);
              // data.regs[32] = v;
              // PC = data.regs[32].toIntValue();
              p("branch to jrtarget:" + Integer.toHexString(decode.jrtarget));
            }
          } else {
            /*
             * if (data.regs[32].toIntValue() == 0){ PC = 0; Value v =
             * Value.createKnown(BitWidth.create(32), 4); data.regs[32] = v; p("PC init"); }
             * else { PC = data.regs[32].toIntValue(); p("PC " + Integer.toHexString(PC));
             * Value v = Value.createKnown(BitWidth.create(32), PC+4); data.regs[32] = v;
             * p("PC increment " + Integer.toHexString(PC)); }
             */
            PC += 4;
            p("PC increment " + Integer.toHexString(PC));
          }
        }
        if (inst.valid()) {
          this.inst = inst;
          inst.flush = false; // valid
        }
      } else {
        p("pc = decode pc");
        PC = decode.PC;
        // Value v = Value.createKnown(BitWidth.create(32), decode.PC);
        // data.regs[32] = v;
        // PC = data.regs[32].toIntValue();
        if (inst.valid()) {
          this.inst = inst;
          inst.flush = false;
        }
      }
    }
  }

  private class Stage {
    protected Instruction inst;
    protected int PC;

    public Stage() {
      inst = new Instruction("NOP");
      PC = -4;
    }
  }
}
