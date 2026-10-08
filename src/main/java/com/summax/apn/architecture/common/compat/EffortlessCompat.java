package com.summax.apn.architecture.common.compat;

/**
 * Tracks when Effortless Building is working out or placing a block, so shapes only change their behaviour for it.
 */
public final class EffortlessCompat {

    // Each flag is only touched from one thread: state lookups on the client, placement on the server.
    private static int findingState, placing;

    private EffortlessCompat() {
    }

    public static void beginFindState() {
        findingState++;
    }

    public static void endFindState() {
        findingState = Math.max(0, findingState - 1);
    }

    public static boolean isFindingState() {
        return findingState > 0;
    }

    public static void beginPlace() {
        placing++;
    }

    public static void endPlace() {
        placing = Math.max(0, placing - 1);
    }

    public static boolean isPlacing() {
        return placing > 0;
    }
}
