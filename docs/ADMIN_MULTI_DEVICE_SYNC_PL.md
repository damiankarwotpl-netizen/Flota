# Synchronizacja APK admin na wielu urządzeniach

## Ważne: jak działa to teraz

Aktualna aplikacja admin zapisuje dane **lokalnie** w bazie Room (`future_v20.db`) na każdym urządzeniu osobno.
To oznacza, że kontakty, auta, pracownicy i odzież **nie synchronizują się automatycznie** między telefonami/tabletami admin.

Zdalna integracja w ustawieniach dotyczy przede wszystkim synchronizacji danych kierowców i logów przebiegu z endpointem kierowców.

## Jak pracować na wielu urządzeniach już teraz (bez backendu centralnego)

Najbezpieczniejszy schemat operacyjny:

1. Wyznacz jedno urządzenie jako **MASTER** (to na nim robisz wszystkie zmiany).
2. Po zmianach na MASTER:
   - użyj `Eksportuj snapshot bazy`, albo
   - przygotuj plik Excel i użyj `Wgraj bazę danych z Excela` na innych urządzeniach.
3. Na urządzeniach podrzędnych importuj dane **zawsze po zamknięciu aplikacji na MASTER**, aby uniknąć konfliktów.
4. Nie edytuj tych samych rekordów równolegle na wielu urządzeniach.

To jest synchronizacja "batch/manual", a nie realtime.

## Jeśli chcesz prawdziwy realtime między wieloma adminami

Potrzebna jest architektura centralna:

- jeden backend API + wspólna baza (np. Postgres),
- autoryzacja użytkowników admin,
- wersjonowanie rekordów (`updatedAt`, `updatedBy`, `version`) i reguła rozwiązywania konfliktów,
- kolejka offline na urządzeniu + synchronizacja delta (pull/push),
- okresowe pełne snapshoty jako backup.

Dopiero wtedy każde urządzenie admin pobiera i wysyła zmiany do jednego źródła prawdy.
