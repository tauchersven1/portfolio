package de.venari.portfolio.derivatives;

import java.lang.reflect.Method;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

public class OpenExposureHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        Client client = extractClient(part);
        ExchangeRateProviderFactory factory = extractFactory(part);

        if (client == null || factory == null)
        {
            MessageDialog.openError(shell, "Derivatives Add-on",
                            "The active Portfolio Performance file context could not be resolved.");
            return;
        }

        new ExposureDialog(shell, client, factory, part).open();
    }

    private Client extractClient(MPart part)
    {
        Object partObject = part != null ? part.getObject() : null;
        if (partObject == null)
            return null;

        try
        {
            Method getClient = partObject.getClass().getMethod("getClient");
            Object result = getClient.invoke(partObject);
            if (result instanceof Client client)
                return client;
        }
        catch (ReflectiveOperationException ignore)
        {
            // fall through to ClientInput
        }

        Object clientInput = extractClientInput(partObject);
        if (clientInput == null)
            return null;

        try
        {
            Method getClient = clientInput.getClass().getMethod("getClient");
            Object result = getClient.invoke(clientInput);
            return result instanceof Client client ? client : null;
        }
        catch (ReflectiveOperationException ignore)
        {
            return null;
        }
    }

    private ExchangeRateProviderFactory extractFactory(MPart part)
    {
        Object partObject = part != null ? part.getObject() : null;
        Object clientInput = extractClientInput(partObject);
        if (clientInput == null)
            return null;

        try
        {
            Method getFactory = clientInput.getClass().getMethod("getExchangeRateProviderFacory");
            Object result = getFactory.invoke(clientInput);
            return result instanceof ExchangeRateProviderFactory factory ? factory : null;
        }
        catch (ReflectiveOperationException ignore)
        {
            return null;
        }
    }

    private Object extractClientInput(Object partObject)
    {
        if (partObject == null)
            return null;

        for (String methodName : new String[] { "getClientInput", "getClient" })
        {
            try
            {
                Method method = partObject.getClass().getMethod(methodName);
                Object result = method.invoke(partObject);
                if (result != null && !(result instanceof Client))
                    return result;
            }
            catch (ReflectiveOperationException ignore)
            {
                // try next accessor
            }
        }

        return null;
    }
}
