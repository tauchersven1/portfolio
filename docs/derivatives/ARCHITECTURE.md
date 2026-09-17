# Derivatives & Exposure Management: architecture

## Status and scope

Bridge 19 / Build 111 is a **technical proof of concept** based on Portfolio Performance 0.87.0. It is not an official Portfolio Performance release and it is not yet a drop-in add-on for an unmodified Portfolio Performance installation.

The repository contains two related experiments:

1. The current integrated reference build on `agent/bridge19-0.87.0`. This is the complete demonstrator used for functional testing.
2. The earlier standalone add-on experiment on `agent/derivatives-addon-standalone`. This tests how features can be contributed through explicit extension points and a separate p2 update site.

Keeping these two tracks separate is important: Build 111 demonstrates the complete behavior, while the standalone branch demonstrates the intended modular direction.

## What changed in the core

Compared with upstream Portfolio Performance 0.87.0, the integrated reference build changes the following core areas.

### Data model and persistence

- A dated `SecurityMultiplier` series was added to `Security`; the default multiplier remains `1.0`.
- Multipliers are persisted in XML and protobuf (`PSecurityMultiplier`).
- `SecurityProperty.Type.DERIVATIVE` provides a namespace for optional derivative master data.
- Dated series were added for option delta and knock-out levels.
- Transaction price calculations use the multiplier that is valid on the transaction date.

### Valuation and calculations

- `DerivativePositionCalculator` supplies derivative-aware market values:
  - options: quantity × option price × multiplier;
  - futures: open/unrealized profit and loss based on the open position;
  - ordinary securities retain their standard valuation.
- `ExposureCalculator` separates market value, nominal exposure and delta-adjusted exposure.
- Underlyings can be resolved by stored UUID, name or ticker-symbol fallback.
- `TradingSymbolExposureGroup` maps a derivative to the trading symbol of its underlying. For example, `MSFT` and `MSFT260918C00490000` are grouped under `MSFT`.

### User interface and imports

- A derivatives area was added to the security editor for multipliers, deltas and contract master data.
- The statement of assets gained derivative and exposure columns.
- Exposure Management and exposure charts were added under Reports.
- Transaction and CSV processing were made multiplier-aware.
- Common OCC/US and selected IB/Eurex option symbols can prefill derivative master data.
- A provider API and a first Vontobel implementation can enrich selected knock-out products.

These changes are covered by focused tests in `name.abuchen.portfolio.tests`, including persistence, symbol parsing, valuation, FX knock-out exposure and trading-symbol grouping.

## Extension points explored

The standalone add-on branch introduces a small public bridge in `name.abuchen.portfolio.ui` instead of accessing internal navigation classes directly.

| Extension area | Prototype API | Purpose |
| --- | --- | --- |
| Report/navigation contribution | `name.abuchen.portfolio.ui.navigation` | Lets an OSGi bundle add report entries to Portfolio Performance navigation. |
| Navigation contributor | `NavigationExtension` | Supplies one or more add-on views. |
| View contract | `AddonView` | Defines title and control creation for an add-on-owned view. |
| Supported context | `AddonViewContext` | Gives the view supported access to the current client and UI context. |
| Host wrapper | `AddonFinanceView` / `AddonViewDescriptor` | Adapts an add-on view to Portfolio Performance's finance-view lifecycle. |

The extension declaration used by the prototype is:

```xml
<extension point="name.abuchen.portfolio.ui.navigation">
  <contributor class="de.venari.portfolio.derivatives.DerivativesNavigationExtension"/>
</extension>
```

The forum discussion identified additional extension points that are not yet complete in the standalone prototype:

- tabs or sections in the security editor;
- contributed columns in existing tables;
- valuation/exposure strategies per instrument;
- master-data providers and symbol resolvers;
- contributed CSV fields and validation;
- model-change notifications;
- lossless persistence of add-on data when the add-on is absent;
- installation, update and removal through a stable p2/update-site mechanism.

## What the add-on prototype does

The standalone bundle `de.venari.portfolio.derivatives` is independently built and packaged as an Eclipse/OSGi feature and p2 update site. It contributes Exposure reports through the navigation extension point and owns its UI and calculation code.

Its purpose is architectural validation, not feature parity with Build 111. It demonstrates that a separately packaged bundle can:

- register report views without patching Portfolio Performance navigation internals;
- receive a supported view context;
- calculate and display exposure information;
- store a simple add-on multiplier in generic security properties;
- be built as a separate feature/update site.

The full derivative master-data editor, complete persistence model, all valuation hooks and the entire Build-111 report set have not yet been migrated out of the integrated fork. That migration depends on the additional extension points listed above.

## Why both implementations exist

The integrated build answers the functional question: *Can Portfolio Performance model and display derivative exposure usefully?*

The standalone prototype answers the architectural question: *Which minimal APIs would let that functionality live outside the core and be installed separately?*

The intended next step is therefore not to treat the complete integrated fork as the final architecture. It is to use it as a working specification, stabilize the small generic core contracts, and move feature-specific code into the add-on incrementally.

## Key source locations

- Integrated calculations: `name.abuchen.portfolio/src/name/abuchen/portfolio/snapshot/`
- Symbol parsing: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/OptionSymbolParser.java`
- Integrated Exposure Management UI: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/ExposureManagementView.java`
- Derivative editor: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/wizards/security/SecurityMultiplierPage.java`
- Standalone experiment: branch `agent/derivatives-addon-standalone`, directory `derivatives-addon-standalone/`

