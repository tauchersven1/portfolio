package name.abuchen.portfolio.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Public UI contract for views contributed by external Portfolio Performance add-ons.
 */
public interface AddonView
{
    Control createBody(Composite parent, AddonViewContext context);

    default void notifyModelUpdated()
    {
        // optional
    }
}
