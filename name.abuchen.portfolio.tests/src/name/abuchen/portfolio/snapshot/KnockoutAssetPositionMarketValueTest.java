package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class KnockoutAssetPositionMarketValueTest
{
    @Test
    public void testFxKnockoutMarketValueIsAvailableInAssetPosition()
    {
        LocalDate date = LocalDate.of(2026, 8, 18);
        Security certificate = new Security("USD/JPY K.O.", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "optionProductType", "KNOCK_OUT_CERTIFICATE");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxUnderlying", "true");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxBaseCurrency", "USD");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxQuoteCurrency", "JPY");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "subscriptionRatio", "100");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "USD/JPY");
        certificate.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 1.0));

        PortfolioTransaction buy = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        buy.setSecurity(certificate);
        buy.setCurrencyCode("EUR");
        buy.setShares(Values.Share.factorize(10));
        buy.setAmount(Values.Amount.factorize(6.50));

        TestCurrencyConverter converter = new TestCurrencyConverter();
        SecurityPosition position = new SecurityPosition(certificate, converter,
                        new SecurityPrice(date, Values.Quote.factorize(0.65)), List.of(buy));
        AssetPosition assetPosition = new AssetPosition(position, converter, date,
                        Money.of("EUR", Values.Amount.factorize(1000)));

        assertThat(position.calculateValue(date), is(Money.of("EUR", Values.Amount.factorize(6.50))));
        assertThat(assetPosition.getValuation(), is(Money.of("EUR", Values.Amount.factorize(6.50))));
    }
}
