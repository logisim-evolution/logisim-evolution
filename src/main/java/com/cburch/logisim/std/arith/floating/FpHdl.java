/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.util.LineBuffer;
import java.util.LinkedHashSet;

/**
 * Emits the VHDL/Verilog helper functions shared by all floating point HDL generators.
 *
 * <p>All results are IEEE-754 round-to-nearest-even with subnormal support. NaNs are produced as
 * the canonical quiet NaN. A format is described by its exponent/mantissa width, the generated
 * code contains these as literals, hence modules are named per format (see {@link #moduleName}).
 *
 * <p>Usage: create one instance per format, call the {@code add*} methods for the operations that
 * are needed (dependencies are pulled in automatically) and finish with {@link #wrap}.
 */
final class FpHdl {

  /** Number of exponent and mantissa bits of a supported floating point width. */
  record Format(int exponentBits, int mantissaBits) {
    static Format of(int width) {
      return switch (width) {
        case 8 -> new Format(4, 3);
        case 16 -> new Format(5, 10);
        case 32 -> new Format(8, 23);
        case 64 -> new Format(11, 52);
        default -> throw new IllegalArgumentException("Unsupported floating point width " + width);
      };
    }

    int width() {
      return 1 + exponentBits + mantissaBits;
    }

    int bias() {
      return (1 << (exponentBits - 1)) - 1;
    }

    int maxExponent() {
      return (1 << exponentBits) - 1;
    }
  }

  private final boolean isVhdl = Hdl.isVhdl();
  private final Format format;
  private final String prefix;
  private final LinkedHashSet<String> declarations = new LinkedHashSet<>();

  FpHdl(int width) {
    this(width, "");
  }

  FpHdl(int width, String prefix) {
    this.format = Format.of(width);
    this.prefix = prefix;
  }

  /** Name of the HDL entity for a component; the generated code depends on the format. */
  static String moduleName(String componentId, Object... parts) {
    final var name = new StringBuilder(CorrectLabel.getCorrectLabel(componentId).toUpperCase());
    for (final var part : parts) name.append('_').append(part.toString().toUpperCase());
    return name.toString();
  }

  // ------------------------------------------------------------------------------------------
  // Template helpers
  // ------------------------------------------------------------------------------------------

  String fill(String template, Object... keyValues) {
    var text = template;
    for (var i = 0; i < keyValues.length; i += 2)
      text = text.replace("@" + keyValues[i] + "@", keyValues[i + 1].toString());
    return text
        .replace("@P@", prefix)
        .replace("@EB@", Integer.toString(format.exponentBits()))
        .replace("@MB@", Integer.toString(format.mantissaBits()))
        .replace("@WB@", Integer.toString(format.width()))
        .replace("@BIAS@", Integer.toString(format.bias()))
        .replace("@EMAX@", Integer.toString(format.maxExponent()));
  }

  void declare(String vhdl, String verilog, Object... keyValues) {
    declarations.add(fill(isVhdl ? vhdl : verilog, keyValues));
  }

  int mb() {
    return format.mantissaBits();
  }

  Format format() {
    return format;
  }

  String packName(int sw) {
    return prefix + (isVhdl ? "fp_pack" : "fp_pack_" + sw);
  }

  private String addCoreName(int sw) {
    return prefix + (isVhdl ? "fp_addcore" : "fp_addcore_" + sw);
  }

  String msbName(int width) {
    return isVhdl ? "fp_msb" : "fp_msb_" + width;
  }

  /** Substitutes the format dependent tokens in code that is part of a module's statements. */
  String code(String template, Object... keyValues) {
    return fill(template, keyValues);
  }

  /**
   * Builds the module body from the collected declarations of all given parts and the statements.
   * For VHDL everything is put in a block, so the functions can be declared after the architecture
   * "begin".
   */
  static LineBuffer wrap(String statements, FpHdl... parts) {
    final var isVhdl = Hdl.isVhdl();
    final var all = new LinkedHashSet<String>();
    for (final var part : parts) all.addAll(part.declarations);
    final var contents = LineBuffer.getBuffer();
    if (isVhdl) {
      contents.add("fpcore : block is");
      for (final var declaration : all) addLines(contents, declaration, "   ");
      contents.add("begin");
      addLines(contents, statements, "   ");
      contents.add("end block fpcore;");
    } else {
      for (final var declaration : all) addLines(contents, declaration, "");
      addLines(contents, statements, "");
    }
    return contents;
  }

  private static void addLines(LineBuffer buffer, String text, String indent) {
    for (final var line : text.strip().split("\n")) {
      buffer.add(line.isBlank() ? "" : indent + line, false);
    }
    buffer.add("", false);
  }

  // ------------------------------------------------------------------------------------------
  // Basic building blocks
  // ------------------------------------------------------------------------------------------

  /** Constants, classification, unpacking (significand and unbiased exponent). */
  FpHdl addBase() {
    declare(
        """
        function @P@fp_nan return std_logic_vector is
           variable r : std_logic_vector(@WB@-1 downto 0) := (others => '0');
        begin
           r(@WB@-2 downto @MB@) := (others => '1');
           r(@MB@-1) := '1';
           return r;
        end function;

        function @P@fp_inf(s : std_logic) return std_logic_vector is
           variable r : std_logic_vector(@WB@-1 downto 0) := (others => '0');
        begin
           r(@WB@-1) := s;
           r(@WB@-2 downto @MB@) := (others => '1');
           return r;
        end function;

        function @P@fp_zero(s : std_logic) return std_logic_vector is
           variable r : std_logic_vector(@WB@-1 downto 0) := (others => '0');
        begin
           r(@WB@-1) := s;
           return r;
        end function;

        function @P@fp_isnan(x : std_logic_vector(@WB@-1 downto 0)) return boolean is
        begin
           return unsigned(x(@WB@-2 downto @MB@)) = @EMAX@ and unsigned(x(@MB@-1 downto 0)) /= 0;
        end function;

        function @P@fp_isinf(x : std_logic_vector(@WB@-1 downto 0)) return boolean is
        begin
           return unsigned(x(@WB@-2 downto @MB@)) = @EMAX@ and unsigned(x(@MB@-1 downto 0)) = 0;
        end function;

        function @P@fp_iszero(x : std_logic_vector(@WB@-1 downto 0)) return boolean is
        begin
           return unsigned(x(@WB@-2 downto 0)) = 0;
        end function;

        function @P@fp_sig(x : std_logic_vector(@WB@-1 downto 0)) return unsigned is
           variable f  : unsigned(@MB@-1 downto 0);
           variable lz : integer;
           variable r  : unsigned(@MB@ downto 0);
        begin
           f := unsigned(x(@MB@-1 downto 0));
           if unsigned(x(@WB@-2 downto @MB@)) /= 0 then
              r := '1' & f;
           else
              lz := @MB@;
              for i in 0 to @MB@-1 loop
                 if f(i) = '1' then
                    lz := @MB@-1-i;
                 end if;
              end loop;
              r := shift_left('0' & f, lz + 1);
           end if;
           return r;
        end function;

        function @P@fp_exp(x : std_logic_vector(@WB@-1 downto 0)) return integer is
           variable f  : unsigned(@MB@-1 downto 0);
           variable ex : integer;
           variable lz : integer;
        begin
           f  := unsigned(x(@MB@-1 downto 0));
           ex := to_integer(unsigned(x(@WB@-2 downto @MB@)));
           if ex /= 0 then
              return ex - @BIAS@;
           end if;
           lz := @MB@;
           for i in 0 to @MB@-1 loop
              if f(i) = '1' then
                 lz := @MB@-1-i;
              end if;
           end loop;
           return 1 - @BIAS@ - lz - 1;
        end function;
        """,
        """
        localparam [@WB@-1:0] @P@NAN_V = {1'b0, {@EB@{1'b1}}, 1'b1, {(@MB@-1){1'b0}}};

        function [@WB@-1:0] @P@fp_inf;
           input s;
           begin
              @P@fp_inf = {s, {@EB@{1'b1}}, {@MB@{1'b0}}};
           end
        endfunction

        function [@WB@-1:0] @P@fp_zero;
           input s;
           begin
              @P@fp_zero = {s, {(@WB@-1){1'b0}}};
           end
        endfunction

        function @P@fp_isnan;
           input [@WB@-1:0] x;
           begin
              @P@fp_isnan = (x[@WB@-2:@MB@] == @EMAX@) && (x[@MB@-1:0] != 0);
           end
        endfunction

        function @P@fp_isinf;
           input [@WB@-1:0] x;
           begin
              @P@fp_isinf = (x[@WB@-2:@MB@] == @EMAX@) && (x[@MB@-1:0] == 0);
           end
        endfunction

        function @P@fp_iszero;
           input [@WB@-1:0] x;
           begin
              @P@fp_iszero = (x[@WB@-2:0] == 0);
           end
        endfunction

        function [@MB@:0] @P@fp_sig;
           input [@WB@-1:0] x;
           reg [@MB@-1:0] f;
           reg [@MB@:0] r;
           integer lz, i;
           begin
              f = x[@MB@-1:0];
              if (x[@WB@-2:@MB@] != 0) r = {1'b1, f};
              else begin
                 lz = @MB@;
                 for (i = 0; i < @MB@; i = i + 1)
                    if (f[i]) lz = @MB@ - 1 - i;
                 r = {1'b0, f} << (lz + 1);
              end
              @P@fp_sig = r;
           end
        endfunction

        function integer @P@fp_exp;
           input [@WB@-1:0] x;
           integer ex, lz, i;
           begin
              ex = x[@WB@-2:@MB@];
              if (ex != 0) @P@fp_exp = ex - @BIAS@;
              else begin
                 lz = @MB@;
                 for (i = 0; i < @MB@; i = i + 1)
                    if (x[i]) lz = @MB@ - 1 - i;
                 @P@fp_exp = 1 - @BIAS@ - lz - 1;
              end
           end
        endfunction
        """);
    return this;
  }

  /** Index of the most significant set bit of a vector of the given width (-1 if none). */
  void addMsb(int width) {
    declare(
        """
        function fp_msb(v : unsigned) return integer is
           variable x : unsigned(v'length-1 downto 0);
           variable p : integer := -1;
        begin
           x := v;
           for i in 0 to v'length-1 loop
              if x(i) = '1' then
                 p := i;
              end if;
           end loop;
           return p;
        end function;
        """,
        """
        function integer fp_msb_@W@;
           input [@W@-1:0] v;
           integer i;
           begin
              fp_msb_@W@ = -1;
              for (i = 0; i < @W@; i = i + 1)
                 if (v[i]) fp_msb_@W@ = i;
           end
        endfunction
        """,
        "W", width);
  }

  /**
   * Rounds and packs sign, the exponent of the most significant bit and a normalized significand
   * (MSB set, or all zero) of {@code sw} bits (at least mantissa+3) with an extra sticky bit.
   */
  void addPack(int sw) {
    addBase();
    declare(
        """
        function @P@fp_pack(sign : std_logic; e : integer; sig : unsigned; sticky : std_logic)
                  return std_logic_vector is
           constant SW : integer := sig'length;
           variable s    : unsigned(SW-1 downto 0);
           variable be   : integer;
           variable sh   : integer;
           variable lost : std_logic;
           variable g    : std_logic;
           variable keep : unsigned(@MB@+1 downto 0);
           variable comp : unsigned(@EB@+@MB@+1 downto 0);
        begin
           s := sig;
           if s = 0 then
              return @P@fp_zero(sign);
           end if;
           be := e + @BIAS@;
           if be >= @EMAX@ then
              return @P@fp_inf(sign);
           end if;
           lost := sticky;
           sh   := 0;
           if be < 1 then
              sh := 1 - be;
              be := 1;
           end if;
           if sh > SW + 1 then
              sh := SW + 1;
           end if;
           if sh > 0 then
              for i in 0 to SW-1 loop
                 if i < sh and s(i) = '1' then
                    lost := '1';
                 end if;
              end loop;
              s := shift_right(s, sh);
           end if;
           keep := '0' & s(SW-1 downto SW-1-@MB@);
           g    := s(SW-2-@MB@);
           for i in 0 to SW-3-@MB@ loop
              if s(i) = '1' then
                 lost := '1';
              end if;
           end loop;
           if g = '1' and (lost = '1' or keep(0) = '1') then
              keep := keep + 1;
           end if;
           comp := (others => '0');
           comp(@EB@+@MB@ downto @MB@) := to_unsigned(be - 1, @EB@+1);
           comp := comp + keep;
           if comp(@EB@+@MB@+1 downto @MB@) >= @EMAX@ then
              return @P@fp_inf(sign);
           end if;
           return sign & std_logic_vector(comp(@EB@+@MB@-1 downto 0));
        end function;
        """,
        """
        function [@WB@-1:0] @PACK@;
           input sign;
           input integer e;
           input [@SW@-1:0] sig;
           input sticky;
           reg [@SW@-1:0] s;
           reg [@MB@+1:0] keep;
           reg [@EB@+@MB@+1:0] comp;
           reg lost, g;
           integer be, sh, i, ebf;
           begin
              if (sig == 0) @PACK@ = @P@fp_zero(sign);
              else begin
                 be = e + @BIAS@;
                 if (be >= @EMAX@) @PACK@ = @P@fp_inf(sign);
                 else begin
                    lost = sticky;
                    sh = 0;
                    s = sig;
                    if (be < 1) begin
                       sh = 1 - be;
                       be = 1;
                    end
                    if (sh > @SW@ + 1) sh = @SW@ + 1;
                    if (sh > 0) begin
                       for (i = 0; i < @SW@; i = i + 1)
                          if (i < sh && s[i]) lost = 1'b1;
                       s = s >> sh;
                    end
                    keep = {1'b0, s[@SW@-1 -: @MB@+1]};
                    g = s[@SW@-2-@MB@];
                    for (i = 0; i <= @SW@-3-@MB@; i = i + 1)
                       if (s[i]) lost = 1'b1;
                    if (g && (lost || keep[0])) keep = keep + 1'b1;
                    ebf = be - 1;
                    comp = {1'b0, ebf[@EB@-1:0], {@MB@{1'b0}}} + keep;
                    if (comp[@EB@+@MB@+1:@MB@] >= @EMAX@) @PACK@ = @P@fp_inf(sign);
                    else @PACK@ = {sign, comp[@EB@+@MB@-1:0]};
                 end
              end
           end
        endfunction
        """,
        "SW", sw,
        "PACK", packName(sw));
  }

  /**
   * Adds two values given as sign, exponent of the MSB and a normalized significand of {@code swi}
   * bits (zero allowed). The significands must be exact; the result is rounded once.
   */
  private void addAddCore(int swi) {
    final var sw = swi + 4;
    addPack(sw);
    addMsb(sw);
    declare(
        """
        function @P@fp_addcore(sa : std_logic; ea : integer; siga : unsigned;
                               sb : std_logic; eb : integer; sigb : unsigned)
                  return std_logic_vector is
           constant SWI : integer := siga'length;
           constant XW  : integer := SWI + 4;
           constant Z3  : unsigned(2 downto 0) := (others => '0');
           constant Z4  : unsigned(3 downto 0) := (others => '0');
           variable ma, mb, bm, lm : unsigned(SWI-1 downto 0);
           variable xa, xb, xr     : unsigned(XW-1 downto 0);
           variable bs, ls, lost   : std_logic;
           variable be, le, d, p   : integer;
        begin
           ma := siga;
           mb := sigb;
           if ma = 0 and mb = 0 then
              return @P@fp_zero(sa and sb);
           elsif mb = 0 then
              return @P@fp_pack(sa, ea, ma & Z4, '0');
           elsif ma = 0 then
              return @P@fp_pack(sb, eb, mb & Z4, '0');
           end if;
           if ea > eb or (ea = eb and ma >= mb) then
              bs := sa; be := ea; bm := ma;
              ls := sb; le := eb; lm := mb;
           else
              bs := sb; be := eb; bm := mb;
              ls := sa; le := ea; lm := ma;
           end if;
           d := be - le;
           if d > XW then
              d := XW;
           end if;
           xa := '0' & bm & Z3;
           xb := '0' & lm & Z3;
           lost := '0';
           for i in 0 to XW-1 loop
              if i < d and xb(i) = '1' then
                 lost := '1';
              end if;
           end loop;
           xb := shift_right(xb, d);
           xb(0) := xb(0) or lost;
           if bs = ls then
              xr := xa + xb;
           else
              xr := xa - xb;
           end if;
           if xr = 0 then
              return @P@fp_zero('0');
           end if;
           p := fp_msb(xr);
           return @P@fp_pack(bs, be + 1 - (XW - 1 - p), shift_left(xr, XW - 1 - p), '0');
        end function;
        """,
        """
        function [@WB@-1:0] @ACORE@;
           input sa;
           input integer ea;
           input [@SWI@-1:0] ma;
           input sb;
           input integer eb;
           input [@SWI@-1:0] mb;
           reg [@SWI@-1:0] bm, lm;
           reg [@XW@-1:0] xa, xb, xr;
           reg bs, ls, lost;
           integer be, le, d, p, i;
           begin
              if (ma == 0 && mb == 0) @ACORE@ = @P@fp_zero(sa & sb);
              else if (mb == 0) @ACORE@ = @PACK@(sa, ea, {ma, 4'b0000}, 1'b0);
              else if (ma == 0) @ACORE@ = @PACK@(sb, eb, {mb, 4'b0000}, 1'b0);
              else begin
                 if (ea > eb || (ea == eb && ma >= mb)) begin
                    bs = sa; be = ea; bm = ma;
                    ls = sb; le = eb; lm = mb;
                 end else begin
                    bs = sb; be = eb; bm = mb;
                    ls = sa; le = ea; lm = ma;
                 end
                 d = be - le;
                 if (d > @XW@) d = @XW@;
                 xa = {1'b0, bm, 3'b000};
                 xb = {1'b0, lm, 3'b000};
                 lost = 1'b0;
                 for (i = 0; i < @XW@; i = i + 1)
                    if (i < d && xb[i]) lost = 1'b1;
                 xb = xb >> d;
                 xb[0] = xb[0] | lost;
                 if (bs == ls) xr = xa + xb;
                 else xr = xa - xb;
                 if (xr == 0) @ACORE@ = @P@fp_zero(1'b0);
                 else begin
                    p = @MSB@(xr);
                    xr = xr << (@XW@ - 1 - p);
                    @ACORE@ = @PACK@(bs, be + 1 - (@XW@ - 1 - p), xr, 1'b0);
                 end
              end
           end
        endfunction
        """,
        "SWI", swi,
        "XW", sw,
        "ACORE", addCoreName(swi),
        "PACK", packName(sw),
        "MSB", msbName(sw));
  }

  // ------------------------------------------------------------------------------------------
  // Operations: after calling addXxx() the function is usable in the statements of the module
  // ------------------------------------------------------------------------------------------

  /** {@code fp_add(a, b, sub)}: a + b, or a - b if sub is '1'. */
  FpHdl addAdd() {
    addBase();
    addAddCore(mb() + 1);
    declare(
        """
        function @P@fp_add(a, b : std_logic_vector(@WB@-1 downto 0); sub : std_logic)
                  return std_logic_vector is
           variable sa, sb : std_logic;
        begin
           sa := a(@WB@-1);
           sb := b(@WB@-1) xor sub;
           if @P@fp_isnan(a) or @P@fp_isnan(b) then
              return @P@fp_nan;
           elsif @P@fp_isinf(a) then
              if @P@fp_isinf(b) and sa /= sb then
                 return @P@fp_nan;
              end if;
              return @P@fp_inf(sa);
           elsif @P@fp_isinf(b) then
              return @P@fp_inf(sb);
           end if;
           return @P@fp_addcore(sa, @P@fp_exp(a), @P@fp_sig(a), sb, @P@fp_exp(b), @P@fp_sig(b));
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_add;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           input sub;
           reg sa, sb;
           integer ea, eb;
           reg [@MB@:0] ma, mb;
           begin
              sa = a[@WB@-1];
              sb = b[@WB@-1] ^ sub;
              if (@P@fp_isnan(a) || @P@fp_isnan(b)) @P@fp_add = @P@NAN_V;
              else if (@P@fp_isinf(a)) begin
                 if (@P@fp_isinf(b) && sa != sb) @P@fp_add = @P@NAN_V;
                 else @P@fp_add = @P@fp_inf(sa);
              end
              else if (@P@fp_isinf(b)) @P@fp_add = @P@fp_inf(sb);
              else begin
                 ea = @P@fp_exp(a);
                 eb = @P@fp_exp(b);
                 ma = @P@fp_sig(a);
                 mb = @P@fp_sig(b);
                 @P@fp_add = @ACORE@(sa, ea, ma, sb, eb, mb);
              end
           end
        endfunction
        """,
        "ACORE", addCoreName(mb() + 1));
    return this;
  }

  /** {@code fp_mul(a, b)}. */
  FpHdl addMul() {
    addBase();
    final var sw = 2 * mb() + 2;
    addPack(sw);
    declare(
        """
        function @P@fp_mul(a, b : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable sg : std_logic;
           variable e  : integer;
           variable pr : unsigned(2*@MB@+1 downto 0);
        begin
           sg := a(@WB@-1) xor b(@WB@-1);
           if @P@fp_isnan(a) or @P@fp_isnan(b) then
              return @P@fp_nan;
           elsif @P@fp_isinf(a) or @P@fp_isinf(b) then
              if @P@fp_iszero(a) or @P@fp_iszero(b) then
                 return @P@fp_nan;
              end if;
              return @P@fp_inf(sg);
           elsif @P@fp_iszero(a) or @P@fp_iszero(b) then
              return @P@fp_zero(sg);
           end if;
           pr := @P@fp_sig(a) * @P@fp_sig(b);
           e  := @P@fp_exp(a) + @P@fp_exp(b);
           if pr(2*@MB@+1) = '1' then
              e := e + 1;
           else
              pr := shift_left(pr, 1);
           end if;
           return @P@fp_pack(sg, e, pr, '0');
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_mul;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           reg sg;
           integer e;
           reg [@MB@:0] ma, mb;
           reg [2*@MB@+1:0] pr;
           begin
              sg = a[@WB@-1] ^ b[@WB@-1];
              if (@P@fp_isnan(a) || @P@fp_isnan(b)) @P@fp_mul = @P@NAN_V;
              else if (@P@fp_isinf(a) || @P@fp_isinf(b)) begin
                 if (@P@fp_iszero(a) || @P@fp_iszero(b)) @P@fp_mul = @P@NAN_V;
                 else @P@fp_mul = @P@fp_inf(sg);
              end
              else if (@P@fp_iszero(a) || @P@fp_iszero(b)) @P@fp_mul = @P@fp_zero(sg);
              else begin
                 ma = @P@fp_sig(a);
                 mb = @P@fp_sig(b);
                 pr = ma * mb;
                 e = @P@fp_exp(a);
                 e = e + @P@fp_exp(b);
                 if (pr[2*@MB@+1]) e = e + 1;
                 else pr = pr << 1;
                 @P@fp_mul = @PACK@(sg, e, pr, 1'b0);
              end
           end
        endfunction
        """,
        "PACK", packName(sw));
    return this;
  }

  /** {@code fp_fma(a, b, c)}: a * b + c with a single rounding. */
  FpHdl addFma() {
    addBase();
    final var swi = 2 * mb() + 2;
    addAddCore(swi);
    declare(
        """
        function @P@fp_fma(a, b, c : std_logic_vector(@WB@-1 downto 0))
                  return std_logic_vector is
           variable sp, sc : std_logic;
           variable ep     : integer;
           variable pr, cs : unsigned(2*@MB@+1 downto 0);
           constant Z      : unsigned(@MB@ downto 0) := (others => '0');
        begin
           sp := a(@WB@-1) xor b(@WB@-1);
           sc := c(@WB@-1);
           if @P@fp_isnan(a) or @P@fp_isnan(b) or @P@fp_isnan(c) then
              return @P@fp_nan;
           elsif (@P@fp_isinf(a) and @P@fp_iszero(b)) or (@P@fp_iszero(a) and @P@fp_isinf(b)) then
              return @P@fp_nan;
           elsif @P@fp_isinf(a) or @P@fp_isinf(b) then
              if @P@fp_isinf(c) and sc /= sp then
                 return @P@fp_nan;
              end if;
              return @P@fp_inf(sp);
           elsif @P@fp_isinf(c) then
              return @P@fp_inf(sc);
           end if;
           pr := @P@fp_sig(a) * @P@fp_sig(b);
           ep := @P@fp_exp(a) + @P@fp_exp(b);
           if pr(2*@MB@+1) = '1' then
              ep := ep + 1;
           else
              pr := shift_left(pr, 1);
           end if;
           cs := @P@fp_sig(c) & Z;
           return @P@fp_addcore(sp, ep, pr, sc, @P@fp_exp(c), cs);
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_fma;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           input [@WB@-1:0] c;
           reg sp, sc;
           integer ep, ec;
           reg [@MB@:0] ma, mb, mc;
           reg [2*@MB@+1:0] pr, cs;
           begin
              sp = a[@WB@-1] ^ b[@WB@-1];
              sc = c[@WB@-1];
              if (@P@fp_isnan(a) || @P@fp_isnan(b) || @P@fp_isnan(c)) @P@fp_fma = @P@NAN_V;
              else if ((@P@fp_isinf(a) && @P@fp_iszero(b)) || (@P@fp_iszero(a) && @P@fp_isinf(b)))
                 @P@fp_fma = @P@NAN_V;
              else if (@P@fp_isinf(a) || @P@fp_isinf(b)) begin
                 if (@P@fp_isinf(c) && sc != sp) @P@fp_fma = @P@NAN_V;
                 else @P@fp_fma = @P@fp_inf(sp);
              end
              else if (@P@fp_isinf(c)) @P@fp_fma = @P@fp_inf(sc);
              else begin
                 ma = @P@fp_sig(a);
                 mb = @P@fp_sig(b);
                 mc = @P@fp_sig(c);
                 pr = ma * mb;
                 ep = @P@fp_exp(a);
                 ep = ep + @P@fp_exp(b);
                 ec = @P@fp_exp(c);
                 if (pr[2*@MB@+1]) ep = ep + 1;
                 else pr = pr << 1;
                 cs = {mc, {(@MB@+1){1'b0}}};
                 @P@fp_fma = @ACORE@(sp, ep, pr, sc, ec, cs);
              end
           end
        endfunction
        """,
        "ACORE", addCoreName(swi));
    return this;
  }

  /** {@code fp_div(a, b)}. */
  FpHdl addDiv() {
    addBase();
    final var sw = mb() + 4;
    addPack(sw);
    declare(
        """
        function @P@fp_div(a, b : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable sg         : std_logic;
           variable e          : integer;
           variable na, da, q, r : unsigned(2*@MB@+3 downto 0);
           variable qs         : unsigned(@MB@+3 downto 0);
           variable st         : std_logic;
        begin
           sg := a(@WB@-1) xor b(@WB@-1);
           if @P@fp_isnan(a) or @P@fp_isnan(b) then
              return @P@fp_nan;
           elsif @P@fp_isinf(a) then
              if @P@fp_isinf(b) then
                 return @P@fp_nan;
              end if;
              return @P@fp_inf(sg);
           elsif @P@fp_isinf(b) then
              return @P@fp_zero(sg);
           elsif @P@fp_iszero(b) then
              if @P@fp_iszero(a) then
                 return @P@fp_nan;
              end if;
              return @P@fp_inf(sg);
           elsif @P@fp_iszero(a) then
              return @P@fp_zero(sg);
           end if;
           na := shift_left(resize(@P@fp_sig(a), 2*@MB@+4), @MB@+3);
           da := resize(@P@fp_sig(b), 2*@MB@+4);
           q  := na / da;
           r  := na rem da;
           if q(@MB@+3) = '1' then
              e  := @P@fp_exp(a) - @P@fp_exp(b);
              qs := q(@MB@+3 downto 0);
           else
              e  := @P@fp_exp(a) - @P@fp_exp(b) - 1;
              qs := shift_left(q(@MB@+3 downto 0), 1);
           end if;
           if r /= 0 then
              st := '1';
           else
              st := '0';
           end if;
           return @P@fp_pack(sg, e, qs, st);
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_div;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           reg sg, st;
           integer e;
           reg [@MB@:0] ma, mb;
           reg [2*@MB@+3:0] na, da, q, r;
           reg [@MB@+3:0] qs;
           begin
              sg = a[@WB@-1] ^ b[@WB@-1];
              if (@P@fp_isnan(a) || @P@fp_isnan(b)) @P@fp_div = @P@NAN_V;
              else if (@P@fp_isinf(a)) begin
                 if (@P@fp_isinf(b)) @P@fp_div = @P@NAN_V;
                 else @P@fp_div = @P@fp_inf(sg);
              end
              else if (@P@fp_isinf(b)) @P@fp_div = @P@fp_zero(sg);
              else if (@P@fp_iszero(b)) begin
                 if (@P@fp_iszero(a)) @P@fp_div = @P@NAN_V;
                 else @P@fp_div = @P@fp_inf(sg);
              end
              else if (@P@fp_iszero(a)) @P@fp_div = @P@fp_zero(sg);
              else begin
                 ma = @P@fp_sig(a);
                 mb = @P@fp_sig(b);
                 na = {ma, {(@MB@+3){1'b0}}};
                 da = mb;
                 q = na / da;
                 r = na % da;
                 e = @P@fp_exp(a);
                 e = e - @P@fp_exp(b);
                 if (q[@MB@+3]) qs = q[@MB@+3:0];
                 else begin
                    e = e - 1;
                    qs = q[@MB@+3:0] << 1;
                 end
                 st = (r != 0);
                 @P@fp_div = @PACK@(sg, e, qs, st);
              end
           end
        endfunction
        """,
        "PACK", packName(sw));
    return this;
  }

  /** {@code fp_rem(a, b)}: IEEE-754 remainder (a - b * rint(a / b)), exact. */
  FpHdl addRemainder() {
    addBase();
    final var sw = mb() + 3;
    addPack(sw);
    addMsb(sw);
    // largest shift needed to align the dividend with the divisor
    final var maxShift = 2 * format.bias() + mb();
    final var width = mb() + 1 + maxShift;
    declare(
        """
        function @P@fp_rem(a, b : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           constant XWD : integer := @XWD@;
           variable ea, eb, d, p : integer;
           variable x, y, q, r, r2 : unsigned(XWD-1 downto 0);
           variable rv : unsigned(@MB@+2 downto 0);
           variable neg, sa : std_logic;
        begin
           sa := a(@WB@-1);
           if @P@fp_isnan(a) or @P@fp_isnan(b) or @P@fp_isinf(a) or @P@fp_iszero(b) then
              return @P@fp_nan;
           elsif @P@fp_isinf(b) or @P@fp_iszero(a) then
              return a;
           end if;
           ea := @P@fp_exp(a);
           eb := @P@fp_exp(b);
           d  := ea - eb;
           if d < -1 then
              return a;
           end if;
           x := shift_left(resize(@P@fp_sig(a), XWD), d + 1);
           y := shift_left(resize(@P@fp_sig(b), XWD), 1);
           q := x / y;
           r := x rem y;
           r2 := shift_left(r, 1);
           neg := '0';
           if r2 > y or (r2 = y and q(0) = '1') then
              r   := y - r;
              neg := '1';
           end if;
           if r = 0 then
              return @P@fp_zero(sa);
           end if;
           rv := resize(r, @MB@+3);
           p  := fp_msb(rv);
           return @P@fp_pack(sa xor neg, eb - @MB@ - 1 + p, shift_left(rv, @MB@+2-p), '0');
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_rem;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           reg sa, neg;
           integer ea, eb, d, p;
           reg [@MB@:0] ma, mb;
           reg [@XWD@-1:0] x, y, q, r, r2;
           reg [@MB@+2:0] rv;
           begin
              sa = a[@WB@-1];
              if (@P@fp_isnan(a) || @P@fp_isnan(b) || @P@fp_isinf(a) || @P@fp_iszero(b))
                 @P@fp_rem = @P@NAN_V;
              else if (@P@fp_isinf(b) || @P@fp_iszero(a)) @P@fp_rem = a;
              else begin
                 ea = @P@fp_exp(a);
                 eb = @P@fp_exp(b);
                 d = ea - eb;
                 if (d < -1) @P@fp_rem = a;
                 else begin
                    ma = @P@fp_sig(a);
                    mb = @P@fp_sig(b);
                    x = ma;
                    x = x << (d + 1);
                    y = mb;
                    y = y << 1;
                    q = x / y;
                    r = x % y;
                    r2 = r << 1;
                    neg = 1'b0;
                    if (r2 > y || (r2 == y && q[0])) begin
                       r = y - r;
                       neg = 1'b1;
                    end
                    if (r == 0) @P@fp_rem = @P@fp_zero(sa);
                    else begin
                       rv = r[@MB@+2:0];
                       p = @MSB@(rv);
                       rv = rv << (@MB@ + 2 - p);
                       @P@fp_rem = @PACK@(sa ^ neg, eb - @MB@ - 1 + p, rv, 1'b0);
                    end
                 end
              end
           end
        endfunction
        """,
        "XWD", width,
        "PACK", packName(sw),
        "MSB", msbName(sw));
    return this;
  }

  /** {@code fp_sqrt(a)}. */
  FpHdl addSqrt() {
    addBase();
    final var sw = mb() + 3;
    addPack(sw);
    declare(
        """
        function @P@fp_sqrt(a : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           constant K  : integer := @MB@+3;
           variable e  : integer;
           variable rd : unsigned(@MB@+1 downto 0);
           variable nn : unsigned(2*K-1 downto 0);
           variable rm, trial, root : unsigned(K+2 downto 0);
           variable st : std_logic;
        begin
           if @P@fp_isnan(a) then
              return @P@fp_nan;
           elsif @P@fp_iszero(a) then
              return a;
           elsif a(@WB@-1) = '1' then
              return @P@fp_nan;
           elsif @P@fp_isinf(a) then
              return a;
           end if;
           e  := @P@fp_exp(a);
           rd := resize(@P@fp_sig(a), @MB@+2);
           if (e mod 2) /= 0 then
              rd := shift_left(rd, 1);
              e  := e - 1;
           end if;
           e  := e / 2;
           nn := shift_left(resize(rd, 2*K), @MB@+4);
           rm := (others => '0');
           root := (others => '0');
           for i in K-1 downto 0 loop
              rm    := rm(K downto 0) & nn(2*i+1 downto 2*i);
              trial := root(K downto 0) & "01";
              if rm >= trial then
                 rm   := rm - trial;
                 root := root(K+1 downto 0) & '1';
              else
                 root := root(K+1 downto 0) & '0';
              end if;
           end loop;
           if rm /= 0 then
              st := '1';
           else
              st := '0';
           end if;
           return @P@fp_pack('0', e, root(K-1 downto 0), st);
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_sqrt;
           input [@WB@-1:0] a;
           integer e, i;
           reg [@MB@:0] ma;
           reg [@MB@+1:0] rd;
           reg [2*@MB@+5:0] nn;
           reg [@MB@+5:0] rm, trial, root;
           reg st;
           begin
              if (@P@fp_isnan(a)) @P@fp_sqrt = @P@NAN_V;
              else if (@P@fp_iszero(a)) @P@fp_sqrt = a;
              else if (a[@WB@-1]) @P@fp_sqrt = @P@NAN_V;
              else if (@P@fp_isinf(a)) @P@fp_sqrt = a;
              else begin
                 e = @P@fp_exp(a);
                 ma = @P@fp_sig(a);
                 rd = ma;
                 if (e % 2 != 0) begin
                    rd = rd << 1;
                    e = e - 1;
                 end
                 e = e / 2;
                 nn = rd;
                 nn = nn << (@MB@ + 4);
                 rm = 0;
                 root = 0;
                 for (i = @MB@ + 2; i >= 0; i = i - 1) begin
                    rm = {rm[@MB@+3:0], nn[2*i+1 -: 2]};
                    trial = {root[@MB@+3:0], 2'b01};
                    if (rm >= trial) begin
                       rm = rm - trial;
                       root = {root[@MB@+4:0], 1'b1};
                    end else root = {root[@MB@+4:0], 1'b0};
                 end
                 st = (rm != 0);
                 @P@fp_sqrt = @PACK@(1'b0, e, root[@MB@+2:0], st);
              end
           end
        endfunction
        """,
        "PACK", packName(sw));
    return this;
  }

  /** {@code fp_lt(a, b)}: ordered a &lt; b (false if either is NaN). */
  FpHdl addLessThan() {
    addBase();
    declare(
        """
        function @P@fp_lt(a, b : std_logic_vector(@WB@-1 downto 0)) return boolean is
        begin
           if @P@fp_isnan(a) or @P@fp_isnan(b) then
              return false;
           elsif @P@fp_iszero(a) and @P@fp_iszero(b) then
              return false;
           elsif a(@WB@-1) /= b(@WB@-1) then
              return a(@WB@-1) = '1';
           elsif a(@WB@-1) = '0' then
              return unsigned(a(@WB@-2 downto 0)) < unsigned(b(@WB@-2 downto 0));
           end if;
           return unsigned(a(@WB@-2 downto 0)) > unsigned(b(@WB@-2 downto 0));
        end function;

        function @P@fp_eq(a, b : std_logic_vector(@WB@-1 downto 0)) return boolean is
        begin
           if @P@fp_isnan(a) or @P@fp_isnan(b) then
              return false;
           end if;
           return a = b or (@P@fp_iszero(a) and @P@fp_iszero(b));
        end function;
        """,
        """
        function @P@fp_lt;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           begin
              if (@P@fp_isnan(a) || @P@fp_isnan(b)) @P@fp_lt = 1'b0;
              else if (@P@fp_iszero(a) && @P@fp_iszero(b)) @P@fp_lt = 1'b0;
              else if (a[@WB@-1] != b[@WB@-1]) @P@fp_lt = a[@WB@-1];
              else if (!a[@WB@-1]) @P@fp_lt = (a[@WB@-2:0] < b[@WB@-2:0]);
              else @P@fp_lt = (a[@WB@-2:0] > b[@WB@-2:0]);
           end
        endfunction

        function @P@fp_eq;
           input [@WB@-1:0] a;
           input [@WB@-1:0] b;
           begin
              if (@P@fp_isnan(a) || @P@fp_isnan(b)) @P@fp_eq = 1'b0;
              else @P@fp_eq = (a == b) || (@P@fp_iszero(a) && @P@fp_iszero(b));
           end
        endfunction
        """);
    return this;
  }

  /** {@code fp_key(x)}: unsigned key whose order is the total order of the non-NaN values. */
  FpHdl addOrderKey() {
    addBase();
    declare(
        """
        function @P@fp_key(x : std_logic_vector(@WB@-1 downto 0)) return unsigned is
        begin
           if x(@WB@-1) = '1' then
              return not unsigned(x);
           end if;
           return '1' & unsigned(x(@WB@-2 downto 0));
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_key;
           input [@WB@-1:0] x;
           begin
              @P@fp_key = x[@WB@-1] ? ~x : {1'b1, x[@WB@-2:0]};
           end
        endfunction
        """);
    return this;
  }

  /**
   * {@code fp_round(x)}: rounds to an integral value. The rounding mode is one of the {@code
   * FpToInt} mode option names (ceil, floor, round, rint, truncate).
   */
  FpHdl addRound(String mode) {
    addBase();
    final var sw = mb() + 3;
    addPack(sw);
    addMsb(sw);
    final var keepsZeroSign = mode.equals("ceil") || mode.equals("floor") || mode.equals("rint");
    final var incVhdl =
        switch (mode) {
          case "ceil" -> "if lost /= 0 and sg = '0' then inc := '1'; end if;";
          case "floor" -> "if lost /= 0 and sg = '1' then inc := '1'; end if;";
          case "round" ->
              "if (sg = '0' and lost >= half) or (sg = '1' and lost > half) then inc := '1'; end if;";
          case "rint" ->
              "if lost > half or (lost = half and ip(0) = '1') then inc := '1'; end if;";
          default -> "inc := '0';";
        };
    final var incVerilog =
        switch (mode) {
          case "ceil" -> "inc = (lost != 0) && !sg;";
          case "floor" -> "inc = (lost != 0) && sg;";
          case "round" -> "inc = sg ? (lost > half) : (lost >= half);";
          case "rint" -> "inc = (lost > half) || (lost == half && ip[0]);";
          default -> "inc = 1'b0;";
        };
    declare(
        """
        function @P@fp_round(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           constant XWD : integer := @MB@+3;
           variable sg, inc : std_logic;
           variable e, f, p : integer;
           variable xv, ip, lost, half, n : unsigned(XWD-1 downto 0);
        begin
           sg := x(@WB@-1);
           if @P@fp_isnan(x) then
              return @P@fp_nan;
           elsif @P@fp_iszero(x) then
              return @P@fp_zero(@ZEROSIGN@);
           elsif @P@fp_isinf(x) then
              return x;
           end if;
           e := @P@fp_exp(x);
           if e >= @MB@ then
              return x;
           end if;
           xv := resize(@P@fp_sig(x), XWD);
           f := @MB@ - e;
           if f > @MB@+2 then
              f := @MB@+2;
           end if;
           ip   := shift_right(xv, f);
           lost := xv - shift_left(ip, f);
           half := shift_left(to_unsigned(1, XWD), f - 1);
           inc  := '0';
           @INC@
           n := ip;
           if inc = '1' then
              n := ip + 1;
           end if;
           if n = 0 then
              return @P@fp_zero(@ZEROSIGN@);
           end if;
           p := fp_msb(n);
           return @P@fp_pack(sg, p, shift_left(n, XWD-1-p), '0');
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_round;
           input [@WB@-1:0] x;
           reg sg, inc;
           integer e, f, p;
           reg [@MB@+2:0] xv, ip, lost, half, n;
           begin
              sg = x[@WB@-1];
              if (@P@fp_isnan(x)) @P@fp_round = @P@NAN_V;
              else if (@P@fp_iszero(x)) @P@fp_round = @P@fp_zero(@ZEROSIGN@);
              else if (@P@fp_isinf(x)) @P@fp_round = x;
              else begin
                 e = @P@fp_exp(x);
                 if (e >= @MB@) @P@fp_round = x;
                 else begin
                    xv = @P@fp_sig(x);
                    f = @MB@ - e;
                    if (f > @MB@ + 2) f = @MB@ + 2;
                    ip = xv >> f;
                    lost = xv - (ip << f);
                    half = 1;
                    half = half << (f - 1);
                    @INC@
                    n = ip + inc;
                    if (n == 0) @P@fp_round = @P@fp_zero(@ZEROSIGN@);
                    else begin
                       p = @MSB@(n);
                       n = n << (@MB@ + 2 - p);
                       @P@fp_round = @PACK@(sg, p, n, 1'b0);
                    end
                 end
              end
           end
        endfunction
        """,
        "ZEROSIGN", keepsZeroSign ? (isVhdl ? "x(@WB@-1)" : "x[@WB@-1]") : (isVhdl ? "'0'" : "1'b0"),
        "INC", isVhdl ? incVhdl : incVerilog,
        "PACK", packName(sw),
        "MSB", msbName(sw));
    return this;
  }

  /**
   * {@code fp_toint(x)}: 64 bit two's complement result of a conversion that saturates like a Java
   * (long) cast does, NaN gives zero.
   */
  FpHdl addToInt(String mode) {
    addBase();
    final var incVhdl =
        switch (mode) {
          case "ceil" -> "if lost /= 0 and sg = '0' then inc := '1'; end if;";
          case "floor" -> "if lost /= 0 and sg = '1' then inc := '1'; end if;";
          case "round" ->
              "if (sg = '0' and lost >= half) or (sg = '1' and lost > half) then inc := '1'; end if;";
          case "rint" ->
              "if lost > half or (lost = half and ip(0) = '1') then inc := '1'; end if;";
          default -> "inc := '0';";
        };
    final var incVerilog =
        switch (mode) {
          case "ceil" -> "inc = (lost != 0) && !sg;";
          case "floor" -> "inc = (lost != 0) && sg;";
          case "round" -> "inc = sg ? (lost > half) : (lost >= half);";
          case "rint" -> "inc = (lost > half) || (lost == half && ip[0]);";
          default -> "inc = 1'b0;";
        };
    declare(
        """
        function @P@fp_toint(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           constant XWD : integer := @MB@+3;
           variable sg, inc : std_logic;
           variable e, f : integer;
           variable xv, ip, lost, half : unsigned(XWD-1 downto 0);
           variable n : unsigned(63 downto 0);
           variable maxv, minv : std_logic_vector(63 downto 0);
        begin
           sg := x(@WB@-1);
           maxv := (others => '1');
           maxv(63) := '0';
           minv := (others => '0');
           minv(63) := '1';
           if @P@fp_isnan(x) or @P@fp_iszero(x) then
              return std_logic_vector(to_unsigned(0, 64));
           elsif @P@fp_isinf(x) then
              if sg = '1' then
                 return minv;
              end if;
              return maxv;
           end if;
           e := @P@fp_exp(x);
           if e >= 63 then
              if sg = '1' then
                 return minv;
              end if;
              return maxv;
           end if;
           xv := resize(@P@fp_sig(x), XWD);
           if e >= @MB@ then
              n := shift_left(resize(xv, 64), e - @MB@);
           else
              f := @MB@ - e;
              if f > @MB@+2 then
                 f := @MB@+2;
              end if;
              ip   := shift_right(xv, f);
              lost := xv - shift_left(ip, f);
              half := shift_left(to_unsigned(1, XWD), f - 1);
              inc  := '0';
              @INC@
              n := resize(ip, 64);
              if inc = '1' then
                 n := n + 1;
              end if;
           end if;
           if sg = '1' then
              n := (not n) + 1;
           end if;
           return std_logic_vector(n);
        end function;
        """,
        """
        function [63:0] @P@fp_toint;
           input [@WB@-1:0] x;
           reg sg, inc;
           integer e, f;
           reg [@MB@+2:0] xv, ip, lost, half;
           reg [63:0] n;
           begin
              sg = x[@WB@-1];
              if (@P@fp_isnan(x) || @P@fp_iszero(x)) @P@fp_toint = 64'd0;
              else if (@P@fp_isinf(x)) @P@fp_toint = sg ? 64'h8000000000000000 : 64'h7FFFFFFFFFFFFFFF;
              else begin
                 e = @P@fp_exp(x);
                 if (e >= 63) @P@fp_toint = sg ? 64'h8000000000000000 : 64'h7FFFFFFFFFFFFFFF;
                 else begin
                    xv = @P@fp_sig(x);
                    if (e >= @MB@) begin
                       n = xv;
                       n = n << (e - @MB@);
                    end else begin
                       f = @MB@ - e;
                       if (f > @MB@ + 2) f = @MB@ + 2;
                       ip = xv >> f;
                       lost = xv - (ip << f);
                       half = 1;
                       half = half << (f - 1);
                       @INC@
                       n = ip + inc;
                    end
                    @P@fp_toint = sg ? (~n + 64'd1) : n;
                 end
              end
           end
        endfunction
        """,
        "INC", isVhdl ? incVhdl : incVerilog);
    return this;
  }

  /** {@code fp_fromint(x)}: converts an {@code intWidth} bit integer. */
  FpHdl addFromInt(int intWidth, boolean signed) {
    addBase();
    final var sw = Math.max(intWidth, mb() + 3);
    addPack(sw);
    addMsb(intWidth);
    declare(
        """
        function @P@fp_fromint(x : std_logic_vector(@IW@-1 downto 0)) return std_logic_vector is
           variable n  : unsigned(@IW@-1 downto 0);
           variable s  : unsigned(@SW@-1 downto 0);
           variable sg : std_logic;
           variable p  : integer;
        begin
           sg := '0';
           n  := unsigned(x);
           if @SIGNED@ and x(@IW@-1) = '1' then
              sg := '1';
              n  := (not n) + 1;
           end if;
           if n = 0 then
              return @P@fp_zero('0');
           end if;
           p := fp_msb(n);
           s := (others => '0');
           s(@SW@-1 downto @SW@-@IW@) := n;
           return @P@fp_pack(sg, p, shift_left(s, @IW@-1-p), '0');
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_fromint;
           input [@IW@-1:0] x;
           reg [@IW@-1:0] n;
           reg [@SW@-1:0] s;
           reg sg;
           integer p;
           begin
              sg = 1'b0;
              n = x;
              if (@SIGNED@ && x[@IW@-1]) begin
                 sg = 1'b1;
                 n = ~x + 1'b1;
              end
              if (n == 0) @P@fp_fromint = @P@fp_zero(1'b0);
              else begin
                 p = @MSB@(n);
                 s = @ALIGNED@;
                 s = s << (@IW@ - 1 - p);
                 @P@fp_fromint = @PACK@(sg, p, s, 1'b0);
              end
           end
        endfunction
        """,
        "IW", intWidth,
        "SW", sw,
        "SIGNED", isVhdl ? (signed ? "true" : "false") : (signed ? "1'b1" : "1'b0"),
        "ALIGNED", sw == intWidth ? "n" : "{n, {" + (sw - intWidth) + "{1'b0}}}",
        "PACK", packName(sw),
        "MSB", msbName(intWidth));
    return this;
  }

  /**
   * {@code fp_from_<source prefix>(x)}: converts a value of the source format, which must have been
   * added with {@link #addBase()} on {@code source}, into this format (round to nearest even).
   */
  FpHdl addConvertFrom(FpHdl source) {
    addBase();
    final var sourceMantissa = source.format.mantissaBits();
    final var sw = Math.max(sourceMantissa + 1, mb() + 3);
    addPack(sw);
    source.addBase();
    declarations.addAll(source.declarations);
    final var aligned =
        sw == sourceMantissa + 1
            ? "m"
            : "{m, {" + (sw - sourceMantissa - 1) + "{1'b0}}}";
    declare(
        """
        function @P@fp_convert(x : std_logic_vector(@SWB@-1 downto 0)) return std_logic_vector is
           variable m : unsigned(@SMB@ downto 0);
           variable s : unsigned(@SW@-1 downto 0);
           variable sg : std_logic;
        begin
           sg := x(@SWB@-1);
           if @S@fp_isnan(x) then
              return @P@fp_nan;
           elsif @S@fp_isinf(x) then
              return @P@fp_inf(sg);
           elsif @S@fp_iszero(x) then
              return @P@fp_zero(sg);
           end if;
           m := @S@fp_sig(x);
           s := (others => '0');
           s(@SW@-1 downto @SW@-@SMB@-1) := m;
           return @P@fp_pack(sg, @S@fp_exp(x), s, '0');
        end function;
        """,
        """
        function [@WB@-1:0] @P@fp_convert;
           input [@SWB@-1:0] x;
           reg [@SMB@:0] m;
           reg [@SW@-1:0] s;
           reg sg;
           integer e;
           begin
              sg = x[@SWB@-1];
              if (@S@fp_isnan(x)) @P@fp_convert = @P@NAN_V;
              else if (@S@fp_isinf(x)) @P@fp_convert = @P@fp_inf(sg);
              else if (@S@fp_iszero(x)) @P@fp_convert = @P@fp_zero(sg);
              else begin
                 m = @S@fp_sig(x);
                 e = @S@fp_exp(x);
                 s = @ALIGNED@;
                 @P@fp_convert = @PACK@(sg, e, s, 1'b0);
              end
           end
        endfunction
        """,
        "S", source.prefix,
        "SWB", source.format.width(),
        "SMB", sourceMantissa,
        "SW", sw,
        "ALIGNED", aligned,
        "PACK", packName(sw));
    return this;
  }
}
