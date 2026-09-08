package name.abuchen.portfolio.ui.editor;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.AddonView;
import name.abuchen.portfolio.ui.AddonViewContext;
import name.abuchen.portfolio.ui.UIConstants;

public class AddonFinanceView extends AbstractFinanceView
{
    private final AddonViewDescriptor descriptor;
    private AddonView delegate;

    @Inject
    public AddonFinanceView(IEclipseContext context)
    {
        this.descriptor = (AddonViewDescriptor) context.get(UIConstants.Parameter.VIEW_PARAMETER);
    }

    @Override
    protected String getDefaultTitle()
    {
        return descriptor.label();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        try
        {
            delegate = descriptor.viewClass().getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Cannot create add-on view " + descriptor.id(), e); //$NON-NLS-1$
        }

        AddonViewContext addonContext = new AddonViewContext()
        {
            @Override
            public name.abuchen.portfolio.model.Client getClient()
            {
                return AddonFinanceView.this.getClient();
            }

            @Override
            public ExchangeRateProviderFactory getExchangeRateProviderFactory()
            {
                return AddonFinanceView.this.getFromContext(ExchangeRateProviderFactory.class);
            }

            @Override
            public org.eclipse.swt.widgets.Shell getShell()
            {
                return AddonFinanceView.this.getActiveShell();
            }

            @Override
            public void markDirty()
            {
                AddonFinanceView.this.markDirty();
            }
        };

        return delegate.createBody(parent, addonContext);
    }

    @Override
    public void notifyModelUpdated()
    {
        if (delegate != null)
            delegate.notifyModelUpdated();
    }
}
