package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class ClientFactoryTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSecurityMultiplierXmlRoundtrip() throws IOException
    {
        Client client = new Client();
        Security security = new Security();
        security.setName("Future with multiplier");
        security.addMultiplier(SecurityMultiplier.of(LocalDate.of(2025, 1, 1), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 1025.125));
        client.addSecurity(security);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ClientFactory.XmlSerialization(false).save(client, output);

        Client loaded = ClientFactory.load(new ByteArrayInputStream(output.toByteArray()));
        Security loadedSecurity = loaded.getSecurities().get(0);

        assertEquals(2, loadedSecurity.getMultipliers().size());
        assertEquals(1000.0, loadedSecurity.getMultiplier(LocalDate.of(2025, 6, 1)), 0.000001);
        assertEquals(1025.125, loadedSecurity.getMultiplier(LocalDate.of(2026, 8, 1)), 0.000001);
    }

    @Test
    public void testSecurityMultiplierProtobufRoundtrip() throws IOException
    {
        Client client = new Client();
        Security security = new Security();
        security.setName("Future with multiplier");
        security.addMultiplier(SecurityMultiplier.of(LocalDate.of(2025, 1, 1), 1000.0));
        security.addMultiplier(SecurityMultiplier.of(LocalDate.of(2026, 1, 1), 1025.125));
        client.addSecurity(security);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ProtobufWriter().save(client, output);

        Client loaded = new ProtobufWriter().load(new ByteArrayInputStream(output.toByteArray()));
        Security loadedSecurity = loaded.getSecurities().get(0);

        assertEquals(2, loadedSecurity.getMultipliers().size());
        assertEquals(1000.0, loadedSecurity.getMultiplier(LocalDate.of(2025, 6, 1)), 0.000001);
        assertEquals(1025.125, loadedSecurity.getMultiplier(LocalDate.of(2026, 8, 1)), 0.000001);
    }

    @Test
    public void testGetFlagsForZipFile() throws IOException
    {
        var zipFile = tempFolder.newFile("test.zip");
        var flags = ClientFactory.getFlags(zipFile);

        assertEquals(EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForZipFileCaseInsensitive() throws IOException
    {
        var zipFile = tempFolder.newFile("test.ZIP");
        var flags = ClientFactory.getFlags(zipFile);

        assertEquals(EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForEncryptedPortfolio() throws IOException
    {
        var encryptedFile = tempFolder.newFile("test.portfolio");

        // Write encrypted signature (PORTFOLIO in bytes)
        try (var out = new FileOutputStream(encryptedFile))
        {
            out.write(new byte[] { 80, 79, 82, 84, 70, 79, 76, 73, 79 });
        }

        var flags = ClientFactory.getFlags(encryptedFile);
        assertEquals(EnumSet.of(SaveFlag.ENCRYPTED), flags);
    }

    @Test
    public void testGetFlagsForCompressedPortfolio() throws IOException
    {
        var compressedFile = tempFolder.newFile("test.portfolio");

        // Write ZIP signature (PK\x03\x04)
        try (var out = new FileOutputStream(compressedFile))
        {
            out.write(new byte[] { 80, 75, 3, 4 });
        }

        var flags = ClientFactory.getFlags(compressedFile);
        assertEquals(EnumSet.of(SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForAribitraryPortfolio() throws IOException
    {
        var compressedFile = tempFolder.newFile("arbitrary.portfolio");

        // Write ZIP signature (PK\x03\x04)
        try (var out = new FileOutputStream(compressedFile))
        {
            out.write(new byte[] { 44, 45, 43, 46 });
        }

        var flags = ClientFactory.getFlags(compressedFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithoutIdReferences() throws IOException
    {
        var xmlFile = tempFolder.newFile("test.xml");

        try (var out = new FileOutputStream(xmlFile))
        {
            out.write("<client><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(xmlFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithIdReferences() throws IOException
    {
        var xmlFile = tempFolder.newFile("test.xml");

        try (var out = new FileOutputStream(xmlFile))
        {
            out.write("<client id=\"123\">".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(xmlFile);
        assertTrue(flags.contains(SaveFlag.XML));
        assertTrue(flags.contains(SaveFlag.ID_REFERENCES));
    }

    @Test
    public void testGetFlagsForEmptyFile() throws IOException
    {
        var emptyFile = tempFolder.newFile("empty.dat");

        var flags = ClientFactory.getFlags(emptyFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("noextension");

        try (var out = new FileOutputStream(file))
        {
            out.write("<client><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForEncryptedFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("encrypted");

        // Write encrypted signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 80, 79, 82, 84, 70, 79, 76, 73, 79 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.ENCRYPTED), flags);
    }

    @Test
    public void testGetFlagsForCompressedFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("compressed");

        // Write ZIP signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 80, 75, 3, 4 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForUnknownFileContent() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        // Write some random content that doesn't match any signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 1, 2, 3, 4, 5 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForUnknownShortFileContent() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        // Write some random content that doesn't match any signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 1 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithIdReferencesInFallback() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        try (var out = new FileOutputStream(file))
        {
            out.write("<client id=\"1\"><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(file);
        assertTrue(flags.contains(SaveFlag.XML));
        assertTrue(flags.contains(SaveFlag.ID_REFERENCES));
    }
}
