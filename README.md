# Kde jsou moje prachy?

Základní desktopová aplikace pro Java 17, Swing a SQLite.

## Požadavky
- Java 17 nebo novější (např. Eclipse Temurin)
- Apache Maven

## Spuštění
Spusťte `run-dev.bat`, případně v příkazové řádce:

```text
mvn clean compile exec:java
```

Databáze je uložena v `data/database.db`. Aplikace při startu automaticky ověří a případně vytvoří tabulky `vydaje` a `kategorie`.

Použité komponenty:
- OpenJDK / Swing
- SQLite
- Xerial SQLite JDBC (Apache License 2.0)
