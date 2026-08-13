package name.abuchen.portfolio.ui.wizards.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityMultiplierPage extends AbstractPage
{
    private static final String TYPE = "type";
    private static final String UNDERLYING = "underlying";
    private static final String EXPIRATION_DATE = "expirationDate";
    private static final String LAST_TRADING_DAY = "lastTradingDay";
    private static final String SETTLEMENT_TYPE = "settlementType";
    private static final String SETTLEMENT_DATE = "settlementDate";
    private static final String EXCHANGE = "exchange";
    private static final String CONTRACT_SYMBOL = "contractSymbol";
    private static final String CONTRACT_SIZE = "contractSize";
    private static final String TICK_SIZE = "tickSize";

    private static final String PUT_CALL = "putCall";
    private static final String STRIKE = "strike";
    private static final String EXERCISE_STYLE = "exerciseStyle";

    private static final String CONTRACT_MONTH = "contractMonth";
    private static final String FIRST_NOTICE_DAY = "firstNoticeDay";
    private static final String FINAL_SETTLEMENT_DATE = "finalSettlementDate";

    private static final String[] DERIVATIVE_PROPERTIES = { TYPE, UNDERLYING, EXPIRATION_DATE, LAST_TRADING_DAY,
                    SETTLEMENT_TYPE, SETTLEMENT_DATE, EXCHANGE, CONTRACT_SYMBOL, CONTRACT_SIZE, TICK_SIZE, PUT_CALL,
                    STRIKE, EXERCISE_STYLE, CONTRACT_MONTH, FIRST_NOTICE_DAY, FINAL_SETTLEMENT_DATE };

    private final Security security;
    private final List<SecurityMultiplier> multipliers = new ArrayList<>();

    private Combo derivativeType;
    private Text underlying;
    private Combo settlementType;
    private Text exchange;
    private Text contractSymbol;
    private Text contractSize;
    private Text tickSize;
    private OptionalDateField expirationDate;
    private OptionalDateField lastTradingDay;
    private OptionalDateField settlementDate;

    private Group optionGroup;
    private Combo putCall;
    private Text strike;
    private Combo exerciseStyle;

    private Group futureGroup;
    private Text contractMonth;
    private OptionalDateField firstNoticeDay;
    private OptionalDateField finalSettlementDate;

    private Composite commonFields;

    private TableViewer viewer;
    private DateTime effectiveDate;
    private Text multiplierValue;

    public SecurityMultiplierPage(Security security)
    {
        this.security = security;
        security.getMultipliers().stream().map(m -> new SecurityMultiplier(m.getDate(), m.getValue()))
                        .forEach(multipliers::add);
        setTitle("Derivatives");
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).spacing(8, 8).applyTo(container);

        createDerivativeMasterData(container);
        createMultiplierSection(container);

        loadDerivativeData();
        updateDerivativeControls();

        setControl(container);
    }

    private void createDerivativeMasterData(Composite container)
    {
        Composite typeRow = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(8, 0).applyTo(typeRow);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(typeRow);

        new Label(typeRow, SWT.NONE).setText("Derivative type");
        derivativeType = new Combo(typeRow, SWT.READ_ONLY);
        derivativeType.setItems("Not a derivative", "Future", "Option");
        derivativeType.select(0);
        derivativeType.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateDerivativeControls();
            }
        });

        commonFields = new Group(container, SWT.NONE);
        ((Group) commonFields).setText("Contract data");
        GridLayoutFactory.fillDefaults().numColumns(4).margins(8, 8).spacing(8, 6).applyTo(commonFields);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(commonFields);

        new Label(commonFields, SWT.NONE).setText("Underlying");
        underlying = new Text(commonFields, SWT.BORDER);
        underlying.setMessage("Security name, ticker or UUID");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(underlying);

        new Label(commonFields, SWT.NONE).setText("Exchange");
        exchange = new Text(commonFields, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(exchange);

        new Label(commonFields, SWT.NONE).setText("Contract / trading symbol");
        contractSymbol = new Text(commonFields, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(contractSymbol);

        new Label(commonFields, SWT.NONE).setText("Settlement");
        settlementType = new Combo(commonFields, SWT.READ_ONLY);
        settlementType.setItems("Not specified", "Cash", "Physical");
        settlementType.select(0);

        new Label(commonFields, SWT.NONE).setText("Contract size");
        contractSize = new Text(commonFields, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(contractSize);

        new Label(commonFields, SWT.NONE).setText("Tick size");
        tickSize = new Text(commonFields, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(tickSize);

        expirationDate = new OptionalDateField(commonFields, "Expiration");
        lastTradingDay = new OptionalDateField(commonFields, "Last trading day");
        settlementDate = new OptionalDateField(commonFields, "Settlement date");

        optionGroup = new Group(container, SWT.NONE);
        optionGroup.setText("Option");
        GridLayoutFactory.fillDefaults().numColumns(4).margins(8, 8).spacing(8, 6).applyTo(optionGroup);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(optionGroup);

        new Label(optionGroup, SWT.NONE).setText("Put / Call");
        putCall = new Combo(optionGroup, SWT.READ_ONLY);
        putCall.setItems("Not specified", "Call", "Put");
        putCall.select(0);

        new Label(optionGroup, SWT.NONE).setText("Strike");
        strike = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(strike);

        new Label(optionGroup, SWT.NONE).setText("Exercise style");
        exerciseStyle = new Combo(optionGroup, SWT.READ_ONLY);
        exerciseStyle.setItems("Not specified", "European", "American", "Bermudan");
        exerciseStyle.select(0);

        futureGroup = new Group(container, SWT.NONE);
        futureGroup.setText("Future");
        GridLayoutFactory.fillDefaults().numColumns(4).margins(8, 8).spacing(8, 6).applyTo(futureGroup);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(futureGroup);

        new Label(futureGroup, SWT.NONE).setText("Contract month");
        contractMonth = new Text(futureGroup, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(contractMonth);

        firstNoticeDay = new OptionalDateField(futureGroup, "First notice day");
        finalSettlementDate = new OptionalDateField(futureGroup, "Final settlement date");
    }

    private void createMultiplierSection(Composite container)
    {
        Group multiplierGroup = new Group(container, SWT.NONE);
        multiplierGroup.setText("Multiplier history");
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(multiplierGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(multiplierGroup);

        Label explanation = new Label(multiplierGroup, SWT.WRAP);
        explanation.setText("A multiplier is effective from its date until the next entry. "
                        + "Before the first entry, the multiplier is 1.0.");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);

        viewer = new TableViewer(multiplierGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 150).applyTo(viewer.getControl());

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

        Composite editor = new Composite(multiplierGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(5).spacing(8, 0).applyTo(editor);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

        new Label(editor, SWT.NONE).setText("Valid from");
        effectiveDate = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
        setDate(LocalDate.now());

        new Label(editor, SWT.NONE).setText("Multiplier");
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

        Composite actions = new Composite(multiplierGroup, SWT.NONE);
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
                if (multipliers.isEmpty() || MessageDialog.openConfirm(getShell(), "Clear multipliers",
                                "Remove all multiplier entries?"))
                {
                    multipliers.clear();
                    viewer.refresh();
                }
            }
        });
    }

    private void loadDerivativeData()
    {
        selectByValue(derivativeType, property(TYPE), "FUTURE", "OPTION");
        selectByValue(settlementType, property(SETTLEMENT_TYPE), "CASH", "PHYSICAL");
        selectByValue(putCall, property(PUT_CALL), "CALL", "PUT");
        selectByValue(exerciseStyle, property(EXERCISE_STYLE), "EUROPEAN", "AMERICAN", "BERMUDAN");

        underlying.setText(valueOrEmpty(property(UNDERLYING)));
        exchange.setText(valueOrEmpty(property(EXCHANGE)));
        contractSymbol.setText(valueOrEmpty(property(CONTRACT_SYMBOL)));
        contractSize.setText(valueOrEmpty(property(CONTRACT_SIZE)));
        tickSize.setText(valueOrEmpty(property(TICK_SIZE)));
        strike.setText(valueOrEmpty(property(STRIKE)));
        contractMonth.setText(valueOrEmpty(property(CONTRACT_MONTH)));

        expirationDate.setValue(property(EXPIRATION_DATE));
        lastTradingDay.setValue(property(LAST_TRADING_DAY));
        settlementDate.setValue(property(SETTLEMENT_DATE));
        firstNoticeDay.setValue(property(FIRST_NOTICE_DAY));
        finalSettlementDate.setValue(property(FINAL_SETTLEMENT_DATE));
    }

    private void updateDerivativeControls()
    {
        boolean isDerivative = derivativeType.getSelectionIndex() > 0;
        boolean isFuture = derivativeType.getSelectionIndex() == 1;
        boolean isOption = derivativeType.getSelectionIndex() == 2;

        setEnabledRecursive(commonFields, isDerivative);
        setEnabledRecursive(futureGroup, isFuture);
        setEnabledRecursive(optionGroup, isOption);

        expirationDate.updateEnabled(isDerivative);
        lastTradingDay.updateEnabled(isDerivative);
        settlementDate.updateEnabled(isDerivative);
        firstNoticeDay.updateEnabled(isFuture);
        finalSettlementDate.updateEnabled(isFuture);
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
            showInvalidMultiplier();
            return;
        }

        if (!Double.isFinite(value) || value <= 0)
        {
            showInvalidMultiplier();
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
        viewer.setSelection(new StructuredSelection(replacement), true);
    }

    private void showInvalidMultiplier()
    {
        MessageDialog.openError(getShell(), "Invalid multiplier", "Enter a positive numeric multiplier.");
        multiplierValue.setFocus();
        multiplierValue.selectAll();
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

        int typeIndex = derivativeType.getSelectionIndex();
        if (typeIndex <= 0)
        {
            for (String name : DERIVATIVE_PROPERTIES)
                setProperty(name, null);
            return;
        }

        setProperty(TYPE, typeIndex == 1 ? "FUTURE" : "OPTION");
        setProperty(UNDERLYING, text(underlying));
        setProperty(EXPIRATION_DATE, expirationDate.getValue());
        setProperty(LAST_TRADING_DAY, lastTradingDay.getValue());
        setProperty(SETTLEMENT_DATE, settlementDate.getValue());
        setProperty(SETTLEMENT_TYPE, comboValue(settlementType, "CASH", "PHYSICAL"));
        setProperty(EXCHANGE, text(exchange));
        setProperty(CONTRACT_SYMBOL, text(contractSymbol));
        setProperty(CONTRACT_SIZE, text(contractSize));
        setProperty(TICK_SIZE, text(tickSize));

        if (typeIndex == 2)
        {
            setProperty(PUT_CALL, comboValue(putCall, "CALL", "PUT"));
            setProperty(STRIKE, text(strike));
            setProperty(EXERCISE_STYLE, comboValue(exerciseStyle, "EUROPEAN", "AMERICAN", "BERMUDAN"));
            setProperty(CONTRACT_MONTH, null);
            setProperty(FIRST_NOTICE_DAY, null);
            setProperty(FINAL_SETTLEMENT_DATE, null);
        }
        else
        {
            setProperty(PUT_CALL, null);
            setProperty(STRIKE, null);
            setProperty(EXERCISE_STYLE, null);
            setProperty(CONTRACT_MONTH, text(contractMonth));
            setProperty(FIRST_NOTICE_DAY, firstNoticeDay.getValue());
            setProperty(FINAL_SETTLEMENT_DATE, finalSettlementDate.getValue());
        }
    }

    private String property(String name)
    {
        return security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, name).orElse(null);
    }

    private void setProperty(String name, String value)
    {
        security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, name,
                        value == null || value.isBlank() ? null : value.trim());
    }

    private static String text(Text control)
    {
        String value = control.getText().trim();
        return value.isEmpty() ? null : value;
    }

    private static String valueOrEmpty(String value)
    {
        return value == null ? "" : value;
    }

    private static void selectByValue(Combo combo, String storedValue, String... values)
    {
        combo.select(0);
        if (storedValue == null)
            return;

        for (int ii = 0; ii < values.length; ii++)
        {
            if (values[ii].equals(storedValue))
            {
                combo.select(ii + 1);
                return;
            }
        }
    }

    private static String comboValue(Combo combo, String... values)
    {
        int index = combo.getSelectionIndex();
        return index > 0 && index <= values.length ? values[index - 1] : null;
    }

    private static void setEnabledRecursive(Composite composite, boolean enabled)
    {
        composite.setEnabled(enabled);
        for (Control child : composite.getChildren())
        {
            child.setEnabled(enabled);
            if (child instanceof Composite childComposite)
                setEnabledRecursive(childComposite, enabled);
        }
    }

    private static final class OptionalDateField
    {
        private final Button enabled;
        private final DateTime date;

        private OptionalDateField(Composite parent, String label)
        {
            new Label(parent, SWT.NONE).setText(label);

            Composite editor = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).spacing(4, 0).applyTo(editor);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

            enabled = new Button(editor, SWT.CHECK);
            date = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
            date.setEnabled(false);

            enabled.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    date.setEnabled(enabled.getSelection());
                }
            });
        }

        private void setValue(String value)
        {
            if (value == null || value.isBlank())
            {
                enabled.setSelection(false);
                date.setEnabled(false);
                return;
            }

            try
            {
                LocalDate parsed = LocalDate.parse(value);
                date.setDate(parsed.getYear(), parsed.getMonthValue() - 1, parsed.getDayOfMonth());
                enabled.setSelection(true);
                date.setEnabled(true);
            }
            catch (RuntimeException e)
            {
                enabled.setSelection(false);
                date.setEnabled(false);
            }
        }

        private String getValue()
        {
            if (!enabled.getSelection())
                return null;

            return LocalDate.of(date.getYear(), date.getMonth() + 1, date.getDay()).toString();
        }

        private void updateEnabled(boolean parentEnabled)
        {
            enabled.setEnabled(parentEnabled);
            date.setEnabled(parentEnabled && enabled.getSelection());
        }
    }
}