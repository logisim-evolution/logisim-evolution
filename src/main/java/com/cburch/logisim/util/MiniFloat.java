/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.util;

public class MiniFloat {
    // Constants from jdk.internal.math.FloatConsts
    // Number of bits in the significand (mantissa) including the implicit leading 1
    public static final int SIGNIFICAND_WIDTH = 24;
    // Exponent bias for single-precision (8-bit exponent field)
    public static final int EXP_BIAS = 127;

    /* Adapted from the Float.class, using the float16ToFloat() method as base.
     */
    public static float miniFloat143ToFloat(byte miniFloat) {
        /*
         * The miniFloat143 format has 1 sign bit, 4 exponent bits, and 3
         * significand bits. The exponent bias is 7.
         */
        int bin8arg = (int)miniFloat;
        int bin8SignBit     = 0x80 & bin8arg;
        int bin8ExpBits     = 0x78 & bin8arg;
        int bin8SignifBits  = 0x07 & bin8arg;

        // Shift left difference in the number of significand bits in
        // the float and miniFloat143 formats
        final int SIGNIF_SHIFT = (SIGNIFICAND_WIDTH - 4);

        float sign = (bin8SignBit != 0) ? -1.0f : 1.0f;

        // Extract miniFloat143 exponent, remove its bias, add in the bias
        // of a float exponent and shift to correct bit location
        // (significand width includes the implicit bit so shift one
        // less).
        int bin8Exp = (bin8ExpBits >> 3) - 7;
        if (bin8Exp == -7) {
            // For subnormal miniFloat143 values and 0, the numerical
            // value is 2^9 * the significand as an integer (no
            // implicit bit).
            return sign * (0x1p-9f * bin8SignifBits);
        } else if (bin8Exp == 8) {
            return (bin8SignifBits == 0) ?
                sign * Float.POSITIVE_INFINITY :
                Float.intBitsToFloat((bin8SignBit << 24) |
                                     0x7f80_0000 |
                                     // Preserve NaN signif bits
                                     ( bin8SignifBits << SIGNIF_SHIFT ));
        }

        assert -7 < bin8Exp  && bin8Exp < 8;

        int floatExpBits = (bin8Exp + EXP_BIAS)
            << (SIGNIFICAND_WIDTH - 1);

        // Compute and combine result sign, exponent, and significand bits.
        return Float.intBitsToFloat((bin8SignBit << 24) |
                                    floatExpBits |
                                    (bin8SignifBits << SIGNIF_SHIFT));
    }

    /* Adapted from the Float.class, using the floatToFloat16() method as base.
     */
    public static byte floatToMiniFloat143(float f) {
        int doppel = Float.floatToRawIntBits(f);
        byte sign_bit = (byte)((doppel & 0x8000_0000) >> 24);

        if (Float.isNaN(f)) {
            // Preserve sign and attempt to preserve significand bits
            return (byte)(sign_bit
                    | 0x78 // max exponent + 1 (4 bits exponent field)
                    | ((doppel & 0x0070_0000) >> 20) // preserve bits 22..20

            );
        }

        float abs_f = Math.abs(f);

        // The overflow threshold is miniFloat143 MAX_VALUE + 1/2 ulp
        if (abs_f >= (0x1.0p8f)) {
            return (byte)(sign_bit | 0x78); // Positive or negative infinity
        }

        // Smallest magnitude nonzero representable miniFloat143 value
        // is equal to 0x1.0p-9; half-way and smaller rounds to zero.
        if (abs_f <= 0x1.0p-9f * 0.5f) { // Covers float zeros and subnormals.
            return sign_bit; // Positive or negative zero
        }

        // Dealing with finite values in exponent range of miniFloat143
        int exp = Math.getExponent(f);
        assert -10 <= exp && exp <= 7;

        // For miniFloat143 subnormals, beside forcing exp to -7, retain
        // the difference expdelta = E_min - exp.  This is the excess
        // shift value, in addition to 21, to be used in the
        // computations below.  Further the (hidden) msb with value 1
        // in f must be involved as well.
        int expdelta = 0;
        int msb = 0x0000_0000;
        if (exp < -6) {
            expdelta = -6 - exp;
            exp = -7;
            msb = 0x0080_0000;
        }
        int f_signif_bits = doppel & 0x007f_ffff | msb;

        // Significand bits as if using rounding to zero (truncation).
        byte signif_bits = (byte)(f_signif_bits >> (20 + expdelta));

        // Round to nearest even
        int lsb    = f_signif_bits & (1 << (20 + expdelta));
        int round  = f_signif_bits & (1 << (19 + expdelta));
        int sticky = f_signif_bits & ((1 << (19 + expdelta)) - 1);

        if (round != 0 && ((lsb | sticky) != 0 )) {
            signif_bits++;
        }

        // No bits set in significand beyond the *first* exponent bit,
        // not just the significand; quantity is added to the exponent
        // to implement a carry out from rounding the significand.
        assert (0xf8 & signif_bits) == 0x0;

        return (byte)(sign_bit | (((exp + 7) << 3) + signif_bits));
    }
}
