package com.dynamiccapsule.core.model

import kotlinx.serialization.Serializable

object AppProjectInfo {
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 100
    const val GITHUB_OWNER = "MrSchnnurbart"
    const val GITHUB_REPO = "Dynamic-Capsule"
    const val GITHUB_REPO_URL = "https://github.com/MrSchnnurbart/Dynamic-Capsule"
    const val GITHUB_RELEASES_URL = "https://github.com/MrSchnnurbart/Dynamic-Capsule/releases"
    const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/MrSchnnurbart/Dynamic-Capsule/releases/latest"
    const val LICENSE_NAME = "MIT License"

    val MIT_LICENSE_TEXT = """
        MIT License

        Copyright (c) 2026 MrSchnnurbart

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
    """.trimIndent()
}

fun isEnglishLang(langCode: String): Boolean =
    langCode.trim().startsWith("en", ignoreCase = true)

fun tr(langCode: String, fr: String, en: String): String =
    if (isEnglishLang(langCode)) en else fr

@Serializable
data class VersionReleaseNote(
    val versionName: String,
    val versionCode: Int,
    val dateIso: String,
    val titleFr: String,
    val titleEn: String,
    val highlightsFr: List<String>,
    val highlightsEn: List<String>
) {
    fun title(lang: String): String = tr(lang, titleFr, titleEn)
    fun highlights(lang: String): List<String> = if (isEnglishLang(lang)) highlightsEn else highlightsFr
}

object ReleaseNotesCatalog {
    val history: List<VersionReleaseNote> = listOf(
        VersionReleaseNote(
            versionName = "1.0.0",
            versionCode = 100,
            dateIso = "2026-11-18",
            titleFr = "Dynamic Capsule v1.0.0 — Première Release Open Source",
            titleEn = "Dynamic Capsule v1.0.0 — First Open Source Release",
            highlightsFr = listOf(
                "Style noir profond (#000000) inspiré du Dynamic Island d'Apple avec physique 60/120 fps (« Comme un Fruit » & « Comme un Robot »).",
                "Opacité au repos à 0 % par défaut (aucune barre noire inutile au repos sur les écrans OLED).",
                "Calibrage universel de la Zone Caméra par Cercle Rouge (Gauche, Centre, Droite ou Sous la barre d'état) pour éviter qu'aucune information ne soit cachée par l'objectif.",
                "7 Cartes Dynamiques réorganisables par glisser-déposer (Lecteur média, Appels, Minuteur, Progression, Torche, Volume, Charge) et mode Capsule Scindée.",
                "17 événements système « Mini Capsule » dont le support batterie AirPods (MaterialPods / OpenPods).",
                "5 gestes configurables + « Balayer vers le haut pour fermer » et 16 actions système/IA/QR.",
                "Système de mise à jour automatique connecté à GitHub Releases (MrSchnnurbart/Dynamic-Capsule), guide d'accueil interactif et support complet Français / Anglais."
            ),
            highlightsEn = listOf(
                "Pure #000000 pitch-black Apple Dynamic Island style with 60/120 fps spring physics ('Like a Fruit' & 'Like a Robot').",
                "0% default idle opacity (completely invisible at rest to protect OLED displays while still detecting gestures).",
                "Universal Red Circle Camera Cutout Safe Zone (Left, Center, Right, or Below Status Bar) so no icon or text is ever hidden behind the front camera lens.",
                "7 drag-and-drop reorderable Dynamic Cards (Media Player, Calls, Timer, Live Progress, Flashlight, Volume, Charging) with Split-Capsule mode.",
                "17 system 'Mini Capsule' popups including AirPods battery rings (MaterialPods / OpenPods).",
                "5 independent gestures + 'Swipe up to close' and 16 system/AI/QR actions.",
                "Built-in GitHub Releases auto-updater (MrSchnnurbart/Dynamic-Capsule), interactive welcome guide, and full English / French localization."
            )
        )
    )

    val currentReleaseNote: VersionReleaseNote
        get() = history.first()
}

@Serializable
sealed interface AppUpdateStatus {
    @Serializable
    data object Idle : AppUpdateStatus

    @Serializable
    data object Checking : AppUpdateStatus

    @Serializable
    data class UpToDate(
        val currentVersion: String = AppProjectInfo.VERSION_NAME
    ) : AppUpdateStatus

    @Serializable
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseTitle: String,
        val releaseNotesBody: String,
        val apkDownloadUrl: String?,
        val htmlReleaseUrl: String,
        val publishedAt: String = ""
    ) : AppUpdateStatus

    @Serializable
    data class Downloading(
        val latestVersion: String,
        val progressPercent: Int
    ) : AppUpdateStatus

    @Serializable
    data class ReadyToInstall(
        val latestVersion: String,
        val localApkPath: String
    ) : AppUpdateStatus

    @Serializable
    data class Error(
        val messageFr: String,
        val messageEn: String
    ) : AppUpdateStatus {
        fun message(lang: String): String = tr(lang, messageFr, messageEn)
    }
}
