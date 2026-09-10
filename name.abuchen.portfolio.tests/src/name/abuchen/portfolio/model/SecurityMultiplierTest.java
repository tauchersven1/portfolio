package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

public class SecurityMultiplierTest
{
    @Test
    public void testDefaultAndDatedValues()
    {
        var security = new Security();
        assertThat(SecurityMultiplier.valueAt(security, LocalDate.of(2026, 1, 1)), is(BigDecimal.ONE));

        security.setPropertyValue(SecurityProperty.Type.FEED, SecurityMultiplier.HISTORY_PROPERTY_NAME,
                        "2026-01-01=10;2026-06-01=100"); //$NON-NLS-1$
        assertThat(SecurityMultiplier.valueAt(security, LocalDate.of(2026, 5, 31)), comparesEqualTo(BigDecimal.TEN));
        assertThat(SecurityMultiplier.valueAt(security, LocalDate.of(2026, 6, 1)),
                        comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void testLegacyAndInvalidValues()
    {
        var security = new Security();
        security.setPropertyValue(SecurityProperty.Type.FEED, SecurityMultiplier.PROPERTY_NAME, "25"); //$NON-NLS-1$
        assertThat(SecurityMultiplier.valueAt(security, LocalDate.now()), is(BigDecimal.valueOf(25)));

        security.setPropertyValue(SecurityProperty.Type.FEED, SecurityMultiplier.HISTORY_PROPERTY_NAME, //$NON-NLS-1$
                        "invalid"); //$NON-NLS-1$
        assertThat(SecurityMultiplier.valueAt(security, LocalDate.now()), is(BigDecimal.ONE));
    }
}
