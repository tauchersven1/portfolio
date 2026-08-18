package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;

@SuppressWarnings("nls")
public class ExposureCalculatorKnockoutRatioTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 8, 18);

    @Test
    public void testExplicitSubscriptionRatioOverridesMultiplierForNonFxKnockout()
    {
        Client client = new Client();
        Security underlying = new Security("Underlying", "EUR");
        underlying.addPrice(new SecurityPrice(DATE, Values.Quote.factorize(125.0)));
        client.addSecurity(underlying);

        Security certificate = certificate(underlying, "0.1");
        client.addSecurity(certificate);

        Money exposure = ExposureCalculator.calculate(client, position(certificate, 2), DATE,
                        new TestCurrencyConverter(), ExposureType.NOTIONAL);

        assertThat(exposure, is(Money.of("EUR", Values.Amount.factorize(25.0))));
    }

    @Test
    public void testMissingSubscriptionRatioKeepsLegacyMultiplierFallbackForNonFxKnockout()
    {
        Client client = new Client();
        Security underlying = new Security("Underlying", "EUR");
        underlying.addPrice(new SecurityPrice(DATE, Values.Quote.factorize(125.0)));
        client.addSecurity(underlying);

        Security certificate = certificate(underlying, null);
        client.addSecurity(certificate);

        Money exposure = ExposureCalculator.calculate(client, position(certificate, 2), DATE,
                        new TestCurrencyConverter(), ExposureType.NOTIONAL);

        assertThat(exposure, is(Money.of("EUR", Values.Amount.factorize(2500.0))));
    }

    private Security certificate(Security underlying, String subscriptionRatio)
    {
        Security certificate = new Security("Stock K.O.", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall", "CALL");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.OPTION_PRODUCT_TYPE,
                        ExposureCalculator.KNOCK_OUT_CERTIFICATE);
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlyingSecurityUUID", underlying.getUUID());
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.SUBSCRIPTION_RATIO,
                        subscriptionRatio);
        certificate.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 10.0));
        return certificate;
    }

    private SecurityPosition position(Security security, long quantity)
    {
        PortfolioTransaction transaction = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        transaction.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        transaction.setSecurity(security);
        transaction.setCurrencyCode("EUR");
        transaction.setShares(Values.Share.factorize(quantity));
        transaction.setAmount(Values.Amount.factorize(quantity * 4.0));

        return new SecurityPosition(security, new TestCurrencyConverter(),
                        new SecurityPrice(DATE, Values.Quote.factorize(4.0)), List.of(transaction));
    }
}
