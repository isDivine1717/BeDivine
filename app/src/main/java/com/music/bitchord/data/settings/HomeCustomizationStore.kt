package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Interface contract for Home Customization persistence and state management.
 */
interface IHomeCustomizationStore {
    val state: StateFlow<HomeCustomizationState>
    fun setTemplate(template: HomeTemplate)
    fun updateSections(sections: List<HomeSectionConfig>)
    fun toggleSectionVisibility(type: HomeSectionType)
    fun moveSection(fromIndex: Int, toIndex: Int)
    fun resetToDefaults()
}

/**
 * Thread-safe persistent reactive store for BeDivine Home screen customization.
 * Backed by SharedPreferences("bedivine_home_customization").
 */
object HomeCustomizationStore : IHomeCustomizationStore {
    private const val PREFS_NAME = "bedivine_home_customization"
    private const val KEY_STATE_JSON = "home_customization_state_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val lock = Any()
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(HomeCustomizationState())
    override val state: StateFlow<HomeCustomizationState> = _state.asStateFlow()

    /**
     * Initializes the store with the Android [Context].
     */
    fun init(context: Context) {
        synchronized(lock) {
            if (prefs != null) return
            val sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            init(sharedPreferences)
        }
    }

    /**
     * Initializes the store with a specific [SharedPreferences] instance.
     * Useful for testing and manual DI.
     */
    fun init(sharedPreferences: SharedPreferences) {
        synchronized(lock) {
            this.prefs = sharedPreferences
            loadFromPrefsLocked()
        }
    }

    /**
     * Ensures store initialization before state mutations if context is available.
     */
    fun ensureInitialized(context: Context) {
        synchronized(lock) {
            if (prefs == null) init(context)
        }
    }

    /**
     * Updates the active home template.
     * If [resetSections] is true, resets sections to the template's default layout.
     * Otherwise, retains the user's ordered sections and visibility, adapting default styles for each type.
     */
    fun setTemplate(template: HomeTemplate, resetSections: Boolean = false) {
        synchronized(lock) {
            val current = _state.value
            val newSections = if (resetSections) {
                template.defaultSections()
            } else {
                // Adapt existing sections to template defaults where applicable while preserving order & visibility
                val defaultStyles = template.defaultSections().associate { it.type to it.style }
                current.sections.map { config ->
                    val adaptedStyle = defaultStyles[config.type] ?: config.style
                    config.copy(style = adaptedStyle)
                }
            }
            saveStateLocked(current.copy(template = template, sections = newSections))
        }
    }

    override fun setTemplate(template: HomeTemplate) {
        setTemplate(template, resetSections = false)
    }

    /**
     * Updates the full section configuration list.
     */
    override fun updateSections(sections: List<HomeSectionConfig>) {
        synchronized(lock) {
            val sanitized = sanitizeSections(sections)
            val current = _state.value
            saveStateLocked(current.copy(sections = sanitized))
        }
    }

    /**
     * Toggles visibility for a specific section type.
     */
    override fun toggleSectionVisibility(type: HomeSectionType) {
        synchronized(lock) {
            val current = _state.value
            var found = false
            val updated = current.sections.map { config ->
                if (config.type == type) {
                    found = true
                    config.copy(visible = !config.visible)
                } else {
                    config
                }
            }.toMutableList()

            // If the section wasn't in the list for any reason, append it toggled
            if (!found) {
                updated.add(HomeSectionConfig(type = type, visible = true))
            }

            saveStateLocked(current.copy(sections = updated))
        }
    }

    /**
     * Moves a section from [fromIndex] to [toIndex].
     * Bounds check ensures no crash on invalid indices.
     */
    override fun moveSection(fromIndex: Int, toIndex: Int) {
        synchronized(lock) {
            val current = _state.value
            val sections = current.sections
            if (fromIndex == toIndex) return
            if (fromIndex !in sections.indices || toIndex !in sections.indices) return

            val mutable = sections.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)

            saveStateLocked(current.copy(sections = mutable))
        }
    }

    /**
     * Resets customization to default template and section layout.
     */
    override fun resetToDefaults() {
        synchronized(lock) {
            val defaultTemplate = HomeTemplate.DYNAMIC_STANDARD
            val defaultState = HomeCustomizationState(
                template = defaultTemplate,
                sections = defaultTemplate.defaultSections(),
            )
            saveStateLocked(defaultState)
        }
    }

    private fun loadFromPrefsLocked() {
        val sp = prefs ?: return
        val rawJson = sp.getString(KEY_STATE_JSON, null)
        val loadedState = if (!rawJson.isNullOrBlank()) {
            runCatching {
                json.decodeFromString<HomeCustomizationState>(rawJson)
            }.getOrNull()
        } else {
            null
        }

        val finalState = if (loadedState != null) {
            // Forward compatibility: guarantee all known section types are present
            val completeSections = sanitizeSections(loadedState.sections)
            loadedState.copy(sections = completeSections)
        } else {
            HomeCustomizationState()
        }

        _state.value = finalState
    }

    private fun saveStateLocked(newState: HomeCustomizationState) {
        _state.value = newState
        val sp = prefs ?: return
        runCatching {
            val jsonString = json.encodeToString(HomeCustomizationState.serializer(), newState)
            sp.edit().putString(KEY_STATE_JSON, jsonString).apply()
        }
    }

    /**
     * Ensures all known [HomeSectionType] entries exist in the layout, appending any missing ones.
     */
    private fun sanitizeSections(input: List<HomeSectionConfig>): List<HomeSectionConfig> {
        val presentTypes = input.map { it.type }.toSet()
        val missing = HomeSectionType.entries.filter { it !in presentTypes }.map { type ->
            HomeSectionConfig(type = type, visible = true)
        }
        return input + missing
    }
}
