package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Resolves the derivative contract multiplier stored by the derivatives add-on.
 */
public final class SecurityMultiplier
{
    public static final String PROPERTY_NAME = "derivatives-addon.multiplier"; //$NON-NLS-1$
    public static final String HISTORY_PROPERTY_NAME = "derivatives-addon.multiplierHistory"; //$NON-NLS-1$

    private SecurityMultiplier()
    {
    }

    public static BigDecimal valueAt(Security security, LocalDate date)
    {
        if (security == null || date == null)
            return BigDecimal.ONE;

        var history = security.getPropertyValue(SecurityProperty.Type.FEED, HISTORY_PROPERTY_NAME).orElse(null);
        if (history != null)
        {
            var value = history.lines().flatMap(line -> java.util.Arrays.stream(line.split(";"))) //$NON-NLS-1$
                            .map(SecurityMultiplier::parseEntry).filter(java.util.Objects::nonNull)
                            .filter(entry -> !entry.date().isAfter(date)).max(Comparator.comparing(Entry::date))
                            .map(Entry::value);
            return value.orElse(BigDecimal.ONE);
        }

        return security.getPropertyValue(SecurityProperty.Type.FEED, PROPERTY_NAME)
                        .map(SecurityMultiplier::parseValue).orElse(BigDecimal.ONE);
    }

    private static Entry parseEntry(String token)
    {
        String[] parts = token.split("=", 2); //$NON-NLS-1$
        if (parts.length != 2)
            return null;
        try
        {
            BigDecimal value = new BigDecimal(parts[1].trim());
            return value.signum() > 0 ? new Entry(LocalDate.parse(parts[0].trim()), value.stripTrailingZeros()) : null;
        }
        catch (RuntimeException ignore)
        {
            return null;
        }
    }

    private static BigDecimal parseValue(String value)
    {
        try
        {
            BigDecimal parsed = new BigDecimal(value.trim());
            return parsed.signum() > 0 ? parsed.stripTrailingZeros() : BigDecimal.ONE;
        }
        catch (RuntimeException ignore)
        {
            return BigDecimal.ONE;
        }
    }

    private record Entry(LocalDate date, BigDecimal value)
    {
    }
}
