package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityDerivativePropertyTest
{
    @Test
    public void testDerivativeProperties()
    {
        Security security = new Security();

        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall", "CALL");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike", "5250");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "exerciseStyle", "EUROPEAN");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "expirationDate", "2026-12-18");
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "settlementType", "CASH");

        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "type").orElseThrow(), is("OPTION"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "putCall").orElseThrow(), is("CALL"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "strike").orElseThrow(), is("5250"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "exerciseStyle").orElseThrow(),
                        is("EUROPEAN"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "expirationDate").orElseThrow(),
                        is("2026-12-18"));
        assertThat(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "settlementType").orElseThrow(),
                        is("CASH"));

        assertThat(security.getPropertyValue(SecurityProperty.Type.FEED, "type").isEmpty(), is(true));
    }
}