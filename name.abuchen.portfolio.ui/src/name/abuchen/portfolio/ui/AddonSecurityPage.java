package name.abuchen.portfolio.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/** Public contract for pages contributed to the security editor by add-ons. */
public interface AddonSecurityPage
{
    String getTitle();

    Control createControl(Composite parent, Client client, Security security);

    default void applyChanges()
    {
        // optional
    }
}
