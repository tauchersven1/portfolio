package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;

@SuppressWarnings("nls")
public class CSVPortfolioTransactionMultiplierTest
{
    @Test
    public void testExplicitCsvMultiplierOverridesMasterData()
    {
        Security security = securityWithMultiplier(10.0);
        importTransaction(security, "25");
        assertThat(security.getMultiplier(LocalDate.parse("2026-06-01")), is(25.0));
    }

    @Test
    public void testMasterDataMultiplierUsedWhenCsvMultiplierMissing()
    {
        Security security = securityWithMultiplier(10.0);
        importTransaction(security, null);
        assertThat(security.getMultiplier(LocalDate.parse("2026-06-01")), is(10.0));
    }

    @Test
    public void testOneUsedWhenCsvAndMasterDataMultiplierMissing()
    {
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        importTransaction(security, null);
        assertThat(security.getMultiplier(LocalDate.parse("2026-06-01")), is(1.0));
    }

    private Security securityWithMultiplier(double multiplier)
    {
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        security.addMultiplier(SecurityMultiplier.of(LocalDate.parse("2026-01-01"), multiplier));
        return security;
    }

    private void importTransaction(Security security, String multiplier)
    {
        Client client = new Client();
        client.addSecurity(security);
        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);
        List<Exception> errors = new ArrayList<>();
        String[] row = new String[] { "2026-06-01", "", "", "SAP.DE", "", "SAP", "100", "EUR", "", "",
                        "", "", "", "1", "BUY", "", "", "", "", multiplier };
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(row), buildField2Column(extractor), errors);
        assertThat(errors, empty());
        assertThat(results.size(), is(1));
    }
}
