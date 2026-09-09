package de.venari.portfolio.derivatives;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class DatedValueSeries
{
    record Entry(LocalDate date, BigDecimal value) {}

    private DatedValueSeries() {}

    static List<Entry> parse(String encoded)
    {
        List<Entry> entries = new ArrayList<>();
        if (encoded == null || encoded.isBlank())
            return entries;
        for (String token : encoded.split(";")) //$NON-NLS-1$
        {
            String[] parts = token.split("=", 2); //$NON-NLS-1$
            try
            {
                if (parts.length == 2)
                    entries.add(new Entry(LocalDate.parse(parts[0]), new BigDecimal(parts[1])));
            }
            catch (RuntimeException ignore)
            {
                // Skip invalid legacy/user values.
            }
        }
        entries.sort(Comparator.comparing(Entry::date));
        return entries;
    }

    static String serialize(List<Entry> entries)
    {
        return entries.stream().sorted(Comparator.comparing(Entry::date))
                        .map(e -> e.date() + "=" + e.value().stripTrailingZeros().toPlainString()) //$NON-NLS-1$
                        .collect(Collectors.joining(";")); //$NON-NLS-1$
    }

    static Optional<BigDecimal> valueAt(String encoded, LocalDate date)
    {
        return parse(encoded).stream().filter(e -> !e.date().isAfter(date)).reduce((a, b) -> b).map(Entry::value);
    }
}
