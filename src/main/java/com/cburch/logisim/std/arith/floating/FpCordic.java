/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcendental floating point functions (exp, log, trigonometry) for the HDL generators, based on
 * CORDIC iterations in fixed point. The results approximate the (double precision) simulation to
 * a few ulp, they are not bit exact.
 *
 * <p>Fixed point numbers are signed with F = 2 * mantissa + 12 fractional bits, which keeps the
 * relative error small even for results near zero (e.g. ln(1 + x) for small x). Circular and
 * hyperbolic CORDIC (F iterations each) provide sin/cos, atan, sinh/cosh and atanh, the latter is
 * used for the logarithm. Arguments of the trigonometric functions are reduced by a multiple of
 * pi/2 with a wide constant 2/pi (Payne-Hanek), so the whole range of the format is supported.
 * Exponentials are reduced by a multiple of ln(2).
 *
 * <p>Functions (names are prefixed with fm_): exp, expm1, pow, ln, log1p, sincos (result is
 * {cos, sin}), atan, asin, acos, sinhcosh (result is {cosh, sinh}), tanh.
 */
final class FpCordic {

  private static final int[] HYPERBOLIC_REPEATS = {4, 13, 40, 121};

  private final FpHdl hdl;
  private final int eb;
  private final int mb;
  private final int f;
  private final int fw;
  private final int n;
  private final int cb;
  private final int sMax;
  private final int wideWidth;
  private final int tanhClampExponent;
  private final Object[] keys;
  private boolean baseDone = false;
  private final List<String> done = new ArrayList<>();

  FpCordic(FpHdl hdl) {
    this.hdl = hdl;
    final var format = hdl.format();
    eb = format.exponentBits();
    mb = format.mantissaBits();
    f = 2 * mb + 12;
    fw = f + eb + 4;
    n = f;
    cb = format.bias() + f + 4;
    sMax = f + eb + 1 - mb;
    wideWidth = mb + 1 + fw + sMax;
    // smallest exponent from which tanh is 1 within rounding: |x| >= (mb + 3) * ln(2) / 2
    var exponent = 0;
    while (Math.pow(2, exponent) < (mb + 3) * Math.log(2) / 2) exponent++;
    tanhClampExponent = exponent;
    keys =
        new Object[] {
          "F", f, "FW", fw, "N", n, "CB", cb, "SMAX", sMax, "WW", wideWidth, "CLAMP",
          tanhClampExponent
        };
  }

  // ------------------------------------------------------------------------------------------
  // Constants
  // ------------------------------------------------------------------------------------------

  private static BigDecimal series(BigDecimal x, boolean alternating, MathContext mc) {
    final var epsilon = BigDecimal.ONE.movePointLeft(mc.getPrecision() + 5);
    final var x2 = x.multiply(x, mc);
    var power = x;
    var sum = BigDecimal.ZERO;
    for (var k = 0; power.abs().compareTo(epsilon) > 0; k++) {
      final var term = power.divide(BigDecimal.valueOf(2L * k + 1), mc);
      sum = (alternating && (k % 2 == 1)) ? sum.subtract(term, mc) : sum.add(term, mc);
      power = power.multiply(x2, mc);
    }
    return sum;
  }

  private static BigDecimal atan(BigDecimal x, MathContext mc) {
    return series(x, true, mc);
  }

  private static BigDecimal atanh(BigDecimal x, MathContext mc) {
    return series(x, false, mc);
  }

  private static BigDecimal pi(MathContext mc) {
    final var fifth = BigDecimal.ONE.divide(BigDecimal.valueOf(5), mc);
    final var inv239 = BigDecimal.ONE.divide(BigDecimal.valueOf(239), mc);
    return atan(fifth, mc)
        .multiply(BigDecimal.valueOf(16))
        .subtract(atan(inv239, mc).multiply(BigDecimal.valueOf(4)), mc);
  }

  private BigInteger scaled(BigDecimal value) {
    return value
        .multiply(new BigDecimal(BigInteger.ONE.shiftLeft(f)))
        .setScale(0, RoundingMode.HALF_EVEN)
        .toBigInteger();
  }

  private String binary(BigInteger value, int width) {
    return String.format("%" + width + "s", value.toString(2)).replace(' ', '0');
  }

  private String vhdlConstant(String name, BigInteger value) {
    return "constant " + name + " : signed(" + (fw - 1) + " downto 0) := \""
        + binary(value, fw) + "\";";
  }

  private String verilogConstant(String name, BigInteger value) {
    return "localparam signed [" + (fw - 1) + ":0] " + name + " = " + fw + "'sh"
        + value.toString(16) + ";";
  }

  /** Declares all constants and the CORDIC angle tables. */
  private void declareConstants() {
    final var mc = new MathContext((int) ((cb + 2 * f + 64) * 0.30103) + 30);
    final var two = BigDecimal.valueOf(2);
    final var pi = pi(mc);
    final var ln2 = atanh(BigDecimal.ONE.divide(BigDecimal.valueOf(3), mc), mc).multiply(two);
    final var ln125 = atanh(BigDecimal.ONE.divide(BigDecimal.valueOf(9), mc), mc).multiply(two);
    final var ln10 = ln2.multiply(BigDecimal.valueOf(3)).add(ln125, mc);

    final var atanTable = new BigInteger[n + 1];
    final var atanhTable = new BigInteger[n + 1];
    var circularGain = BigDecimal.ONE;
    var hyperbolicGain = BigDecimal.ONE;
    atanTable[n] = BigInteger.ZERO;
    atanhTable[0] = BigInteger.ZERO;
    for (var i = 0; i <= n; i++) {
      final var step = BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(i)), mc);
      if (i < n) {
        atanTable[i] = scaled(i == 0 ? pi.divide(BigDecimal.valueOf(4), mc) : atan(step, mc));
        circularGain =
            circularGain.multiply(
                BigDecimal.ONE.add(step.multiply(step, mc), mc).sqrt(mc), mc);
      }
      if (i >= 1) {
        atanhTable[i] = scaled(atanh(step, mc));
        var repeats = 1;
        for (final var repeat : HYPERBOLIC_REPEATS) if (repeat == i) repeats++;
        for (var r = 0; r < repeats; r++) {
          hyperbolicGain =
              hyperbolicGain.multiply(
                  BigDecimal.ONE.subtract(step.multiply(step, mc), mc).sqrt(mc), mc);
        }
      }
    }
    final var one = BigInteger.ONE.shiftLeft(f);
    final var values = new java.util.LinkedHashMap<String, BigInteger>();
    values.put("ONE", one);
    values.put("HALF", BigInteger.ONE.shiftLeft(f - 1));
    values.put("PI", scaled(pi));
    values.put("PIH", scaled(pi.divide(two, mc)));
    values.put("LN2", scaled(ln2));
    values.put("INVLN2", scaled(BigDecimal.ONE.divide(ln2, mc)));
    values.put("INVLN10", scaled(BigDecimal.ONE.divide(ln10, mc)));
    values.put("SQRT2", scaled(two.sqrt(mc)));
    values.put("INVK", scaled(BigDecimal.ONE.divide(circularGain, mc)));
    values.put("INVKH", scaled(BigDecimal.ONE.divide(hyperbolicGain, mc)));
    final var twoOverPi =
        two.divide(pi, mc)
            .multiply(new BigDecimal(BigInteger.ONE.shiftLeft(cb)))
            .setScale(0, RoundingMode.FLOOR)
            .toBigInteger();

    final var vhdl = new StringBuilder();
    final var verilog = new StringBuilder();
    for (final var entry : values.entrySet()) {
      vhdl.append(vhdlConstant(entry.getKey(), entry.getValue())).append('\n');
      verilog.append(verilogConstant(entry.getKey(), entry.getValue())).append('\n');
    }
    vhdl.append("constant TWOPI : unsigned(").append(cb - 1).append(" downto 0) := \"")
        .append(binary(twoOverPi, cb)).append("\";\n");
    verilog.append("localparam [").append(cb - 1).append(":0] TWOPI = ").append(cb).append("'h")
        .append(twoOverPi.toString(16)).append(";\n");

    vhdl.append("type fx_table is array (0 to ").append(n).append(") of signed(")
        .append(fw - 1).append(" downto 0);\n");
    appendVhdlTable(vhdl, "ATAN_T", atanTable);
    appendVhdlTable(vhdl, "ATANH_T", atanhTable);
    appendVerilogTable(verilog, "atan_tab", atanTable);
    appendVerilogTable(verilog, "atanh_tab", atanhTable);
    hdl.declare(vhdl.toString(), verilog.toString());
  }

  private void appendVhdlTable(StringBuilder out, String name, BigInteger[] table) {
    out.append("constant ").append(name).append(" : fx_table := (\n");
    for (var i = 0; i < table.length; i++) {
      out.append("   \"").append(binary(table[i], fw)).append('"')
          .append(i == table.length - 1 ? "\n" : ",\n");
    }
    out.append(");\n");
  }

  private void appendVerilogTable(StringBuilder out, String name, BigInteger[] table) {
    out.append("function signed [").append(fw - 1).append(":0] ").append(name).append(";\n");
    out.append("   input integer i;\n   begin\n      case (i)\n");
    for (var i = 0; i < table.length; i++) {
      out.append("         ").append(i).append(": ").append(name).append(" = ").append(fw)
          .append("'sh").append(table[i].toString(16)).append(";\n");
    }
    out.append("         default: ").append(name).append(" = 0;\n");
    out.append("      endcase\n   end\nendfunction\n");
  }

  private void declare(String vhdl, String verilog, Object... extraKeys) {
    final var all = new Object[keys.length + extraKeys.length];
    System.arraycopy(keys, 0, all, 0, keys.length);
    System.arraycopy(extraKeys, 0, all, keys.length, extraKeys.length);
    hdl.declare(vhdl, verilog, all);
  }

  private boolean once(String name) {
    if (done.contains(name)) return false;
    done.add(name);
    return true;
  }

  // ------------------------------------------------------------------------------------------
  // Fixed point primitives
  // ------------------------------------------------------------------------------------------

  private void base() {
    if (baseDone) return;
    baseDone = true;
    hdl.addBase();
    hdl.addPack(fw);
    hdl.addMsb(fw);
    hdl.addAdd();
    declareConstants();
    declare(
        """
        function fx_mul(a, b : signed(@FW@-1 downto 0)) return signed is
           variable p : signed(2*@FW@-1 downto 0);
        begin
           p := a * b;
           return p(@F@+@FW@-1 downto @F@);
        end function;

        function fx_cossin(z0 : signed(@FW@-1 downto 0)) return std_logic_vector is
           variable x, y, z, xn, yn : signed(@FW@-1 downto 0);
        begin
           x := INVK;
           y := (others => '0');
           z := z0;
           for i in 0 to @N@-1 loop
              if z >= 0 then
                 xn := x - shift_right(y, i);
                 yn := y + shift_right(x, i);
                 z  := z - ATAN_T(i);
              else
                 xn := x + shift_right(y, i);
                 yn := y - shift_right(x, i);
                 z  := z + ATAN_T(i);
              end if;
              x := xn;
              y := yn;
           end loop;
           return std_logic_vector(y) & std_logic_vector(x);
        end function;

        function fx_hyprot(z0 : signed(@FW@-1 downto 0)) return std_logic_vector is
           variable x, y, z, xn, yn : signed(@FW@-1 downto 0);
        begin
           x := INVKH;
           y := (others => '0');
           z := z0;
           for i in 1 to @N@ loop
              for rep in 0 to 1 loop
                 if rep = 0 or i = 4 or i = 13 or i = 40 or i = 121 then
                    if z >= 0 then
                       xn := x + shift_right(y, i);
                       yn := y + shift_right(x, i);
                       z  := z - ATANH_T(i);
                    else
                       xn := x - shift_right(y, i);
                       yn := y - shift_right(x, i);
                       z  := z + ATANH_T(i);
                    end if;
                    x := xn;
                    y := yn;
                 end if;
              end loop;
           end loop;
           return std_logic_vector(y) & std_logic_vector(x);
        end function;

        function fx_circvec(x0, y0 : signed(@FW@-1 downto 0)) return signed is
           variable x, y, z, xn, yn : signed(@FW@-1 downto 0);
        begin
           x := x0;
           y := y0;
           z := (others => '0');
           for i in 0 to @N@-1 loop
              if y >= 0 then
                 xn := x + shift_right(y, i);
                 yn := y - shift_right(x, i);
                 z  := z + ATAN_T(i);
              else
                 xn := x - shift_right(y, i);
                 yn := y + shift_right(x, i);
                 z  := z - ATAN_T(i);
              end if;
              x := xn;
              y := yn;
           end loop;
           return z;
        end function;

        function fx_hypvec(x0, y0 : signed(@FW@-1 downto 0)) return signed is
           variable x, y, z, xn, yn : signed(@FW@-1 downto 0);
        begin
           x := x0;
           y := y0;
           z := (others => '0');
           for i in 1 to @N@ loop
              for rep in 0 to 1 loop
                 if rep = 0 or i = 4 or i = 13 or i = 40 or i = 121 then
                    if y >= 0 then
                       xn := x - shift_right(y, i);
                       yn := y - shift_right(x, i);
                       z  := z + ATANH_T(i);
                    else
                       xn := x + shift_right(y, i);
                       yn := y + shift_right(x, i);
                       z  := z - ATANH_T(i);
                    end if;
                    x := xn;
                    y := yn;
                 end if;
              end loop;
           end loop;
           return z;
        end function;

        function fx_sqrt(v : signed(@FW@-1 downto 0)) return signed is
           constant K : integer := @F@+1;
           variable nn : unsigned(2*K-1 downto 0);
           variable rm, trial, root : unsigned(K+2 downto 0);
        begin
           nn := shift_left(resize(unsigned(v), 2*K), @F@);
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
           return signed(resize(root, @FW@));
        end function;

        function fx2fp(v : signed(@FW@-1 downto 0); kexp : integer) return std_logic_vector is
           variable mag : unsigned(@FW@-1 downto 0);
           variable p   : integer;
        begin
           if v(@FW@-1) = '1' then
              mag := unsigned(-v);
           else
              mag := unsigned(v);
           end if;
           if mag = 0 then
              return fp_zero('0');
           end if;
           p := fp_msb(mag);
           return fp_pack(v(@FW@-1), p - @F@ + kexp, shift_left(mag, @FW@-1-p), '0');
        end function;

        function fp2fx(x : std_logic_vector(@WB@-1 downto 0)) return signed is
           variable u  : unsigned(@FW@-1 downto 0);
           variable sh : integer;
        begin
           u  := resize(fp_sig(x), @FW@);
           sh := fp_exp(x) - @MB@ + @F@;
           if sh >= 0 then
              u := shift_left(u, sh);
           else
              u := shift_right(u, -sh);
           end if;
           if x(@WB@-1) = '1' then
              return -signed(u);
           end if;
           return signed(u);
        end function;

        function fx_expk(xf : signed(@FW@-1 downto 0)) return integer is
           variable p : signed(@FW@-1 downto 0);
        begin
           p := fx_mul(xf, INVLN2) + HALF;
           return to_integer(resize(shift_right(p, @F@), 32));
        end function;

        function fx_expr(xf : signed(@FW@-1 downto 0)) return signed is
           variable kl : signed(32+@FW@-1 downto 0);
        begin
           kl := to_signed(fx_expk(xf), 32) * LN2;
           return xf - kl(@FW@-1 downto 0);
        end function;

        function fx_lnme(m : signed(@FW@-1 downto 0); e : integer) return signed is
           variable mm, z : signed(@FW@-1 downto 0);
           variable ee    : integer;
           variable el    : signed(32+@FW@-1 downto 0);
        begin
           mm := m;
           ee := e;
           if mm > SQRT2 then
              mm := shift_right(mm, 1);
              ee := ee + 1;
           end if;
           z  := fx_hypvec(mm + ONE, mm - ONE);
           el := to_signed(ee, 32) * LN2;
           return el(@FW@-1 downto 0) + shift_left(z, 1);
        end function;
        """,
        """
        function signed [@FW@-1:0] fx_mul;
           input signed [@FW@-1:0] a;
           input signed [@FW@-1:0] b;
           reg signed [2*@FW@-1:0] p;
           begin
              p = a * b;
              p = p >>> @F@;
              fx_mul = p[@FW@-1:0];
           end
        endfunction

        function [2*@FW@-1:0] fx_cossin;
           input signed [@FW@-1:0] z0;
           reg signed [@FW@-1:0] x, y, z, xn, yn;
           integer i;
           begin
              x = INVK;
              y = 0;
              z = z0;
              for (i = 0; i < @N@; i = i + 1) begin
                 if (z >= 0) begin
                    xn = x - (y >>> i);
                    yn = y + (x >>> i);
                    z  = z - atan_tab(i);
                 end else begin
                    xn = x + (y >>> i);
                    yn = y - (x >>> i);
                    z  = z + atan_tab(i);
                 end
                 x = xn;
                 y = yn;
              end
              fx_cossin = {y, x};
           end
        endfunction

        function [2*@FW@-1:0] fx_hyprot;
           input signed [@FW@-1:0] z0;
           reg signed [@FW@-1:0] x, y, z, xn, yn;
           integer i, rep;
           begin
              x = INVKH;
              y = 0;
              z = z0;
              for (i = 1; i <= @N@; i = i + 1)
                 for (rep = 0; rep < 2; rep = rep + 1)
                    if (rep == 0 || i == 4 || i == 13 || i == 40 || i == 121) begin
                       if (z >= 0) begin
                          xn = x + (y >>> i);
                          yn = y + (x >>> i);
                          z  = z - atanh_tab(i);
                       end else begin
                          xn = x - (y >>> i);
                          yn = y - (x >>> i);
                          z  = z + atanh_tab(i);
                       end
                       x = xn;
                       y = yn;
                    end
              fx_hyprot = {y, x};
           end
        endfunction

        function signed [@FW@-1:0] fx_circvec;
           input signed [@FW@-1:0] x0;
           input signed [@FW@-1:0] y0;
           reg signed [@FW@-1:0] x, y, z, xn, yn;
           integer i;
           begin
              x = x0;
              y = y0;
              z = 0;
              for (i = 0; i < @N@; i = i + 1) begin
                 if (y >= 0) begin
                    xn = x + (y >>> i);
                    yn = y - (x >>> i);
                    z  = z + atan_tab(i);
                 end else begin
                    xn = x - (y >>> i);
                    yn = y + (x >>> i);
                    z  = z - atan_tab(i);
                 end
                 x = xn;
                 y = yn;
              end
              fx_circvec = z;
           end
        endfunction

        function signed [@FW@-1:0] fx_hypvec;
           input signed [@FW@-1:0] x0;
           input signed [@FW@-1:0] y0;
           reg signed [@FW@-1:0] x, y, z, xn, yn;
           integer i, rep;
           begin
              x = x0;
              y = y0;
              z = 0;
              for (i = 1; i <= @N@; i = i + 1)
                 for (rep = 0; rep < 2; rep = rep + 1)
                    if (rep == 0 || i == 4 || i == 13 || i == 40 || i == 121) begin
                       if (y >= 0) begin
                          xn = x - (y >>> i);
                          yn = y - (x >>> i);
                          z  = z + atanh_tab(i);
                       end else begin
                          xn = x + (y >>> i);
                          yn = y + (x >>> i);
                          z  = z - atanh_tab(i);
                       end
                       x = xn;
                       y = yn;
                    end
              fx_hypvec = z;
           end
        endfunction

        function signed [@FW@-1:0] fx_sqrt;
           input signed [@FW@-1:0] v;
           reg [2*@F@+1:0] nn;
           reg [@F@+3:0] rm, trial, root;
           integer i;
           begin
              nn = v;
              nn = nn << @F@;
              rm = 0;
              root = 0;
              for (i = @F@; i >= 0; i = i - 1) begin
                 rm = {rm[@F@+1:0], nn[2*i+1 -: 2]};
                 trial = {root[@F@+1:0], 2'b01};
                 if (rm >= trial) begin
                    rm = rm - trial;
                    root = {root[@F@+2:0], 1'b1};
                 end else root = {root[@F@+2:0], 1'b0};
              end
              fx_sqrt = root;
           end
        endfunction

        function [@WB@-1:0] fx2fp;
           input signed [@FW@-1:0] v;
           input integer kexp;
           reg sg;
           reg [@FW@-1:0] mag;
           integer p;
           begin
              sg = v[@FW@-1];
              mag = sg ? -v : v;
              if (mag == 0) fx2fp = fp_zero(1'b0);
              else begin
                 p = fp_msb_@FW@(mag);
                 mag = mag << (@FW@ - 1 - p);
                 fx2fp = fp_pack_@FW@(sg, p - @F@ + kexp, mag, 1'b0);
              end
           end
        endfunction

        function signed [@FW@-1:0] fp2fx;
           input [@WB@-1:0] x;
           reg [@FW@-1:0] u;
           reg signed [@FW@-1:0] r;
           integer sh;
           begin
              u = fp_sig(x);
              sh = fp_exp(x);
              sh = sh - @MB@ + @F@;
              if (sh >= 0) u = u << sh;
              else if (-sh >= @FW@) u = 0;
              else u = u >> (-sh);
              r = u;
              fp2fx = x[@WB@-1] ? -r : r;
           end
        endfunction

        function integer fx_expk;
           input signed [@FW@-1:0] xf;
           reg signed [@FW@-1:0] p;
           begin
              p = fx_mul(xf, INVLN2) + HALF;
              p = p >>> @F@;
              fx_expk = p;
           end
        endfunction

        function signed [@FW@-1:0] fx_expr;
           input signed [@FW@-1:0] xf;
           integer k;
           reg signed [@FW@+31:0] kl;
           begin
              k = fx_expk(xf);
              kl = k * LN2;
              fx_expr = xf - kl[@FW@-1:0];
           end
        endfunction

        function signed [@FW@-1:0] fx_lnme;
           input signed [@FW@-1:0] m;
           input integer e;
           reg signed [@FW@-1:0] mm, z;
           reg signed [@FW@+31:0] el;
           integer ee;
           begin
              mm = m;
              ee = e;
              if (mm > SQRT2) begin
                 mm = mm >>> 1;
                 ee = ee + 1;
              end
              z = fx_hypvec(mm + ONE, mm - ONE);
              el = ee * LN2;
              fx_lnme = el[@FW@-1:0] + (z <<< 1);
           end
        endfunction
        """);
  }

  // ------------------------------------------------------------------------------------------
  // Exponential functions
  // ------------------------------------------------------------------------------------------

  /** {@code fm_exp(x)}. */
  FpCordic addExp() {
    base();
    if (!once("exp")) return this;
    declare(
        """
        function fm_exp(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e, k  : integer;
           variable xf, r : signed(@FW@-1 downto 0);
           variable hr    : std_logic_vector(2*@FW@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_isinf(x) then
              if x(@WB@-1) = '1' then
                 return fp_zero('0');
              end if;
              return x;
           elsif fp_iszero(x) then
              return fx2fp(ONE, 0);
           end if;
           e := fp_exp(x);
           if e >= @EB@ then
              if x(@WB@-1) = '1' then
                 return fp_zero('0');
              end if;
              return fp_inf('0');
           elsif e <= -(@MB@+3) then
              return fx2fp(ONE, 0);
           end if;
           xf := fp2fx(x);
           k  := fx_expk(xf);
           r  := fx_expr(xf);
           hr := fx_hyprot(r);
           return fx2fp(signed(hr(@FW@-1 downto 0)) + signed(hr(2*@FW@-1 downto @FW@)), k);
        end function;
        """,
        """
        function [@WB@-1:0] fm_exp;
           input [@WB@-1:0] x;
           integer e, k;
           reg signed [@FW@-1:0] xf, r, s;
           reg [2*@FW@-1:0] hr;
           begin
              if (fp_isnan(x)) fm_exp = NAN_V;
              else if (fp_isinf(x)) fm_exp = x[@WB@-1] ? fp_zero(1'b0) : x;
              else if (fp_iszero(x)) fm_exp = fx2fp(ONE, 0);
              else begin
                 e = fp_exp(x);
                 if (e >= @EB@) fm_exp = x[@WB@-1] ? fp_zero(1'b0) : fp_inf(1'b0);
                 else if (e <= -(@MB@+3)) fm_exp = fx2fp(ONE, 0);
                 else begin
                    xf = fp2fx(x);
                    k = fx_expk(xf);
                    r = fx_expr(xf);
                    hr = fx_hyprot(r);
                    s = $signed(hr[@FW@-1:0]) + $signed(hr[2*@FW@-1:@FW@]);
                    fm_exp = fx2fp(s, k);
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  /** {@code fm_expm1(x)}. */
  FpCordic addExpm1() {
    base();
    if (!once("expm1")) return this;
    declare(
        """
        function fm_expm1(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e, k  : integer;
           variable xf, r, s : signed(@FW@-1 downto 0);
           variable hr    : std_logic_vector(2*@FW@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_isinf(x) then
              if x(@WB@-1) = '1' then
                 return fx2fp(-ONE, 0);
              end if;
              return x;
           elsif fp_iszero(x) then
              return x;
           end if;
           e := fp_exp(x);
           if e >= @EB@ then
              if x(@WB@-1) = '1' then
                 return fx2fp(-ONE, 0);
              end if;
              return fp_inf('0');
           elsif e <= -(@MB@+3) then
              return x;
           end if;
           xf := fp2fx(x);
           k  := fx_expk(xf);
           r  := fx_expr(xf);
           hr := fx_hyprot(r);
           s  := signed(hr(@FW@-1 downto 0)) + signed(hr(2*@FW@-1 downto @FW@));
           if k = 0 then
              return fx2fp(s - ONE, 0);
           end if;
           return fp_add(fx2fp(s, k), fx2fp(ONE, 0), '1');
        end function;
        """,
        """
        function [@WB@-1:0] fm_expm1;
           input [@WB@-1:0] x;
           integer e, k;
           reg signed [@FW@-1:0] xf, r, s;
           reg [2*@FW@-1:0] hr;
           reg [@WB@-1:0] ex, one;
           begin
              if (fp_isnan(x)) fm_expm1 = NAN_V;
              else if (fp_isinf(x)) fm_expm1 = x[@WB@-1] ? fx2fp(-ONE, 0) : x;
              else if (fp_iszero(x)) fm_expm1 = x;
              else begin
                 e = fp_exp(x);
                 if (e >= @EB@) fm_expm1 = x[@WB@-1] ? fx2fp(-ONE, 0) : fp_inf(1'b0);
                 else if (e <= -(@MB@+3)) fm_expm1 = x;
                 else begin
                    xf = fp2fx(x);
                    k = fx_expk(xf);
                    r = fx_expr(xf);
                    hr = fx_hyprot(r);
                    s = $signed(hr[@FW@-1:0]) + $signed(hr[2*@FW@-1:@FW@]);
                    if (k == 0) fm_expm1 = fx2fp(s - ONE, 0);
                    else begin
                       ex = fx2fp(s, k);
                       one = fx2fp(ONE, 0);
                       fm_expm1 = fp_add(ex, one, 1'b1);
                    end
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  /** {@code fm_pow(base, exponent)} with the special cases of Java's Math.pow. */
  FpCordic addPow() {
    base();
    if (!once("pow")) return this;
    declare(
        """
        function fm_isint(y : std_logic_vector(@WB@-1 downto 0)) return boolean is
           variable e : integer;
           variable s : unsigned(@MB@ downto 0);
        begin
           e := fp_exp(y);
           s := fp_sig(y);
           if e >= @MB@ then
              return true;
           elsif e < 0 then
              return false;
           end if;
           for i in 0 to @MB@ loop
              if i < @MB@ - e and s(i) = '1' then
                 return false;
              end if;
           end loop;
           return true;
        end function;

        function fm_isodd(y : std_logic_vector(@WB@-1 downto 0)) return boolean is
           variable e : integer;
           variable s : unsigned(@MB@ downto 0);
        begin
           e := fp_exp(y);
           s := fp_sig(y);
           if e < 0 or e > @MB@ or not fm_isint(y) then
              return false;
           end if;
           return s(@MB@ - e) = '1';
        end function;

        function fm_bit(c : boolean) return std_logic is
        begin
           if c then
              return '1';
           end if;
           return '0';
        end function;

        function fm_pow(b, y : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable bx          : unsigned(@EB@-1 downto 0);
           variable gt1, eq1    : boolean;
           variable yodd, rneg  : boolean;
           variable sat         : boolean;
           variable xneg        : std_logic;
           variable lf, xf, r   : signed(@FW@-1 downto 0);
           variable lmag        : unsigned(@FW@-1 downto 0);
           variable prod        : unsigned(@MB@+@FW@ downto 0);
           variable wide        : unsigned(@WW@-1 downto 0);
           variable s, k, eb    : integer;
           variable hr          : std_logic_vector(2*@FW@-1 downto 0);
           variable res         : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_iszero(y) then
              return fx2fp(ONE, 0);
           elsif fp_isnan(b) or fp_isnan(y) then
              return fp_nan;
           end if;
           bx  := unsigned(b(@WB@-2 downto @MB@));
           gt1 := bx > @BIAS@ or (bx = @BIAS@ and unsigned(b(@MB@-1 downto 0)) /= 0);
           eq1 := bx = @BIAS@ and unsigned(b(@MB@-1 downto 0)) = 0;
           if fp_isinf(y) then
              if eq1 then
                 return fp_nan;
              elsif gt1 = (y(@WB@-1) = '0') then
                 return fp_inf('0');
              end if;
              return fp_zero('0');
           end if;
           yodd := fm_isodd(y);
           rneg := b(@WB@-1) = '1' and yodd;
           if fp_iszero(b) then
              if y(@WB@-1) = '0' then
                 return fp_zero(fm_bit(rneg));
              end if;
              return fp_inf(fm_bit(rneg));
           elsif fp_isinf(b) then
              if y(@WB@-1) = '0' then
                 return fp_inf(fm_bit(rneg));
              end if;
              return fp_zero(fm_bit(rneg));
           elsif b(@WB@-1) = '1' and not fm_isint(y) then
              return fp_nan;
           elsif eq1 then
              if rneg then
                 return fx2fp(-ONE, 0);
              end if;
              return fx2fp(ONE, 0);
           end if;
           eb   := fp_exp(b);
           lf   := fx_lnme(shift_left(signed(resize(fp_sig(b), @FW@)), @F@-@MB@), eb);
           lmag := unsigned(abs(lf));
           s    := fp_exp(y) - @MB@;
           prod := fp_sig(y) * lmag;
           sat  := s >= @SMAX@;
           wide := (others => '0');
           if not sat then
              wide := resize(prod, @WW@);
              if s >= 0 then
                 wide := shift_left(wide, s);
              else
                 wide := shift_right(wide, -s);
              end if;
              sat := wide(@WW@-1 downto @F@+@EB@+2) /= 0;
           end if;
           xneg := lf(@FW@-1) xor y(@WB@-1);
           if sat then
              if xneg = '1' then
                 return fp_zero(fm_bit(rneg));
              end if;
              return fp_inf(fm_bit(rneg));
           end if;
           xf := signed(wide(@FW@-1 downto 0));
           if xneg = '1' then
              xf := -xf;
           end if;
           k   := fx_expk(xf);
           r   := fx_expr(xf);
           hr  := fx_hyprot(r);
           res := fx2fp(signed(hr(@FW@-1 downto 0)) + signed(hr(2*@FW@-1 downto @FW@)), k);
           res(@WB@-1) := fm_bit(rneg);
           return res;
        end function;
        """,
        """
        function fm_isint;
           input [@WB@-1:0] y;
           integer e, i;
           reg [@MB@:0] s;
           begin
              e = fp_exp(y);
              s = fp_sig(y);
              if (e >= @MB@) fm_isint = 1'b1;
              else if (e < 0) fm_isint = 1'b0;
              else begin
                 fm_isint = 1'b1;
                 for (i = 0; i < @MB@ - e; i = i + 1)
                    if (s[i]) fm_isint = 1'b0;
              end
           end
        endfunction

        function fm_isodd;
           input [@WB@-1:0] y;
           integer e;
           reg [@MB@:0] s;
           reg isint;
           begin
              e = fp_exp(y);
              s = fp_sig(y);
              isint = fm_isint(y);
              if (e < 0 || e > @MB@ || !isint) fm_isodd = 1'b0;
              else fm_isodd = s[@MB@ - e];
           end
        endfunction

        function [@WB@-1:0] fm_pow;
           input [@WB@-1:0] b;
           input [@WB@-1:0] y;
           reg gt1, eq1, yodd, yint, rneg, sat, xneg;
           reg [@EB@-1:0] bx;
           reg signed [@FW@-1:0] lf, xf, r;
           reg [@FW@-1:0] lmag;
           reg [@MB@:0] sy, sb;
           reg [@MB@+@FW@:0] prod;
           reg [@WW@-1:0] wide;
           reg [2*@FW@-1:0] hr;
           reg [@WB@-1:0] res;
           integer s, k, eb;
           begin
              bx = b[@WB@-2:@MB@];
              gt1 = (bx > @BIAS@) || (bx == @BIAS@ && b[@MB@-1:0] != 0);
              eq1 = (bx == @BIAS@) && (b[@MB@-1:0] == 0);
              yodd = fm_isodd(y);
              yint = fm_isint(y);
              rneg = b[@WB@-1] && yodd;
              if (fp_iszero(y)) fm_pow = fx2fp(ONE, 0);
              else if (fp_isnan(b) || fp_isnan(y)) fm_pow = NAN_V;
              else if (fp_isinf(y)) begin
                 if (eq1) fm_pow = NAN_V;
                 else if (gt1 == !y[@WB@-1]) fm_pow = fp_inf(1'b0);
                 else fm_pow = fp_zero(1'b0);
              end
              else if (fp_iszero(b)) fm_pow = y[@WB@-1] ? fp_inf(rneg) : fp_zero(rneg);
              else if (fp_isinf(b)) fm_pow = y[@WB@-1] ? fp_zero(rneg) : fp_inf(rneg);
              else if (b[@WB@-1] && !yint) fm_pow = NAN_V;
              else if (eq1) fm_pow = rneg ? fx2fp(-ONE, 0) : fx2fp(ONE, 0);
              else begin
                 eb = fp_exp(b);
                 sb = fp_sig(b);
                 lf = sb;
                 lf = lf <<< (@F@ - @MB@);
                 lf = fx_lnme(lf, eb);
                 lmag = lf[@FW@-1] ? -lf : lf;
                 s = fp_exp(y);
                 s = s - @MB@;
                 sy = fp_sig(y);
                 prod = sy * lmag;
                 sat = (s >= @SMAX@);
                 wide = 0;
                 if (!sat) begin
                    wide = prod;
                    if (s >= 0) wide = wide << s;
                    else wide = wide >> (-s);
                    sat = (wide[@WW@-1:@F@+@EB@+2] != 0);
                 end
                 xneg = lf[@FW@-1] ^ y[@WB@-1];
                 if (sat) fm_pow = xneg ? fp_zero(rneg) : fp_inf(rneg);
                 else begin
                    xf = wide[@FW@-1:0];
                    if (xneg) xf = -xf;
                    k = fx_expk(xf);
                    r = fx_expr(xf);
                    hr = fx_hyprot(r);
                    res = fx2fp($signed(hr[@FW@-1:0]) + $signed(hr[2*@FW@-1:@FW@]), k);
                    res[@WB@-1] = rneg;
                    fm_pow = res;
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  // ------------------------------------------------------------------------------------------
  // Logarithms
  // ------------------------------------------------------------------------------------------

  /** {@code fm_ln(x, base10)}: natural logarithm, or the logarithm base 10 if base10 is '1'. */
  FpCordic addLn() {
    base();
    if (!once("ln")) return this;
    declare(
        """
        function fm_ln(x : std_logic_vector(@WB@-1 downto 0); base10 : std_logic)
                  return std_logic_vector is
           variable e  : integer;
           variable l  : signed(@FW@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_iszero(x) then
              return fp_inf('1');
           elsif x(@WB@-1) = '1' then
              return fp_nan;
           elsif fp_isinf(x) then
              return x;
           end if;
           e := fp_exp(x);
           if e = 0 and unsigned(x(@MB@-1 downto 0)) = 0 then
              return fp_zero('0');
           end if;
           l := fx_lnme(shift_left(signed(resize(fp_sig(x), @FW@)), @F@-@MB@), e);
           if base10 = '1' then
              l := fx_mul(l, INVLN10);
           end if;
           return fx2fp(l, 0);
        end function;
        """,
        """
        function [@WB@-1:0] fm_ln;
           input [@WB@-1:0] x;
           input base10;
           integer e;
           reg [@MB@:0] sg;
           reg signed [@FW@-1:0] l;
           begin
              if (fp_isnan(x)) fm_ln = NAN_V;
              else if (fp_iszero(x)) fm_ln = fp_inf(1'b1);
              else if (x[@WB@-1]) fm_ln = NAN_V;
              else if (fp_isinf(x)) fm_ln = x;
              else begin
                 e = fp_exp(x);
                 if (e == 0 && x[@MB@-1:0] == 0) fm_ln = fp_zero(1'b0);
                 else begin
                    sg = fp_sig(x);
                    l = sg;
                    l = l <<< (@F@ - @MB@);
                    l = fx_lnme(l, e);
                    if (base10) l = fx_mul(l, INVLN10);
                    fm_ln = fx2fp(l, 0);
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  /** {@code fm_log1p(x)}; also needs {@link #addLn()}. */
  FpCordic addLog1p() {
    addLn();
    if (!once("log1p")) return this;
    declare(
        """
        function fm_log1p(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e, p, ee : integer;
           variable v, m, l  : signed(@FW@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_isinf(x) then
              if x(@WB@-1) = '1' then
                 return fp_nan;
              end if;
              return x;
           elsif fp_iszero(x) then
              return x;
           end if;
           e := fp_exp(x);
           if x(@WB@-1) = '1' and e >= 0 then
              if e >= 1 or unsigned(x(@MB@-1 downto 0)) /= 0 then
                 return fp_nan;
              end if;
              return fp_inf('1');
           elsif e <= -(@MB@+2) then
              return x;
           elsif e >= 1 then
              return fm_ln(fp_add(x, fx2fp(ONE, 0), '0'), '0');
           end if;
           v  := ONE + fp2fx(x);
           p  := fp_msb(unsigned(v));
           ee := p - @F@;
           if p >= @F@ then
              m := shift_right(v, p - @F@);
           else
              m := shift_left(v, @F@ - p);
           end if;
           l := fx_lnme(m, ee);
           return fx2fp(l, 0);
        end function;
        """,
        """
        function [@WB@-1:0] fm_log1p;
           input [@WB@-1:0] x;
           integer e, p, ee;
           reg signed [@FW@-1:0] v, m, l;
           reg [@WB@-1:0] sum;
           begin
              if (fp_isnan(x)) fm_log1p = NAN_V;
              else if (fp_isinf(x)) fm_log1p = x[@WB@-1] ? NAN_V : x;
              else if (fp_iszero(x)) fm_log1p = x;
              else begin
                 e = fp_exp(x);
                 if (x[@WB@-1] && e >= 0) begin
                    if (e >= 1 || x[@MB@-1:0] != 0) fm_log1p = NAN_V;
                    else fm_log1p = fp_inf(1'b1);
                 end
                 else if (e <= -(@MB@+2)) fm_log1p = x;
                 else if (e >= 1) begin
                    sum = fp_add(x, fx2fp(ONE, 0), 1'b0);
                    fm_log1p = fm_ln(sum, 1'b0);
                 end
                 else begin
                    v = fp2fx(x);
                    v = ONE + v;
                    p = fp_msb_@FW@(v);
                    ee = p - @F@;
                    if (p >= @F@) m = v >>> (p - @F@);
                    else m = v <<< (@F@ - p);
                    l = fx_lnme(m, ee);
                    fm_log1p = fx2fp(l, 0);
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  // ------------------------------------------------------------------------------------------
  // Trigonometric functions
  // ------------------------------------------------------------------------------------------

  /** {@code fm_sincos(x)}: result is {cos, sin}. */
  FpCordic addSinCos() {
    base();
    if (!once("sincos")) return this;
    declare(
        """
        function fm_sincos(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e, s       : integer;
           variable prod, t    : unsigned(@MB@+@CB@ downto 0);
           variable f          : signed(@CB@-1 downto 0);
           variable q          : unsigned(1 downto 0);
           variable phi, rr, sn, cs, sx, cx : signed(@FW@-1 downto 0);
           variable sc         : std_logic_vector(2*@FW@-1 downto 0);
           variable sinf, cosf : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_isnan(x) or fp_isinf(x) then
              return fp_nan & fp_nan;
           elsif fp_iszero(x) then
              return fx2fp(ONE, 0) & x;
           end if;
           e := fp_exp(x);
           if e <= -(@MB@/2+2) then
              return fx2fp(ONE, 0) & x;
           end if;
           s    := e - @MB@;
           prod := fp_sig(x) * TWOPI;
           if s >= 0 then
              t := shift_left(prod, s);
           else
              t := shift_right(prod, -s);
           end if;
           f := signed(t(@CB@-1 downto 0));
           q := unsigned(t(@CB@+1 downto @CB@));
           if t(@CB@-1) = '1' then
              q := q + 1;
           end if;
           phi := resize(shift_right(f, @CB@ - @F@), @FW@);
           rr  := fx_mul(phi, PIH);
           sc  := fx_cossin(rr);
           sn  := signed(sc(2*@FW@-1 downto @FW@));
           cs  := signed(sc(@FW@-1 downto 0));
           case to_integer(q) is
              when 0 => sx := sn;  cx := cs;
              when 1 => sx := cs;  cx := -sn;
              when 2 => sx := -sn; cx := -cs;
              when others => sx := -cs; cx := sn;
           end case;
           sinf := fx2fp(sx, 0);
           cosf := fx2fp(cx, 0);
           sinf(@WB@-1) := sinf(@WB@-1) xor x(@WB@-1);
           return cosf & sinf;
        end function;
        """,
        """
        function [2*@WB@-1:0] fm_sincos;
           input [@WB@-1:0] x;
           integer e, s;
           reg [@MB@+@CB@:0] prod, t;
           reg [@MB@:0] sg;
           reg signed [@CB@-1:0] f;
           reg [1:0] q;
           reg signed [@FW@-1:0] phi, rr, sn, cs, sx, cx;
           reg [2*@FW@-1:0] sc;
           reg [@WB@-1:0] sinf, cosf;
           begin
              if (fp_isnan(x) || fp_isinf(x)) fm_sincos = {NAN_V, NAN_V};
              else if (fp_iszero(x)) fm_sincos = {fx2fp(ONE, 0), x};
              else begin
                 e = fp_exp(x);
                 if (e <= -(@MB@/2+2)) fm_sincos = {fx2fp(ONE, 0), x};
                 else begin
                    s = e - @MB@;
                    sg = fp_sig(x);
                    prod = sg * TWOPI;
                    if (s >= 0) t = prod << s;
                    else t = prod >> (-s);
                    f = t[@CB@-1:0];
                    q = t[@CB@+1:@CB@] + t[@CB@-1];
                    phi = f >>> (@CB@ - @F@);
                    rr = fx_mul(phi, PIH);
                    sc = fx_cossin(rr);
                    sn = sc[2*@FW@-1:@FW@];
                    cs = sc[@FW@-1:0];
                    case (q)
                       2'd0: begin sx = sn; cx = cs; end
                       2'd1: begin sx = cs; cx = -sn; end
                       2'd2: begin sx = -sn; cx = -cs; end
                       default: begin sx = -cs; cx = sn; end
                    endcase
                    sinf = fx2fp(sx, 0);
                    cosf = fx2fp(cx, 0);
                    sinf[@WB@-1] = sinf[@WB@-1] ^ x[@WB@-1];
                    fm_sincos = {cosf, sinf};
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  /** {@code fm_atan(x)}, {@code fm_asin(x)} and {@code fm_acos(x)}. */
  FpCordic addArc() {
    base();
    if (!once("arc")) return this;
    final var numeratorWidth = f + mb + 2;
    declare(
        """
        function fm_atan(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e : integer;
           variable v, z, u : signed(@FW@-1 downto 0);
           variable num, q : unsigned(@NW@-1 downto 0);
           variable res : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_isinf(x) then
              res := fx2fp(PIH, 0);
              res(@WB@-1) := x(@WB@-1);
              return res;
           elsif fp_iszero(x) then
              return x;
           end if;
           e := fp_exp(x);
           if e <= -(@MB@/2+2) then
              return x;
           end if;
           if e < 0 then
              v := abs(fp2fx(x));
              z := fx_circvec(ONE, v);
           else
              u := (others => '0');
              if e <= @F@+@MB@ then
                 num := shift_left(to_unsigned(1, @NW@), @F@+@MB@-e);
                 q   := num / resize(fp_sig(x), @NW@);
                 u   := signed(resize(q, @FW@));
              end if;
              z := PIH - fx_circvec(ONE, u);
           end if;
           res := fx2fp(z, 0);
           res(@WB@-1) := x(@WB@-1);
           return res;
        end function;

        function fm_asin(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e : integer;
           variable xa, w, root, z : signed(@FW@-1 downto 0);
           variable bx : unsigned(@EB@-1 downto 0);
           variable res : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_isnan(x) or fp_isinf(x) then
              return fp_nan;
           elsif fp_iszero(x) then
              return x;
           end if;
           bx := unsigned(x(@WB@-2 downto @MB@));
           if bx > @BIAS@ or (bx = @BIAS@ and unsigned(x(@MB@-1 downto 0)) /= 0) then
              return fp_nan;
           end if;
           e := fp_exp(x);
           if e <= -(@MB@/2+2) then
              return x;
           end if;
           xa   := abs(fp2fx(x));
           w    := fx_mul(ONE - xa, ONE + xa);
           root := fx_sqrt(w);
           z    := fx_circvec(root, xa);
           res  := fx2fp(z, 0);
           res(@WB@-1) := x(@WB@-1);
           return res;
        end function;

        function fm_acos(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable xa, w, root, z : signed(@FW@-1 downto 0);
           variable bx : unsigned(@EB@-1 downto 0);
        begin
           if fp_isnan(x) or fp_isinf(x) then
              return fp_nan;
           end if;
           bx := unsigned(x(@WB@-2 downto @MB@));
           if bx > @BIAS@ or (bx = @BIAS@ and unsigned(x(@MB@-1 downto 0)) /= 0) then
              return fp_nan;
           end if;
           xa   := abs(fp2fx(x));
           w    := fx_mul(ONE - xa, ONE + xa);
           root := fx_sqrt(w);
           if xa = ONE then
              z := (others => '0');
           else
              z := fx_circvec(xa, root);
           end if;
           if x(@WB@-1) = '1' then
              z := PI - z;
           end if;
           return fx2fp(z, 0);
        end function;
        """,
        """
        function [@WB@-1:0] fm_atan;
           input [@WB@-1:0] x;
           integer e;
           reg signed [@FW@-1:0] v, z, u;
           reg [@NW@-1:0] num, q, den;
           reg [@MB@:0] sg;
           reg [@WB@-1:0] res;
           begin
              if (fp_isnan(x)) fm_atan = NAN_V;
              else if (fp_isinf(x)) begin
                 res = fx2fp(PIH, 0);
                 res[@WB@-1] = x[@WB@-1];
                 fm_atan = res;
              end
              else if (fp_iszero(x)) fm_atan = x;
              else begin
                 e = fp_exp(x);
                 if (e <= -(@MB@/2+2)) fm_atan = x;
                 else begin
                    if (e < 0) begin
                       v = fp2fx(x);
                       if (v < 0) v = -v;
                       z = fx_circvec(ONE, v);
                    end else begin
                       u = 0;
                       if (e <= @F@ + @MB@) begin
                          sg = fp_sig(x);
                          num = 1;
                          num = num << (@F@ + @MB@ - e);
                          den = sg;
                          q = num / den;
                          u = q;
                       end
                       z = fx_circvec(ONE, u);
                       z = PIH - z;
                    end
                    res = fx2fp(z, 0);
                    res[@WB@-1] = x[@WB@-1];
                    fm_atan = res;
                 end
              end
           end
        endfunction

        function [@WB@-1:0] fm_asin;
           input [@WB@-1:0] x;
           integer e;
           reg signed [@FW@-1:0] xa, w, root, z;
           reg [@EB@-1:0] bx;
           reg [@WB@-1:0] res;
           begin
              bx = x[@WB@-2:@MB@];
              if (fp_isnan(x) || fp_isinf(x)) fm_asin = NAN_V;
              else if (fp_iszero(x)) fm_asin = x;
              else if (bx > @BIAS@ || (bx == @BIAS@ && x[@MB@-1:0] != 0)) fm_asin = NAN_V;
              else begin
                 e = fp_exp(x);
                 if (e <= -(@MB@/2+2)) fm_asin = x;
                 else begin
                    xa = fp2fx(x);
                    if (xa < 0) xa = -xa;
                    w = fx_mul(ONE - xa, ONE + xa);
                    root = fx_sqrt(w);
                    z = fx_circvec(root, xa);
                    res = fx2fp(z, 0);
                    res[@WB@-1] = x[@WB@-1];
                    fm_asin = res;
                 end
              end
           end
        endfunction

        function [@WB@-1:0] fm_acos;
           input [@WB@-1:0] x;
           reg signed [@FW@-1:0] xa, w, root, z;
           reg [@EB@-1:0] bx;
           begin
              bx = x[@WB@-2:@MB@];
              if (fp_isnan(x) || fp_isinf(x)) fm_acos = NAN_V;
              else if (bx > @BIAS@ || (bx == @BIAS@ && x[@MB@-1:0] != 0)) fm_acos = NAN_V;
              else begin
                 xa = fp2fx(x);
                 if (xa < 0) xa = -xa;
                 w = fx_mul(ONE - xa, ONE + xa);
                 root = fx_sqrt(w);
                 z = (xa == ONE) ? 0 : fx_circvec(xa, root);
                 if (x[@WB@-1]) z = PI - z;
                 fm_acos = fx2fp(z, 0);
              end
           end
        endfunction
        """,
        "NW", numeratorWidth);
    return this;
  }

  // ------------------------------------------------------------------------------------------
  // Hyperbolic functions
  // ------------------------------------------------------------------------------------------

  /** {@code fm_sinhcosh(x)}: result is {cosh, sinh}. */
  FpCordic addSinhCosh() {
    base();
    if (!once("sinhcosh")) return this;
    declare(
        """
        function fm_sinhcosh(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable e, k          : integer;
           variable xa, r, sh, ch : signed(@FW@-1 downto 0);
           variable hr            : std_logic_vector(2*@FW@-1 downto 0);
           variable a, b, sinhf, coshf : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan & fp_nan;
           elsif fp_isinf(x) then
              return fp_inf('0') & x;
           elsif fp_iszero(x) then
              return fx2fp(ONE, 0) & x;
           end if;
           e := fp_exp(x);
           if e >= @EB@ then
              return fp_inf('0') & fp_inf(x(@WB@-1));
           elsif e <= -(@MB@/2+2) then
              return fx2fp(ONE, 0) & x;
           end if;
           xa := abs(fp2fx(x));
           k  := fx_expk(xa);
           r  := fx_expr(xa);
           hr := fx_hyprot(r);
           sh := signed(hr(2*@FW@-1 downto @FW@));
           ch := signed(hr(@FW@-1 downto 0));
           if k = 0 then
              sinhf := fx2fp(sh, 0);
              coshf := fx2fp(ch, 0);
           else
              a := fx2fp(ch + sh, k - 1);
              b := fx2fp(ch - sh, -k - 1);
              sinhf := fp_add(a, b, '1');
              coshf := fp_add(a, b, '0');
           end if;
           sinhf(@WB@-1) := x(@WB@-1);
           return coshf & sinhf;
        end function;
        """,
        """
        function [2*@WB@-1:0] fm_sinhcosh;
           input [@WB@-1:0] x;
           integer e, k;
           reg signed [@FW@-1:0] xa, r, sh, ch;
           reg [2*@FW@-1:0] hr;
           reg [@WB@-1:0] a, b, sinhf, coshf;
           begin
              if (fp_isnan(x)) fm_sinhcosh = {NAN_V, NAN_V};
              else if (fp_isinf(x)) fm_sinhcosh = {fp_inf(1'b0), x};
              else if (fp_iszero(x)) fm_sinhcosh = {fx2fp(ONE, 0), x};
              else begin
                 e = fp_exp(x);
                 if (e >= @EB@) fm_sinhcosh = {fp_inf(1'b0), fp_inf(x[@WB@-1])};
                 else if (e <= -(@MB@/2+2)) fm_sinhcosh = {fx2fp(ONE, 0), x};
                 else begin
                    xa = fp2fx(x);
                    if (xa < 0) xa = -xa;
                    k = fx_expk(xa);
                    r = fx_expr(xa);
                    hr = fx_hyprot(r);
                    sh = hr[2*@FW@-1:@FW@];
                    ch = hr[@FW@-1:0];
                    if (k == 0) begin
                       sinhf = fx2fp(sh, 0);
                       coshf = fx2fp(ch, 0);
                    end else begin
                       a = fx2fp(ch + sh, k - 1);
                       b = fx2fp(ch - sh, -k - 1);
                       sinhf = fp_add(a, b, 1'b1);
                       coshf = fp_add(a, b, 1'b0);
                    end
                    sinhf[@WB@-1] = x[@WB@-1];
                    fm_sinhcosh = {coshf, sinhf};
                 end
              end
           end
        endfunction
        """);
    return this;
  }

  /** {@code fm_tanh(x)}; also needs a divider ({@link FpHdl#addDiv()}). */
  FpCordic addTanh() {
    addSinhCosh();
    hdl.addDiv();
    if (!once("tanh")) return this;
    declare(
        """
        function fm_tanh(x : std_logic_vector(@WB@-1 downto 0)) return std_logic_vector is
           variable sc  : std_logic_vector(2*@WB@-1 downto 0);
           variable res : std_logic_vector(@WB@-1 downto 0);
        begin
           if fp_isnan(x) then
              return fp_nan;
           elsif fp_isinf(x) or fp_exp(x) >= @CLAMP@ then
              res := fx2fp(ONE, 0);
              res(@WB@-1) := x(@WB@-1);
              return res;
           end if;
           sc := fm_sinhcosh(x);
           return fp_div(sc(@WB@-1 downto 0), sc(2*@WB@-1 downto @WB@));
        end function;
        """,
        """
        function [@WB@-1:0] fm_tanh;
           input [@WB@-1:0] x;
           reg [2*@WB@-1:0] sc;
           reg [@WB@-1:0] res, sinhf, coshf;
           integer e;
           begin
              e = fp_exp(x);
              if (fp_isnan(x)) fm_tanh = NAN_V;
              else if (fp_isinf(x) || (!fp_iszero(x) && e >= @CLAMP@)) begin
                 res = fx2fp(ONE, 0);
                 res[@WB@-1] = x[@WB@-1];
                 fm_tanh = res;
              end else begin
                 sc = fm_sinhcosh(x);
                 sinhf = sc[@WB@-1:0];
                 coshf = sc[2*@WB@-1:@WB@];
                 fm_tanh = fp_div(sinhf, coshf);
              end
           end
        endfunction
        """);
    return this;
  }
}
