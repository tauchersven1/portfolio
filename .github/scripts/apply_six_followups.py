from pathlib import Path


def replace_once(text, old, new, label):
    if old not in text:
        if new in text:
            return text, False
        raise SystemExit(f"Pattern not found for {label}")
    return text.replace(old, new, 1), True


changed = False

# Derivative master data: earliest transaction date, underlying autocomplete,
# multiplier explanation.
p = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java')
s = p.read_text(encoding='utf-8')

s, c = replace_once(s,
    'import java.util.List;\nimport java.util.Map;\n',
    'import java.util.List;\nimport java.util.Locale;\nimport java.util.Map;\n',
    'Locale import')
changed |= c

s, c = replace_once(s,
    '    private final Map<String, Security> underlyingSecurities = new LinkedHashMap<>();\n\n    private Combo derivativeType;',
    '    private final Map<String, Security> underlyingSecurities = new LinkedHashMap<>();\n    private boolean updatingUnderlyingSuggestions;\n\n    private Combo derivativeType;',
    'autocomplete state')
changed |= c

old = '''        new Label(commonFields, SWT.NONE).setText("Underlying");
        underlying = new Combo(commonFields, SWT.DROP_DOWN);
        underlying.setToolTipText("Select another security or enter a free-text underlying");
        client.getSecurities().stream().filter(s -> s != security)
                        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).forEach(s -> {
                            String label = underlyingLabel(s);
                            underlyingSecurities.put(label, s);
                            underlying.add(label);
                        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(underlying);
'''
new = '''        new Label(commonFields, SWT.NONE).setText("Underlying");
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
'''
s, c = replace_once(s, old, new, 'underlying autocomplete listener')
changed |= c

old = '''        Label explanation = new Label(multiplierGroup, SWT.WRAP);
        explanation.setText(
                        "A multiplier is effective from its date until the next entry. Before the first entry, the multiplier is 1.0.");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);
'''
new = '''        String multiplierHelp = "The multiplier is stored as a time series rather than a single scalar because it can change over the life of an instrument. "
                        + "A value is effective from its date until the next entry; before the first entry the multiplier is 1.0. "
                        + "Practical examples are AUD and SEK government futures, where the applicable conversion/multiplier can change between transaction dates.";
        Label explanation = new Label(multiplierGroup, SWT.WRAP);
        explanation.setText("(i) Multiplier history - hover for details");
        explanation.setToolTipText(multiplierHelp);
        multiplierGroup.setToolTipText(multiplierHelp);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(explanation);
'''
s, c = replace_once(s, old, new, 'multiplier help')
changed |= c

for old_call, new_call, label in [
    ('setMultiplierDate(LocalDate.now());', 'setMultiplierDate(earliestTransactionDate());', 'multiplier initial date'),
    ('setDeltaDate(LocalDate.now());', 'setDeltaDate(earliestTransactionDate());', 'delta initial date'),
    ('setKnockoutLevelDate(LocalDate.now());', 'setKnockoutLevelDate(earliestTransactionDate());', 'KO initial date')]:
    s, c = replace_once(s, old_call, new_call, label)
    changed |= c

marker = '''    private void loadDerivativeData()
    {
'''
helper = '''    private LocalDate earliestTransactionDate()
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
'''
s, c = replace_once(s, marker, helper, 'underlying autocomplete helper')
changed |= c
p.write_text(s, encoding='utf-8')

# Exposure management: taxonomy classification filter, hard refresh for
# exposure type/currency, and second chart tab by trading symbol.
p = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java')
s = p.read_text(encoding='utf-8')

s, c = replace_once(s,
    'import org.eclipse.swt.widgets.Label;\n',
    'import org.eclipse.swt.widgets.Label;\nimport org.eclipse.swt.widgets.TabFolder;\nimport org.eclipse.swt.widgets.TabItem;\n',
    'tab imports')
changed |= c
s, c = replace_once(s,
    'import name.abuchen.portfolio.model.Client;\nimport name.abuchen.portfolio.model.Security;\n',
    'import name.abuchen.portfolio.model.Classification;\nimport name.abuchen.portfolio.model.Client;\nimport name.abuchen.portfolio.model.Security;\nimport name.abuchen.portfolio.model.Taxonomy;\n',
    'taxonomy imports')
changed |= c
s, c = replace_once(s,
    '    private Combo underlying;\n    private Combo tradingSymbol;\n',
    '    private Combo underlying;\n    private Combo underlyingClassification;\n    private Combo tradingSymbol;\n',
    'classification combo field')
changed |= c
s, c = replace_once(s,
    '    private Canvas chart;\n',
    '    private Canvas chart;\n    private Canvas tradingSymbolChart;\n',
    'second chart field')
changed |= c

old = '''        Group chartGroup = new Group(body, SWT.NONE);
        chartGroup.setText("Exposure by Maturity");
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(chartGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chartGroup);

        chart = new Canvas(chartGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        chart.setBackground(Colors.theme().defaultBackground());
        chart.addPaintListener(this::paintChart);
        GridDataFactory.fillDefaults().grab(true, true).hint(800, 420).applyTo(chart);
'''
new = '''        TabFolder chartTabs = new TabFolder(body, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chartTabs);

        Composite maturityPage = new Composite(chartTabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(maturityPage);
        chart = new Canvas(maturityPage, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        chart.setBackground(Colors.theme().defaultBackground());
        chart.addPaintListener(e -> paintChart(e, false));
        GridDataFactory.fillDefaults().grab(true, true).hint(800, 420).applyTo(chart);
        TabItem maturityTab = new TabItem(chartTabs, SWT.NONE);
        maturityTab.setText("Exposure by Maturity");
        maturityTab.setControl(maturityPage);

        Composite symbolPage = new Composite(chartTabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(symbolPage);
        tradingSymbolChart = new Canvas(symbolPage, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        tradingSymbolChart.setBackground(Colors.theme().defaultBackground());
        tradingSymbolChart.addPaintListener(e -> paintChart(e, true));
        GridDataFactory.fillDefaults().grab(true, true).hint(800, 420).applyTo(tradingSymbolChart);
        TabItem symbolTab = new TabItem(chartTabs, SWT.NONE);
        symbolTab.setText("Exposure by Trading Symbol");
        symbolTab.setControl(symbolPage);
'''
s, c = replace_once(s, old, new, 'chart tabs')
changed |= c

s, c = replace_once(s,
    'GridLayoutFactory.fillDefaults().numColumns(10).margins(8, 8).spacing(8, 4).applyTo(filters);',
    'GridLayoutFactory.fillDefaults().numColumns(11).margins(8, 8).spacing(8, 4).applyTo(filters);',
    'filter columns')
changed |= c
s, c = replace_once(s,
    '        underlying = combo(filters, "Underlying", ALL);\n        tradingSymbol = combo(filters, "Trading Symbol", ALL);\n',
    '        underlying = combo(filters, "Underlying", ALL);\n        underlyingClassification = combo(filters, "Underlying classification", ALL, "Not specified");\n        tradingSymbol = combo(filters, "Trading Symbol", ALL);\n',
    'classification filter')
changed |= c

old = '''        List.of(exposureType, instrumentType, underlying, tradingSymbol, putCall, direction, maturityRange, groupBy,
                        currency, totalBar).forEach(c -> c.addListener(SWT.Selection, e -> refreshReport()));
'''
new = '''        exposureType.addListener(SWT.Selection, e -> notifyModelUpdated());
        currency.addListener(SWT.Selection, e -> notifyModelUpdated());
        List.of(instrumentType, underlying, underlyingClassification, tradingSymbol, putCall, direction, maturityRange,
                        groupBy, totalBar).forEach(c -> c.addListener(SWT.Selection, e -> refreshReport()));
'''
s, c = replace_once(s, old, new, 'hard refresh listeners')
changed |= c

s, c = replace_once(s,
    '        rebuildUnderlyingFilter(snapshot);\n        rebuildTradingSymbolFilter(snapshot);\n',
    '        rebuildUnderlyingFilter(snapshot);\n        rebuildUnderlyingClassificationFilter(snapshot);\n        rebuildTradingSymbolFilter(snapshot);\n',
    'classification filter rebuild')
changed |= c

old = '''    private void rebuildTradingSymbolFilter(ClientSnapshot snapshot)
    {
        if (tradingSymbol == null)
            return;

        String selected = tradingSymbol.getText();
        Set<String> values = new LinkedHashSet<>();
        values.add(ALL);
        snapshot.getAssetPositions().map(AssetPosition::getSecurity).filter(s -> s != null)
                        .map(Security::getTickerSymbol).filter(s -> s != null && !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER).forEach(values::add);

        tradingSymbol.setItems(values.toArray(String[]::new));
        int index = tradingSymbol.indexOf(selected);
        tradingSymbol.select(index >= 0 ? index : 0);
    }
'''
new = '''    private void rebuildUnderlyingClassificationFilter(ClientSnapshot snapshot)
    {
        if (underlyingClassification == null)
            return;

        String selected = underlyingClassification.getText();
        Set<String> values = new LinkedHashSet<>();
        values.add(ALL);
        values.add("Not specified");
        snapshot.getAssetPositions().map(AssetPosition::getSecurity).filter(s -> s != null)
                        .forEach(s -> values.addAll(underlyingClassifications(s)));

        underlyingClassification.setItems(values.toArray(String[]::new));
        int index = underlyingClassification.indexOf(selected);
        underlyingClassification.select(index >= 0 ? index : 0);
    }

    private void rebuildTradingSymbolFilter(ClientSnapshot snapshot)
    {
        if (tradingSymbol == null)
            return;

        String selected = tradingSymbol.getText();
        Set<String> values = new LinkedHashSet<>();
        values.add(ALL);
        snapshot.getAssetPositions().map(AssetPosition::getSecurity).filter(s -> s != null)
                        .map(this::tradingSymbolLabel).filter(s -> !s.isBlank() && !"No trading symbol".equals(s))
                        .sorted(String.CASE_INSENSITIVE_ORDER).forEach(values::add);

        tradingSymbol.setItems(values.toArray(String[]::new));
        int index = tradingSymbol.indexOf(selected);
        tradingSymbol.select(index >= 0 ? index : 0);
    }
'''
s, c = replace_once(s, old, new, 'classification/trading symbol rebuild')
changed |= c

old = '''        // Exposure type and currency changes require a fresh snapshot/calculation.
        if (selectedExposureType() != currentExposureType || converter == null
                        || !converter.getTermCurrency().equals(currency.getText()))
        {
            notifyModelUpdated();
            return;
        }

'''
s, c = replace_once(s, old, '', 'remove indirect refresh loop')
changed |= c
s, c = replace_once(s,
    '        chart.redraw();\n',
    '        chart.redraw();\n        if (tradingSymbolChart != null && !tradingSymbolChart.isDisposed())\n            tradingSymbolChart.redraw();\n',
    'redraw both charts')
changed |= c

old = '''        if (!ALL.equals(underlying.getText()) && !underlying.getText().equals(row.underlying()))
            return false;
        if (!ALL.equals(tradingSymbol.getText())
                        && (cash || !tradingSymbol.getText().equals(row.security().getTickerSymbol())))
            return false;
'''
new = '''        if (!ALL.equals(underlying.getText()) && !underlying.getText().equals(row.underlying()))
            return false;

        String selectedClassification = underlyingClassification.getText();
        if (!ALL.equals(selectedClassification))
        {
            if (cash || linkedUnderlying(row.security()) == null)
                return false;
            Set<String> classifications = underlyingClassifications(row.security());
            if ("Not specified".equals(selectedClassification))
            {
                if (!classifications.isEmpty())
                    return false;
            }
            else if (!classifications.contains(selectedClassification))
                return false;
        }

        if (!ALL.equals(tradingSymbol.getText())
                        && (cash || !tradingSymbol.getText().equals(tradingSymbolLabel(row.security()))))
            return false;
'''
s, c = replace_once(s, old, new, 'classification matching')
changed |= c

s, c = replace_once(s,
    '    private void paintChart(PaintEvent event)\n    {\n        GC gc = event.gc;\n        Rectangle area = chart.getClientArea();\n',
    '    private void paintChart(PaintEvent event, boolean byTradingSymbol)\n    {\n        GC gc = event.gc;\n        Rectangle area = ((Canvas) event.widget).getClientArea();\n',
    'generic chart painter')
changed |= c

old = '''        filtered.stream().sorted(Comparator.comparing(this::maturitySortKey)).forEach(row -> values
                        .computeIfAbsent(row.maturity(), k -> new LinkedHashMap<>())
                        .merge(groupLabel(row), row.exposure().getAmount(), Long::sum));
'''
new = '''        if (byTradingSymbol)
        {
            filtered.stream().sorted(Comparator.comparing(r -> tradingSymbolLabel(r.security()), String.CASE_INSENSITIVE_ORDER))
                            .forEach(row -> values.computeIfAbsent(tradingSymbolLabel(row.security()),
                                            k -> new LinkedHashMap<>())
                                            .merge(groupLabel(row), row.exposure().getAmount(), Long::sum));
        }
        else
        {
            filtered.stream().sorted(Comparator.comparing(this::maturitySortKey)).forEach(row -> values
                            .computeIfAbsent(row.maturity(), k -> new LinkedHashMap<>())
                            .merge(groupLabel(row), row.exposure().getAmount(), Long::sum));
        }
'''
s, c = replace_once(s, old, new, 'trading symbol bucketing')
changed |= c

marker = '''    private String maturitySortKey(ExposureRow row)
    {
'''
helper = '''    private String tradingSymbolLabel(Security security)
    {
        if (security == null)
            return CASH;
        String contractSymbol = property(security, "contractSymbol");
        if (contractSymbol != null && !contractSymbol.isBlank())
            return contractSymbol;
        String ticker = security.getTickerSymbol();
        return ticker == null || ticker.isBlank() ? "No trading symbol" : ticker;
    }

    private Security linkedUnderlying(Security derivative)
    {
        if (derivative == null)
            return null;
        String uuid = property(derivative, DerivativePositionCalculator.UNDERLYING_SECURITY_UUID);
        if (uuid == null || uuid.isBlank())
            return null;
        return getClient().getSecurities().stream().filter(s -> uuid.equals(s.getUUID())).findFirst().orElse(null);
    }

    private Set<String> underlyingClassifications(Security derivative)
    {
        Security linked = linkedUnderlying(derivative);
        if (linked == null)
            return Set.of();

        Set<String> answer = new LinkedHashSet<>();
        for (Taxonomy taxonomy : getClient().getTaxonomies())
        {
            for (Classification classification : taxonomy.getClassifications(linked))
                answer.add(taxonomy.getName() + ": " + classification.getName());
        }
        return answer;
    }

    private String maturitySortKey(ExposureRow row)
    {
'''
s, c = replace_once(s, marker, helper, 'classification and trading symbol helpers')
changed |= c

p.write_text(s, encoding='utf-8')
Path('patch-changed.txt').write_text('true' if changed else 'false', encoding='utf-8')
print('changed=' + ('true' if changed else 'false'))
