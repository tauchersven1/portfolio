package de.venari.portfolio.derivatives;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.ui.AddonSecurityPage;

public class DerivativeSecurityPage implements AddonSecurityPage
{
    private static final String PREFIX = "derivatives-addon."; //$NON-NLS-1$
    private static final Pattern US_OPTION = Pattern.compile("^([A-Z0-9]{1,8})(\\d{6})([CP])(\\d{8})$"); //$NON-NLS-1$
    private static final Pattern EUREX_OPTION = Pattern.compile("^([CP])(.+?)(20\\d{6})(\\d+(?:[.,]\\d+)?)M$"); //$NON-NLS-1$
    private static final Pattern FUTURE = Pattern.compile("^([A-Z0-9._-]+?)([FGHJKMNQUVXZ])(\\d{1,2})$"); //$NON-NLS-1$
    private static final String FUTURE_MONTH_CODES = "FGHJKMNQUVXZ"; //$NON-NLS-1$
    private Security security;
    private Client client;
    private TabFolder instrumentPages;
    private Text exchange, contractSymbol, expirationDate, underlyingCurrency, pricingCurrency;
    private Combo underlying;
    private final List<Security> underlyingSecurities = new ArrayList<>();
    private Text contractMonth, firstNoticeDate, lastTradingDate, strike, issuer;
    private Combo putCall, exerciseStyle;
    private Button fxInstrument, regularOption, knockOutCertificate;
    private DatedValueEditor multiplier, delta, knockOutLevel;

    @Override
    public String getTitle()
    {
        return "Derivate"; //$NON-NLS-1$
    }

    @Override
    public Control createControl(Composite parent, Client client, Security security)
    {
        this.client = client;
        this.security = security;
        TabFolder root = new TabFolder(parent, SWT.NONE);
        createMasterDataPage(page(root, "Stammdaten")); //$NON-NLS-1$
        createValuesPage(page(root, "Multiplier/Delta")); //$NON-NLS-1$
        loadValues();
        return root;
    }

    private Composite page(TabFolder folder, String title)
    {
        Composite body = new Composite(folder, SWT.NONE);
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(body);
        return body;
    }

    private void createMasterDataPage(Composite parent)
    {
        GridLayoutFactory.fillDefaults().margins(12, 12).spacing(8, 10).applyTo(parent);
        Group common = new Group(parent, SWT.NONE);
        common.setText("Gemeinsame Stammdaten"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(common);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(10, 8).applyTo(common);
        new Label(common, SWT.NONE).setText("Underlying"); //$NON-NLS-1$
        underlying = new Combo(common, SWT.READ_ONLY);
        GridDataFactory.fillDefaults().grab(true, false).hint(260, SWT.DEFAULT).applyTo(underlying);
        populateUnderlyings();
        exchange = text(common, "Exchange"); //$NON-NLS-1$
        contractSymbol = text(common, "Kontrakt- / Handelssymbol"); //$NON-NLS-1$
        expirationDate = text(common, "Fälligkeit (JJJJ-MM-TT)"); //$NON-NLS-1$
        new Label(common, SWT.NONE).setText("FX Instrument"); //$NON-NLS-1$
        fxInstrument = new Button(common, SWT.CHECK);
        underlyingCurrency = text(common, "Underlying Currency"); //$NON-NLS-1$
        pricingCurrency = text(common, "Pricing Currency"); //$NON-NLS-1$
        fxInstrument.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent event)
            {
                updateFxState();
            }
        });

        instrumentPages = new TabFolder(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(instrumentPages);
        Composite futures = page(instrumentPages, "Futures"); //$NON-NLS-1$
        GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 12).spacing(10, 8).applyTo(futures);
        contractMonth = text(futures, "Kontraktmonat"); //$NON-NLS-1$
        firstNoticeDate = text(futures, "First Notice Day (JJJJ-MM-TT)"); //$NON-NLS-1$
        lastTradingDate = text(futures, "Last Trading Date (JJJJ-MM-TT)"); //$NON-NLS-1$

        Composite options = page(instrumentPages, "Optionen"); //$NON-NLS-1$
        GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 12).spacing(10, 8).applyTo(options);
        new Label(options, SWT.NONE).setText("Produkttyp"); //$NON-NLS-1$
        Composite kinds = new Composite(options, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(kinds);
        regularOption = new Button(kinds, SWT.RADIO);
        regularOption.setText("Option"); //$NON-NLS-1$
        knockOutCertificate = new Button(kinds, SWT.RADIO);
        knockOutCertificate.setText("K.O.-Zertifikat"); //$NON-NLS-1$
        putCall = combo(options, "Put / Call", "Nicht angegeben", "Call", "Put"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        strike = text(options, "Basispreis"); //$NON-NLS-1$
        exerciseStyle = combo(options, "Ausübungsart", "Nicht angegeben", "Europäisch", "Amerikanisch", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        "Bermudan"); //$NON-NLS-1$
        issuer = text(options, "Emittent"); //$NON-NLS-1$
        strike.addModifyListener(event -> updateKnockOutValue());
        SelectionAdapter listener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent event)
            {
                updateKnockOutState();
            }
        };
        regularOption.addSelectionListener(listener);
        knockOutCertificate.addSelectionListener(listener);
    }

    private void createValuesPage(Composite parent)
    {
        GridLayoutFactory.fillDefaults().numColumns(3).margins(12, 12).spacing(12, 8).applyTo(parent);
        multiplier = new DatedValueEditor(parent, "Multiplier", BigDecimal.ONE); //$NON-NLS-1$
        delta = new DatedValueEditor(parent, "Delta", BigDecimal.ONE); //$NON-NLS-1$
        knockOutLevel = new DatedValueEditor(parent, "K.O. Level", null); //$NON-NLS-1$
    }

    private void loadValues()
    {
        selectUnderlying(value(property("underlying"))); //$NON-NLS-1$
        exchange.setText(value(property("exchange"))); //$NON-NLS-1$
        contractSymbol.setText(value(property("contractSymbol"))); //$NON-NLS-1$
        expirationDate.setText(value(property("expirationDate"))); //$NON-NLS-1$
        fxInstrument.setSelection(Boolean.parseBoolean(value(property("fxInstrument")))); //$NON-NLS-1$
        underlyingCurrency.setText(value(property("underlyingCurrency"))); //$NON-NLS-1$
        pricingCurrency.setText(value(property("pricingCurrency"))); //$NON-NLS-1$
        contractMonth.setText(value(property("contractMonth"))); //$NON-NLS-1$
        firstNoticeDate.setText(value(property("firstNoticeDate"))); //$NON-NLS-1$
        lastTradingDate.setText(value(property("lastTradingDate"))); //$NON-NLS-1$
        strike.setText(value(property("strike"))); //$NON-NLS-1$
        issuer.setText(value(property("issuer"))); //$NON-NLS-1$
        select(putCall, property("putCall"), new String[] { "", "CALL", "PUT" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        select(exerciseStyle, property("exerciseStyle"),
                        new String[] { "", "EUROPEAN", "AMERICAN", "BERMUDAN" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        String type = value(property("instrumentType")); //$NON-NLS-1$
        instrumentPages.setSelection("FUTURE".equalsIgnoreCase(type) ? 0 : 1); //$NON-NLS-1$
        boolean ko = "KNOCK_OUT_CERTIFICATE".equalsIgnoreCase(type); //$NON-NLS-1$
        regularOption.setSelection(!ko);
        knockOutCertificate.setSelection(ko);
        String history = property("multiplierHistory"); //$NON-NLS-1$
        if (history == null)
            history = defaultDate() + "=" + AddonMultiplier.get(security).toPlainString(); //$NON-NLS-1$
        LocalDate defaultDate = defaultDate();
        multiplier.load(history, defaultDate);
        delta.load(property("deltaHistory"), defaultDate); //$NON-NLS-1$
        knockOutLevel.load(property("knockOutLevelHistory"), defaultDate); //$NON-NLS-1$
        updateFxState();
        updateKnockOutState();
        autofillFromSymbol();
    }

    private void populateUnderlyings()
    {
        underlying.removeAll();
        underlyingSecurities.clear();
        underlying.add(""); //$NON-NLS-1$
        client.getSecurities().stream().filter(candidate -> !candidate.equals(security))
                        .sorted(Comparator.comparing(this::underlyingLabel, String.CASE_INSENSITIVE_ORDER))
                        .forEach(candidate -> {
                            underlyingSecurities.add(candidate);
                            underlying.add(underlyingLabel(candidate));
                        });
        underlying.select(0);
    }

    private String underlyingLabel(Security candidate)
    {
        String ticker = value(candidate.getTickerSymbol());
        return ticker.isBlank() ? candidate.getName() : candidate.getName() + " (" + ticker + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void selectUnderlying(String stored)
    {
        underlying.select(0);
        for (int ii = 0; ii < underlyingSecurities.size(); ii++)
        {
            Security candidate = underlyingSecurities.get(ii);
            if (stored.equalsIgnoreCase(value(candidate.getTickerSymbol())) || stored.equals(candidate.getUUID())
                            || stored.equalsIgnoreCase(candidate.getName()))
            {
                underlying.select(ii + 1);
                return;
            }
        }
    }

    private String selectedUnderlying()
    {
        int index = underlying.getSelectionIndex() - 1;
        if (index < 0 || index >= underlyingSecurities.size())
            return null;
        Security selected = underlyingSecurities.get(index);
        String ticker = value(selected.getTickerSymbol());
        return ticker.isBlank() ? selected.getUUID() : ticker;
    }

    private void autofillFromSymbol()
    {
        String symbol = value(security.getTickerSymbol()).trim().toUpperCase(Locale.ROOT).replace(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (symbol.isBlank())
            return;

        Matcher us = US_OPTION.matcher(symbol);
        if (us.matches())
        {
            LocalDate expiry = parseUsDate(us.group(2));
            applyOptionAutofill(us.group(1), expiry, us.group(3), new BigDecimal(us.group(4)).movePointLeft(3));
            return;
        }

        Matcher eurex = EUREX_OPTION.matcher(symbol);
        if (eurex.matches())
        {
            LocalDate expiry = parseIsoCompactDate(eurex.group(3));
            applyOptionAutofill(eurex.group(2), expiry, eurex.group(1),
                            new BigDecimal(eurex.group(4).replace(',', '.')));
            return;
        }

        Matcher future = FUTURE.matcher(symbol);
        if (future.matches())
            applyFutureAutofill(future.group(1), future.group(2).charAt(0), future.group(3));
    }

    private void applyOptionAutofill(String root, LocalDate expiry, String callPut, BigDecimal parsedStrike)
    {
        instrumentPages.setSelection(1);
        regularOption.setSelection(true);
        knockOutCertificate.setSelection(false);
        autofillUnderlying(root);
        autofill(contractSymbol, root);
        autofill(expirationDate, expiry == null ? null : expiry.toString());
        autofill(firstNoticeDate, expiry == null ? null : expiry.toString());
        autofill(lastTradingDate, expiry == null ? null : expiry.toString());
        autofill(strike, parsedStrike.stripTrailingZeros().toPlainString());
        int selection = "C".equals(callPut) ? 1 : 2; //$NON-NLS-1$
        String previous = putCall.getSelectionIndex() < 0 ? "" : putCall.getItem(putCall.getSelectionIndex()); //$NON-NLS-1$
        if (!previous.equals(putCall.getItem(selection)) && !previous.equals(putCall.getItem(0)))
            putCall.setForeground(putCall.getDisplay().getSystemColor(SWT.COLOR_RED));
        putCall.select(selection);
        updateKnockOutState();
    }

    private void applyFutureAutofill(String root, char monthCode, String yearCode)
    {
        int month = FUTURE_MONTH_CODES.indexOf(monthCode) + 1;
        int year = parseFutureYear(yearCode);
        if (month < 1 || year < 1)
            return;
        YearMonth contract = YearMonth.of(year, month);
        instrumentPages.setSelection(0);
        autofillUnderlying(root);
        autofill(contractSymbol, root);
        autofill(contractMonth, contract.toString());

        LocalDate expiry = knownFutureExpiry(root, contract);
        if (expiry != null)
        {
            autofill(expirationDate, expiry.toString());
            autofill(firstNoticeDate, expiry.toString());
            autofill(lastTradingDate, expiry.toString());
        }
    }

    private LocalDate knownFutureExpiry(String root, YearMonth contract)
    {
        return switch (root)
        {
            case "ES", "MES", "NQ", "MNQ", "RTY", "M2K" -> contract.atDay(1) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                            .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY));
            default -> null;
        };
    }

    private int parseFutureYear(String value)
    {
        int parsed = Integer.parseInt(value);
        if (value.length() == 2)
            return 2000 + parsed;
        int decade = (LocalDate.now().getYear() / 10) * 10;
        int year = decade + parsed;
        return year < LocalDate.now().getYear() - 2 ? year + 10 : year;
    }

    private LocalDate parseUsDate(String value)
    {
        return parseIsoCompactDate("20" + value); //$NON-NLS-1$
    }

    private LocalDate parseIsoCompactDate(String value)
    {
        try
        {
            return LocalDate.of(Integer.parseInt(value.substring(0, 4)), Integer.parseInt(value.substring(4, 6)),
                            Integer.parseInt(value.substring(6, 8)));
        }
        catch (RuntimeException ignore)
        {
            return null;
        }
    }

    private void autofillUnderlying(String ticker)
    {
        String previous = selectedUnderlying();
        for (int ii = 0; ii < underlyingSecurities.size(); ii++)
        {
            Security candidate = underlyingSecurities.get(ii);
            if (ticker.equalsIgnoreCase(value(candidate.getTickerSymbol())))
            {
                String replacement = value(candidate.getTickerSymbol());
                if (previous != null && !previous.equalsIgnoreCase(replacement))
                    underlying.setForeground(underlying.getDisplay().getSystemColor(SWT.COLOR_RED));
                underlying.select(ii + 1);
                return;
            }
        }
    }

    private void autofill(Text field, String replacement)
    {
        if (replacement == null || replacement.isBlank())
            return;
        String previous = field.getText().trim();
        if (!previous.isBlank() && !previous.equalsIgnoreCase(replacement))
            field.setForeground(field.getDisplay().getSystemColor(SWT.COLOR_RED));
        field.setText(replacement);
    }

    private LocalDate defaultDate()
    {
        return client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                        .filter(transaction -> security.equals(transaction.getSecurity()))
                        .map(transaction -> transaction.getDateTime().toLocalDate()).min(LocalDate::compareTo)
                        .orElse(LocalDate.now());
    }

    private Text text(Composite parent, String label)
    {
        new Label(parent, SWT.NONE).setText(label);
        Text field = new Text(parent, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).hint(260, SWT.DEFAULT).applyTo(field);
        return field;
    }

    private Combo combo(Composite parent, String label, String... items)
    {
        new Label(parent, SWT.NONE).setText(label);
        Combo field = new Combo(parent, SWT.READ_ONLY);
        field.setItems(items);
        field.select(0);
        GridDataFactory.fillDefaults().grab(true, false).hint(260, SWT.DEFAULT).applyTo(field);
        return field;
    }

    private void select(Combo combo, String current, String[] values)
    {
        for (int ii = 0; ii < values.length; ii++)
            if (values[ii].equalsIgnoreCase(value(current)))
                combo.select(ii);
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
        security.setPropertyValue(SecurityProperty.Type.FEED, PREFIX + name,
                        value == null || value.isBlank() ? null : value.trim());
    }

    private void updateKnockOutState()
    {
        if (knockOutLevel != null)
        {
            knockOutLevel.setEnabled(knockOutCertificate.getSelection());
            if (knockOutCertificate.getSelection())
                knockOutLevel.initialize(defaultDate(), decimalValue(strike.getText()));
        }
    }

    private void updateKnockOutValue()
    {
        if (knockOutLevel != null && knockOutCertificate.getSelection())
            knockOutLevel.initialize(defaultDate(), decimalValue(strike.getText()));
    }

    private BigDecimal decimalValue(String value)
    {
        if (value == null || value.isBlank())
            return null;
        try
        {
            return new BigDecimal(value.trim().replace(',', '.'));
        }
        catch (NumberFormatException ignore)
        {
            return null;
        }
    }

    private void updateFxState()
    {
        boolean enabled = fxInstrument.getSelection();
        underlyingCurrency.setEnabled(enabled);
        pricingCurrency.setEnabled(enabled);
    }

    @Override
    public void applyChanges()
    {
        boolean future = instrumentPages.getSelectionIndex() == 0;
        set("instrumentType", future ? "FUTURE" //$NON-NLS-1$ //$NON-NLS-2$
                        : knockOutCertificate.getSelection() ? "KNOCK_OUT_CERTIFICATE" : "OPTION"); //$NON-NLS-1$ //$NON-NLS-2$
        set("underlying", selectedUnderlying()); //$NON-NLS-1$
        set("exchange", exchange.getText()); //$NON-NLS-1$
        set("contractSymbol", contractSymbol.getText()); //$NON-NLS-1$
        set("expirationDate", expirationDate.getText()); //$NON-NLS-1$
        set("fxInstrument", Boolean.toString(fxInstrument.getSelection())); //$NON-NLS-1$
        set("underlyingCurrency", fxInstrument.getSelection() ? underlyingCurrency.getText() : null); //$NON-NLS-1$
        set("pricingCurrency", fxInstrument.getSelection() ? pricingCurrency.getText() : null); //$NON-NLS-1$
        set("contractMonth", contractMonth.getText()); //$NON-NLS-1$
        set("firstNoticeDate", firstNoticeDate.getText()); //$NON-NLS-1$
        set("lastTradingDate", lastTradingDate.getText()); //$NON-NLS-1$
        set("putCall", new String[] { "", "CALL", "PUT" }[putCall.getSelectionIndex()]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        set("strike", strike.getText()); //$NON-NLS-1$
        set("issuer", issuer.getText()); //$NON-NLS-1$
        set("exerciseStyle", new String[] { "", "EUROPEAN", "AMERICAN", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        "BERMUDAN" }[exerciseStyle.getSelectionIndex()]); //$NON-NLS-1$ //$NON-NLS-2$
        set("multiplierHistory", multiplier.serialize()); //$NON-NLS-1$
        set("deltaHistory", delta.serialize()); //$NON-NLS-1$
        set("knockOutLevelHistory", knockOutCertificate.getSelection() ? knockOutLevel.serialize() : null); //$NON-NLS-1$
        AddonMultiplier.set(security, multiplier.effectiveValue(LocalDate.now(), BigDecimal.ONE));
    }

    private static final class DatedValueEditor extends Composite
    {
        private final Table table;
        private final Button add, remove;
        private final BigDecimal defaultValue;
        private LocalDate defaultDate;

        private DatedValueEditor(Composite parent, String title, BigDecimal defaultValue)
        {
            super(parent, SWT.NONE);
            this.defaultValue = defaultValue;
            GridDataFactory.fillDefaults().grab(true, true).applyTo(this);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
            Label heading = new Label(this, SWT.NONE);
            heading.setText(title);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(heading);
            table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
            GridDataFactory.fillDefaults().grab(true, true).span(2, 1).hint(260, 240).applyTo(table);
            column("Gültig ab", 120); //$NON-NLS-1$
            column("Wert", 120); //$NON-NLS-1$
            add = new Button(this, SWT.PUSH);
            add.setText("Hinzufügen"); //$NON-NLS-1$
            add.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent event)
                {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setText(new String[] { defaultDate.toString(),
                                    DatedValueEditor.this.defaultValue == null ? "" : DatedValueEditor.this.defaultValue.toPlainString() }); //$NON-NLS-1$
                    edit(item, 0);
                }
            });
            remove = new Button(this, SWT.PUSH);
            remove.setText("Entfernen"); //$NON-NLS-1$
            remove.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent event)
                {
                    if (table.getSelectionIndex() >= 0)
                        table.remove(table.getSelectionIndex());
                }
            });
            table.addListener(SWT.MouseDoubleClick, event -> {
                TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
                if (item != null)
                    edit(item, event.x < table.getColumn(0).getWidth() ? 0 : 1);
            });
        }

        private void column(String title, int width)
        {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(title);
            column.setWidth(width);
        }

        private void edit(TableItem item, int column)
        {
            TableEditor editor = new TableEditor(table);
            Text input = new Text(table, SWT.BORDER);
            input.setText(item.getText(column));
            input.selectAll();
            input.setFocus();
            editor.grabHorizontal = true;
            editor.setEditor(input, item, column);
            input.addListener(SWT.FocusOut, e -> {
                item.setText(column, input.getText().trim());
                input.dispose();
                editor.dispose();
            });
            input.addListener(SWT.Traverse, e -> {
                if (e.detail == SWT.TRAVERSE_RETURN)
                    table.setFocus();
                else if (e.detail == SWT.TRAVERSE_ESCAPE)
                {
                    input.dispose();
                    editor.dispose();
                    e.doit = false;
                }
            });
        }

        private void load(String encoded, LocalDate defaultDate)
        {
            this.defaultDate = defaultDate;
            List<DatedValueSeries.Entry> entries = DatedValueSeries.parse(encoded);
            if (entries.isEmpty() && defaultValue != null)
                entries = List.of(new DatedValueSeries.Entry(defaultDate, defaultValue));
            for (DatedValueSeries.Entry entry : entries)
            {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(new String[] { entry.date().toString(), entry.value().toPlainString() });
            }
        }

        private void initialize(LocalDate date, BigDecimal value)
        {
            if (table.getItemCount() > 0)
            {
                TableItem first = table.getItem(0);
                if (value != null && first.getText(1).isBlank())
                    first.setText(1, value.stripTrailingZeros().toPlainString());
                return;
            }

            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] { date.toString(),
                            value == null ? "" : value.stripTrailingZeros().toPlainString() }); //$NON-NLS-1$
        }

        private String serialize()
        {
            List<DatedValueSeries.Entry> entries = new ArrayList<>();
            for (TableItem item : table.getItems())
            {
                try
                {
                    entries.add(new DatedValueSeries.Entry(LocalDate.parse(item.getText(0)),
                                    new BigDecimal(item.getText(1).replace(',', '.'))));
                }
                catch (RuntimeException ignore)
                {
                    // Ignore incomplete rows.
                }
            }
            return DatedValueSeries.serialize(entries);
        }

        private BigDecimal effectiveValue(LocalDate date, BigDecimal fallback)
        {
            return DatedValueSeries.valueAt(serialize(), date).orElse(fallback);
        }

        @Override
        public void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);
            table.setEnabled(enabled);
            add.setEnabled(enabled);
            remove.setEnabled(enabled);
        }
    }
}
