package name.abuchen.portfolio.derivatives.addon;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
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
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, @Optional Client client)
    {
        MPart activePart = partService.getActivePart();
        new ExposureDialog(shell, client, factory, activePart).open();
    }
}
