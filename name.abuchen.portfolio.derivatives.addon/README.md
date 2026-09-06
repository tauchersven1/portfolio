# Portfolio Performance Derivatives Add-on - PoC 0.1.0

This branch is based on the unchanged Portfolio Performance 0.87.0 release commit.

## Scope

The PoC deliberately contains only one add-on feature: a minimal Exposure Management view.

It does **not** modify Portfolio Performance's Security model, protobuf schema, transaction dialogs, derivative master-data mapping, or order entry.

## Multiplier

The multiplier belongs to the add-on. It is stored on the security as an existing Portfolio Performance `SecurityProperty.Type.FEED` property named:

`derivatives-addon.multiplier`

Default: `1`.

This avoids adding a multiplier field to the Portfolio Performance core model.

## Test

1. Start the PoC build and open a Portfolio Performance file.
2. Open **Tools -> Exposure Management (Add-on)**.
3. The table shows Instrument, Market value, Multiplier and Exposure.
4. Double-click a row and enter a multiplier greater than zero.
5. Exposure is recalculated as `market value * multiplier`.
6. Save the Portfolio Performance file, close it, reopen it and verify that the multiplier is retained.

This first calculation is intentionally simple. Options, futures, KO certificates, delta-adjusted exposure, underlyings, mappings and the previous full Exposure Management UI are follow-up steps after the add-on loading and persistence concept is proven.
