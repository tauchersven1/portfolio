package name.abuchen.portfolio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityMultiplier;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Quote;

/**
 * A helper for extracting historic quotes from the transactions of a security.
 * Aims to provide at least quotes from buying or selling to allow calculating
 * the correct performance.
 */
public class QuoteFromTransactionExtractor
{
    private final Client client;
    private final CurrencyConverter converter;

    /**
     * Constructs an instance.
     *
     * @param client
     *            {@link Client}
     */
    public QuoteFromTransactionExtractor(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;
    }

    /**
     * Extracts the quotes for the given {@link Security}.
     *
     * @param security
     *            {@link Security}
     * @return true if quotes were found, else false
     */
    public boolean extractQuotes(Security security)
    {
        if (security.getCurrencyCode() == null)
            return false;

        boolean bChanges = false;
        SecurityPrice pLatest = null;
        // walk through all transactions for security
        for (TransactionPair<?> p : security.getTransactions(client))
        {
            Transaction t = p.getTransaction();
            // check the type of the transaction
            if (t instanceof PortfolioTransaction pt)
            {
                // get date and quote and build a price from it
                Quote q = pt.getGrossPricePerShare();
                LocalDate d = pt.getDateTime().toLocalDate();

                /*
                 * The transaction model stores multiplier-aware gross values. For
                 * securities with a multiplier, getGrossPricePerShare() therefore
                 * represents market quote * multiplier. Historical security prices
                 * must remain raw market quotes; valuation and exposure apply the
                 * multiplier later.
                 */
                BigDecimal multiplier = SecurityMultiplier.valueAt(security, d);
                if (multiplier.compareTo(BigDecimal.ONE) != 0)
                {
                    long rawAmount = BigDecimal.valueOf(q.getAmount())
                                    .divide(multiplier, 0, RoundingMode.HALF_UP)
                                    .longValue();
                    q = Quote.of(q.getCurrencyCode(), rawAmount);
                }

                // check if currency conversion is needed
                if (!q.getCurrencyCode().equals(security.getCurrencyCode()))
                    q = converter.with(security.getCurrencyCode()).convert(d, q);

                SecurityPrice price = new SecurityPrice(d, q.getAmount());
                bChanges |= security.addPrice(price);
                // remember the latest price
                if ((pLatest == null) || d.isAfter(pLatest.getDate()))
                {
                    pLatest = price;
                }
            }
        }
        // set the latest price (if at least one price was found)
        if (pLatest != null)
        {
            LatestSecurityPrice lsp = new LatestSecurityPrice(pLatest.getDate(), pLatest.getValue());
            bChanges |= security.setLatest(lsp);
        }
        return bChanges;
    }
}
