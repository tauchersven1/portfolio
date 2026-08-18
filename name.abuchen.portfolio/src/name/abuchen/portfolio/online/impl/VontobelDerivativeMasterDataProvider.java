package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.DerivativeMasterDataProvider;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Enriches Vontobel leverage products from the public German product pages.
 * The parser deliberately extracts only stable labels visible on those pages.
 */
@SuppressWarnings("nls")
public class VontobelDerivativeMasterDataProvider implements DerivativeMasterDataProvider
{
    private static final List<String> PRODUCT_PATHS = List.of(
                    "/de-de/produkte/hebel/turbo-optionsscheine-open-end/",
                    "/de-de/produkte/hebel/turbo-optionsscheine/",
                    "/de-de/produkte/hebel/mini-futures/",
                    "/de-de/produkte/hebel/optionsscheine/");

    private static final Pattern ISIN = Pattern.compile("\\b(DE[A-Z0-9]{10})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WKN = Pattern.compile("\\bWKN\\s+([A-Z0-9]{6})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FX_PAIR = Pattern.compile("\\b([A-Z]{3})/([A-Z]{3})\\b");
    private static final Pattern FX_PER_ONE = Pattern.compile("\\b([A-Z]{3})\\s+per\\s+1\\s+([A-Z]{3})\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIKE = Pattern.compile("Basispreis\\s*:?[\\s|]*([0-9][0-9.,]*)\\s*([A-Z]{3})?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern KO = Pattern.compile("(?:Knock-Out(?:\\s+Barriere|\\s+Schwelle)?|K\\.O\\.)\\s*:?[\\s|]*([0-9][0-9.,]*)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern RATIO = Pattern.compile("Bezugsverh(?:ä|&auml;)ltnis\\s*:?[\\s|]*([0-9][0-9.,]*)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_TRADING = Pattern.compile("Erster Handelstag\\s*:?[\\s|]*(\\d{2}\\.\\d{2}\\.\\d{4})",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern LAST_TRADING = Pattern.compile("Letzter Handelstag\\s*:?[\\s|]*(\\d{2}\\.\\d{2}\\.\\d{4})",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUATION = Pattern.compile("Bewertungstag\\s*:?[\\s|]*(\\d{2}\\.\\d{2}\\.\\d{4})",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern REPAYMENT = Pattern.compile("R(?:ü|&uuml;)ckzahlung(?:stag)?\\s*:?[\\s|]*(\\d{2}\\.\\d{2}\\.\\d{4})",
                    Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY);

    @Override
    public String getName()
    {
        return "Vontobel Markets";
    }

    @Override
    public Optional<Result> lookup(Security security) throws IOException
    {
        String identifier = firstNonBlank(security.getIsin(), security.getWkn());
        if (identifier == null)
            return Optional.empty();

        String html = null;
        String matchedPath = null;
        IOException last = null;
        for (String path : PRODUCT_PATHS)
        {
            try
            {
                String candidate = new WebAccess("markets.vontobel.com", path + identifier.trim()).get();
                if (containsIdentifier(candidate, identifier))
                {
                    html = candidate;
                    matchedPath = path;
                    break;
                }
            }
            catch (WebAccessException e)
            {
                last = e;
            }
        }

        if (html == null)
        {
            if (last != null && last.getHttpErrorCode() >= 500)
                throw last;
            return Optional.empty();
        }

        String text = normalizeHtml(html);
        Result result = new Result();
        result.put("issuer", "Vontobel");
        match(ISIN, text, 1).ifPresent(value -> result.put("isin", value.toUpperCase(Locale.ROOT)));
        match(WKN, text, 1).ifPresent(value -> result.put("wkn", value.toUpperCase(Locale.ROOT)));

        boolean standardOption = matchedPath != null && matchedPath.endsWith("/optionsscheine/");
        result.put("type", "OPTION");
        result.put("optionProductType", standardOption ? "VANILLA" : "KNOCK_OUT_CERTIFICATE");

        if (containsWord(text, "Call") || containsWord(text, "Long"))
            result.put("putCall", "CALL");
        else if (containsWord(text, "Put") || containsWord(text, "Short"))
            result.put("putCall", "PUT");

        match(STRIKE, text, 1).map(VontobelDerivativeMasterDataProvider::normalizeDecimal)
                        .ifPresent(value -> result.put("strike", value));
        match(RATIO, text, 1).map(VontobelDerivativeMasterDataProvider::normalizeDecimal)
                        .ifPresent(value -> result.put("subscriptionRatio", value));

        if (!standardOption)
        {
            match(KO, text, 1).map(VontobelDerivativeMasterDataProvider::normalizeDecimal).ifPresent(value -> {
                result.put("initialKnockoutLevel", value);
                result.put("currentKnockoutLevel", value);
            });
        }

        Optional<String[]> pair = extractFxPair(text);
        if (pair.isPresent())
        {
            String[] currencies = pair.get();
            result.put("underlying", currencies[0] + "/" + currencies[1]);
            result.put("fxUnderlying", "true");
            result.put("fxBaseCurrency", currencies[0]);
            result.put("fxQuoteCurrency", currencies[1]);
        }

        matchDate(FIRST_TRADING, text).ifPresent(value -> result.put("firstTradingDay", value));
        matchDate(LAST_TRADING, text).ifPresent(value -> result.put("lastTradingDay", value));
        matchDate(VALUATION, text).ifPresent(value -> result.put("expirationDate", value));
        matchDate(REPAYMENT, text).ifPresent(value -> result.put("settlementDate", value));

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private static Optional<String[]> extractFxPair(String text)
    {
        Matcher direct = FX_PAIR.matcher(text.toUpperCase(Locale.ROOT));
        if (direct.find())
            return Optional.of(new String[] { direct.group(1), direct.group(2) });

        Matcher perOne = FX_PER_ONE.matcher(text.toUpperCase(Locale.ROOT));
        if (perOne.find())
            return Optional.of(new String[] { perOne.group(2), perOne.group(1) });

        return Optional.empty();
    }

    private static Optional<String> matchDate(Pattern pattern, String text)
    {
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
        return html != null && html.toUpperCase(Locale.ROOT).contains(identifier.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean containsWord(String text, String word)
    {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private static String normalizeHtml(String html)
    {
        return html.replaceAll("(?is)<script.*?</script>", " ")
                        .replaceAll("(?is)<style.*?</style>", " ")
                        .replaceAll("(?s)<[^>]+>", " ")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&auml;", "ä")
                        .replace("&ouml;", "ö")
                        .replace("&uuml;", "ü")
                        .replace("&Auml;", "Ä")
                        .replace("&Ouml;", "Ö")
                        .replace("&Uuml;", "Ü")
                        .replaceAll("\\s+", " ")
                        .trim();
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
