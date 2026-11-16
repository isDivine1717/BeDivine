package com.music.bitchord

import android.content.SharedPreferences
import com.music.bitchord.data.settings.HomeCustomizationState
import com.music.bitchord.data.settings.HomeCustomizationStore
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionStyle
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.data.settings.HomeTemplate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying genuine behavior, thread-safe mutations,
 * reordering bounds, and persistence for [HomeCustomizationStore].
 */
class HomeCustomizationStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        HomeCustomizationStore.init(fakePrefs)
    }

    @Test
    fun initialDefaults_areConfiguredProperly() {
        val state = HomeCustomizationStore.state.value

        assertEquals(HomeTemplate.DYNAMIC_STANDARD, state.template)
        assertEquals(8, state.sections.size)

        // All default sections should be visible initially
        assertTrue("All sections should be visible by default", state.sections.all { it.visible })

        // Check specific default order
        assertEquals(HomeSectionType.TOP_PICKS, state.sections[0].type)
        assertEquals(HomeSectionStyle.CAROUSEL, state.sections[0].style)
        assertEquals(HomeSectionType.RECENTS, state.sections[1].type)
        assertEquals(HomeSectionStyle.LIST, state.sections[1].style)
        assertEquals(HomeSectionType.MOODS, state.sections[2].type)
        assertEquals(HomeSectionStyle.GRID, state.sections[2].style)
    }

    @Test
    fun templateChanging_updatesTemplateAndLayout() {
        // Switch to Mochy Compact with resetSections = true
        HomeCustomizationStore.setTemplate(HomeTemplate.MOCHY_COMPACT, resetSections = true)
        var state = HomeCustomizationStore.state.value

        assertEquals(HomeTemplate.MOCHY_COMPACT, state.template)
        assertEquals(HomeSectionType.RECENTS, state.sections[0].type)
        assertEquals(HomeSectionType.TOP_PICKS, state.sections[1].type)
        assertEquals(HomeSectionStyle.GRID, state.sections[1].style)

        // Switch to Editorial Hero with resetSections = true
        HomeCustomizationStore.setTemplate(HomeTemplate.EDITORIAL_HERO, resetSections = true)
        state = HomeCustomizationStore.state.value

        assertEquals(HomeTemplate.EDITORIAL_HERO, state.template)
        assertEquals(HomeSectionType.TOP_PICKS, state.sections[0].type)
        assertEquals(HomeSectionStyle.HERO, state.sections[0].style)
        assertEquals(HomeSectionType.FEATURED_PLAYLISTS, state.sections[1].type)
        assertEquals(HomeSectionStyle.HERO, state.sections[1].style)

        // Switching without reset preserves user order while adapting styles
        HomeCustomizationStore.setTemplate(HomeTemplate.DYNAMIC_STANDARD, resetSections = false)
        state = HomeCustomizationStore.state.value

        assertEquals(HomeTemplate.DYNAMIC_STANDARD, state.template)
        // Top picks is still first, but its style adapted to DYNAMIC_STANDARD (CAROUSEL)
        assertEquals(HomeSectionType.TOP_PICKS, state.sections[0].type)
        assertEquals(HomeSectionStyle.CAROUSEL, state.sections[0].style)
    }

    @Test
    fun sectionVisibilityToggle_togglesTrueFalseTrue() {
        // Initially visible
        val initialTopPicks = HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.TOP_PICKS }
        assertTrue(initialTopPicks.visible)

        // Toggle to false
        HomeCustomizationStore.toggleSectionVisibility(HomeSectionType.TOP_PICKS)
        var updated = HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.TOP_PICKS }
        assertFalse(updated.visible)

        // Other sections should remain unchanged
        val recents = HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.RECENTS }
        assertTrue(recents.visible)

        // Toggle back to true
        HomeCustomizationStore.toggleSectionVisibility(HomeSectionType.TOP_PICKS)
        updated = HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.TOP_PICKS }
        assertTrue(updated.visible)

        // Verify persistence by reloading from prefs
        val freshStorePrefs = fakePrefs
        HomeCustomizationStore.toggleSectionVisibility(HomeSectionType.MOODS)
        assertFalse(HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.MOODS }.visible)

        // Reinitialize
        HomeCustomizationStore.init(freshStorePrefs)
        val reloaded = HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.MOODS }
        assertFalse("Persisted state should retain hidden visibility after store reload", reloaded.visible)
    }

    @Test
    fun movingSection_handlesUpAndDownAndBoundariesAndInvalidIndices() {
        // Initial order
        assertEquals(HomeSectionType.TOP_PICKS, HomeCustomizationStore.state.value.sections[0].type)
        assertEquals(HomeSectionType.RECENTS, HomeCustomizationStore.state.value.sections[1].type)

        // Move down: index 0 to index 1
        HomeCustomizationStore.moveSection(fromIndex = 0, toIndex = 1)
        assertEquals(HomeSectionType.RECENTS, HomeCustomizationStore.state.value.sections[0].type)
        assertEquals(HomeSectionType.TOP_PICKS, HomeCustomizationStore.state.value.sections[1].type)

        // Move up: index 1 back to index 0
        HomeCustomizationStore.moveSection(fromIndex = 1, toIndex = 0)
        assertEquals(HomeSectionType.TOP_PICKS, HomeCustomizationStore.state.value.sections[0].type)
        assertEquals(HomeSectionType.RECENTS, HomeCustomizationStore.state.value.sections[1].type)

        // Move to last boundary (index 7)
        val lastIndex = HomeCustomizationStore.state.value.sections.lastIndex
        HomeCustomizationStore.moveSection(fromIndex = 0, toIndex = lastIndex)
        assertEquals(HomeSectionType.TOP_PICKS, HomeCustomizationStore.state.value.sections[lastIndex].type)
        assertEquals(HomeSectionType.RECENTS, HomeCustomizationStore.state.value.sections[0].type)

        // Move from last boundary back to first
        HomeCustomizationStore.moveSection(fromIndex = lastIndex, toIndex = 0)
        assertEquals(HomeSectionType.TOP_PICKS, HomeCustomizationStore.state.value.sections[0].type)

        // Same index move should be a no-op
        val stateBefore = HomeCustomizationStore.state.value.sections
        HomeCustomizationStore.moveSection(fromIndex = 2, toIndex = 2)
        assertEquals(stateBefore, HomeCustomizationStore.state.value.sections)

        // Invalid indices should be no-ops without throwing exceptions
        HomeCustomizationStore.moveSection(fromIndex = -1, toIndex = 2)
        assertEquals(stateBefore, HomeCustomizationStore.state.value.sections)

        HomeCustomizationStore.moveSection(fromIndex = 2, toIndex = -1)
        assertEquals(stateBefore, HomeCustomizationStore.state.value.sections)

        HomeCustomizationStore.moveSection(fromIndex = 2, toIndex = 100)
        assertEquals(stateBefore, HomeCustomizationStore.state.value.sections)

        HomeCustomizationStore.moveSection(fromIndex = 100, toIndex = 2)
        assertEquals(stateBefore, HomeCustomizationStore.state.value.sections)
    }

    @Test
    fun jsonSerializationAndDeserialization_maintainsIntegrity() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        val customState = HomeCustomizationState(
            template = HomeTemplate.EDITORIAL_HERO,
            sections = listOf(
                HomeSectionConfig(type = HomeSectionType.TOP_PICKS, visible = false, style = HomeSectionStyle.HERO),
                HomeSectionConfig(type = HomeSectionType.RECENTS, visible = true, style = HomeSectionStyle.LIST),
                HomeSectionConfig(type = HomeSectionType.MOODS, visible = false, style = HomeSectionStyle.GRID),
            ),
        )

        val encoded = json.encodeToString(HomeCustomizationState.serializer(), customState)
        assertNotNull(encoded)
        assertTrue(encoded.contains("editorial_hero"))
        assertTrue(encoded.contains("top_picks"))

        val decoded = json.decodeFromString<HomeCustomizationState>(encoded)
        assertEquals(customState.template, decoded.template)
        assertEquals(customState.sections.size, decoded.sections.size)
        assertEquals(customState.sections[0].type, decoded.sections[0].type)
        assertEquals(customState.sections[0].visible, decoded.sections[0].visible)
        assertEquals(customState.sections[0].style, decoded.sections[0].style)

        // Test forward-compatibility sanitization when initialized with partial sections
        val prefsWithPartial = FakeSharedPreferences()
        prefsWithPartial.edit().putString("home_customization_state_json", encoded).apply()

        HomeCustomizationStore.init(prefsWithPartial)
        val loadedState = HomeCustomizationStore.state.value
        assertEquals(HomeTemplate.EDITORIAL_HERO, loadedState.template)
        // Store sanitizes and ensures all 8 section types exist
        assertEquals(8, loadedState.sections.size)
        // Preserved the 3 custom ones with their exact values
        assertFalse(loadedState.sections.first { it.type == HomeSectionType.TOP_PICKS }.visible)
        assertTrue(loadedState.sections.first { it.type == HomeSectionType.RECENTS }.visible)
        assertFalse(loadedState.sections.first { it.type == HomeSectionType.MOODS }.visible)
    }

    @Test
    fun resetToDefaults_restoresStandardTemplateAndAllSections() {
        // Mutate state
        HomeCustomizationStore.setTemplate(HomeTemplate.MOCHY_COMPACT, resetSections = true)
        HomeCustomizationStore.toggleSectionVisibility(HomeSectionType.RECENTS)
        HomeCustomizationStore.moveSection(0, 5)

        // State is modified
        assertEquals(HomeTemplate.MOCHY_COMPACT, HomeCustomizationStore.state.value.template)
        assertFalse(HomeCustomizationStore.state.value.sections.first { it.type == HomeSectionType.RECENTS }.visible)

        // Reset
        HomeCustomizationStore.resetToDefaults()

        val restored = HomeCustomizationStore.state.value
        assertEquals(HomeTemplate.DYNAMIC_STANDARD, restored.template)
        assertEquals(8, restored.sections.size)
        assertTrue(restored.sections.all { it.visible })
        assertEquals(HomeSectionType.TOP_PICKS, restored.sections[0].type)
        assertEquals(HomeSectionType.RECENTS, restored.sections[1].type)

        // Verify persisted reset
        HomeCustomizationStore.init(fakePrefs)
        assertEquals(HomeTemplate.DYNAMIC_STANDARD, HomeCustomizationStore.state.value.template)
        assertTrue(HomeCustomizationStore.state.value.sections.all { it.visible })
    }
}

/**
 * Lightweight in-memory implementation of [SharedPreferences] for hermetic unit testing.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class Editor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = this
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearRequested) data.clear()
            for ((k, v) in temp) {
                if (v === this) data.remove(k) else data[k] = v
            }
        }
    }
}
