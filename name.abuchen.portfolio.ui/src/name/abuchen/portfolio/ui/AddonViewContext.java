package name.abuchen.portfolio.ui;

import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * Stable context exposed to externally contributed add-on views.
 */
public interface AddonViewContext
{
    Client getClient();

    ExchangeRateProviderFactory getExchangeRateProviderFactory();

    Shell getShell();

    void markDirty();
}
