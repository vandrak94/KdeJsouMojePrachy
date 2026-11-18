import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Jednotné české formátování názvů měsíců s počátečním velkým písmenem. */
public final class MonthNameFormatter {
    private static final Locale CZECH = Locale.forLanguageTag("cs-CZ");
    private MonthNameFormatter() { }

    public static String format(LocalDate date, String pattern) {
        String text = date.format(DateTimeFormatter.ofPattern(pattern, CZECH));
        if (text.isEmpty()) return text;
        int first = text.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toTitleCase(first))
                .append(text.substring(Character.charCount(first))).toString();
    }
}
