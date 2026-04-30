package localnews_backend.util;

public final class Helpers {

    private Helpers() {}

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String capitalize(String value) {
        if (isBlank(value)) return value;
        return value.substring(0, 1).toUpperCase()
                + (value.length() > 1 ? value.substring(1).toLowerCase() : "");
    }

    public static String sanitize(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }
}
