package name.abuchen.portfolio.ui.wizards.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityDelta;
import name.abuchen.portfolio.model.SecurityKnockoutLevel;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityMultiplierPage extends AbstractPage
{
    private static final String TYPE = "type";
    private static final String UNDERLYING = "underlying";
    private static final String UNDERLYING_SECURITY_UUID = "underlyingSecurityUUID";
    private static final String FIRST_TRADING_DAY = "firstTradingDay";
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
    private static final String OPTION_PRODUCT_TYPE = "optionProductType";
    private static final String INITIAL_KNOCKOUT_LEVEL = "initialKnockoutLevel";

    private static final String CONTRACT_MONTH = "contractMonth";
    private static final String FIRST_NOTICE_DAY = "firstNoticeDay";
    private static final String FINAL_SETTLEMENT_DATE = "finalSettlementDate"; // legacy; removed from UI

    private static final String[] DERIVATIVE_PROPERTIES = { TYPE, UNDERLYING, UNDERLYING_SECURITY_UUID,
                    FIRST_TRADING_DAY, EXPIRATION_DATE, LAST_TRADING_DAY, SETTLEMENT_TYPE, SETTLEMENT_DATE, EXCHANGE,
                    CONTRACT_SYMBOL, CONTRACT_SIZE, TICK_SIZE, PUT_CALL, STRIKE, EXERCISE_STYLE, OPTION_PRODUCT_TYPE,
                    INITIAL_KNOCKOUT_LEVEL, CONTRACT_MONTH, FIRST_NOTICE_DAY, FINAL_SETTLEMENT_DATE };

    private final Client client;
    private final Security security;
    private final List<SecurityMultiplier> multipliers = new ArrayList<>();
    private final List<SecurityDelta> deltas = new ArrayList<>();
    private final List<SecurityKnockoutLevel> knockoutLevels = new ArrayList<>();
    private final Map<String, Security> underlyingSecurities = new LinkedHashMap<>();
    private boolean updatingUnderlyingSuggestions;

    private Combo derivativeType;
    private Combo underlying;
    private Combo settlementType;
    private Text exchange;
    private Text contractSymbol;
    private Text contractSize;
    private Text tickSize;
    private OptionalDateField firstTradingDay;
    private OptionalDateField expirationDate;
    private OptionalDateField lastTradingDay;
    private OptionalDateField settlementDate;

    private Group optionGroup;
    private Combo putCall;
    private Text strike;
    private Combo exerciseStyle;
    private Combo optionProductType;
    private Label initialKnockoutLevelLabel;
    private Text initialKnockoutLevel;

    private Group futureGroup;
    private Text contractMonth;
    private OptionalDateField firstNoticeDay;

    private Composite commonFields;

    private TableViewer viewer;
    private DateTime effectiveDate;
    private Text multiplierValue;

    private TableViewer deltaViewer;
    private DateTime deltaEffectiveDate;
    private Text deltaValue;

    private Group knockoutLevelGroup;
    private TableViewer knockoutLevelViewer;
    private DateTime knockoutLevelEffectiveDate;
    private Text knockoutLevelValue;

    public SecurityMultiplierPage(Client client, Security security)
    {
        this.client = client;
        this.security = security;
        security.getMultipliers().stream().map(m -> new SecurityMultiplier(m.getDate(), m.getValue()))
                        .forEach(multipliers::add);
        deltas.addAll(SecurityDelta.getDeltas(security));
        knockoutLevels.addAll(SecurityKnockoutLevel.getLevels(security));
        setTitle("Derivate");
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(0, 0).applyTo(container);

        TabFolder tabs = new TabFolder(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabs);

        Composite masterData = new Composite(tabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).spacing(8, 8).applyTo(masterData);
        createDerivativeMasterData(masterData);

        TabItem masterDataTab = new TabItem(tabs, SWT.NONE);
        masterDataTab.setText("Stammdaten");
        masterDataTab.setControl(masterData);

        Composite riskParameters = new Composite(tabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(true).margins(10, 10).spacing(10, 8).applyTo(riskParameters);
        createMultiplierSection(riskParameters);
        createDeltaSection(riskParameters);
        createKnockoutLevelSection(riskParameters);

        TabItem parametersTab = new TabItem(tabs, SWT.NONE);
        parametersTab.setText("Delta / Multiplikator / K.O.");
        parametersTab.setControl(riskParameters);

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
        underlying = new Combo(commonFields, SWT.DROP_DOWN);
        underlying.setToolTipText("Select another security or enter a free-text underlying. Typing filters the available assets.");
        client.getSecurities().stream().filter(s -> s != security)
                        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).forEach(s -> {
                            String label = underlyingLabel(s);
                            underlyingSecurities.put(label, s);
                            underlying.add(label);
                        });
        underlying.addModifyListener(e -> updateUnderlyingSuggestions());
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

        firstTradingDay = new OptionalDateField(commonFields, "First Trading Day");
        lastTradingDay = new OptionalDateField(commonFields, "Last Trading Day");

        addEmptyHalf(commonFields);
        expirationDate = new OptionalDateField(commonFields, "Expiration Date");

        addEmptyHalf(commonFields);
        settlementDate = new OptionalDateField(commonFields, "Settlement Date");

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
        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(strike);

        new Label(optionGroup, SWT.NONE).setText("Exercise style");
        exerciseStyle = new Combo(optionGroup, SWT.READ_ONLY);
        exerciseStyle.setItems("Not specified", "European", "American", "Bermudan");
        exerciseStyle.select(0);

        new Label(optionGroup, SWT.NONE).setText("Product type");
        optionProductType = new Combo(optionGroup, SWT.READ_ONLY);
        optionProductType.setItems("Not specified", "Standard option", "K.O. certificate");
        optionProductType.select(0);
        optionProductType.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateOptionProductControls();
            }
        });

        initialKnockoutLevelLabel = new Label(optionGroup, SWT.NONE);
        initialKnockoutLevelLabel.setText("Initial K.O. level");
        initialKnockoutLevel = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(initialKnockoutLevel);

        futureGroup = new Group(container, SWT.NONE);
        futureGroup.setText("Future");
        GridLayoutFactory.fillDefaults().numColumns(4).margins(8, 8).spacing(8, 6).applyTo(futureGroup);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(futureGroup);

        new Label(futureGroup, SWT.NONE).setText("Contract month");
        contractMonth = new Text(futureGroup, SWT.BORDER);
        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(contractMonth);

        firstNoticeDay = new OptionalDateField(futureGroup, "First notice day");
    }

    private static void addEmptyHalf(Composite parent)
    {
        new Label(parent, SWT.NONE);
        new Label(parent, SWT.NONE);
    }

    private void createMultiplierSection(Composite container)
    {
        Group multiplierGroup = new Group(container, SWT.NONE);
        multiplierGroup.setText("Multiplier history");
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(multiplierGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(multiplierGroup);

        String multiplierHelp = "The multiplier is stored as a time series rather than a single scalar because it can change over the life of an instrument. "
                        + "A value is effective from its date until the next entry; before the first entry the multiplier is 1.0. "
                        + "Practical examples are AUD and SEK government futures, where the applicable conversion/multiplier can change between transaction dates.";
        Label explanation = new Label(multiplierGroup, SWT.WRAP);
        explanation.setText("(i) Multiplier history - hover for details");
        explanation.setToolTipText(multiplierHelp);
        multiplierGroup.setToolTipText(multiplierHelp);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);

        viewer = new TableViewer(multiplierGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).hint(275, 170).applyTo(viewer.getControl());

        TableViewerColumn dateColumn = new TableViewerColumn(viewer, SWT.NONE);
        dateColumn.getColumn().setText("Valid from");
        dateColumn.getColumn().setWidth(140);
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
        valueColumn.getColumn().setWidth(120);
        valueColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Double.toString(((SecurityMultiplier) element).getMultiplier());
            }
        });

        viewer.setInput(multipliers);
        viewer.addSelectionChangedListener(event -> loadMultiplierSelection());

        Composite editor = new Composite(multiplierGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).spacing(8, 4).applyTo(editor);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

        new Label(editor, SWT.NONE).setText("Valid from");
        effectiveDate = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
        setMultiplierDate(earliestTransactionDate());

        new Label(editor, SWT.NONE).setText("Multiplier");
        multiplierValue = new Text(editor, SWT.BORDER | SWT.RIGHT);
        multiplierValue.setText("1.0");
        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(multiplierValue);

        Button addOrReplace = new Button(editor, SWT.PUSH);
        addOrReplace.setText("Add / replace");
        GridDataFactory.fillDefaults().span(4, 1).applyTo(addOrReplace);
        addOrReplace.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addOrReplaceMultiplier();
            }
        });

        createMultiplierActions(multiplierGroup);
    }

    private void createMultiplierActions(Composite parent)
    {
        Composite actions = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(8, 0).applyTo(actions);

        Button delete = new Button(actions, SWT.PUSH);
        delete.setText("Delete selected");
        delete.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IStructuredSelection selection = viewer.getStructuredSelection();
                if (!selection.isEmpty())
                {
                    multipliers.remove(selection.getFirstElement());
                    viewer.refresh();
                }
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

    private void createDeltaSection(Composite container)
    {
        Group deltaGroup = new Group(container, SWT.NONE);
        deltaGroup.setText("Delta history");
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(deltaGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(deltaGroup);

        Label explanation = new Label(deltaGroup, SWT.WRAP);
        explanation.setText(
                        "Delta is effective from its date until the next entry. Before the first entry, Delta is 1.0. Standard option deltas must be between -1.0 and 1.0.");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);

        deltaViewer = new TableViewer(deltaGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        deltaViewer.getTable().setHeaderVisible(true);
        deltaViewer.getTable().setLinesVisible(true);
        deltaViewer.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).hint(275, 170).applyTo(deltaViewer.getControl());

        TableViewerColumn dateColumn = new TableViewerColumn(deltaViewer, SWT.NONE);
        dateColumn.getColumn().setText("Valid from");
        dateColumn.getColumn().setWidth(140);
        dateColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((SecurityDelta) element).getDate());
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(deltaViewer, SWT.RIGHT);
        valueColumn.getColumn().setText("Delta");
        valueColumn.getColumn().setWidth(120);
        valueColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Double.toString(((SecurityDelta) element).getDelta());
            }
        });

        deltaViewer.setInput(deltas);
        deltaViewer.addSelectionChangedListener(event -> loadDeltaSelection());

        Composite editor = new Composite(deltaGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).spacing(8, 4).applyTo(editor);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

        new Label(editor, SWT.NONE).setText("Valid from");
        deltaEffectiveDate = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
        setDeltaDate(earliestTransactionDate());

        new Label(editor, SWT.NONE).setText("Delta");
        deltaValue = new Text(editor, SWT.BORDER | SWT.RIGHT);
        deltaValue.setText("1.0");
        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(deltaValue);

        Button addOrReplace = new Button(editor, SWT.PUSH);
        addOrReplace.setText("Add / replace");
        GridDataFactory.fillDefaults().span(4, 1).applyTo(addOrReplace);
        addOrReplace.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addOrReplaceDelta();
            }
        });

        Composite actions = new Composite(deltaGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(8, 0).applyTo(actions);

        Button delete = new Button(actions, SWT.PUSH);
        delete.setText("Delete selected");
        delete.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IStructuredSelection selection = deltaViewer.getStructuredSelection();
                if (!selection.isEmpty())
                {
                    deltas.remove(selection.getFirstElement());
                    deltaViewer.refresh();
                }
            }
        });

        Button clear = new Button(actions, SWT.PUSH);
        clear.setText("Clear all");
        clear.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (deltas.isEmpty()
                                || MessageDialog.openConfirm(getShell(), "Clear deltas", "Remove all Delta entries?"))
                {
                    deltas.clear();
                    deltaViewer.refresh();
                }
            }
        });
    }

    private void createKnockoutLevelSection(Composite container)
    {
        knockoutLevelGroup = new Group(container, SWT.NONE);
        knockoutLevelGroup.setText("Current K.O. level history");
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(knockoutLevelGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(knockoutLevelGroup);

        Label explanation = new Label(knockoutLevelGroup, SWT.WRAP);
        explanation.setText("The current K.O. level is effective from its date until the next entry. This is useful for open-end K.O. certificates whose K.O. level changes over time.");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);

        knockoutLevelViewer = new TableViewer(knockoutLevelGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        knockoutLevelViewer.getTable().setHeaderVisible(true);
        knockoutLevelViewer.getTable().setLinesVisible(true);
        knockoutLevelViewer.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).hint(275, 170).applyTo(knockoutLevelViewer.getControl());

        TableViewerColumn dateColumn = new TableViewerColumn(knockoutLevelViewer, SWT.NONE);
        dateColumn.getColumn().setText("Valid from");
        dateColumn.getColumn().setWidth(140);
        dateColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((SecurityKnockoutLevel) element).getDate());
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(knockoutLevelViewer, SWT.RIGHT);
        valueColumn.getColumn().setText("K.O. level");
        valueColumn.getColumn().setWidth(120);
        valueColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Double.toString(((SecurityKnockoutLevel) element).getLevel());
            }
        });

        knockoutLevelViewer.setInput(knockoutLevels);
        knockoutLevelViewer.addSelectionChangedListener(event -> loadKnockoutLevelSelection());

        Composite editor = new Composite(knockoutLevelGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).spacing(8, 4).applyTo(editor);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(editor);

        new Label(editor, SWT.NONE).setText("Valid from");
        knockoutLevelEffectiveDate = new DateTime(editor, SWT.DATE | SWT.DROP_DOWN);
        setKnockoutLevelDate(earliestTransactionDate());

        new Label(editor, SWT.NONE).setText("K.O. level");
        knockoutLevelValue = new Text(editor, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(knockoutLevelValue);

        Button addOrReplace = new Button(editor, SWT.PUSH);
        addOrReplace.setText("Add / replace");
        GridDataFactory.fillDefaults().span(4, 1).applyTo(addOrReplace);
        addOrReplace.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addOrReplaceKnockoutLevel();
            }
        });

        Composite actions = new Composite(knockoutLevelGroup, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(8, 0).applyTo(actions);

        Button delete = new Button(actions, SWT.PUSH);
        delete.setText("Delete selected");
        delete.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                IStructuredSelection selection = knockoutLevelViewer.getStructuredSelection();
                if (!selection.isEmpty())
                {
                    knockoutLevels.remove(selection.getFirstElement());
                    knockoutLevelViewer.refresh();
                }
            }
        });

        Button clear = new Button(actions, SWT.PUSH);
        clear.setText("Clear all");
        clear.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (knockoutLevels.isEmpty() || MessageDialog.openConfirm(getShell(), "Clear K.O. levels",
                                "Remove all current K.O. level entries?"))
                {
                    knockoutLevels.clear();
                    knockoutLevelViewer.refresh();
                }
            }
        });
    }

    private LocalDate earliestTransactionDate()
    {
        return security.getTransactions(client).stream()
                        .map(pair -> pair.getTransaction().getDateTime().toLocalDate())
                        .min(LocalDate::compareTo).orElse(LocalDate.now());
    }

    private void updateUnderlyingSuggestions()
    {
        if (underlying == null || updatingUnderlyingSuggestions)
            return;

        String typed = underlying.getText();
        String search = typed.trim().toLowerCase(Locale.ROOT);
        String[] matches = underlyingSecurities.keySet().stream()
                        .filter(label -> search.isEmpty() || label.toLowerCase(Locale.ROOT).contains(search))
                        .toArray(String[]::new);

        updatingUnderlyingSuggestions = true;
        try
        {
            underlying.setItems(matches);
            underlying.setText(typed);
            underlying.setSelection(new org.eclipse.swt.graphics.Point(typed.length(), typed.length()));
            if (!search.isEmpty() && matches.length > 0)
                underlying.setListVisible(true);
        }
        finally
        {
            updatingUnderlyingSuggestions = false;
        }
    }

    private LocalDate earliestTransactionDate()
    {
        return security.getTransactions(client).stream()
                        .map(pair -> pair.getTransaction().getDateTime().toLocalDate())
                        .min(LocalDate::compareTo).orElse(LocalDate.now());
    }

    private void updateUnderlyingSuggestions()
    {
        if (underlying == null || updatingUnderlyingSuggestions)
            return;

        String typed = underlying.getText();
        String search = typed.trim().toLowerCase(Locale.ROOT);
        String[] matches = underlyingSecurities.keySet().stream()
                        .filter(label -> search.isEmpty() || label.toLowerCase(Locale.ROOT).contains(search))
                        .toArray(String[]::new);

        updatingUnderlyingSuggestions = true;
        try
        {
            underlying.setItems(matches);
            underlying.setText(typed);
            underlying.setSelection(new org.eclipse.swt.graphics.Point(typed.length(), typed.length()));
            if (!search.isEmpty() && matches.length > 0)
                underlying.setListVisible(true);
        }
        finally
        {
            updatingUnderlyingSuggestions = false;
        }
    }

    private void loadDerivativeData()
    {
        selectByValue(derivativeType, property(TYPE), "FUTURE", "OPTION");
        selectByValue(settlementType, property(SETTLEMENT_TYPE), "CASH", "PHYSICAL");
        selectByValue(putCall, property(PUT_CALL), "CALL", "PUT");
        selectByValue(exerciseStyle, property(EXERCISE_STYLE), "EUROPEAN", "AMERICAN", "BERMUDAN");
        selectByValue(optionProductType, property(OPTION_PRODUCT_TYPE), "VANILLA", "KNOCK_OUT_CERTIFICATE");

        String underlyingUUID = property(UNDERLYING_SECURITY_UUID);
        Security linkedUnderlying = underlyingUUID == null ? null
                        : client.getSecurities().stream().filter(s -> underlyingUUID.equals(s.getUUID())).findFirst()
                                        .orElse(null);
        underlying.setText(
                        linkedUnderlying != null ? underlyingLabel(linkedUnderlying) : valueOrEmpty(property(UNDERLYING)));

        exchange.setText(valueOrEmpty(property(EXCHANGE)));
        contractSymbol.setText(valueOrEmpty(property(CONTRACT_SYMBOL)));
        contractSize.setText(valueOrEmpty(property(CONTRACT_SIZE)));
        tickSize.setText(valueOrEmpty(property(TICK_SIZE)));
        strike.setText(valueOrEmpty(property(STRIKE)));
        initialKnockoutLevel.setText(valueOrEmpty(property(INITIAL_KNOCKOUT_LEVEL)));
        contractMonth.setText(valueOrEmpty(property(CONTRACT_MONTH)));

        firstTradingDay.setValue(property(FIRST_TRADING_DAY));
        expirationDate.setValue(property(EXPIRATION_DATE));
        lastTradingDay.setValue(property(LAST_TRADING_DAY));
        settlementDate.setValue(property(SETTLEMENT_DATE));
        firstNoticeDay.setValue(property(FIRST_NOTICE_DAY));
    }

    private void updateDerivativeControls()
    {
        boolean isDerivative = derivativeType.getSelectionIndex() > 0;
        boolean isFuture = derivativeType.getSelectionIndex() == 1;
        boolean isOption = derivativeType.getSelectionIndex() == 2;

        setEnabledRecursive(commonFields, isDerivative);
        setEnabledRecursive(futureGroup, isFuture);
        setEnabledRecursive(optionGroup, isOption);

        firstTradingDay.updateEnabled(isDerivative);
        expirationDate.updateEnabled(isDerivative);
        lastTradingDay.updateEnabled(isDerivative);
        settlementDate.updateEnabled(isDerivative);
        firstNoticeDay.updateEnabled(isFuture);

        updateOptionProductControls();
    }

    private void updateOptionProductControls()
    {
        boolean isOption = derivativeType != null && derivativeType.getSelectionIndex() == 2;
        boolean isKnockout = isOption && optionProductType != null && optionProductType.getSelectionIndex() == 2;

        if (initialKnockoutLevelLabel != null)
            initialKnockoutLevelLabel.setEnabled(isKnockout);
        if (initialKnockoutLevel != null)
            initialKnockoutLevel.setEnabled(isKnockout);
        if (knockoutLevelGroup != null)
            setEnabledRecursive(knockoutLevelGroup, isKnockout);
    }

    private void loadMultiplierSelection()
    {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        SecurityMultiplier selected = (SecurityMultiplier) selection.getFirstElement();
        setMultiplierDate(selected.getDate());
        multiplierValue.setText(Double.toString(selected.getMultiplier()));
    }

    private void loadDeltaSelection()
    {
        IStructuredSelection selection = deltaViewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        SecurityDelta selected = (SecurityDelta) selection.getFirstElement();
        setDeltaDate(selected.getDate());
        deltaValue.setText(Double.toString(selected.getDelta()));
    }

    private void loadKnockoutLevelSelection()
    {
        IStructuredSelection selection = knockoutLevelViewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        SecurityKnockoutLevel selected = (SecurityKnockoutLevel) selection.getFirstElement();
        setKnockoutLevelDate(selected.getDate());
        knockoutLevelValue.setText(Double.toString(selected.getLevel()));
    }

    private void addOrReplaceMultiplier()
    {
        double value = parsePositive(multiplierValue, "Invalid multiplier", "Enter a positive numeric multiplier.");
        if (Double.isNaN(value))
            return;

        SecurityMultiplier replacement = SecurityMultiplier.of(getMultiplierDate(), value);
        int index = Collections.binarySearch(multipliers, replacement);
        if (index >= 0)
            multipliers.set(index, replacement);
        else
            multipliers.add(~index, replacement);

        viewer.refresh();
        viewer.setSelection(new StructuredSelection(replacement), true);
    }

    private void addOrReplaceDelta()
    {
        double value;
        try
        {
            value = Double.parseDouble(deltaValue.getText().trim().replace(',', '.'));
        }
        catch (NumberFormatException e)
        {
            showInvalidDelta();
            return;
        }

        if (!Double.isFinite(value) || value < -1.0 || value > 1.0)
        {
            showInvalidDelta();
            return;
        }

        SecurityDelta replacement = SecurityDelta.of(getDeltaDate(), value);
        int index = Collections.binarySearch(deltas, replacement);
        if (index >= 0)
            deltas.set(index, replacement);
        else
            deltas.add(~index, replacement);

        deltaViewer.refresh();
        deltaViewer.setSelection(new StructuredSelection(replacement), true);
    }

    private void addOrReplaceKnockoutLevel()
    {
        double value = parsePositive(knockoutLevelValue, "Invalid K.O. level", "Enter a positive numeric K.O. level.");
        if (Double.isNaN(value))
            return;

        SecurityKnockoutLevel replacement = SecurityKnockoutLevel.of(getKnockoutLevelDate(), value);
        int index = Collections.binarySearch(knockoutLevels, replacement);
        if (index >= 0)
            knockoutLevels.set(index, replacement);
        else
            knockoutLevels.add(~index, replacement);

        knockoutLevelViewer.refresh();
        knockoutLevelViewer.setSelection(new StructuredSelection(replacement), true);
    }

    private double parsePositive(Text control, String title, String message)
    {
        double value;
        try
        {
            value = Double.parseDouble(control.getText().trim().replace(',', '.'));
        }
        catch (NumberFormatException e)
        {
            MessageDialog.openError(getShell(), title, message);
            control.setFocus();
            control.selectAll();
            return Double.NaN;
        }

        if (!Double.isFinite(value) || value <= 0)
        {
            MessageDialog.openError(getShell(), title, message);
            control.setFocus();
            control.selectAll();
            return Double.NaN;
        }

        return value;
    }

    private void showInvalidDelta()
    {
        MessageDialog.openError(getShell(), "Invalid Delta", "Enter a numeric Delta between -1.0 and 1.0.");
        deltaValue.setFocus();
        deltaValue.selectAll();
    }

    private LocalDate getMultiplierDate()
    {
        return LocalDate.of(effectiveDate.getYear(), effectiveDate.getMonth() + 1, effectiveDate.getDay());
    }

    private void setMultiplierDate(LocalDate date)
    {
        if (effectiveDate != null)
            effectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    private LocalDate getDeltaDate()
    {
        return LocalDate.of(deltaEffectiveDate.getYear(), deltaEffectiveDate.getMonth() + 1, deltaEffectiveDate.getDay());
    }

    private void setDeltaDate(LocalDate date)
    {
        if (deltaEffectiveDate != null)
            deltaEffectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    private LocalDate getKnockoutLevelDate()
    {
        return LocalDate.of(knockoutLevelEffectiveDate.getYear(), knockoutLevelEffectiveDate.getMonth() + 1,
                        knockoutLevelEffectiveDate.getDay());
    }

    private void setKnockoutLevelDate(LocalDate date)
    {
        if (knockoutLevelEffectiveDate != null)
            knockoutLevelEffectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    public void applyChanges()
    {
        security.removeAllMultipliers();
        multipliers.forEach(m -> security.addMultiplier(new SecurityMultiplier(m.getDate(), m.getValue())));
        SecurityDelta.replaceAll(security, deltas);

        int typeIndex = derivativeType.getSelectionIndex();
        if (typeIndex <= 0)
        {
            SecurityKnockoutLevel.replaceAll(security, Collections.emptyList());
            for (String name : DERIVATIVE_PROPERTIES)
                setProperty(name, null);
            return;
        }

        setProperty(TYPE, typeIndex == 1 ? "FUTURE" : "OPTION");

        String underlyingText = comboText(underlying);
        Security selectedUnderlying = underlyingSecurities.get(underlyingText);
        setProperty(UNDERLYING, underlyingText);
        setProperty(UNDERLYING_SECURITY_UUID, selectedUnderlying == null ? null : selectedUnderlying.getUUID());

        setProperty(FIRST_TRADING_DAY, firstTradingDay.getValue());
        setProperty(EXPIRATION_DATE, expirationDate.getValue());
        setProperty(LAST_TRADING_DAY, lastTradingDay.getValue());
        setProperty(SETTLEMENT_DATE, settlementDate.getValue());
        setProperty(SETTLEMENT_TYPE, comboValue(settlementType, "CASH", "PHYSICAL"));
        setProperty(EXCHANGE, text(exchange));
        setProperty(CONTRACT_SYMBOL, text(contractSymbol));
        setProperty(CONTRACT_SIZE, text(contractSize));
        setProperty(TICK_SIZE, text(tickSize));
        setProperty(FINAL_SETTLEMENT_DATE, null);

        if (typeIndex == 2)
        {
            setProperty(PUT_CALL, comboValue(putCall, "CALL", "PUT"));
            setProperty(STRIKE, text(strike));
            setProperty(EXERCISE_STYLE, comboValue(exerciseStyle, "EUROPEAN", "AMERICAN", "BERMUDAN"));
            setProperty(OPTION_PRODUCT_TYPE, comboValue(optionProductType, "VANILLA", "KNOCK_OUT_CERTIFICATE"));

            boolean isKnockout = optionProductType.getSelectionIndex() == 2;
            setProperty(INITIAL_KNOCKOUT_LEVEL, isKnockout ? text(initialKnockoutLevel) : null);
            SecurityKnockoutLevel.replaceAll(security, isKnockout ? knockoutLevels : Collections.emptyList());

            setProperty(CONTRACT_MONTH, null);
            setProperty(FIRST_NOTICE_DAY, null);
        }
        else
        {
            setProperty(PUT_CALL, null);
            setProperty(STRIKE, null);
            setProperty(EXERCISE_STYLE, null);
            setProperty(OPTION_PRODUCT_TYPE, null);
            setProperty(INITIAL_KNOCKOUT_LEVEL, null);
            SecurityKnockoutLevel.replaceAll(security, Collections.emptyList());

            setProperty(CONTRACT_MONTH, text(contractMonth));
            setProperty(FIRST_NOTICE_DAY, firstNoticeDay.getValue());
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

    private static String comboText(Combo control)
    {
        String value = control.getText().trim();
        return value.isEmpty() ? null : value;
    }

    private static String underlyingLabel(Security security)
    {
        String ticker = security.getTickerSymbol();
        return ticker == null || ticker.isBlank() ? security.getName() : security.getName() + " [" + ticker + "]";
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
