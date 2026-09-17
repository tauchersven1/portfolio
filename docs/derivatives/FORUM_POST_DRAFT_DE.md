# Entwurf – Update zum Derivate-Add-on und Exposure Management

Hallo zusammen,

wie angekündigt habe ich den Proof of Concept weitergeführt und dabei zwei Dinge bewusst getrennt betrachtet:

1. einen vollständigen, integrierten Referenzstand, mit dem sich die fachlichen Funktionen testen lassen;
2. einen separaten Add-on-Prototyp, mit dem sich die notwendigen Extension Points untersuchen lassen.

Der aktuelle integrierte Referenzstand ist **Bridge 19 / Feature Build 111** auf Basis von Portfolio Performance 0.87.0. Er enthält die bisherige Derivate- und Exposure-Funktionalität und dient als ausführbare Spezifikation beziehungsweise Testumgebung.

Neu beziehungsweise zuletzt ergänzt wurde die Gruppierung nach dem Basiswert-Symbol: Eine Microsoft-Aktie mit dem Symbol `MSFT` und die Option `MSFT260918C00490000` werden in **Exposure by Trading Symbol** nun gemeinsam unter `MSFT` dargestellt. Dafür wird zunächst ein explizit verknüpfter Basiswert verwendet; ersatzweise wird bei erkennbaren Optionssymbolen das Underlying aus dem Kontraktsymbol abgeleitet.

Parallel dazu habe ich einen eigenständigen Eclipse/OSGi-Add-on-Prototyp gebaut. Dieser wird als separates Feature mit eigener p2 Update Site erzeugt und trägt Exposure-Berichte über einen kleinen Navigation Extension Point bei. Dadurch muss das Add-on nicht auf interne Klassen der Navigation zugreifen.

Der Prototyp zeigt derzeit insbesondere:

- Registrierung eigener Berichtsansichten über `name.abuchen.portfolio.ui.navigation`,
- einen kleinen öffentlichen View-Vertrag (`AddonView` und `AddonViewContext`),
- eine vom Add-on besessene Exposure-Ansicht,
- separate Paketierung als OSGi-Feature und Update Site.

Wichtig ist die Abgrenzung: Das separat paketierte Add-on hat noch nicht den gesamten Funktionsumfang des integrierten Build 111. Der vollständige Referenzstand enthält weiterhin Core-Anpassungen für das Datenmodell, die Persistenz, die derivatespezifische Bewertung, den Wertpapiereditor, Imports und die Exposure-Berechnung.

Aus der bisherigen Arbeit ergeben sich aus meiner Sicht folgende sinnvolle Extension Points für Portfolio Performance:

- Berichtsansichten und Navigation,
- zusätzliche Reiter beziehungsweise Bereiche im Wertpapiereditor,
- zusätzliche Spalten in vorhandenen Tabellen,
- Bewertungs- und Exposure-Strategien für bestimmte Instrumente,
- Stammdatenprovider und Symbol-Resolver,
- CSV-Felder und Validierungen,
- Modelländerungsereignisse,
- persistente Add-on-Daten, die auch ohne installiertes Add-on verlustfrei erhalten bleiben.

Der aktuelle Stand bestätigt damit beide Seiten der bisherigen Diskussion: Die fachliche Exposure-Funktion lässt sich umsetzen, aber eine saubere Trennung vom Kern benötigt einige bewusst definierte, möglichst kleine Schnittstellen. Eine vollständige Unabhängigkeit von App und Add-on halte ich ebenfalls nicht für realistisch; eine separate Installation und Aktualisierung bei klar versionierten Verträgen erscheint aber grundsätzlich machbar.

Für einen einfachen Test habe ich das Repository auf einen klaren Downloadstand vorbereitet. Die Veröffentlichung enthält:

- die Windows-Version Bridge 19 / Build 111 mit gebündelter Java-Laufzeit,
- eine synthetische Demo-Datei,
- eine technische Beschreibung der Core-Änderungen, Extension Points und des Add-on-Prototyps,
- eine SHA-256-Prüfsumme.

Die Demo-Datei enthält keine persönlichen Portfolio- oder Kontodaten.

Download und technische Dokumentation:

<https://github.com/tauchersven1/portfolio/releases/tag/derivatives-poc-bridge19-build111>

Der Code ist ausdrücklich weiterhin ein Proof of Concept und kein fertiger Upstream-Pull-Request. Besonders interessieren würden mich Rückmeldungen zu zwei Punkten:

1. Ist die vorgeschlagene Trennung zwischen kleinem Core-Bridge-API und fachlichem Add-on nachvollziehbar?
2. Welche der genannten Extension Points wären aus Projektsicht ein sinnvoller erster, bewusst kleiner Schritt?

Viele Grüße

Sven
