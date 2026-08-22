package name.abuchen.portfolio.ui.views.holdings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.ExposureCalculator;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TimeMachineDropDown;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsOutsidePie;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;

@SuppressWarnings("nls")
public class HoldingsExposurePieChartView extends AbstractFinanceView
{
    private enum ExposureBucket
    {
        GROSS, LONG, SHORT
    }

    @Inject
    private ExchangeRateProviderFactory factory;

    private CurrencyConverter converter;
    private ClientFilterDropDown clientFilter;
    private TimeMachineDropDown timeMachineDropDown;
    private DropDown exposureBasis;

    private ExposureType currentExposureType = ExposureType.DELTA_ADJUSTED;
    private LocalDate valuationDate = LocalDate.now();
    private ClientSnapshot snapshot;

    private ExposureDonut grossChart;
    private ExposureDonut longChart;
    private ExposureDonut shortChart;

    @PostConstruct
    protected void construct()
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        HoldingsExposurePieChartView.class.getSimpleName(), filter -> notifyModelUpdated());
        timeMachineDropDown = new TimeMachineDropDown(date -> notifyModelUpdated());
        exposureBasis = new DropDown("Delta adjusted");
        exposureBasis.setMenuListener(manager -> {
            manager.add(createExposureTypeAction("Delta adjusted", ExposureType.DELTA_ADJUSTED));
            manager.add(createExposureTypeAction("Notional", ExposureType.NOTIONAL));
        });

        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);
        snapshot = ClientSnapshot.create(filteredClient, converter, valuationDate,
                        clientFilter.getClientFilterMenu().getSelectedItem().getLabel());
    }

    private Action createExposureTypeAction(String label, ExposureType type)
    {
        Action action = new SimpleAction(label, a -> {
            currentExposureType = type;
            exposureBasis.setLabel(label);
            notifyModelUpdated();
        });
        action.setChecked(currentExposureType == type);
        return action;
    }

    @Override
    protected String getDefaultTitle()
    {
        String title = "Bestand Exposure";
        if (clientFilter != null && clientFilter.hasActiveFilter())
            title += " : " + clientFilter.getClientFilterMenu().getSelectedItem().getLabel();
        return title;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(exposureBasis);
        toolBar.add(timeMachineDropDown);
        toolBar.add(clientFilter);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite body = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(true).margins(8, 8).spacing(8, 8).applyTo(body);

        grossChart = new ExposureDonut(body, "Gross Exposure", ExposureBucket.GROSS);
        longChart = new ExposureDonut(body, "Long Exposure", ExposureBucket.LONG);
        shortChart = new ExposureDonut(body, "Short Exposure", ExposureBucket.SHORT);

        refreshCharts();
        updateTitle(getDefaultTitle());
        return body;
    }

    @Override
    public void notifyModelUpdated()
    {
        if (clientFilter == null)
            return;

        valuationDate = timeMachineDropDown.getTimeMachineDate().orElse(LocalDate.now());
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);

        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        snapshot = ClientSnapshot.create(filteredClient, converter, valuationDate,
                        clientFilter.getClientFilterMenu().getSelectedItem().getLabel());

        refreshCharts();

        Optional<LocalDate> snapshotDate = timeMachineDropDown.getTimeMachineDate();
        String dateLabel = snapshotDate.map(date -> " | " + Values.Date.format(date)).orElse("");
        updateTitle(getDefaultTitle() + dateLabel);
        setInformationPaneInput(null);
    }

    private void refreshCharts()
    {
        if (snapshot == null || grossChart == null)
            return;

        List<ExposureEntry> entries = new ArrayList<>();
        snapshot.getAssetPositions().forEach(asset -> {
            Money exposure = calculateExposure(asset);
            if (exposure != null && !exposure.isZero())
                entries.add(new ExposureEntry(asset, exposure.getAmount()));
        });

        grossChart.refresh(entries);
        longChart.refresh(entries);
        shortChart.refresh(entries);
    }

    private Money calculateExposure(AssetPosition asset)
    {
        if (asset.getSecurity() == null)
            return asset.getValuation();

        SecurityPosition position = asset.getPosition();
        return ExposureCalculator.calculate(getClient(), position, valuationDate, converter, currentExposureType);
    }

    private record ExposureEntry(AssetPosition asset, long amount)
    {
    }

    private class ExposureDonut
    {
        private final String title;
        private final ExposureBucket bucket;
        private final CircularChart chart;
        private final Map<String, NodeData> id2nodeData = new HashMap<>();
        private List<String> lastLabels;
        private long total;

        private class NodeData
        {
            private final AssetPosition asset;
            private final long amount;
            private final String percentage;

            private NodeData(AssetPosition asset, long amount, String percentage)
            {
                this.asset = asset;
                this.amount = amount;
                this.percentage = percentage;
            }
        }

        private ExposureDonut(Composite parent, String title, ExposureBucket bucket)
        {
            this.title = title;
            this.bucket = bucket;

            Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(1).margins(4, 4).applyTo(container);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

            Label heading = new Label(container, SWT.CENTER);
            heading.setText(title);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(heading);

            chart = new CircularChart(container, SeriesType.DOUGHNUT, this::getNodeLabel);
            GridDataFactory.fillDefaults().grab(true, true).hint(340, 420).applyTo(chart);
            chart.addLabelPainter(new RenderLabelsCenteredInPie(chart, this::getNodeLabel));
            chart.addLabelPainter(new RenderLabelsOutsidePie(chart, this::getAssetLabel));

            chart.getToolTip().setToolTipBuilder((tooltip, currentNode) -> {
                Composite data = new Composite(tooltip, SWT.NONE);
                GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

                NodeData nodeData = id2nodeData.get(currentNode.getId());
                if (nodeData == null)
                {
                    Label chartLabel = new Label(data, SWT.NONE);
                    GridDataFactory.fillDefaults().span(2, 1).applyTo(chartLabel);
                    chartLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                    chartLabel.setText(title);

                    Label value = new Label(data, SWT.NONE);
                    GridDataFactory.fillDefaults().span(2, 1).applyTo(value);
                    value.setText(Values.Money.format(Money.of(converter.getTermCurrency(), total)));
                }
                else
                {
                    Label assetLabel = new Label(data, SWT.NONE);
                    assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                    assetLabel.setText(nodeData.asset.getDescription());

                    Label percentage = new Label(data, SWT.NONE);
                    percentage.setText(nodeData.percentage);

                    Label value = new Label(data, SWT.NONE);
                    GridDataFactory.fillDefaults().span(2, 1).applyTo(value);
                    value.setText(Values.Money.format(Money.of(converter.getTermCurrency(), nodeData.amount)));
                }
            });

            ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp,
                            event -> chart.getNodeAt(event.x, event.y).ifPresent(node -> {
                                NodeData data = id2nodeData.get(node.getId());
                                if (data != null)
                                    setInformationPaneInput(data.asset.getInvestmentVehicle());
                            }));
        }

        private void refresh(List<ExposureEntry> allEntries)
        {
            List<ExposureEntry> entries = allEntries.stream().filter(this::include)
                            .map(e -> new ExposureEntry(e.asset(), displayAmount(e.amount())))
                            .filter(e -> e.amount() > 0)
                            .sorted((left, right) -> Long.compare(right.amount(), left.amount())).toList();

            total = entries.stream().mapToLong(ExposureEntry::amount).sum();
            id2nodeData.clear();

            List<Double> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (ExposureEntry entry : entries)
            {
                String nodeId = entry.asset().getInvestmentVehicle().getUUID();
                labels.add(nodeId);
                values.add(entry.amount() / Values.Amount.divider());

                double share = total == 0 ? 0d : (double) entry.amount() / total;
                id2nodeData.put(nodeId,
                                new NodeData(entry.asset(), entry.amount(), Values.Percent2.format(share)));
            }

            ICircularSeries<?> series = (ICircularSeries<?>) chart.getSeriesSet().getSeries(title);
            if (series == null)
            {
                series = createSeries(values, labels);
            }
            else if (hasSameDataSet(labels))
            {
                ListIterator<String> iterator = labels.listIterator();
                while (iterator.hasNext())
                {
                    int index = iterator.nextIndex();
                    Node node = series.getNodeById(iterator.next());
                    if (node != null)
                        node.setValue(values.get(index));
                }
            }
            else
            {
                series = createSeries(values, labels);
            }

            setColors(series, values.size());
            chart.updateAngleBounds();
            chart.redraw();
        }

        private boolean include(ExposureEntry entry)
        {
            return switch (bucket)
            {
                case GROSS -> true;
                case LONG -> entry.amount() > 0;
                case SHORT -> entry.amount() < 0;
            };
        }

        private long displayAmount(long amount)
        {
            return bucket == ExposureBucket.LONG ? amount : Math.abs(amount);
        }

        private boolean hasSameDataSet(List<String> labels)
        {
            List<String> sorted = new ArrayList<>(labels);
            Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
            return sorted.equals(lastLabels);
        }

        private ICircularSeries<?> createSeries(List<Double> values, List<String> labels)
        {
            ICircularSeries<?> series = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                            title);
            series.setSeries(labels.toArray(new String[0]), values.stream().mapToDouble(Double::doubleValue).toArray());
            series.setSliceColor(chart.getPlotArea().getBackground());
            lastLabels = new ArrayList<>(labels);
            Collections.sort(lastLabels, String.CASE_INSENSITIVE_ORDER);
            return series;
        }

        private void setColors(ICircularSeries<?> series, int colorCount)
        {
            CircularChart.PieColors wheel = new CircularChart.PieColors();
            Color[] colors = new Color[colorCount];
            for (int index = 0; index < colors.length; index++)
                colors[index] = wheel.next();
            series.setColor(colors);
        }

        private String getNodeLabel(Node node)
        {
            NodeData data = id2nodeData.get(node.getId());
            return data == null ? null : data.percentage;
        }

        private String getAssetLabel(Node node)
        {
            NodeData data = id2nodeData.get(node.getId());
            return data == null ? "" : data.asset.getDescription();
        }
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
    }
}
