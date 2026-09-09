package de.venari.portfolio.derivatives;

import java.math.BigDecimal;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.ui.AddonSecurityPage;

public class DerivativeSecurityPage implements AddonSecurityPage
{
    private static final String PREFIX = "derivatives-addon."; //$NON-NLS-1$

    private Security security;
    private Combo instrumentType;
    private Combo putCall;
    private Text underlying;
    private Text contractSymbol;
    private Text expirationDate;
    private Text multiplier;

    @Override
    public String getTitle()
    {
        return "Derivate"; //$NON-NLS-1$
    }

    @Override
    public Control createControl(Composite parent, Client client, Security security)
    {
        this.security = security;

        Composite body = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 12).spacing(10, 8).applyTo(body);

        instrumentType = combo(body, "Instrumententyp", "Kein Derivat", "Option", "Future", "K.O.-Zertifikat"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        putCall = combo(body, "Put / Call", "Nicht angegeben", "Call", "Put"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        underlying = text(body, "Underlying"); //$NON-NLS-1$
        contractSymbol = text(body, "Kontrakt- / Handelssymbol"); //$NON-NLS-1$
        expirationDate = text(body, "Fälligkeit (JJJJ-MM-TT)"); //$NON-NLS-1$
        multiplier = text(body, "Multiplikator"); //$NON-NLS-1$

        select(instrumentType, property("instrumentType"), new String[] { "", "OPTION", "FUTURE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        "KNOCK_OUT_CERTIFICATE" }); //$NON-NLS-1$
        select(putCall, property("putCall"), new String[] { "", "CALL", "PUT" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        underlying.setText(value(property("underlying"))); //$NON-NLS-1$
        contractSymbol.setText(value(property("contractSymbol"))); //$NON-NLS-1$
        expirationDate.setText(value(property("expirationDate"))); //$NON-NLS-1$
        multiplier.setText(AddonMultiplier.get(security).stripTrailingZeros().toPlainString());

        return body;
    }

    private Combo combo(Composite parent, String label, String... values)
    {
        new Label(parent, SWT.NONE).setText(label);
        Combo combo = new Combo(parent, SWT.READ_ONLY);
        combo.setItems(values);
        combo.select(0);
        GridDataFactory.fillDefaults().grab(true, false).hint(260, SWT.DEFAULT).applyTo(combo);
        return combo;
    }

    private Text text(Composite parent, String label)
    {
        new Label(parent, SWT.NONE).setText(label);
        Text text = new Text(parent, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).hint(260, SWT.DEFAULT).applyTo(text);
        return text;
    }

    private void select(Combo combo, String current, String[] storedValues)
    {
        for (int index = 0; index < storedValues.length; index++)
            if (storedValues[index].equalsIgnoreCase(value(current)))
                combo.select(index);
    }

    private String property(String name)
    {
        return security.getPropertyValue(SecurityProperty.Type.FEED, PREFIX + name).orElse(null);
    }

    private String value(String value)
    {
        return value == null ? "" : value; //$NON-NLS-1$
    }

    private void set(String name, String value)
    {
        String normalized = value == null || value.isBlank() ? null : value.trim();
        security.setPropertyValue(SecurityProperty.Type.FEED, PREFIX + name, normalized);
    }

    @Override
    public void applyChanges()
    {
        String[] types = { "", "OPTION", "FUTURE", "KNOCK_OUT_CERTIFICATE" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        String[] directions = { "", "CALL", "PUT" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        set("instrumentType", types[instrumentType.getSelectionIndex()]); //$NON-NLS-1$
        set("putCall", directions[putCall.getSelectionIndex()]); //$NON-NLS-1$
        set("underlying", underlying.getText()); //$NON-NLS-1$
        set("contractSymbol", contractSymbol.getText()); //$NON-NLS-1$
        set("expirationDate", expirationDate.getText()); //$NON-NLS-1$

        try
        {
            BigDecimal value = new BigDecimal(multiplier.getText().trim());
            if (value.signum() > 0)
                AddonMultiplier.set(security, value);
        }
        catch (NumberFormatException ignore)
        {
            // Keep the previously stored multiplier for invalid input.
        }
    }
}
