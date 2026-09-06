package name.abuchen.portfolio.derivatives.addon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

public class ExposureDialog extends Dialog
{
    private record Row(Security security, Money value, BigDecimal multiplier, Money exposure)
    {
    }

    private final Client client;
    private final ExchangeRateProviderFactory factory;
    private final MPart activePart;

    private TableViewer table;
    private List<Row> rows = List.of();

    public ExposureDialog(Shell parentShell, Client client, ExchangeRateProviderFactory factory, MPart activePart)
    {
        super(parentShell);
        this.client = client;
        this.factory = factory;
        this.activePart = activePart;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText("Exposure Management - Derivatives Add-on"); //$NON-NLS-1$
        newShell.setSize(900, 560);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

        if (client == null || factory == null)
        {
            MessageDialog.openInformation(getShell(), "Exposure Management", //$NON-NLS-1$
                            "Please open a Portfolio Performance file first."); //$NON-NLS-1$
            return container;
        }

        table = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getControl());
        table.setContentProvider(ArrayContentProvider.getInstance());

        addColumn("Instrument", 300, row -> row.security().getName()); //$NON-NLS-1$
        addColumn("Market value", 170, row -> Values.Money.format(row.value())); //$NON-NLS-1$
        addColumn("Multiplier", 120, row -> row.multiplier().toPlainString()); //$NON-NLS-1$
        addColumn("Exposure", 180, row -> Values.Money.format(row.exposure())); //$NON-NLS-1$

        table.getTable().addListener(SWT.MouseDoubleClick, event -> editMultiplier());

        refresh();
        return container;
    }

    private void addColumn(String title, int width, java.util.function.Function<Row, String> value)
    {
        TableViewerColumn column = new TableViewerColumn(table, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return value.apply((Row) element);
            }
        });
    }

    private void refresh()
    {
        CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());

        List<Row> answer = new ArrayList<>();
        snapshot.getAssetPositions().forEach(asset -> addRow(answer, asset));
        rows = answer;
        table.setInput(rows);
    }

    private void addRow(List<Row> answer, AssetPosition asset)
    {
        Security security = asset.getSecurity();
        if (security == null)
            return;

        Money value = asset.getValuation();
        BigDecimal multiplier = AddonMultiplier.get(security);
        Money exposure = value.multiplyAndRound(multiplier.doubleValue());

        answer.add(new Row(security, value, multiplier, exposure));
    }

    private void editMultiplier()
    {
        IStructuredSelection selection = table.getStructuredSelection();
        Row row = (Row) selection.getFirstElement();
        if (row == null)
            return;

        InputDialog dialog = new InputDialog(getShell(), "Multiplier", //$NON-NLS-1$
                        "Multiplier for " + row.security().getName(), row.multiplier().toPlainString(), value -> {
                            try
                            {
                                BigDecimal parsed = new BigDecimal(value);
                                return parsed.signum() > 0 ? null : "Multiplier must be greater than zero"; //$NON-NLS-1$
                            }
                            catch (NumberFormatException e)
                            {
                                return "Please enter a valid number"; //$NON-NLS-1$
                            }
                        });

        if (dialog.open() != Window.OK)
            return;

        BigDecimal multiplier = new BigDecimal(dialog.getValue());
        if (AddonMultiplier.set(row.security(), multiplier))
        {
            client.touch();
            if (activePart != null)
                activePart.setDirty(true);
        }

        refresh();
    }
}
