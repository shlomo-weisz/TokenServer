package com.tokenlearn.server.util;

import java.util.Locale;
import java.util.Map;

public final class WeekdayUtil {
    private static final Map<String, String> EN_BY_LOWER = Map.of(
            "sunday", "Sunday",
            "monday", "Monday",
            "tuesday", "Tuesday",
            "wednesday", "Wednesday",
            "thursday", "Thursday",
            "friday", "Friday",
            "saturday", "Saturday");

    private static final Map<String, String> EN_BY_HE = Map.of(
            "ראשון", "Sunday",
            "שני", "Monday",
            "שלישי", "Tuesday",
            "רביעי", "Wednesday",
            "חמישי", "Thursday",
            "שישי", "Friday",
            "שבת", "Saturday");

    private WeekdayUtil() {
    }

    public static String normalizeToEnglishOrNull(String rawDay) {
        if (rawDay == null) {
            return null;
        }
        String day = rawDay.trim();
        if (day.isEmpty()) {
            return null;
        }

        String en = EN_BY_LOWER.get(day.toLowerCase(Locale.ROOT));
        if (en != null) {
            return en;
        }

        return EN_BY_HE.get(day);
    }
}
