from pathlib import Path

path = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java')
text = path.read_text(encoding='utf-8')

old = '''        applyDateIfEmpty(firstTradingDay, result.get(FIRST_TRADING_DAY));
        applyDateIfEmpty(expirationDate, result.get(EXPIRATION_DATE));
        applyDateIfEmpty(lastTradingDay, result.get(LAST_TRADING_DAY));
        applyDateIfEmpty(settlementDate, result.get(SETTLEMENT_DATE));
'''
new = '''        applyDateIfEmpty(firstTradingDay, result.get(FIRST_TRADING_DAY));

        String expirationDefault = result.get(EXPIRATION_DATE);
        String lastTradingDefault = result.get(LAST_TRADING_DAY);
        String settlementDefault = result.get(SETTLEMENT_DATE);
        String sharedEndDateDefault = firstNonBlank(expirationDefault, lastTradingDefault, settlementDefault);

        applyDateIfEmpty(expirationDate,
                        expirationDefault != null && !expirationDefault.isBlank() ? expirationDefault : sharedEndDateDefault);
        applyDateIfEmpty(lastTradingDay,
                        lastTradingDefault != null && !lastTradingDefault.isBlank() ? lastTradingDefault : sharedEndDateDefault);
        applyDateIfEmpty(settlementDate,
                        settlementDefault != null && !settlementDefault.isBlank() ? settlementDefault : sharedEndDateDefault);
'''
if old not in text:
    raise SystemExit('date lookup block not found')
text = text.replace(old, new, 1)

marker = '''    private void applyDateIfEmpty(OptionalDateField field, String value)
    {
'''
helper = '''    private static String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (value != null && !value.isBlank())
                return value;
        }
        return null;
    }

'''
if marker not in text:
    raise SystemExit('applyDateIfEmpty marker not found')
text = text.replace(marker, helper + marker, 1)

old = '''        createAlignedLabel(optionGroup, "Strike");
        strike = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().hint(OPTION_FIELD_WIDTH, SWT.DEFAULT).applyTo(strike);
'''
new = '''        createAlignedLabel(optionGroup, "Strike");
        strike = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().hint(KO_DETAIL_FIELD_WIDTH, SWT.DEFAULT).applyTo(strike);
'''
if old not in text:
    raise SystemExit('strike width block not found')
text = text.replace(old, new, 1)

old = '''        optionGroup = new Group(container, SWT.NONE);
        optionGroup.setText("Option");
'''
new = '''        TabFolder derivativeDetailsTabs = new TabFolder(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(derivativeDetailsTabs);

        Composite optionTab = new Composite(derivativeDetailsTabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).applyTo(optionTab);
        TabItem optionTabItem = new TabItem(derivativeDetailsTabs, SWT.NONE);
        optionTabItem.setText("Option");
        optionTabItem.setControl(optionTab);

        Composite futureTab = new Composite(derivativeDetailsTabs, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).applyTo(futureTab);
        TabItem futureTabItem = new TabItem(derivativeDetailsTabs, SWT.NONE);
        futureTabItem.setText("Future");
        futureTabItem.setControl(futureTab);

        optionGroup = new Group(optionTab, SWT.NONE);
        optionGroup.setText("Option");
'''
if old not in text:
    raise SystemExit('option group parent block not found')
text = text.replace(old, new, 1)

old = '''        futureGroup = new Group(container, SWT.NONE);
        futureGroup.setText("Future");
'''
new = '''        futureGroup = new Group(futureTab, SWT.NONE);
        futureGroup.setText("Future");
'''
if old not in text:
    raise SystemExit('future group parent block not found')
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
