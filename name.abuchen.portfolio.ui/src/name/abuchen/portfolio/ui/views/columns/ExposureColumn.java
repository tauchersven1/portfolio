package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityDelta;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.DerivativePositionCalculator;
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

        setDescription("Delta-adjusted economic exposure. Options use the strike; futures use the linked underlying. "
                        + "Formula: quantity x reference price x multiplier x delta, converted into the reporting currency.");
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
            return getSecurityExposure(element);

        String currencyCode = converterProvider.get().getTermCurrency();
        if (element.isCategory() || element.isGroupByTaxonomy())
            return element.getChildren().map(this::getExposure).filter(Objects::nonNull)
                            .collect(MoneyCollectors.sum(currencyCode));

        // Cash accounts do not create market exposure.
        return null;
    }

    private Money getSecurityExposure(Element element)
    {
        Security security = element.getSecurity();
        String derivativeType = DerivativePositionCalculator.getDerivativeType(security);

        // For ordinary securities, economic exposure equals current market value.
        if (derivativeType == null)
            return element.getValuation();

        LocalDate date = dateProvider.get();
        long shares = element.getSecurityPosition().getShares();
        long referenceValue;
        String exposureCurrency;

        if (DerivativePositionCalculator.OPTION.equals(derivativeType))
        {
            Long strikeValue = DerivativePositionCalculator.getOptionStrikeQuoteValue(security);
            if (strikeValue == null)
                return null;

            referenceValue = strikeValue.longValue();
            exposureCurrency = security.getCurrencyCode();
        }
        else if (DerivativePositionCalculator.FUTURE.equals(derivativeType))
        {
            Security underlying = DerivativePositionCalculator.resolveUnderlying(client, security);
            if (underlying == null)
                return null;

            referenceValue = underlying.getSecurityPrice(date).getValue();
            exposureCurrency = underlying.getCurrencyCode();
        }
        else
        {
            return element.getValuation();
        }

        double multiplier = security.getMultiplier(date);
        double delta = SecurityDelta.getDelta(security, date);

        Money rawExposure = DerivativePositionCalculator.valueOf(shares, referenceValue, multiplier * delta,
                        exposureCurrency);
        return converterProvider.get().convert(date, rawExposure);
    }
}
