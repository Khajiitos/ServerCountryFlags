package me.khajiitos.servercountryflags.common.util;

public enum FlagPosition {
    BEHIND_NAME("behindName"),
    LEFT("left"),
    RIGHT("right"),
    DEFAULT("default");

    private final String name;

    FlagPosition(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
