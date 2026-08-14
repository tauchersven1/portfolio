package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityKnockoutLevelTest
{
    @Test
    public void testTimeDependentKnockoutLevel()
    {
        Security security = new Security("KO certificate", "EUR");

        SecurityKnockoutLevel.replaceAll(security,
                        List.of(SecurityKnockoutLevel.of(LocalDate.of(2026, 8, 1), 100.0),
                                        SecurityKnockoutLevel.of(LocalDate.of(2026, 8, 10), 101.25)));

        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 7, 31)), is(nullValue()));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 1)), is(100.0));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 9)), is(100.0));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 10)), is(101.25));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 20)), is(101.25));
    }

    @Test
    public void testReplaceAllRemovesOldEntries()
    {
        Security security = new Security("KO certificate", "EUR");
        SecurityKnockoutLevel.replaceAll(security,
                        List.of(SecurityKnockoutLevel.of(LocalDate.of(2026, 8, 1), 100.0)));

        SecurityKnockoutLevel.replaceAll(security,
                        List.of(SecurityKnockoutLevel.of(LocalDate.of(2026, 8, 5), 102.0)));

        assertThat(SecurityKnockoutLevel.getLevels(security).size(), is(1));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 4)), is(nullValue()));
        assertThat(SecurityKnockoutLevel.getLevel(security, LocalDate.of(2026, 8, 5)), is(102.0));
    }
}
