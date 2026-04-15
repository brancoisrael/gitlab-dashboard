package br.com.ib.gitdashboards.i18n;

import jakarta.enterprise.context.ApplicationScoped;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@ApplicationScoped
public class MessageService {

    private static final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");
    private static final String BUNDLE_NAME = "messages";

    public String get(String key, String acceptLanguage) {
        var locale = resolverLocale(acceptLanguage);
        try {
            var bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /** Variante com substituição de parâmetros via {@link MessageFormat} (ex.: {@code {0}}, {@code {1}}). */
    public String get(String key, String acceptLanguage, Object... args) {
        var template = get(key, acceptLanguage);
        return args.length > 0 ? MessageFormat.format(template, args) : template;
    }

    private Locale resolverLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT_LOCALE;
        }
        var tag = acceptLanguage.split(",")[0].split(";")[0].trim();
        var locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? DEFAULT_LOCALE : locale;
    }
}
