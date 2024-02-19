package me.khajiitos.servercountryflags.common.util;

public enum FlagPosition {
    BEHIND_NAME("behindName"),
    LEFT("left"),
    RIGHT("right"),
    BOTTOM_RIGHT("bottomRight"),
    TOOLTIP_SERVER_NAME("tooltipServerName"),
    TOOLTIP_PING("tooltipPing");

    private final String name;

    FlagPosition(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
