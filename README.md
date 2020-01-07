# Bestandsmeldung an Sportbund aus JVerein

Dieses kleine Java-Programm ermöglicht es, eine Bestandsmeldung der Mitglieder an den Landessportbund aus der OpenSource Vereinsverwaltung [JVerein](https://www.jverein.de/) zu erstellen.
Getestet wurde das Programm mit einem kleineren Verein (ca. 200 Mitglieder) und zwei Sportarten und einer Meldung an den [Württembergischen Landessportbund](https://www.wlsb.de/).

Dabei wird eine Austauschdatei entsprechend der [Schnittstellenspezifikation des DOSB](https://cdn.dosb.de/user_upload/www.dosb.de/LandingPage/Service-Medien/schnitt/lsb_schnitt.pdf) erstellt.

Als Quelle für die Daten wird die H2-Datenbank von JVerein verwendet. Es werden alle Mitglieder, die nicht oder am Stichtag noch nicht ausgetreten sind, berücksichtigt. D.h. alle Mitglieder, bei denen kein Austrittdatum oder ein Austrittsdatum in der Zukunft eingetragen ist, werden berücksichtigt.

Der an den Landesverband zu meldende Fachverband wird in einer Eigenschaftengruppe direkt in JVerein verwaltet. D.h. es muss eine entsprechende Eigenschaftengruppe "Sportart" angelegt und dazu dann Eigenschaften erstellt werden. Diese Eigenschaften müssen direkt die Nummer des Fachverbands entsprechend der [Verbandsdefinition](https://cdn.dosb.de/user_upload/www.dosb.de/LandingPage/Service-Medien/schnitt/lsb_kodierung_Version_2019.pdf) enthalten. Die konkreten Sportarten werden aktuell nicht in die automatische Meldung mit aufgenommen, sondern nur der jeweilige Fachverband gemeldet.
Beim jeweiligen Mitglied wird dann die Sportart, in der es aktiv ist, angehakt.

## Beispiel Eigenschaften der Gruppe Sportart in JVerein
```
Bezeichnung:       | Gruppe:
--------------------------------
Ski alpin [73]     | Sportart
Radsport [59]      | Sportart
```

## Kompilieren
```
javac bestandsmeldung.java
```

## Ausführen
```
java -classpath /path/to/h2-1.4.199.jar;. bestandsmeldung
```
Unter Linux wird statt dem Semikolon im Classpath ein Doppelpunkt benötigt.

Die benötigte h2 library ist Teil von Hibiscus und liegt in dessen 'lib'-Verzeichnis.

Optional kann ein Stichtag-Datum als Parameter mit übergeben werden. Ansonsten wird der 1. Januar des gerade aktuellen Jahres als Stichtag verwendet.
```
java -classpath /path/to/h2-1.4.199.jar;. bestandsmeldung 01.01.2020
```

## Hinweise
- Berücksichtigt nur die Meldung zu den Fachverbänden. Keine Aufsplittung in die Sportarten.
- Es muss eine Eigenschaftengruppe 'Sportart' mit den zu meldenden Sportarten geben.
- Die Fachverbandsnummern müssen in eckigen Klammern [] als Teil der Bezeichung der Sportart stehen.
- Jedem Mitglied muss eine Sportart in JVerein zugewiesen sein. Also mindestens eine der Eigenschaften angehakt sein.
- Alle Mitglieder werden als aktiv gemeldet. Passive Mitglieder sind aktuell in diesem Tool nicht vorgesehen.
- Der Pfad zur JVerein-Datenbank ist hart codiert.
- Die Ausgabe erfolgt ins aktuelle Verzeichnis. Es wird entsprechend der Spezifikation die Datei 'ja.dat' erstellt.

## Weiterentwicklung
Verbesserungsvorschläge und Hinweise sind willkommen.

## License
[GPLv3](https://www.gnu.org/licenses/)
