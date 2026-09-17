# Testing Bridge 19 / Build 111

## Download

Use the Windows x86_64 ZIP from the GitHub release. It includes a Java runtime. Extract the complete archive to a new directory and start `PortfolioPerformance.exe`.

Do not install it over an existing Portfolio Performance installation. This is an experimental reference build.

## Demo portfolio

Open `examples/Derivatives_AddIn_Demo.xml`. The file contains synthetic demonstration data only.

For the trading-symbol grouping test, open:

`Reports → Statement of Assets → Exposuremanagement → Exposure by Trading Symbol`

The following two instruments should appear in the same `MSFT` group:

- Microsoft stock with ticker `MSFT`
- option `MSFT260918C00490000`

The grouping first resolves an explicitly linked underlying. If that is unavailable, it parses the option contract symbol and looks for one unique non-derivative security with the corresponding ticker.

## Suggested checks

1. Switch between Market Value, Nominal and Delta Adjusted.
2. Compare gross, net, long and short exposure.
3. Inspect grouping by underlying, trading symbol and maturity.
4. Open the Microsoft stock and option master data.
5. Change a filter and confirm that KPIs and charts refresh together.

Use a copy of any real portfolio file. The proof of concept changes the data model and should not be your only way to open an important portfolio.

