package de.venari.portfolio.derivatives;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.Navigation;

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
            Method getClientInput = part.getObject().getClass().getMethod("getClientInput");
            Object clientInput = getClientInput.invoke(part.getObject());
            if (clientInput == null)
                return;

            Method getNavigation = clientInput.getClass().getMethod("getNavigation");
            Object value = getNavigation.invoke(clientInput);
            if (value instanceof Navigation navigation)
                install(navigation);
        }
        catch (ReflectiveOperationException ignore)
        {
            // active part is not a PortfolioPart
        }
    }

    private void install(Navigation navigation) throws ReflectiveOperationException
    {
        Navigation.Item reports = navigation.getRoots()
                        .filter(item -> "Berichte".equals(item.getLabel()) || "Reports".equals(item.getLabel()))
                        .findFirst().orElse(null);

        if (reports != null && reports.getChildren().noneMatch(item -> "Exposure".equals(item.getLabel())))
        {
            Navigation.Item exposureReport = createViewItem("Exposure", ExposureReportView.class);
            addChild(reports, exposureReport);
            notifyChanged(navigation, exposureReport);
        }

        Navigation.Item derivatives = navigation.getRoots()
                        .filter(item -> "Derivate".equals(item.getLabel()))
                        .findFirst().orElse(null);

        if (derivatives == null)
        {
            derivatives = createSectionItem("Derivate");
            addRoot(navigation, derivatives);
        }

        if (derivatives.getChildren().noneMatch(item -> "Exposure Management".equals(item.getLabel())))
        {
            Navigation.Item management = createViewItem("Exposure Management", ExposureManagementView.class);
            addChild(derivatives, management);
            notifyChanged(navigation, management);
        }
        else
        {
            notifyChanged(navigation, derivatives);
        }
    }

    private Navigation.Item createSectionItem(String label) throws ReflectiveOperationException
    {
        Constructor<Navigation.Item> constructor = Navigation.Item.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(label);
    }

    private Navigation.Item createViewItem(String label, Class<? extends AbstractFinanceView> view)
                    throws ReflectiveOperationException
    {
        Constructor<Navigation.Item> constructor = Navigation.Item.class.getDeclaredConstructor(String.class, Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(label, view);
    }

    private void addChild(Navigation.Item parent, Navigation.Item child) throws ReflectiveOperationException
    {
        Method add = Navigation.Item.class.getDeclaredMethod("add", Navigation.Item.class);
        add.setAccessible(true);
        add.invoke(parent, child);
    }

    @SuppressWarnings("unchecked")
    private void addRoot(Navigation navigation, Navigation.Item root) throws ReflectiveOperationException
    {
        Field roots = Navigation.class.getDeclaredField("roots");
        roots.setAccessible(true);
        ((List<Navigation.Item>) roots.get(navigation)).add(root);
        notifyChanged(navigation, root);
    }

    @SuppressWarnings("unchecked")
    private void notifyChanged(Navigation navigation, Navigation.Item item) throws ReflectiveOperationException
    {
        Field listeners = Navigation.class.getDeclaredField("listeners");
        listeners.setAccessible(true);
        for (Navigation.Listener listener : List.copyOf((List<Navigation.Listener>) listeners.get(navigation)))
            listener.changed(item);
    }
}
