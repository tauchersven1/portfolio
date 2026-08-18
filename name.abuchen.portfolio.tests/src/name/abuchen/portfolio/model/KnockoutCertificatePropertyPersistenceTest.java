package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

@SuppressWarnings("nls")
public class KnockoutCertificatePropertyPersistenceTest
{
    @Test
    public void testKnockoutMasterDataSurvivesXmlRoundtrip() throws Exception
    {
        Client client = new Client();
        Security certificate = new Security("EUR/JPY K.O.", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "optionProductType", "KNOCK_OUT_CERTIFICATE");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "issuer", "Vontobel");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "issuerProductId", "VH6D6U");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "subscriptionRatio", "100");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxUnderlying", "true");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxBaseCurrency", "EUR");
        certificate.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "fxQuoteCurrency", "JPY");
        client.addSecurity(certificate);

        ClientFactory.XmlSerialization serialization = new ClientFactory.XmlSerialization(false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serialization.save(client, output);

        Client loaded = serialization.load(new InputStreamReader(
                        new ByteArrayInputStream(output.toByteArray()), StandardCharsets.UTF_8));
        Security restored = loaded.getSecurities().stream().filter(s -> "EUR/JPY K.O.".equals(s.getName())).findFirst()
                        .orElseThrow();

        assertProperty(restored, "issuer", "Vontobel");
        assertProperty(restored, "issuerProductId", "VH6D6U");
        assertProperty(restored, "subscriptionRatio", "100");
        assertProperty(restored, "fxUnderlying", "true");
        assertProperty(restored, "fxBaseCurrency", "EUR");
        assertProperty(restored, "fxQuoteCurrency", "JPY");
    }

    private void assertProperty(Security security, String name, String expected)
    {
        assertEquals(expected, security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, name).orElse(null));
    }
}
