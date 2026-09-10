package de.venari.portfolio.derivatives;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.DerivativeExposure;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AddonView;
import name.abuchen.portfolio.ui.AddonViewContext;

public class LongShortExposureReportView implements AddonView
{
    private enum Bucket
    {
        GROSS, LONG, SHORT
    }

    private record Entry(String label, long amount)
    {
    }

    private AddonViewContext context;
    private CurrencyConverter converter;
    private List<Entry> entries = List.of();

    private Canvas grossChart;
    private Canvas longChart;
    private Canvas shortChart;

    @Override
    public Control createBody(Composite parent, AddonViewContext context)
    {
        this.context = context;
        this.converter = new CurrencyConverterImpl(context.getExchangeRateProviderFactory(),
                        context.getClient().getBaseCurrency());

        Composite body = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(true).margins(8, 8).spacing(8, 8).applyTo(body);

        grossChart = createChart(body, "Gross Exposure", Bucket.GROSS); //$NON-NLS-1$
        longChart = createChart(body, "Long Exposure", Bucket.LONG); //$NON-NLS-1$
        shortChart = createChart(body, "Short Exposure", Bucket.SHORT); //$NON-NLS-1$

        refresh();
        return body;
    }

    @Override
    public void notifyModelUpdated()
    {
        refresh();
    }

    private Canvas createChart(Composite parent, String title, Bucket bucket)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(4, 4).spacing(4, 6).applyTo(container);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

        Label heading = new Label(container, SWT.CENTER);
        heading.setText(title);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(heading);

        Canvas chart = new Canvas(container, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        chart.addPaintListener(e -> paintDonut(e, bucket, title));
        GridDataFactory.fillDefaults().grab(true, true).hint(320, 420).applyTo(chart);
        return chart;
    }

    private void refresh()
    {
        if (grossChart == null || grossChart.isDisposed())
            return;

        converter = new CurrencyConverterImpl(context.getExchangeRateProviderFactory(),
                        context.getClient().getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(context.getClient(), converter, LocalDate.now());

        List<Entry> answer = new ArrayList<>();
        snapshot.getAssetPositions().forEach(asset -> addEntry(answer, asset));
        entries = answer;

        grossChart.redraw();
        longChart.redraw();
        shortChart.redraw();
    }

    private void addEntry(List<Entry> answer, AssetPosition asset)
    {
        Security security = asset.getSecurity();
        Money marketValue = asset.getValuation();
        Money exposure = marketValue;

        if (security != null)
        {
            DerivativeExposure.Result result = DerivativeExposure.calculate(context.getClient(), asset,
                            LocalDate.now());
            exposure = result.net();
        }

        if (!exposure.isZero())
            answer.add(new Entry(asset.getDescription(), exposure.getAmount()));
    }

    private void paintDonut(PaintEvent event, Bucket bucket, String title)
    {
        GC gc = event.gc;
        Rectangle area = ((Canvas) event.widget).getClientArea();

        List<Entry> values = entries.stream().filter(e -> include(e, bucket))
                        .map(e -> new Entry(e.label(), displayAmount(e.amount(), bucket)))
                        .filter(e -> e.amount() > 0)
                        .sorted(Comparator.comparingLong(Entry::amount).reversed()).toList();

        long total = values.stream().mapToLong(Entry::amount).sum();
        if (total == 0)
        {
            gc.drawText("No exposure", 16, 16, true); //$NON-NLS-1$
            return;
        }

        int diameter = Math.max(80, Math.min(area.width - 36, area.height - 130));
        int x = (area.width - diameter) / 2;
        int y = 20;
        int hole = Math.max(36, diameter / 2);
        int holeX = x + (diameter - hole) / 2;
        int holeY = y + (diameter - hole) / 2;

        int[] systemColors = { SWT.COLOR_BLUE, SWT.COLOR_DARK_YELLOW, SWT.COLOR_DARK_GREEN, SWT.COLOR_MAGENTA,
                        SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_RED, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_MAGENTA };

        int start = 0;
        for (int index = 0; index < values.size(); index++)
        {
            Entry entry = values.get(index);
            int arc = index == values.size() - 1 ? 360 - start
                            : (int) Math.round(360d * entry.amount() / total);
            gc.setBackground(context.getShell().getDisplay()
                            .getSystemColor(systemColors[index % systemColors.length]));
            gc.fillArc(x, y, diameter, diameter, start, arc);
            start += arc;
        }

        gc.setBackground(((Canvas) event.widget).getBackground());
        gc.fillOval(holeX, holeY, hole, hole);

        String totalText = Values.Money.format(Money.of(converter.getTermCurrency(), total));
        int totalWidth = gc.textExtent(totalText).x;
        gc.drawText(totalText, area.width / 2 - totalWidth / 2, y + diameter / 2 - 8, true);

        int legendY = y + diameter + 16;
        int maxLegendRows = Math.max(1, (area.height - legendY - 8) / 20);
        int rowCount = Math.min(maxLegendRows, values.size());

        for (int index = 0; index < rowCount; index++)
        {
            Entry entry = values.get(index);
            gc.setBackground(context.getShell().getDisplay()
                            .getSystemColor(systemColors[index % systemColors.length]));
            gc.fillRectangle(12, legendY + index * 20 + 3, 12, 12);

            double share = (double) entry.amount() / total;
            String label = fitLabel(gc, entry.label(), Math.max(80, area.width - 112));
            String percent = Values.Percent2.format(share);
            gc.drawText(label, 30, legendY + index * 20, true);
            int width = gc.textExtent(percent).x;
            gc.drawText(percent, area.width - width - 12, legendY + index * 20, true);
        }

        if (values.size() > rowCount)
        {
            String more = "+" + (values.size() - rowCount) + " more"; //$NON-NLS-1$ //$NON-NLS-2$
            gc.drawText(more, 30, legendY + rowCount * 20, true);
        }
    }

    private boolean include(Entry entry, Bucket bucket)
    {
        return switch (bucket)
        {
            case GROSS -> true;
            case LONG -> entry.amount() > 0;
            case SHORT -> entry.amount() < 0;
        };
    }

    private long displayAmount(long amount, Bucket bucket)
    {
        return bucket == Bucket.LONG ? amount : Math.abs(amount);
    }

    private String fitLabel(GC gc, String label, int maxWidth)
    {
        if (label == null || label.isBlank())
            return "-"; //$NON-NLS-1$
        if (gc.textExtent(label).x <= maxWidth)
            return label;

        String suffix = "..."; //$NON-NLS-1$
        int length = label.length();
        while (length > 1 && gc.textExtent(label.substring(0, length) + suffix).x > maxWidth)
            length--;
        return label.substring(0, length) + suffix;
    }
}
