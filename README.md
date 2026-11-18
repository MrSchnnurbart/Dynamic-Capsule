# Dynamic Capsule — Apple Dynamic Island for Android

[![Android CI & Release APK](https://github.com/MrSchnnurbart/Dynamic-Capsule/actions/workflows/android-release.yml/badge.svg)](https://github.com/MrSchnnurbart/Dynamic-Capsule/actions/workflows/android-release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-12%2B%20(API%2031%2B)-3DDC84.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4%20%7C%20Jetpack%20Compose-7F52FF.svg)](https://kotlinlang.org)

**Dynamic Capsule** est une application Android native en **Kotlin (Jetpack Compose + Material 3)**, 100 % open-source et sans publicité, qui apporte une capsule interactive flottante au style sobre et noir profond inspiré du **Dynamic Island d'Apple**, adaptée à n'importe quel poinçon ou encoche de caméra Android.

*🇬🇧 English documentation is available [below](#-english-overview).*

---

## ✨ Fonctionnalités Principales (FR)

- **Design Noir Profond Style Apple Dynamic Island** :
  - Capsule `#000000` pure sans contour fluo ni halo superflu, animations physiques à **60/120 fps** (`SpringAnimation` : modes *« Comme un Fruit (Ressort Apple) »* et *« Comme un Robot (Fluide Android) »*).
  - **Opacité au repos à `0% (Recommandé)`** : totalement invisible lorsqu'aucune activité n'est en cours (zéro consommation CPU et protection des dalles OLED), tout en détectant les gestes sur la zone caméra.
- **Zone Caméra Anti-Masquage Universelle (Cercle Rouge)** :
  - Calibrage visuel par **Cercle Rouge + ligne blanche d'alignement** à placer sous l'objectif photo frontal.
  - Prise en charge des caméras à **Gauche**, au **Centre**, à **Droite** ou du mode **Sous la barre d'état**. Les icônes, pochettes et textes s'écartent automatiquement du poinçon caméra afin qu'aucune information ne soit jamais cachée.
- **7 Cartes Dynamiques (Réorganisables par Drag & Drop & Mode Scindé)** :
  1. **Lecteur média** (`MediaSessionManager` : pochette, titre, artiste, égaliseur animé, contrôles Lecture/Pause/Suivant/Précédent).
  2. **Appels** (appels téléphoniques et messageries avec chronomètre, haut-parleur, muet et raccrocher).
  3. **Minuteur & chronomètre** (suivi en direct de l'horloge système).
  4. **Mises à jour en direct & progression** (`ProgressBar` Google Maps, Play Store, téléchargements, Telegram).
  5. **Lampe torche** (`CameraManager.TorchCallback` : extinction en 1 tap + réglage d'intensité sur 5 niveaux).
  6. **Volume** (contrôle visuel automatique lorsqu'un média joue).
  7. **Charge en cours** (pourcentage, puissance et estimation).
  - *Mode Capsule Scindée* : affiche une pastille secondaire détachée lorsque 2 activités ou plus sont actives simultanément.
- **17 Événements « Mini Capsule » (Popups Temporaires)** :
  - **Appareil** : Déverrouillage de l'écran.
  - **Batterie** : Début de charge, fin de charge, alerte batterie faible (5 %), économiseur d'énergie.
  - **Connectivité** : Wi-Fi connecté, point d'accès mobile (Hotspot), VPN, Mode Avion, Localisation (GPS).
  - **Audio & Bluetooth** : Écouteurs/casque Bluetooth, changement de mode sonore (Sonnerie / Vibreur / Silencieux), Ne pas déranger (DND).
  - **Affichage & Périphériques** : Luminosité adaptative, bascule Thème Clair/Sombre, rotation automatique, périphérique USB OTG.
  - **Intégrations tierces** : Jauges circulaires de batterie AirPods (Gauche / Droite / Boîtier) compatibles *MaterialPods / OpenPods*.
- **5 Gestes Indépendants & 16 Actions** :
  - Gestes : Appui simple, Appui long, Double appui, Balayage gauche, Balayage droite + **Balayer vers le haut pour fermer (`Swipe up to close`)**.
  - Actions : Défilement intelligent (Scroll), Capture d'écran, Lancer une application, Recherche d'application, Lampe torche, Curseurs / Raccourcis / Codes-barres, Widget, Appel direct, Broadcast Intent personnalisé (Tasker / MacroDroid), QR Code & Code-128, Contrôle média, Action d'accessibilité, Assistant IA (ChatGPT), Luminosité auto, Mode volume, Verrouiller l'écran, Paramètres système.
- **Mise à jour automatique GitHub & Support Bilingue Complet (FR / EN)** :
  - Guide de bienvenue interactif au premier démarrage.
  - Vérificateur et téléchargeur de mises à jour intégré connecté aux [GitHub Releases](https://github.com/MrSchnnurbart/Dynamic-Capsule/releases) avec affichage automatique des notes de version à chaque mise à jour.

---

## 📲 Installation & Permissions

1. Téléchargez la dernière version de l'APK depuis la section [**Releases**](https://github.com/MrSchnnurbart/Dynamic-Capsule/releases).
2. Installez l'APK sur votre appareil Android (Android 12 / API 31 minimum).
3. Suivez le **Guide de premier démarrage** intégré pour activer :
   - **Service d'Accessibilité** (`TYPE_ACCESSIBILITY_OVERLAY`) : requis uniquement pour dessiner la capsule par-dessus la barre d'état et exécuter les gestes (aucune donnée personnelle n'est collectée).
   - **Accès aux Notifications** (`NotificationListenerService`) : requis pour lire le lecteur média actif, la progression et les notifications choisies.
   - **Exemption d'optimisation batterie** : garantit que le système ne ferme pas la capsule en arrière-plan.
   - **Shizuku (Optionnel)** : permet de masquer sélectivement les icônes de la barre d'état (`icon_blacklist`) avec restauration automatique.

---

## 🇬🇧 English Overview

**Dynamic Capsule** is a free, ad-free, open-source native Android application built with **Kotlin, Jetpack Compose, and Material 3** that brings an authentic, pitch-black **Apple Dynamic Island** experience to any Android device.

### Key Highlights
- **Sober Apple Dynamic Island Style**: Pure `#000000` pill with 60/120 fps spring physics (*"Like a Fruit"* & *"Like a Robot"* presets) and `0%` idle opacity by default to protect OLED screens.
- **Camera Cutout Safe Zone (Red Circle Calibration)**: Align the red calibration circle directly under your front camera (`Left`, `Center`, `Right`, or `Below Status Bar`) so icons and text automatically split around the lens and are never occluded.
- **7 Reorderable Dynamic Cards & Split Capsule**: Media Player, Active Calls, Timer & Stopwatch, Live Progress, Flashlight, Volume, and Charging Status.
- **17 System Mini-Capsule Popups**, **5 Gestures & 16 Actions**, **App Notifications inside the Capsule**, **Built-in GitHub Auto-Updater**, and **Full English & French Localization**.

---

## 🛠️ Architecture & Compilation (Build from Source)

Le projet repose sur une architecture multi-modules Gradle propre :
- `:app` — Point d'entrée, navigation Material 3, thème et `activity-alias`.
- `:core:model` — Modèles de domaine immuables, localisation FR/EN (`AppStrings`) et catalogue des notes de version (`ReleaseNotesCatalog`).
- `:core:datastore` — Persistance réactive Jetpack DataStore.
- `:core:database` — Base SQLite asynchrone (profils d'écran multi-résolutions Portrait/Paysage, applications autorisées, codes-barres).
- `:core:system` — `CapsuleStateEngine`, `SystemEventMonitor`, `CapsuleNotificationListenerService`, `FlashlightController`, `ShizukuStatusBarManager`, et `GitHubUpdateManager`.
- `:feature:overlay` — `CapsuleAccessibilityService`, `OverlayWindowController`, physique 120 fps et composants Compose de la capsule.
- `:feature:onboarding` — Guide d'accueil au premier démarrage et assistant de permissions.
- `:feature:settings` — Écrans de configuration Material 3.

```bash
git clone https://github.com/MrSchnnurbart/Dynamic-Capsule.git
cd Dynamic-Capsule
./gradlew assembleDebug testDebugUnitTest
```

---

## 📄 Licence / License

Distribué sous la **Licence MIT**. Consultez le fichier [LICENSE](LICENSE) pour plus d'informations.
