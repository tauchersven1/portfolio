package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.DerivativePositionCalculator;
import name.abuchen.portfolio.snapshot.ExposureCalculator;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimeMachineDropDown;

@SuppressWarnings("nls")
public class ExposureManagementView extends AbstractFinanceView
{
    private static final String ALL = "All";
    private static final String TOTAL = "Total";
    private static final String OPEN_END = "Open End";
    private static final String NO_MATURITY = "No maturity";
    private static final String CASH = "Cash";

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault());

    @Inject
    private ExchangeRateProviderFactory factory;

    private ClientFilterDropDown clientFilter;
    private TimeMachineDropDown timeMachineDropDown;

    private Combo exposureType;
    private Combo chartView;
    private Combo instrumentType;
    private Combo underlying;
    private Combo underlyingClassification;
    private Combo tradingSymbol;
    private Combo putCall;
    private Combo direction;
    private Combo maturityRange;
    private Combo groupBy;
    private Combo currency;
    private Combo totalBar;

    private Label grossValue;
    private Label netValue;
    private Label longValue;
    private Label shortValue;
    private Canvas chart;
    private Canvas tradingSymbolChart;

    private LocalDate valuationDate = LocalDate.now();
    private CurrencyConverter converter;
    private List<ExposureRow> rows = List.of();

    @PostConstruct
    public void setup()
    {
        clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        ExposureManagementView.class.getSimpleName(), filter -> notifyModelUpdated());
        timeMachineDropDown = new TimeMachineDropDown(date -> notifyModelUpdated());
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
    }

    @Override
    protected String getDefaultTitle()
    {
        return "Bestand Exposure";
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(timeMachineDropDown);
        toolBar.add(clientFilter);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite body = new Composite(parent, SWT.NONE);
        body.setBackground(Colors.theme().defaultBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(8, 8).applyTo(body);

        createFilters(body);
        createKpis(body);

        TabFolder chartTabs = new TabFolder(body, SWT.NONE);
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

        notifyModelUpdated();
        return body;
    }

    private void createFilters(Composite parent)
    {
        Group filters = new Group(parent, SWT.NONE);
        filters.setText("Filters");
        GridLayoutFactory.fillDefaults().numColumns(12).margins(8, 8).spacing(8, 4).applyTo(filters);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(filters);

        exposureType = combo(filters, "Exposure basis", "Delta-adjusted", "Notional", "Market value");
        chartView = combo(filters, "Chart view", "Gross Exposure", "Net Exposure (Long / Short)");
        instrumentType = combo(filters, "Instrument type", ALL, "Derivatives", "Option", "Future",
                        "K.O. certificate", "Non-derivatives", CASH);
        underlying = combo(filters, "Underlying", ALL);
        underlyingClassification = combo(filters, "Underlying classification", ALL, "Not specified");
        tradingSymbol = combo(filters, "Trading Symbol", ALL);
        putCall = combo(filters, "Put / Call", ALL, "Call", "Put", "Not specified");
        direction = combo(filters, "Direction", ALL, "Long", "Short");
        maturityRange = combo(filters, "Maturity range", "3M", "6M", "1Y", ALL);
        maturityRange.select(3);
        groupBy = combo(filters, "Group by", "Put / Call", "Instrument type", "Underlying");
        currency = combo(filters, "Currency", getClient().getBaseCurrency());
        getClient().getUsedCurrencies().stream().map(c -> c.getCurrencyCode()).sorted()
                        .filter(c -> currency.indexOf(c) < 0).forEach(currency::add);
        totalBar = combo(filters, "Total bar", "Hide", "Show");

        exposureType.addListener(SWT.Selection, e -> notifyModelUpdated());
        currency.addListener(SWT.Selection, e -> notifyModelUpdated());
        List.of(chartView, instrumentType, underlying, underlyingClassification, tradingSymbol, putCall, direction,
                        maturityRange, groupBy, totalBar).forEach(c -> c.addListener(SWT.Selection, e -> refreshReport()));
    }

    private Combo combo(Composite parent, String label, String... items)
    {
        Composite box = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(box);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(box);

        new Label(box, SWT.NONE).setText(label);
        Combo combo = new Combo(box, SWT.READ_ONLY);
        combo.setItems(items);
        combo.select(0);
        GridDataFactory.fillDefaults().grab(true, false).hint(130, SWT.DEFAULT).applyTo(combo);
        return combo;
    }

    private void createKpis(Composite parent)
    {
        Composite kpis = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(true).spacing(8, 0).applyTo(kpis);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(kpis);

        grossValue = kpi(kpis, "Gross Exposure");
        netValue = kpi(kpis, "Net Exposure");
        longValue = kpi(kpis, "Long Exposure");
        shortValue = kpi(kpis, "Short Exposure");
    }

    private Label kpi(Composite parent, String title)
    {
        Group group = new Group(parent, SWT.NONE);
        group.setText(title);
        GridLayoutFactory.fillDefaults().margins(10, 8).applyTo(group);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

        Label value = new Label(group, SWT.RIGHT);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(value);
        value.setText("-");
        return value;
    }

    @Override
    public void notifyModelUpdated()
    {
        if (clientFilter == null)
            return;

        valuationDate = timeMachineDropDown.getTimeMachineDate().orElse(LocalDate.now());
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);

        String reportingCurrency = currency == null || currency.getSelectionIndex() < 0 ? getClient().getBaseCurrency()
                        : currency.getText();
        converter = new CurrencyConverterImpl(factory, reportingCurrency);

        ClientSnapshot snapshot = ClientSnapshot.create(filteredClient, converter, valuationDate);
        rebuildSecurityFilters(snapshot);
        refreshRows(snapshot);
        refreshReport();
        updateTitle(getDefaultTitle() + " | " + Values.Date.format(valuationDate));
    }

    private void rebuildSecurityFilters(ClientSnapshot snapshot)
    {
        rebuildUnderlyingFilter(snapshot);
        rebuildUnderlyingClassificationFilter(snapshot);
        rebuildTradingSymbolFilter(snapshot);
    }

    private void rebuildUnderlyingFilter(ClientSnapshot snapshot)
    {
        if (underlying == null)
            return;

        String selected = underlying.getText();
        Set<String> values = new LinkedHashSet<>();
        values.add(ALL);
        snapshot.getAssetPositions().map(AssetPosition::getSecurity).filter(s -> s != null)
                        .map(s -> property(s, DerivativePositionCalculator.UNDERLYING)).filter(s -> s != null && !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER).forEach(values::add);

        underlying.setItems(values.toArray(String[]::new));
        int index = underlying.indexOf(selected);
        underlying.select(index >= 0 ? index : 0);
    }

    private void rebuildUnderlyingClassificationFilter(ClientSnapshot snapshot)
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

    private void refreshRows(ClientSnapshot snapshot)
    {
        ExposureType type = selectedExposureType();
        List<ExposureRow> answer = new ArrayList<>();

        snapshot.getAssetPositions().forEach(asset -> {
            Security security = asset.getSecurity();
            SecurityPosition position = asset.getPosition();

            if (security == null)
            {
                Money exposure = asset.getValuation();
                if (!exposure.isZero())
                    answer.add(new ExposureRow(null, position, exposure, NO_MATURITY, null, CASH, CASH,
                                    asset.getDescription()));
                return;
            }

            Money exposure = ExposureCalculator.calculate(getClient(), position, valuationDate, converter, type);
            if (exposure == null || exposure.isZero())
                return;

            answer.add(new ExposureRow(security, position, exposure, maturity(security), maturityDate(security),
                            instrumentLabel(security), putCallLabel(security), underlyingLabel(security)));
        });
        rows = answer;
    }

    private void refreshReport()
    {
        if (chart == null || chart.isDisposed())
            return;

        List<ExposureRow> filtered = rows.stream().filter(this::matchesFilters).toList();
        long gross = filtered.stream().mapToLong(r -> Math.abs(r.exposure().getAmount())).sum();
        long net = filtered.stream().mapToLong(r -> r.exposure().getAmount()).sum();
        long longExposure = filtered.stream().mapToLong(r -> Math.max(0L, r.exposure().getAmount())).sum();
        long shortExposure = filtered.stream().mapToLong(r -> Math.min(0L, r.exposure().getAmount())).sum();

        String ccy = converter.getTermCurrency();
        grossValue.setText(Values.Money.format(Money.of(ccy, gross)));
        netValue.setText(Values.Money.format(Money.of(ccy, net)));
        longValue.setText(Values.Money.format(Money.of(ccy, longExposure)));
        shortValue.setText(Values.Money.format(Money.of(ccy, shortExposure)));

        chart.redraw();
        if (tradingSymbolChart != null && !tradingSymbolChart.isDisposed())
            tradingSymbolChart.redraw();
    }

    private ExposureType selectedExposureType()
    {
        if (exposureType == null)
            return ExposureType.DELTA_ADJUSTED;
        return switch (exposureType.getSelectionIndex())
        {
            case 1 -> ExposureType.NOTIONAL;
            case 2 -> ExposureType.MARKET_VALUE;
            default -> ExposureType.DELTA_ADJUSTED;
        };
    }

    private boolean matchesFilters(ExposureRow row)
    {
        String selectedInstrument = instrumentType.getText();
        boolean cash = row.security() == null;
        boolean derivative = !cash && DerivativePositionCalculator.getDerivativeType(row.security()) != null;
        if ("Derivatives".equals(selectedInstrument) && !derivative)
            return false;
        if ("Option".equals(selectedInstrument) && (cash || !DerivativePositionCalculator.isOption(row.security())))
            return false;
        if ("Future".equals(selectedInstrument) && (cash || !DerivativePositionCalculator.isFuture(row.security())))
            return false;
        if ("K.O. certificate".equals(selectedInstrument)
                        && (cash || !ExposureCalculator.isKnockoutCertificate(row.security())))
            return false;
        if ("Non-derivatives".equals(selectedInstrument) && derivative)
            return false;
        if (CASH.equals(selectedInstrument) && !cash)
            return false;

        if (!ALL.equals(underlying.getText()) && !underlying.getText().equals(row.underlying()))
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
        if (!ALL.equals(putCall.getText()) && !putCall.getText().equals(row.putCall()))
            return false;
        if ("Long".equals(direction.getText()) && !row.exposure().isPositive())
            return false;
        if ("Short".equals(direction.getText()) && !row.exposure().isNegative())
            return false;

        if (row.maturityDate() != null && !ALL.equals(maturityRange.getText()))
        {
            int months = switch (maturityRange.getText())
            {
                case "3M" -> 3;
                case "6M" -> 6;
                case "1Y" -> 12;
                default -> Integer.MAX_VALUE;
            };
            if (row.maturityDate().isAfter(valuationDate.plusMonths(months)))
                return false;
        }

        return true;
    }

    private void paintChart(PaintEvent event, boolean byTradingSymbol)
    {
        GC gc = event.gc;
        Rectangle area = ((Canvas) event.widget).getClientArea();
        gc.setForeground(Colors.theme().defaultForeground());

        List<ExposureRow> filtered = rows.stream().filter(this::matchesFilters).toList();
        if (filtered.isEmpty())
        {
            gc.drawText("No exposure for the selected filters", 20, 20, true);
            return;
        }

        if (chartView != null && chartView.getSelectionIndex() == 1)
        {
            int gap = 16;
            int half = Math.max(1, (area.height - gap) / 2);
            Rectangle longArea = new Rectangle(area.x, area.y, area.width, half);
            Rectangle shortArea = new Rectangle(area.x, area.y + half + gap, area.width,
                            Math.max(1, area.height - half - gap));
            paintPositiveChart(gc, longArea, filtered.stream().filter(r -> r.exposure().isPositive()).toList(),
                            byTradingSymbol, "Long Exposure", false);
            paintPositiveChart(gc, shortArea, filtered.stream().filter(r -> r.exposure().isNegative()).toList(),
                            byTradingSymbol, "Short Exposure", true);
        }
        else
        {
            paintPositiveChart(gc, area, filtered, byTradingSymbol, "Gross Exposure", true);
        }
    }

    private void paintPositiveChart(GC gc, Rectangle area, List<ExposureRow> chartRows, boolean byTradingSymbol,
                    String title, boolean absoluteValues)
    {
        gc.setForeground(Colors.theme().defaultForeground());
        gc.drawText(title, 12, area.y + 8, true);

        if (chartRows.isEmpty())
        {
            gc.drawText("No exposure", 20, area.y + 32, true);
            return;
        }

        Map<String, Map<String, Long>> values = new LinkedHashMap<>();
        if (totalBar != null && totalBar.getSelectionIndex() == 1)
        {
            Map<String, Long> total = new LinkedHashMap<>();
            chartRows.forEach(row -> total.merge(groupLabel(row), chartValue(row, absoluteValues), Long::sum));
            values.put(TOTAL, total);
        }

        if (byTradingSymbol)
        {
            chartRows.stream().sorted(Comparator.comparing(r -> tradingSymbolLabel(r.security()), String.CASE_INSENSITIVE_ORDER))
                            .forEach(row -> values.computeIfAbsent(tradingSymbolLabel(row.security()),
                                            k -> new LinkedHashMap<>())
                                            .merge(groupLabel(row), chartValue(row, absoluteValues), Long::sum));
        }
        else
        {
            chartRows.stream().sorted(Comparator.comparing(this::maturitySortKey)).forEach(row -> values
                            .computeIfAbsent(row.maturity(), k -> new LinkedHashMap<>())
                            .merge(groupLabel(row), chartValue(row, absoluteValues), Long::sum));
        }

        Set<String> groups = new LinkedHashSet<>();
        values.values().forEach(v -> groups.addAll(v.keySet()));
        long max = values.values().stream()
                        .mapToLong(bucket -> bucket.values().stream().mapToLong(Long::longValue).sum()).max().orElse(1L);
        if (max <= 0)
            max = 1;

        int left = 90;
        int right = 20;
        int top = 40;
        int bottom = byTradingSymbol ? 72 : 50;
        int plotX = area.x + left;
        int plotY = area.y + top;
        int plotWidth = Math.max(1, area.width - left - right);
        int plotHeight = Math.max(1, area.height - top - bottom);
        int baselineY = plotY + plotHeight;

        gc.setForeground(Colors.GRAY);
        gc.drawLine(plotX, baselineY, area.x + area.width - right, baselineY);
        gc.drawLine(plotX, plotY, plotX, baselineY);
        gc.setForeground(Colors.theme().defaultForeground());
        gc.drawText(Values.Money.format(Money.of(converter.getTermCurrency(), max)), area.x + 2, plotY - 8, true);
        gc.drawText("0", area.x + 2, baselineY - 8, true);

        Color[] palette = { Colors.ICON_BLUE, Colors.ICON_ORANGE, Colors.ICON_GREEN, Colors.EQUITY,
                        Colors.OTHER_CATEGORY, Colors.DARK_BLUE };
        List<String> groupList = new ArrayList<>(groups);
        int legendX = plotX;
        for (int i = 0; i < groupList.size(); i++)
        {
            gc.setBackground(palette[i % palette.length]);
            gc.fillRectangle(legendX, area.y + 8, 12, 12);
            gc.setForeground(Colors.theme().defaultForeground());
            gc.drawText(groupList.get(i), legendX + 17, area.y + 6, true);
            legendX += 25 + gc.textExtent(groupList.get(i)).x;
        }

        int count = values.size();
        double step = plotWidth / (double) count;
        int barWidth = Math.max(8, (int) Math.min(60, step * 0.62));
        int bucketIndex = 0;
        for (Map.Entry<String, Map<String, Long>> bucket : values.entrySet())
        {
            int centerX = plotX + (int) Math.round((bucketIndex + 0.5) * step);
            int x = centerX - barWidth / 2;
            int y = baselineY;

            for (int groupIndex = 0; groupIndex < groupList.size(); groupIndex++)
            {
                long value = bucket.getValue().getOrDefault(groupList.get(groupIndex), 0L);
                if (value <= 0)
                    continue;
                int height = Math.max(1, (int) Math.round(value * (double) plotHeight / max));
                y -= height;
                gc.setBackground(palette[groupIndex % palette.length]);
                gc.fillRectangle(x, y, barWidth, height);
            }

            gc.setForeground(Colors.theme().defaultForeground());
            String label = bucket.getKey();
            int labelY = baselineY + 8;
            if (byTradingSymbol)
            {
                int maxLabelWidth = Math.max(24, (int) Math.floor(step * 1.8) - 8);
                label = fitAxisLabel(gc, label, maxLabelWidth);
                labelY += (bucketIndex % 2) * 18;
            }
            int textWidth = gc.textExtent(label).x;
            gc.drawText(label, centerX - textWidth / 2, labelY, true);
            bucketIndex++;
        }
    }

    private long chartValue(ExposureRow row, boolean absoluteValue)
    {
        long value = row.exposure().getAmount();
        return absoluteValue ? Math.abs(value) : Math.max(0L, value);
    }

    private String fitAxisLabel(GC gc, String label, int maxWidth)
    {
        if (gc.textExtent(label).x <= maxWidth)
            return label;

        String suffix = "...";
        int length = label.length();
        while (length > 1 && gc.textExtent(label.substring(0, length) + suffix).x > maxWidth)
            length--;
        return label.substring(0, length) + suffix;
    }

    private String groupLabel(ExposureRow row)
    {
        return switch (groupBy.getSelectionIndex())
        {
            case 1 -> row.instrumentType();
            case 2 -> row.underlying();
            default -> row.putCall();
        };
    }

    private String tradingSymbolLabel(Security security)
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
        if (row.maturityDate() != null)
            return row.maturityDate().toString();
        if (OPEN_END.equals(row.maturity()))
            return "9998";
        return "9999";
    }

    private String maturity(Security security)
    {
        LocalDate date = maturityDate(security);
        if (date != null)
            return YearMonth.from(date).format(MONTH_FORMAT);
        return ExposureCalculator.isKnockoutCertificate(security) ? OPEN_END : NO_MATURITY;
    }

    private LocalDate maturityDate(Security security)
    {
        String value = property(security, "expirationDate");
        if (value == null || value.isBlank())
            return null;
        try
        {
            return LocalDate.parse(value);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private String instrumentLabel(Security security)
    {
        if (ExposureCalculator.isKnockoutCertificate(security))
            return "K.O.";
        if (DerivativePositionCalculator.isOption(security))
            return "Option";
        if (DerivativePositionCalculator.isFuture(security))
            return "Future";
        return "Security";
    }

    private String putCallLabel(Security security)
    {
        if (ExposureCalculator.isKnockoutCertificate(security))
            return "K.O.";
        if (DerivativePositionCalculator.isFuture(security))
            return "Future";
        String value = property(security, "putCall");
        if ("CALL".equals(value))
            return "Call";
        if ("PUT".equals(value))
            return "Put";
        return "Not specified";
    }

    private String underlyingLabel(Security security)
    {
        String value = property(security, DerivativePositionCalculator.UNDERLYING);
        return value == null || value.isBlank() ? security.getName() : value;
    }

    private static String property(Security security, String name)
    {
        return security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, name).orElse(null);
    }

    private record ExposureRow(Security security, SecurityPosition position, Money exposure, String maturity,
                    LocalDate maturityDate, String instrumentType, String putCall, String underlying)
    {
    }
}
