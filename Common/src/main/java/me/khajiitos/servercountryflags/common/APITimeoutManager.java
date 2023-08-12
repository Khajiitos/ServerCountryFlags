package me.khajiitos.servercountryflags.common;

public class APITimeoutManager {
    private static int requestsLeft = 45;
    private static long cooldownReset = -1;
    private static int requestsSent = 0;

    public static boolean isOnCooldown() {
        return requestsLeft <= 0 && System.currentTimeMillis() < cooldownReset;
    }

    public static int getRequestsSent() {
        return requestsSent;
    }

    public static void incrementRequestsSent() {
        requestsSent++;
    }

    public static void decrementRequestsSent() {
        requestsSent--;
    }

    public static void setRequestsLeft(int apiRequestsLeft) {
        requestsLeft = apiRequestsLeft;
    }

    public static void setSecondsLeftUntilReset(int apiSecondsLeft) {
        cooldownReset = System.currentTimeMillis() + apiSecondsLeft * 1000L;
    }
}