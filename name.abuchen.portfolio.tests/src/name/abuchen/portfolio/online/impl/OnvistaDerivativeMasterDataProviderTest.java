package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

import name.abuchen.portfolio.online.DerivativeMasterDataProvider.Result;

@SuppressWarnings("nls")
public class OnvistaDerivativeMasterDataProviderTest
{
    @Test
    public void testParsesIndexKnockoutCertificate()
    {
        String html = """
                        <html><body>
                        <h1>TURBO SHORT AUF DAX PERFORMANCE INDEX</h1>
                        <div>WKN WA8ZVB</div>
                        <div>ISIN DE000WA8ZVB5</div>
                        <div>Emittent UBS</div>
                        <div>Basispreis 28.350,00 Pkt.</div>
                        <div>K.O. 28.350,00 Pkt.</div>
                        <div>Hebel 11,31 x</div>
                        <div>Bezugsverhältnis 0,010</div>
                        <div>Bewertungstag 16.09.2026</div>
                        <div>Basiswert DAX Xetra · 11:15:46 26.051,24 Pkt.</div>
                        </body></html>
                        """;

        Result result = OnvistaDerivativeMasterDataProvider.parsePage(html);

        assertThat(result.get("type"), is("OPTION"));
        assertThat(result.get("optionProductType"), is("KNOCK_OUT_CERTIFICATE"));
        assertThat(result.get("putCall"), is("PUT"));
        assertThat(result.get("issuer"), is("UBS"));
        assertThat(result.get("issuerProductId"), is("WA8ZVB"));
        assertThat(result.get("strike"), is("28350.00"));
        assertThat(result.get("initialKnockoutLevel"), is("28350.00"));
        assertThat(result.get("currentKnockoutLevel"), is("28350.00"));
        assertThat(result.get("issuerLeverage"), is("11.31"));
        assertThat(result.get("subscriptionRatio"), is("0.010"));
        assertThat(result.get("expirationDate"), is("2026-09-16"));
        assertThat(result.get("lastTradingDay"), is("2026-09-16"));
    }

    @Test
    public void testParsesFxKnockoutAndCurrencies()
    {
        String html = """
                        <html><body>
                        <h1>OPEN END TURBO OPTIONSSCHEIN LONG AUF EUR/USD</h1>
                        <div>WKN TEST01</div>
                        <div>ISIN DE000TEST010</div>
                        <div>Emittent HSBC</div>
                        <div>Basispreis 1,1450 USD</div>
                        <div>K.O.-Schwelle 1,1450 USD</div>
                        <div>Hebel 18,42 x</div>
                        <div>Bezugsverhältnis 100,00</div>
                        <div>Basiswert EUR/USD 1,1661 USD</div>
                        </body></html>
                        """;

        Result result = OnvistaDerivativeMasterDataProvider.parsePage(html);

        assertThat(result.get("putCall"), is("CALL"));
        assertThat(result.get("underlying"), is("EUR/USD"));
        assertThat(result.get("fxUnderlying"), is("true"));
        assertThat(result.get("fxBaseCurrency"), is("EUR"));
        assertThat(result.get("fxQuoteCurrency"), is("USD"));
        assertThat(result.get("issuer"), is("HSBC"));
        assertThat(result.get("issuerLeverage"), is("18.42"));
        assertThat(result.get("subscriptionRatio"), is("100.00"));
    }

    @Test
    public void testParsesEquityUnderlyingAndIssuerPrice()
    {
        String html = """
                        <html><body>
                        <h1>OPEN END TURBO OPTIONSSCHEIN LONG AUF LAM RESEARCH</h1>
                        <div>WKN MR6TV2</div>
                        <div>ISIN DE000MR6TV26</div>
                        <div>Emittent Morgan Stanley</div>
                        <div>Basispreis 310,00 USD</div>
                        <div>K.O. 310,00 USD</div>
                        <div>Hebel 8,5 x</div>
                        <div>Bezugsverhältnis 0,10</div>
                        <div>Basiswert Lam Research Corporation Nasdaq · 16:00 315,84 USD</div>
                        </body></html>
                        """;

        Result result = OnvistaDerivativeMasterDataProvider.parsePage(html);

        assertThat(result.get("underlying"), is("Lam Research Corporation"));
        assertThat(result.get("issuer"), is("Morgan Stanley"));
        assertThat(result.get("issuerUnderlyingPrice"), is("315.84"));
        assertThat(result.get("issuerUnderlyingCurrency"), is("USD"));
    }
}
