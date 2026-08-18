from pathlib import Path

p = Path('name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java')
s = p.read_text(encoding='utf-8')

old = '''        if (base.equals(quote))\n        {\n            MessageDialog.openError(getShell(), "Invalid currency pair",\n                            "Base currency and quote currency must be different.");\n            fxQuoteCurrency.setFocus();\n            fxQuoteCurrency.selectAll();\n            return false;\n        }\n\n        return true;\n    }\n'''
new = '''        if (base.equals(quote))\n        {\n            MessageDialog.openError(getShell(), "Invalid currency pair",\n                            "Base currency and quote currency must be different.");\n            fxQuoteCurrency.setFocus();\n            fxQuoteCurrency.selectAll();\n            return false;\n        }\n\n        String[] pair = parseFxPair(comboText(underlying));\n        if (pair != null && (!pair[0].equals(base) || !pair[1].equals(quote)))\n        {\n            MessageDialog.openError(getShell(), "Currency pair does not match underlying",\n                            "The FX underlying is " + pair[0] + "/" + pair[1]\n                                            + ". Use " + pair[0] + " as base currency and " + pair[1]\n                                            + " as quote currency.");\n            fxBaseCurrency.setFocus();\n            fxBaseCurrency.selectAll();\n            return false;\n        }\n\n        return true;\n    }\n'''
if old not in s:
    raise SystemExit('validation insertion point not found')
s = s.replace(old, new, 1)

old = '''    private static String normalizeDecimal(String value)\n    {\n        return value == null ? null : value.replace(',', '.');\n    }\n\n    private static String underlyingLabel(Security security)\n'''
new = '''    private static String normalizeDecimal(String value)\n    {\n        return value == null ? null : value.replace(',', '.');\n    }\n\n    private static String[] parseFxPair(String value)\n    {\n        if (value == null || value.isBlank())\n            return null;\n\n        var matcher = java.util.regex.Pattern.compile("(?i)([A-Z]{3})\\\\s*/\\\\s*([A-Z]{3})").matcher(value);\n        if (!matcher.find())\n            return null;\n\n        return new String[] { matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2).toUpperCase(Locale.ROOT) };\n    }\n\n    private static String underlyingLabel(Security security)\n'''
if old not in s:
    raise SystemExit('helper insertion point not found')
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
