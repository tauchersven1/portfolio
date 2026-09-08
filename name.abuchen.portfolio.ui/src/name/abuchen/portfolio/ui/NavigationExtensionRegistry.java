package name.abuchen.portfolio.ui;

/**
 * Registry presented to external navigation extensions.
 */
public interface NavigationExtensionRegistry
{
    String SECTION_SECURITIES = "securities"; //$NON-NLS-1$
    String SECTION_MASTER_DATA = "master-data"; //$NON-NLS-1$
    String SECTION_REPORTS = "reports"; //$NON-NLS-1$
    String SECTION_TAXONOMIES = "taxonomies"; //$NON-NLS-1$
    String SECTION_GENERAL_DATA = "general-data"; //$NON-NLS-1$

    void addSection(String id, String label);

    void addView(String sectionId, String id, String label, Class<? extends AddonView> viewClass);
}
