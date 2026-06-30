package com.codesec.engineadapter;

public class EngineHealth {
    private boolean ok;
    private String engineVersion;
    private long scanCount;

    public EngineHealth() {}

    public EngineHealth(boolean ok, String engineVersion, long scanCount) {
        this.ok = ok;
        this.engineVersion = engineVersion;
        this.scanCount = scanCount;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public long getScanCount() { return scanCount; }
    public void setScanCount(long scanCount) { this.scanCount = scanCount; }
}
