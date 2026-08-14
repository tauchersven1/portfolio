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
import name.abuchen.portfolio.model.SecurityDelta;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;

@SuppressWarnings("nls")
public class ExposureCalculatorTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 8, 14);

    @Test
    public void testStandardOptionSupportsNotionalAndDeltaAdjustedExposure()
    {
        Client client = new Client();
        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "200");
        option.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 100.0));
        SecurityDelta.replaceAll(option, List.of(SecurityDelta.of(LocalDate.of(2026, 1, 1), 0.5)));
        client.addSecurity(option);

        SecurityPosition position = position(option, 2, 4.0);
        TestCurrencyConverter converter = new TestCurrencyConverter();

        assertThat(ExposureCalculator.calculate(client, position, DATE, converter, ExposureType.NOTIONAL),
                        is(Money.of("EUR", Values.Amount.factorize(40000.0))));
        assertThat(ExposureCalculator.calculate(client, position, DATE, converter, ExposureType.DELTA_ADJUSTED),
                        is(Money.of("EUR", Values.Amount.factorize(20000.0))));
    }

    @Test
    public void testKnockoutCertificateUsesUnderlyingAsExposureReference()
    {
        Client client = new Client();
        Security underlying = new Security("Underlying", "EUR");
        underlying.addPrice(new SecurityPrice(DATE, Values.Quote.factorize(125.0)));
        client.addSecurity(underlying);

        Security certificate = new Security("K.O.", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "optionProductType", "KNOCK_OUT_CERTIFICATE");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlyingSecurityUUID", underlying.getUUID());
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "100");
        certificate.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 10.0));
        client.addSecurity(certificate);

        SecurityPosition position = position(certificate, 3, 2.0);
        Money exposure = ExposureCalculator.calculate(client, position, DATE, new TestCurrencyConverter(),
                        ExposureType.NOTIONAL);

        assertThat(exposure, is(Money.of("EUR", Values.Amount.factorize(3750.0))));
    }

    @Test
    public void testMarketValueUsesDerivativePositionValuation()
    {
        Client client = new Client();
        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 100.0));
        client.addSecurity(option);

        SecurityPosition position = position(option, 2, 4.0);
        Money exposure = ExposureCalculator.calculate(client, position, DATE, new TestCurrencyConverter(),
                        ExposureType.MARKET_VALUE);

        assertThat(exposure, is(Money.of("EUR", Values.Amount.factorize(800.0))));
    }

    private SecurityPosition position(Security security, long quantity, double price)
    {
        PortfolioTransaction buy = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        buy.setSecurity(security);
        buy.setCurrencyCode("EUR");
        buy.setShares(Values.Share.factorize(quantity));
        buy.setAmount(Values.Amount.factorize(quantity * price));

        return new SecurityPosition(security, new TestCurrencyConverter(),
                        new SecurityPrice(DATE, Values.Quote.factorize(price)), List.of(buy));
    }
}
