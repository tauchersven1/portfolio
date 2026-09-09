package de.venari.portfolio.derivatives;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.dialogs.InputDialog;
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

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AddonView;
import name.abuchen.portfolio.ui.AddonViewContext;

public class ExposureManagementView implements AddonView
{
    private record Row(Security security, Money marketValue, BigDecimal multiplier, Money exposure)
    {
    }

    private AddonViewContext context;
    private TableViewer table;

    @Override
    public Control createBody(Composite parent, AddonViewContext context)
    {
        this.context = context;
        Composite body = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(body);

        table = new TableViewer(body, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);
        table.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getControl());

        addColumn("Instrument", 320, row -> row.security().getName()); //$NON-NLS-1$
        addColumn("Market value", 160, row -> Values.Money.format(row.marketValue())); //$NON-NLS-1$
        addColumn("Multiplier", 120, row -> row.multiplier().toPlainString()); //$NON-NLS-1$
        addColumn("Exposure", 160, row -> Values.Money.format(row.exposure())); //$NON-NLS-1$

        table.getTable().addListener(SWT.MouseDoubleClick, e -> editMultiplier());
        refresh();
        return body;
    }

    @Override
    public void notifyModelUpdated()
    {
        refresh();
    }

    private void addColumn(String title, int width, Function<Row, String> value)
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
        if (table == null || table.getControl().isDisposed())
            return;

        LocalDate date = LocalDate.now();
        CurrencyConverter converter = new CurrencyConverterImpl(context.getExchangeRateProviderFactory(), context.getClient().getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(context.getClient(), converter, date);

        List<Row> rows = new ArrayList<>();
        snapshot.getAssetPositions().forEach(asset -> addRow(rows, asset));
        table.setInput(rows);
    }

    private void addRow(List<Row> rows, AssetPosition asset)
    {
        Security security = asset.getSecurity();
        if (security == null)
            return;

        Money marketValue = asset.getValuation();
        BigDecimal multiplier = AddonMultiplier.get(security);
        Money exposure = marketValue.multiplyAndRound(multiplier.doubleValue());
        rows.add(new Row(security, marketValue, multiplier, exposure));
    }

    private void editMultiplier()
    {
        IStructuredSelection selection = table.getStructuredSelection();
        Row row = (Row) selection.getFirstElement();
        if (row == null)
            return;

        InputDialog dialog = new InputDialog(context.getShell(), "Multiplier", //$NON-NLS-1$
                        "Multiplier for " + row.security().getName(), row.multiplier().toPlainString(), value -> { //$NON-NLS-1$
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

        if (AddonMultiplier.set(row.security(), new BigDecimal(dialog.getValue())))
        {
            context.getClient().touch();
            context.markDirty();
        }

        refresh();
    }
}
