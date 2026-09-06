package name.abuchen.portfolio.derivatives.addon;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

public class OpenExposureHandler
{
    @Execute
    public void execute(EPartService partService, ExchangeRateProviderFactory factory,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        MPart activePart = partService.getActivePart();

        if (activePart == null || activePart.getContext() == null)
        {
            new ExposureDialog(shell, null, null, null).open();
            return;
        }

        Client client = activePart.getContext().get(Client.class);
        new ExposureDialog(shell, client, factory, activePart).open();
    }
}
