/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 Stefan Bitzer
 */

/*
 * Hinweise:
 * - Berücksichtigt nur die Meldung zu den Verbandsnummern. Keine Aufsplittung in die Untergruppen.
 * - Es muss eine Eigenschaftengruppe mit den zu meldenden Sportarten geben.
 * - Verbandsnummern müssen in eckigen Klammern [] als Teil der Bezeichung der Sportart stehen.
 * - Alle Mitglieder werden als "aktiv" gemeldet. Keine passiven Mitglieder.
 * - Die Datenbank wird im selben Ordner erwartet oder muss konfiguriert werden.
 *
 * Schnittstellenspezifikation des DOSB:
 * https://cdn.dosb.de/user_upload/www.dosb.de/LandingPage/Service-Medien/schnitt/lsb_schnitt.pdf
 */

// TODO: von Kommandozeile einlesen: Vereinsnummer, Eigenschaftgruppe, Speicherort Datenbank

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class bestandsmeldung {
	// KONFIGURATION
	// Vereinsnummer, die vom Landsverband vergeben wird. Muss exakt 10 Stellen lang sein.
	private static final String VEREINSNUMMER = "xx-xxx    ";
	// Speicherort und Dateiname der H2-Datenbank.
	private static final String SPEICHERORT_DATABASE = "~/.jameica/jverein/h2db/jverein";
	// Definition der Ausgabedatei.
	private static final String AUSGABE_DATEI = "ja.dat";
	// Name der Eigenschaftengruppe, die für die Meldung verwendet wird.
	private static final String EIGENSCHAFTENGRUPPE_SPORTART = "Sportart";

	public static void main(String[] args) throws Exception {
		if(VEREINSNUMMER.length() != 10) {
			System.err.println("!!! FEHLER: Vereinsnummer ist nicht 10 Zeichen lang.");
			return;
		}

		// Stichtag erster Tag im aktuellen Jahr
		String stichtag = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime()) + "-01-01";
		// hole Stichtag optional als Argument von der Kommandozeile
		if(args.length > 0) {
			try {
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
				Date date = format.parse(args[0]);
				stichtag = new SimpleDateFormat("yyyy-MM-dd").format(date);
			} catch (Exception e) { }
		}

		// Aktuelles Datum als Erzeugungsdatum
		String datumErzeugung = new SimpleDateFormat("dd.MM.yyyy_HH:mm").format(Calendar.getInstance().getTime());

		// Zur H2 Datenbank verbinden
		Class.forName("org.h2.Driver");
		Connection connection = DriverManager.getConnection("jdbc:h2:" + SPEICHERORT_DATABASE, "jverein", "jverein");

		System.out.println("");
		System.out.print("Bestandsmeldung Sportarten an Landessportbund zum Stichtag: ");
		System.out.println(new SimpleDateFormat("dd.MM.yyyy").format(new SimpleDateFormat("yyyy-MM-dd").parse(stichtag)));
		System.out.println("(anderer Stichtag kann als Argument in der Form TT.MM.JJJJ übergeben werden)");
		System.out.println("");
		System.out.println("Datenbank: " + SPEICHERORT_DATABASE);
		System.out.println("");

		// get Eigenschaftenbezeichung from DB
		Hashtable<Integer, Integer> hashtableIdVerband = new Hashtable<>();
		System.out.println("Folgende Eigenschaften aus der Eigenschaftgruppe '" + EIGENSCHAFTENGRUPPE_SPORTART + "' werden verwendet und der Verbandsnummer zugeordnet:");
		Statement statement = connection.createStatement();
		ResultSet resultset = statement.executeQuery("SELECT EIGENSCHAFT.ID, EIGENSCHAFT.BEZEICHNUNG FROM EIGENSCHAFT INNER JOIN EIGENSCHAFTGRUPPE ON EIGENSCHAFT.EIGENSCHAFTGRUPPE = EIGENSCHAFTGRUPPE.ID WHERE EIGENSCHAFTGRUPPE.BEZEICHNUNG = '" + EIGENSCHAFTENGRUPPE_SPORTART + "' ORDER BY EIGENSCHAFT.ID;");
		while(resultset.next()) {
			// hole Verbandsnummer aus der Bezeichung. Muss innerhalb der Klammern stehen.
			int openBracket = resultset.getString("BEZEICHNUNG").indexOf('[');
			int closingBracket = resultset.getString("BEZEICHNUNG").indexOf(']');

			if (openBracket >= 0 && closingBracket >= 0 && closingBracket > openBracket) {
				int verbandsnummer = Integer.parseInt(resultset.getString("BEZEICHNUNG").substring(openBracket + 1, closingBracket));
				hashtableIdVerband.put(Integer.parseInt(resultset.getString("ID")), verbandsnummer);

				System.out.print(resultset.getString("BEZEICHNUNG"));
				System.out.print(" --> ");
				System.out.print(verbandsnummer);
				System.out.println("");
			}
		}
		System.out.println("");
		resultset.close();
		statement.close();

		if(hashtableIdVerband.size() == 0) {
			System.err.println("!!! FEHLER: Keine Eigenschaften in der Eigenschaftgruppe '" + EIGENSCHAFTENGRUPPE_SPORTART + "' gefunden. Gruppe nicht angelegt? Die Bezeichung der Eigenschaften muss auch die Verbandsnummer in eckigen Klammern [] enthalten. Beispiel:");
			System.err.println("Bezeichnung:       | Gruppe:");
			System.err.println("--------------------------------");
			System.err.println("Ski alpin [73]     | Sportart");
			System.err.println("Radsport [59]      | Sportart");
			System.err.println("");
			return;
		}

		// erstelle SQL command für Mitgliederabfrage
		StringBuffer sqlCommand1 = new StringBuffer("SELECT MITGLIED.ID, MITGLIED.GEBURTSDATUM, MITGLIED.GESCHLECHT, EIGENSCHAFTEN.EIGENSCHAFT FROM MITGLIED LEFT JOIN EIGENSCHAFTEN ON MITGLIED.ID = EIGENSCHAFTEN.MITGLIED AND (");
		int counter = 0;
		Iterator<Integer> itr = hashtableIdVerband.keySet().iterator();
		while(itr.hasNext()) {
			counter++;
			Integer key = itr.next();
			sqlCommand1.append("EIGENSCHAFTEN.EIGENSCHAFT = ");
			sqlCommand1.append(key);

			if(itr.hasNext()) {
				sqlCommand1.append(" OR ");
			}
		}
		sqlCommand1.append(") WHERE (MITGLIED.AUSTRITT IS NULL) OR (MITGLIED.AUSTRITT > '");
		sqlCommand1.append(stichtag);
		sqlCommand1.append("') ORDER BY MITGLIED.GEBURTSDATUM, MITGLIED.ID, EIGENSCHAFTEN.EIGENSCHAFT;");

		// get Mitglieder from DB
		statement = connection.createStatement();
		resultset = statement.executeQuery(sqlCommand1.toString());
		PrintStream printstream = new PrintStream(new FileOutputStream(AUSGABE_DATEI));

		int lastRunJahrgang = 0;
		int lastRunMitgliedId = 0;
		String lastRunGeschlecht = "";
		int ameldung_m_aktiv = 0;
		int ameldung_w_aktiv = 0;
		int gesamtzahlMitglieder = 0;

		Hashtable<Integer, Integer> hashtableMitgliedSportart = new Hashtable<>();
		Hashtable<Integer, Integer> hashtableMitgliederSportartMaennlich = new Hashtable<>();
		Hashtable<Integer, Integer> hashtableMitgliederSportartWeiblich = new Hashtable<>();

		while (resultset.next()) {
			// Bei mindestens einem Mitglied ist keine Sportart in den Eigenschaften zugewiesen --> Fehler ausgeben und abbrechen
			if (resultset.getString("EIGENSCHAFT") == null) {
				System.err.println("!!! FEHLER: Mitglied ohne mindestens eine der abgefragten Eigenschaften. Mitglieds-ID: " + resultset.getString("ID"));
			}

			// nächstes Mitglied oder letzter Datensatz, also Zähler übernehmen
			if (resultset.isLast() || (lastRunMitgliedId != 0 && lastRunMitgliedId != Integer.parseInt(resultset.getString("ID")))) {
				if(lastRunGeschlecht.equals("m")) {	// männlich
					ameldung_m_aktiv++;

					itr = hashtableMitgliedSportart.keySet().iterator();
					while(itr.hasNext()) {
						Integer key = itr.next();

						int value = 0;
						try {
							value = hashtableMitgliederSportartMaennlich.get(key);
						} catch (NullPointerException e) {}

						hashtableMitgliederSportartMaennlich.put(key, ++value);
					}
				}
				else {	// weiblich
					ameldung_w_aktiv++;

					itr = hashtableMitgliedSportart.keySet().iterator();
					while(itr.hasNext()) {
						Integer key = itr.next();

						int value = 0;
						try {
							value = hashtableMitgliederSportartWeiblich.get(key);
						} catch (NullPointerException e) {}

						hashtableMitgliederSportartWeiblich.put(key, ++value);
					}
				}

				// Hashtables für nächsten Durchgang leeren
				hashtableMitgliedSportart = new Hashtable<>();
			}

			// nächster Jahrgang kommt daran oder letzter Datensatz, deshalb noch zuvor die Daten ausgeben für Verbandsnummer und die A-Meldung
			if (resultset.isLast() || (lastRunJahrgang != 0 && lastRunJahrgang != Integer.parseInt(resultset.getString("GEBURTSDATUM").substring(0, 4)))) {
				Hashtable<Integer, Integer> verbandsnummerDone = new Hashtable<>();

				// B-Meldungen für Verbandsnummer des letzten Jahrgangs
				itr = hashtableIdVerband.keySet().iterator();
				while(itr.hasNext()) {
					Integer key = itr.next();

					// nur ausgeben, wenn diese Verbandsnummer noch nicht ausgegeben wurde
					int thisVerbandsnummerDone = 0;
					try {
						thisVerbandsnummerDone = verbandsnummerDone.get(hashtableIdVerband.get(key));
					} catch (NullPointerException e) {}

					if(thisVerbandsnummerDone == 0) {
						int valueMaennlich = 0;
						try {
							valueMaennlich = hashtableMitgliederSportartMaennlich.get(hashtableIdVerband.get(key));
						} catch (NullPointerException e) {}

						int valueWeiblich = 0;
						try {
							valueWeiblich = hashtableMitgliederSportartWeiblich.get(hashtableIdVerband.get(key));
						} catch (NullPointerException e) {}

						verbandsnummerDone.put(hashtableIdVerband.get(key), 1);

						// mindestens eine Meldung in Weiblich oder Männlich
						if (valueMaennlich > 0 || valueWeiblich > 0) {
							printstream.print(VEREINSNUMMER);	// Vereinsnummer
							printstream.print("        ");	// Schlüssel1 (leer)
							printstream.print("        ");	// Schlüssel2 (leer)
							printstream.print(String.format("%04d", hashtableIdVerband.get(key)));	// Skz1 - Verbandsnummer
							printstream.print(String.format("%04d", 0));	// Skz2 - Sportartennummer
							printstream.print(String.format("%04d", lastRunJahrgang));	// Jahrgang
							printstream.print(String.format("%08d", valueMaennlich));	// Maennlich_aktiv
							printstream.print(String.format("%08d", 0));	// Maennlich_passiv
							printstream.print(String.format("%08d", valueWeiblich));	// Weiblich_aktiv
							printstream.print(String.format("%08d", 0));	// Weiblich_passiv
							printstream.print(datumErzeugung);	// Datum_erzeugung
							printstream.println("");
						}
					}
				}

				// Hashtables für nächsten Jahrgang leeren
				hashtableMitgliederSportartMaennlich = new Hashtable<>();
				hashtableMitgliederSportartWeiblich = new Hashtable<>();

				// A-Meldung für den letzten Jahrgang
				printstream.print(VEREINSNUMMER);	// Vereinsnummer
				printstream.print("        ");	// Schlüssel1 (leer)
				printstream.print("        ");	// Schlüssel2 (leer)
				printstream.print(String.format("%04d", 0));	// Skz1 - Verbandsnummer
				printstream.print(String.format("%04d", 0));	// Skz2 - Sportartennummer
				printstream.print(String.format("%04d", lastRunJahrgang));	// Jahrgang
				printstream.print(String.format("%08d", ameldung_m_aktiv));	// Maennlich_aktiv
				printstream.print(String.format("%08d", 0));	// Maennlich_passiv
				printstream.print(String.format("%08d", ameldung_w_aktiv));	// Weiblich_aktiv
				printstream.print(String.format("%08d", 0));	// Weiblich_passiv
				printstream.print(datumErzeugung);	// Datum_erzeugung
				printstream.println("");

				gesamtzahlMitglieder += ameldung_m_aktiv + ameldung_w_aktiv;

				// Zähler zurücksetzen für nächsten Jahrgang
				ameldung_m_aktiv = 0;
				ameldung_w_aktiv = 0;
			}

			// Zähler erhöhen für konkrete Verbandsnummer (aktueller Durchlauf)
			if(resultset.getString("EIGENSCHAFT") != null) {
				hashtableMitgliedSportart.put(hashtableIdVerband.get(Integer.parseInt(resultset.getString("EIGENSCHAFT"))), 1);
			}

			// vorbereiten für nächsten Durchlauf
			lastRunJahrgang = Integer.parseInt(resultset.getString("GEBURTSDATUM").substring(0, 4));
			lastRunMitgliedId = Integer.parseInt(resultset.getString("ID"));
			lastRunGeschlecht = resultset.getString("GESCHLECHT");
		}

		System.out.println("Gesamtzahl Miglieder in dieser Meldung: " + gesamtzahlMitglieder);
		System.out.println("");
		System.out.println("Ausgabe wurde in die Datei '" + AUSGABE_DATEI + "' geschrieben.");
		System.out.println("");

		printstream.close();
		resultset.close();
		statement.close();
		connection.close();
	}
}
