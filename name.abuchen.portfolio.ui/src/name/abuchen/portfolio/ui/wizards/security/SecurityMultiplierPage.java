package name.abuchen.portfolio.ui.wizards.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityMultiplierPage extends AbstractPage
{
    private final Security security;
    private final List<SecurityMultiplier> multipliers = new ArrayList<>();

    private TableViewer viewer;
    private DateTime effectiveDate;
    private Text multiplierValue;

    public SecurityMultiplierPage(Security security)
    {
        this.security = security;
        security.getMultipliers().stream()
                        .map(m -> new SecurityMultiplier(m.getDate(), m.getValue()))
                        .forEach(multipliers::add);
        setTitle("Multipliers");
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).spacing(8, 8).applyTo(container);

        Label explanation = new Label(container, SWT.WRAP);
        explanation.setText("A multiplier is effective from its date until the next entry. Before the first entry, the multiplier is 1.0.");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);

        viewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 260).applyTo(viewer.getControl());

        TableViewerColumn dateColumn = new TableViewerColumn(viewer, SWT.NONE);
        dateColumn.getColumn().setText("Valid from");
        dateColumn.getColumn().setWidth(180);
        dateColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((SecurityMultiplier) element).getDate());
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.RIGHT);
        valueColumn.getColumn().setText("Multiplier");
        valueColumn.getColumn().setWidth(180);
        valueColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Double.toString(((SecurityMultiplier) element).getMultiplier());
            }
        });

        viewer.setInput(multipliers);
        viewer.addSelectionChangedListener(event -> loadSelection());

        Composite editor = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(5).spacing(8, 0).applyTo(editor);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

        Label dateLabel = new Label(editor, SWT.NONE);
        dateLabel.setText("Valid from");

        effectiveDate = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
        setDate(LocalDate.now());

        Label multiplierLabel = new Label(editor, SWT.NONE);
        multiplierLabel.setText("Multiplier");

        multiplierValue = new Text(editor, SWT.BORDER | SWT.RIGHT);
        multiplierValue.setText("1.0");
        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(multiplierValue);

        Button addOrReplace = new Button(editor, SWT.PUSH);
        addOrReplace.setText("Add / replace");
        addOrReplace.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addOrReplaceMultiplier();
            }
        });

        Composite actions = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(8, 0).applyTo(actions);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(actions);

        Button delete = new Button(actions, SWT.PUSH);
        delete.setText("Delete selected");
        delete.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                deleteSelected();
            }
        });

        Button clear = new Button(actions, SWT.PUSH);
        clear.setText("Clear all");
        clear.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                multipliers.clear();
                viewer.refresh();
            }
        });

        setControl(container);
    }

    private void loadSelection()
    {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        SecurityMultiplier selected = (SecurityMultiplier) selection.getFirstElement();
        setDate(selected.getDate());
        multiplierValue.setText(Double.toString(selected.getMultiplier()));
    }

    private void addOrReplaceMultiplier()
    {
        double value;
        try
        {
            value = Double.parseDouble(multiplierValue.getText().trim().replace(',', '.'));
        }
        catch (NumberFormatException e)
        {
            multiplierValue.setFocus();
            return;
        }

        if (!Double.isFinite(value) || value <= 0)
        {
            multiplierValue.setFocus();
            return;
        }

        LocalDate date = getDate();
        SecurityMultiplier replacement = SecurityMultiplier.of(date, value);

        int index = Collections.binarySearch(multipliers, replacement);
        if (index >= 0)
            multipliers.set(index, replacement);
        else
            multipliers.add(~index, replacement);

        viewer.refresh();
        viewer.setSelection(new org.eclipse.jface.viewers.StructuredSelection(replacement), true);
    }

    private void deleteSelected()
    {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        multipliers.remove(selection.getFirstElement());
        viewer.refresh();
    }

    private LocalDate getDate()
    {
        return LocalDate.of(effectiveDate.getYear(), effectiveDate.getMonth() + 1, effectiveDate.getDay());
    }

    private void setDate(LocalDate date)
    {
        if (effectiveDate != null)
            effectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    public void applyChanges()
    {
        security.removeAllMultipliers();
        multipliers.forEach(m -> security.addMultiplier(new SecurityMultiplier(m.getDate(), m.getValue())));
    }
}
