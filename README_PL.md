# Confirmatio Geo Test

Prototyp Android do sprawdzenia, czy telefon może pozostawać monitorowany w strefie przy wygaszonym ekranie.

## Co robi
- Ustawia bieżącą lokalizację telefonu jako środek strefy.
- Domyślny promień: 100 m (można zmienić).
- Po uruchomieniu testu startuje foreground service typu `location`.
- Żąda lokalizacji co 2 minuty i zapisuje: czas, W STREFIE / POZA STREFĄ, odległość oraz dokładność GPS.
- Działa po zgaszeniu ekranu, dopóki użytkownik nie zatrzyma monitoringu lub system/aplikacja nie zostanie ręcznie zatrzymana.

## Jak uruchomić
1. Otwórz folder projektu w Android Studio.
2. Poczekaj na Gradle Sync.
3. Podłącz telefon z Androidem przez USB i włącz Debugowanie USB.
4. Uruchom aplikację przyciskiem Run.
5. Przyznaj „Dokładną lokalizację” i powiadomienia.
6. Stań przy domu i naciśnij „USTAW TUTAJ ŚRODEK STREFY”.
7. Zostaw promień 100 m na pierwszy test.
8. Naciśnij „START TESTU”.
9. Zgaś ekran na 6–10 minut.
10. Otwórz aplikację i użyj „ODŚWIEŻ LOG”. Powinno być kilka wpisów co ok. 2 minuty.
11. Następnie z włączonym testem oddal się ponad 100 m od domu na kilka minut. W logu powinien pojawić się wpis „POZA STREFĄ”.
12. Wróć do domu i sprawdź, czy status wrócił do „W STREFIE”.

## Ważne
- 2 minuty to interwał żądany. Android i warunki GPS mogą dostarczyć odczyt trochę później; nie gwarantujemy sekundowej punktualności.
- Test nie wysyła lokalizacji do Internetu. Wszystko zapisuje lokalnie w telefonie.
- W wersji docelowej do mszy dane mogłyby być wysyłane do serwera tylko w postaci zdarzeń obecności, np. „w strefie/poza strefą + czas”, bez przechowywania pełnej trasy.
