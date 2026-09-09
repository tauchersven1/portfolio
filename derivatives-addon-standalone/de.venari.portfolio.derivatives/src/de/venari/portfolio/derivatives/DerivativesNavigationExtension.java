package de.venari.portfolio.derivatives;

import name.abuchen.portfolio.ui.NavigationExtension;
import name.abuchen.portfolio.ui.NavigationExtensionRegistry;

public class DerivativesNavigationExtension implements NavigationExtension
{
    @Override
    public void contribute(NavigationExtensionRegistry registry)
    {
        registry.addSection("derivatives", "Derivate"); //$NON-NLS-1$ //$NON-NLS-2$
        registry.addView("derivatives", "exposure-management", "Exposure Management", ExposureManagementView.class); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        registry.addView(NavigationExtensionRegistry.SECTION_REPORTS, "long-short-exposure-report", //$NON-NLS-1$
                        "Bestand - Long / Short Exposure", LongShortExposureReportView.class); //$NON-NLS-1$
        registry.addView(NavigationExtensionRegistry.SECTION_REPORTS, "total-exposure-report", //$NON-NLS-1$
                        "Bestand - Gesamtexposure", ExposureReportView.class); //$NON-NLS-1$
    }
}
