from pathlib import Path

p = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java')
s = p.read_text(encoding='utf-8')

replacements = [
(
'''    private void setKnockoutLevelDate(LocalDate date)\n    {\n        if (knockoutLevelEffectiveDate != null)\n            knockoutLevelEffectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());\n    }\n\n    public void applyChanges()\n    {\n''',
'''    private void setKnockoutLevelDate(LocalDate date)\n    {\n        if (knockoutLevelEffectiveDate != null)\n            knockoutLevelEffectiveDate.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());\n    }\n\n    private boolean validateKnockoutMasterData()\n    {\n        boolean isKnockout = derivativeType.getSelectionIndex() == 2 && optionProductType.getSelectionIndex() == 2;\n        if (!isKnockout)\n            return true;\n\n        String ratio = text(subscriptionRatio);\n        if (fxUnderlying.getSelection() && ratio == null)\n        {\n            MessageDialog.openError(getShell(), "Missing subscription ratio",\n                            "Enter a positive subscription ratio for an FX K.O. certificate.");\n            subscriptionRatio.setFocus();\n            return false;\n        }\n\n        if (ratio != null)\n        {\n            try\n            {\n                double value = Double.parseDouble(ratio.replace(',', '.'));\n                if (!Double.isFinite(value) || value <= 0)\n                    throw new NumberFormatException();\n            }\n            catch (NumberFormatException e)\n            {\n                MessageDialog.openError(getShell(), "Invalid subscription ratio",\n                                "Enter a positive numeric subscription ratio.");\n                subscriptionRatio.setFocus();\n                subscriptionRatio.selectAll();\n                return false;\n            }\n        }\n\n        if (!fxUnderlying.getSelection())\n            return true;\n\n        String base = upper(text(fxBaseCurrency));\n        String quote = upper(text(fxQuoteCurrency));\n        if (base == null || !base.matches("[A-Z]{3}"))\n        {\n            MessageDialog.openError(getShell(), "Invalid base currency",\n                            "Enter a three-letter currency code such as EUR.");\n            fxBaseCurrency.setFocus();\n            fxBaseCurrency.selectAll();\n            return false;\n        }\n        if (quote == null || !quote.matches("[A-Z]{3}"))\n        {\n            MessageDialog.openError(getShell(), "Invalid quote currency",\n                            "Enter a three-letter currency code such as JPY.");\n            fxQuoteCurrency.setFocus();\n            fxQuoteCurrency.selectAll();\n            return false;\n        }\n        if (base.equals(quote))\n        {\n            MessageDialog.openError(getShell(), "Invalid currency pair",\n                            "Base currency and quote currency must be different.");\n            fxQuoteCurrency.setFocus();\n            fxQuoteCurrency.selectAll();\n            return false;\n        }\n\n        return true;\n    }\n\n    public void applyChanges()\n    {\n        if (!validateKnockoutMasterData())\n            return;\n'''
),
(
'''            setProperty(SUBSCRIPTION_RATIO, isKnockout ? text(subscriptionRatio) : null);\n''',
'''            setProperty(SUBSCRIPTION_RATIO, isKnockout ? normalizeDecimal(text(subscriptionRatio)) : null);\n'''
),
(
'''    private static String upper(String value)\n    {\n        return value == null ? null : value.toUpperCase(Locale.ROOT);\n    }\n\n    private static String underlyingLabel(Security security)\n''',
'''    private static String upper(String value)\n    {\n        return value == null ? null : value.toUpperCase(Locale.ROOT);\n    }\n\n    private static String normalizeDecimal(String value)\n    {\n        return value == null ? null : value.replace(',', '.');\n    }\n\n    private static String underlyingLabel(Security security)\n'''
)
]

for old, new in replacements:
    if old not in s:
        raise SystemExit(f'Expected block not found:\n{old}')
    s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
