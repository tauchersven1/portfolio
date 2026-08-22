package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityMultiplierLookupTest
{
    private Security security;

    @Before
    public void setup()
    {
        security = new Security();
    }

    @Test
    public void testDefaultMultiplier()
    {
        assertThat(security.getMultiplier(LocalDate.parse("2026-01-01")), is(1.0));
    }

    @Test
    public void testLookupOfExactMultiplier()
    {
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));

        assertThat(security.getMultiplier(LocalDate.parse("2026-01-01")), is(1000.0));
    }

    @Test
    public void testPreviousMultiplierIsUsedBetweenEntries()
    {
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-07-01"), 1125.0));

        assertThat(security.getMultiplier(LocalDate.parse("2026-05-01")), is(1000.0));
        assertThat(security.getMultiplier(LocalDate.parse("2026-08-01")), is(1125.0));
    }

    @Test
    public void testDefaultMultiplierBeforeFirstEntry()
    {
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));

        assertThat(security.getMultiplier(LocalDate.parse("2025-12-31")), is(1.0));
    }

    @Test
    public void testReplacingMultiplierForSameDate()
    {
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1250.0));

        assertThat(security.getMultipliers().size(), is(1));
        assertThat(security.getMultiplier(LocalDate.parse("2026-01-01")), is(1250.0));
    }

    @Test
    public void testDeepCopyCopiesMultipliers()
    {
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-07-01"), 1125.0));

        Security copy = security.deepCopy();

        assertThat(copy.getMultipliers().size(), is(2));
        assertThat(copy.getMultiplier(LocalDate.parse("2026-08-01")), is(1125.0));
    }
}