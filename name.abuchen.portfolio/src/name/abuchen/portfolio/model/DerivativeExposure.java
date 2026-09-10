package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;

/**
 * Resolves derivative master data and calculates delta-adjusted exposure.
 */
public final class DerivativeExposure
{
    public static final String PREFIX = "derivatives-addon."; //$NON-NLS-1$

    public record Result(Money gross, Money net, Money notional, BigDecimal multiplier, BigDecimal delta,
                    BigDecimal leverage, BigDecimal knockOutLevel)
    {
    }

    private DerivativeExposure()
    {
    }

    public static Result calculate(Client client, AssetPosition position, LocalDate date)
    {
        Security security = position != null ? position.getSecurity() : null;
        if (security == null)
            return null;

        Money marketValue = position.getValuation();
        BigDecimal multiplier = SecurityMultiplier.valueAt(security, date);
        BigDecimal delta = valueAt(security, "deltaHistory", date, BigDecimal.ONE); //$NON-NLS-1$
        BigDecimal knockOutLevel = valueAt(security, "knockOutLevelHistory", date, null); //$NON-NLS-1$
        BigDecimal leverage = null;
        String instrumentType = property(security, "instrumentType"); //$NON-NLS-1$
        boolean put = "PUT".equalsIgnoreCase(property(security, "putCall")); //$NON-NLS-1$ //$NON-NLS-2$
        BigDecimal directionalDelta = put ? delta.abs().negate() : delta.abs();
        BigDecimal notionalFactor = multiplier;
        BigDecimal factor;

        if ("OPTION".equalsIgnoreCase(instrumentType)) //$NON-NLS-1$
        {
            BigDecimal strike = decimalProperty(security, "strike", BigDecimal.ONE); //$NON-NLS-1$
            notionalFactor = notionalFactor.multiply(strike, Values.MC);
            BigDecimal optionPrice = BigDecimal.valueOf(position.getPosition().getPrice().getValue())
                            .movePointLeft(Values.Quote.precision());
            if (optionPrice.signum() == 0)
                return null;
            notionalFactor = notionalFactor.divide(optionPrice, Values.MC);
            factor = notionalFactor.multiply(directionalDelta, Values.MC);
        }
        else
        {
            factor = multiplier.multiply(directionalDelta, Values.MC);
        }

        if ("KNOCK_OUT_CERTIFICATE".equalsIgnoreCase(instrumentType)) //$NON-NLS-1$
        {
            leverage = calculateLeverage(client, security, knockOutLevel, date);
            factor = leverage != null ? put ? leverage.negate() : leverage : BigDecimal.ZERO;
        }

        Money net = marketValue.multiplyAndRound(factor.doubleValue());
        Money gross = Money.of(net.getCurrencyCode(), Math.abs(net.getAmount()));
        Money notional = "KNOCK_OUT_CERTIFICATE".equalsIgnoreCase(instrumentType) ? null //$NON-NLS-1$
                        : marketValue.multiplyAndRound(notionalFactor.doubleValue());
        return new Result(gross, net, notional, multiplier, delta, leverage, knockOutLevel);
    }

    public static BigDecimal valueAt(Security security, String historyName, LocalDate date, BigDecimal defaultValue)
    {
        String encoded = property(security, historyName);
        if (encoded == null || date == null)
            return defaultValue;

        return encoded.lines().flatMap(line -> java.util.Arrays.stream(line.split(";"))) //$NON-NLS-1$
                        .map(DerivativeExposure::parseEntry).filter(java.util.Objects::nonNull)
                        .filter(entry -> !entry.date().isAfter(date)).max(Comparator.comparing(Entry::date))
                        .map(Entry::value).orElse(defaultValue);
    }

    public static String property(Security security, String name)
    {
        if (security == null)
            return null;
        return security.getPropertyValue(SecurityProperty.Type.FEED, PREFIX + name).orElse(null);
    }

    private static BigDecimal decimalProperty(Security security, String name, BigDecimal defaultValue)
    {
        String value = property(security, name);
        if (value == null || value.isBlank())
            return defaultValue;
        try
        {
            return new BigDecimal(value.trim().replace(',', '.'));
        }
        catch (NumberFormatException ignore)
        {
            return defaultValue;
        }
    }

    private static BigDecimal calculateLeverage(Client client, Security certificate, BigDecimal knockOutLevel,
                    LocalDate date)
    {
        if (client == null || knockOutLevel == null || knockOutLevel.signum() <= 0)
            return null;

        Security underlying = resolveUnderlying(client, property(certificate, "underlying")); //$NON-NLS-1$
        SecurityPrice price = underlying != null ? underlying.getSecurityPrice(date) : null;
        if (price == null || price.getValue() <= 0)
            return null;

        BigDecimal underlyingPrice = BigDecimal.valueOf(price.getValue()).movePointLeft(Values.Quote.precision());
        boolean shortKnockOut = "PUT".equalsIgnoreCase(property(certificate, "putCall")); //$NON-NLS-1$ //$NON-NLS-2$
        BigDecimal denominator = shortKnockOut
                        ? knockOutLevel.divide(underlyingPrice, Values.MC).subtract(BigDecimal.ONE)
                        : underlyingPrice.divide(knockOutLevel, Values.MC).subtract(BigDecimal.ONE);
        if (denominator.signum() <= 0)
            return null;

        return BigDecimal.ONE.divide(denominator, 12, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static Security resolveUnderlying(Client client, String reference)
    {
        if (reference == null || reference.isBlank())
            return null;
        return client.getSecurities().stream()
                        .filter(security -> reference.equals(security.getUUID())
                                        || reference.equalsIgnoreCase(security.getTickerSymbol())
                                        || reference.equalsIgnoreCase(security.getIsin())
                                        || reference.equalsIgnoreCase(security.getName()))
                        .findFirst().orElse(null);
    }

    private static Entry parseEntry(String token)
    {
        String[] parts = token.split("=", 2); //$NON-NLS-1$
        if (parts.length != 2)
            return null;
        try
        {
            return new Entry(LocalDate.parse(parts[0].trim()), new BigDecimal(parts[1].trim()));
        }
        catch (RuntimeException ignore)
        {
            return null;
        }
    }

    private record Entry(LocalDate date, BigDecimal value)
    {
    }
}
