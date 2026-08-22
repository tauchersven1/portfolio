package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses OCC-style option symbols such as IAG260918C00017000.
 */
public final class OptionSymbolParser
{
    private static final Pattern OCC_SYMBOL = Pattern.compile("^([A-Z0-9.\\-]{1,10})(\\d{2})(\\d{2})(\\d{2})([CP])(\\d{8})$"); //$NON-NLS-1$

    public static final class OptionData
    {
        private final String underlying;
        private final LocalDate expirationDate;
        private final String putCall;
        private final BigDecimal strike;

        private OptionData(String underlying, LocalDate expirationDate, String putCall, BigDecimal strike)
        {
            this.underlying = underlying;
            this.expirationDate = expirationDate;
            this.putCall = putCall;
            this.strike = strike;
        }

        public String getUnderlying()
        {
            return underlying;
        }

        public LocalDate getExpirationDate()
        {
            return expirationDate;
        }

        public String getPutCall()
        {
            return putCall;
        }

        public BigDecimal getStrike()
        {
            return strike;
        }
    }

    private OptionSymbolParser()
    {
    }

    public static Optional<OptionData> parse(String symbol)
    {
        if (symbol == null)
            return Optional.empty();

        String normalized = symbol.trim().toUpperCase();
        Matcher matcher = OCC_SYMBOL.matcher(normalized);
        if (!matcher.matches())
            return Optional.empty();

        try
        {
            int year = 2000 + Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            int day = Integer.parseInt(matcher.group(4));
            LocalDate expirationDate = LocalDate.of(year, month, day);

            BigDecimal strike = new BigDecimal(matcher.group(6)).movePointLeft(3).stripTrailingZeros();
            String putCall = "C".equals(matcher.group(5)) ? "CALL" : "PUT"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            return Optional.of(new OptionData(matcher.group(1), expirationDate, putCall, strike));
        }
        catch (DateTimeException | NumberFormatException e)
        {
            return Optional.empty();
        }
    }

    /**
     * Populates missing derivative master data from the security ticker symbol.
     * Existing values always win.
     */
    public static boolean populateMissingDerivativeProperties(Security security)
    {
        Optional<OptionData> parsed = parse(security.getTickerSymbol());
        if (parsed.isEmpty())
            return false;

        OptionData data = parsed.get();
        boolean changed = false;
        changed |= setIfMissing(security, "type", "OPTION"); //$NON-NLS-1$ //$NON-NLS-2$
        changed |= setIfMissing(security, "underlying", data.getUnderlying()); //$NON-NLS-1$
        changed |= setIfMissing(security, "expirationDate", data.getExpirationDate().toString()); //$NON-NLS-1$
        changed |= setIfMissing(security, "putCall", data.getPutCall()); //$NON-NLS-1$
        changed |= setIfMissing(security, "strike", data.getStrike().toPlainString()); //$NON-NLS-1$
        changed |= setIfMissing(security, "contractSymbol", security.getTickerSymbol().trim()); //$NON-NLS-1$
        return changed;
    }

    private static boolean setIfMissing(Security security, String name, String value)
    {
        if (security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, name).filter(v -> !v.isBlank()).isPresent())
            return false;

        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, name, value);
        return true;
    }
}
