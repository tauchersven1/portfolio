package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.DerivativeMasterDataProvider.Result;
import name.abuchen.portfolio.online.impl.VontobelDerivativeMasterDataProvider;

/**
 * Central entry point for derivative master-data enrichment.
 */
public final class DerivativeMasterDataLookup
{
    private static final List<DerivativeMasterDataProvider> PROVIDERS = List.of(
                    new VontobelDerivativeMasterDataProvider());

    private DerivativeMasterDataLookup()
    {
    }

    public static Optional<Result> lookup(Security security) throws IOException
    {
        IOException last = null;
        for (DerivativeMasterDataProvider provider : PROVIDERS)
        {
            try
            {
                Optional<Result> result = provider.lookup(security);
                if (result.isPresent() && !result.get().isEmpty())
                    return result;
            }
            catch (IOException e)
            {
                last = e;
            }
        }

        if (last != null)
            throw last;
        return Optional.empty();
    }
}
