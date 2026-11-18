package com.dynamiccapsule.core.system

import com.dynamiccapsule.core.model.AnimationOpenStyle
import com.dynamiccapsule.core.model.AppProjectInfo
import com.dynamiccapsule.core.model.AppThemeMode
import com.dynamiccapsule.core.model.CameraCutoutPlacement
import com.dynamiccapsule.core.model.CapsuleAction
import com.dynamiccapsule.core.model.CapsuleGestureType
import com.dynamiccapsule.core.model.DynamicCardType
import com.dynamiccapsule.core.model.MiniCapsuleEventType
import com.dynamiccapsule.core.model.ReleaseNotesCatalog
import com.dynamiccapsule.core.model.isEnglishLang
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.update.GitHubUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateAndI18nTest {

    @Test
    fun `semantic version comparison accurately detects newer GitHub releases`() {
        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("1.0.0", "1.0.1"))
        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("1.0.0", "v1.1.0"))
        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("1.2.3", "2.0.0"))
        assertTrue(GitHubUpdateManager.isRemoteVersionNewer("1.0", "1.0.1"))

        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("1.0.0", "1.0.0"))
        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("1.0.0", "v1.0.0"))
        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("1.2.0", "1.1.9"))
        assertFalse(GitHubUpdateManager.isRemoteVersionNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `bilingual FR and EN localization helpers and all enums expose non-empty distinct labels`() {
        assertTrue(isEnglishLang("en"))
        assertTrue(isEnglishLang("en-US"))
        assertFalse(isEnglishLang("fr"))
        assertFalse(isEnglishLang("fr-FR"))

        assertEquals("Français", tr("fr", "Français", "English"))
        assertEquals("English", tr("en", "Français", "English"))

        DynamicCardType.entries.forEach { card ->
            assertTrue(card.title("fr").isNotBlank())
            assertTrue(card.title("en").isNotBlank())
            assertNotEquals(card.title("fr"), card.title("en"))
            assertNotEquals(card.subtitle("fr"), card.subtitle("en"))
        }

        MiniCapsuleEventType.entries.forEach { event ->
            assertTrue(event.title("fr").isNotBlank())
            assertTrue(event.title("en").isNotBlank())
            assertNotEquals(event.description("fr"), event.description("en"))
        }

        CapsuleGestureType.entries.forEach { gesture ->
            assertNotEquals(gesture.title("fr"), gesture.title("en"))
        }

        CameraCutoutPlacement.entries.forEach { placement ->
            assertNotEquals(placement.label("fr"), placement.label("en"))
        }

        AnimationOpenStyle.entries.forEach { style ->
            assertNotEquals(style.label("fr"), style.label("en"))
        }

        AppThemeMode.entries.forEach { theme ->
            assertNotEquals(theme.label("fr"), theme.label("en"))
        }

        val action = CapsuleAction.TakeScreenshot
        assertEquals("Capture d'écran", action.title("fr"))
        assertEquals("Take Screenshot", action.title("en"))
    }

    @Test
    fun `project metadata and release notes catalog are properly configured`() {
        assertEquals("https://github.com/MrSchnnurbart/Dynamic-Capsule", AppProjectInfo.GITHUB_REPO_URL)
        assertTrue(AppProjectInfo.MIT_LICENSE_TEXT.contains("MIT License"))
        assertTrue(AppProjectInfo.MIT_LICENSE_TEXT.contains("MrSchnnurbart"))

        val current = ReleaseNotesCatalog.currentReleaseNote
        assertEquals(AppProjectInfo.VERSION_NAME, current.versionName)
        assertEquals(AppProjectInfo.VERSION_CODE, current.versionCode)
        assertTrue(current.highlights("fr").isNotEmpty())
        assertTrue(current.highlights("en").isNotEmpty())
    }
}
