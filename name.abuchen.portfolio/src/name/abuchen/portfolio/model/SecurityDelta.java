package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A time-dependent option delta attached to a security. A delta is valid from
 * its date until it is superseded by a later entry. If no entry exists for a
 * requested date, calls default to +1.0 and puts to -1.0.
 */
public class SecurityDelta implements Comparable<SecurityDelta>
{
    public static final long DIVIDER = 1_000_000L;
    private static final String PROPERTY_PREFIX = "delta."; //$NON-NLS-1$
    private static final String DERIVATIVE_TYPE = "type"; //$NON-NLS-1$
    private static final String OPTION = "OPTION"; //$NON-NLS-1$
    private static final String PUT_CALL = "putCall"; //$NON-NLS-1$
    private static final String PUT = "PUT"; //$NON-NLS-1$

    public static final class ByDate implements Comparator<SecurityDelta>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SecurityDelta d1, SecurityDelta d2)
        {
            return d1.date.compareTo(d2.date);
        }
    }

    private LocalDate date;
    private long value;

    public SecurityDelta()
    {
    }

    public SecurityDelta(LocalDate date, long value)
    {
        this.date = Objects.requireNonNull(date);
        this.value = value;
    }

    public static SecurityDelta of(LocalDate date, double delta)
    {
        return new SecurityDelta(date, Math.round(delta * DIVIDER));
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

    public double getDelta()
    {
        return value / (double) DIVIDER;
    }

    public static List<SecurityDelta> getDeltas(Security security)
    {
        List<SecurityDelta> answer = new ArrayList<>();

        security.getProperties()
                        .filter(p -> p.getType() == SecurityProperty.Type.DERIVATIVE
                                        && p.getName().startsWith(PROPERTY_PREFIX))
                        .forEach(p -> {
                            try
                            {
                                LocalDate date = LocalDate.parse(p.getName().substring(PROPERTY_PREFIX.length()));
                                double delta = Double.parseDouble(p.getValue());
                                answer.add(SecurityDelta.of(date, delta));
                            }
                            catch (RuntimeException ignore)
                            {
                                // Ignore malformed legacy/manual entries instead of breaking file loading.
                            }
                        });

        Collections.sort(answer);
        return Collections.unmodifiableList(answer);
    }

    public static double getDelta(Security security, LocalDate requestedDate)
    {
        Objects.requireNonNull(requestedDate);

        List<SecurityDelta> deltas = getDeltas(security);
        if (deltas.isEmpty())
            return getDefaultDelta(security);

        SecurityDelta probe = new SecurityDelta(requestedDate, 0);
        int index = Collections.binarySearch(deltas, probe);

        if (index >= 0)
            return deltas.get(index).getDelta();
        if (index == -1)
            return getDefaultDelta(security);

        return deltas.get(-index - 2).getDelta();
    }

    public static double getDefaultDelta(Security security)
    {
        boolean option = OPTION.equals(security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, DERIVATIVE_TYPE)
                        .orElse(null));
        boolean put = PUT.equals(
                        security.getPropertyValue(SecurityProperty.Type.DERIVATIVE, PUT_CALL).orElse(null));
        return option && put ? -1.0 : 1.0;
    }

    public static void replaceAll(Security security, List<SecurityDelta> deltas)
    {
        security.removePropertyIf(p -> p.getType() == SecurityProperty.Type.DERIVATIVE
                        && p.getName().startsWith(PROPERTY_PREFIX));

        for (SecurityDelta delta : deltas)
        {
            security.setPropertyValue(SecurityProperty.Type.DERIVATIVE, PROPERTY_PREFIX + delta.getDate(),
                            Double.toString(delta.getDelta()));
        }
    }

    @Override
    public int compareTo(SecurityDelta other)
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
        if (!(obj instanceof SecurityDelta))
            return false;

        SecurityDelta other = (SecurityDelta) obj;
        return value == other.value && Objects.equals(date, other.date);
    }
}
