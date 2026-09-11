package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.DerivativeExposure;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DerivativeExposureTest
{
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    @Test
    public void testOptionUsesDatedMultiplierAndDelta()
    {
        Client client = new Client();
        Security option = security("Option", 100);
        option.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.instrumentType", "OPTION");
        option.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.multiplierHistory",
                        "2026-01-01=10;2027-01-01=20");
        option.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.deltaHistory", "2026-01-01=0.5");
        option.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.strike", "150");

        DerivativeExposure.Result result = DerivativeExposure.calculate(client, position(option, 2), DATE);

        assertThat(result.net(), is(Money.of(CurrencyUnit.EUR, 150000)));
        assertThat(result.gross(), is(Money.of(CurrencyUnit.EUR, 150000)));
        assertThat(result.notional(), is(Money.of(CurrencyUnit.EUR, 300000)));
    }

    @Test
    public void testPutDirectionDoesNotDependOnEnteredDeltaSign()
    {
        Client client = new Client();
        Security put = security("Put", 100);
        put.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.instrumentType", "OPTION");
        put.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.putCall", "PUT");
        put.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.multiplierHistory", "2026-01-01=10");
        put.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.deltaHistory", "2026-01-01=0.5");
        put.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.strike", "150");

        DerivativeExposure.Result longPut = DerivativeExposure.calculate(client, position(put, 2), DATE);
        DerivativeExposure.Result shortPut = DerivativeExposure.calculate(client, position(put, -2), DATE);

        assertThat(longPut.net(), is(Money.of(CurrencyUnit.EUR, -150000)));
        assertThat(shortPut.net(), is(Money.of(CurrencyUnit.EUR, 150000)));
        assertThat(longPut.notional(), is(Money.of(CurrencyUnit.EUR, 300000)));
        assertThat(shortPut.notional(), is(Money.of(CurrencyUnit.EUR, -300000)));
    }

    @Test
    public void testFutureNotionalUsesPriceAndMultiplierWithoutDelta()
    {
        Client client = new Client();
        Security future = security("Future", 100);
        future.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.instrumentType", "FUTURE");
        future.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.multiplierHistory", "2026-01-01=10");
        future.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.deltaHistory", "2026-01-01=0.25");

        DerivativeExposure.Result result = DerivativeExposure.calculate(client, position(future, 2), DATE);

        assertThat(result.net(), is(Money.of(CurrencyUnit.EUR, 50000)));
        assertThat(result.notional(), is(Money.of(CurrencyUnit.EUR, 200000)));
    }

    @Test
    public void testLongAndShortKnockOutUseHistoricalUnderlyingAndLevel()
    {
        Client client = new Client();
        Security underlying = security("Underlying", 120);
        underlying.setTickerSymbol("UND");
        client.addSecurity(underlying);

        Security longCertificate = knockOut("Long KO", "CALL", 100);
        longCertificate.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.underlying", "UND");
        DerivativeExposure.Result longResult = DerivativeExposure.calculate(client, position(longCertificate, 2),
                        DATE);
        assertThat(longResult.leverage().intValueExact(), is(5));
        assertThat(longResult.net(), is(Money.of(CurrencyUnit.EUR, 10000)));

        Security shortCertificate = knockOut("Short KO", "PUT", 150);
        shortCertificate.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.underlying", "UND");
        DerivativeExposure.Result shortResult = DerivativeExposure.calculate(client, position(shortCertificate, 2),
                        DATE);
        assertThat(shortResult.leverage().intValueExact(), is(4));
        assertThat(shortResult.net(), is(Money.of(CurrencyUnit.EUR, -8000)));
        assertThat(shortResult.gross(), is(Money.of(CurrencyUnit.EUR, 8000)));
    }

    @Test
    public void testKnockOutWithoutLeverageLeavesExposureEmptyForMarketValueFallback()
    {
        Client client = new Client();
        Security certificate = knockOut("KO without underlying", "CALL", 100);

        DerivativeExposure.Result result = DerivativeExposure.calculate(client, position(certificate, 2), DATE);

        assertThat(result.net(), is((Money) null));
        assertThat(result.gross(), is((Money) null));
        assertThat(result.notional(), is((Money) null));
    }

    private Security knockOut(String name, String putCall, int level)
    {
        Security security = security(name, 10);
        security.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.instrumentType",
                        "KNOCK_OUT_CERTIFICATE");
        security.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.putCall", putCall);
        security.setPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon.knockOutLevelHistory",
                        "2026-01-01=" + level);
        return security;
    }

    private Security security(String name, int price)
    {
        Security security = new Security(name, CurrencyUnit.EUR);
        security.addPrice(new SecurityPrice(DATE, Values.Quote.factorize(price)));
        return security;
    }

    private AssetPosition position(Security security, long shares)
    {
        PortfolioTransaction transaction = new PortfolioTransaction();
        transaction.setType(shares >= 0 ? PortfolioTransaction.Type.BUY : PortfolioTransaction.Type.SELL);
        transaction.setSecurity(security);
        transaction.setShares(Math.abs(shares) * Values.Share.factor());
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        SecurityPosition position = new SecurityPosition(security, new TestCurrencyConverter(),
                        security.getSecurityPrice(DATE), List.of(transaction));
        return new AssetPosition(position, new TestCurrencyConverter(), DATE, Money.of(CurrencyUnit.EUR, 1000000));
    }
}
