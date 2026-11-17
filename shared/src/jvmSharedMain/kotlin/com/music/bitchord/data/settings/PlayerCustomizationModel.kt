package com.music.bitchord.data.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual design and layout style of the Now Playing player.
 */
@Serializable
enum class PlayerStyle(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("mochy")
    MOCHY(
        id = "mochy",
        titleEs = "Mochy Inmersivo",
        descEs = "Portada edge-to-edge de borde a borde, desvanecimiento líquido y controles compactos sin espacio muerto",
    ),

    @SerialName("classic")
    CLASSIC(
        id = "classic",
        titleEs = "BitChord Clásico",
        descEs = "Carátula cuadrada contenida y espaciado dinámico tradicional",
    );
}

/**
 * Cover sizing options for the Now Playing screen.
 */
@Serializable
enum class PlayerCoverSize(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("normal")
    NORMAL(
        id = "normal",
        titleEs = "Estándar",
        descEs = "Carátula cuadrada clásica balanceada",
    ),

    @SerialName("expanded_down")
    EXPANDED_DOWN(
        id = "expanded_down",
        titleEs = "Expandida hacia abajo",
        descEs = "Mayor escala y altura aprovechando el espacio inferior",
    ),

    @SerialName("compact")
    COMPACT(
        id = "compact",
        titleEs = "Compacta",
        descEs = "Tamaño reducido para dar máxima prioridad a controles y letras",
    );
}

/**
 * Animated effects for the Now Playing album cover.
 */
@Serializable
enum class PlayerCoverAnimation(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("static")
    STATIC(
        id = "static",
        titleEs = "Estática",
        descEs = "Carátula fija con esquinas suaves y sombra profunda",
    ),

    @SerialName("subtle_pulse")
    SUBTLE_PULSE(
        id = "subtle_pulse",
        titleEs = "Pulso armónico",
        descEs = "Suave respiración rítmica que late mientras suena la música",
    ),

    @SerialName("vinyl")
    VINYL(
        id = "vinyl",
        titleEs = "Modo Vinilo",
        descEs = "Disco de vinilo circular giratorio con textura realista",
    );
}

/**
 * Backdrop visual styles and blur modes for the player.
 */
@Serializable
enum class PlayerBackdropStyle(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("liquid_glass")
    LIQUID_GLASS(
        id = "liquid_glass",
        titleEs = "Liquid Glass",
        descEs = "Fondo dinámico difuminado con colores extraídos de la carátula",
    ),

    @SerialName("gradient")
    GRADIENT(
        id = "gradient",
        titleEs = "Gradiente Suave",
        descEs = "Degradado sutil y sobrio de tonos oscuros",
    ),

    @SerialName("oled_black")
    OLED_BLACK(
        id = "oled_black",
        titleEs = "Negro OLED Puro",
        descEs = "Fondo negro absoluto (0% blur) para máximo contraste y ahorro de batería",
    );
}

/**
 * One-touch quick presets for instant Now Playing configuration.
 */
@Serializable
enum class PlayerPreset(
    val id: String,
    val titleEs: String,
    val descEs: String,
) {
    @SerialName("custom")
    CUSTOM(
        id = "custom",
        titleEs = "Personalizado",
        descEs = "Configuración libre adaptada a tu gusto",
    ),

    @SerialName("minimal_oled")
    MINIMAL_OLED(
        id = "minimal_oled",
        titleEs = "Minimalista OLED",
        descEs = "Fondo negro puro, controles esenciales y máxima eficiencia",
    ),

    @SerialName("immersive")
    IMMERSIVE(
        id = "immersive",
        titleEs = "Inmersivo Cine",
        descEs = "Portada expandida, blur denso y controles estilizados",
    ),

    @SerialName("ergonomic")
    ERGONOMIC(
        id = "ergonomic",
        titleEs = "Ergonómico (One-Hand)",
        descEs = "Controles desplazados hacia abajo para fácil alcance con una sola mano",
    ),

    @SerialName("power_user")
    POWER_USER(
        id = "power_user",
        titleEs = "Pro / Power-User",
        descEs = "Todos los controles visibles (Aleatorio, Repetir, Letras, Cola y Volumen)",
    );
}

/**
 * Immutable state holding all customizable parameters of the Now Playing player.
 */
@Serializable
data class PlayerCustomizationState(
    val playerStyle: PlayerStyle = PlayerStyle.MOCHY,
    val preset: PlayerPreset = PlayerPreset.CUSTOM,
    /**
     * Vertical offset for the control deck in DP (-40dp to +60dp).
     * Positive values move controls closer to the bottom thumb area.
     */
    val controlsOffsetDp: Int = 0,
    val showShuffle: Boolean = true,
    val showRepeat: Boolean = true,
    val showProgressBar: Boolean = true,
    val showTimestamps: Boolean = true,
    val showFavorite: Boolean = true,
    val showLyricsButton: Boolean = true,
    val showQueueButton: Boolean = true,
    val showVolumeBar: Boolean = true,
    val coverSize: PlayerCoverSize = PlayerCoverSize.NORMAL,
    val coverAnimation: PlayerCoverAnimation = PlayerCoverAnimation.STATIC,
    /**
     * Blur opacity / intensity (0.0f = pure black, 1.0f = full Liquid Glass blur).
     */
    val blurIntensity: Float = 0.85f,
    val backdropStyle: PlayerBackdropStyle = PlayerBackdropStyle.LIQUID_GLASS,
) {
    companion object {
        fun forPreset(preset: PlayerPreset): PlayerCustomizationState = when (preset) {
            PlayerPreset.CUSTOM -> PlayerCustomizationState(preset = PlayerPreset.CUSTOM)
            PlayerPreset.MINIMAL_OLED -> PlayerCustomizationState(
                preset = PlayerPreset.MINIMAL_OLED,
                controlsOffsetDp = 10,
                showShuffle = false,
                showRepeat = false,
                showProgressBar = true,
                showTimestamps = true,
                showFavorite = true,
                showLyricsButton = true,
                showQueueButton = false,
                showVolumeBar = false,
                coverSize = PlayerCoverSize.NORMAL,
                coverAnimation = PlayerCoverAnimation.STATIC,
                blurIntensity = 0.0f,
                backdropStyle = PlayerBackdropStyle.OLED_BLACK,
            )
            PlayerPreset.IMMERSIVE -> PlayerCustomizationState(
                preset = PlayerPreset.IMMERSIVE,
                controlsOffsetDp = 20,
                showShuffle = false,
                showRepeat = false,
                showProgressBar = true,
                showTimestamps = true,
                showFavorite = true,
                showLyricsButton = true,
                showQueueButton = true,
                showVolumeBar = false,
                coverSize = PlayerCoverSize.EXPANDED_DOWN,
                coverAnimation = PlayerCoverAnimation.SUBTLE_PULSE,
                blurIntensity = 1.0f,
                backdropStyle = PlayerBackdropStyle.LIQUID_GLASS,
            )
            PlayerPreset.ERGONOMIC -> PlayerCustomizationState(
                preset = PlayerPreset.ERGONOMIC,
                controlsOffsetDp = 45,
                showShuffle = true,
                showRepeat = true,
                showProgressBar = true,
                showTimestamps = true,
                showFavorite = true,
                showLyricsButton = true,
                showQueueButton = true,
                showVolumeBar = true,
                coverSize = PlayerCoverSize.NORMAL,
                coverAnimation = PlayerCoverAnimation.STATIC,
                blurIntensity = 0.85f,
                backdropStyle = PlayerBackdropStyle.LIQUID_GLASS,
            )
            PlayerPreset.POWER_USER -> PlayerCustomizationState(
                preset = PlayerPreset.POWER_USER,
                controlsOffsetDp = 0,
                showShuffle = true,
                showRepeat = true,
                showProgressBar = true,
                showTimestamps = true,
                showFavorite = true,
                showLyricsButton = true,
                showQueueButton = true,
                showVolumeBar = true,
                coverSize = PlayerCoverSize.NORMAL,
                coverAnimation = PlayerCoverAnimation.STATIC,
                blurIntensity = 0.85f,
                backdropStyle = PlayerBackdropStyle.LIQUID_GLASS,
            )
        }
    }
}
