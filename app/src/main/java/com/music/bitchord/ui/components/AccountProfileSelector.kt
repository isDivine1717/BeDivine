package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.YouTubeProfile
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ProfileCardStore
import com.music.bitchord.ui.icons.SfIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.Locale

/** Shared compact selector for every visible YouTube account avatar, featuring the interactive 3D ID Card. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    hazeState: HazeState,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    onCustomizeCard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by remember { mutableStateOf(false) }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val cardState by ProfileCardStore.state.collectAsStateWithLifecycle()

    val activeAccount = remember(accounts, activeAccountId) {
        accounts.firstOrNull { it.accountId == activeAccountId } ?: accounts.firstOrNull()
    }
    val activeProfile = remember(activeAccount, activeProfileId) {
        activeAccount?.profiles?.firstOrNull { it.profileId == activeProfileId }
            ?: activeAccount?.profiles?.firstOrNull()
    }

    val fallbackName = activeProfile?.name
        ?.ifBlank { activeAccount?.name.orEmpty() }
        ?.ifBlank { ListenTogether.nickname() }
        ?.ifBlank { "divine" }
        ?: ListenTogether.nickname().ifBlank { "divine" }

    val cardDisplayName = cardState.displayName.ifBlank { fallbackName }
    val cardPhoto = cardState.resolvedPhotoModel(activeProfile?.avatar)
    val isSpanish = remember { Locale.getDefault().language.startsWith("es", ignoreCase = true) }

    val shape = MaterialTheme.shapes.extraLarge
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    val material = HazeMaterials.thin(surface)
    val style = remember(material, dark) {
        HazeStyle(
            backgroundColor = material.backgroundColor,
            tints = material.tints + HazeTint(
                if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.40f),
            ),
            blurRadius = material.blurRadius,
            noiseFactor = material.noiseFactor,
            fallbackTint = material.fallbackTint,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = style,
                        )
                    },
                )
                .border(
                    0.5.dp,
                    if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                    shape,
                )
                .clickable(onClick = {}),
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 14.dp)) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
                    ) {
                        Text(
                            text = if (isSpanish) "¡Hola de nuevo, $cardDisplayName!" else "Hello again, $cardDisplayName!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        InteractiveProfileIdCard3D(
                            username = cardDisplayName,
                            photo = cardPhoto,
                            cardNumber = cardState.cardNumber,
                            joinedText = cardState.formattedJoinDate(),
                            theme = cardState.theme,
                            effect = cardState.effect,
                            showDotBackdrop = cardState.showDotBackdrop,
                            showBadgeText = cardState.showBadgeText,
                            showEditHint = true,
                            onClick = onCustomizeCard,
                        )
                    }
                }

                if (managing && accounts.isNotEmpty()) {
                    accounts.forEach { account ->
                        item {
                            Text(
                                account.email.ifBlank { account.name.ifBlank { stringResource(R.string.accounts) } },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 4.dp),
                            )
                        }
                        items(account.profiles.size) { index ->
                            val profile = account.profiles[index]
                            ProfileRow(
                                profile = profile,
                                selected = account.accountId == activeAccountId && profile.profileId == activeProfileId,
                                managing = false,
                                onClick = {
                                    onSelect(account, profile)
                                    onDismiss()
                                },
                                onRemove = { onRemoveAccount(account) },
                            )
                        }
                    }
                }

                item {
                    SelectorAction(
                        icon = SfIcons.Add,
                        label = stringResource(R.string.add_account),
                        onClick = onAddAccount,
                    )
                }
                item {
                    SelectorAction(
                        icon = SfIcons.Close,
                        label = stringResource(R.string.manage_accounts),
                    ) { managing = !managing }
                }
                item {
                    SelectorAction(
                        icon = SfIcons.Settings,
                        label = stringResource(R.string.settings),
                        onClick = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: YouTubeProfile,
    selected: Boolean,
    managing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (profile.avatar != null) {
            AsyncImage(
                profile.avatar,
                null,
                Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )
        } else {
            Icon(
                SfIcons.AccountPlaceholder,
                null,
                Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                profile.handle.ifBlank {
                    stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected && !managing) {
            Icon(
                Icons.Rounded.Check,
                stringResource(R.string.selected_account),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.sign_out),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onRemove)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SelectorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(iconSize))
        }
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}
