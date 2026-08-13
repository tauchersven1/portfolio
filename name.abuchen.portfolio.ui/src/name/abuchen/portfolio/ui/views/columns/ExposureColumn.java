package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityDelta;
import name.abuchen.portfolio.model.SecurityProperty;
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
    private static final String UNDERLYING_SECURITY_UUID = "underlyingSecurityUUID";

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

        setDescription("Delta-adjusted economic exposure. Options use the linked underlying; futures use the futures quote. "
                        + "Formula: quantity x price x multiplier x delta, converted into the reporting currency.");
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
                return element.isGroupByTaxonomy() || element.isCategory() ? boldFontProvider.get() : null;
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
        long quoteValue;
        String exposureCurrency;

        if (DerivativePositionCalculator.OPTION.equals(derivativeType))
        {
            String uuid = security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, UNDERLYING_SECURITY_UUID)
                            .orElse(null);
            if (uuid == null)
                return null;

            Security underlying = client.getSecurities().stream().filter(s -> uuid.equals(s.getUUID())).findFirst()
                            .orElse(null);
            if (underlying == null)
                return null;

            quoteValue = underlying.getSecurityPrice(date).getValue();
            exposureCurrency = underlying.getCurrencyCode();
        }
        else if (DerivativePositionCalculator.FUTURE.equals(derivativeType))
        {
            quoteValue = element.getSecurityPosition().getPrice().getValue();
            exposureCurrency = security.getCurrencyCode();
        }
        else
        {
            return element.getValuation();
        }

        double multiplier = security.getMultiplier(date);
        double delta = SecurityDelta.getDelta(security, date);

        Money rawExposure = DerivativePositionCalculator.valueOf(shares, quoteValue, multiplier * delta,
                        exposureCurrency);
        return converterProvider.get().convert(date, rawExposure);
    }
}
