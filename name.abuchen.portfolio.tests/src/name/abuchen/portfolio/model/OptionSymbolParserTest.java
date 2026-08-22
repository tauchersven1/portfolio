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
    public void testInvalidSymbolIsIgnored()
    {
        assertThat(OptionSymbolParser.parse("IAG").isEmpty(), is(true));
        assertThat(OptionSymbolParser.parse("IAG261332C00017000").isEmpty(), is(true));
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
}
