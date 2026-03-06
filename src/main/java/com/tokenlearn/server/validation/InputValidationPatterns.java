package com.tokenlearn.server.validation;

public final class InputValidationPatterns {
    private InputValidationPatterns() {
    }

    public static final String NAME = "^[\\p{L}\\p{M}](?:[\\p{L}\\p{M}'\\-\\s]{0,48}[\\p{L}\\p{M}])?$";
    public static final String PHONE = "^\\+?[0-9][0-9()\\-\\s]{5,19}$";
    public static final String NO_HTML_TAGS = "^[^<>]*$";
    public static final String URL_HTTP_OR_BLOB = "^(?:|https?://[^\\s<>]+)$";
    public static final String TIME_24H = "^(?:[01]\\d|2[0-3]):[0-5]\\d$";
    public static final String TIME_OR_ISO_LOCAL_DATETIME = "^(?:(?:[01]\\d|2[0-3]):[0-5]\\d|\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?)$";
    public static final String WEEKDAY_EN = "^(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)$";
    public static final String WEEKDAY_EN_OR_HE = "^(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|ראשון|שני|שלישי|רביעי|חמישי|שישי|שבת)$";
    public static final String RESET_TOKEN = "^[A-Za-z0-9_-]{16,128}$";
    public static final String GOOGLE_ID_TOKEN = "^[A-Za-z0-9._\\-=]+$";
}
