package name.abuchen.portfolio.snapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public final class DerivativePositionCalculator
{
    public static final String DERIVATIVE_TYPE = "type"; //$NON-NLS-1$
    public static final String UNDERLYING = "underlying"; //$NON-NLS-1$
    public static final String UNDERLYING_SECURITY_UUID = "underlyingSecurityUUID"; //$NON-NLS-1$
    public static final String STRIKE = "strike"; //$NON-NLS-1$
    public static final String OPTION = "OPTION"; //$NON-NLS-1$
    public static final String FUTURE = "FUTURE"; //$NON-NLS-1$

    private DerivativePositionCalculator()
    {
    }

    public static String getDerivativeType(Security security)
    {
        return security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, DERIVATIVE_TYPE).orElse(null);
    }

    public static boolean isOption(Security security)
    {
        return OPTION.equals(getDerivativeType(security));
    }

    public static boolean isFuture(Security security)
    {
        return FUTURE.equals(getDerivativeType(security));
    }

    public static Long getOptionStrikeQuoteValue(Security security)
    {
        String strike = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, STRIKE).orElse(null);
        if (strike == null || strike.isBlank())
            return null;

        try
        {
            double value = Double.parseDouble(strike.trim().replace(',', '.'));
            if (!Double.isFinite(value) || value < 0)
                return null;
            return Values.Quote.factorize(value);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public static Security resolveUnderlying(Client client, Security derivative)
    {
        String uuid = derivative.getPropertyValue(SecurityProperty.Type.DERIVATIVE, UNDERLYING_SECURITY_UUID)
                        .orElse(null);
        if (uuid != null && !uuid.isBlank())
        {
            Security byUUID = client.getSecurities().stream().filter(s -> uuid.equals(s.getUUID())).findFirst()
                            .orElse(null);
            if (byUUID != null)
                return byUUID;
        }

        String underlying = derivative.getPropertyValue(SecurityProperty.Type.DERIVATIVE, UNDERLYING).orElse(null);
        if (underlying == null || underlying.isBlank())
            return null;

        String search = underlying.trim();
        List<Security> matches = client.getSecurities().stream().filter(s -> s != derivative)
                        .filter(s -> matchesUnderlying(s, search)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static boolean matchesUnderlying(Security security, String search)
    {
        if (security.getName() != null && security.getName().equalsIgnoreCase(search))
            return true;

        String ticker = security.getTickerSymbol();
        if (ticker != null && ticker.equalsIgnoreCase(search))
            return true;

        if (ticker != null && security.getName() != null)
        {
            String label = security.getName() + " [" + ticker + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            if (label.equalsIgnoreCase(search))
                return true;
        }

        String normalizedSearch = normalizeUnderlyingName(search);
        String normalizedName = normalizeUnderlyingName(security.getName());
        return !normalizedSearch.isEmpty() && normalizedSearch.equals(normalizedName);
    }

    private static String normalizeUnderlyingName(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.toLowerCase(Locale.ROOT)
                        .replaceAll("\\[[^\\]]+\\]", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("\\bclass\\s+[a-z0-9]+\\b", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("\\bregistered\\s+shares?\\b", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("\\b(common|ordinary)\\s+(stock|shares?)\\b", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("\\b(incorporated|inc|corporation|corp|limited|ltd|plc|ag|se|nv|sa)\\b", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("\\bdl\\b", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .replaceAll("[^a-z0-9]+", " ") //$NON-NLS-1$ //$NON-NLS-2$
                        .trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static Money calculateMarketValue(Security security, long shares, SecurityPrice price,
                    List<PortfolioTransaction> transactions, CurrencyConverter converter, LocalDate valuationDate)
    {
        if (isOption(security))
            return valueOf(shares, price.getValue(), security.getMultiplier(valuationDate), security.getCurrencyCode());

        if (isFuture(security))
            return calculateFutureUnrealizedProfit(security, price, transactions, converter, valuationDate);

        return valueOf(shares, price.getValue(), 1.0, security.getCurrencyCode());
    }

    public static Money valueOf(long shares, long quoteValue, double multiplier, String currencyCode)
    {
        long value = BigDecimal.valueOf(shares)
                        .movePointLeft(Values.Share.precision())
                        .multiply(BigDecimal.valueOf(quoteValue), Values.MC)
                        .multiply(BigDecimal.valueOf(multiplier), Values.MC)
                        .movePointLeft(Values.Quote.precisionDeltaToMoney())
                        .setScale(0, RoundingMode.HALF_UP).longValue();
        return Money.of(currencyCode, value);
    }

    private static Money calculateFutureUnrealizedProfit(Security security, SecurityPrice currentPrice,
                    List<PortfolioTransaction> transactions, CurrencyConverter converter, LocalDate valuationDate)
    {
        OpenPosition open = calculateOpenPosition(security, transactions, converter);
        if (open.shares == 0)
            return Money.of(security.getCurrencyCode(), 0);

        BigDecimal priceDifference = BigDecimal.valueOf(currentPrice.getValue()).subtract(open.averagePrice);
        long unrealized = BigDecimal.valueOf(open.shares)
                        .movePointLeft(Values.Share.precision())
                        .multiply(priceDifference, Values.MC)
                        .multiply(BigDecimal.valueOf(security.getMultiplier(valuationDate)), Values.MC)
                        .movePointLeft(Values.Quote.precisionDeltaToMoney())
                        .setScale(0, RoundingMode.HALF_UP).longValue();

        return Money.of(security.getCurrencyCode(), unrealized);
    }

    private static OpenPosition calculateOpenPosition(Security security, List<PortfolioTransaction> transactions,
                    CurrencyConverter converter)
    {
        long openShares = 0;
        BigDecimal averagePrice = BigDecimal.ZERO;
        CurrencyConverter securityConverter = converter.with(security.getCurrencyCode());

        List<PortfolioTransaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparing(PortfolioTransaction::getDateTime));

        for (PortfolioTransaction transaction : sorted)
        {
            long transactionShares = signedShares(transaction);
            if (transactionShares == 0)
                continue;

            double transactionMultiplier = security.getMultiplier(transaction.getDateTime().toLocalDate());
            if (transactionMultiplier <= 0d)
                transactionMultiplier = 1d;

            BigDecimal transactionPrice = BigDecimal
                            .valueOf(transaction.getGrossPricePerShare(securityConverter).getAmount())
                            .multiply(BigDecimal.valueOf(transactionMultiplier), Values.MC);

            if (openShares == 0 || Long.signum(openShares) == Long.signum(transactionShares))
            {
                long oldAbsolute = Math.abs(openShares);
                long transactionAbsolute = Math.abs(transactionShares);
                long newAbsolute = oldAbsolute + transactionAbsolute;

                averagePrice = averagePrice.multiply(BigDecimal.valueOf(oldAbsolute), Values.MC)
                                .add(transactionPrice.multiply(BigDecimal.valueOf(transactionAbsolute), Values.MC))
                                .divide(BigDecimal.valueOf(newAbsolute), Values.MC);
                openShares += transactionShares;
            }
            else
            {
                long oldShares = openShares;
                openShares += transactionShares;

                if (openShares == 0)
                    averagePrice = BigDecimal.ZERO;
                else if (Long.signum(openShares) != Long.signum(oldShares))
                    averagePrice = transactionPrice;
            }
        }

        return new OpenPosition(openShares, averagePrice);
    }

    private static long signedShares(PortfolioTransaction transaction)
    {
        return transaction.getType().isPurchase() ? transaction.getShares() : -transaction.getShares();
    }

    private record OpenPosition(long shares, BigDecimal averagePrice)
    {
    }
}
