package me.khajiitos.servercountryflags.common.util;

public record APIResponse(Status status, LocationInfo locationInfo) {
    public boolean cooldown() {
        return status == Status.COOLDOWN;
    }

    public boolean unknown() {
        return status == Status.UNKNOWN;
    }

    public enum Status {
        SUCCESS,
        UNKNOWN,
        COOLDOWN
    }
}