from pathlib import Path

path = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java')
text = path.read_text()

old = '''    private LocalDate valuationDate = LocalDate.now();
    private CurrencyConverter converter;
    private List<ExposureRow> rows = List.of();'''
new = '''    private LocalDate valuationDate = LocalDate.now();
    private CurrencyConverter converter;
    private ExposureType currentExposureType = ExposureType.DELTA_ADJUSTED;
    private List<ExposureRow> rows = List.of();'''
assert old in text
text = text.replace(old, new, 1)

old = '''    private void refreshRows(ClientSnapshot snapshot)
    {
        ExposureType type = selectedExposureType();
        List<ExposureRow> answer = new ArrayList<>();'''
new = '''    private void refreshRows(ClientSnapshot snapshot)
    {
        ExposureType type = selectedExposureType();
        currentExposureType = type;
        List<ExposureRow> answer = new ArrayList<>();'''
assert old in text
text = text.replace(old, new, 1)

old = '''        // Currency changes require a fresh snapshot and conversion.
        if (converter == null || !converter.getTermCurrency().equals(currency.getText()))
        {
            notifyModelUpdated();
            return;
        }'''
new = '''        // Exposure type and currency changes require a fresh snapshot/calculation.
        if (selectedExposureType() != currentExposureType || converter == null
                        || !converter.getTermCurrency().equals(currency.getText()))
        {
            notifyModelUpdated();
            return;
        }'''
assert old in text
text = text.replace(old, new, 1)

path.write_text(text)
