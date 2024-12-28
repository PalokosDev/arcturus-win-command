# Habbo Win Command Plugin

Ein Plugin für den Arcturus Emulator, das einen `:win` Command implementiert, mit dem Eventmanager Gewinnern verschiedene Preisoptionen anbieten können.

## Features

- `:win` Command für Eventmanager
- Gewinner können aus 3 vordefinierten Gewinnoptionen wählen
- Automatische Vergabe von Eventpunkten
- Logging aller Gewinnvergaben
- Konfigurierbare Gewinnoptionen über das Housekeeping
- Berechtigungssystem für Eventmanager

## Installation

1. Kompiliere das Plugin oder lade die neueste Version von den Releases herunter
2. Platziere die JAR-Datei im `plugins` Ordner deines Arcturus Emulators
3. Starte den Emulator neu
4. Konfiguriere die Gewinnoptionen über das Housekeeping

## Datenbank

Das Plugin erstellt automatisch zwei neue Tabellen:

### hotel_win_options
Speichert die verfügbaren Gewinnoptionen
```sql
CREATE TABLE hotel_win_options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    win_option VARCHAR(255) NOT NULL
);
```

### hotel_win_logs
Protokolliert alle Gewinnvergaben
```sql
CREATE TABLE hotel_win_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    staff_username VARCHAR(255) NOT NULL,
    winner_username VARCHAR(255) NOT NULL,
    selected_win VARCHAR(255) NOT NULL
);
```

## Permissions

Füge folgende Permission für Eventmanager hinzu:
- `cmd_win` - Erlaubt die Nutzung des `:win` Commands

## Verwendung

1. Konfiguriere mindestens 3 Gewinnoptionen über das Housekeeping
2. Nutze den Command `:win username` um einem Gewinner die Auswahlmöglichkeit zu geben
3. Der Gewinner erhält ein Popup mit den 3 Auswahlmöglichkeiten
4. Nach Auswahl wird der Gewinn protokolliert und ein Eventpunkt vergeben

## Contributing

1. Fork das Repository
2. Erstelle einen Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Committe deine Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. Push zum Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen Pull Request

## License

Dieses Projekt ist unter der MIT Lizenz lizenziert. Siehe [LICENSE](LICENSE) für Details.

## Author

- **Palokos** - *Initial work* - [Palokos](https://github.com/PalokosDev)

## Acknowledgments

* Arcturus Emulator Team
* Habbo Community
