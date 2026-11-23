package com.hokokrtet.rwguard.models;

public class ViolationRecord {
    private final String checkName;
    private final String details;
    private final long timestamp;
    private final int severity;

    public ViolationRecord(String checkName, String details, int severity) {
        this.checkName = checkName;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
        this.severity = severity;
    }

    public String getCheckName() {
        return checkName;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getSeverity() {
        return severity;
    }
}
