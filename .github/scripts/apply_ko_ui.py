from pathlib import Path

p = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java')
s = p.read_text(encoding='utf-8')

replacements = []

replacements.append((
'''    private static final String INITIAL_KNOCKOUT_LEVEL = "initialKnockoutLevel";\n''',
'''    private static final String INITIAL_KNOCKOUT_LEVEL = "initialKnockoutLevel";\n    private static final String ISSUER = "issuer";\n    private static final String ISSUER_PRODUCT_ID = "issuerProductId";\n    private static final String SUBSCRIPTION_RATIO = "subscriptionRatio";\n    private static final String FX_UNDERLYING = "fxUnderlying";\n    private static final String FX_BASE_CURRENCY = "fxBaseCurrency";\n    private static final String FX_QUOTE_CURRENCY = "fxQuoteCurrency";\n'''))

replacements.append((
'''                    CONTRACT_SYMBOL, CONTRACT_SIZE, TICK_SIZE, PUT_CALL, STRIKE, EXERCISE_STYLE, OPTION_PRODUCT_TYPE,\n                    INITIAL_KNOCKOUT_LEVEL, CONTRACT_MONTH, FIRST_NOTICE_DAY, FINAL_SETTLEMENT_DATE };\n''',
'''                    CONTRACT_SYMBOL, CONTRACT_SIZE, TICK_SIZE, PUT_CALL, STRIKE, EXERCISE_STYLE, OPTION_PRODUCT_TYPE,\n                    INITIAL_KNOCKOUT_LEVEL, ISSUER, ISSUER_PRODUCT_ID, SUBSCRIPTION_RATIO, FX_UNDERLYING,\n                    FX_BASE_CURRENCY, FX_QUOTE_CURRENCY, CONTRACT_MONTH, FIRST_NOTICE_DAY, FINAL_SETTLEMENT_DATE };\n'''))

replacements.append((
'''    private Label initialKnockoutLevelLabel;\n    private Text initialKnockoutLevel;\n''',
'''    private Label initialKnockoutLevelLabel;\n    private Text initialKnockoutLevel;\n    private Group knockoutDetailsGroup;\n    private Text issuer;\n    private Text issuerProductId;\n    private Text subscriptionRatio;\n    private Button fxUnderlying;\n    private Text fxBaseCurrency;\n    private Text fxQuoteCurrency;\n'''))

replacements.append((
'''        initialKnockoutLevel = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);\n        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(initialKnockoutLevel);\n\n        futureGroup = new Group(container, SWT.NONE);\n''',
'''        initialKnockoutLevel = new Text(optionGroup, SWT.BORDER | SWT.RIGHT);\n        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(initialKnockoutLevel);\n\n        knockoutDetailsGroup = new Group(optionGroup, SWT.NONE);\n        knockoutDetailsGroup.setText("K.O. certificate details");\n        GridLayoutFactory.fillDefaults().numColumns(4).margins(8, 8).spacing(8, 6).applyTo(knockoutDetailsGroup);\n        GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(knockoutDetailsGroup);\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("Issuer");\n        issuer = new Text(knockoutDetailsGroup, SWT.BORDER);\n        GridDataFactory.fillDefaults().grab(true, false).applyTo(issuer);\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("Issuer product ID");\n        issuerProductId = new Text(knockoutDetailsGroup, SWT.BORDER);\n        GridDataFactory.fillDefaults().grab(true, false).applyTo(issuerProductId);\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("Subscription ratio");\n        subscriptionRatio = new Text(knockoutDetailsGroup, SWT.BORDER | SWT.RIGHT);\n        subscriptionRatio.setToolTipText("Base units represented by one certificate, e.g. 100 for a EUR/JPY turbo representing 100 EUR.");\n        GridDataFactory.fillDefaults().hint(120, SWT.DEFAULT).applyTo(subscriptionRatio);\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("FX Underlying");\n        fxUnderlying = new Button(knockoutDetailsGroup, SWT.CHECK);\n        fxUnderlying.setText("Currency pair");\n        fxUnderlying.addSelectionListener(new SelectionAdapter()\n        {\n            @Override\n            public void widgetSelected(SelectionEvent e)\n            {\n                updateFxUnderlyingControls();\n            }\n        });\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("Base currency");\n        fxBaseCurrency = new Text(knockoutDetailsGroup, SWT.BORDER);\n        fxBaseCurrency.setTextLimit(3);\n        fxBaseCurrency.setToolTipText("ISO currency code of the base currency, e.g. EUR in EUR/JPY.");\n        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(fxBaseCurrency);\n\n        new Label(knockoutDetailsGroup, SWT.NONE).setText("Quote currency");\n        fxQuoteCurrency = new Text(knockoutDetailsGroup, SWT.BORDER);\n        fxQuoteCurrency.setTextLimit(3);\n        fxQuoteCurrency.setToolTipText("ISO currency code of the quote currency, e.g. JPY in EUR/JPY.");\n        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(fxQuoteCurrency);\n\n        futureGroup = new Group(container, SWT.NONE);\n'''))

replacements.append((
'''        strike.setText(valueOrEmpty(property(STRIKE)));\n        initialKnockoutLevel.setText(valueOrEmpty(property(INITIAL_KNOCKOUT_LEVEL)));\n        contractMonth.setText(valueOrEmpty(property(CONTRACT_MONTH)));\n''',
'''        strike.setText(valueOrEmpty(property(STRIKE)));\n        initialKnockoutLevel.setText(valueOrEmpty(property(INITIAL_KNOCKOUT_LEVEL)));\n        issuer.setText(valueOrEmpty(property(ISSUER)));\n        issuerProductId.setText(valueOrEmpty(property(ISSUER_PRODUCT_ID)));\n        subscriptionRatio.setText(valueOrEmpty(property(SUBSCRIPTION_RATIO)));\n        fxUnderlying.setSelection("true".equalsIgnoreCase(property(FX_UNDERLYING)));\n        fxBaseCurrency.setText(valueOrEmpty(property(FX_BASE_CURRENCY)));\n        fxQuoteCurrency.setText(valueOrEmpty(property(FX_QUOTE_CURRENCY)));\n        contractMonth.setText(valueOrEmpty(property(CONTRACT_MONTH)));\n'''))

replacements.append((
'''        if (knockoutLevelGroup != null)\n            setEnabledRecursive(knockoutLevelGroup, isKnockout);\n    }\n\n    private void updateDeltaDefaultForPutCall()\n''',
'''        if (knockoutDetailsGroup != null)\n            setEnabledRecursive(knockoutDetailsGroup, isKnockout);\n        if (knockoutLevelGroup != null)\n            setEnabledRecursive(knockoutLevelGroup, isKnockout);\n\n        updateFxUnderlyingControls();\n    }\n\n    private void updateFxUnderlyingControls()\n    {\n        boolean isKnockout = derivativeType != null && derivativeType.getSelectionIndex() == 2\n                        && optionProductType != null && optionProductType.getSelectionIndex() == 2;\n        boolean enabled = isKnockout && fxUnderlying != null && fxUnderlying.getSelection();\n        if (fxBaseCurrency != null)\n            fxBaseCurrency.setEnabled(enabled);\n        if (fxQuoteCurrency != null)\n            fxQuoteCurrency.setEnabled(enabled);\n    }\n\n    private void updateDeltaDefaultForPutCall()\n'''))

replacements.append((
'''            boolean isKnockout = optionProductType.getSelectionIndex() == 2;\n            setProperty(INITIAL_KNOCKOUT_LEVEL, isKnockout ? text(initialKnockoutLevel) : null);\n            SecurityKnockoutLevel.replaceAll(security, isKnockout ? knockoutLevels : Collections.emptyList());\n\n            setProperty(CONTRACT_MONTH, null);\n''',
'''            boolean isKnockout = optionProductType.getSelectionIndex() == 2;\n            setProperty(INITIAL_KNOCKOUT_LEVEL, isKnockout ? text(initialKnockoutLevel) : null);\n            setProperty(ISSUER, isKnockout ? text(issuer) : null);\n            setProperty(ISSUER_PRODUCT_ID, isKnockout ? text(issuerProductId) : null);\n            setProperty(SUBSCRIPTION_RATIO, isKnockout ? text(subscriptionRatio) : null);\n\n            boolean isFxUnderlying = isKnockout && fxUnderlying.getSelection();\n            setProperty(FX_UNDERLYING, isFxUnderlying ? "true" : null);\n            setProperty(FX_BASE_CURRENCY, isFxUnderlying ? upper(text(fxBaseCurrency)) : null);\n            setProperty(FX_QUOTE_CURRENCY, isFxUnderlying ? upper(text(fxQuoteCurrency)) : null);\n            SecurityKnockoutLevel.replaceAll(security, isKnockout ? knockoutLevels : Collections.emptyList());\n\n            setProperty(CONTRACT_MONTH, null);\n'''))

replacements.append((
'''            setProperty(OPTION_PRODUCT_TYPE, null);\n            setProperty(INITIAL_KNOCKOUT_LEVEL, null);\n            SecurityKnockoutLevel.replaceAll(security, Collections.emptyList());\n''',
'''            setProperty(OPTION_PRODUCT_TYPE, null);\n            setProperty(INITIAL_KNOCKOUT_LEVEL, null);\n            setProperty(ISSUER, null);\n            setProperty(ISSUER_PRODUCT_ID, null);\n            setProperty(SUBSCRIPTION_RATIO, null);\n            setProperty(FX_UNDERLYING, null);\n            setProperty(FX_BASE_CURRENCY, null);\n            setProperty(FX_QUOTE_CURRENCY, null);\n            SecurityKnockoutLevel.replaceAll(security, Collections.emptyList());\n'''))

replacements.append((
'''    private static String comboText(Combo control)\n    {\n        String value = control.getText().trim();\n        return value.isEmpty() ? null : value;\n    }\n\n    private static String underlyingLabel(Security security)\n''',
'''    private static String comboText(Combo control)\n    {\n        String value = control.getText().trim();\n        return value.isEmpty() ? null : value;\n    }\n\n    private static String upper(String value)\n    {\n        return value == null ? null : value.toUpperCase(Locale.ROOT);\n    }\n\n    private static String underlyingLabel(Security security)\n'''))

for old, new in replacements:
    if old not in s:
        raise SystemExit(f'Expected block not found:\n{old}')
    s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
