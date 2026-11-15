package com.music.bitchord.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.settings.ProfileCardEffect
import com.music.bitchord.data.settings.ProfileCardStore
import com.music.bitchord.data.settings.ProfileCardTheme
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.ui.icons.SfIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.util.Locale

/**
 * Full-screen 3D ID Card Customizer and First-Run Post-Update Welcome Screen.
 *
 * When [isFirstLaunchWelcome] is true, shows a celebratory welcome header for the update
 * inviting the user to personalize their new interactive 3D ID card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCardCustomizerScreen(
    fallbackName: String,
    fallbackAvatarUrl: String?,
    isFirstLaunchWelcome: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val cardState by ProfileCardStore.state.collectAsStateWithLifecycle()

    val isSpanish = remember {
        Locale.getDefault().language.startsWith("es", ignoreCase = true)
    }

    var nameInput by rememberSaveable {
        mutableStateOf(
            cardState.displayName.ifBlank {
                fallbackName.ifBlank { ListenTogether.nickname().ifBlank { "divine" } }
            },
        )
    }
    var numberInput by rememberSaveable { mutableStateOf(cardState.cardNumber) }
    var selectedTheme by rememberSaveable { mutableStateOf(cardState.theme) }
    var selectedEffect by rememberSaveable { mutableStateOf(cardState.effect) }
    var showDotBackdrop by rememberSaveable { mutableStateOf(cardState.showDotBackdrop) }
    var showBadgeText by rememberSaveable { mutableStateOf(cardState.showBadgeText) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoCleared by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            photoCleared = false
        }
    }

    val launchPhotoPicker = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val previewPhoto: Any? = when {
        pickedPhotoUri != null -> pickedPhotoUri
        photoCleared -> fallbackAvatarUrl
        else -> cardState.resolvedPhotoModel(fallbackAvatarUrl)
    }

    val saveAndClose: () -> Unit = {
        if (!saving) {
            saving = true
            focusManager.clearFocus()
            scope.launch {
                val finalName = nameInput.trim().ifBlank {
                    fallbackName.ifBlank { "divine" }
                }
                if (photoCleared && pickedPhotoUri == null) {
                    ProfileCardStore.clearCustomPhoto(context)
                } else if (pickedPhotoUri != null) {
                    ProfileCardStore.saveCustomPhoto(context, pickedPhotoUri!!)
                }
                ProfileCardStore.updateStyle(
                    displayName = finalName,
                    cardNumber = numberInput.trim().ifBlank { cardState.cardNumber },
                    theme = selectedTheme,
                    effect = selectedEffect,
                    showDotBackdrop = showDotBackdrop,
                    showBadgeText = showBadgeText,
                )
                if (ListenTogether.nickname().isBlank()) {
                    ListenTogether.setNickname(finalName)
                }
                if (isFirstLaunchWelcome) {
                    ProfileCardStore.markSetupShown()
                }
                onDismiss()
            }
        }
    }

    BackHandler(onBack = {
        if (isFirstLaunchWelcome) {
            ProfileCardStore.markSetupShown()
        }
        onDismiss()
    })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0D))
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        // Dynamic ambient glow matching the selected card theme
        val animatedGlow by animateColorAsState(
            targetValue = selectedTheme.stripTextColor.copy(alpha = 0.22f),
            animationSpec = tween(450),
            label = "cardCustomizerGlow",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedGlow, Color.Transparent),
                        radius = 1100f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFirstLaunchWelcome) {
                    Image(
                        painter = painterResource(Res.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.size(width = 24.dp, height = 34.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSpanish) "Novedad en BeDivine" else "New in BeDivine",
                            color = selectedTheme.stripTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = if (isSpanish) "Personaliza tu Tarjeta 3D" else "Customize your 3D Card",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = if (isSpanish) "Omitir" else "Skip",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                ProfileCardStore.markSetupShown()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                } else {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = if (isSpanish) "Cerrar" else "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSpanish) "Personalizar Tarjeta 3D" else "Customize 3D Card",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isSpanish) "Arrastra la tarjeta con el dedo para moverla en 3D" else "Drag the card with your finger to tilt in 3D",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                if (isFirstLaunchWelcome) {
                    Text(
                        text = if (isSpanish) {
                            "¡Bienvenido a la nueva actualización! Ahora tienes tu propia tarjeta de identificación 3D interactiva en el menú de perfil. Muévela con el dedo y diséñala a tu gusto."
                        } else {
                            "Welcome to the new update! You now have your own interactive 3D ID Card in the profile menu. Move it with your finger and style it your way."
                        },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }

                // Live Interactive 3D Card Preview
                InteractiveProfileIdCard3D(
                    username = nameInput.ifBlank { fallbackName.ifBlank { "divine" } },
                    photo = previewPhoto,
                    cardNumber = numberInput.ifBlank { cardState.cardNumber },
                    joinedText = cardState.formattedJoinDate(),
                    theme = selectedTheme,
                    effect = selectedEffect,
                    showDotBackdrop = showDotBackdrop,
                    showBadgeText = showBadgeText,
                    showEditHint = false,
                    onPhotoClick = launchPhotoPicker,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isSpanish) "✨ Toca y arrastra la tarjeta para probar el efecto 3D en vivo" else "✨ Touch and drag the card to test the live 3D effect",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(22.dp))

                // 1. Photo & Name Section
                SectionHeader(if (isSpanish) "IDENTIDAD Y FOTO" else "IDENTITY & PHOTO")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF151519))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C34))
                            .border(1.5.dp, selectedTheme.stripTextColor.copy(alpha = 0.7f), CircleShape)
                            .clickable(onClick = launchPhotoPicker),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (previewPhoto != null) {
                            AsyncImage(
                                model = previewPhoto,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = SfIcons.Add,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSpanish) "Foto de la tarjeta" else "Card Photo",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isSpanish) "Elige cualquier imagen de tu galería" else "Pick any image from your gallery",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallActionChip(
                                label = if (isSpanish) "Cambiar foto" else "Change photo",
                                accent = selectedTheme.stripTextColor,
                                onClick = launchPhotoPicker,
                            )
                            if (pickedPhotoUri != null || (cardState.customPhotoPath.isNotBlank() && !photoCleared)) {
                                SmallActionChip(
                                    label = if (isSpanish) "Restaurar" else "Reset",
                                    accent = Color.White.copy(alpha = 0.6f),
                                    onClick = {
                                        pickedPhotoUri = null
                                        photoCleared = true
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Name Field
                CustomizerTextField(
                    label = if (isSpanish) "Nombre en la tarjeta" else "Card Display Name",
                    value = nameInput,
                    placeholder = fallbackName.ifBlank { "divine" },
                    onValueChange = { nameInput = it.take(32) },
                    onDone = { focusManager.clearFocus() },
                )

                Spacer(Modifier.height(12.dp))

                // USR# Tag Field
                CustomizerTextField(
                    label = if (isSpanish) "Código de identificación (USR#)" else "ID Tag (USR#)",
                    value = numberInput,
                    placeholder = "44944084647",
                    keyboardType = KeyboardType.Ascii,
                    onValueChange = { numberInput = it.take(16) },
                    onDone = { focusManager.clearFocus() },
                    trailing = {
                        IconButton(
                            onClick = { numberInput = ProfileCardStore.generateRandomCardNumber() },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = if (isSpanish) "Generar aleatorio" else "Randomize",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )

                Spacer(Modifier.height(24.dp))

                // 2. Color Theme Palette Section
                SectionHeader(if (isSpanish) "COLOR Y TEMA" else "COLOR & THEME")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileCardTheme.entries.forEach { themeOption ->
                        val selected = themeOption == selectedTheme
                        ThemeSwatchCard(
                            theme = themeOption,
                            isSpanish = isSpanish,
                            selected = selected,
                            onClick = { selectedTheme = themeOption },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 3. Visual Effects Section
                SectionHeader(if (isSpanish) "EFECTOS VISUALES 3D" else "3D VISUAL EFFECTS")
                Spacer(Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileCardEffect.entries.forEach { effectOption ->
                        val selected = effectOption == selectedEffect
                        EffectOptionPill(
                            label = if (isSpanish) effectOption.labelEs else effectOption.labelEn,
                            selected = selected,
                            accentColor = selectedTheme.stripTextColor,
                            onClick = { selectedEffect = effectOption },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 4. Extra Style Toggles
                SectionHeader(if (isSpanish) "DETALLES ADICIONALES" else "EXTRA DETAILS")
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF151519))
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                ) {
                    ToggleRow(
                        title = if (isSpanish) "Fondo de matriz de puntos" else "Dot-matrix stage backdrop",
                        subtitle = if (isSpanish) "Muestra el marco punteado estilo Mochy detrás de la tarjeta" else "Show the dotted Mochy-style stage behind the card",
                        checked = showDotBackdrop,
                        accent = selectedTheme.stripTextColor,
                        onCheckedChange = { showDotBackdrop = it },
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    ToggleRow(
                        title = if (isSpanish) "Etiqueta \"ID CARD OF\"" else "\"ID CARD OF\" badge",
                        subtitle = if (isSpanish) "Muestra el encabezado sobre tu nombre en la tarjeta" else "Show the header label above your name on the card",
                        checked = showBadgeText,
                        accent = selectedTheme.stripTextColor,
                        onCheckedChange = { showBadgeText = it },
                    )
                }

                Spacer(Modifier.height(28.dp))
            }

            // Bottom Save Button
            Box(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0A0A0D).copy(alpha = 0.95f), Color(0xFF0A0A0D)),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                SaveCardButton(
                    label = if (isFirstLaunchWelcome) {
                        if (isSpanish) "Guardar y continuar a BeDivine" else "Save & continue to BeDivine"
                    } else {
                        if (isSpanish) "Guardar cambios" else "Save changes"
                    },
                    enabled = !saving,
                    onClick = saveAndClose,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 11.5.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SmallActionChip(
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .border(0.5.dp, accent.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CustomizerTextField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF151519))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.White.copy(alpha = 0.30f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun ThemeSwatchCard(
    theme: ProfileCardTheme,
    isSpanish: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Color.White else Color.White.copy(alpha = 0.12f)
    val borderWidth = if (selected) 2.dp else 0.75.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF151519))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(theme.primaryColor, theme.secondaryColor),
                    ),
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .height(46.dp)
                    .background(theme.stripBackground),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isSpanish) theme.labelEs else theme.labelEn,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EffectOptionPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.24f) else Color(0xFF151519)
    val strokeColor = if (selected) accentColor else Color.White.copy(alpha = 0.10f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(if (selected) 1.5.dp else 0.75.dp, strokeColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = SfIcons.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent.copy(alpha = 0.75f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
    }
}

@Composable
private fun SaveCardButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "saveCardScale",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFF0A0A0D),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
