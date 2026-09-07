# Portfolio Performance Derivatives Add-on

Independent Eclipse/OSGi add-on for Portfolio Performance.

## PoC 0.1 scope
- separate installable feature
- separate p2 update site
- official Portfolio Performance update site as dependency
- Exposure Management entry under the existing **View / Ansicht** menu
- add-on-owned multiplier stored through Portfolio Performance's generic security properties
- simple PoC exposure formula: market value × multiplier

No Portfolio Performance core classes, protobuf schema, or application model files are modified.

Build:
```
mvn -f derivatives-addon-standalone/pom.xml clean verify
```

The generated p2 repository is created in:
`de.venari.portfolio.derivatives.updatesite/target/repository`
