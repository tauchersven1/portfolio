package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses OCC-style option symbols as used by Interactive Brokers CSV exports. */
public final class InteractiveBrokersOptionSymbol
{
    private static final Pattern PATTERN = Pattern.compile("^([A-Za-z0-9.]+)(\\d{6})([CP])(\\d{8})$"); //$NON-NLS-1$

    public record Parsed(String tradingSymbol, LocalDate expirationDate, String putCall, BigDecimal strike)
    {
    }

    private InteractiveBrokersOptionSymbol()
    {
    }

    public static Optional<Parsed> parse(String symbol)
    {
        if (symbol == null)
            return Optional.empty();

        Matcher matcher = PATTERN.matcher(symbol.trim());
        if (!matcher.matches())
            return Optional.empty();

        try
        {
            String date = matcher.group(2);
            int year = 2000 + Integer.parseInt(date.substring(0, 2));
            int month = Integer.parseInt(date.substring(2, 4));
            int day = Integer.parseInt(date.substring(4, 6));
            LocalDate expiration = LocalDate.of(year, month, day);

            String putCall = "P".equals(matcher.group(3)) ? "PUT" : "CALL"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            BigDecimal strike = new BigDecimal(matcher.group(4)).movePointLeft(3).stripTrailingZeros();

            return Optional.of(new Parsed(matcher.group(1), expiration, putCall, strike));
        }
        catch (NumberFormatException | DateTimeException e)
        {
            return Optional.empty();
        }
    }
}
