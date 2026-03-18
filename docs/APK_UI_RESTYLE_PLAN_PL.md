# Plan: jak upodobnić APK do makiety (bez utraty funkcji)

Ten dokument opisuje **bezpieczny plan przebudowy UI** aplikacji Kivy/KivyMD tak, aby wizualnie przypominała przedstawiony screen (karty, zaokrąglenia, gradientowy topbar, większe odstępy, nowoczesne przyciski), ale bez usuwania żadnych istniejących opcji biznesowych.

## 1) Co już jest i czego nie ruszać

Najważniejsze funkcje aplikacji są już zaimplementowane i działają w logice backendowej, dlatego redesign trzeba robić warstwowo (UI oddzielnie od logiki):

- przełączanie motywu jasny/ciemny (`switch_theme`, bridge do KivyMD),
- eksport danych i raportów,
- wysyłki e-mail,
- widoki kontaktów/zarządzania,
- dodatkowy moduł „clothes”.

Punkty zaczepienia w kodzie:

- `App.switch_theme()` + iteracja po ekranach do odświeżenia UI.  
- Budowa ekranów przez `self.sc_ref` i dedykowane `setup_*_ui()` (to jest dobre pod refactor UI).  
- Most KivyMD (`apply_md_theme`) ustawia bazowe palety i `theme_style`.

## 2) Strategia „bez utraty opcji”

Zamiast przepisywać logikę ekranów, zrób 3-warstwowy podział:

1. **Design tokens** – jedna tabela stylu (kolory, promienie, spacing, wysokości elementów).
2. **UI kit** – wspólne komponenty (karty, sekcje, list-row, primary/secondary button).
3. **Kompozycja ekranów** – każdy istniejący ekran składany z gotowych komponentów, ale z tymi samymi callbackami i tymi samymi danymi.

Dzięki temu:

- zachowujesz wszystkie akcje (`on_press`, eksport, mail, sync),
- zmieniasz wygląd globalnie,
- możesz robić redesign etapami ekran po ekranie.

## 3) Konkretne zmiany wizualne (jak na screenie)

### 3.1 Topbar i nagłówki

- Ujednolicić topbar: wysokość, ikonę „back”, tytuł, subtelny gradient blue.
- Zachować aktualną nawigację (`AppActionBar`), ale dodać nowy wariant stylu „modern”.
- Dodać większy margines od status bara i równy padding poziomy.

### 3.2 Karty sekcji

- Wszystkie sekcje ustawień i formularzy zamienić na „soft cards”:
  - duży `radius` (np. 18–24 dp),
  - jasne tło (light) / ciemny granat (dark),
  - subtelny cień,
  - spacing 12–16 dp.
- Zamiast surowych `BoxLayout` używać powtarzalnych komponentów `SectionCard`.

### 3.3 Kontrolki i CTA

- Przełączniki, inputy i przyciski ujednolicić:
  - główne CTA (zielono-niebieskie gradientowe),
  - wtórne CTA (neutralne),
  - danger CTA (czerwone).
- Zachować wszystkie obecne callbacki przycisków; zmienić tylko styl i kompozycję.

### 3.4 Ekrany ze screena: mapowanie 1:1

- **Ustawienia**: grupy „Powiadomienia / Motyw / Ogólne” jako osobne karty.
- **Skanowanie QR**: pełnoekranowy podgląd z ciemnym tłem + centralny skaner + wyraźny przycisk „Zeskanuj”.
- **Tryb offline**: dedykowany modal/screen z ikoną i krótką informacją + jedno CTA „OK”.
- **Eksport/Udostępnianie**: kafle PDF/XLS + kafle kanałów (Gmail, Outlook, WhatsApp).
- **Podsumowanie raportu**: layout oparty o nagłówek + metryki + miniatury zdjęć.
- **Czat/Wysyłanie**: układ bąbelków i „załącznik card”, bez zmiany mechaniki wysyłki.

## 4) Kolejność prac (iteracyjnie)

1. **Etap A – fundament stylu**
   - utworzyć moduł tokenów (`app_modules/design_tokens.py`),
   - rozbudować `ui_helpers.py` o nowoczesne komponenty,
   - dodać warianty light/dark zgodne z `apply_md_theme`.

2. **Etap B – ekrany o największej ekspozycji**
   - Home + Settings + Email/Report,
   - bez zmiany metod biznesowych (tylko layout i style).

3. **Etap C – flow eksportu i wysyłki**
   - ekran eksportu PDF/XLS,
   - ekran wyboru kanału,
   - ekran czatu/wysyłania raportu.

4. **Etap D – QR i offline**
   - nowy ekran skanera,
   - handling offline (`Popup`/screen fallback),
   - testy na Androidzie i desktopie.

## 5) Checklist „nie tracimy żadnej opcji”

Przed i po redesignie przejdź dokładnie ten sam smoke test:

- [ ] import danych i filtrowanie,
- [ ] export pojedynczego rekordu,
- [ ] eksport zbiorczy,
- [ ] wysyłka pojedynczego e-mail,
- [ ] wysyłka grupowa,
- [ ] przełączanie dark/light,
- [ ] działanie wszystkich zakładek w `self.sc_ref`,
- [ ] moduł `clothes` (sizes/orders/reports),
- [ ] działanie na Androidzie (buildozer) i desktopie.

## 6) Ryzyka i jak je ograniczyć

- **Ryzyko:** mieszanie logiki i UI w jednym miejscu utrudnia redesign.  
  **Mitigacja:** każdą zmianę robić w `setup_*_ui()` i helperach komponentów.

- **Ryzyko:** niespójny dark mode.  
  **Mitigacja:** wszystkie kolory brać z tokenów i z jednego punktu (`apply_md_theme` + tokens).

- **Ryzyko:** regresje callbacków.  
  **Mitigacja:** testy klikane + logowanie akcji krytycznych (export, send).

## 7) Minimalny zakres startowy (1 sprint)

Jeśli chcesz szybki efekt jak na makiecie, zacznij od:

1. nowy topbar + karty sekcji,
2. nowe style przycisków/inputs,
3. przebudowa jednego pełnego flow: **Ustawienia -> Raport -> Eksport/Wysyłka**.

To da największą różnicę wizualną przy minimalnym ryzyku dla funkcji.
