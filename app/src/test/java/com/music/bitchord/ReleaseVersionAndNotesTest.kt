package com.music.bitchord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseVersionAndNotesTest {

    @Test
    fun rootBuildGradle_containsBumpedVersion() {
        val rootDir = findProjectRoot()
        val buildGradleFile = File(rootDir, "build.gradle.kts")
        assertTrue("build.gradle.kts must exist", buildGradleFile.exists())

        val content = buildGradleFile.readText()
        val versionCodeRegex = Regex("""extra\["bitchordVersionCode"\]\s*=\s*(\d+)""")
        val versionNameRegex = Regex("""extra\["bitchordVersionName"\]\s*=\s*"([^"]+)"""")

        val codeMatch = versionCodeRegex.find(content)
        val nameMatch = versionNameRegex.find(content)

        assertTrue("bitchordVersionCode must be present", codeMatch != null)
        assertTrue("bitchordVersionName must be present", nameMatch != null)

        val versionCode = codeMatch!!.groupValues[1].toInt()
        val versionName = nameMatch!!.groupValues[1]

        assertEquals("bitchordVersionCode must be incremented to 44", 44, versionCode)
        assertEquals("bitchordVersionName must be 1.0.4", "1.0.4", versionName)
    }

    @Test
    fun releaseNotes_bothFilesExistAndMatchExactly() {
        val rootDir = findProjectRoot()
        val rootNotesFile = File(rootDir, "RELEASE_NOTES.md")
        val assetNotesFile = File(rootDir, "app/src/main/assets/RELEASE_NOTES.md")

        assertTrue("Root RELEASE_NOTES.md must exist", rootNotesFile.exists())
        assertTrue("Assets RELEASE_NOTES.md must exist", assetNotesFile.exists())

        val rootContent = rootNotesFile.readText().trim()
        val assetContent = assetNotesFile.readText().trim()

        assertEquals(
            "Root RELEASE_NOTES.md and app/src/main/assets/RELEASE_NOTES.md must match byte-for-byte",
            rootContent,
            assetContent
        )
    }

    @Test
    fun releaseNotes_containSpanishSectionForV104WithAllRequirements() {
        val rootDir = findProjectRoot()
        val notesFile = File(rootDir, "RELEASE_NOTES.md")
        val content = notesFile.readText()

        // Must start with v1.0.4 header
        assertTrue(
            "Release notes must feature v1.0.4 at the top",
            content.contains("## ✨ Novedades en BeDivine v1.0.4")
        )

        // Must document 3 templates
        assertTrue("Must mention 3 templates", content.contains("3 Plantillas") || content.contains("plantillas"))
        assertTrue("Must mention Estándar Dinámica", content.contains("Estándar Dinámica"))
        assertTrue("Must mention Mochy Compacto", content.contains("Mochy Compacto"))
        assertTrue("Must mention Editorial Hero", content.contains("Editorial Hero"))

        // Must document customization in settings
        assertTrue("Must mention Personalización", content.contains("Personalización"))
        assertTrue("Must mention order/reordering", content.contains("orden", ignoreCase = true) || content.contains("Reorganización"))
        assertTrue("Must mention visibility", content.contains("visibilidad", ignoreCase = true) || content.contains("Visible"))

        // Must document performance / fluidez
        assertTrue("Must mention 60/120 fps or fluidez", content.contains("60/120") || content.contains("fluidez", ignoreCase = true))

        // Must document Spanish localization
        assertTrue("Must mention Spanish localization", content.contains("español", ignoreCase = true))

        // Must preserve existing notes for older releases
        assertTrue("Must preserve v1.0.3 notes", content.contains("## ✨ Novedades en BeDivine v1.0.3"))
        assertTrue("Must preserve v1.0.2 notes", content.contains("## ✨ Novedades en BeDivine v1.0.2"))
        assertTrue("Must preserve v1.0.1 notes", content.contains("## ✨ Novedades en BeDivine v1.0.1"))
    }

    private fun findProjectRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir ?: File(".")
    }
}
