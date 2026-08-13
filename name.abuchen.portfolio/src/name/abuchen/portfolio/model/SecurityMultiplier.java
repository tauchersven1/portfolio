package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * A time-dependent scalar multiplier attached to a security. A multiplier is
 * valid from its date until it is superseded by a later entry.
 */
public class SecurityMultiplier implements Comparable<SecurityMultiplier>
{
    public static final long DIVIDER = 1_000_000L;

    public static final class ByDate implements Comparator<SecurityMultiplier>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SecurityMultiplier m1, SecurityMultiplier m2)
        {
            return m1.date.compareTo(m2.date);
        }
    }

    private LocalDate date;
    private long value;

    public SecurityMultiplier()
    {
    }

    public SecurityMultiplier(LocalDate date, long value)
    {
        this.date = Objects.requireNonNull(date);
        this.value = value;
    }

    public static SecurityMultiplier of(LocalDate date, double multiplier)
    {
        return new SecurityMultiplier(date, Math.round(multiplier * DIVIDER));
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = Objects.requireNonNull(date);
    }

    public long getValue()
    {
        return value;
    }

    public void setValue(long value)
    {
        this.value = value;
    }

    public double getMultiplier()
    {
        return value / (double) DIVIDER;
    }

    @Override
    public int compareTo(SecurityMultiplier other)
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
        if (!(obj instanceof SecurityMultiplier))
            return false;

        SecurityMultiplier other = (SecurityMultiplier) obj;
        return value == other.value && Objects.equals(date, other.date);
    }
}