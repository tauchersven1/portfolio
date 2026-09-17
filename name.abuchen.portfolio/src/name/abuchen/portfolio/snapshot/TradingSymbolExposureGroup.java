package name.abuchen.portfolio.snapshot;

import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.OptionSymbolParser;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;

public final class TradingSymbolExposureGroup
{
    private static final String CONTRACT_SYMBOL = "contractSymbol"; //$NON-NLS-1$
    private static final String NO_TRADING_SYMBOL = "No trading symbol"; //$NON-NLS-1$

    public record SymbolGroup(String identity, String label)
    {
    }

    private TradingSymbolExposureGroup()
    {
    }

    public static SymbolGroup resolve(Client client, Security security)
    {
        if (DerivativePositionCalculator.getDerivativeType(security) == null)
            return forSecurity(security);

        Security underlying = DerivativePositionCalculator.resolveUnderlying(client, security);
        if (underlying != null)
            return forSecurity(underlying);

        String symbol = derivativeSymbol(security);
        Security symbolUnderlying = findUniqueUnderlying(client, security, symbol);
        if (symbolUnderlying != null)
            return forSecurity(symbolUnderlying);

        if (symbol == null || symbol.isBlank())
            return new SymbolGroup("security:" + security.getUUID(), NO_TRADING_SYMBOL); //$NON-NLS-1$

        return new SymbolGroup("symbol:" + symbol.toUpperCase(Locale.ROOT), symbol); //$NON-NLS-1$
    }

    private static SymbolGroup forSecurity(Security security)
    {
        String ticker = security.getTickerSymbol();
        String label = ticker == null || ticker.isBlank() ? NO_TRADING_SYMBOL : ticker.trim();
        return new SymbolGroup("security:" + security.getUUID(), label); //$NON-NLS-1$
    }

    private static String derivativeSymbol(Security security)
    {
        String contractSymbol = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, CONTRACT_SYMBOL)
                        .orElse(null);
        if (contractSymbol != null && !contractSymbol.isBlank())
        {
            String trimmed = contractSymbol.trim();
            return OptionSymbolParser.parse(trimmed).map(OptionSymbolParser.OptionData::getUnderlying).orElse(trimmed);
        }

        String ticker = security.getTickerSymbol();
        if (ticker == null || ticker.isBlank())
            return null;
        String trimmed = ticker.trim();
        return OptionSymbolParser.parse(trimmed).map(OptionSymbolParser.OptionData::getUnderlying).orElse(trimmed);
    }

    private static Security findUniqueUnderlying(Client client, Security derivative, String symbol)
    {
        if (symbol == null || symbol.isBlank())
            return null;

        List<Security> matches = client.getSecurities().stream().filter(candidate -> candidate != derivative)
                        .filter(candidate -> DerivativePositionCalculator.getDerivativeType(candidate) == null)
                        .filter(candidate -> candidate.getTickerSymbol() != null)
                        .filter(candidate -> symbol.equalsIgnoreCase(candidate.getTickerSymbol().trim())).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }
}
