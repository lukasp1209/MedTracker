# MedTracker

MedTracker ist eine einfache Android-App, die dir hilft, deine tägliche Medikamenteneinnahme zu verfolgen und dich zuverlässig daran zu erinnern.

## Funktionen

*   **Medikamentenverwaltung:** Erfasse Name, Dosierung und zusätzliche Hinweise für deine Medikamente.
*   **Tägliche Erinnerungen:** Plane präzise Alarme für die tägliche Einnahme zu einer festgelegten Uhrzeit.
*   **Status verfolgen:** Markiere Medikamente als "genommen", um den Überblick über deine Historie zu behalten.
*   **Zuverlässigkeit:** Die App plant Alarme nach einem Geräteneustart automatisch neu.
*   **Moderne UI:** Gebaut mit Jetpack Compose und Material Design 3.

## Voraussetzungen

*   Android 8.0 (API Level 26) oder höher.
*   Für exakte Alarme (Android 12+) und Benachrichtigungen (Android 13+) werden entsprechende Berechtigungen in der App angefordert.

## Installation / Entwicklung

1.  Klone das Repository.
2.  Öffne das Projekt in **Android Studio**.
3.  Lasse Gradle das Projekt synchronisieren.
4.  Starte die App auf einem Emulator oder einem physischen Gerät.

## Projektstruktur

*   `app/src/main/java/com/example/medtracker/`: Enthält den Quellcode (UI, Repository, Alarm-Logik).
*   `app/src/main/res/`: Enthält die Ressourcen (Layouts, Werte, XML).

## Technologien

*   **Sprache:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Architektur:** Repository Pattern
*   **Zeitmanagement:** Java Time API & AlarmManager
