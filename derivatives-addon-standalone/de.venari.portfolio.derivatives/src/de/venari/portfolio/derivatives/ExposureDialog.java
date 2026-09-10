package de.venari.portfolio.derivatives;

import java.math.BigDecimal;
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
import java.util.function.Function;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.DerivativeExposure;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

public class ExposureDialog extends Dialog
{
    private static final String ALL = "All"; //$NON-NLS-1$
    private static final String NO_MATURITY = "No maturity"; //$NON-NLS-1$
    private static final String OPEN_END = "Open End"; //$NON-NLS-1$
    private static final String TOTAL = "Total"; //$NON-NLS-1$

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault());

    private record Row(Security security, Money value, BigDecimal multiplier, Money exposure, String maturity,
                    LocalDate maturityDate, String instrumentType, String putCall, String underlying,
                    String tradingSymbol)
    {
    }

    private final Client client;
    private final ExchangeRateProviderFactory factory;
    private final MPart activePart;

    private Combo instrumentType;
    private Combo direction;
    private Combo maturityRange;
    private Combo groupBy;
    private Combo totalBar;

    private Label grossValue;
    private Label netValue;
    private Label longValue;
    private Label shortValue;

    private Canvas maturityChart;
    private Canvas symbolChart;
    private TableViewer table;

    private List<Row> rows = List.of();
    private CurrencyConverter converter;
    private LocalDate valuationDate = LocalDate.now();

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
        newShell.setSize(1180, 760);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayoutFactory.fillDefaults().margins(10, 10).spacing(8, 8).applyTo(container);

        converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

        createFilters(container);
        createKpis(container);
        createTabs(container);

        getShell().getDisplay().asyncExec(this::refresh);
        return container;
    }

    private void createFilters(Composite parent)
    {
        Group filters = new Group(parent, SWT.NONE);
        filters.setText("Filters"); //$NON-NLS-1$
        GridLayoutFactory.fillDefaults().numColumns(5).margins(8, 8).spacing(10, 4).applyTo(filters);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(filters);

        instrumentType = combo(filters, "Instrument type", ALL, "Derivatives", "Option", "Future", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        "K.O. certificate", "Non-derivatives"); //$NON-NLS-1$ //$NON-NLS-2$
        direction = combo(filters, "Direction", ALL, "Long", "Short"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        maturityRange = combo(filters, "Maturity range", ALL, "3M", "6M", "1Y"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        groupBy = combo(filters, "Group by", "Put / Call", "Instrument type", "Underlying"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        totalBar = combo(filters, "Total bar", "Show", "Hide"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        List.of(instrumentType, direction, maturityRange, groupBy, totalBar)
                        .forEach(c -> c.addListener(SWT.Selection, e -> refreshReport()));
    }

    private Combo combo(Composite parent, String label, String... items)
    {
        Composite box = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().spacing(0, 2).applyTo(box);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(box);

        new Label(box, SWT.NONE).setText(label);
        Combo combo = new Combo(box, SWT.READ_ONLY);
        combo.setItems(items);
        combo.select(0);
        GridDataFactory.fillDefaults().grab(true, false).hint(150, SWT.DEFAULT).applyTo(combo);
        return combo;
    }

    private void createKpis(Composite parent)
    {
        Composite kpis = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(true).spacing(8, 0).applyTo(kpis);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(kpis);

        grossValue = kpi(kpis, "Gross Exposure"); //$NON-NLS-1$
        netValue = kpi(kpis, "Net Exposure"); //$NON-NLS-1$
        longValue = kpi(kpis, "Long Exposure"); //$NON-NLS-1$
        shortValue = kpi(kpis, "Short Exposure"); //$NON-NLS-1$
    }

    private Label kpi(Composite parent, String title)
    {
        Group group = new Group(parent, SWT.NONE);
        group.setText(title);
        GridLayoutFactory.fillDefaults().margins(10, 8).applyTo(group);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

        Label value = new Label(group, SWT.RIGHT);
        value.setText("-"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(value);
        return value;
    }

    private void createTabs(Composite parent)
    {
        TabFolder tabs = new TabFolder(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabs);

        Composite maturityPage = new Composite(tabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(maturityPage);
        maturityChart = new Canvas(maturityPage, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        maturityChart.addPaintListener(e -> paintChart(e, false));
        GridDataFactory.fillDefaults().grab(true, true).hint(900, 420).applyTo(maturityChart);
        TabItem maturityTab = new TabItem(tabs, SWT.NONE);
        maturityTab.setText("Exposure by Maturity"); //$NON-NLS-1$
        maturityTab.setControl(maturityPage);

        Composite symbolPage = new Composite(tabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(symbolPage);
        symbolChart = new Canvas(symbolPage, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        symbolChart.addPaintListener(e -> paintChart(e, true));
        GridDataFactory.fillDefaults().grab(true, true).hint(900, 420).applyTo(symbolChart);
        TabItem symbolTab = new TabItem(tabs, SWT.NONE);
        symbolTab.setText("Exposure by Trading Symbol"); //$NON-NLS-1$
        symbolTab.setControl(symbolPage);

        Composite tablePage = new Composite(tabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(tablePage);
        table = new TableViewer(tablePage, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);
        table.setContentProvider(ArrayContentProvider.getInstance());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table.getControl());

        addColumn("Instrument", 300, row -> row.security().getName()); //$NON-NLS-1$
        addColumn("Market value", 160, row -> Values.Money.format(row.value())); //$NON-NLS-1$
        addColumn("Multiplier", 100, row -> row.multiplier().toPlainString()); //$NON-NLS-1$
        addColumn("Exposure", 160, row -> Values.Money.format(row.exposure())); //$NON-NLS-1$
        addColumn("Maturity", 120, Row::maturity); //$NON-NLS-1$
        addColumn("Underlying", 220, Row::underlying); //$NON-NLS-1$
        table.getTable().addListener(SWT.MouseDoubleClick, event -> editMultiplier());

        TabItem tableTab = new TabItem(tabs, SWT.NONE);
        tableTab.setText("Positions"); //$NON-NLS-1$
        tableTab.setControl(tablePage);
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
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, valuationDate);
        List<Row> answer = new ArrayList<>();
        snapshot.getAssetPositions().forEach(asset -> addRow(answer, asset));
        rows = answer;

        if (table != null && !table.getControl().isDisposed())
            table.setInput(rows);

        refreshReport();
    }

    private void addRow(List<Row> answer, AssetPosition asset)
    {
        Security security = asset.getSecurity();
        if (security == null)
            return;

        Money value = asset.getValuation();
        DerivativeExposure.Result result = DerivativeExposure.calculate(client, asset, valuationDate);
        BigDecimal multiplier = result.leverage() != null ? result.leverage()
                        : result.multiplier().multiply(result.delta(), Values.MC);
        Money exposure = result.net();

        LocalDate maturityDate = maturityDate(security);
        String maturity = maturityDate != null ? YearMonth.from(maturityDate).format(MONTH_FORMAT)
                        : isKnockout(security) ? OPEN_END : NO_MATURITY;

        answer.add(new Row(security, value, multiplier, exposure, maturity, maturityDate,
                        instrumentType(security), putCall(security), underlying(security), tradingSymbol(security)));
    }

    private void refreshReport()
    {
        if (maturityChart == null || maturityChart.isDisposed())
            return;

        List<Row> filtered = rows.stream().filter(this::matchesFilters).toList();

        long gross = filtered.stream().mapToLong(r -> Math.abs(r.exposure().getAmount())).sum();
        long net = filtered.stream().mapToLong(r -> r.exposure().getAmount()).sum();
        long longExposure = filtered.stream().mapToLong(r -> Math.max(0L, r.exposure().getAmount())).sum();
        long shortExposure = filtered.stream().mapToLong(r -> Math.min(0L, r.exposure().getAmount())).sum();

        String ccy = converter.getTermCurrency();
        grossValue.setText(Values.Money.format(Money.of(ccy, gross)));
        netValue.setText(Values.Money.format(Money.of(ccy, net)));
        longValue.setText(Values.Money.format(Money.of(ccy, longExposure)));
        shortValue.setText(Values.Money.format(Money.of(ccy, shortExposure)));

        if (table != null && !table.getControl().isDisposed())
            table.setInput(filtered);

        maturityChart.redraw();
        symbolChart.redraw();
    }

    private boolean matchesFilters(Row row)
    {
        String type = instrumentType.getText();
        boolean derivative = !"Security".equals(row.instrumentType()); //$NON-NLS-1$

        if ("Derivatives".equals(type) && !derivative) //$NON-NLS-1$
            return false;
        if ("Option".equals(type) && !"Option".equals(row.instrumentType())) //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        if ("Future".equals(type) && !"Future".equals(row.instrumentType())) //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        if ("K.O. certificate".equals(type) && !"K.O.".equals(row.instrumentType())) //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        if ("Non-derivatives".equals(type) && derivative) //$NON-NLS-1$
            return false;

        if ("Long".equals(direction.getText()) && !row.exposure().isPositive()) //$NON-NLS-1$
            return false;
        if ("Short".equals(direction.getText()) && !row.exposure().isNegative()) //$NON-NLS-1$
            return false;

        if (row.maturityDate() != null && !ALL.equals(maturityRange.getText()))
        {
            int months = switch (maturityRange.getText())
            {
                case "3M" -> 3; //$NON-NLS-1$
                case "6M" -> 6; //$NON-NLS-1$
                case "1Y" -> 12; //$NON-NLS-1$
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

        List<Row> filtered = rows.stream().filter(this::matchesFilters).toList();
        if (filtered.isEmpty())
        {
            gc.drawText("No exposure for the selected filters", 20, 20, true); //$NON-NLS-1$
            return;
        }

        Map<String, Map<String, Long>> values = new LinkedHashMap<>();

        if (totalBar.getSelectionIndex() == 0)
        {
            Map<String, Long> total = new LinkedHashMap<>();
            filtered.forEach(row -> total.merge(groupLabel(row), row.exposure().getAmount(), Long::sum));
            values.put(TOTAL, total);
        }

        Comparator<Row> comparator = byTradingSymbol
                        ? Comparator.comparing(Row::tradingSymbol, String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparing(this::maturitySortKey);

        filtered.stream().sorted(comparator).forEach(row -> values
                        .computeIfAbsent(byTradingSymbol ? row.tradingSymbol() : row.maturity(),
                                        key -> new LinkedHashMap<>())
                        .merge(groupLabel(row), row.exposure().getAmount(), Long::sum));

        Set<String> groups = new LinkedHashSet<>();
        values.values().forEach(bucket -> groups.addAll(bucket.keySet()));

        long positiveMax = 0;
        long negativeMax = 0;
        for (Map<String, Long> bucket : values.values())
        {
            long positive = bucket.values().stream().mapToLong(v -> Math.max(0L, v)).sum();
            long negative = bucket.values().stream().mapToLong(v -> Math.min(0L, v)).sum();
            positiveMax = Math.max(positiveMax, positive);
            negativeMax = Math.min(negativeMax, negative);
        }

        int left = 100;
        int right = 20;
        int top = 46;
        int bottom = byTradingSymbol ? 78 : 55;
        int plotWidth = Math.max(1, area.width - left - right);
        int plotHeight = Math.max(1, area.height - top - bottom);
        long span = positiveMax - negativeMax;
        if (span == 0)
            span = 1;

        int zeroY = top + (int) Math.round(positiveMax * (double) plotHeight / span);

        gc.drawLine(left, zeroY, area.width - right, zeroY);
        gc.drawLine(left, top, left, top + plotHeight);
        gc.drawText(Values.Money.format(Money.of(converter.getTermCurrency(), positiveMax)), 4, top - 8, true);
        gc.drawText(Values.Money.format(Money.of(converter.getTermCurrency(), negativeMax)), 4,
                        top + plotHeight - 8, true);

        int[] systemColors = { SWT.COLOR_BLUE, SWT.COLOR_DARK_YELLOW, SWT.COLOR_DARK_GREEN, SWT.COLOR_MAGENTA,
                        SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_RED };
        List<String> groupList = new ArrayList<>(groups);

        int legendX = left;
        for (int i = 0; i < groupList.size(); i++)
        {
            Color color = getShell().getDisplay().getSystemColor(systemColors[i % systemColors.length]);
            gc.setBackground(color);
            gc.fillRectangle(legendX, 10, 12, 12);
            gc.drawText(groupList.get(i), legendX + 17, 8, true);
            legendX += 25 + gc.textExtent(groupList.get(i)).x;
        }

        int count = values.size();
        double step = plotWidth / (double) count;
        int barWidth = Math.max(8, (int) Math.min(60, step * 0.62));

        int bucketIndex = 0;
        for (Map.Entry<String, Map<String, Long>> bucket : values.entrySet())
        {
            int centerX = left + (int) Math.round((bucketIndex + 0.5) * step);
            int x = centerX - barWidth / 2;
            int posY = zeroY;
            int negY = zeroY;

            for (int groupIndex = 0; groupIndex < groupList.size(); groupIndex++)
            {
                long value = bucket.getValue().getOrDefault(groupList.get(groupIndex), 0L);
                if (value == 0)
                    continue;

                int height = Math.max(1, (int) Math.round(Math.abs(value) * (double) plotHeight / span));
                gc.setBackground(getShell().getDisplay()
                                .getSystemColor(systemColors[groupIndex % systemColors.length]));

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

            String label = fitAxisLabel(gc, bucket.getKey(), Math.max(32, (int) Math.floor(step * 1.7)));
            int textWidth = gc.textExtent(label).x;
            int labelY = top + plotHeight + 8 + (byTradingSymbol ? (bucketIndex % 2) * 20 : 0);
            gc.drawText(label, centerX - textWidth / 2, labelY, true);
            bucketIndex++;
        }
    }

    private String fitAxisLabel(GC gc, String label, int maxWidth)
    {
        if (gc.textExtent(label).x <= maxWidth)
            return label;

        String suffix = "..."; //$NON-NLS-1$
        int length = label.length();
        while (length > 1 && gc.textExtent(label.substring(0, length) + suffix).x > maxWidth)
            length--;
        return label.substring(0, length) + suffix;
    }

    private String groupLabel(Row row)
    {
        return switch (groupBy.getSelectionIndex())
        {
            case 1 -> row.instrumentType();
            case 2 -> row.underlying();
            default -> row.putCall();
        };
    }

    private String maturitySortKey(Row row)
    {
        if (row.maturityDate() != null)
            return row.maturityDate().toString();
        if (OPEN_END.equals(row.maturity()))
            return "9998"; //$NON-NLS-1$
        return "9999"; //$NON-NLS-1$
    }

    private String instrumentType(Security security)
    {
        String value = property(security, "instrumentType"); //$NON-NLS-1$
        if ("OPTION".equalsIgnoreCase(value)) //$NON-NLS-1$
            return "Option"; //$NON-NLS-1$
        if ("FUTURE".equalsIgnoreCase(value)) //$NON-NLS-1$
            return "Future"; //$NON-NLS-1$
        if ("KNOCK_OUT_CERTIFICATE".equalsIgnoreCase(value)) //$NON-NLS-1$
            return "K.O."; //$NON-NLS-1$
        return "Security"; //$NON-NLS-1$
    }

    private String putCall(Security security)
    {
        String value = property(security, "putCall"); //$NON-NLS-1$
        if ("CALL".equalsIgnoreCase(value)) //$NON-NLS-1$
            return "Call"; //$NON-NLS-1$
        if ("PUT".equalsIgnoreCase(value)) //$NON-NLS-1$
            return "Put"; //$NON-NLS-1$
        return instrumentType(security);
    }

    private String underlying(Security security)
    {
        String value = property(security, "underlying"); //$NON-NLS-1$
        return value == null || value.isBlank() ? security.getName() : value;
    }

    private String tradingSymbol(Security security)
    {
        String value = property(security, "contractSymbol"); //$NON-NLS-1$
        if (value != null && !value.isBlank())
            return value;
        value = security.getTickerSymbol();
        return value == null || value.isBlank() ? security.getName() : value;
    }

    private boolean isKnockout(Security security)
    {
        return "KNOCK_OUT_CERTIFICATE".equalsIgnoreCase(property(security, "instrumentType")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private LocalDate maturityDate(Security security)
    {
        String value = property(security, "expirationDate"); //$NON-NLS-1$
        if (value == null || value.isBlank())
            return null;

        try
        {
            return LocalDate.parse(value);
        }
        catch (RuntimeException ignore)
        {
            return null;
        }
    }

    private String property(Security security, String name)
    {
        return security.getPropertyValue(SecurityProperty.Type.FEED, "derivatives-addon." + name).orElse(null); //$NON-NLS-1$
    }

    private void editMultiplier()
    {
        IStructuredSelection selection = table.getStructuredSelection();
        Row row = (Row) selection.getFirstElement();
        if (row == null)
            return;

        InputDialog dialog = new InputDialog(getShell(), "Multiplier", //$NON-NLS-1$
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
            client.touch();
            if (activePart != null)
                activePart.setDirty(true);
        }

        refresh();
    }
}
