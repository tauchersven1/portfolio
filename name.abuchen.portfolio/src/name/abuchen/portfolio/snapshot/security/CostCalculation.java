package name.abuchen.portfolio.snapshot.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class CostCalculation extends Calculation
{
    public record CostCalculationResult(long sharesHeld, Money fifoCost, TrailRecord fifoCostTrail, Money netFifoCost,
                    Money movingAverageCost, Money netMovingAverageCost, Money fifoQuoteCost, Money netFifoQuoteCost,
                    Money movingAverageQuoteCost, Money netMovingAverageQuoteCost, Money fees, Money taxes)
    {
    }

    private static class LineItem
    {
        private TransactionOwner<?> owner;
        private long shares;
        private long grossAmount;
        private long netAmount;
        private long grossQuoteAmount;
        private long netQuoteAmount;

        private final TrailRecord trail;

        /**
         * Holds the original number of shares (of the transaction). The
         * original shares are needed to calculate fractions if the transaction
         * is split up multiple times
         */
        private final long originalShares;

        public LineItem(TransactionOwner<?> owner, long shares, long grossAmount, long netAmount,
                        long grossQuoteAmount, long netQuoteAmount, TrailRecord trail)
        {
            this.owner = owner;
            this.shares = shares;
            this.grossAmount = grossAmount;
            this.netAmount = netAmount;
            this.grossQuoteAmount = grossQuoteAmount;
            this.netQuoteAmount = netQuoteAmount;
            this.trail = trail;
            this.originalShares = shares;
        }
    }

    private List<LineItem> fifo = new ArrayList<>();

    private long movingRelativeCost = 0;
    private long movingRelativeNetCost = 0;
    private long movingRelativeQuoteCost = 0;
    private long movingRelativeNetQuoteCost = 0;
    private long heldShares = 0;

    private long fees;
    private long taxes;

    private double getMultiplier(java.time.LocalDate date)
    {
        if (getSecurity() == null || date == null)
            return 1d;

        double multiplier = getSecurity().getMultiplier(date);
        return multiplier > 0d ? multiplier : 1d;
    }

    private long normalizeQuoteCost(long amount, java.time.LocalDate date)
    {
        return Math.round(amount / getMultiplier(date));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item)
    {
        Money valuation = item.getValue();
        SecurityPosition position = item.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        long amount = converter.convert(item.getDateTime(), valuation).getAmount();
        long quoteAmount = normalizeQuoteCost(amount, item.getDateTime().toLocalDate());

        TrailRecord trail = TrailRecord.ofPosition(item.getDateTime().toLocalDate(), (Portfolio) item.getOwner(),
                        position);

        if (!getTermCurrency().equals(valuation.getCurrencyCode()))
            trail = trail.convert(Money.of(getTermCurrency(), amount),
                            converter.getRate(item.getDateTime(), valuation.getCurrencyCode()));

        fifo.add(new LineItem(item.getOwner(), position.getShares(), amount, amount, quoteAmount, quoteAmount, trail));
        movingRelativeCost += amount;
        movingRelativeNetCost += amount;
        movingRelativeQuoteCost += quoteAmount;
        movingRelativeNetQuoteCost += quoteAmount;
        heldShares += position.getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        long fee = t.getUnitSum(Unit.Type.FEE, converter).getAmount();
        long tax = t.getUnitSum(Unit.Type.TAX, converter).getAmount();
        fees += fee;
        taxes += tax;

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                long grossAmount = t.getMonetaryAmount(converter).getAmount();
                long netAmount = t.getGrossValue(converter).getAmount();
                long grossQuoteAmount = normalizeQuoteCost(grossAmount, t.getDateTime().toLocalDate());
                long netQuoteAmount = normalizeQuoteCost(netAmount, t.getDateTime().toLocalDate());

                TrailRecord trail = TrailRecord.ofTransaction(t);
                if (!getTermCurrency().equals(t.getCurrencyCode()))
                    trail = trail.convert(Money.of(getTermCurrency(), grossAmount),
                                    converter.getRate(t.getDateTime(), t.getCurrencyCode()));

                fifo.add(new LineItem(item.getOwner(), t.getShares(), grossAmount, netAmount, grossQuoteAmount,
                                netQuoteAmount, trail));
                movingRelativeCost += grossAmount;
                movingRelativeNetCost += netAmount;
                movingRelativeQuoteCost += grossQuoteAmount;
                movingRelativeNetQuoteCost += netQuoteAmount;
                heldShares += t.getShares();
                break;

            case SELL:
            case DELIVERY_OUTBOUND:
                long sold = t.getShares();

                long remaining = heldShares - sold;
                if (remaining <= 0)
                {
                    movingRelativeCost = 0;
                    movingRelativeNetCost = 0;
                    movingRelativeQuoteCost = 0;
                    movingRelativeNetQuoteCost = 0;
                    heldShares = 0;
                }
                else
                {
                    movingRelativeCost = Math.round(movingRelativeCost / (double) heldShares * remaining);
                    movingRelativeNetCost = Math.round(movingRelativeNetCost / (double) heldShares * remaining);
                    movingRelativeQuoteCost = Math.round(movingRelativeQuoteCost / (double) heldShares * remaining);
                    movingRelativeNetQuoteCost = Math
                                    .round(movingRelativeNetQuoteCost / (double) heldShares * remaining);
                    heldShares = remaining;
                }

                for (LineItem entry : fifo)
                {
                    if (sold <= 0)
                        break;

                    if (!entry.owner.equals(item.getOwner()))
                        continue;

                    if (entry.shares == 0)
                        continue;

                    long n = Math.min(sold, entry.shares);

                    entry.grossAmount -= Math.round(n / (double) entry.shares * entry.grossAmount);
                    entry.netAmount -= Math.round(n / (double) entry.shares * entry.netAmount);
                    entry.grossQuoteAmount -= Math.round(n / (double) entry.shares * entry.grossQuoteAmount);
                    entry.netQuoteAmount -= Math.round(n / (double) entry.shares * entry.netQuoteAmount);
                    entry.shares -= n;

                    sold -= n;
                }

                if (sold > 0)
                {
                    // FIXME Oops. More sold than bought.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }

                break;

            case TRANSFER_IN:
                long moved = t.getShares();

                TransactionOwner<?> source = t.getCrossEntry().getCrossOwner(t);

                // iterate on copy b/c underlying list can be changed
                for (LineItem entry : new ArrayList<>(fifo))
                {
                    if (moved <= 0)
                        break;

                    if (!entry.owner.equals(source))
                        continue;

                    if (entry.shares == 0)
                        continue;

                    long n = Math.min(moved, entry.shares);

                    if (n == entry.shares)
                    {
                        // if all shares are moved, simply re-assign owner of
                        // the shares
                        entry.owner = item.getOwner();
                    }
                    else
                    {
                        long transferredGrossAmount = Math.round(n / (double) entry.shares * entry.grossAmount);
                        long transferredNetAmount = Math.round(n / (double) entry.shares * entry.netAmount);
                        long transferredGrossQuoteAmount = Math
                                        .round(n / (double) entry.shares * entry.grossQuoteAmount);
                        long transferredNetQuoteAmount = Math.round(n / (double) entry.shares * entry.netQuoteAmount);

                        LineItem transfer = new LineItem(item.getOwner(), //
                                        n, //
                                        transferredGrossAmount, //
                                        transferredNetAmount, //
                                        transferredGrossQuoteAmount, //
                                        transferredNetQuoteAmount, //
                                        entry.trail.fraction(Money.of(getTermCurrency(), transferredGrossAmount), n,
                                                        entry.originalShares) //
                                                        .transfer(item.getDateTime().toLocalDate(), entry.owner,
                                                                        item.getOwner()));

                        entry.grossAmount -= transferredGrossAmount;
                        entry.netAmount -= transferredNetAmount;
                        entry.grossQuoteAmount -= transferredGrossQuoteAmount;
                        entry.netQuoteAmount -= transferredNetQuoteAmount;
                        entry.shares -= n;

                        fifo.add(fifo.indexOf(entry) + 1, transfer);
                    }

                    moved -= n;
                }

                if (moved > 0)
                {
                    // FIXME Oops. More moved than available.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(moved), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }

                break;

            case TRANSFER_OUT:
                // ignore -> handled via TRANSFER_IN
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
        switch (t.getType())
        {
            case TAXES:
                taxes += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case TAX_REFUND:
                taxes -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case FEES:
                fees += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case FEES_REFUND:
                fees -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            default:
        }
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        taxes += t.getTransaction().orElseThrow(IllegalArgumentException::new).getUnitSum(Unit.Type.TAX, converter)
                        .getAmount();

        t.setFifoCost(getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED));
        t.setMovingAverageCost(getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED));
        t.setTotalShares(getSharesHeld());
    }

    public CostCalculationResult getResult()
    {
        return new CostCalculationResult(getSharesHeld(), getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        getFifoCostTrail(), getCost(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        getQuoteCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        getQuoteCost(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        getQuoteCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        getQuoteCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED), getFees(), getTaxes());
    }

    public TrailRecord getFifoCostTrail()
    {
        return TrailRecord.of(fifo.stream().filter(entry -> entry.grossAmount > 0) //
                        .map(entry -> entry.trail.fraction(Money.of(getTermCurrency(), entry.grossAmount), entry.shares,
                                        entry.originalShares))
                        .toList());
    }

    private long getSharesHeld()
    {
        long shares = 0;
        for (LineItem entry : fifo)
            shares += entry.shares;
        return shares;
    }

    public Money getFees()
    {
        return Money.of(getTermCurrency(), fees);
    }

    public Money getTaxes()
    {
        return Money.of(getTermCurrency(), taxes);
    }

    public Money getCost(CostMethod method, TaxesAndFees taxesAndFees)
    {
        return Money.of(getTermCurrency(), switch (method)
        {
            case FIFO -> sumFifo(taxesAndFees);
            case MOVING_AVERAGE -> taxesAndFees.isIncluded() ? movingRelativeCost : movingRelativeNetCost;
        });
    }

    public Money getQuoteCost(CostMethod method, TaxesAndFees taxesAndFees)
    {
        return Money.of(getTermCurrency(), switch (method)
        {
            case FIFO -> sumFifoQuoteCost(taxesAndFees);
            case MOVING_AVERAGE -> taxesAndFees.isIncluded() ? movingRelativeQuoteCost : movingRelativeNetQuoteCost;
        });
    }

    private long sumFifo(TaxesAndFees taxesAndFees)
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += taxesAndFees.isIncluded() ? entry.grossAmount : entry.netAmount;
        return cost;
    }

    private long sumFifoQuoteCost(TaxesAndFees taxesAndFees)
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += taxesAndFees.isIncluded() ? entry.grossQuoteAmount : entry.netQuoteAmount;
        return cost;
    }
}
