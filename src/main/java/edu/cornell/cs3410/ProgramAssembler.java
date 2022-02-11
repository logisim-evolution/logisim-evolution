package edu.cornell.cs3410;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramAssembler {

    private static final int NO_OP = 0x13; //really?  completely separate from other nops

    private ProgramAssembler() {

    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("usage: ProgramAssembler <risc-v-asm-file>");
            System.exit(1);
        }
        Listing code = new Listing();
        try {
            code.load(new File(args[0]));
            for (Segment segment : code.seg) {
                for (int i = 0; i < segment.data.length; i++) {
                    int pc = segment.start_pc + i;
                    int instr = code.instr(pc);
                    System.out.println(toHex(pc * 4, 8) + " : " + toHex(instr, 8) + " : " + disassemble(instr, pc * 4));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class Segment implements Cloneable {
        public int start_pc;
        public int data[];

        public Segment(int pc, int d[]) {
            start_pc = pc;
            data = d;
        }
    }

    static class Listing implements Cloneable {
        private String src = "";
        private Segment seg[] = new Segment[0];
        private ProgramState state;
        private ArrayList<String> src_lines = new ArrayList<String>();
        private ArrayList<Integer> addr_map = new ArrayList<Integer>();

        public Listing() {
        }

        public Listing(String value) throws IOException {
            setSource(value);
        }

        public void setListener(ProgramState state) {
            this.state = state;
        }

        public void load(File file) throws IOException {
            String s = readFully(file);
            setSource(s);
        }

        public ProgramState getState() {
            return state;
        }

        public String getSource() {
            return src;
        }

        public int getLineCount() {
            return src_lines.size();
        }

        public String getLine(int index) {
            return src_lines.get(index);
        }

        public int getAddressOf(int index) {
            Integer i = addr_map.get(index);
            if (i == null) {
                return -1;
            }
            return i.intValue();
        }

        public void setSource(String s) throws IOException {
            ArrayList<String> sl = splitLines(s);
            ArrayList<Integer> am = new ArrayList<Integer>();
            seg = assemble(sl, 0, am);
            src = s;
            addr_map = am;
            src_lines = sl;
        }

        public boolean isEmpty() {
            return seg.length == 0;
        }

        int instr(int i) {
            Segment s = segmentOf(i);
            if (s != null)
                return s.data[i - s.start_pc];
            else
                return NO_OP;
        }

        Segment segmentOf(int i) {
            for (int s = 0; s < seg.length; s++) {
                if (i >= seg[s].start_pc && i < seg[s].start_pc + seg[s].data.length) {
                    // System.out.println("segment of "+i+" is "+seg[s].start_pc);
                    return seg[s];
                }
            }
            // System.out.println("segment of "+i+" is null");
            return null;
        }

        @Override
        public Listing clone() {
            try {
                return (Listing) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    private static String toHex(int i, int digits) {
        if (digits > 8)
            digits = 8;
        String s = Long.toHexString(i & 0xffffffffL);
        if (s.length() >= digits)
            return "0x" + s;
        else
            return "0x00000000".substring(0, 2 + digits - s.length()) + s;
    }

    private static String readFully(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        StringBuffer buf = new StringBuffer();
        while ((line = in.readLine()) != null)
            buf.append(line + "\n");
        return buf.toString();
    }

    private static ArrayList<String> splitLines(String src) throws IOException {
        BufferedReader in = new BufferedReader(new StringReader(src));
        String line;
        ArrayList<String> buf = new ArrayList<String>();
        while ((line = in.readLine()) != null)
            buf.add(line);
        return buf;
    }

    private static Pattern pat0 = Pattern.compile("\\s+");
    private static Pattern pat1 = Pattern.compile("\\s*,\\s*");

    private static ArrayList<String> normalize(ArrayList<String> lines) {
        ArrayList<String> res = new ArrayList<String>();
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            int i = line.indexOf('#');
            if (i == 0)
                line = "";
            else if (i > 0)
                line = line.substring(0, i);
            line = line.trim();
            line = pat0.matcher(line).replaceAll(" ");
            line = pat1.matcher(line).replaceAll(",");
            if (line.length() == 0)
                res.add(null);
            else
                res.add(line);
        }
        return res;
    }

    // some patterns for recognizing the following types of assembly parameters.
    static String _reg = "(x[0-9]|x30|x31|x[1-2][0-9]|zero|a[0-7]|t[0-6]|s[0-9]|s1[0-1]|gp|sp|fp|ra|tp)"; // List of
                                                                                                          // accepted
                                                                                                          // risc-v
                                                                                                          // register
                                                                                                          // names
    static String __pc = "(?:pc|PC)";
    static String __hex = "0x[a-fA-F0-9]+";
    static String __decimal = "-?\\d+";
    static String __label = "[a-zA-Z]\\w*";

    // immediate regex for other immediate instructions
    static String _immNoLabel = "(" + __hex + "|" + __decimal + ")";

    // immediate regex that can be used for jumping
    static String _imm = "(" + __hex + "|" + __decimal + "|" + __pc + "|" + __label + ")";

    private static int parseSegmentAddress(int lineno, String addr) throws IOException {
        if (addr.toLowerCase().startsWith("0x"))
            return Integer.parseInt(addr.substring(2), 16);
        char c = addr.charAt(0);
        if ((c >= '0' && c <= '9'))
            return Integer.parseInt(addr);
        throw new ParseException("Line " + (lineno + 1) + ": illegal address '" + addr + "' in assembly directive");
    }

    static class ParseException extends IOException {
        private static final long serialVersionUID = 4648870901015801834L;
        StringBuffer msg;
        int count = 0;

        public ParseException() {
            msg = new StringBuffer();
        }

        public ParseException(String m) {
            this();
            add(m);
        }

        public void add(String m) {
            msg.append("\n");
            msg.append(m);
            count++;
        }

        public void add(ParseException e) {
            msg.append(e.msg.toString());
            count += e.getCount();
        }

        public String getMessage() {
            return "Assembling risc-v instructions: " + count + (count == 1 ? " error:" : " errors:") + msg.toString();
        }

        public int getCount() {
            return count;
        }
    }

    /*
     * Pass 1 takes in the lines of the assembly file and splits it up into
     * individual commands, addresses, and enters each of these lines into a hashmap
     * for future decoding usage. Furthermore, it breaks down jump and function
     * labels via regex handling and enters those into a hashmap for later
     * reference.
     */
    static Pattern pat_label = Pattern.compile("(" + __label + ")");

    private static HashMap<String, Integer> pass1(ArrayList<String> lines, int start_address,
            ArrayList<Integer> addr_map) throws IOException {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        int addr = start_address;
        addr_map.clear();
        ParseException err = new ParseException();
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null) {
                addr_map.add(null);
                continue;
            }
            int i;
            if (line.toLowerCase().startsWith(".text")) {
                i = line.indexOf(' ');
                if (i > 0) {
                    try {
                        int a = parseSegmentAddress(lineno, line.substring(i + 1));
                        if ((a & 3) != 0)
                            err.add("Line " + (lineno + 1) + ": mis-aligned address '" + line.substring(i + 1)
                                    + "' in .text assembly directive");
                        addr = a & ~3;
                    } catch (ParseException e) {
                        err.add(e);
                    }
                }
                addr_map.add(null);
                continue;
            }
            if (line.toLowerCase().startsWith(".word")) {
                addr_map.add(new Integer(addr));
                addr += 4;
                continue;
            }
            if (line.startsWith(".")) {
                err.add("Line " + (lineno + 1) + ": unrecognized assembly directive '" + line + "'");
                continue;
            }

            i = line.indexOf(':');
            if (i >= 0) {
                String name = line.substring(0, i).trim();
                if (name.length() == 0) {
                    err.add("Line " + (lineno + 1) + ": expected label name before ':'");
                    continue;
                }
                Matcher m = pat_label.matcher(name);
                if (name.equalsIgnoreCase("pc") || !m.matches()) {
                    err.add("Line " + (lineno + 1) + ": illegal label name '" + name + "' before ':'");
                    continue;
                }
                map.put(name, new Integer(addr));
                if (i < line.length() - 1) {
                    // label: instruction
                    line = line.substring(i + 1).trim();
                    lines.set(lineno, line);
                    addr_map.add(new Integer(addr));
                    addr += 4;
                } else {
                    // label:
                    addr_map.add(null);
                    lines.set(lineno, null);
                }
            } else {
                addr_map.add(new Integer(addr));
                addr += 4;
            }
        }
        if (err.getCount() > 0)
            throw err;
        return map;
    }

    // only the cmds (commands) hashmaps is truly necessary
    static HashMap<String, Command> cmds = new HashMap<String, Command>();
    static HashMap<Integer, Command> opcodes = new HashMap<Integer, Command>();
    static HashMap<Integer, Command> fcodes = new HashMap<Integer, Command>();
    static HashMap<Integer, Command> socodes = new HashMap<Integer, Command>();

    // returns an n-bit number (with leading zeros for n<32).
    // if SIGNED_ABSOLUTE, the accepted inputs are:
    // - hex (with no more than n bits)
    // - decimal (positive or negative values in the range -2^(n-1)..2^(n-1)-1)
    // - "pc" (as long as it is in the range)
    // - label (as long as it is in the range)
    // if UNSIGNED_ABSOLUTE, the accepted inputs are:
    // - as above, but no negative decimals, and with a range check of 0..2^n-1
    // instead
    // if SIGNED_RELATIVE, the accepted inputs are:
    // - hex or decimal, as above (no relative offsetting)
    // - "pc" or label, minus (addr+4) (as long as this result is in the range)
    // if ANY_ABSOLUTE, the accepted inputs are:
    // - anything that fits in n bits)
    private static enum Type {
        SIGNED_RELATIVE, SIGNED_ABSOLUTE, UNSIGNED_ABSOLUTE, ANY_ABSOLUTE
    };

    /*
     * Resolve takes in an immediate value and converts it from String to int after
     * parsing the string to modify it correctly depending on the type of immediate
     * it is. Returns the correct (int) immediate value.
     */
    private static int resolve(int lineno, String imm, int addr, HashMap<String, Integer> sym, Type type, int nbits)
            throws IOException {
        int offset = (type == Type.SIGNED_RELATIVE ? addr + 4 : 0);
        long min = (type == Type.UNSIGNED_ABSOLUTE ? 0 : (-1L << (nbits - 1)));
        long max = (type == Type.UNSIGNED_ABSOLUTE ? ((1L << nbits) - 1) : ((1L << (nbits - 1)) - 1));
        int mask = (int) (1L << nbits) - 1;
        long val;
        try {
            if (imm.length() == 0)
                throw new NumberFormatException();
            char c = imm.charAt(0);
            if (imm.equalsIgnoreCase("pc")) {
                // Unsure about this functionality
                // How do we encode PC into 32 bit instructions.
                val = ((long) addr & 0xffffffffL) - offset;
            } else if (imm.toLowerCase().startsWith("0x")) {
                val = Long.parseLong(imm.substring(2), 16);
                // nb: this check is different,
                // to allow 0xffff to mean "-1" for signed abs/relative,
                // but 65535 for unsigned absolute
                if ((val & mask) != val) {
                    throw new ParseException("Line " + (lineno + 1) + ": overflow in " + type + " '" + imm + "' ("
                            + nbits + " bits maximum)");
                }
                return (int) (val & mask);
            } else if ((c == '-') || (c >= '0' && c <= '9')) {
                // for integer immediates
                if (type == Type.UNSIGNED_ABSOLUTE && c == '-') {
                    // TODO: Finish fixing the unsigned - value conversion
                    min = -1 * max - 1;
                }

                val = Long.parseLong(imm);
            } else {
                // If control instruction uses Label, get address of label
                Integer a = sym.get(imm);
                if (a == null)
                    throw new ParseException("Line " + (lineno + 1) + ": expecting " + type
                            + ", but no such label or number '" + imm + "'");
                val = ((long) a.intValue() & 0xffffffffL) - offset;
                imm = imm + " (" + val + ")";
            }

            if (type == Type.ANY_ABSOLUTE) {
                // nb: this check is different,
                // to allow 0xfff to mean "-1" for signed abs/relative,
                // but 4096 for unsigned absolute
                if ((val & mask) != val) {
                    throw new ParseException("Line " + (lineno + 1) + ": overflow in " + type + " '" + imm + "' ("
                            + nbits + " bits maximum)");
                }
            } else {
                if (val < min || val > max) {
                    throw new ParseException("Line " + (lineno + 1) + ": overflow in " + type + " '" + imm
                            + "' : allowed range is " + min + " (" + toHex((int) min & mask, 1) + ") to " + max + " ("
                            + toHex((int) max & mask, 1) + ")");
                }
            }
            return (int) (val & mask);
        } catch (NumberFormatException e) {
            throw new ParseException("Line " + (lineno + 1) + ": invalid " + type + " '" + imm + "'");
        }
    }

    /*
     * Main abstract command class to create individual instruction objects.
     */
    private static abstract class Command {
        String name;
        int opcode;

        Command(String name, int op) {
            this.name = name;
            opcode = op;
            cmds.put(name, this);
        }

        abstract String decode(int addr, int instr);

        abstract int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException;
    }

    private static class Nop extends Command {
        Nop(String name, int op) {
            super(name, op);
        }

        String decode(int addr, int instr) {
            return name;
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
            return 0x13;
        }
    }

    private static class Syscall extends Command {
        Syscall(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }

        String decode(int addr, int instr) {
            return name;
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
            return 6;
        }
    }

    private static class Eret extends Command {
        Eret(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }

        String decode(int addr, int instr) {
            return name;
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
            return (8 << 26) | (1 << 25) | 6;
        }
    }

    static Pattern pat_word = Pattern.compile(_imm);

    private static class Word extends Command {
        Word(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_word.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line " + (lineno + 1) + ": '" + name + "' expects integer argument");
            }

            int word = resolve(lineno, m.group(1), addr, sym, Type.ANY_ABSOLUTE, 32);
            return word;
        }

        String decode(int addr, int instr) {
            return name + " " + toHex(instr, 8);
        }
    }

    private static int reg(String r) throws NumberFormatException {
        /*
         * Register ABI Name Description Saver x0 zero Hard-wired zero — x1 ra Return
         * address Caller x2 sp Stack pointer Callee x3 gp Global pointer — x4 tp T r a
         *  pointer — x5–7 t0–2 Temporaries Caller x8 s0/fp Saved regis er/fr me point
         *  Callee x9 s1 Saved register Callee x10–11 a0–1 Fun tion arg men s/r rn 
         *  al Caller x12–17 a2–7 Function arguments Cal er x18 27 s2– 1 Saved s
         * ers Callee x28–31 t3–6 Temporaries Cal er
         */
        int i = 0;
        switch (r.charAt(0)) {
        case 'z':
            return 0;
        case 'r':
            return 1;
        case 's':
            if (r.charAt(1) == 'p')
                return 2;
            i = Integer.parseInt(r.substring(1));
            if (i == 0 || i == 1)
                return 8 + i;
            else if (i > 1 && i <= 11)
                return 16 + i;
        case 'g':
            return 3;
        case 't':
            if (r.charAt(1) == 'p')
                return 4;
            i = Integer.parseInt(r.substring(1));
            if (i <= 2)
                return 5 + i;
            else if (i > 2 && i <= 6)
                return 25 + i;
        case 'f':
            return 8;
        case 'a':
            i = Integer.parseInt(r.substring(1));
            return 10 + i;
        default:
            return Integer.parseInt(r.replaceAll("x", "")); // hits default case of "x[0-31]"
        }
    }

    private static abstract class ImmInst extends Command {
        int func3;
        boolean shift = false;

        ImmInst(String name, int op, int f3) {
            super(name, op);
            this.func3 = f3;
            if ((func3 == 5 || func3 == 1) && op == 0x13)
                shift = true;
            opcodes.put(new Integer(op), this);
        }

        int encode(String rd, String rs1, int imm, int lineno) throws IOException {
            try {
                int dest = reg(rd);
                int src = reg(rs1);
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid destination register: x" + dest);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid source register: x" + src);
                }

                // 3/10/2019
                if (shift && (imm > 31 || imm < 0)) {
                    throw new ParseException("Line " + (lineno + 1) + " Invalid Immediate:" + imm + " for Shift");
                }

                if (shift && name.charAt(2) == 'a') {
                    return (1 << 30) | (imm << 20) | (src << 15) | (func3 << 12) | (dest << 7) | opcode;
                }
                return (imm << 20) | (src << 15) | (func3 << 12) | (dest << 7) | opcode;
            } catch (NumberFormatException e) {
                throw new ParseException("Line " + (lineno + 1) + ": invalid arguments to '" + name + "': "
                        + e.getMessage() + "registers: (" + rd + ", " + rs1 + ") = (" + Integer.toString(reg(rd)) + ","
                        + Integer.toString(reg(rs1)) + ")");
            }
        }

        String rD(int instr) {
            return "x" + ((instr >> 7) & 0x1f);
        }

        String rs1(int instr) {
            return "x" + ((instr >> 15) & 0x1f);
        }

        String sImm(int instr) {
            if (shift) {
                return "" + toHex((instr >> 20) & 0x1f, 2);
            } else
                return "" + toHex((instr >> 20) & 0xfff, 3);
        }
    }

    static Pattern pat_Imm = Pattern.compile(_reg + "," + _reg + "," + _imm);

    private static class ImmReg extends ImmInst {
        Type itype;
        boolean debug;

        ImmReg(String name, int op, int f3, boolean signed, boolean debug) {
            super(name, op, f3);
            this.debug = debug;
            itype = (signed ? Type.SIGNED_ABSOLUTE : Type.UNSIGNED_ABSOLUTE);
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_Imm.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line " + (lineno + 1) + ": '" + name + "' expects xD, xS, Imm " + itype
                        + " with args: " + args);
            }

            int imm = resolve(lineno, m.group(3), addr, sym, itype, 12);

            return encode(m.group(1), m.group(2), imm, lineno);
        }

        String decode(int addr, int instr) {
            return name + " " + rD(instr) + ", " + rs1(instr) + ", " + sImm(instr);
        }
    }

    static Pattern pat_Load = Pattern.compile(_reg + "," + _imm + "\\(" + _reg + "\\)");

    private static class Load extends ImmInst {
        Type itype;
        boolean debug;

        Load(String name, int op, int f3, boolean debug) {
            super(name, op, f3);
            this.debug = debug;
            itype = Type.SIGNED_ABSOLUTE;
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_Load.matcher(args);
            if (!m.matches()) {
                throw new ParseException(
                        "Line " + (lineno + 1) + ": '" + name + "' expects xD, Imm(xS) with args:" + args);
            }

            int imm = resolve(lineno, m.group(2), addr, sym, itype, 12);
            // if (debug) {throw new ParseException(""+toHex(encode(m.group(1), m.group(3),
            // imm, lineno),8));}
            // if (debug) {throw new ParseException("Args: " + args + ", register values: "
            // + "x" + reg(m.group(1)) + ", x" + reg(m.group(3)));}
            return encode(m.group(1), m.group(3), imm, lineno);
        }

        String decode(int addr, int instr) {
            return name + " " + rD(instr) + ", " + sImm(instr) + "(" + rs1(instr) + ")";
        }
    }

    private static abstract class RegInst extends Command {
        int func7;
        int func3;

        RegInst(String name, int op, int f7, int f3) {
            super(name, op);
            this.func7 = f7;
            this.func3 = f3;
            opcodes.put(new Integer(op), this);
        }

        int encode(String rd, String rs1, String rs2, int lineno) throws IOException {
            try {
                int dest = reg(rd);
                int src = reg(rs1);
                int tgt = reg(rs2);
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid destination register: x" + dest);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid source register: x" + src);
                }
                if ((tgt & 0x1f) != tgt) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid source register: x" + src);
                }
                return (func7 << 25) | (tgt << 20) | (src << 15) | (func3 << 12) | (dest << 7) | opcode;
            } catch (NumberFormatException e) {
                throw new ParseException(
                        "Line " + (lineno + 1) + ": invalid arguments to '" + name + "': " + e.getMessage()
                                + "registers: (" + rd + ", " + rs1 + ", " + rs2 + ") = (" + Integer.toString(reg(rd))
                                + ", " + Integer.toString(reg(rs1)) + ", " + Integer.toString(reg(rs2)) + ")");
            }
        }

        String rD(int instr) {
            return "x" + ((instr >> 7) & 0x1f);
        }

        String rs1(int instr) {
            return "x" + ((instr >> 15) & 0x1f);
        }

        String rs2(int instr) {
            return "x" + ((instr >> 20) & 0x1f);
        }
    }

    static Pattern pat_RegReg = Pattern.compile(_reg + "," + _reg + "," + _reg);

    private static class RegReg extends RegInst {
        boolean debug;

        RegReg(String name, int op, int f7, int f3, boolean debug) {
            super(name, op, f7, f3);
            this.debug = debug;
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_RegReg.matcher(args);
            if (!m.matches()) {
                throw new ParseException(
                        "Line " + (lineno + 1) + ": '" + name + "' expects xD, xS, xT with args:" + args);
            }
            // if (debug) {throw new ParseException(""+toHex(encode(m.group(1), m.group(2),
            // m.group(3), lineno),8));}
            // if (debug) {throw new ParseException("Args: " + args + ", register values: "
            // + "x" + reg(m.group(1)) + ", x" + reg(m.group(2)) + ", x" +
            // reg(m.group(3)));}
            return encode(m.group(1), m.group(2), m.group(3), lineno);
        }

        String decode(int addr, int instr) {
            return name + " " + rD(instr) + ", " + rs1(instr) + ", " + rs2(instr);
        }
    }

    private static abstract class BrStInst extends Command {
        int func3;

        BrStInst(String name, int op, int f3) {
            super(name, op);
            this.func3 = f3;
            opcodes.put(new Integer(op), this);
        }

        int encode(String rs1, String rs2, int imm7, int imm5, int lineno) throws IOException {
            try {
                int src = reg(rs1);
                int tgt = reg(rs2);
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid source register: x" + src);
                }
                if ((tgt & 0x1f) != tgt) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid source register: x" + src);
                }
                return (imm7 << 25) | (tgt << 20) | (src << 15) | (func3 << 12) | (imm5 << 7) | opcode;
            } catch (NumberFormatException e) {
                throw new ParseException("Line " + (lineno + 1) + ": invalid arguments to '" + name + "': "
                        + e.getMessage() + "registers: (" + rs1 + ", " + rs2 + ") = (" + Integer.toString(reg(rs1))
                        + ", " + Integer.toString(reg(rs2)) + ")");
            }
        }

        String rs1(int instr) {
            return "x" + ((instr >> 15) & 0x1f);
        }

        String rs2(int instr) {
            return "x" + ((instr >> 20) & 0x1f);
        }
    }

    static Pattern pat_Store = Pattern.compile(_reg + "," + _imm + "\\(" + _reg + "\\)");

    private static class Store extends BrStInst {
        boolean debug;

        Store(String name, int op, int f3, boolean debug) {
            super(name, op, f3);
            this.debug = debug;
        }

        // sImm for Store instructions.
        String sImm(int instr) {
            int i11_5 = (instr >>> 25) & 0x7f;
            int i4_0 = (instr >>> 7) & 0x1f;
            return "" + toHex(((i11_5 << 5) | i4_0) & 0xfff, 3);
        }

        String decode(int addr, int instr) {
            return name + " " + rs2(instr) + ", " + sImm(instr) + "(" + rs1(instr) + ")";
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_Store.matcher(args);
            if (!m.matches()) {
                throw new ParseException(
                        "Line " + (lineno + 1) + ": '" + name + "' expects xT, Imm(xS) with args: " + args);
            }
            int imm = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_ABSOLUTE, 12);
            int imm7 = (imm >> 5) & 0x7f;
            int imm5 = imm & 0x1f;
            // if (debug) {throw new ParseException(""+toHex(encode(m.group(3), m.group(1),
            // imm7, imm5, lineno),8));}
            // if (debug) {throw new ParseException("Args: " + args + ", register values: "
            // + "x" + reg(m.group(3)) + ", x" + reg(m.group(1)));}
            return encode(m.group(3), m.group(1), imm7, imm5, lineno);
        }
    }

    static Pattern pat_Branch = Pattern.compile(_reg + "," + _reg + "," + _imm);
    static Pattern pat_LabBr = Pattern.compile(_reg + "," + _reg + "," + __label);

    private static class Branch extends BrStInst {
        boolean debug;

        Branch(String name, int op, int f3, boolean debug) {
            super(name, op, f3);
            this.debug = debug;
        }

        // sImm function of command object
        String sImm(int instr) {
            int i12 = (instr >>> 31) & 1;
            int i10_5 = (instr >>> 25) & 0x3f;
            int i4_1 = (instr >>> 8) & 0xf;
            int i11 = (instr >>> 7) & 1;
            int imm = (i12 << 11) | (i11 << 10) | (i10_5 << 4) | i4_1;
            imm <<= 1;
            return "" + toHex(imm & 0x1fff, 4);
        }

        String decode(int addr, int instr) {
            return name + " " + rs1(instr) + ", " + rs2(instr) + ", " + sImm(instr);
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_Branch.matcher(args);
            if (!m.matches()) {
                throw new ParseException(
                        "Line " + (lineno + 1) + ": '" + name + "' expects xS, xT, Imm ;with args: " + args);
            }

            // Resolve the 13 bit immediate
            int imm = resolve(lineno, m.group(3), addr, sym, Type.SIGNED_ABSOLUTE, 13);

            Matcher n = pat_LabBr.matcher(args);
            if (n.matches()) {
                imm = imm - addr;
            }
            imm = (imm >>> 1) & 0xfff;

            int imm12 = (imm >> 11) & 1;
            int imm10_5 = (imm >> 4) & 0x3f;
            int imm4_1 = imm & 0xf;
            int imm11 = (imm >> 10) & 1;

            int imm7 = (imm12 << 6) | imm10_5;
            int imm5 = (imm4_1 << 1) | imm11;

            return encode(m.group(1), m.group(2), imm7, imm5, lineno);
        }
    }

    static Pattern pat_LuiAPC = Pattern.compile(_reg + "," + _imm);

    private static class LuiAPCInst extends Command {
        boolean debug;

        LuiAPCInst(String name, int op, boolean debug) {
            super(name, op);
            opcodes.put(new Integer(op), this);
            this.debug = debug;
        }

        String decode(int addr, int instr) {
            return name + " " + rD(instr) + ", " + sImm(instr);
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            try {
                Matcher m = pat_LuiAPC.matcher(args);
                if (!m.matches()) {
                    throw new ParseException(
                            "Line " + (lineno + 1) + ": '" + name + "' expects xD, Imm with args: " + args);
                }

                int imm = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_ABSOLUTE, 20);
                int dest = reg(m.group(1));
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid destination register: x" + dest);
                }
                return (imm << 12) | (dest << 7) | opcode;
            } catch (NumberFormatException e) {
                Matcher m = pat_LuiAPC.matcher(args);
                throw new ParseException(
                        "Line " + (lineno + 1) + ": invalid arguments to '" + name + "': " + e.getMessage()
                                + " registers: (" + m.group(2) + ") = (" + Integer.toString(reg(m.group(2))) + ")");
            }
        }

        String rD(int instr) {
            return "x" + ((instr >> 7) & 0x1f);
        }

        String sImm(int instr) {
            return "" + toHex((instr >> 12) & 0xfffff, 5);
        }
    }

    /*
     * This class represents an instance of the Jump and Link instruction A
     * well-formed JAL instruction is JAL rd, imm : where an imm can take the form
     * of a hex or decimal number or a label. This immediate is 20 bits long.
     */
    static Pattern pat_jal = Pattern.compile(_reg + "," + _imm);
    static Pattern label_jal = Pattern.compile(_reg + "," + __label);

    private static class JumpInstance extends Command {
        JumpInstance(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }

        String decode(int addr, int instr) {
            // return the decoding/i.e. the instruction itself
            return name + " " + rD(instr) + ", " + sImm(instr);
        }

        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            try {
                // If the argument matches this jal pattern then continue else throw an exception
                Matcher label = label_jal.matcher(args);
                Matcher m = pat_jal.matcher(args);

                if (!m.matches()) {
                    throw new ParseException(
                            "Line " + (lineno + 1) + ": '" + name + "' expects xD, Imm with args: " + args);
                }

                // resolve the matched immediate to a 21 bit immediate, we assume Imm[0] is 0
                int imm = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_ABSOLUTE, 21);

                // If Jal uses Label then subtract address offset
                if (label.matches()) {
                    imm = (imm - addr);
                }

                if ((imm & 1) != 0) {
                    throw new ParseException("line " + (lineno + 1) + ": invalid jal offset (" + imm + "): offset must be divisible by 2");

                }

                // Shift 21 bit immediate over by 1 to fit in encoding
                imm = (imm >>> 1) & 0xfffff;

                // get the destination register and check for validity
                int dest = reg(m.group(1));

                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line " + (lineno + 1) + ": invalid destination register: x" + dest);
                }

                // Adjust immediates to be in correct bit positions for RISC-V
                // well formed encoding: i[20] | i[10:1] | i[11] | i[19:12] | rd | opcode
                int i20 = ((imm >>> 19) & 1) << 31;
                int i10_1 = ((imm>>>0) & 0x3ff) << 21;
                int i11 = ((imm >>> 10) & 1) << 20;
                int i19_12 = ((imm >>> 11) & 0xff) << 12;
                // return well formed encoding
                return i20 | i10_1 | i11 | i19_12 | (dest << 7) | opcode;
            } catch (NumberFormatException e) {
                // If exception is caught then throw a logisim error message
                Matcher m = pat_jal.matcher(args);
                throw new ParseException(
                        "Line " + (lineno + 1) + ": invalid arguments to '" + name + "': " + e.getMessage()
                                + " registers: (" + m.group(2) + ") = (" + Integer.toString(reg(m.group(2))) + ")");
            }
        }

        String rD(int instr) {
            // return destination register as string
            return "x" + ((instr >>> 7) & 0x1f);
        }

        String sImm(int instr) {
            // determine immediate
            int i19_12 = (instr >>> 12) & 0xff;
            int i11 = (instr >>> 20) & 1;
            int i10_1 = (instr >>> 21) & 0x3ff;
            int i20 = (instr >>> 31) & 1;
            int imm = (i20 << 19) | (i19_12 << 11) | (i11 << 10) | (i10_1 << 0);

            imm <<= 1;
            // return 21 bit immediate as string in hex formatting
            return "" + toHex(imm & 0x1fffff, 6);
        }
    }

    static {
        // Last boolean is a debug boolean to enable Print Debugs for class testing
        new Word(".word", -1);
        new Nop("nop", 0x0);
        new Syscall("syscall", 0x10);
        new Eret("eret", 0x8);
        new Load("lb", 0x3, 0, false);
        new Load("lh", 0x3, 1, false);
        new Load("lw", 0x3, 2, false);
        new Load("lbu", 0x3, 4, false);
        new Load("lhu", 0x3, 5, false);
        // new Branch("b", 0x63, 0, true); // need to implement fully, movi,rd,imm
        new Branch("beq", 0x63, 0, true);
        new Branch("bne", 0x63, 1, false);
        new Branch("blt", 0x63, 4, false);
        new Branch("bge", 0x63, 5, false);
        new Branch("bltu", 0x63, 6, false);
        new Branch("bgeu", 0x63, 7, false);
        new Store("sb", 0x23, 0, false);
        new Store("sh", 0x23, 1, false);
        new Store("sw", 0x23, 2, true);
        // new ImmReg("movi", 0x13, 0, true, false) // need to implement fully,
        // movi,rd,imm
        new ImmReg("addi", 0x13, 0, true, true);
        new ImmReg("slti", 0x13, 2, true, false);
        new ImmReg("sltiu", 0x13, 3, false, false);
        new ImmReg("xori", 0x13, 4, true, false);
        new ImmReg("ori", 0x13, 6, true, false);
        new ImmReg("andi", 0x13, 7, true, false);
        new ImmReg("slli", 0x13, 1, true, false);
        new ImmReg("srli", 0x13, 5, true, false);
        new ImmReg("srai", 0x13, 5, true, false);

        // new ImmReg("mov", 0x33, 0, 0, false) // need to implement fully, mov,rd,rs
        new RegReg("add", 0x33, 0, 0, true);
        new RegReg("sub", 0x33, 0x20, 0, false);
        new RegReg("sll", 0x33, 0, 1, false);
        new RegReg("slt", 0x33, 0, 2, false);
        new RegReg("sltu", 0x33, 0, 3, false);
        new RegReg("xor", 0x33, 0, 4, false);
        new RegReg("or", 0x33, 0, 6, false);
        new RegReg("and", 0x33, 0, 7, false);
        new RegReg("srl", 0x33, 0, 5, false);
        new RegReg("sra", 0x33, 0x20, 5, false);
        new RegReg("mul", 0x33, 1, 0, false);
        new LuiAPCInst("lui", 0x37, true);
        new LuiAPCInst("auipc", 0x17, false);

        // Updated 1/27/19
        new ImmReg("jalr", 0x67, 0, true, false);
        new JumpInstance("jal", 0x6f);
    }

    private static Segment[] pass2(ArrayList<String> lines, int start_address, HashMap<String, Integer> sym)
            throws IOException {
        ParseException err = new ParseException();
        int addr = start_address;
        int cnt = 0;
        ArrayList<Segment> seglist = new ArrayList<Segment>();
        int pc = start_address >>> 2;
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null)
                continue;
            if (line.toLowerCase().startsWith(".text ")) {
                if (cnt > 0) {
                    seglist.add(new Segment(pc, new int[cnt]));
                }
                cnt = 0;
                pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ') + 1)) >>> 2;
            } else {
                cnt++;
            }
        }
        if (cnt > 0) {
            seglist.add(new Segment(pc, new int[cnt]));
        }
        Segment[] seg = new Segment[seglist.size()];
        if (seg.length == 0) {
            return seg;
        }
        for (int s = 0; s < seg.length; s++) {
            seg[s] = seglist.get(s);
            for (int s2 = 0; s2 < s; s2++) {
                if (seg[s].start_pc < seg[s2].start_pc + seg[s2].data.length
                        && seg[s2].start_pc < seg[s].start_pc + seg[s].data.length)
                    err.add("Assembly segment at " + toHex(seg[s].start_pc * 4, 8) + ".."
                            + toHex((seg[s].start_pc + seg[s].data.length) * 4, 8) + " overlaps with segment at "
                            + toHex(seg[s2].start_pc * 4, 8) + ".."
                            + toHex((seg[s2].start_pc + seg[s2].data.length) * 4, 8));
            }
        }

        int cs = 0;
        cnt = 0;
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null) {
                continue;
            }
            int i = line.indexOf(' ');
            String instr = i >= 0 ? line.substring(0, i) : line;
            String args = i >= 0 ? line.substring(i + 1) : "";
            if (instr.equalsIgnoreCase(".text")) {
                cs = -1;
                pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ') + 1)) >>> 2;
                addr = pc << 2;
                cnt = 0;
                for (int s = 0; s < seg.length; s++) {
                    if (seg[s].start_pc == pc) {
                        cs = s;
                        break;
                    }
                }
                if (cs < 0) {
                    err.add("Line " + (lineno + 1) + ": internal error: bad segment");
                }
            } else {
                Command cmd = cmds.get(instr.toLowerCase());
                if (cmd == null) {
                    err.add("Line " + (lineno + 1) + ": unrecognized instruction: '" + instr + "'");
                } else if (cs >= 0) {
                    try {
                        seg[cs].data[cnt++] = cmd.encode(lineno, addr, args, sym);
                    } catch (ParseException e) {
                        err.add(e);
                    }
                    addr += 4;
                }
            }
        }
        if (err.getCount() > 0) {
            throw err;
        }
        return seg;
    }

    private static Segment[] assemble(ArrayList<String> src_lines, int start_address, ArrayList<Integer> addr_map)
            throws IOException {
        ArrayList<String> lines = normalize(src_lines);
        HashMap<String, Integer> sym = pass1(lines, start_address, addr_map);
        return pass2(lines, start_address, sym);
    }

    static String disassemble(int instr, int addr) {
        int op = instr & 0x7f;
        int func3 = (instr >> 12) & 0x7;
        int func7 = (instr >> 25) & 0x7f;
        Command cmd = null;
        String name = "";
        boolean invalid;
        if (instr == 0x13) { //addi x0, x0, 0
            name = "nop";
        }
        else if (op == 0x6f) {
            name = "jal";
        } else if (op == 0x67) {
            name = "jalr";
        } else if (op == 0x17) {
            name = "auipc";
        } else if (op == 0x37) {
            name = "lui";
        } else if (op == 0x63) {
            switch (func3) {
            case 0:
                name = "beq";
                break;
            case 1:
                name = "bne";
                break;
            case 4:
                name = "blt";
                break;
            case 5:
                name = "bge";
                break;
            case 6:
                name = "bltu";
                break;
            case 7:
                name = "bgeu";
                break;
            default:
                invalid = true;
                break;
            }
        } else if (op == 0x3) {
            switch (func3) {
            case 0:
                name = "lb";
                break;
            case 1:
                name = "lh";
                break;
            case 2:
                name = "lw";
                break;
            case 4:
                name = "lbu";
                break;
            case 5:
                name = "lhu";
                break;
            default:
                invalid = true;
                break;
            }
        } else if (op == 0x23) {
            switch (func3) {
            case 0:
                name = "sb";
                break;
            case 1:
                name = "sh";
                break;
            case 2:
                name = "sw";
                break;
            default:
                invalid = true;
                break;
            }
        } else if (op == 0x13) {
            switch (func3) {
            case 0:
                name = "addi";
                break;
            case 2:
                name = "slti";
                break;
            case 3:
                name = "sltiu";
                break;
            case 4:
                name = "xori";
                break;
            case 6:
                name = "ori";
                break;
            case 7:
                name = "andi";
                break;
            case 1:
                name = "slli";
                break;
            case 5:
                name = (((func7 >> 5) & 0x3) == 1) ? "srai" : "srli";
                break;
            default:
                invalid = true;
                break;
            }
        } else if (op == 0x33) {
            switch (func3) {
            case 0:
                name = (func7 == 0x20) ? "sub" : (func7 == 0x1) ? "mul" : "add";
                break;
            case 1:
                name = "sll";
                break;
            case 2:
                name = "slt";
                break;
            case 3:
                name = "sltu";
                break;
            case 4:
                name = "xor";
                break;
            case 5:
                name = (func7 == 0x20) ? "sra" : "srl";
                break;
            case 6:
                name = "or";
                break;
            case 7:
                name = "and";
                break;
            default:
                invalid = true;
                break;
            }
        }

        cmd = cmds.get(name);
        if (cmd == null) {
            cmd = opcodes.get(new Integer(-1));
        }

        return cmd.decode(addr, instr);
    }

}
