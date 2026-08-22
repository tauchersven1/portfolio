package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.CoreMatchers.nullValue;

import org.junit.Test;

import name.abuchen.portfolio.online.DerivativeMasterDataProvider.Result;

@SuppressWarnings("nls")
public class VontobelDerivativeMasterDataProviderTest
{
    @Test
    public void testParsesUsdJpyOpenEndKnockout()
    {
        String html = """
                        <html><body>
                        <h1>JPY per 1 USD</h1>
                        <div>Call Hebel: 12,5</div>
                        <div>ISIN DE000VY7SRS7 WKN VY7SRS</div>
                        <div>Knock-Out Barriere | 160,70 JPY</div>
                        <div>Basispreis | 160,70 JPY</div>
                        <div>Bezugsverhältnis | 100,00</div>
                        <div>15.06.2026 Ausgabetag</div>
                        <div>16.06.2026 Erster Handelstag</div>
                        <div>Open-End</div>
                        </body></html>
                        """;

        Result result = VontobelDerivativeMasterDataProvider.parsePage(html,
                        "/de-de/produkte/hebel/turbo-optionsscheine-open-end/");

        assertThat(result.get("issuer"), is("Vontobel"));
        assertThat(result.get("type"), is("OPTION"));
        assertThat(result.get("optionProductType"), is("KNOCK_OUT_CERTIFICATE"));
        assertThat(result.get("putCall"), is("CALL"));
        assertThat(result.get("underlying"), is("USD/JPY"));
        assertThat(result.get("fxUnderlying"), is("true"));
        assertThat(result.get("fxBaseCurrency"), is("USD"));
        assertThat(result.get("fxQuoteCurrency"), is("JPY"));
        assertThat(result.get("strike"), is("160.70"));
        assertThat(result.get("initialKnockoutLevel"), is("160.70"));
        assertThat(result.get("currentKnockoutLevel"), is("160.70"));
        assertThat(result.get("subscriptionRatio"), is("100.00"));
        assertThat(result.get("issuerLeverage"), is("12.5"));
        assertThat(result.get("firstTradingDay"), is("2026-06-16"));
    }

    @Test
    public void testParsesDirectFxPairAndFixedMaturityDates()
    {
        String html = """
                        <html><body>
                        <div>Turbo-Optionsschein</div>
                        <h1>USD/JPY</h1>
                        <div>Put Laufzeit: 18.12.2026</div>
                        <div>ISIN DE000TEST123 WKN ABC123</div>
                        <div>Basispreis 165,25 JPY</div>
                        <div>Knock-Out Barriere 165,25 JPY</div>
                        <div>Bezugsverhältnis 100,00</div>
                        <div>01.07.2026 Erster Handelstag</div>
                        <div>18.12.2026 Letzter Handelstag</div>
                        <div>18.12.2026 Bewertungstag</div>
                        <div>28.12.2026 Rückzahlungstag</div>
                        </body></html>
                        """;

        Result result = VontobelDerivativeMasterDataProvider.parsePage(html,
                        "/de-de/produkte/hebel/turbo-optionsscheine/");

        assertThat(result.get("putCall"), is("PUT"));
        assertThat(result.get("fxBaseCurrency"), is("USD"));
        assertThat(result.get("fxQuoteCurrency"), is("JPY"));
        assertThat(result.get("lastTradingDay"), is("2026-12-18"));
        assertThat(result.get("expirationDate"), is("2026-12-18"));
        assertThat(result.get("settlementDate"), is("2026-12-28"));
    }

    @Test
    public void testParsesEquityUnderlyingDecimalRatioAndIssuerPrice()
    {
        String html = """
                        <html><body>
                        <div>Turbo-Optionsschein Open End</div>
                        <div>Long</div>
                        <div>ISIN DE000BD2SXK2 WKN BD2SXK</div>
                        <div>Basiswert Airbnb Inc. Class A</div>
                        <div>Basiswertkurs 125,40 USD</div>
                        <div>Basispreis 130,50 USD</div>
                        <div>Knock-Out Barriere 130,50 USD</div>
                        <div>Bezugsverhältnis 0,1</div>
                        </body></html>
                        """;

        Result result = VontobelDerivativeMasterDataProvider.parsePage(html,
                        "/de-de/produkte/hebel/turbo-optionsscheine-open-end/");

        assertThat(result.get("underlying"), is("Airbnb Inc. Class A"));
        assertThat(result.get("subscriptionRatio"), is("0.1"));
        assertThat(result.get("issuerUnderlyingPrice"), is("125.40"));
        assertThat(result.get("issuerUnderlyingCurrency"), is("USD"));
        assertThat(result.get("optionProductType"), is("KNOCK_OUT_CERTIFICATE"));
        assertThat(result.get("putCall"), is("CALL"));
    }

    @Test
    public void testRejectsWebsiteSearchTextAsUnderlying()
    {
        String html = """
                        <html><body>
                        <div>Long</div>
                        <div>ISIN DE000BD2SXK2 WKN BD2SXK</div>
                        <div>Basiswert e, Produkte und Themen suchen</div>
                        <div>Basiswertkurs 125,40 USD</div>
                        <div>Basispreis 130,50 USD</div>
                        <div>Bezugsverhältnis 0,1</div>
                        </body></html>
                        """;

        Result result = VontobelDerivativeMasterDataProvider.parsePage(html,
                        "/de-de/produkte/hebel/turbo-optionsscheine-open-end/");

        assertThat(result.get("underlying"), is(nullValue()));
        assertThat(result.get("issuerUnderlyingPrice"), is("125.40"));
        assertThat(result.get("issuerUnderlyingCurrency"), is("USD"));
    }
}
