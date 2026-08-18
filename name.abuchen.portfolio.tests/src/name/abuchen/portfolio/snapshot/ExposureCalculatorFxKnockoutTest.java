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
public class ExposureCalculatorFxKnockoutTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 8, 18);

    @Test
    public void testFxKnockoutDoesNotRequireLinkedUnderlyingSecurity()
    {
        Client client = new Client();
        Security certificate = certificate("CALL", 0.5);
        client.addSecurity(certificate);

        Money notional = ExposureCalculator.calculate(client,
                        position(certificate, 2, PortfolioTransaction.Type.BUY), DATE, new TestCurrencyConverter(),
                        ExposureType.NOTIONAL);
        Money deltaAdjusted = ExposureCalculator.calculate(client,
                        position(certificate, 2, PortfolioTransaction.Type.BUY), DATE, new TestCurrencyConverter(),
                        ExposureType.DELTA_ADJUSTED);

        assertThat(notional, is(Money.of("EUR", Values.Amount.factorize(200.0))));
        assertThat(deltaAdjusted, is(Money.of("EUR", Values.Amount.factorize(100.0))));
    }

    @Test
    public void testFxKnockoutCallPutAndLongShortDirections()
    {
        Client client = new Client();
        Security call = certificate("CALL", 0.5);
        Security put = certificate("PUT", -0.5);
        client.addSecurity(call);
        client.addSecurity(put);
        TestCurrencyConverter converter = new TestCurrencyConverter();

        assertThat(ExposureCalculator.calculate(client, position(call, 2, PortfolioTransaction.Type.BUY), DATE,
                        converter, ExposureType.NOTIONAL), is(Money.of("EUR", Values.Amount.factorize(200.0))));
        assertThat(ExposureCalculator.calculate(client, position(call, 2, PortfolioTransaction.Type.SELL), DATE,
                        converter, ExposureType.NOTIONAL), is(Money.of("EUR", Values.Amount.factorize(-200.0))));
        assertThat(ExposureCalculator.calculate(client, position(put, 2, PortfolioTransaction.Type.BUY), DATE,
                        converter, ExposureType.NOTIONAL), is(Money.of("EUR", Values.Amount.factorize(-200.0))));
        assertThat(ExposureCalculator.calculate(client, position(put, 2, PortfolioTransaction.Type.SELL), DATE,
                        converter, ExposureType.NOTIONAL), is(Money.of("EUR", Values.Amount.factorize(200.0))));

        assertThat(ExposureCalculator.calculate(client, position(call, 2, PortfolioTransaction.Type.BUY), DATE,
                        converter, ExposureType.DELTA_ADJUSTED),
                        is(Money.of("EUR", Values.Amount.factorize(100.0))));
        assertThat(ExposureCalculator.calculate(client, position(put, 2, PortfolioTransaction.Type.BUY), DATE,
                        converter, ExposureType.DELTA_ADJUSTED),
                        is(Money.of("EUR", Values.Amount.factorize(-100.0))));
    }

    @Test
    public void testFxKnockoutMarketValueRemainsPriceTimesQuantityTimesMultiplier()
    {
        Client client = new Client();
        Security certificate = certificate("CALL", 0.5);
        client.addSecurity(certificate);

        Money marketValue = ExposureCalculator.calculate(client,
                        position(certificate, 2, PortfolioTransaction.Type.BUY), DATE, new TestCurrencyConverter(),
                        ExposureType.MARKET_VALUE);

        assertThat(marketValue, is(Money.of("EUR", Values.Amount.factorize(8.0))));
    }

    @Test
    public void testFxKnockoutFallsBackToMultiplierWhenRatioIsMissing()
    {
        Client client = new Client();
        Security certificate = certificate("CALL", 0.5);
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.SUBSCRIPTION_RATIO, null);
        certificate.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 2), 50.0));
        client.addSecurity(certificate);

        Money exposure = ExposureCalculator.calculate(client,
                        position(certificate, 2, PortfolioTransaction.Type.BUY), DATE, new TestCurrencyConverter(),
                        ExposureType.NOTIONAL);

        assertThat(exposure, is(Money.of("EUR", Values.Amount.factorize(100.0))));
    }

    private Security certificate(String putCall, double delta)
    {
        Security certificate = new Security("EUR/JPY K.O.", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall", putCall);
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.OPTION_PRODUCT_TYPE,
                        ExposureCalculator.KNOCK_OUT_CERTIFICATE);
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.FX_UNDERLYING, "true");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.FX_BASE_CURRENCY, "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.FX_QUOTE_CURRENCY, "JPY");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, ExposureCalculator.SUBSCRIPTION_RATIO, "100");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "EUR/JPY");
        certificate.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 1.0));
        SecurityDelta.replaceAll(certificate, List.of(SecurityDelta.of(LocalDate.of(2026, 1, 1), delta)));
        return certificate;
    }

    private SecurityPosition position(Security security, long quantity, PortfolioTransaction.Type type)
    {
        PortfolioTransaction transaction = new PortfolioTransaction(type);
        transaction.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        transaction.setSecurity(security);
        transaction.setCurrencyCode("EUR");
        transaction.setShares(Values.Share.factorize(quantity));
        transaction.setAmount(Values.Amount.factorize(quantity * 4.0));

        return new SecurityPosition(security, new TestCurrencyConverter(),
                        new SecurityPrice(DATE, Values.Quote.factorize(4.0)), List.of(transaction));
    }
}
