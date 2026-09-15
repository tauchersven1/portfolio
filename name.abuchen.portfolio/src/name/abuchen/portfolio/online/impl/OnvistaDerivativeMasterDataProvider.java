package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.DerivativeMasterDataProvider;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Enriches derivative master data from public onvista product pages. Onvista
 * supports short URLs such as /DE000... and /WKN which redirect to the actual
 * product page. The parser deliberately relies on visible labels instead of
 * internal JSON structures so changes in frontend implementation have less
 * impact.
 */
@SuppressWarnings("nls")
public class OnvistaDerivativeMasterDataProvider implements DerivativeMasterDataProvider
{
    private static final Pattern ISIN = Pattern.compile("\\b([A-Z]{2}[A-Z0-9]{10})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WKN = Pattern.compile("\\bWKN\\s*([A-Z0-9]{6})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUER = Pattern.compile(
                    "Emittent\\s*:?[\\s|]*([A-Za-z0-9ÄÖÜäöüß&.()' /+-]{2,80}?)(?=\\s+(?:WKN|ISIN|Geld|Brief|Basispreis|K\\.O\\.|Knock-Out|Hebel|Bezugsverh))",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FX_PAIR = Pattern.compile("\\b([A-Z]{3})[ /-]([A-Z]{3})\\b");
    private static final Pattern STRIKE = Pattern.compile("Basispreis\\s*:?[\\s|]*([0-9][0-9.,]*)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern KO = Pattern.compile(
                    "(?:K\\.O\\.|K\\.?O\\.?[- ]?(?:Schwelle)?|Knock-Out(?:[- ]?(?:Schwelle|Barriere))?)\\s*:?[\\s|]*([0-9][0-9.,]*)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern RATIO = Pattern.compile(
                    "Bezugsverh(?:ä|&auml;)ltnis\\s*:?[\\s|]*([0-9][0-9.,]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVERAGE = Pattern.compile("Hebel\\s*:?[\\s|]*([0-9][0-9.,]*)\\s*x?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERLYING_PRICE = Pattern.compile(
                    "Basiswert(?:kurs)?\\s+.{0,120}?\\s(?:·\\s*)?(?:[^0-9]{0,40})?([0-9][0-9.,]*)\\s*([A-Z]{3})\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERLYING_FROM_TITLE = Pattern.compile(
                    "(?:LONG|SHORT|CALL|PUT|ZERTIFIKAT|OPTIONSSCHEIN)[^|]{0,100}?\\sAUF\\s+(.{2,120}?)(?=\\s+(?:WKN|ISIN|Emittent|Geld|Brief|Basispreis|K\\.O\\.|Knock-Out|Hebel|Bezugsverh))",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERLYING_LABEL = Pattern.compile(
                    "Basiswert\\s*:?[\\s|]*([A-Za-z0-9ÄÖÜäöüß&.()' /+_-]{2,120}?)(?=\\s+(?:[A-Za-z][A-Za-z ._-]{0,30}\\s*·|Basispreis|K\\.O\\.|Knock-Out|Hebel|Bezugsverh|Rechtlich|WKN|ISIN|Emittent))",
                    Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY);

    @Override
    public String getName()
    {
        return "onvista";
    }

    @Override
    public Optional<Result> lookup(Security security) throws IOException
    {
        String identifier = firstNonBlank(security.getIsin(), security.getWkn());
        if (identifier == null)
            return Optional.empty();

        try
        {
            String html = download(identifier.trim());
            if (html == null || !containsIdentifier(html, identifier))
                return Optional.empty();

            Result result = parsePage(html);
            if (result.isEmpty())
                return Optional.empty();

            String issuerLeverage = result.get("issuerLeverage");
            if (issuerLeverage != null && security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, "issuerLeverage").isEmpty())
                security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, "issuerLeverage", issuerLeverage);

            return Optional.of(result);
        }
        catch (WebAccessException e)
        {
            if (e.getHttpErrorCode() >= 500)
                throw e;
            return Optional.empty();
        }
    }

    private String download(String identifier) throws IOException
    {
        String host = "www.onvista.de";
        String path = "/" + identifier;

        for (int ii = 0; ii < 4; ii++)
        {
            try
            {
                return new WebAccess(host, path).get();
            }
            catch (WebAccessException e)
            {
                if (e.getHttpErrorCode() < 300 || e.getHttpErrorCode() >= 400)
                    throw e;

                Optional<String> location = e.getHeader("Location").stream().findFirst();
                if (location.isEmpty())
                    throw e;

                URI uri = URI.create(location.get());
                if (uri.getHost() != null)
                {
                    if (!"onvista.de".equalsIgnoreCase(uri.getHost()) && !"www.onvista.de".equalsIgnoreCase(uri.getHost()))
                        throw e;
                    host = uri.getHost();
                }

                String nextPath = uri.getRawPath();
                if (nextPath == null || nextPath.isBlank())
                    throw e;
                if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank())
                    nextPath += "?" + uri.getRawQuery();
                path = nextPath;
            }
        }

        return null;
    }

    static Result parsePage(String html)
    {
        String text = normalizeHtml(html);
        Result result = new Result();

        result.put("type", "OPTION");
        result.put("optionProductType", "KNOCK_OUT_CERTIFICATE");

        match(ISIN, text, 1).ifPresent(value -> result.put("isin", value.toUpperCase(Locale.ROOT)));
        match(WKN, text, 1).ifPresent(value -> {
            result.put("wkn", value.toUpperCase(Locale.ROOT));
            result.put("issuerProductId", value.toUpperCase(Locale.ROOT));
        });
        match(ISSUER, text, 1).map(OnvistaDerivativeMasterDataProvider::normalizeLabel)
                        .ifPresent(value -> result.put("issuer", value));

        if (containsWord(text, "Long") || containsWord(text, "Call"))
            result.put("putCall", "CALL");
        else if (containsWord(text, "Short") || containsWord(text, "Put"))
            result.put("putCall", "PUT");

        match(STRIKE, text, 1).map(OnvistaDerivativeMasterDataProvider::normalizeDecimal)
                        .ifPresent(value -> result.put("strike", value));
        match(KO, text, 1).map(OnvistaDerivativeMasterDataProvider::normalizeDecimal).ifPresent(value -> {
            result.put("initialKnockoutLevel", value);
            result.put("currentKnockoutLevel", value);
        });
        match(RATIO, text, 1).map(OnvistaDerivativeMasterDataProvider::normalizeDecimal)
                        .ifPresent(value -> result.put("subscriptionRatio", value));
        match(LEVERAGE, text, 1).map(OnvistaDerivativeMasterDataProvider::normalizeDecimal)
                        .ifPresent(value -> result.put("issuerLeverage", value));

        Optional<String[]> fxPair = extractFxPair(text);
        if (fxPair.isPresent())
        {
            String[] currencies = fxPair.get();
            result.put("underlying", currencies[0] + "/" + currencies[1]);
            result.put("fxUnderlying", "true");
            result.put("fxBaseCurrency", currencies[0]);
            result.put("fxQuoteCurrency", currencies[1]);
        }
        else
        {
            extractUnderlying(text).ifPresent(value -> result.put("underlying", value));
            Matcher price = UNDERLYING_PRICE.matcher(text);
            if (price.find())
            {
                result.put("issuerUnderlyingPrice", normalizeDecimal(price.group(1)));
                result.put("issuerUnderlyingCurrency", price.group(2).toUpperCase(Locale.ROOT));
            }
        }

        matchDate(text, "Bewertungstag").ifPresent(value -> {
            result.put("expirationDate", value);
            result.put("lastTradingDay", value);
        });

        return result;
    }

    private static Optional<String[]> extractFxPair(String text)
    {
        Matcher matcher = FX_PAIR.matcher(text.toUpperCase(Locale.ROOT));
        while (matcher.find())
        {
            String first = matcher.group(1);
            String second = matcher.group(2);
            if (!first.equals(second) && isCurrency(first) && isCurrency(second))
                return Optional.of(new String[] { first, second });
        }
        return Optional.empty();
    }

    private static boolean isCurrency(String value)
    {
        return switch (value)
        {
            case "AED", "AUD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "JPY", "KRW", "MXN", "NOK", "NZD", "PLN", "RON", "RUB", "SEK", "SGD", "THB", "TRY", "USD", "ZAR" -> true;
            default -> false;
        };
    }

    private static Optional<String> extractUnderlying(String text)
    {
        Optional<String> value = match(UNDERLYING_LABEL, text, 1);
        if (value.isEmpty())
            value = match(UNDERLYING_FROM_TITLE, text, 1);
        return value.map(OnvistaDerivativeMasterDataProvider::normalizeLabel).filter(v -> !v.isBlank());
    }

    private static Optional<String> matchDate(String text, String label)
    {
        Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s*:?[\\s|]*(\\d{2}\\.\\d{2}\\.\\d{4})",
                        Pattern.CASE_INSENSITIVE);
        Optional<String> value = match(pattern, text, 1);
        if (value.isEmpty())
            return Optional.empty();
        try
        {
            return Optional.of(LocalDate.parse(value.get(), GERMAN_DATE).toString());
        }
        catch (DateTimeParseException e)
        {
            return Optional.empty();
        }
    }

    private static Optional<String> match(Pattern pattern, String text, int group)
    {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.ofNullable(matcher.group(group)) : Optional.empty();
    }

    private static boolean containsIdentifier(String html, String identifier)
    {
        return identifier != null && html != null
                        && html.toUpperCase(Locale.ROOT).contains(identifier.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean containsWord(String text, String word)
    {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private static String normalizeHtml(String html)
    {
        return html.replaceAll("(?is)<script.*?</script>", " ").replaceAll("(?is)<style.*?</style>", " ")
                        .replaceAll("(?s)<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                        .replace("&auml;", "ä").replace("&ouml;", "ö").replace("&uuml;", "ü")
                        .replace("&Auml;", "Ä").replace("&Ouml;", "Ö").replace("&Uuml;", "Ü")
                        .replace("&#x27;", "'").replace("&quot;", "\"").replaceAll("\\s+", " ").trim();
    }

    private static String normalizeLabel(String value)
    {
        return value.replaceAll("\\s+", " ").replaceAll("^[|: -]+|[|: -]+$", "").trim();
    }

    private static String normalizeDecimal(String value)
    {
        String cleaned = value.trim();
        if (cleaned.contains(",") && cleaned.contains("."))
            cleaned = cleaned.replace(".", "").replace(',', '.');
        else
            cleaned = cleaned.replace(',', '.');
        return cleaned;
    }

    private static String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (value != null && !value.isBlank())
                return value;
        }
        return null;
    }
}
