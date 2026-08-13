package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityDeltaLookupTest
{
    @Test
    public void testDefaultDeltaIsOne()
    {
        Security security = new Security();
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-08-13")), is(1.0));
    }

    @Test
    public void testTimeDependentDeltaLookup()
    {
        Security security = new Security();
        SecurityDelta.replaceAll(security,
                        List.of(SecurityDelta.of(LocalDate.parse("2026-01-01"), 0.75),
                                        SecurityDelta.of(LocalDate.parse("2026-07-01"), -0.40)));

        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2025-12-31")), is(1.0));
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-06-30")), is(0.75));
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-07-01")), is(-0.40));
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-12-31")), is(-0.40));
    }

    @Test
    public void testDeltaPropertiesAreReplaced()
    {
        Security security = new Security();
        SecurityDelta.replaceAll(security,
                        List.of(SecurityDelta.of(LocalDate.parse("2026-01-01"), 0.5)));
        SecurityDelta.replaceAll(security,
                        List.of(SecurityDelta.of(LocalDate.parse("2026-02-01"), 0.25)));

        assertThat(SecurityDelta.getDeltas(security).size(), is(1));
        assertThat(SecurityDelta.getDeltas(security).get(0).getDate(), is(LocalDate.parse("2026-02-01")));
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-01-15")), is(1.0));
        assertThat(SecurityDelta.getDelta(security, LocalDate.parse("2026-03-01")), is(0.25));
    }
}