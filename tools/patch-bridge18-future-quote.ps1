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

Set-Content -Path $path -Value $text -Encoding UTF8
Write-Host 'Patched StatementOfAssetsViewer future quote display to remove contract multiplier.'
