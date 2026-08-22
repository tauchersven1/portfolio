package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

@SuppressWarnings("nls")
public class OptionSymbolParserTest
{
    @Test
    public void testParseCallSymbol()
    {
        OptionSymbolParser.OptionData data = OptionSymbolParser.parse("IAG260918C00017000").orElseThrow();

        assertThat(data.getUnderlying(), is("IAG"));
        assertThat(data.getExpirationDate(), is(LocalDate.of(2026, 9, 18)));
        assertThat(data.getPutCall(), is("CALL"));
        assertThat(data.getStrike(), is(new BigDecimal("17")));
    }

    @Test
    public void testParsePutSymbol()
    {
        OptionSymbolParser.OptionData data = OptionSymbolParser.parse("IAG260918P00017000").orElseThrow();

        assertThat(data.getUnderlying(), is("IAG"));
        assertThat(data.getPutCall(), is("PUT"));
        assertThat(data.getStrike(), is(new BigDecimal("17")));
    }

    @Test
    public void testParseCompactEurexIbCallSymbol()
    {
        OptionSymbolParser.OptionData data = OptionSymbolParser.parse("CSIE20260821305M").orElseThrow();

        assertThat(data.getUnderlying(), is("SIE"));
        assertThat(data.getExpirationDate(), is(LocalDate.of(2026, 8, 21)));
        assertThat(data.getPutCall(), is("CALL"));
        assertThat(data.getStrike(), is(new BigDecimal("305")));
    }

    @Test
    public void testParseSpacedEurexIbPutSymbol()
    {
        OptionSymbolParser.OptionData data = OptionSymbolParser.parse("P BMW 20221216 72 M").orElseThrow();

        assertThat(data.getUnderlying(), is("BMW"));
        assertThat(data.getExpirationDate(), is(LocalDate.of(2022, 12, 16)));
        assertThat(data.getPutCall(), is("PUT"));
        assertThat(data.getStrike(), is(new BigDecimal("72")));
    }

    @Test
    public void testInvalidSymbolIsIgnored()
    {
        assertThat(OptionSymbolParser.parse("IAG").isEmpty(), is(true));
        assertThat(OptionSymbolParser.parse("IAG261332C00017000").isEmpty(), is(true));
        assertThat(OptionSymbolParser.parse("CSIE20261321305M").isEmpty(), is(true));
    }

    @Test
    public void testPopulateOnlyMissingDerivativeProperties()
    {
        Security security = new Security();
        security.setTickerSymbol("IAG260918C00017000");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "18");

        assertThat(OptionSymbolParser.populateMissingDerivativeProperties(security), is(true));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "type").orElseThrow(), is("OPTION"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying").orElseThrow(), is("IAG"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "expirationDate").orElseThrow(),
                        is("2026-09-18"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall").orElseThrow(), is("CALL"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike").orElseThrow(), is("18"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "contractSymbol").orElseThrow(),
                        is("IAG260918C00017000"));
    }

    @Test
    public void testPopulateCompactEurexIbSymbol()
    {
        Security security = new Security();
        security.setTickerSymbol("CSIE20260821305M");

        assertThat(OptionSymbolParser.populateMissingDerivativeProperties(security), is(true));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "type").orElseThrow(), is("OPTION"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlying").orElseThrow(), is("SIE"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "expirationDate").orElseThrow(),
                        is("2026-08-21"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall").orElseThrow(), is("CALL"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike").orElseThrow(), is("305"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "contractSymbol").orElseThrow(),
                        is("CSIE20260821305M"));
    }
}
