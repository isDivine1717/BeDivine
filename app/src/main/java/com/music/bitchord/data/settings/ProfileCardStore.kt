package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/**
 * Color themes for the BeDivine 3D Profile ID Card.
 */
enum class ProfileCardTheme(
    val labelEs: String,
    val labelEn: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val stripBackground: Color,
    val stripTextColor: Color,
    val bodyTextColor: Color,
    val mutedTextColor: Color,
    val isMetallicLight: Boolean = false,
) {
    ONYX_DARK(
        labelEs = "Ónix Oscuro",
        labelEn = "Onyx Dark",
        primaryColor = Color(0xFF2B2B2E),
        secondaryColor = Color(0xFF1E1E21),
        stripBackground = Color(0xFF151517),
        stripTextColor = Color(0xFF8E8E93),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFB5B5BA),
    ),
    SILVER_CHROME(
        labelEs = "Plata Cromo",
        labelEn = "Silver Chrome",
        primaryColor = Color(0xFFE4E6EB),
        secondaryColor = Color(0xFFA8ABB3),
        stripBackground = Color(0xFF222327),
        stripTextColor = Color(0xFFD5D7DD),
        bodyTextColor = Color(0xFF111114),
        mutedTextColor = Color(0xFF3E3E46),
        isMetallicLight = true,
    ),
    DIVINE_VIOLET(
        labelEs = "Violeta Divino",
        labelEn = "Divine Violet",
        primaryColor = Color(0xFF3B1D5E),
        secondaryColor = Color(0xFF1A0B2E),
        stripBackground = Color(0xFF110720),
        stripTextColor = Color(0xFFB888F8),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFD0B3F5),
    ),
    CRIMSON_PULSE(
        labelEs = "Carmesí",
        labelEn = "Crimson Pulse",
        primaryColor = Color(0xFF5C1622),
        secondaryColor = Color(0xFF26070D),
        stripBackground = Color(0xFF180408),
        stripTextColor = Color(0xFFFF758F),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFF2B6C1),
    ),
    CYBER_CYAN(
        labelEs = "Cian Neón",
        labelEn = "Cyber Cyan",
        primaryColor = Color(0xFF0E3D4A),
        secondaryColor = Color(0xFF061B22),
        stripBackground = Color(0xFF041116),
        stripTextColor = Color(0xFF5CE1E6),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFA3EBF0),
    ),
    ROYAL_GOLD(
        labelEs = "Oro Real",
        labelEn = "Royal Gold",
        primaryColor = Color(0xFF4A3810),
        secondaryColor = Color(0xFF211805),
        stripBackground = Color(0xFF140E02),
        stripTextColor = Color(0xFFFFD166),
        bodyTextColor = Color(0xFFFFF8E7),
        mutedTextColor = Color(0xFFE6C875),
    ),
    EMERALD_NIGHT(
        labelEs = "Esmeralda",
        labelEn = "Emerald Night",
        primaryColor = Color(0xFF113F2B),
        secondaryColor = Color(0xFF071C13),
        stripBackground = Color(0xFF04120C),
        stripTextColor = Color(0xFF52B788),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFA8E6CF),
    ),
    ROSE_QUARTZ(
        labelEs = "Cuarzo Rosa",
        labelEn = "Rose Quartz",
        primaryColor = Color(0xFF52213B),
        secondaryColor = Color(0xFF260D1B),
        stripBackground = Color(0xFF170710),
        stripTextColor = Color(0xFFFF99C8),
        bodyTextColor = Color.White,
        mutedTextColor = Color(0xFFF7C6DE),
    ),
}

/**
 * Visual surface effects for the 3D Profile ID Card.
 */
enum class ProfileCardEffect(
    val labelEs: String,
    val labelEn: String,
) {
    HOLOGRAPHIC("Holográfico", "Holographic"),
    METALLIC_BRUSHED("Metal Cepillado", "Brushed Metal"),
    DOT_MATRIX("Matriz de Puntos", "Dot Matrix"),
    NEON_GLOW("Borde Neón", "Neon Border"),
    STARDUST("Polvo Estelar", "Stardust"),
    MINIMAL_MATTE("Mate Minimalista", "Minimal Matte"),
}

data class ProfileCardState(
    val displayName: String = "",
    val cardNumber: String = "44944084647",
    val customPhotoPath: String = "",
    val joinedEpochDay: Long = -1L,
    val theme: ProfileCardTheme = ProfileCardTheme.ONYX_DARK,
    val effect: ProfileCardEffect = ProfileCardEffect.HOLOGRAPHIC,
    val showDotBackdrop: Boolean = true,
    val showBadgeText: Boolean = true,
    val setupShownForV1_0_1: Boolean = false,
) {
    fun formattedJoinDate(locale: Locale = Locale.getDefault()): String {
        val date = if (joinedEpochDay > 0L) {
            LocalDate.ofEpochDay(joinedEpochDay)
        } else {
            LocalDate.now()
        }
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
        val isSpanish = locale.language.startsWith("es", ignoreCase = true)
        val prefix = if (isSpanish) "se unió el " else "joined at "
        return prefix + date.format(formatter)
    }

    fun resolvedPhotoModel(fallbackAccountAvatar: String?): Any? {
        if (customPhotoPath.isNotBlank()) {
            val file = File(customPhotoPath)
            if (file.exists()) return file
            if (customPhotoPath.contains("://")) return customPhotoPath
        }
        return fallbackAccountAvatar?.takeIf { it.isNotBlank() }
    }
}

object ProfileCardStore {
    private const val PREFS_NAME = "bedivine_profile_card"
    private const val PHOTO_PREFIX = "bedivine_card_pic_"

    private const val KEY_DISPLAY_NAME = "card_display_name"
    private const val KEY_CARD_NUMBER = "card_number"
    private const val KEY_CUSTOM_PHOTO_PATH = "card_custom_photo_path"
    private const val KEY_JOINED_EPOCH_DAY = "card_joined_epoch_day"
    private const val KEY_THEME = "card_theme"
    private const val KEY_EFFECT = "card_effect"
    private const val KEY_SHOW_DOT_BACKDROP = "card_show_dot_backdrop"
    private const val KEY_SHOW_BADGE_TEXT = "card_show_badge_text"
    private const val KEY_SETUP_SHOWN_V101 = "card_setup_shown_v1_0_1"

    private lateinit var prefs: SharedPreferences

    val state = MutableStateFlow(ProfileCardState())

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var cardNumber = prefs.getString(KEY_CARD_NUMBER, null).orEmpty()
        if (cardNumber.isBlank()) {
            cardNumber = (10_000_000_000L + Random.nextLong(89_999_999_999L)).toString()
            prefs.edit().putString(KEY_CARD_NUMBER, cardNumber).apply()
        }

        var joinedEpochDay = prefs.getLong(KEY_JOINED_EPOCH_DAY, -1L)
        if (joinedEpochDay <= 0L) {
            joinedEpochDay = runCatching {
                val installMs = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
                if (installMs > 0L) {
                    Instant.ofEpochMilli(installMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                } else {
                    LocalDate.now().toEpochDay()
                }
            }.getOrDefault(LocalDate.now().toEpochDay())
            prefs.edit().putLong(KEY_JOINED_EPOCH_DAY, joinedEpochDay).apply()
        }

        val theme = runCatching {
            ProfileCardTheme.valueOf(prefs.getString(KEY_THEME, null) ?: ProfileCardTheme.ONYX_DARK.name)
        }.getOrDefault(ProfileCardTheme.ONYX_DARK)

        val effect = runCatching {
            ProfileCardEffect.valueOf(prefs.getString(KEY_EFFECT, null) ?: ProfileCardEffect.HOLOGRAPHIC.name)
        }.getOrDefault(ProfileCardEffect.HOLOGRAPHIC)

        state.value = ProfileCardState(
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            cardNumber = cardNumber,
            customPhotoPath = prefs.getString(KEY_CUSTOM_PHOTO_PATH, "").orEmpty(),
            joinedEpochDay = joinedEpochDay,
            theme = theme,
            effect = effect,
            showDotBackdrop = prefs.getBoolean(KEY_SHOW_DOT_BACKDROP, true),
            showBadgeText = prefs.getBoolean(KEY_SHOW_BADGE_TEXT, true),
            setupShownForV1_0_1 = prefs.getBoolean(KEY_SETUP_SHOWN_V101, false),
        )
    }

    fun ensureInitialized(context: Context) {
        if (!this::prefs.isInitialized) init(context)
    }

    fun updateStyle(
        displayName: String = state.value.displayName,
        cardNumber: String = state.value.cardNumber,
        theme: ProfileCardTheme = state.value.theme,
        effect: ProfileCardEffect = state.value.effect,
        showDotBackdrop: Boolean = state.value.showDotBackdrop,
        showBadgeText: Boolean = state.value.showBadgeText,
    ) {
        val cleanName = displayName.trim().take(32)
        val cleanNumber = cardNumber.trim().ifBlank { state.value.cardNumber }.take(16)
        state.value = state.value.copy(
            displayName = cleanName,
            cardNumber = cleanNumber,
            theme = theme,
            effect = effect,
            showDotBackdrop = showDotBackdrop,
            showBadgeText = showBadgeText,
        )
        if (this::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_DISPLAY_NAME, cleanName)
                .putString(KEY_CARD_NUMBER, cleanNumber)
                .putString(KEY_THEME, theme.name)
                .putString(KEY_EFFECT, effect.name)
                .putBoolean(KEY_SHOW_DOT_BACKDROP, showDotBackdrop)
                .putBoolean(KEY_SHOW_BADGE_TEXT, showBadgeText)
                .apply()
        }
    }

    fun markSetupShown() {
        state.value = state.value.copy(setupShownForV1_0_1 = true)
        if (this::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_SETUP_SHOWN_V101, true).apply()
        }
    }

    fun clearCustomPhoto(context: Context) {
        ensureInitialized(context)
        val oldPath = state.value.customPhotoPath
        state.value = state.value.copy(customPhotoPath = "")
        prefs.edit().putString(KEY_CUSTOM_PHOTO_PATH, "").apply()
        if (oldPath.isNotBlank()) deleteOldPhoto(context.applicationContext, oldPath)
    }

    suspend fun saveCustomPhoto(context: Context, uri: Uri): String? =
        withContext(NonCancellable + Dispatchers.IO) {
            ensureInitialized(context)
            val app = context.applicationContext
            val newPath = runCatching {
                val file = File(app.filesDir, "$PHOTO_PREFIX${System.currentTimeMillis()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                file.absolutePath
            }.getOrNull()

            if (newPath != null) {
                val oldPath = state.value.customPhotoPath
                state.value = state.value.copy(customPhotoPath = newPath)
                prefs.edit().putString(KEY_CUSTOM_PHOTO_PATH, newPath).apply()
                if (oldPath.isNotBlank() && oldPath != newPath) {
                    deleteOldPhoto(app, oldPath)
                }
            }
            newPath
        }

    fun generateRandomCardNumber(): String =
        (10_000_000_000L + Random.nextLong(89_999_999_999L)).toString()

    private fun deleteOldPhoto(context: Context, path: String) {
        runCatching {
            val file = File(path)
            if (file.parentFile == context.filesDir && file.name.startsWith(PHOTO_PREFIX)) {
                file.delete()
            }
        }
    }
}
