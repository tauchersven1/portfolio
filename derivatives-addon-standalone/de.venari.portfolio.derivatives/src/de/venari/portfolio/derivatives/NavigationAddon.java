package de.venari.portfolio.derivatives;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;

public class NavigationAddon
{
    @PostConstruct
    public void initialize()
    {
        // active part injection below installs navigation entries
    }

    @Inject
    @Optional
    public void activePartChanged(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        if (part == null || part.getObject() == null)
            return;

        try
        {
            Method getClientInput = part.getObject().getClass().getMethod("getClientInput"); //$NON-NLS-1$
            Object clientInput = getClientInput.invoke(part.getObject());
            if (clientInput == null)
                return;

            Method getNavigation = clientInput.getClass().getMethod("getNavigation"); //$NON-NLS-1$
            Object navigation = getNavigation.invoke(clientInput);
            if (navigation != null)
                install(navigation);
        }
        catch (ReflectiveOperationException ignore)
        {
            // active part is not a PortfolioPart or PP internals changed
        }
    }

    private void install(Object navigation) throws ReflectiveOperationException
    {
        Object reports = findRootByLabel(navigation, "Berichte", "Reports"); //$NON-NLS-1$ //$NON-NLS-2$
        if (reports != null && findChildByLabel(reports, "Exposure") == null) //$NON-NLS-1$
        {
            Object exposureReport = createViewItem("Exposure", ExposureReportView.class); //$NON-NLS-1$
            addChild(reports, exposureReport);
            notifyChanged(navigation, exposureReport);
        }

        Object derivatives = findRootByLabel(navigation, "Derivate"); //$NON-NLS-1$
        if (derivatives == null)
        {
            derivatives = createSectionItem("Derivate"); //$NON-NLS-1$
            addRoot(navigation, derivatives);
        }

        if (findChildByLabel(derivatives, "Exposure Management") == null) //$NON-NLS-1$
        {
            Object management = createViewItem("Exposure Management", ExposureManagementView.class); //$NON-NLS-1$
            addChild(derivatives, management);
            notifyChanged(navigation, management);
        }
        else
        {
            notifyChanged(navigation, derivatives);
        }
    }

    private Object findRootByLabel(Object navigation, String... labels) throws ReflectiveOperationException
    {
        Method getRoots = navigation.getClass().getMethod("getRoots"); //$NON-NLS-1$
        Object result = getRoots.invoke(navigation);
        if (!(result instanceof Stream<?> roots))
            return null;

        try (roots)
        {
            return roots.filter(item -> hasLabel(item, labels)).findFirst().orElse(null);
        }
    }

    private Object findChildByLabel(Object parent, String label) throws ReflectiveOperationException
    {
        Method getChildren = parent.getClass().getMethod("getChildren"); //$NON-NLS-1$
        Object result = getChildren.invoke(parent);
        if (!(result instanceof Stream<?> children))
            return null;

        try (children)
        {
            return children.filter(item -> hasLabel(item, label)).findFirst().orElse(null);
        }
    }

    private boolean hasLabel(Object item, String... labels)
    {
        try
        {
            Method getLabel = item.getClass().getMethod("getLabel"); //$NON-NLS-1$
            Object value = getLabel.invoke(item);
            for (String label : labels)
                if (label.equals(value))
                    return true;
        }
        catch (ReflectiveOperationException ignore)
        {
            // not a navigation item
        }
        return false;
    }

    private Object createSectionItem(String label) throws ReflectiveOperationException
    {
        Class<?> itemClass = Class.forName("name.abuchen.portfolio.ui.editor.Navigation$Item"); //$NON-NLS-1$
        Constructor<?> constructor = itemClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(label);
    }

    private Object createViewItem(String label, Class<?> view) throws ReflectiveOperationException
    {
        Class<?> itemClass = Class.forName("name.abuchen.portfolio.ui.editor.Navigation$Item"); //$NON-NLS-1$
        Constructor<?> constructor = itemClass.getDeclaredConstructor(String.class, Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(label, view);
    }

    private void addChild(Object parent, Object child) throws ReflectiveOperationException
    {
        Method add = parent.getClass().getDeclaredMethod("add", parent.getClass()); //$NON-NLS-1$
        add.setAccessible(true);
        add.invoke(parent, child);
    }

    @SuppressWarnings("unchecked")
    private void addRoot(Object navigation, Object root) throws ReflectiveOperationException
    {
        Field roots = navigation.getClass().getDeclaredField("roots"); //$NON-NLS-1$
        roots.setAccessible(true);
        ((List<Object>) roots.get(navigation)).add(root);
        notifyChanged(navigation, root);
    }

    @SuppressWarnings("unchecked")
    private void notifyChanged(Object navigation, Object item) throws ReflectiveOperationException
    {
        Field listeners = navigation.getClass().getDeclaredField("listeners"); //$NON-NLS-1$
        listeners.setAccessible(true);
        for (Object listener : new ArrayList<>((List<Object>) listeners.get(navigation)))
        {
            Method changed = listener.getClass().getMethod("changed", item.getClass()); //$NON-NLS-1$
            changed.invoke(listener, item);
        }
    }
}
