package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;

@SuppressWarnings("nls")
public class TradingSymbolExposureGroupTest
{
    @Test
    public void testStockAndLinkedOptionUseSameUnderlyingGroup()
    {
        Client client = new Client();
        Security stock = security("Microsoft", "MSFT");
        client.addSecurity(stock);

        Security option = option("MSFT260918C00490000");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "underlyingSecurityUUID", stock.getUUID());
        client.addSecurity(option);

        TradingSymbolExposureGroup.Group stockGroup = TradingSymbolExposureGroup.resolve(client, stock);
        TradingSymbolExposureGroup.Group optionGroup = TradingSymbolExposureGroup.resolve(client, option);

        assertThat(optionGroup, is(stockGroup));
        assertThat(optionGroup.label(), is("MSFT"));
    }

    @Test
    public void testOccSymbolFindsUniqueUnderlyingWithoutStoredMapping()
    {
        Client client = new Client();
        Security stock = security("Microsoft", "MSFT");
        client.addSecurity(stock);
        Security option = option("MSFT260918C00490000");
        client.addSecurity(option);

        assertThat(TradingSymbolExposureGroup.resolve(client, option),
                        is(TradingSymbolExposureGroup.resolve(client, stock)));
    }

    @Test
    public void testEncodedContractSymbolIsParsedBeforeFallback()
    {
        Client client = new Client();
        Security stock = security("Microsoft", "MSFT");
        client.addSecurity(stock);
        Security option = option("MSFT260918C00490000");
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "contractSymbol", "MSFT260918C00490000");
        client.addSecurity(option);

        assertThat(TradingSymbolExposureGroup.resolve(client, option),
                        is(TradingSymbolExposureGroup.resolve(client, stock)));
    }

    @Test
    public void testAmbiguousTickerDoesNotSelectAnArbitraryUnderlying()
    {
        Client client = new Client();
        client.addSecurity(security("Microsoft US", "MSFT"));
        client.addSecurity(security("Microsoft duplicate", "MSFT"));
        Security option = option("MSFT260918C00490000");
        client.addSecurity(option);

        TradingSymbolExposureGroup.Group group = TradingSymbolExposureGroup.resolve(client, option);

        assertThat(group.identity(), is("symbol:MSFT"));
        assertThat(group.label(), is("MSFT"));
    }

    private static Security security(String name, String ticker)
    {
        Security security = new Security(name, "USD");
        security.setTickerSymbol(ticker);
        return security;
    }

    private static Security option(String ticker)
    {
        Security option = security("Microsoft Call", ticker);
        option.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "type", "OPTION");
        return option;
    }
}
