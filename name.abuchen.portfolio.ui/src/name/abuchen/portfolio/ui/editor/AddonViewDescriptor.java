package name.abuchen.portfolio.ui.editor;

import name.abuchen.portfolio.ui.AddonView;

record AddonViewDescriptor(String id, String label, Class<? extends AddonView> viewClass)
{
}
