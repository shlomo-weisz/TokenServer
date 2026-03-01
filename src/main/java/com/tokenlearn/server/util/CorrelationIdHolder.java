package com.tokenlearn.server.util;

public final class CorrelationIdHolder {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationIdHolder() {
    }

    public static void set(String value) {
        HOLDER.set(value);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
