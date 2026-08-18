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

/**
 * Shared calculation of economic exposure for reporting and statement-of-assets
 * columns.
 */
public final class ExposureCalculator
{
    public enum ExposureType
    {
        MARKET_VALUE, NOTIONAL, DELTA_ADJUSTED
    }

    public static final String OPTION_PRODUCT_TYPE = "optionProductType"; //$NON-NLS-1$
    public static final String KNOCK_OUT_CERTIFICATE = "KNOCK_OUT_CERTIFICATE"; //$NON-NLS-1$
    public static final String FX_UNDERLYING = "fxUnderlying"; //$NON-NLS-1$
    public static final String FX_BASE_CURRENCY = "fxBaseCurrency"; //$NON-NLS-1$
    public static final String FX_QUOTE_CURRENCY = "fxQuoteCurrency"; //$NON-NLS-1$
    public static final String SUBSCRIPTION_RATIO = "subscriptionRatio"; //$NON-NLS-1$
    public static final String ISSUER = "issuer"; //$NON-NLS-1$
    public static final String ISSUER_PRODUCT_ID = "issuerProductId"; //$NON-NLS-1$

    private ExposureCalculator()
    {
    }

    public static Money calculate(Client client, SecurityPosition position, LocalDate date,
                    CurrencyConverter converter, ExposureType exposureType)
    {
        Security security = position.getSecurity();
        if (security == null)
            return null;

        String derivativeType = DerivativePositionCalculator.getDerivativeType(security);

        if (exposureType == ExposureType.MARKET_VALUE)
        {
            if (DerivativePositionCalculator.OPTION.equals(derivativeType))
            {
                Money marketValue = DerivativePositionCalculator.valueOf(position.getShares(),
                                position.getPrice().getValue(), security.getMultiplier(date), security.getCurrencyCode());
                return converter.convert(date, marketValue);
            }

            return converter.convert(date, position.calculateValue(date));
        }

        if (derivativeType == null)
            return converter.convert(date, position.calculateValue(date));

        if (isFxKnockoutCertificate(security))
            return calculateFxKnockoutExposure(position, security, date, exposureType);

        long referenceValue;
        String exposureCurrency;

        if (DerivativePositionCalculator.FUTURE.equals(derivativeType) || isKnockoutCertificate(security))
        {
            Security underlying = DerivativePositionCalculator.resolveUnderlying(client, security);
            if (underlying == null)
                return null;

            referenceValue = underlying.getSecurityPrice(date).getValue();
            exposureCurrency = underlying.getCurrencyCode();
        }
        else if (DerivativePositionCalculator.OPTION.equals(derivativeType))
        {
            Long strikeValue = DerivativePositionCalculator.getOptionStrikeQuoteValue(security);
            if (strikeValue == null)
                return null;

            referenceValue = strikeValue.longValue();
            exposureCurrency = security.getCurrencyCode();
        }
        else
        {
            return converter.convert(date, position.calculateValue(date));
        }

        double factor = isKnockoutCertificate(security) ? getSubscriptionRatio(security, date)
                        : security.getMultiplier(date);
        if (!Double.isFinite(factor) || factor <= 0)
            return null;

        if (exposureType == ExposureType.DELTA_ADJUSTED)
            factor *= SecurityDelta.getDelta(security, date);
        else if (exposureType == ExposureType.NOTIONAL && DerivativePositionCalculator.OPTION.equals(derivativeType))
            factor *= SecurityDelta.getDefaultDelta(security);

        Money rawExposure = DerivativePositionCalculator.valueOf(position.getShares(), referenceValue, factor,
                        exposureCurrency);
        return converter.convert(date, rawExposure);
    }

    private static Money calculateFxKnockoutExposure(SecurityPosition position, Security security, LocalDate date,
                    ExposureType exposureType)
    {
        String baseCurrency = normalizeCurrency(
                        security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_BASE_CURRENCY).orElse(null));
        String quoteCurrency = normalizeCurrency(
                        security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_QUOTE_CURRENCY).orElse(null));

        if (baseCurrency == null || quoteCurrency == null || baseCurrency.equals(quoteCurrency))
            return null;

        double factor = getSubscriptionRatio(security, date);
        if (!Double.isFinite(factor) || factor <= 0)
            return null;

        if (exposureType == ExposureType.DELTA_ADJUSTED)
            factor *= SecurityDelta.getDelta(security, date);
        else
            factor *= SecurityDelta.getDefaultDelta(security);

        return DerivativePositionCalculator.valueOf(position.getShares(), Values.Quote.factorize(1.0), factor,
                        baseCurrency);
    }

    private static double getSubscriptionRatio(Security security, LocalDate date)
    {
        String value = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, SUBSCRIPTION_RATIO).orElse(null);
        if (value != null && !value.isBlank())
        {
            try
            {
                double parsed = Double.parseDouble(value.trim().replace(',', '.'));
                if (Double.isFinite(parsed) && parsed > 0)
                    return parsed;
            }
            catch (NumberFormatException e)
            {
                // Fall back to the historical multiplier for legacy or malformed data.
            }
        }

        return security.getMultiplier(date);
    }

    private static String normalizeCurrency(String value)
    {
        if (value == null)
            return null;

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{3}") ? normalized : null; //$NON-NLS-1$
    }

    public static boolean isKnockoutCertificate(Security security)
    {
        return KNOCK_OUT_CERTIFICATE.equals(security
                        .getPropertyValue(SecurityProperty.Type.DERIVATIVE, OPTION_PRODUCT_TYPE).orElse(null));
    }

    public static boolean isFxKnockoutCertificate(Security security)
    {
        return isKnockoutCertificate(security) && Boolean.parseBoolean(
                        security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, FX_UNDERLYING).orElse("false"));
    }
}
