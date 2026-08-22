# About

[Portfolio Performance](https://www.portfolio-performance.info): Track and evaluate the performance of your investment portfolio across stocks, cryptocurrencies, and other assets.

## Derivatives & Exposure Management Proof of Concept

This fork contains an experimental extension for derivatives and portfolio exposure analysis. The work started from the need to answer a question that market-value views alone cannot fully address: **what economic market exposure does a portfolio actually carry?**

The proof of concept currently includes, among other things:

- nominal and delta-adjusted exposure calculations
- long/short, gross and net exposure views
- exposure aggregation by underlying, trading symbol and maturity
- derivative master data for options and futures
- support for knock-out certificates, including current/initial knock-out levels and subscription ratios
- derivative-aware market-value calculations
- option-symbol parsing for common US option symbols and selected Interactive Brokers / Eurex symbols
- derivative master-data lookup support, currently including Vontobel knock-out products

The implementation is intended as a **technical reference and discussion basis**, not as a finished upstream pull request. Data model, UI and calculation details may be adapted to the architecture and conventions of the upstream Portfolio Performance project.

### Reproducible reference build

- **Reference build:** Feature Build #84
- **Build status:** success
- **Commit:** [`6dd92ff372feab24302442006e641a7acaf9a15e`](https://github.com/tauchersven1/portfolio/commit/6dd92ff372feab24302442006e641a7acaf9a15e)
- **Stable snapshot branch:** [`poc/derivatives-build84`](https://github.com/tauchersven1/portfolio/tree/poc/derivatives-build84)
- **Development branch:** [`agent/derivatives-official-release`](https://github.com/tauchersven1/portfolio/tree/agent/derivatives-official-release)

The derivatives work on the development branch is based on Portfolio Performance 0.87.0. A small synthetic Portfolio Performance XML demo portfolio is available for reproducing the derivative and exposure examples; it contains only demonstration data and no personal portfolio information.

## Status

[![Build Status](https://github.com/portfolio-performance/portfolio/workflows/CI/badge.svg)](https://github.com/portfolio-performance/portfolio/actions?query=workflow%3ACI) [![Latest Release](https://img.shields.io/github/release/buchen/portfolio.svg)](https://github.com/portfolio-performance/portfolio/releases/latest) [![Release Date](https://img.shields.io/github/release-date/buchen/portfolio?color=blue)](https://github.com/portfolio-performance/portfolio/releases/latest) [![License](https://img.shields.io/github/license/buchen/portfolio.svg)](https://github.com/portfolio-performance/portfolio/blob/master/LICENSE)

[![LOC](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=ncloc)](https://sonarcloud.io/dashboard?id=name.abuchen.portfolio%3Aportfolio-app) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=bugs)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=BUG) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=vulnerabilities)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=VULNERABILITY) [![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=coverage)](https://sonarcloud.io/component_measures?id=name.abuchen.portfolio%3Aportfolio-app&metric=coverage)


## Links

* [Homepage](https://www.portfolio-performance.info)
* [Downloads](https://github.com/portfolio-performance/portfolio/releases)
* [Forum](https://forum.portfolio-performance.info/)
* [Manual](https://help.portfolio-performance.info/en)


## Contributing Source Code

* [Development setup](CONTRIBUTING.md#development-setup)
* [Project setup](CONTRIBUTING.md#eclipse-ide-setup)
* [Contributing code](CONTRIBUTING.md#contributing-code)
* [Images and Icons](CONTRIBUTING.md#images-and-icons)
* [Translations](CONTRIBUTING.md#translations)
* [Interactive Flex Query Importers](CONTRIBUTING.md#interactive-flex-query-importers)
* [PDF Importers](CONTRIBUTING.md#pdf-importers)
* [Trade Calendars](CONTRIBUTING.md#trade-calendars)


## Public GPG Key

Fingerprint: `E46E 6F8F F02E 4C83 5690 8458 9239 277F 560C 95AC`

* [OpenPGP](https://keys.openpgp.org/search?q=0xe46e6f8ff02e4c83569084589239277f560c95ac)
* [Ubuntu Keyserver](https://keyserver.ubuntu.com/pks/lookup?search=e46e6f8ff02e4c83569084589239277f560c95ac&fingerprint=on&op=index)


## License

Eclipse Public License
https://www.eclipse.org/legal/epl-v10.html
