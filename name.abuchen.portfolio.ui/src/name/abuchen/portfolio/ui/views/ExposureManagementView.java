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

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
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
    private static final String OPEN_END = "Open End";
    private static final String NO_MATURITY = "No maturity";

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault());

    @Inject
    private ExchangeRateProviderFactory factory;

    private ClientFilterDropDown clientFilter;
    private TimeMachineDropDown timeMachineDropDown;

    private Combo exposureType;
    private Combo instrumentType;
    private Combo underlying;
    private Combo putCall;
    private Combo direction;
    private Combo maturityRange;
    private Combo groupBy;
    private Combo currency;

    private Label grossValue;
    private Label netValue;
    private Label longValue;
    private Label shortValue;
    private Canvas chart;

    private LocalDate valuationDate = LocalDate.now();
    private CurrencyConverter converter;
    private ExposureType currentExposureType = ExposureType.DELTA_ADJUSTED;
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
        return "Exposuremanagement";
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

        Group chartGroup = new Group(body, SWT.NONE);
        chartGroup.setText("Exposure by Maturity");
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(chartGroup);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chartGroup);

        chart = new Canvas(chartGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        chart.setBackground(Colors.theme().defaultBackground());
        chart.addPaintListener(this::paintChart);
        GridDataFactory.fillDefaults().grab(true, true).hint(800, 420).applyTo(chart);

        notifyModelUpdated();
        return body;
    }

    private void createFilters(Composite parent)
    {
        Group filters = new Group(parent, SWT.NONE);
        filters.setText("Filters");
        GridLayoutFactory.fillDefaults().numColumns(8).margins(8, 8).spacing(8, 4).applyTo(filters);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(filters);

        exposureType = combo(filters, "Exposure type", "Delta adjusted", "Notional", "Market value");
        instrumentType = combo(filters, "Instrument type", "All", "Derivatives", "Option", "Future",
                        "K.O. certificate", "Non-derivatives", "Cash");
        instrumentType.select(1);
        underlying = combo(filters, "Underlying", ALL);
        putCall = combo(filters, "Put / Call", ALL, "Call", "Put", "Not specified");
        direction = combo(filters, "Direction", ALL, "Long", "Short");
        maturityRange = combo(filters, "Maturity range", "3M", "6M", "1Y", ALL);
        maturityRange.select(3);
        groupBy = combo(filters, "Group by", "Put / Call", "Instrument type", "Underlying");
        currency = combo(filters, "Currency", getClient().getBaseCurrency());
        getClient().getUsedCurrencies().stream().map(c -> c.getCurrencyCode()).sorted()
                        .filter(c -> currency.indexOf(c) < 0).forEach(currency::add);

        List.of(exposureType, instrumentType, underlying, putCall, direction, maturityRange, groupBy, currency)
                        .forEach(c -> c.addListener(SWT.Selection, e -> refreshReport()));
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
        rebuildUnderlyingFilter(snapshot);
        refreshRows(snapshot);
        refreshReport();
        updateTitle(getDefaultTitle() + " | " + Values.Date.format(valuationDate));
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

    private void refreshRows(ClientSnapshot snapshot)
    {
        ExposureType type = selectedExposureType();
        currentExposureType = type;
        List<ExposureRow> answer = new ArrayList<>();

        snapshot.getAssetPositions().forEach(asset -> {
            Security security = asset.getSecurity();
            SecurityPosition position = asset.getPosition();

            if (security == null)
            {
                Money exposure = asset.getValuation();
                if (!exposure.isZero())
                    answer.add(new ExposureRow(null, position, exposure, NO_MATURITY, null, "Cash", "Cash",
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

        // Exposure type and currency changes require a fresh snapshot/calculation.
        if (selectedExposureType() != currentExposureType || converter == null
                        || !converter.getTermCurrency().equals(currency.getText()))
        {
            notifyModelUpdated();
            return;
        }

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
        if ("Option".equals(selectedInstrument)
                        && (cash || !DerivativePositionCalculator.isOption(row.security())))
            return false;
        if ("Future".equals(selectedInstrument)
                        && (cash || !DerivativePositionCalculator.isFuture(row.security())))
            return false;
        if ("K.O. certificate".equals(selectedInstrument)
                        && (cash || !ExposureCalculator.isKnockoutCertificate(row.security())))
            return false;
        if ("Non-derivatives".equals(selectedInstrument) && (cash || derivative))
            return false;
        if ("Cash".equals(selectedInstrument) && !cash)
            return false;

        if (!ALL.equals(underlying.getText()) && !underlying.getText().equals(row.underlying()))
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

    private void paintChart(PaintEvent event)
    {
        GC gc = event.gc;
        Rectangle area = chart.getClientArea();
        gc.setForeground(Colors.theme().defaultForeground());

        List<ExposureRow> filtered = rows.stream().filter(this::matchesFilters).toList();
        if (filtered.isEmpty())
        {
            gc.drawText("No exposure for the selected filters", 20, 20, true);
            return;
        }

        Map<String, Map<String, Long>> values = new LinkedHashMap<>();
        filtered.stream().sorted(Comparator.comparing(this::maturitySortKey)).forEach(row -> values
                        .computeIfAbsent(row.maturity(), k -> new LinkedHashMap<>())
                        .merge(groupLabel(row), row.exposure().getAmount(), Long::sum));

        Set<String> groups = new LinkedHashSet<>();
        values.values().forEach(v -> groups.addAll(v.keySet()));

        long positiveMax = 0;
        long negativeMax = 0;
        for (Map<String, Long> bucket : values.values())
        {
            long positive = bucket.values().stream().mapToLong(v -> Math.max(0L, v)).sum();
            long negative = bucket.values().stream().mapToLong(v -> Math.min(0L, v)).sum();
            positiveMax = Math.max(positiveMax, positive);
            negativeMax = Math.min(negativeMax, negative);
        }

        int left = 90;
        int right = 20;
        int top = 44;
        int bottom = 55;
        int plotWidth = Math.max(1, area.width - left - right);
        int plotHeight = Math.max(1, area.height - top - bottom);
        long span = positiveMax - negativeMax;
        if (span == 0)
            span = 1;
        int zeroY = top + (int) Math.round(positiveMax * (double) plotHeight / span);

        gc.setForeground(Colors.GRAY);
        gc.drawLine(left, zeroY, area.width - right, zeroY);
        gc.drawLine(left, top, left, top + plotHeight);

        gc.setForeground(Colors.theme().defaultForeground());
        gc.drawText(Values.Money.format(Money.of(converter.getTermCurrency(), positiveMax)), 2, top - 8, true);
        gc.drawText(Values.Money.format(Money.of(converter.getTermCurrency(), negativeMax)), 2,
                        top + plotHeight - 8, true);

        Color[] palette = { Colors.ICON_BLUE, Colors.ICON_ORANGE, Colors.ICON_GREEN, Colors.EQUITY,
                        Colors.OTHER_CATEGORY, Colors.DARK_BLUE };
        List<String> groupList = new ArrayList<>(groups);
        int legendX = left;
        for (int i = 0; i < groupList.size(); i++)
        {
            gc.setBackground(palette[i % palette.length]);
            gc.fillRectangle(legendX, 8, 12, 12);
            gc.setForeground(Colors.theme().defaultForeground());
            gc.drawText(groupList.get(i), legendX + 17, 6, true);
            legendX += 25 + gc.textExtent(groupList.get(i)).x;
        }

        int count = values.size();
        double step = plotWidth / (double) count;
        int barWidth = Math.max(8, (int) Math.min(60, step * 0.62));
        int bucketIndex = 0;
        for (Map.Entry<String, Map<String, Long>> bucket : values.entrySet())
        {
            int x = left + (int) Math.round((bucketIndex + 0.5) * step) - barWidth / 2;
            int posY = zeroY;
            int negY = zeroY;

            for (int groupIndex = 0; groupIndex < groupList.size(); groupIndex++)
            {
                long value = bucket.getValue().getOrDefault(groupList.get(groupIndex), 0L);
                if (value == 0)
                    continue;

                int height = Math.max(1, (int) Math.round(Math.abs(value) * (double) plotHeight / span));
                gc.setBackground(palette[groupIndex % palette.length]);
                if (value > 0)
                {
                    posY -= height;
                    gc.fillRectangle(x, posY, barWidth, height);
                }
                else
                {
                    gc.fillRectangle(x, negY, barWidth, height);
                    negY += height;
                }
            }

            gc.setForeground(Colors.theme().defaultForeground());
            String label = bucket.getKey();
            int textWidth = gc.textExtent(label).x;
            gc.drawText(label, x + (barWidth - textWidth) / 2, top + plotHeight + 8, true);
            bucketIndex++;
        }
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
