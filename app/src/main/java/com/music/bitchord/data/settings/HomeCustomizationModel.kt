package com.music.bitchord.data.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual templates available for BeDivine's home screen.
 */
@Serializable
enum class HomeTemplate(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("dynamic_standard")
    DYNAMIC_STANDARD(
        id = "dynamic_standard",
        titleEs = "Estándar Dinámica",
        descEs = "Diseño completo con Top Picks, carruseles y secciones dinámicas",
    ),

    @SerialName("mochy_compact")
    MOCHY_COMPACT(
        id = "mochy_compact",
        titleEs = "Mochy Compacto",
        descEs = "Accesos rápidos en cuadrícula 2x2 y alta densidad de reproducción",
    ),

    @SerialName("editorial_hero")
    EDITORIAL_HERO(
        id = "editorial_hero",
        titleEs = "Editorial Hero",
        descEs = "Cabecera editorial inmersiva con tarjetas destacadas y estilo visual premium",
    );

    /**
     * Default layout of home sections for this template.
     */
    fun defaultSections(): List<HomeSectionConfig> = when (this) {
        DYNAMIC_STANDARD -> listOf(
            HomeSectionConfig(type = HomeSectionType.TOP_PICKS, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.RECENTS, visible = true, style = HomeSectionStyle.LIST),
            HomeSectionConfig(type = HomeSectionType.MOODS, visible = true, style = HomeSectionStyle.GRID),
            HomeSectionConfig(type = HomeSectionType.MIXED_FOR_YOU, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.LISTEN_AGAIN, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.RECOMMENDED_VIDEOS, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.FEATURED_PLAYLISTS, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.NEW_RELEASES_CHARTS, visible = true, style = HomeSectionStyle.CAROUSEL),
        )
        MOCHY_COMPACT -> listOf(
            HomeSectionConfig(type = HomeSectionType.RECENTS, visible = true, style = HomeSectionStyle.LIST),
            HomeSectionConfig(type = HomeSectionType.TOP_PICKS, visible = true, style = HomeSectionStyle.GRID),
            HomeSectionConfig(type = HomeSectionType.MOODS, visible = true, style = HomeSectionStyle.GRID),
            HomeSectionConfig(type = HomeSectionType.MIXED_FOR_YOU, visible = true, style = HomeSectionStyle.LIST),
            HomeSectionConfig(type = HomeSectionType.LISTEN_AGAIN, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.FEATURED_PLAYLISTS, visible = true, style = HomeSectionStyle.GRID),
            HomeSectionConfig(type = HomeSectionType.NEW_RELEASES_CHARTS, visible = true, style = HomeSectionStyle.LIST),
            HomeSectionConfig(type = HomeSectionType.RECOMMENDED_VIDEOS, visible = true, style = HomeSectionStyle.CAROUSEL),
        )
        EDITORIAL_HERO -> listOf(
            HomeSectionConfig(type = HomeSectionType.TOP_PICKS, visible = true, style = HomeSectionStyle.HERO),
            HomeSectionConfig(type = HomeSectionType.FEATURED_PLAYLISTS, visible = true, style = HomeSectionStyle.HERO),
            HomeSectionConfig(type = HomeSectionType.RECENTS, visible = true, style = HomeSectionStyle.LIST),
            HomeSectionConfig(type = HomeSectionType.MIXED_FOR_YOU, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.MOODS, visible = true, style = HomeSectionStyle.GRID),
            HomeSectionConfig(type = HomeSectionType.NEW_RELEASES_CHARTS, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.LISTEN_AGAIN, visible = true, style = HomeSectionStyle.CAROUSEL),
            HomeSectionConfig(type = HomeSectionType.RECOMMENDED_VIDEOS, visible = true, style = HomeSectionStyle.CAROUSEL),
        )
    }
}

/**
 * Types of content sections that can appear on the home screen.
 */
@Serializable
enum class HomeSectionType(
    val id: String,
    val titleEs: String,
    val subtitleEs: String = "",
) {
    @SerialName("top_picks")
    TOP_PICKS(
        id = "top_picks",
        titleEs = "Tus favoritos / Para ti",
        subtitleEs = "Estaciones personalizadas y descubrimientos",
    ),

    @SerialName("recents")
    RECENTS(
        id = "recents",
        titleEs = "Escuchado recientemente",
        subtitleEs = "Tus pistas y álbumes más recientes",
    ),

    @SerialName("moods")
    MOODS(
        id = "moods",
        titleEs = "Estados de ánimo",
        subtitleEs = "Explora música según tu estado de ánimo",
    ),

    @SerialName("mixed_for_you")
    MIXED_FOR_YOU(
        id = "mixed_for_you",
        titleEs = "Mezclas personalizadas",
        subtitleEs = "Mixes hechos para ti",
    ),

    @SerialName("listen_again")
    LISTEN_AGAIN(
        id = "listen_again",
        titleEs = "Escuchar de nuevo",
        subtitleEs = "Música que te encanta volver a escuchar",
    ),

    @SerialName("recommended_videos")
    RECOMMENDED_VIDEOS(
        id = "recommended_videos",
        titleEs = "Videos recomendados",
        subtitleEs = "Videos musicales sugeridos",
    ),

    @SerialName("featured_playlists")
    FEATURED_PLAYLISTS(
        id = "featured_playlists",
        titleEs = "Listas destacadas",
        subtitleEs = "Colecciones y listas de reproducción",
    ),

    @SerialName("new_releases_charts")
    NEW_RELEASES_CHARTS(
        id = "new_releases_charts",
        titleEs = "Nuevos lanzamientos y listas",
        subtitleEs = "Lo más nuevo y éxitos del momento",
    );
}

/**
 * Visual presentation style for a home section.
 */
@Serializable
enum class HomeSectionStyle(
    val id: String,
    val titleEs: String,
) {
    @SerialName("carousel")
    CAROUSEL(id = "carousel", titleEs = "Carrusel"),

    @SerialName("grid")
    GRID(id = "grid", titleEs = "Cuadrícula"),

    @SerialName("list")
    LIST(id = "list", titleEs = "Lista compacta"),

    @SerialName("hero")
    HERO(id = "hero", titleEs = "Destacado Hero"),
}

/**
 * Configuration for a single home section: type, visibility, and layout style.
 */
@Serializable
data class HomeSectionConfig(
    val type: HomeSectionType,
    val visible: Boolean = true,
    val style: HomeSectionStyle = HomeSectionStyle.CAROUSEL,
)

/**
 * Complete state of home screen customization: active template and ordered sections.
 */
@Serializable
data class HomeCustomizationState(
    val template: HomeTemplate = HomeTemplate.DYNAMIC_STANDARD,
    val sections: List<HomeSectionConfig> = defaultSections(HomeTemplate.DYNAMIC_STANDARD),
)

/**
 * Default sections helper for a given template.
 */
fun defaultSections(template: HomeTemplate): List<HomeSectionConfig> = template.defaultSections()
