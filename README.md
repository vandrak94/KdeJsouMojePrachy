# Kde jsou moje prachy?

Jednoduchá aplikace pro evidenci osobních výdajů. 
Funguje offline a bez registrace.

## Spuštění

1. Rozbalte stažený ZIP.
2. Spusťte `KdeJsouMojePrachy.exe` ze složky aplikace.

Není potřeba instalovat Javu ani databázový server.

## Přehled výdajů

- V nabídce **Rozsah** zvolte **Den, Týden, Měsíc** nebo **Rok**.
- Tlačítkem kalendáře vyberte požadované období.
- Přehled zobrazuje celkovou částku, jednotlivé výdaje a koláčový graf podle kategorií.
- Barvy výdajů odpovídají kategoriím. 
- Poznámku k výdaji zobrazíte najetím myší.

## Správa výdajů

- **Vložit výdaj:** Vyberte datum a kategorii, zadejte kladnou částku a případně poznámku. Poté klikněte na **Uložit**.
- **Upravit výdaj:** Klikněte na ikonu tužky u příslušného výdaje.
- **Smazat výdaj:** Klikněte na ikonu koše a potvrďte odstranění.

## Kategorie

V **Nastavení → Kategorie** můžete:

- Přidat novou kategorii tlačítkem **Přidat kategorii**.
- Změnit název nebo barvu kategorie ikonou tužky.
- Odstranit kategorii ikonou koše po potvrzení.

**Kategorii používanou u některého výdaje nelze odstranit.**

## Nastavení databáze

V **Nastavení → Databáze** můžete prohlížet uložené záznamy nebo vyčistit tabulky výdajů a kategorií.

**Pozor:** Vyčištění tabulky odstraní její data. Předem doporučujeme zálohu.

## Zálohování dat

Výdaje a kategorie jsou uloženy lokálně v souboru `data/database.db`.

1. Ukončete aplikaci.
2. Zkopírujte `database.db` na bezpečné místo.

Pro obnovení dat ukončete aplikaci a nahraďte soubor záložní kopií. 
Aplikaci spouštějte z její složky, aby používala správnou databázi.

## Další informace

- Částky se zobrazují v Kč.
- Aplikace si pamatuje velikosti dialogů.
- Licenční informace použitých komponent jsou přiloženy ve složce `licenses`.
