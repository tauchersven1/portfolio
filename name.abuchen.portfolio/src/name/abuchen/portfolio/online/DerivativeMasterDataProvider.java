package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;

/**
 * Provider API for enriching derivative master data from an external source.
 * Providers return suggested values only; applying them without overwriting
 * existing user data is the responsibility of the UI.
 */
public interface DerivativeMasterDataProvider
{
    public static final class Result
    {
        private final Map<String, String> values = new LinkedHashMap<>();

        public Result put(String key, String value)
        {
            if (key != null && value != null && !value.isBlank())
                values.put(key, value.trim());
            return this;
        }

        public String get(String key)
        {
            return values.get(key);
        }

        public Map<String, String> values()
        {
            return Collections.unmodifiableMap(values);
        }

        public boolean isEmpty()
        {
            return values.isEmpty();
        }
    }

    String getName();

    Optional<Result> lookup(Security security) throws IOException;
}
