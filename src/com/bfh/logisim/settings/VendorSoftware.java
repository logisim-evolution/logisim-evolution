package com.bfh.logisim.settings;

public class VendorSoftware {

    public char vendor;
    public String name;
    public String[] bin;

    public VendorSoftware(char vendor, String name, String[] bin) {
        this.vendor = vendor;
        this.name = name;
        this.bin = bin;
    }
}
