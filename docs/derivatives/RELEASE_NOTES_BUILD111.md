# Bridge 19 / Build 111

Experimental Windows reference build for derivative and exposure management on Portfolio Performance 0.87.0.

This release contains the complete integrated feature state developed through Build 111. It is not the earlier, functionally smaller standalone add-on prototype.

## Highlights

- Derivative master data for options, futures and knock-out certificates
- Dated multiplier, delta and knock-out level series
- Multiplier-aware transactions, valuation and historical calculations
- Market value, nominal exposure and delta-adjusted exposure
- Gross, net, long and short exposure KPIs
- Exposure columns in the statement of assets
- Exposure charts and grouping by underlying, trading symbol and maturity
- OCC/US and selected IB/Eurex symbol parsing
- Knock-out and FX exposure use cases
- Vontobel master-data provider proof of concept
- `MSFT` stock and `MSFT260918C00490000` option grouped together under `MSFT`

## Files

- `PortfolioPerformance-Bridge19-Windows-x86_64-Build111.zip`: complete integrated Windows application with bundled Java runtime
- `Derivatives_AddIn_Demo.xml`: synthetic demo portfolio
- `SHA256SUMS.txt`: checksum for the Windows ZIP

## Reproducibility

- Portfolio Performance basis: 0.87.0
- Feature build: 111
- Executable source commit: `ed48abef2259335ab4759138bdf6d6e2ff32e363`
- GitHub Actions run: <https://github.com/tauchersven1/portfolio/actions/runs/35206476942>

This is a technical proof of concept, not an official Portfolio Performance release. Back up portfolio files before testing.
