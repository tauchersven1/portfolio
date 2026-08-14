from pathlib import Path

path = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/editor/Navigation.java')
text = path.read_text()

old = 'import name.abuchen.portfolio.ui.views.StatementOfAssetsHistoryView;\nimport name.abuchen.portfolio.ui.views.StatementOfAssetsView;'
new = 'import name.abuchen.portfolio.ui.views.ExposureManagementView;\nimport name.abuchen.portfolio.ui.views.StatementOfAssetsHistoryView;\nimport name.abuchen.portfolio.ui.views.StatementOfAssetsView;'
assert old in text
text = text.replace(old, new, 1)

old = '''        statementOfAssets.add(new Item(Messages.ClientEditorLabelChart, StatementOfAssetsHistoryView.class, true));
        statementOfAssets.add(new Item(Messages.ClientEditorLabelHoldings, HoldingsPieChartView.class, true));'''
new = '''        statementOfAssets.add(new Item(Messages.ClientEditorLabelChart, StatementOfAssetsHistoryView.class, true));
        statementOfAssets.add(new Item(Messages.ClientEditorLabelHoldings, HoldingsPieChartView.class, true));
        statementOfAssets.add(new Item("Exposuremanagement", ExposureManagementView.class, true)); //$NON-NLS-1$'''
assert old in text
text = text.replace(old, new, 1)

path.write_text(text)
