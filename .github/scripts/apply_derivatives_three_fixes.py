from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Expected text not found in {path}: {old[:120]!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')

# 1) Restore persisted derivative columns only after all custom columns are registered.
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/StatementOfAssetsView.java',
    '''        addDerivativeContractColumns();
        assetViewer.getColumnHelper().addColumn(createPutCallColumn());

        assetViewer.setToolBarManager(getViewToolBarManager());''',
    '''        addDerivativeContractColumns();
        assetViewer.getColumnHelper().addColumn(createPutCallColumn());

        // createControl() restores the column configuration before the custom derivative
        // columns are registered. Re-apply it now so persisted derivative columns are
        // resolved by their stable IDs as well.
        assetViewer.getColumnHelper().createColumns();

        assetViewer.setToolBarManager(getViewToolBarManager());''')

# 2) Cash positions contribute their market value to the Statement of Assets exposure column.
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/columns/ExposureColumn.java',
    '''        if (element.isSecurity())
            return ExposureCalculator.calculate(client, element.getSecurityPosition(), dateProvider.get(),
                            converterProvider.get(), ExposureType.DELTA_ADJUSTED);

        String currencyCode = converterProvider.get().getTermCurrency();''',
    '''        if (element.isSecurity())
            return ExposureCalculator.calculate(client, element.getSecurityPosition(), dateProvider.get(),
                            converterProvider.get(), ExposureType.DELTA_ADJUSTED);

        if (element.isAccount())
            return element.getValuation();

        String currencyCode = converterProvider.get().getTermCurrency();''')
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/columns/ExposureColumn.java',
    '''        // Cash accounts do not create market exposure.
        return null;''',
    '''        return null;''')

# 3) Include cash accounts in Exposure Management with exposure equal to market value.
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java',
    '''        instrumentType = combo(filters, "Instrument type", "All", "Derivatives", "Option", "Future",
                        "K.O. certificate", "Non-derivatives");''',
    '''        instrumentType = combo(filters, "Instrument type", "All", "Derivatives", "Option", "Future",
                        "K.O. certificate", "Non-derivatives", "Cash");''')
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java',
    '''        snapshot.getAssetPositions().filter(p -> p.getSecurity() != null).forEach(asset -> {
            Security security = asset.getSecurity();
            SecurityPosition position = asset.getPosition();
            Money exposure = ExposureCalculator.calculate(getClient(), position, valuationDate, converter, type);
            if (exposure == null || exposure.isZero())
                return;

            answer.add(new ExposureRow(security, position, exposure, maturity(security), maturityDate(security),
                            instrumentLabel(security), putCallLabel(security), underlyingLabel(security)));
        });''',
    '''        snapshot.getAssetPositions().forEach(asset -> {
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
        });''')
replace_once(
    'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java',
    '''        String selectedInstrument = instrumentType.getText();
        boolean derivative = DerivativePositionCalculator.getDerivativeType(row.security()) != null;
        if ("Derivatives".equals(selectedInstrument) && !derivative)
            return false;
        if ("Option".equals(selectedInstrument) && !DerivativePositionCalculator.isOption(row.security()))
            return false;
        if ("Future".equals(selectedInstrument) && !DerivativePositionCalculator.isFuture(row.security()))
            return false;
        if ("K.O. certificate".equals(selectedInstrument) && !ExposureCalculator.isKnockoutCertificate(row.security()))
            return false;
        if ("Non-derivatives".equals(selectedInstrument) && derivative)
            return false;''',
    '''        String selectedInstrument = instrumentType.getText();
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
            return false;''')

# 4) The transaction model stores the quoted price without multiplier, while gross
# values/cost per share include it. Convert transaction-derived chart prices back
# to quote scale so entry lines and buy/sell markers align with historical quotes.
chart = 'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/SecuritiesChart.java'
replace_once(
    chart,
    '''        label.setText(MessageFormat.format(Messages.LabelToolTipInvestmentDetails, Values.Share.format(t.getShares()),
                        Values.CalculatedQuote.format(
                                        t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())))));''',
    '''        label.setText(MessageFormat.format(Messages.LabelToolTipInvestmentDetails, Values.Share.format(t.getShares()),
                        Values.CalculatedQuote.format(getTransactionQuote(t))));''')
replace_once(
    chart,
    '''                    double value = t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode()))
                                    .getAmount() / Values.Quote.divider();''',
    '''                    double value = getTransactionQuote(t).getAmount() / Values.Quote.divider();''')
replace_once(
    chart,
    '''            double[] values = transactions.stream().mapToDouble(
                            t -> t.getGrossPricePerShare(converter.with(t.getSecurity().getCurrencyCode())).getAmount()
                                            / Values.Quote.divider())
                            .toArray();''',
    '''            double[] values = transactions.stream()
                            .mapToDouble(t -> getTransactionQuote(t).getAmount() / Values.Quote.divider()).toArray();''')
replace_once(
    chart,
    '''    private void addDividendTooltip(Composite composite, AccountTransaction t)
    {''',
    '''    private Quote getTransactionQuote(PortfolioTransaction transaction)
    {
        Security security = transaction.getSecurity();
        Quote grossPrice = transaction.getGrossPricePerShare(converter.with(security.getCurrencyCode()));
        double multiplier = security.getMultiplier(transaction.getDateTime().toLocalDate());

        if (multiplier == 0d || multiplier == 1d)
            return grossPrice;

        return Quote.of(grossPrice.getCurrencyCode(), Math.round(grossPrice.getAmount() / multiplier));
    }

    private void addDividendTooltip(Composite composite, AccountTransaction t)
    {''')
replace_once(
    chart,
    '''        return purchasePricePerShare.isZero() ? Optional.empty()
                        : Optional.of(purchasePricePerShare.getAmount() / Values.Quote.divider());''',
    '''        double multiplier = security.getMultiplier(date);
        return purchasePricePerShare.isZero() || multiplier == 0d ? Optional.empty()
                        : Optional.of(purchasePricePerShare.getAmount() / Values.Quote.divider() / multiplier);''')

print('Applied combined derivatives fixes successfully')
