package com.music.bitchord

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionStyle
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.data.settings.HomeTemplate
import com.music.bitchord.ui.screens.home.HomeLayoutResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Home Template Rendering and Dynamic Section Layout:
 * 1. Template Configurations validity and distinctness across all 3 templates.
 * 2. Section Filtering (visibility toggles).
 * 3. Section Ordering (user-configured reordering).
 * 4. Dynamic shelf mapping from remote feeds to section types.
 */
class HomeTemplateRenderingTest {

    @Test
    fun templateConfigurations_areValidAndDistinct() {
        val standardSections = HomeTemplate.DYNAMIC_STANDARD.defaultSections()
        val mochySections = HomeTemplate.MOCHY_COMPACT.defaultSections()
        val editorialSections = HomeTemplate.EDITORIAL_HERO.defaultSections()

        // 1. All templates have 8 sections
        assertEquals(8, standardSections.size)
        assertEquals(8, mochySections.size)
        assertEquals(8, editorialSections.size)

        // 2. All section types are present in each template
        val allTypes = HomeSectionType.entries.toSet()
        assertEquals(allTypes, standardSections.map { it.type }.toSet())
        assertEquals(allTypes, mochySections.map { it.type }.toSet())
        assertEquals(allTypes, editorialSections.map { it.type }.toSet())

        // 3. Configurations are distinct between templates
        assertNotEquals(standardSections, mochySections)
        assertNotEquals(standardSections, editorialSections)
        assertNotEquals(mochySections, editorialSections)

        // 4. Verify template-specific signature characteristics
        // Standard template starts with TOP_PICKS in CAROUSEL
        assertEquals(HomeSectionType.TOP_PICKS, standardSections[0].type)
        assertEquals(HomeSectionStyle.CAROUSEL, standardSections[0].style)

        // Mochy template starts with RECENTS in LIST, and TOP_PICKS is GRID
        assertEquals(HomeSectionType.RECENTS, mochySections[0].type)
        assertEquals(HomeSectionStyle.LIST, mochySections[0].style)
        val mochyTopPicks = mochySections.first { it.type == HomeSectionType.TOP_PICKS }
        assertEquals(HomeSectionStyle.GRID, mochyTopPicks.style)

        // Editorial Hero starts with TOP_PICKS in HERO style and FEATURED_PLAYLISTS in HERO style
        assertEquals(HomeSectionType.TOP_PICKS, editorialSections[0].type)
        assertEquals(HomeSectionStyle.HERO, editorialSections[0].style)
        assertEquals(HomeSectionType.FEATURED_PLAYLISTS, editorialSections[1].type)
        assertEquals(HomeSectionStyle.HERO, editorialSections[1].style)
    }

    @Test
    fun sectionFiltering_excludesHiddenSectionsFromActiveList() {
        val initialSections = HomeTemplate.DYNAMIC_STANDARD.defaultSections()
        assertEquals(8, HomeLayoutResolver.resolveActiveSections(initialSections).size)

        // Hide RECENTS and MOODS
        val customizedSections = initialSections.map { config ->
            when (config.type) {
                HomeSectionType.RECENTS, HomeSectionType.MOODS -> config.copy(visible = false)
                else -> config
            }
        }

        val active = HomeLayoutResolver.resolveActiveSections(customizedSections)

        // 6 visible sections remain
        assertEquals(6, active.size)
        assertFalse(active.any { it.type == HomeSectionType.RECENTS })
        assertFalse(active.any { it.type == HomeSectionType.MOODS })
        assertTrue(active.all { it.visible })

        // Remaining items maintain their relative order
        val expectedTypes = listOf(
            HomeSectionType.TOP_PICKS,
            HomeSectionType.MIXED_FOR_YOU,
            HomeSectionType.LISTEN_AGAIN,
            HomeSectionType.RECOMMENDED_VIDEOS,
            HomeSectionType.FEATURED_PLAYLISTS,
            HomeSectionType.NEW_RELEASES_CHARTS,
        )
        assertEquals(expectedTypes, active.map { it.type })
    }

    @Test
    fun sectionOrdering_preservesExactConfiguredOrder() {
        // Construct an arbitrary reordered configuration:
        // NEW_RELEASES_CHARTS first, then LISTEN_AGAIN, then MOODS, then TOP_PICKS
        val reordered = listOf(
            HomeSectionConfig(type = HomeSectionType.NEW_RELEASES_CHARTS, visible = true),
            HomeSectionConfig(type = HomeSectionType.LISTEN_AGAIN, visible = true),
            HomeSectionConfig(type = HomeSectionType.RECENTS, visible = false), // Hidden
            HomeSectionConfig(type = HomeSectionType.MOODS, visible = true),
            HomeSectionConfig(type = HomeSectionType.TOP_PICKS, visible = true),
            HomeSectionConfig(type = HomeSectionType.MIXED_FOR_YOU, visible = false), // Hidden
            HomeSectionConfig(type = HomeSectionType.FEATURED_PLAYLISTS, visible = true),
            HomeSectionConfig(type = HomeSectionType.RECOMMENDED_VIDEOS, visible = true),
        )

        val active = HomeLayoutResolver.resolveActiveSections(reordered)

        // 6 visible sections in the exact order requested
        assertEquals(6, active.size)
        val expectedOrder = listOf(
            HomeSectionType.NEW_RELEASES_CHARTS,
            HomeSectionType.LISTEN_AGAIN,
            HomeSectionType.MOODS,
            HomeSectionType.TOP_PICKS,
            HomeSectionType.FEATURED_PLAYLISTS,
            HomeSectionType.RECOMMENDED_VIDEOS,
        )
        assertEquals(expectedOrder, active.map { it.type })
    }

    @Test
    fun dynamicShelfMapping_mapsShelvesToCorrectSectionTypes() {
        val shelves = listOf(
            HomeShelf(title = "Recents", items = emptyList(), layout = ShelfLayout.LIST),
            HomeShelf(title = "Recently played", items = emptyList()),
            HomeShelf(title = "Mixed for you", items = emptyList()),
            HomeShelf(title = "My Daily Mix 1", items = emptyList()),
            HomeShelf(title = "Listen again", items = emptyList()),
            HomeShelf(title = "Forgotten favorites", items = emptyList()),
            HomeShelf(title = "Recommended music videos", items = emptyList(), layout = ShelfLayout.VIDEO),
            HomeShelf(title = "Shorts", items = emptyList()),
            HomeShelf(title = "Charts", items = emptyList()),
            HomeShelf(title = "New releases", items = emptyList()),
            HomeShelf(title = "Trending", items = emptyList()),
            HomeShelf(title = "Hits today", items = emptyList()),
            HomeShelf(title = "From the community", items = emptyList()),
            HomeShelf(title = "Featured Playlists", items = emptyList()),
        )

        val grouped = HomeLayoutResolver.groupShelvesBySectionType(shelves)

        // 1. Recents mapped
        val recents = grouped[HomeSectionType.RECENTS].orEmpty()
        assertEquals(2, recents.size)
        assertTrue(recents.any { it.title == "Recents" })
        assertTrue(recents.any { it.title == "Recently played" })

        // 2. Mixed for you mapped
        val mixed = grouped[HomeSectionType.MIXED_FOR_YOU].orEmpty()
        assertEquals(2, mixed.size)
        assertTrue(mixed.any { it.title == "Mixed for you" })
        assertTrue(mixed.any { it.title == "My Daily Mix 1" })

        // 3. Listen again mapped
        val listenAgain = grouped[HomeSectionType.LISTEN_AGAIN].orEmpty()
        assertEquals(2, listenAgain.size)
        assertTrue(listenAgain.any { it.title == "Listen again" })
        assertTrue(listenAgain.any { it.title == "Forgotten favorites" })

        // 4. Recommended videos mapped
        val videos = grouped[HomeSectionType.RECOMMENDED_VIDEOS].orEmpty()
        assertEquals(2, videos.size)
        assertTrue(videos.any { it.title == "Recommended music videos" })
        assertTrue(videos.any { it.title == "Shorts" })

        // 5. New releases & charts mapped
        val charts = grouped[HomeSectionType.NEW_RELEASES_CHARTS].orEmpty()
        assertEquals(4, charts.size)
        assertTrue(charts.any { it.title == "Charts" })
        assertTrue(charts.any { it.title == "New releases" })
        assertTrue(charts.any { it.title == "Trending" })
        assertTrue(charts.any { it.title == "Hits today" })

        // 6. Featured playlists mapped
        val playlists = grouped[HomeSectionType.FEATURED_PLAYLISTS].orEmpty()
        assertEquals(2, playlists.size)
        assertTrue(playlists.any { it.title == "From the community" })
        assertTrue(playlists.any { it.title == "Featured Playlists" })
    }

    @Test
    fun dynamicShelfMapping_handlesSpanishAndMultilingualTitles() {
        val spanishShelves = listOf(
            HomeShelf(title = "Escuchado recientemente", items = emptyList()),
            HomeShelf(title = "Mezclas personalizadas", items = emptyList()),
            HomeShelf(title = "Escuchar de nuevo", items = emptyList()),
            HomeShelf(title = "Videos recomendados", items = emptyList()),
            HomeShelf(title = "Nuevos lanzamientos", items = emptyList()),
            HomeShelf(title = "Listas de éxitos", items = emptyList()),
            HomeShelf(title = "Listas destacadas para ti", items = emptyList()),
        )

        assertEquals(HomeSectionType.RECENTS, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[0]))
        assertEquals(HomeSectionType.MIXED_FOR_YOU, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[1]))
        assertEquals(HomeSectionType.LISTEN_AGAIN, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[2]))
        assertEquals(HomeSectionType.RECOMMENDED_VIDEOS, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[3]))
        assertEquals(HomeSectionType.NEW_RELEASES_CHARTS, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[4]))
        assertEquals(HomeSectionType.NEW_RELEASES_CHARTS, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[5]))
        assertEquals(HomeSectionType.FEATURED_PLAYLISTS, HomeLayoutResolver.mapShelfToSectionType(spanishShelves[6]))
    }

    @Test
    fun isRemoteShelfSection_identifiesRemoteTypesProperly() {
        // Local/Special sections
        assertFalse(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.TOP_PICKS))
        assertFalse(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.RECENTS))
        assertFalse(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.MOODS))

        // Remote feed sections
        assertTrue(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.MIXED_FOR_YOU))
        assertTrue(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.LISTEN_AGAIN))
        assertTrue(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.RECOMMENDED_VIDEOS))
        assertTrue(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.FEATURED_PLAYLISTS))
        assertTrue(HomeLayoutResolver.isRemoteShelfSection(HomeSectionType.NEW_RELEASES_CHARTS))
    }
}
