package name.abuchen.portfolio.ui;

/**
 * Public extension contract for adding sections and views to Portfolio Performance navigation.
 */
@FunctionalInterface
public interface NavigationExtension
{
    void contribute(NavigationExtensionRegistry registry);
}
