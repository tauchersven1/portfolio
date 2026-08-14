package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A time-dependent knock-out level attached to an option security. A level is
 * valid from its date until it is superseded by a later entry.
 */
public class SecurityKnockoutLevel implements Comparable<SecurityKnockoutLevel>
{
    public static final long DIVIDER = 1_000_000L;
    private static final String PROPERTY_PREFIX = "knockoutLevel."; //$NON-NLS-1$

    public static final class ByDate implements Comparator<SecurityKnockoutLevel>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SecurityKnockoutLevel l1, SecurityKnockoutLevel l2)
        {
            return l1.date.compareTo(l2.date);
        }
    }

    private LocalDate date;
    private long value;

    public SecurityKnockoutLevel()
    {
    }

    public SecurityKnockoutLevel(LocalDate date, long value)
    {
        this.date = Objects.requireNonNull(date);
        this.value = value;
    }

    public static SecurityKnockoutLevel of(LocalDate date, double level)
    {
        return new SecurityKnockoutLevel(date, Math.round(level * DIVIDER));
    }

    public LocalDate getDate()
    {
        return date;
    }

    public long getValue()
    {
        return value;
    }

    public double getLevel()
    {
        return value / (double) DIVIDER;
    }

    public static List<SecurityKnockoutLevel> getLevels(Security security)
    {
        List<SecurityKnockoutLevel> answer = new ArrayList<>();

        security.getProperties()
                        .filter(p -> p.getType() == SecurityProperty.Type.DERIVATIVE
                                        && p.getName().startsWith(PROPERTY_PREFIX))
                        .forEach(p -> {
                            try
                            {
                                LocalDate date = LocalDate.parse(p.getName().substring(PROPERTY_PREFIX.length()));
                                double level = Double.parseDouble(p.getValue());
                                answer.add(SecurityKnockoutLevel.of(date, level));
                            }
                            catch (RuntimeException ignore)
                            {
                                // Ignore malformed legacy/manual entries instead of breaking file loading.
                            }
                        });

        Collections.sort(answer);
        return Collections.unmodifiableList(answer);
    }

    public static Double getLevel(Security security, LocalDate requestedDate)
    {
        Objects.requireNonNull(requestedDate);

        List<SecurityKnockoutLevel> levels = getLevels(security);
        if (levels.isEmpty())
            return null;

        SecurityKnockoutLevel probe = new SecurityKnockoutLevel(requestedDate, 0);
        int index = Collections.binarySearch(levels, probe);

        if (index >= 0)
            return levels.get(index).getLevel();
        if (index == -1)
            return null;

        return levels.get(-index - 2).getLevel();
    }

    public static void replaceAll(Security security, List<SecurityKnockoutLevel> levels)
    {
        security.removePropertyIf(p -> p.getType() == SecurityProperty.Type.DERIVATIVE
                        && p.getName().startsWith(PROPERTY_PREFIX));

        for (SecurityKnockoutLevel level : levels)
        {
            security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, PROPERTY_PREFIX + level.getDate(),
                            Double.toString(level.getLevel()));
        }
    }

    @Override
    public int compareTo(SecurityKnockoutLevel other)
    {
        return date.compareTo(other.date);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(date, value);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof SecurityKnockoutLevel))
            return false;

        SecurityKnockoutLevel other = (SecurityKnockoutLevel) obj;
        return value == other.value && Objects.equals(date, other.date);
    }
}
