package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityMultiplierLookupTest
{
    @Test
    public void testBeforeFirstMultiplierUsesOldestValue()
    {
        Security security = securityWithHistory();
        assertThat(security.getMultiplier(LocalDate.parse("2025-12-31")), is(10.0));
    }

    @Test
    public void testBetweenMultiplierEntriesUsesLatestKnownValue()
    {
        Security security = securityWithHistory();
        assertThat(security.getMultiplier(LocalDate.parse("2026-06-30")), is(10.0));
    }

    @Test
    public void testAfterLastMultiplierUsesLatestValue()
    {
        Security security = securityWithHistory();
        assertThat(security.getMultiplier(LocalDate.parse("2027-01-01")), is(20.0));
    }

    @Test
    public void testEmptyHistoryDefaultsToOne()
    {
        Security security = new Security();
        assertThat(security.getMultiplier(LocalDate.parse("2026-01-01")), is(1.0));
    }

    private Security securityWithHistory()
    {
        Security security = new Security();
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 10.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-07-01"), 20.0));
        return security;
    }
}
