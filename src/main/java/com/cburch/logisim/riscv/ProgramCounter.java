package com.cburch.logisim.riscv;

public class ProgramCounter {
    long value;

    public ProgramCounter(long value) {
        set(value);
    }

    public void increment() {
        value = (value + 4) & 0xffffffff;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

}
