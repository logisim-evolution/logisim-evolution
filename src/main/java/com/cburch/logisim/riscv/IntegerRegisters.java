package com.cburch.logisim.riscv;

public class IntegerRegisters {
    long x[] = new long[] {0,0,0,0,0,0,0,0,0,0,
                           0,0,0,0,0,0,0,0,0,0,
                           0,0,0,0,0,0,0,0,0,0,
                           0,0};

    public long get(int index) {
        return x[index];
    }

    public void set(int index, long value) {
        if(index != 0) {
            x[index] = value;
        }
    }
}
