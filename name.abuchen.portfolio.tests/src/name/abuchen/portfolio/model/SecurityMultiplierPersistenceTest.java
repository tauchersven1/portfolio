package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class SecurityMultiplierPersistenceTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testXmlRoundTrip() throws IOException
    {
        Client client = createClient();
        client.getSaveFlags().clear();
        client.getSaveFlags().add(SaveFlag.XML);

        File file = tempFolder.newFile("multipliers.xml");
        ClientFactory.save(client, file);

        Client loaded;
        try (FileInputStream input = new FileInputStream(file))
        {
            loaded = ClientFactory.load(input);
        }

        assertMultipliers(loaded);
    }

    @Test
    public void testProtobufRoundTrip() throws IOException
    {
        Client client = createClient();
        ProtobufWriter writer = new ProtobufWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.save(client, output);

        Client loaded = writer.load(new ByteArrayInputStream(output.toByteArray()));

        assertMultipliers(loaded);
    }

    private Client createClient()
    {
        Client client = new Client();
        Security security = new Security();
        security.setName("Test Future");
        security.setCurrencyCode("EUR");
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-07-01"), 1125.25));
        client.addSecurity(security);
        return client;
    }

    private void assertMultipliers(Client client)
    {
        assertThat(client.getSecurities().size(), is(1));

        Security security = client.getSecurities().get(0);
        assertThat(security.getMultipliers().size(), is(2));
        assertThat(security.getMultiplier(LocalDate.parse("2025-12-31")), is(1000.0));
        assertThat(security.getMultiplier(LocalDate.parse("2026-05-01")), is(1000.0));
        assertThat(security.getMultiplier(LocalDate.parse("2026-08-01")), is(1125.25));
    }
}
