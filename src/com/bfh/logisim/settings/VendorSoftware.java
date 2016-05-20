package com.bfh.logisim.settings;

import java.io.File;

public class VendorSoftware {

    private char vendor;
    private String name;
    private String[] bin;
    private String toolPath = "Unknown";

    public VendorSoftware(char vendor, String name, String[] bin) {
        this.vendor = vendor;
        this.name = name;
        this.bin = bin;
    }

    public void setToolPath(String path) {
        toolPath = path;
    }

    public String getToolPath() {
        return toolPath;
    }

    public String getName() {
        return name;
    }

    public char getVendor() {
        return vendor;
    }

    public String[] getBinaries() {
        return bin;
    }

    public String getBinaryPath(int binPos) {
        return toolPath + File.separator + bin[binPos];
    }
}
