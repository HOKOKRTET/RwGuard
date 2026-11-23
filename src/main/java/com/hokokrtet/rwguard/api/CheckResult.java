package com.hokokrtet.rwguard.api;

public class CheckResult {
    private static final CheckResult ALLOW_INSTANCE = new CheckResult(true, PacketAction.ALLOW, null, 0);

    private final boolean passed;
    private final PacketAction action;
    private final String reason;
    private final int severity;

    public CheckResult(boolean passed, PacketAction action, String reason, int severity) {
        this.passed = passed;
        this.action = action;
        this.reason = reason;
        this.severity = severity;
    }

    public CheckResult(PacketAction action, String reason, int severity) {
        this(action == PacketAction.ALLOW, action, reason, severity);
    }

    public static CheckResult passed() {
        return ALLOW_INSTANCE;
    }

    public static CheckResult allow() {
        return ALLOW_INSTANCE;
    }

    public static CheckResult cancel(String reason) {
        return new CheckResult(false, PacketAction.CANCEL, reason, 1);
    }

    public static CheckResult cancel(String reason, int severity) {
        return new CheckResult(false, PacketAction.CANCEL, reason, severity);
    }

    public static CheckResult drop(String reason) {
        return new CheckResult(false, PacketAction.DROP, reason, 2);
    }

    public static CheckResult drop(String reason, int severity) {
        return new CheckResult(false, PacketAction.DROP, reason, severity);
    }

    public static CheckResult kick(String reason) {
        return new CheckResult(false, PacketAction.KICK, reason, 5);
    }

    public static CheckResult kick(String reason, int severity) {
        return new CheckResult(false, PacketAction.KICK, reason, severity);
    }

    public static CheckResult action(PacketAction action, String reason, int severity) {
        return new CheckResult(action == PacketAction.ALLOW, action, reason, severity);
    }

    public boolean isPassed() {
        return passed;
    }

    public PacketAction getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public int getSeverity() {
        return severity;
    }
}
