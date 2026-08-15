from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Expected text not found in {path}: {old[:160]!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')

viewer = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/StatementOfAssetsViewer.java')
text = viewer.read_text(encoding='utf-8')
text = text.replace(
    '    private AbstractFinanceView owner;\n    private ShowHideColumnHelper support;\n\n    private final Client client;',
    '    private AbstractFinanceView owner;\n    private ShowHideColumnHelper support;\n    private final List<Column> additionalColumns = new ArrayList<>();\n\n    private final Client client;',
    1)
text = text.replace(
    '    public Control createControl(Composite parent, boolean isConfigurable)\n    {\n        Control control = createColumns(parent, isConfigurable);',
    '    public void addColumn(Column column)\n    {\n        if (support == null)\n            additionalColumns.add(column);\n        else\n            support.addColumn(column);\n    }\n\n    public Control createControl(Composite parent, boolean isConfigurable)\n    {\n        Control control = createColumns(parent, isConfigurable);',
    1)
text = text.replace(
    '        column = new QuoteRangeColumn(LocalDate::now,\n                        owner.getPart().getReportingPeriods().stream().collect(toMutableList()));\n        column.getSorter().wrap(ElementComparator::new);\n        support.addColumn(column);\n\n        support.createColumns(isConfigurable);',
    '        column = new QuoteRangeColumn(LocalDate::now,\n                        owner.getPart().getReportingPeriods().stream().collect(toMutableList()));\n        column.getSorter().wrap(ElementComparator::new);\n        support.addColumn(column);\n\n        // Register view-specific columns before restoring the persisted configuration\n        // so their stable IDs can be resolved in a single pass.\n        additionalColumns.forEach(support::addColumn);\n\n        support.createColumns(isConfigurable);',
    1)
viewer.write_text(text, encoding='utf-8')

view = 'name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/StatementOfAssetsView.java'
replace_once(
    view,
    '''        assetViewer = make(StatementOfAssetsViewer.class);\n        Control control = assetViewer.createControl(parent, true);\n\n        ExposureColumn exposureColumn = new ExposureColumn(getClient(), () -> currentSnapshotDate,\n                        () -> currentConverter, () -> null);\n        exposureColumn.setGroupLabel(DERIVATIVES_GROUP);\n        exposureColumn.setVisible(true);\n        assetViewer.getColumnHelper().addColumn(exposureColumn);\n\n        addDerivativeContractColumns();\n        assetViewer.getColumnHelper().addColumn(createPutCallColumn());\n\n        // createControl() restores the column configuration before the custom derivative\n        // columns are registered. Re-apply it now so persisted derivative columns are\n        // resolved by their stable IDs as well.\n        assetViewer.getColumnHelper().createColumns();\n\n        assetViewer.setToolBarManager(getViewToolBarManager());''',
    '''        assetViewer = make(StatementOfAssetsViewer.class);\n\n        ExposureColumn exposureColumn = new ExposureColumn(getClient(), () -> currentSnapshotDate,\n                        () -> currentConverter, () -> null);\n        exposureColumn.setGroupLabel(DERIVATIVES_GROUP);\n        exposureColumn.setVisible(true);\n        assetViewer.addColumn(exposureColumn);\n\n        addDerivativeContractColumns();\n        assetViewer.addColumn(createPutCallColumn());\n\n        Control control = assetViewer.createControl(parent, true);\n        assetViewer.setToolBarManager(getViewToolBarManager());''')

p = Path(view)
text = p.read_text(encoding='utf-8').replace('assetViewer.getColumnHelper().addColumn(createDerivativePropertyColumn(', 'assetViewer.addColumn(createDerivativePropertyColumn(')
p.write_text(text, encoding='utf-8')

print('Fixed derivative column pre-registration')
