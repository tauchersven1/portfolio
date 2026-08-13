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
}
