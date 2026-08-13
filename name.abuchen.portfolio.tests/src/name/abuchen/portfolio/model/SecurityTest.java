package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.Test;

import name.abuchen.portfolio.util.Pair;

@SuppressWarnings("nls")
public class SecurityTest
{

    @Test
    public void testThatDeepCopyIncludesAllProperties()
                    throws IntrospectionException, IllegalAccessException, InvocationTargetException
    {
        BeanInfo info = Introspector.getBeanInfo(Security.class);

        Security source = new Security();

        int skipped = 0;

        // set properties
        for (PropertyDescriptor p : info.getPropertyDescriptors())
        {
            if ("UUID".equals(p.getName())) //$NON-NLS-1$
                continue;

            if (p.getPropertyType() == String.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, UUID.randomUUID().toString());
            else if (p.getPropertyType() == boolean.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, true);
            else if (p.getPropertyType() == int.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, new Random().nextInt());
            else
                skipped++;
        }

        assertThat(skipped, equalTo(14));

        Security target = source.deepCopy();
        assertThat(target.getUUID(), not(equalTo(source.getUUID())));
        assertThat(target.getEphemeralData(), not(equalTo(source.getEphemeralData())));

        // compare
        for (PropertyDescriptor p : info.getPropertyDescriptors()) // NOSONAR
        {
            if ("UUID".equals(p.getName())) //$NON-NLS-1$
                continue;

            if (p.getPropertyType() != String.class && p.getPropertyType() != boolean.class
                            && p.getPropertyType() != int.class)
                continue;

            Object sourceValue = p.getReadMethod().invoke(source);
            Object targetValue = p.getReadMethod().invoke(target);

            assertThat(targetValue, equalTo(sourceValue));
        }
    }

    @Test
    public void testSetLatest()
    {
        Security security = new Security();
        assertThat(security.setLatest(null), is(false));

        LatestSecurityPrice latest = new LatestSecurityPrice(LocalDate.now(), 1);
        assertThat(security.setLatest(latest), is(true));
        assertThat(security.setLatest(latest), is(false));
        assertThat(security.setLatest(null), is(true));
        assertThat(security.setLatest(null), is(false));

        LatestSecurityPrice second = new LatestSecurityPrice(LocalDate.now(), 2);
        assertThat(security.setLatest(latest), is(true));
        assertThat(security.setLatest(second), is(true));
    }

    @Test
    public void testPrices()
    {
        Security security = new Security();

        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 1), 100)), is(true));
        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 2), 102)), is(true));
        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 2), 102)), is(false));
        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 2), 104)), is(true));
        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2014, 12, 31), 99)), is(true));

        assertThat(security.getPrices().size(), is(3));
        assertThat(security.getPrices().get(0).getValue(), is(99L));
        assertThat(security.getPrices().get(1).getValue(), is(100L));
        assertThat(security.getPrices().get(2).getValue(), is(104L));
    }

    @Test
    public void testAddAllPrices()
    {
        Security security = new Security();

        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 1), 100)), is(true));
        assertThat(security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 2), 102)), is(true));

        List<SecurityPrice> newPrices = List.of(new SecurityPrice(LocalDate.of(2015, 1, 1), 101),
                        new SecurityPrice(LocalDate.of(2015, 1, 2), 103),
                        new SecurityPrice(LocalDate.of(2015, 1, 3), 104));

        assertThat(security.addAllPrices(newPrices), is(true));
        assertThat(security.getPrices().size(), is(3));
        assertThat(security.getPrices().get(0).getValue(), is(100L));
        assertThat(security.getPrices().get(1).getValue(), is(103L));
        assertThat(security.getPrices().get(2).getValue(), is(104L));
    }

    @Test
    public void testGetSecurityPrice()
    {
        Security security = new Security();

        security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 1), 100));
        security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 3), 103));
        security.addPrice(new SecurityPrice(LocalDate.of(2015, 1, 5), 105));

        assertThat(security.getSecurityPrice(LocalDate.of(2014, 12, 31)).getValue(), is(100L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 1)).getValue(), is(100L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 2)).getValue(), is(100L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 3)).getValue(), is(103L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 4)).getValue(), is(103L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 5)).getValue(), is(105L));
        assertThat(security.getSecurityPrice(LocalDate.of(2015, 1, 6)).getValue(), is(105L));
    }

    @Test
    public void testLatestTwoSecurityPrices()
    {
        Security security = new Security();
        assertThat(security.getLatestTwoSecurityPrices(), is(Optional.empty()));

        LocalDate today = LocalDate.now();
        security.addPrice(new SecurityPrice(today.minusDays(2), 100));
        security.addPrice(new SecurityPrice(today.minusDays(1), 102));
        security.addPrice(new SecurityPrice(today, 104));

        Optional<Pair<SecurityPrice, SecurityPrice>> result = security.getLatestTwoSecurityPrices();
        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLeft().getValue(), is(104L));
        assertThat(result.get().getRight().getValue(), is(102L));
    }
}
