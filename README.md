# Bestandsmeldung an Sportbund aus JVerein

Dieses kleine Java-Programm ermöglicht es, eine Bestandsmeldung der Mitglieder an den Landessportbund aus der OpenSource Vereinsverwaltung [JVerein](https://www.jverein.de/) zu erstellen.
Getestet wurde das Programm mit einem kleineren Verein (ca. 200 Mitglieder) und zwei Sportarten und einer Meldung an den [Württembergischen Landessportbund](https://www.wlsb.de/).

Dabei wird eine Austauschdatei entsprechend der [Schnittstellenspezifikation des DOSB](https://cdn.dosb.de/user_upload/www.dosb.de/LandingPage/Service-Medien/schnitt/lsb_schnitt.pdf) erstellt.

## Kompilieren
```bash
javac bestandsmeldung.java
```

## Ausführen
```bash
java -classpath /path/to/h2-1.4.199.jar:. bestandsmeldung
```
Unter Windows wird statt dem Doppelpunkt im Classpath wahrscheinlich ein Semikolon benötigt.

Die benötigte h2 library ist Teil von Hibiscus und liegt in dessen 'lib'-Verzeichnis.

Optional kann ein Stichtag-Datum als Parameter mit übergeben werden. Ansonsten wird der 1. Januar des gerade aktuellen Jahres als Stichtag verwendet.
```bash
java -classpath /path/to/h2-1.4.199.jar:. bestandsmeldung 01.01.2020
```

## Hinweise
- Berücksichtigt nur die Meldung zu den Verbandsnummern. Keine Aufsplittung in die Untergruppen.
- Es muss eine Eigenschaftengruppe 'Sportart' mit den zu meldenden Sportarten geben.
- Verbandsnummern müssen in eckigen Klammern [] als Teil der Bezeichung der Sportart stehen.
- Alle Mitglieder werden als "aktiv" gemeldet. Keine passiven Mitglieder.
- Der Pfad zur JVerein-Datenbank ist hart codiert.
- Die Ausgabe erfolgt ins aktuelle Verzeichnis. Es wird entsprechend der Spezifikation die Datei 'ja.dat' erstellt.

## Beispiel Eigenschaft mit Gruppe
```bash
Bezeichnung:       | Gruppe:
--------------------------------
Ski alpin [73]     | Sportart
Radsport [59]      | Sportart
```

## Weiterentwicklung
Verbesserungsvorschläge und Hinweise sind willkommen.

## License
[GPLv3](https://www.gnu.org/licenses/)
