package com.fireflytornado.mcupdate.javafx;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Loads the preset's own UI text for the operating system display language. */
final class Lang {

    private static final String BUNDLE_NAME = "lang.messages";
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(
            BUNDLE_NAME,
            supportedLocale(Locale.getDefault(Locale.Category.DISPLAY)),
            Lang.class.getClassLoader(),
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private Lang() {
    }

    static String text(String key, Object... arguments) {
        String pattern = MESSAGES.getString(key);
        return arguments.length == 0
                ? pattern
                : new MessageFormat(pattern, MESSAGES.getLocale()).format(arguments);
    }

    static Locale supportedLocale(Locale locale) {
        if (!"zh".equalsIgnoreCase(locale.getLanguage())) {
            return Locale.ROOT;
        }
        String country = locale.getCountry();
        if ("TW".equalsIgnoreCase(country)
                || "HK".equalsIgnoreCase(country)
                || "MO".equalsIgnoreCase(country)) {
            return Locale.TRADITIONAL_CHINESE;
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
