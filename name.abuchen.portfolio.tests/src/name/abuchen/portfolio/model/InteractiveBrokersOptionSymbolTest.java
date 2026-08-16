package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

@SuppressWarnings("nls")
public class InteractiveBrokersOptionSymbolTest
{
    @Test
    public void testPutSymbol()
    {
        InteractiveBrokersOptionSymbol.Parsed parsed = InteractiveBrokersOptionSymbol.parse("GLD261218P00425000")
                        .orElseThrow();

        assertThat(parsed.tradingSymbol(), is("GLD"));
        assertThat(parsed.expirationDate(), is(LocalDate.of(2026, 12, 18)));
        assertThat(parsed.putCall(), is("PUT"));
        assertThat(parsed.strike(), is(new BigDecimal("425")));
    }

    @Test
    public void testCallSymbol()
    {
        InteractiveBrokersOptionSymbol.Parsed parsed = InteractiveBrokersOptionSymbol.parse("AAPL270115C00195000")
                        .orElseThrow();

        assertThat(parsed.tradingSymbol(), is("AAPL"));
        assertThat(parsed.expirationDate(), is(LocalDate.of(2027, 1, 15)));
        assertThat(parsed.putCall(), is("CALL"));
        assertThat(parsed.strike(), is(new BigDecimal("195")));
    }

    @Test
    public void testInvalidSymbolIsIgnored()
    {
        assertThat(InteractiveBrokersOptionSymbol.parse("GLD").isEmpty(), is(true));
        assertThat(InteractiveBrokersOptionSymbol.parse("GLD269918P00425000").isEmpty(), is(true));
    }
}
