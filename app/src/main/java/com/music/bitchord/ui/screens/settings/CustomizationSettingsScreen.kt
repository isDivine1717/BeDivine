package com.music.bitchord.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.HomeCustomizationState
import com.music.bitchord.data.settings.HomeCustomizationStore
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionStyle
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.data.settings.HomeTemplate
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.icons.SfIcons

/**
 * Standalone Customization Settings Screen (Ajustes > Personalización).
 */
@Composable
fun CustomizationSettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onBack: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = SfIcons.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.customization),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        CustomizationSettingsContent()
    }
}

enum class CustomizationTab(val title: String) {
    HOME("Pantalla de Inicio"),
    PLAYER("Reproductor"),
}

/**
 * Reusable Customization Content embedded into SettingsSheet or CustomizationSettingsScreen.
 */
@Composable
fun CustomizationSettingsContent(
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(CustomizationTab.HOME) }
    val state by HomeCustomizationStore.state.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Selector Pills (Inicio | Reproductor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1C1E))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CustomizationTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                val bg = if (isSelected) Color(0xFF3A3A3C) else Color.Transparent
                val textColor = if (isSelected) Color.White else Color(0xFF8E8E93)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { activeTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.title,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (activeTab == CustomizationTab.HOME) {
            // Section 1: Template Selection
            HomeTemplatesSection(
                activeTemplate = state.template,
                onSelectTemplate = { template ->
                    HomeCustomizationStore.setTemplate(template)
                },
            )

            Spacer(Modifier.height(24.dp))

            // Section 2: Section Reordering & Visibility
            HomeSectionsReorderSection(
                sections = state.sections,
                onToggleVisibility = { type ->
                    HomeCustomizationStore.toggleSectionVisibility(type)
                },
                onMoveUp = { index ->
                    if (index > 0) {
                        HomeCustomizationStore.moveSection(index, index - 1)
                    }
                },
                onMoveDown = { index ->
                    if (index < state.sections.lastIndex) {
                        HomeCustomizationStore.moveSection(index, index + 1)
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            // Section 3: Reset to Defaults Button
            ResetDefaultsSection(
                onResetClick = { showResetDialog = true },
            )

            Spacer(Modifier.height(32.dp))
        } else {
            // Section: Player Customization
            PlayerCustomizationContent()
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.reset_to_defaults_confirm_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(text = stringResource(R.string.reset_to_defaults_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HomeCustomizationStore.resetToDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.reset_to_defaults),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * Top section: visual preview cards for selectable Home templates.
 */
@Composable
private fun HomeTemplatesSection(
    activeTemplate: HomeTemplate,
    onSelectTemplate: (HomeTemplate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_templates_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.home_templates_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HomeTemplate.entries.forEach { template ->
                val selected = template == activeTemplate
                TemplateCard(
                    template = template,
                    selected = selected,
                    onClick = { onSelectTemplate(template) },
                )
            }
        }
    }
}

/**
 * Visual card displaying a template preview, title, description and selection state.
 */
@Composable
private fun TemplateCard(
    template: HomeTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200),
        label = "templateBorderColor",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.5f else 0.25f),
        modifier = Modifier
            .width(240.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Miniature Mockup Wireframe
            TemplateMockupPreview(
                template = template,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = template.titleEs,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = template.descEs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
        }
    }
}

/**
 * Miniature wireframe mockup previewing the template's layout hierarchy.
 */
@Composable
private fun TemplateMockupPreview(
    template: HomeTemplate,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val cardFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val secondaryFill = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)

    Box(modifier = modifier.padding(8.dp)) {
        when (template) {
            HomeTemplate.DYNAMIC_STANDARD -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            Modifier
                                .size(width = 44.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Top Picks Portrait Carousel
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(width = 44.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cardFill),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Compact shelf
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(4) {
                            Box(
                                Modifier
                                    .size(width = 24.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(secondaryFill),
                            )
                        }
                    }
                }
            }

            HomeTemplate.MOCHY_COMPACT -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Compact Top Bar with glow
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor),
                    )
                    Spacer(Modifier.height(6.dp))
                    // 2x2 Hop-in pill grid
                    repeat(2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            repeat(2) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cardFill),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // 3 compact track rows
                    repeat(2) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(secondaryFill),
                        )
                    }
                }
            }

            HomeTemplate.EDITORIAL_HERO -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Hero Spotlight Banner with gradient
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                                    ),
                                ),
                            ),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .size(width = 54.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.8f)),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Horizontal spotlight cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(width = 50.dp, height = 26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(secondaryFill),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Middle section: interactive reordering and visibility controls for all sections.
 */
@Composable
private fun HomeSectionsReorderSection(
    sections: List<HomeSectionConfig>,
    onToggleVisibility: (HomeSectionType) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_sections_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.home_sections_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Count pill
            val visibleCount = sections.count { it.visible }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    text = "$visibleCount / ${sections.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                sections.forEachIndexed { index, config ->
                    SectionReorderRow(
                        config = config,
                        index = index,
                        totalCount = sections.size,
                        onToggleVisibility = { onToggleVisibility(config.type) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                    )
                    if (index < sections.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(horizontal = 16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual section row with drag indicator, up/down movement, title, style pill, and toggle switch.
 */
@Composable
private fun SectionReorderRow(
    config: HomeSectionConfig,
    index: Int,
    totalCount: Int,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val isVisible = config.visible
    val rowAlpha = if (isVisible) 1f else 0.45f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable(onClick = onToggleVisibility)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Up & Down movement buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 6.dp),
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(24.dp),
            ) {
                Text(
                    text = "▲",
                    fontSize = 11.sp,
                    color = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(24.dp),
            ) {
                Text(
                    text = "▼",
                    fontSize = 11.sp,
                    color = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                )
            }
        }

        // Section Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
        ) {
            Icon(
                imageVector = sectionIcon(config.type),
                contentDescription = null,
                tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Titles and style badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.type.titleEs,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                // Style badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                ) {
                    Text(
                        text = config.style.titleEs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                Text(
                    text = if (isVisible) stringResource(R.string.visible) else stringResource(R.string.hidden),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Direct toggle switch
        IosSwitch(
            checked = isVisible,
            onCheckedChange = { onToggleVisibility() },
        )

        Spacer(Modifier.width(6.dp))

        // Drag Grip handle icon
        Text(
            text = "⠿",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/**
 * Bottom section: Reset to defaults action button.
 */
@Composable
private fun ResetDefaultsSection(
    onResetClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onResetClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = SfIcons.Revert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.reset_to_defaults),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Maps each [HomeSectionType] to its designated SF Symbol icon.
 */
private fun sectionIcon(type: HomeSectionType): ImageVector = when (type) {
    HomeSectionType.TOP_PICKS -> SfIcons.StarFill
    HomeSectionType.RECENTS -> SfIcons.Clock
    HomeSectionType.MOODS -> SfIcons.Waves
    HomeSectionType.MIXED_FOR_YOU -> SfIcons.Shuffle
    HomeSectionType.LISTEN_AGAIN -> SfIcons.Cached
    HomeSectionType.RECOMMENDED_VIDEOS -> SfIcons.Video
    HomeSectionType.FEATURED_PLAYLISTS -> SfIcons.AddToPlaylist
    HomeSectionType.NEW_RELEASES_CHARTS -> SfIcons.BarChart
}
