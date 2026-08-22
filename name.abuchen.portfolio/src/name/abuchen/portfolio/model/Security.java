package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

/**
 * A <code>Security</code> is used for assets that have historical prices
 * attached.
 * <p/>
 * <strong>Attributes</strong> are managed and edited by the user while
 * <strong>properties</strong> are managed by the program.
 */
public final class Security implements Attributable, InvestmentVehicle
{
    public static final class ByName implements Comparator<Security>, Serializable
    {
        private static final long serialVersionUID = 1L;

        private final SecurityNameConfig config;

        public ByName()
        {
            this(SecurityNameConfig.NONE);
        }

        public ByName(SecurityNameConfig config)
        {
            this.config = config;
        }

        @Override
        public int compare(Security s1, Security s2)
        {
            if (s1 == null && s2 == null)
                return 0;
            else if (s1 == null)
                return -1;
            else if (s2 == null)
                return 1;

            return TextUtil.compare(s1.getName(config), s2.getName(config));
        }
    }

    private String uuid;
    private String onlineId;

    private String name;
    private String currencyCode = CurrencyUnit.EUR;
    private String targetCurrencyCode;

    private String note;

    private String isin;
    private String tickerSymbol;
    private String wkn;
    private String calendar;

    // feed and feedURL are used to update historical prices
    private String feed;
    private String feedURL;
    private List<SecurityPrice> prices = new ArrayList<>();
    private List<SecurityMultiplier> multipliers = new ArrayList<>();

    // latestFeed and latestFeedURL are used to update the latest (current)
    // quote. If null, the values from feed and feedURL are used instead.
    private String latestFeed;
    private String latestFeedURL;
    private LatestSecurityPrice latest;

    private Attributes attributes;

    private List<SecurityEvent> events;
    private List<SecurityProperty> properties;

    private boolean isRetired = false;

    /**
     * Stores ephemeral data related to the current session that is not
     * persisted.
     */
    private transient SecurityEphemeralData data; // NOSONAR

    private Instant updatedAt;

    @Deprecated
    private String type;

    @Deprecated
    private String industryClassification;

    @VisibleForTesting
    public Security()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Security(String name, String currencyCode)
    {
        this();
        this.name = name;
        this.currencyCode = currencyCode;
    }

    public Security(String name, String isin, String tickerSymbol, String feed)
    {
        this();
        this.name = name;
        this.isin = isin;
        this.tickerSymbol = tickerSymbol;
        this.feed = feed;
    }

    /* package */ Security(String uuid)
    {
        this.uuid = uuid;
    }

    @Override
    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        uuid = UUID.randomUUID().toString();
    }

    public void fixMissingUUID()
    {
        if (uuid == null)
            generateUUID();
    }

    public String getOnlineId()
    {
        return onlineId;
    }

    public void setOnlineId(String onlineId)
    {
        this.onlineId = onlineId;
        this.updatedAt = Instant.now();
    }

    @Override
    public String getName()
    {
        return name;
    }

    public String getName(SecurityNameConfig config)
    {
        switch (config)
        {
            case NONE:
                return name;
            case ISIN:
                return getIsin() != null ? getIsin() + " (" + name + ")" : name; //$NON-NLS-1$ //$NON-NLS-2$
            case TICKER_SYMBOL:
                return getTickerSymbol() != null ? getTickerSymbol() + " (" + name + ")" : name; //$NON-NLS-1$ //$NON-NLS-2$
            case WKN:
                return getWkn() != null ? getWkn() + " (" + name + ")" : name; //$NON-NLS-1$ //$NON-NLS-2$
            default:
                throw new IllegalArgumentException(config.name());
        }
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    @Override
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
        this.updatedAt = Instant.now();
    }

    public String getTargetCurrencyCode()
    {
        return this.targetCurrencyCode;
    }

    public void setTargetCurrencyCode(String targetCurrencyCode)
    {
        this.targetCurrencyCode = targetCurrencyCode;
        this.updatedAt = Instant.now();
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
        this.updatedAt = Instant.now();
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        this.isin = isin;
        this.updatedAt = Instant.now();
    }

    public String getTickerSymbol()
    {
        return tickerSymbol;
    }

    public void setTickerSymbol(String tickerSymbol)
    {
        this.tickerSymbol = tickerSymbol;
        this.updatedAt = Instant.now();
    }

    public String getTickerSymbolWithoutStockMarket()
    {
        if (tickerSymbol == null)
            return null;

        int p = tickerSymbol.indexOf('.');
        return p >= 0 ? tickerSymbol.substring(0, p) : tickerSymbol;
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        this.wkn = wkn;
        this.updatedAt = Instant.now();
    }

    public String getCalendar()
    {
        return calendar;
    }

    public void setCalendar(String calendar)
    {
        this.calendar = calendar;
        this.updatedAt = Instant.now();
    }

    public boolean isExchangeRate()
    {
        return this.targetCurrencyCode != null;
    }

    public String getExternalIdentifier()
    {
        if (isin != null && isin.length() > 0)
            return isin;
        else if (tickerSymbol != null && tickerSymbol.length() > 0)
            return getTickerSymbolWithoutStockMarket();
        else if (wkn != null && wkn.length() > 0)
            return wkn;
        else
            return name;
    }

    @Deprecated
    /* package */String getIndustryClassification()
    {
        return industryClassification;
    }

    @Deprecated
    /* package */void setIndustryClassification(String industryClassification)
    {
        this.industryClassification = industryClassification;
    }

    @Deprecated
    /* package */String getType()
    {
        return type;
    }

    @Deprecated
    /* package */void setType(String type)
    {
        this.type = type;
    }

    public String getFeed()
    {
        return feed;
    }

    public void setFeed(String feed)
    {
        this.feed = feed;
        this.updatedAt = Instant.now();
    }

    public String getFeedURL()
    {
        return feedURL;
    }

    public void setFeedURL(String feedURL)
    {
        this.feedURL = feedURL;
        this.updatedAt = Instant.now();
    }

    public List<SecurityPrice> getPrices()
    {
        return Collections.unmodifiableList(prices);
    }

    public List<SecurityMultiplier> getMultipliers()
    {
        if (multipliers == null)
            multipliers = new ArrayList<>();
        return Collections.unmodifiableList(multipliers);
    }

    public boolean addMultiplier(SecurityMultiplier multiplier)
    {
        Objects.requireNonNull(multiplier);

        if (multipliers == null)
            multipliers = new ArrayList<>();

        int index = Collections.binarySearch(multipliers, multiplier);

        if (index < 0)
        {
            multipliers.add(~index, multiplier);
            return true;
        }

        SecurityMultiplier replaced = multipliers.get(index);
        if (!replaced.equals(multiplier))
        {
            multipliers.set(index, multiplier);
            return true;
        }

        return false;
    }

    public void removeMultiplier(SecurityMultiplier multiplier)
    {
        if (multipliers != null)
            multipliers.remove(multiplier);
    }

    public void removeAllMultipliers()
    {
        if (multipliers != null)
            multipliers.clear();
    }

    public SecurityMultiplier getSecurityMultiplier(LocalDate requestedDate)
    {
        Objects.requireNonNull(requestedDate);

        if (multipliers == null || multipliers.isEmpty())
            return SecurityMultiplier.of(requestedDate, 1.0);

        SecurityMultiplier last = multipliers.get(multipliers.size() - 1);
        if (!last.getDate().isAfter(requestedDate))
            return last;

        SecurityMultiplier p = new SecurityMultiplier(requestedDate, 0);
        int index = Collections.binarySearch(multipliers, p);

        if (index >= 0)
            return multipliers.get(index);
        else if (index == -1)
            return multipliers.get(0);
        else
            return multipliers.get(-index - 2);
    }

    public double getMultiplier(LocalDate requestedDate)
    {
        return getSecurityMultiplier(requestedDate).getMultiplier();
    }

    /* protobuf only */ void protobufSetMultipliers(List<SecurityMultiplier> newMultipliers)
    {
        this.multipliers = newMultipliers;
    }

    public List<SecurityPrice> getPricesIncludingLatest()
    {
        List<SecurityPrice> copy = new ArrayList<>(prices);

        if (latest == null)
            return copy;

        int index = Collections.binarySearch(copy, new SecurityPrice(latest.getDate(), latest.getValue()));

        if (index >= 0)
            return copy;

        copy.add(~index, latest);
        return copy;
    }

    public List<SecurityPrice> getLatestNPricesOfDate(LocalDate dateOfLastPrice, int numberOfPrices)
    {
        List<SecurityPrice> allPrices = getPricesIncludingLatest();

        int index = Collections.binarySearch(allPrices, new SecurityPrice(dateOfLastPrice, 0),
                        new SecurityPrice.ByDate());

        if (index < 0)
            index = -index - 2;

        if (index >= allPrices.size())
            index = allPrices.size() - 1;

        int fromIndex = index - numberOfPrices + 1;
        if (fromIndex < 0)
            fromIndex = 0;

        return new ArrayList<>(allPrices.subList(fromIndex, index + 1));
    }

    public boolean addPrice(SecurityPrice price)
    {
        return addPrice(price, true);
    }

    public boolean addPrice(SecurityPrice price, boolean overwriteExisting)
    {
        Objects.requireNonNull(price);

        int index = Collections.binarySearch(prices, price);

        if (index < 0)
        {
            prices.add(~index, price);
            return true;
        }
        else
        {
            SecurityPrice replaced = prices.get(index);

            if (!replaced.equals(price) && (overwriteExisting || replaced.getValue() == 0.0))
            {
                prices.set(index, price);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public boolean addAllPrices(List<SecurityPrice> newPrices)
    {
        if (newPrices.isEmpty())
            return false;

        LocalDate now = LocalDate.now();

        LocalDate last = null;
        if (!this.prices.isEmpty())
            last = this.prices.get(this.prices.size() - 1).getDate();

        boolean isUpdated = false;
        for (SecurityPrice p : newPrices)
        {
            if (!p.getDate().isAfter(now))
            {
                boolean doOverwrite = p.getDate().equals(last);
                boolean isAdded = addPrice(p, doOverwrite);
                isUpdated = isUpdated || isAdded;
            }
        }
        return isUpdated;
    }

    /* protobuf only */ void protobufSetPrices(List<SecurityPrice> newPrices)
    {
        this.prices = newPrices;
    }

    public void removePrice(SecurityPrice price)
    {
        prices.remove(price);
    }

    public void removeAllPrices()
    {
        prices.clear();
    }

    public SecurityPrice getSecurityPrice(LocalDate requestedDate)
    {
        SecurityPrice lastHistoric = prices.isEmpty() ? null : prices.get(prices.size() - 1);

        if (latest != null //
                        && (lastHistoric == null //
                                        || (!requestedDate.isBefore(latest.getDate()) && //
                                                        !latest.getDate().isBefore(lastHistoric.getDate()) //
                                        )))
            return latest;

        if (lastHistoric == null)
            return new SecurityPrice(requestedDate, 0);

        if (!lastHistoric.getDate().isAfter(requestedDate))
            return lastHistoric;

        SecurityPrice p = new SecurityPrice(requestedDate, 0);
        int index = Collections.binarySearch(prices, p);

        if (index >= 0)
            return prices.get(index);
        else if (index == -1)
            return prices.get(0);
        else
            return prices.get(-index - 2);
    }

    public Optional<Pair<SecurityPrice, SecurityPrice>> getLatestTwoSecurityPrices()
    {
        if (prices.isEmpty())
            return Optional.empty();

        List<SecurityPrice> list = getPricesIncludingLatest();
        if (list.size() < 2)
            return Optional.empty();

        LocalDate now = LocalDate.now();

        SecurityPrice today = null;

        int index = list.size() - 1;
        while (index >= 0)
        {
            today = list.get(index);
            if (!today.getDate().isAfter(now))
                break;
            index--;
        }

        return index > 0 ? Optional.of(new Pair<>(list.get(index), list.get(index - 1))) : Optional.empty();
    }

    public String getLatestFeed()
    {
        return latestFeed;
    }

    public void setLatestFeed(String latestFeed)
    {
        this.latestFeed = latestFeed;
        this.updatedAt = Instant.now();
    }

    public String getLatestFeedURL()
    {
        return latestFeedURL;
    }

    public void setLatestFeedURL(String latestFeedURL)
    {
        this.latestFeedURL = latestFeedURL;
        this.updatedAt = Instant.now();
    }

    public LatestSecurityPrice getLatest()
    {
        return latest;
    }

    public boolean setLatest(LatestSecurityPrice latest)
    {
        if (!Objects.equals(latest, this.latest))
        {
            this.latest = latest;
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean isRetired()
    {
        return isRetired;
    }

    @Override
    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
        this.updatedAt = Instant.now();
    }

    public List<SecurityEvent> getEvents()
    {
        if (this.events == null)
            this.events = new ArrayList<>();
        return events;
    }

    public void addEvent(SecurityEvent event)
    {
        if (this.events == null)
            this.events = new ArrayList<>();
        this.events.add(event);
    }

    public void removeEvent(SecurityEvent event)
    {
        if (this.events == null)
            this.events = new ArrayList<>();
        this.events.remove(event);
    }

    public boolean removeAllEvents()
    {
        boolean removed = this.events != null && !this.events.isEmpty();
        this.events = null;
        return removed;
    }

    public boolean removeEventIf(Predicate<SecurityEvent> filter)
    {
        if (events != null)
            return events.removeIf(filter);
        else
            return false;
    }

    public Stream<SecurityProperty> getProperties()
    {
        if (properties == null)
            properties = new ArrayList<>();
        return properties.stream();
    }

    public Optional<String> getPropertyValue(SecurityProperty.Type type, String name)
    {
        return getProperties().filter(p -> p.getType() == type && name.equals(p.getName()))
                        .map(SecurityProperty::getValue).findAny();
    }

    public boolean setPropertyValue(SecurityProperty.Type type, String name, String value)
    {
        if (properties == null)
            properties = new ArrayList<>();

        Optional<SecurityProperty> prop = properties.stream()
                        .filter(p -> p.getType() == type && name.equals(p.getName())).findAny();

        if (prop.isPresent())
        {
            boolean isEqual = Objects.equals(prop.get().getValue(), value);

            if (!isEqual)
            {
                properties.remove(prop.get());

                if (value != null)
                    properties.add(new SecurityProperty(type, name, value));
            }

            return !isEqual;
        }
        else if (value != null)
        {
            properties.add(new SecurityProperty(type, name, value));
            return true;
        }
        else
        {
            return false;
        }
    }

    public void addProperty(SecurityProperty data)
    {
        if (properties == null)
            properties = new ArrayList<>();
        this.properties.add(data);
    }

    public boolean removeProperty(SecurityProperty data)
    {
        if (properties == null)
            return false;
        return this.properties.remove(data);
    }

    public boolean removePropertyIf(Predicate<SecurityProperty> filter)
    {
        if (properties != null)
            return properties.removeIf(filter);
        else
            return false;
    }

    @Override
    public Attributes getAttributes()
    {
        if (attributes == null)
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
        this.updatedAt = Instant.now();
    }

    public Stream<Bookmark> getCustomBookmarks(Client client)
    {
        List<Bookmark> bookmarks = new ArrayList<>();

        var myAttributes = getAttributes();
        client.getSettings().getAttributeTypes() //
                        .filter(t -> t.getType() == Bookmark.class) //
                        .forEachOrdered(attributeType -> {
                            var bookmark = (Bookmark) myAttributes.get(attributeType);
                            if (bookmark != null)
                            {
                                bookmarks.add(new Bookmark(bookmark.getPattern().equals(bookmark.getLabel())
                                                ? attributeType.getName()
                                                : bookmark.getLabel(), bookmark.getPattern()));
                            }
                        });

        if (!Strings.isNullOrEmpty(note))
        {
            Pattern urlPattern = Pattern.compile("(https?\\:\\/\\/[^ \\t\\r\\n]+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
            Matcher m = urlPattern.matcher(note);
            while (m.find())
                bookmarks.add(new Bookmark(m.group()));
        }

        return bookmarks.stream();
    }

    public List<TransactionPair<?>> getTransactions(Client client)
    {
        List<TransactionPair<?>> answer = new ArrayList<>();

        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream() //
                            .filter(t -> this.equals(t.getSecurity()))
                            .filter(t -> t.getType() == AccountTransaction.Type.INTEREST
                                            || t.getType() == AccountTransaction.Type.DIVIDENDS
                                            || t.getType() == AccountTransaction.Type.TAXES
                                            || t.getType() == AccountTransaction.Type.TAX_REFUND
                                            || t.getType() == AccountTransaction.Type.FEES
                                            || t.getType() == AccountTransaction.Type.FEES_REFUND)
                            .map(t -> new TransactionPair<AccountTransaction>(account, t)) //
                            .forEach(answer::add);
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream() //
                            .filter(t -> this.equals(t.getSecurity()))
                            .map(t -> new TransactionPair<PortfolioTransaction>(portfolio, t)) //
                            .forEach(answer::add);
        }

        return answer;
    }

    public boolean hasTransactions(Client client)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            Optional<PortfolioTransaction> transaction = portfolio.getTransactions().stream()
                            .filter(t -> this.equals(t.getSecurity())).findAny();

            if (transaction.isPresent())
                return true;
        }

        for (Account account : client.getAccounts())
        {
            Optional<AccountTransaction> transaction = account.getTransactions().stream()
                            .filter(t -> this.equals(t.getSecurity())).findAny();

            if (transaction.isPresent())
                return true;
        }

        return false;
    }

    public SecurityEphemeralData getEphemeralData()
    {
        if (data == null)
            data = new SecurityEphemeralData();
        return data;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public Security deepCopy()
    {
        Security answer = new Security();

        answer.onlineId = onlineId;

        answer.name = name;
        answer.currencyCode = currencyCode;
        answer.targetCurrencyCode = targetCurrencyCode;

        answer.note = note;
        answer.isin = isin;
        answer.tickerSymbol = tickerSymbol;
        answer.wkn = wkn;
        answer.calendar = calendar;

        answer.feed = feed;
        answer.feedURL = feedURL;

        answer.prices = new ArrayList<>(
                        prices.stream().map(p -> new SecurityPrice(p.getDate(), p.getValue())).toList());

        if (multipliers != null)
            answer.multipliers = new ArrayList<>(multipliers.stream()
                            .map(m -> new SecurityMultiplier(m.getDate(), m.getValue())).toList());

        answer.latestFeed = latestFeed;
        answer.latestFeedURL = latestFeedURL;
        answer.latest = latest;

        answer.events = new ArrayList<>(getEvents());

        if (properties != null)
            answer.properties = new ArrayList<>(properties);

        answer.isRetired = isRetired;

        answer.updatedAt = updatedAt;

        return answer;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String toInfoString()
    {
        StringBuilder b = new StringBuilder();
        b.append(name);

        if (notEmpty(isin))
            b.append('\n').append(isin);
        if (notEmpty(wkn))
            b.append('\n').append(wkn);
        if (notEmpty(tickerSymbol))
            b.append('\n').append(tickerSymbol);

        if (notEmpty(note))
            b.append("\n\n").append(note); //$NON-NLS-1$

        return b.toString();
    }

    private boolean notEmpty(String s)
    {
        return s != null && s.length() > 0;
    }
}