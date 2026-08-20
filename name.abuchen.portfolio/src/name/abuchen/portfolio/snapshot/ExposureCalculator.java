package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.Locale;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityDelta;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public final class ExposureCalculator
{
    public enum ExposureType
    {
        MARKET_VALUE, NOTIONAL, DELTA_ADJUSTED
    }

    public static final String OPTION_PRODUCT_TYPE = "optionProductType";
    public static final String KNOCK_OUT_CERTIFICATE = "KNOCK_OUT_CERTIFICATE";
    public static final String FX_UNDERLYING = "fxUnderlying";
    public static final String FX_BASE_CURRENCY = "fxBaseCurrency";
    public static final String FX_QUOTE_CURRENCY = "fxQuoteCurrency";
    public static final String FX_EXPOSURE_CURRENCY = "fxExposureCurrency";
    public static final String ISSUER_UNDERLYING_PRICE = "issuerUnderlyingPrice";
    public static final String ISSUER_UNDERLYING_CURRENCY = "issuerUnderlyingCurrency";
    public static final String SUBSCRIPTION_RATIO = "subscriptionRatio";
    public static final String ISSUER = "issuer";
    public static final String ISSUER_PRODUCT_ID = "issuerProductId";

    private ExposureCalculator()
    {
    }

    public static Money calculate(Client client, SecurityPosition position, LocalDate date,
                    CurrencyConverter converter, ExposureType exposureType)
    {
        Security security = position.getSecurity();
        if (security == null) return null;
        String derivativeType = DerivativePositionCalculator.getDerivativeType(security);

        if (exposureType == ExposureType.MARKET_VALUE)
        {
            if (DerivativePositionCalculator.OPTION.equals(derivativeType))
                return converter.convert(date, DerivativePositionCalculator.valueOf(position.getShares(),
                                position.getPrice().getValue(), security.getMultiplier(date), security.getCurrencyCode()));
            return converter.convert(date, position.calculateValue(date));
        }
        if (derivativeType == null) return converter.convert(date, position.calculateValue(date));

        if (isFxKnockoutCertificate(security))
        {
            Money raw = calculateFxKnockoutExposure(position, security, date, exposureType, converter);
            return raw == null ? null : converter.convert(date, raw);
        }

        long referenceValue;
        String exposureCurrency;
        if (DerivativePositionCalculator.FUTURE.equals(derivativeType) || isKnockoutCertificate(security))
        {
            Security underlying = DerivativePositionCalculator.resolveUnderlying(client, security);
            if (underlying != null)
            {
                referenceValue = underlying.getSecurityPrice(date).getValue();
                exposureCurrency = underlying.getCurrencyCode();
            }
            else if (isKnockoutCertificate(security))
            {
                Double fallbackPrice = getPositiveDecimal(security, ISSUER_UNDERLYING_PRICE);
                exposureCurrency = normalizeCurrency(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE,
                                ISSUER_UNDERLYING_CURRENCY).orElse(null));
                if (fallbackPrice == null || exposureCurrency == null) return null;
                referenceValue = Values.Quote.factorize(fallbackPrice);
            }
            else return null;
        }
        else if (DerivativePositionCalculator.OPTION.equals(derivativeType))
        {
            Long strikeValue = DerivativePositionCalculator.getOptionStrikeQuoteValue(security);
            if (strikeValue == null) return null;
            referenceValue = strikeValue;
            exposureCurrency = security.getCurrencyCode();
        }
        else return converter.convert(date, position.calculateValue(date));

        double factor = isKnockoutCertificate(security) ? getSubscriptionRatio(security, date) : security.getMultiplier(date);
        if (!Double.isFinite(factor) || factor <= 0) return null;
        if (exposureType == ExposureType.DELTA_ADJUSTED) factor *= SecurityDelta.getDelta(security, date);
        else if (exposureType == ExposureType.NOTIONAL && DerivativePositionCalculator.OPTION.equals(derivativeType))
            factor *= SecurityDelta.getDefaultDelta(security);

        return converter.convert(date, DerivativePositionCalculator.valueOf(position.getShares(), referenceValue,
                        factor, exposureCurrency));
    }

    private static Money calculateFxKnockoutExposure(SecurityPosition position, Security security, LocalDate date,
                    ExposureType exposureType, CurrencyConverter converter)
    {
        String base = normalizeCurrency(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_BASE_CURRENCY).orElse(null));
        String quote = normalizeCurrency(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_QUOTE_CURRENCY).orElse(null));
        if (base == null || quote == null || base.equals(quote)) return null;
        double factor = getSubscriptionRatio(security, date);
        if (!Double.isFinite(factor) || factor <= 0) return null;
        factor *= exposureType == ExposureType.DELTA_ADJUSTED ? SecurityDelta.getDelta(security, date)
                        : SecurityDelta.getDefaultDelta(security);
        Money baseExposure = DerivativePositionCalculator.valueOf(position.getShares(), Values.Quote.factorize(1.0), factor, base);
        String display = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_EXPOSURE_CURRENCY).orElse("BASE");
        if (!"QUOTE".equalsIgnoreCase(display)) return baseExposure;
        CurrencyConverter quoteConverter = converter.with(quote);
        return quoteConverter == null ? null : quoteConverter.convert(date, baseExposure);
    }

    private static Double getPositiveDecimal(Security security, String key)
    {
        String value = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, key).orElse(null);
        if (value == null || value.isBlank()) return null;
        try
        {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return Double.isFinite(parsed) && parsed > 0 ? parsed : null;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static double getSubscriptionRatio(Security security, LocalDate date)
    {
        Double value = getPositiveDecimal(security, SUBSCRIPTION_RATIO);
        return value != null ? value : security.getMultiplier(date);
    }

    private static String normalizeCurrency(String value)
    {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{3}") ? normalized : null;
    }

    public static boolean isKnockoutCertificate(Security security)
    {
        return KNOCK_OUT_CERTIFICATE.equals(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, OPTION_PRODUCT_TYPE).orElse(null));
    }

    public static boolean isFxKnockoutCertificate(Security security)
    {
        return isKnockoutCertificate(security) && Boolean.parseBoolean(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_UNDERLYING).orElse("false"));
    }
}
