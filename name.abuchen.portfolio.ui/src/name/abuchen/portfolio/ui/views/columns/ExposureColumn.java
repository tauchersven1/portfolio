package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ExposureCalculator;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;
import name.abuchen.portfolio.ui.util.AttributeComparator;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.Element;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.ElementComparator;

@SuppressWarnings("nls")
public class ExposureColumn extends Column
{
    private final Client client;
    private final Supplier<LocalDate> dateProvider;
    private final Supplier<CurrencyConverter> converterProvider;
    private final Supplier<Font> boldFontProvider;

    public ExposureColumn(Client client, Supplier<LocalDate> dateProvider, Supplier<CurrencyConverter> converterProvider,
                    Supplier<Font> boldFontProvider)
    {
        super("derivativeExposure", "Exposure", SWT.RIGHT, 90);
        this.client = client;
        this.dateProvider = dateProvider;
        this.converterProvider = converterProvider;
        this.boldFontProvider = boldFontProvider;

        setDescription("Delta-adjusted economic exposure. Standard options use the strike; futures and K.O. "
                        + "certificates use the linked underlying. Formula: quantity x reference price x multiplier "
                        + "x delta, converted into the reporting currency.");
        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Money exposure = getExposure((Element) e);
                return exposure == null ? null : Values.Money.format(exposure, client.getBaseCurrency());
            }

            @Override
            public Font getFont(Object e)
            {
                Element element = (Element) e;
                if (!element.isGroupByTaxonomy() && !element.isCategory())
                    return null;

                Font font = boldFontProvider == null ? null : boldFontProvider.get();
                return font != null ? font
                                : JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
            }
        });
        setComparator(new ElementComparator(new AttributeComparator(e -> getExposure((Element) e))));
        setVisible(false);
    }

    private Money getExposure(Element element)
    {
        if (element.isSecurity())
            return ExposureCalculator.calculate(client, element.getSecurityPosition(), dateProvider.get(),
                            converterProvider.get(), ExposureType.DELTA_ADJUSTED);

        if (element.isAccount())
            return element.getValuation();

        String currencyCode = converterProvider.get().getTermCurrency();
        if (element.isCategory() || element.isGroupByTaxonomy())
            return element.getChildren().map(this::getExposure).filter(Objects::nonNull)
                            .collect(MoneyCollectors.sum(currencyCode));

        return null;
    }
}
