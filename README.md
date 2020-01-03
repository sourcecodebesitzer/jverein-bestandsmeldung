# jverein-bestandsmeldung

Dieses kleine Java-Programm ermöglicht es, eine Bestandsmeldung an den Landessportbund aus der OpenSource Vereinsverwaltung [JVerein](www.jverein.de) zu erstellen. Getestet wurde das Programm mit einem kleineren Verein (ca. 200 Mitglieder) und zwei Sportarten und einer Meldung an den [Württembergischen Landessportbund](www.wlsb.de).

Dabei wird eine Austauschdatei entsprechend der [Schnittstellenspezifikation des DOSB](https://cdn.dosb.de/user_upload/www.dosb.de/LandingPage/Service-Medien/schnitt/lsb_schnitt.pdf) erstellt.

## Kompilieren
javac bestandsmeldung.java

## Ausführen
```bash
java -classpath /path/to/h2-1.4.199.jar:. bestandsmeldung
```
Optional kann ein Stichtag-Datum als Parameter mit übergeben werden.

## Hinweise
- Berücksichtigt nur die Meldung zu den Verbandsnummern. Keine Aufsplittung in die Untergruppen.
- Es muss eine Eigenschaftengruppe mit den zu meldenden Sportarten geben.
- Verbandsnummern müssen in eckigen Klammern [] als Teil der Bezeichung der Sportart stehen.
- Alle Mitglieder werden als "aktiv" gemeldet. Keine passiven Mitglieder.
- Der Pfad zur JVerein-Datenbank ist aktuell hart codiert.

## Beispiel Eigenschaft mit Gruppe
```bash
Bezeichnung:       | Gruppe:
--------------------------------
Ski alpin [73]     | Sportart
Radsport [59]      | Sportart
```

# License
GPLv3
