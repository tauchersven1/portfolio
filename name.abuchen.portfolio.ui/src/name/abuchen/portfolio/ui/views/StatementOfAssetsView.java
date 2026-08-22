package name.abuchen.portfolio.ui.views;

import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ExposureCalculator.ExposureType;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.TimeMachineDropDown;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.Element;
import name.abuchen.portfolio.ui.views.columns.ExposureColumn;
import name.abuchen.portfolio.ui.views.panes.ChartPane;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesDataQualityPane;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.Pair;

public class StatementOfAssetsView extends AbstractFinanceView
{
    private static final String DERIVATIVES_GROUP = "Derivate"; //$NON-NLS-1$

    private StatementOfAssetsViewer assetViewer;
    private PropertyChangeListener currencyChangeListener;
    private ClientFilterDropDown clientFilter;
    private TimeMachineDropDown timeMachineDropDown;

    private LocalDate currentSnapshotDate = LocalDate.now();
    private CurrencyConverter currentConverter;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return assetViewer == null ? Messages.LabelStatementOfAssets : Messages.LabelStatementOfAssets + //
                        " (" + assetViewer.getColumnHelper().getConfigurationName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void notifyModelUpdated()
    {
        StatementOfAssetsViewer.Element selection = (StatementOfAssetsViewer.Element) assetViewer.getTableViewer()
                        .getStructuredSelection().getFirstElement();

        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);

        var snapshotDate = timeMachineDropDown.getTimeMachineDate();
        currentSnapshotDate = snapshotDate.orElse(LocalDate.now());
        currentConverter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        assetViewer.setInput(clientFilter.getSelectedFilter(), currentSnapshotDate, currentConverter);
        updateTitle(getDefaultTitle());

        if (selection != null)
            assetViewer.selectSubject(selection.getSubject());
    }

    @Override
    protected void addButtons(final ToolBarManager toolBar)
    {
        DropDown dropDown = new DropDown(getClient().getBaseCurrency());

        Function<CurrencyUnit, Action> asAction = unit -> {
            Action action = new SimpleAction(unit.getLabel(), a -> {
                dropDown.setLabel(unit.getCurrencyCode());
                getClient().setBaseCurrency(unit.getCurrencyCode());
            });
            action.setChecked(getClient().getBaseCurrency().equals(unit.getCurrencyCode()));
            return action;
        };

        dropDown.setMenuListener(manager -> {
            getClient().getUsedCurrencies().forEach(unit -> manager.add(asAction.apply(unit)));
            manager.add(new Separator());

            List<Pair<String, List<CurrencyUnit>>> available = CurrencyUnit.getAvailableCurrencyUnitsGrouped();
            for (Pair<String, List<CurrencyUnit>> pair : available)
            {
                MenuManager submenu = new MenuManager(pair.getLeft());
                manager.add(submenu);
                pair.getRight().forEach(unit -> submenu.add(asAction.apply(unit)));
            }
        });

        toolBar.add(dropDown);
        currencyChangeListener = e -> dropDown.setLabel(e.getNewValue().toString());
        getClient().addPropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$

        timeMachineDropDown = new TimeMachineDropDown(date -> notifyModelUpdated());
        toolBar.add(timeMachineDropDown);

        this.clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        StatementOfAssetsView.class.getSimpleName(), filter -> notifyModelUpdated());
        toolBar.add(clientFilter);

        Action export = new SimpleAction(null, action -> new TableViewerCSVExporter(assetViewer.getTableViewer())
                        .export(Messages.LabelStatementOfAssets + ".csv")); //$NON-NLS-1$
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);
        toolBar.add(export);

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> assetViewer.menuAboutToShow(manager)));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assetViewer = make(StatementOfAssetsViewer.class);
        Control control = assetViewer.createControl(parent, true);

        ExposureColumn nominalExposureColumn = new ExposureColumn(getClient(), () -> currentSnapshotDate,
                        () -> currentConverter, () -> null);
        nominalExposureColumn.setGroupLabel(DERIVATIVES_GROUP);
        nominalExposureColumn.setVisible(true);
        assetViewer.getColumnHelper().addColumn(nominalExposureColumn);

        ExposureColumn deltaAdjustedExposureColumn = new ExposureColumn(getClient(), () -> currentSnapshotDate,
                        () -> currentConverter, () -> null, ExposureType.DELTA_ADJUSTED,
                        "derivativeExposureDeltaAdjusted", "Exposure deltadj."); //$NON-NLS-1$ //$NON-NLS-2$
        deltaAdjustedExposureColumn.setGroupLabel(DERIVATIVES_GROUP);
        deltaAdjustedExposureColumn.setVisible(true);
        assetViewer.getColumnHelper().addColumn(deltaAdjustedExposureColumn);

        addDerivativeContractColumns();
        assetViewer.getColumnHelper().addColumn(createPutCallColumn());
        restoreColumnConfiguration();

        assetViewer.setToolBarManager(getViewToolBarManager());

        updateTitle(getDefaultTitle());
        assetViewer.getColumnHelper().addListener(() -> {
            updateTitle(getDefaultTitle());

            if (Platform.OS_LINUX.equals(Platform.getOS()))
                notifyModelUpdated();
        });

        hookContextMenu(assetViewer.getTableViewer().getControl(),
                        manager -> assetViewer.hookMenuListener(manager, StatementOfAssetsView.this));
        assetViewer.hookKeyListener();

        assetViewer.getTableViewer().addSelectionChangedListener(e -> {
            var selection = e.getStructuredSelection();

            if (selection.size() == 1)
                setInformationPaneInput(selection.getFirstElement());
            else
                setInformationPaneInput(SecuritySelection.from(getClient(), selection));
        });

        notifyModelUpdated();

        return control;
    }

    private void restoreColumnConfiguration()
    {
        String identifier = StatementOfAssetsViewer.class.getName();
        var configSet = getClient().getSettings().getConfigurationSet(identifier);
        String activeUUID = getPreferenceStore().getString(identifier + "$picked"); //$NON-NLS-1$
        String data = configSet.lookup(activeUUID).map(config -> config.getData())
                        .orElseGet(() -> configSet.getConfigurations().findFirst().map(config -> config.getData())
                                        .orElse("")); //$NON-NLS-1$
        assetViewer.getColumnHelper().onConfigurationPicked(data);
    }

    private void addDerivativeContractColumns()
    {
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeUnderlying", "Underlying",
                        "underlying", 140)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeContractSymbol",
                        "Contract / Trading Symbol", "contractSymbol", 150)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeExchange", "Exchange",
                        "exchange", 90)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeContractMonth",
                        "Contract Month", "contractMonth", 100)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeFirstTradingDay",
                        "First Trading Day", "firstTradingDay", 110)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeLastTradingDay",
                        "Last Trading Day", "lastTradingDay", 110)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeExpirationDate",
                        "Expiration Date", "expirationDate", 110)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeSettlementDate",
                        "Settlement Date", "settlementDate", 110)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeFirstNoticeDay",
                        "First Notice Day", "firstNoticeDay", 110)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeSettlementType",
                        "Settlement", "settlementType", 90)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeContractSize",
                        "Contract Size", "contractSize", 100)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn("derivativeTickSize", "Tick Size",
                        "tickSize", 90)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private Column createDerivativePropertyColumn(String id, String label, String propertyName, int width)
    {
        Column column = new Column(id, label, SWT.LEFT, width);
        column.setGroupLabel(DERIVATIVES_GROUP);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isSecurity())
                    return null;

                Security security = element.getSecurity();
                if (property(security, "type") == null) //$NON-NLS-1$
                    return null;

                String value = property(security, propertyName);
                if ("settlementType".equals(propertyName)) //$NON-NLS-1$
                {
                    if ("CASH".equals(value)) //$NON-NLS-1$
                        return "Cash"; //$NON-NLS-1$
                    if ("PHYSICAL".equals(value)) //$NON-NLS-1$
                        return "Physical"; //$NON-NLS-1$
                }
                return value;
            }
        });
        column.setVisible(false);
        return column;
    }

    private Column createPutCallColumn()
    {
        Column column = new Column("derivativePutCall", "Put / Call", SWT.LEFT, 80); //$NON-NLS-1$ //$NON-NLS-2$
        column.setGroupLabel(DERIVATIVES_GROUP);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isSecurity())
                    return null;

                String value = property(element.getSecurity(), "putCall"); //$NON-NLS-1$
                if ("CALL".equals(value)) //$NON-NLS-1$
                    return "Call"; //$NON-NLS-1$
                if ("PUT".equals(value)) //$NON-NLS-1$
                    return "Put"; //$NON-NLS-1$
                return null;
            }
        });
        column.setVisible(false);
        return column;
    }

    private static String property(Security security, String name)
    {
        return security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, name).orElse(null);
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(ChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
        pages.add(make(HistoricalPricesDataQualityPane.class));
    }

    @Override
    public void dispose()
    {
        if (currencyChangeListener != null)
            getClient().removePropertyChangeListener("baseCurrency", currencyChangeListener); //$NON-NLS-1$
    }
}
