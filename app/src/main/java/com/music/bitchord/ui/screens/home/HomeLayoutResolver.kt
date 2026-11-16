package com.music.bitchord.ui.screens.home

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.settings.HomeCustomizationState
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.data.settings.HomeTemplate

/**
 * Pure business logic for resolving, filtering, ordering, and mapping
 * home screen sections and dynamic shelves according to user customization.
 */
object HomeLayoutResolver {

    /**
     * Filters out hidden sections, returning only visible sections in configured order.
     */
    fun resolveActiveSections(sections: List<HomeSectionConfig>): List<HomeSectionConfig> {
        return sections.filter { it.visible }
    }

    /**
     * Maps an incoming remote [HomeShelf] from YouTube Music to a corresponding [HomeSectionType].
     */
    fun mapShelfToSectionType(shelf: HomeShelf): HomeSectionType {
        val title = shelf.title.trim().lowercase()
        val subtitle = shelf.subtitle.trim().lowercase()

        return when {
            shelf.layout == ShelfLayout.LIST ||
                title.equals("recents", ignoreCase = true) ||
                title.equals("recently played", ignoreCase = true) ||
                title.equals("gần đây", ignoreCase = true) ||
                title.equals("escuchado recientemente", ignoreCase = true) ||
                title.equals("historial", ignoreCase = true) -> HomeSectionType.RECENTS

            shelf.layout == ShelfLayout.VIDEO ||
                title.contains("video", ignoreCase = true) ||
                title.contains("shorts", ignoreCase = true) ||
                subtitle.contains("video", ignoreCase = true) -> HomeSectionType.RECOMMENDED_VIDEOS

            title.contains("mix", ignoreCase = true) ||
                title.contains("mezcla", ignoreCase = true) ||
                title.contains("radio", ignoreCase = true) ||
                title.contains("my mix", ignoreCase = true) -> HomeSectionType.MIXED_FOR_YOU

            title.contains("listen again", ignoreCase = true) ||
                title.contains("escuchar de nuevo", ignoreCase = true) ||
                title.contains("forgotten", ignoreCase = true) ||
                title.contains("favoritos olvidados", ignoreCase = true) ||
                title.contains("giai điệu quen thuộc", ignoreCase = true) ||
                title.contains("repeat", ignoreCase = true) -> HomeSectionType.LISTEN_AGAIN

            title.contains("chart", ignoreCase = true) ||
                title.contains("bảng xếp hạng", ignoreCase = true) ||
                title.contains("new release", ignoreCase = true) ||
                title.contains("lanzamiento", ignoreCase = true) ||
                title.contains("novedades", ignoreCase = true) ||
                title.contains("bản phát hành mới", ignoreCase = true) ||
                title.contains("trending", ignoreCase = true) ||
                title.contains("thịnh hành", ignoreCase = true) ||
                title.contains("hits", ignoreCase = true) ||
                title.contains("éxitos", ignoreCase = true) -> HomeSectionType.NEW_RELEASES_CHARTS

            title.contains("playlist", ignoreCase = true) ||
                title.contains("lista", ignoreCase = true) ||
                title.contains("community", ignoreCase = true) ||
                title.contains("comunidad", ignoreCase = true) ||
                title.contains("for you", ignoreCase = true) ||
                title.contains("para ti", ignoreCase = true) ||
                title.contains("featured", ignoreCase = true) ||
                title.contains("destacado", ignoreCase = true) ||
                title.contains("library", ignoreCase = true) -> HomeSectionType.FEATURED_PLAYLISTS

            else -> HomeSectionType.FEATURED_PLAYLISTS
        }
    }

    /**
     * Groups a collection of remote shelves by their mapped [HomeSectionType].
     */
    fun groupShelvesBySectionType(shelves: List<HomeShelf>): Map<HomeSectionType, List<HomeShelf>> {
        val map = mutableMapOf<HomeSectionType, MutableList<HomeShelf>>()
        for (shelf in shelves) {
            val type = mapShelfToSectionType(shelf)
            map.getOrPut(type) { mutableListOf() }.add(shelf)
        }
        return map
    }

    /**
     * Returns true if the given section type corresponds to remote shelves from the feed.
     */
    fun isRemoteShelfSection(type: HomeSectionType): Boolean {
        return when (type) {
            HomeSectionType.MIXED_FOR_YOU,
            HomeSectionType.LISTEN_AGAIN,
            HomeSectionType.RECOMMENDED_VIDEOS,
            HomeSectionType.FEATURED_PLAYLISTS,
            HomeSectionType.NEW_RELEASES_CHARTS -> true
            else -> false
        }
    }
}
