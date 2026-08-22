package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

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

@SuppressWarnings("nls")
public class DerivativePositionCalculatorTest
{
    @Test
    public void testOptionMarketValueUsesMultiplier()
    {
        LocalDate date = LocalDate.of(2026, 8, 13);
        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 100.0));

        Money value = DerivativePositionCalculator.calculateMarketValue(option, Values.Share.factorize(2),
                        new SecurityPrice(date, Values.Quote.factorize(4.0)), List.of(), new TestCurrencyConverter(),
                        date);

        assertThat(value, is(Money.of("EUR", Values.Amount.factorize(800.0))));
    }

    @Test
    public void testFutureMarketValueIsUnrealizedProfit()
    {
        LocalDate date = LocalDate.of(2026, 8, 13);
        Security future = new Security("Future", "EUR");
        future.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "FUTURE");
        future.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 50.0));

        PortfolioTransaction buy = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        buy.setSecurity(future);
        buy.setCurrencyCode("EUR");
        buy.setShares(Values.Share.factorize(2));
        buy.setAmount(Values.Amount.factorize(200.0));

        Money value = DerivativePositionCalculator.calculateMarketValue(future, Values.Share.factorize(2),
                        new SecurityPrice(date, Values.Quote.factorize(110.0)), List.of(buy),
                        new TestCurrencyConverter(), date);

        assertThat(value, is(Money.of("EUR", Values.Amount.factorize(1000.0))));
    }

    @Test
    public void testSecurityPositionOptionMarketValueUsesMultiplier()
    {
        LocalDate date = LocalDate.of(2026, 8, 13);
        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 100.0));

        PortfolioTransaction buy = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        buy.setSecurity(option);
        buy.setCurrencyCode("EUR");
        buy.setShares(Values.Share.factorize(2));
        buy.setAmount(Values.Amount.factorize(8.0));

        SecurityPosition position = new SecurityPosition(option, new TestCurrencyConverter(),
                        new SecurityPrice(date, Values.Quote.factorize(4.0)), List.of(buy));

        assertThat(position.calculateValue(), is(Money.of("EUR", Values.Amount.factorize(800.0))));
    }

    @Test
    public void testSecurityPositionFutureMarketValueUsesUnrealizedProfit()
    {
        LocalDate date = LocalDate.of(2026, 8, 13);
        Security future = new Security("Future", "EUR");
        future.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "FUTURE");
        future.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 50.0));

        PortfolioTransaction buy = new PortfolioTransaction(PortfolioTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        buy.setSecurity(future);
        buy.setCurrencyCode("EUR");
        buy.setShares(Values.Share.factorize(2));
        buy.setAmount(Values.Amount.factorize(200.0));

        SecurityPosition position = new SecurityPosition(future, new TestCurrencyConverter(),
                        new SecurityPrice(date, Values.Quote.factorize(110.0)), List.of(buy));

        assertThat(position.calculateValue(), is(Money.of("EUR", Values.Amount.factorize(1000.0))));
    }

    @Test
    public void testExistingSecurityConvertedToOptionResolvesUnderlyingByStoredNameWithoutUUID()
    {
        Client client = new Client();
        Security underlying = new Security("Apple Inc.", "USD");
        underlying.setTickerSymbol("AAPL");
        client.addSecurity(underlying);

        Security option = new Security("Existing asset", "USD");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "Apple Inc.");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "200");
        client.addSecurity(option);

        assertThat(DerivativePositionCalculator.resolveUnderlying(client, option), is(underlying));
    }

    @Test
    public void testOptionResolvesUnderlyingByTickerWithoutUUID()
    {
        Client client = new Client();
        Security underlying = new Security("Apple Inc.", "USD");
        underlying.setTickerSymbol("AAPL");
        client.addSecurity(underlying);

        Security option = new Security("Option", "USD");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "aapl");
        client.addSecurity(option);

        assertThat(DerivativePositionCalculator.resolveUnderlying(client, option), is(underlying));
    }

    @Test
    public void testOptionUnderlyingUUIDTakesPrecedenceOverTextFallback()
    {
        Client client = new Client();
        Security byName = new Security("Apple Inc.", "USD");
        byName.setTickerSymbol("AAPL");
        Security byUUID = new Security("Different security", "USD");
        client.addSecurity(byName);
        client.addSecurity(byUUID);

        Security option = new Security("Option", "USD");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "Apple Inc.");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlyingSecurityUUID", byUUID.getUUID());
        client.addSecurity(option);

        assertThat(DerivativePositionCalculator.resolveUnderlying(client, option), is(byUUID));
    }

    @Test
    public void testOptionUnderlyingFallbackRequiresUniqueMatch()
    {
        Client client = new Client();
        Security first = new Security("Index", "EUR");
        Security second = new Security("Index", "EUR");
        client.addSecurity(first);
        client.addSecurity(second);

        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying", "Index");
        client.addSecurity(option);

        assertNull(DerivativePositionCalculator.resolveUnderlying(client, option));
    }

    @Test
    public void testOptionStrikeIsExposureReferenceWithoutUnderlying()
    {
        Security option = new Security("Option", "USD");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "200.50");

        assertThat(DerivativePositionCalculator.getOptionStrikeQuoteValue(option),
                        is(Long.valueOf(Values.Quote.factorize(200.50))));
    }

    @Test
    public void testOptionStrikeSupportsDecimalComma()
    {
        Security option = new Security("Option", "EUR");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "123,45");

        assertThat(DerivativePositionCalculator.getOptionStrikeQuoteValue(option),
                        is(Long.valueOf(Values.Quote.factorize(123.45))));
    }

    @Test
    public void testOptionExposureScalesWithMultiplierAndDelta()
    {
        long shares = Values.Share.factorize(2);
        long strike = Values.Quote.factorize(200.0);

        Money exposure = DerivativePositionCalculator.valueOf(shares, strike, 100.0 * 0.5, "USD");

        assertThat(exposure, is(Money.of("USD", Values.Amount.factorize(20000.0))));
    }
}
