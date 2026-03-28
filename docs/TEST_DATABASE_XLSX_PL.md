# Testowa baza danych Excel (APK admin)

Pliki testowe do wczytania (tekstowe, bez binarek):

- `assets/flota_test_database_pack/` (zestaw CSV)

## Co zawiera plik

Arkusze:

1. `Kontakty`
2. `Pracownicy`
3. `Zakłady`
4. `Rozmiary`
5. `Samochody`
6. `Zamówienia Ubrania`
7. `Historia Ubrań`
8. `Płace Podgląd`

## Jak używać

1. W APK admin przejdź do importu bazy (ustawienia/import danych).
2. Wybierz odpowiedni plik CSV z `assets/flota_test_database_pack/`.
3. Po imporcie sprawdź moduły:
   - Kontakty
   - Pracownicy
   - Zakłady
   - Ubrania robocze (rozmiary + dane pomocnicze do zamówień)
4. W module Wypłaty możesz użyć arkusza `Płace Podgląd` jako danych wejściowych do podglądu i testów wysyłki.

### Środowiska bez obsługi binarek (np. PR/Diff)

W repo trzymane są wyłącznie pliki tekstowe CSV, żeby PR/Diff działały poprawnie.
Jeśli potrzebujesz ZIP/XLSX, wygeneruj je lokalnie na podstawie folderu `assets/flota_test_database_pack/`.

Szybkie pakowanie ZIP:

```bash
./scripts/package_test_db.sh
```

Skrypt utworzy:

- `assets/flota_test_database_pack_local.zip`

Szybkie generowanie jednego pliku XLSX (bez commitowania binarki):

```bash
./scripts/build_test_xlsx.py
```

Skrypt utworzy:

- `assets/flota_test_database_generated.xlsx`

## Uwagi

- Import bazy danych w kodzie natywnym mapuje przede wszystkim: kontakty, pracowników, zakłady i rozmiary.
- Pozostałe arkusze są dostarczone jako gotowe dane testowe dla pozostałych modułów (manualne scenariusze QA/UAT).
