package de.venari.portfolio.derivatives;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;

public final class AddonMultiplier
{
    public static final String PROPERTY_NAME = "derivatives-addon.multiplier"; //$NON-NLS-1$

    private AddonMultiplier()
    {
    }

    public static BigDecimal get(Security security)
    {
        String history = security.getPropertyValue(SecurityProperty.Type.FEED,
                        "derivatives-addon.multiplierHistory").orElse(null); //$NON-NLS-1$
        if (history != null)
            return DatedValueSeries.valueAt(history, LocalDate.now()).orElse(BigDecimal.ONE);

        return security.getPropertyValue(SecurityProperty.Type.FEED, PROPERTY_NAME)
                        .map(AddonMultiplier::parse)
                        .orElse(BigDecimal.ONE);
    }

    public static boolean set(Security security, BigDecimal multiplier)
    {
        BigDecimal normalized = multiplier.stripTrailingZeros();

        if (normalized.signum() <= 0)
            throw new IllegalArgumentException("Multiplier must be greater than zero"); //$NON-NLS-1$

        return security.setPropertyValue(SecurityProperty.Type.FEED, PROPERTY_NAME, normalized.toPlainString());
    }

    private static BigDecimal parse(String value)
    {
        try
        {
            BigDecimal answer = new BigDecimal(value);
            return answer.signum() > 0 ? answer : BigDecimal.ONE;
        }
        catch (NumberFormatException ignore)
        {
            return BigDecimal.ONE;
        }
    }
}
