$path = 'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/StatementOfAssetsViewer.java'
$text = Get-Content $path -Raw

$oldLabel = @'
                Security security = element.getSecurity();
                return Values.Quote.format(security.getCurrencyCode(),
                                element.getSecurityPosition().getPrice().getValue(), client.getBaseCurrency());
'@
$newLabel = @'
                Security security = element.getSecurity();
                return Values.Quote.format(security.getCurrencyCode(), displayedQuoteValue(element),
                                client.getBaseCurrency());
'@
if (-not $text.Contains($oldLabel)) { throw 'Quote label block not found' }
$text = $text.Replace($oldLabel, $newLabel)

$oldComparator = @'
            return Money.of(element.getSecurity().getCurrencyCode(),
                            element.getSecurityPosition().getPrice().getValue());
'@
$newComparator = @'
            return Money.of(element.getSecurity().getCurrencyCode(), displayedQuoteValue(element));
'@
if (-not $text.Contains($oldComparator)) { throw 'Quote comparator block not found' }
$text = $text.Replace($oldComparator, $newComparator)

$marker = @'
    private Money marketValue(Element element)
'@
$helper = @'
    private long displayedQuoteValue(Element element)
    {
        long value = element.getSecurityPosition().getPrice().getValue();
        if (!isFuture(element))
            return value;

        BigDecimal multiplier = SecurityMultiplier.valueAt(element.getSecurity(), model.getDate());
        if (multiplier == null || multiplier.signum() == 0 || BigDecimal.ONE.compareTo(multiplier) == 0)
            return value;

        return BigDecimal.valueOf(value).divide(multiplier, Values.MC).longValue();
    }

    private Money marketValue(Element element)
'@
if (-not $text.Contains($marker)) { throw 'marketValue marker not found' }
$text = $text.Replace($marker, $helper)

$oldFutureValue = @'
                if (record != null)
                {
                    Money currentContractValue = element.getValuation().multiplyAndRound(
                                    SecurityMultiplier.valueAt(element.getSecurity(), model.getDate()).doubleValue());
                    return currentContractValue.subtract(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED));
                }
'@
$newFutureValue = @'
                if (record != null)
                {
                    // For futures the internally stored valuation already contains the
                    // contract multiplier. The Statement of Assets market value must be
                    // the unrealized P&L as of the statement date, not the notional.
                    Money currentContractValue = element.getValuation();
                    return currentContractValue.subtract(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED));
                }
'@
if (-not $text.Contains($oldFutureValue)) { throw 'Future market value block not found' }
$text = $text.Replace($oldFutureValue, $newFutureValue)

Set-Content -Path $path -Value $text -Encoding UTF8
Write-Host 'Patched StatementOfAssetsViewer future quote display and futures unrealized PnL market value.'

$transactionsPath = 'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/TransactionsViewer.java'
$transactions = Get-Content $transactionsPath -Raw
$oldMultiplierColumn = @'
        column = new Column("multiplier", "Multiplier", SWT.RIGHT, 80); //$NON-NLS-1$ //$NON-NLS-2$
'@
$newMultiplierColumn = @'
        // Use a new stable column id so existing saved table layouts cannot hide
        // the restored multiplier column from earlier accepted builds.
        column = new Column("multiplier_v2", "Multiplier", SWT.RIGHT, 80); //$NON-NLS-1$ //$NON-NLS-2$
        column.setVisible(true);
'@
if (-not $transactions.Contains($oldMultiplierColumn)) { throw 'Multiplier column block not found' }
$transactions = $transactions.Replace($oldMultiplierColumn, $newMultiplierColumn)
Set-Content -Path $transactionsPath -Value $transactions -Encoding UTF8
Write-Host 'Restored visible date-dependent Multiplier column in transaction overview.'
