package os.kei.ui.page.main.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.AppLiquidButton
import os.kei.ui.page.main.widget.glass.AppLiquidPrimaryToggle
import os.kei.ui.page.main.widget.glass.LiquidRoundedCard
import os.kei.ui.page.main.widget.glass.LiquidSlider
import os.kei.ui.page.main.widget.glass.LiquidSurface
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidBackdropPlaygroundSample(
    accent: Color,
    contentColor: Color,
    secondaryColor: Color
) {
    val playgroundBackdrop = rememberLayerBackdrop()
    Text(
        text = stringResource(R.string.debug_component_lab_liquid_backdrop_playground_label),
        color = contentColor,
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 1
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(playgroundBackdrop)
        ) {
            DebugLiquidBackdropPalette(
                accent = accent,
                modifier = Modifier.matchParentSize()
            )
        }
        LiquidRoundedCard(
            backdrop = playgroundBackdrop,
            cornerRadius = 42.dp,
            surfaceColor = Color.Transparent,
            blurRadius = 10.dp,
            lensRadius = 28.dp,
            chromaticAberration = true,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .height(138.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_large_clear_card),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_large_clear_card_body),
                    color = secondaryColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DebugLiquidClusterButton(
                        backdrop = playgroundBackdrop,
                        tint = accent.copy(alpha = 0.44f),
                        iconTint = accent
                    )
                    DebugLiquidClusterButton(
                        backdrop = playgroundBackdrop,
                        tint = Color.Unspecified,
                        iconTint = contentColor
                    )
                }
            }
        }
        LiquidSurface(
            backdrop = playgroundBackdrop,
            shape = ContinuousCapsule,
            tint = accent.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .width(132.dp)
                .height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = appLucideMusicIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_clear_capsule),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DebugLiquidBackdropPalette(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFFEAF5FF))
        drawRoundRect(
            color = Color(0xFF43D8C9).copy(alpha = 0.62f),
            topLeft = Offset(size.width * -0.12f, size.height * 0.34f),
            size = Size(size.width * 0.72f, size.height * 0.84f),
            cornerRadius = CornerRadius(size.height * 0.30f)
        )
        drawRoundRect(
            color = Color(0xFF2E7DFF).copy(alpha = 0.72f),
            topLeft = Offset(size.width * 0.34f, size.height * -0.18f),
            size = Size(size.width * 0.86f, size.height * 0.74f),
            cornerRadius = CornerRadius(size.height * 0.22f)
        )
        drawRoundRect(
            color = Color(0xFFFF4D7D).copy(alpha = 0.42f),
            topLeft = Offset(size.width * 0.06f, size.height * 0.08f),
            size = Size(size.width * 0.32f, size.height * 0.34f),
            cornerRadius = CornerRadius(size.height * 0.16f)
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.42f),
            topLeft = Offset(size.width * 0.54f, size.height * 0.56f),
            size = Size(size.width * 0.36f, size.height * 0.28f),
            cornerRadius = CornerRadius(size.height * 0.14f)
        )
    }
}

@Composable
internal fun DebugLiquidTransparentButtonSamples(
    backdrop: Backdrop,
    contentColor: Color
) {
    Text(
        text = stringResource(R.string.debug_component_lab_liquid_clear_label),
        color = contentColor,
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 1
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLiquidButton(
            onClick = {},
            backdrop = backdrop,
            height = 46.dp,
            horizontalPadding = 12.dp,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = appLucidePlayIcon(),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = stringResource(R.string.debug_component_lab_liquid_clear_capsule),
                color = contentColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LiquidSurface(
            backdrop = backdrop,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(46.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = appLucideConfigIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_clear_rect),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        LiquidSurface(
            backdrop = backdrop,
            shape = CircleShape,
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = appLucideMoreIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_liquid_clear_circle),
                tint = contentColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(19.dp)
            )
        }
    }
}

@Composable
internal fun DebugLiquidSurfaceFamilySamples(
    backdrop: Backdrop,
    accent: Color,
    contentColor: Color,
    secondaryColor: Color
) {
    val cardBackdrop = rememberLayerBackdrop()
    Text(
        text = stringResource(R.string.debug_component_lab_liquid_surface_family_label),
        color = contentColor,
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 1
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidRoundedCard(
            backdrop = backdrop,
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.08f),
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(128.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(cardBackdrop)
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_rounded_card_title),
                        color = contentColor,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_rounded_card_body),
                        color = secondaryColor,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    LiquidSurface(
                        backdrop = cardBackdrop,
                        tint = accent.copy(alpha = 0.28f),
                        modifier = Modifier
                            .height(34.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.debug_component_lab_liquid_card_pill),
                            color = contentColor,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            modifier = Modifier.align(Alignment.Center),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        LiquidSurface(
            backdrop = backdrop,
            shape = ContinuousCapsule,
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.08f),
            modifier = Modifier
                .width(58.dp)
                .height(128.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = appLucideMusicIcon(),
                    contentDescription = stringResource(R.string.debug_component_lab_liquid_vertical_capsule),
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
                Icon(
                    imageVector = appLucideHeartIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = appLucideDownloadIcon(),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun DebugLiquidClusterCardSample(
    backdrop: Backdrop,
    accent: Color,
    contentColor: Color,
    secondaryColor: Color
) {
    val clusterBackdrop = rememberLayerBackdrop()
    LiquidRoundedCard(
        backdrop = backdrop,
        surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.06f),
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(clusterBackdrop)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_cluster_card_title),
                        color = contentColor,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_cluster_card_body),
                        color = secondaryColor,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DebugLiquidClusterButton(
                        backdrop = clusterBackdrop,
                        tint = Color(0xFF30D158).copy(alpha = 0.50f),
                        iconTint = Color(0xFF30D158)
                    )
                    DebugLiquidClusterButton(
                        backdrop = clusterBackdrop,
                        tint = accent.copy(alpha = 0.50f),
                        iconTint = accent
                    )
                    DebugLiquidClusterButton(
                        backdrop = clusterBackdrop,
                        tint = Color(0xFFFF3B30).copy(alpha = 0.48f),
                        iconTint = Color(0xFFFF3B30)
                    )
                    DebugLiquidClusterButton(
                        backdrop = clusterBackdrop,
                        tint = Color.Unspecified,
                        iconTint = contentColor
                    )
                    LiquidSurface(
                        backdrop = clusterBackdrop,
                        shape = ContinuousCapsule,
                        tint = Color(0xFFFFC857).copy(alpha = 0.40f),
                        modifier = Modifier
                            .width(52.dp)
                            .height(92.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = appLucidePlayIcon(),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(
                                imageVector = appLucideHeartIcon(),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLiquidClusterButton(
    backdrop: Backdrop,
    tint: Color,
    iconTint: Color
) {
    LiquidSurface(
        backdrop = backdrop,
        shape = CircleShape,
        tint = tint,
        modifier = Modifier.size(46.dp)
    ) {
        Icon(
            imageVector = appLucideMusicIcon(),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
        )
    }
}

@Composable
internal fun DebugLiquidParameterPanelSample(
    backdrop: Backdrop,
    accent: Color,
    contentColor: Color,
    secondaryColor: Color,
    cornerDemoValue: Float,
    onCornerDemoValueChange: (Float) -> Unit,
    blurDemoValue: Float,
    onBlurDemoValueChange: (Float) -> Unit,
    refractionHeightValue: Float,
    onRefractionHeightValueChange: (Float) -> Unit,
    refractionDemoValue: Float,
    onRefractionDemoValueChange: (Float) -> Unit,
    chromaticAberrationEnabled: Boolean,
    onChromaticAberrationChange: (Boolean) -> Unit
) {
    val panelBackdrop = rememberLayerBackdrop()
    val previewCorner = 18.dp + 42.dp * cornerDemoValue.coerceIn(0f, 1f)
    val previewBlur = 2.dp + 18.dp * blurDemoValue.coerceIn(0f, 1f)
    val previewLens = 6.dp + 30.dp * refractionHeightValue.coerceIn(0f, 1f)
    LiquidRoundedCard(
        backdrop = backdrop,
        surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.08f),
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(panelBackdrop)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_parameter_panel_title),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1
                )
                LiquidRoundedCard(
                    backdrop = panelBackdrop,
                    cornerRadius = previewCorner,
                    tint = accent.copy(alpha = 0.20f * refractionDemoValue.coerceIn(0f, 1f)),
                    surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(
                        alpha = 0.03f + 0.08f * refractionDemoValue.coerceIn(0f, 1f)
                    ),
                    blurRadius = previewBlur,
                    lensRadius = previewLens,
                    chromaticAberration = chromaticAberrationEnabled,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.debug_component_lab_liquid_parameter_preview_title),
                            color = contentColor,
                            fontSize = AppTypographyTokens.Body.fontSize,
                            lineHeight = AppTypographyTokens.Body.lineHeight,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.debug_component_lab_liquid_parameter_preview_body),
                            color = secondaryColor,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DebugLiquidParameterSlider(
                    label = stringResource(R.string.debug_component_lab_liquid_parameter_corner),
                    value = cornerDemoValue,
                    onValueChange = onCornerDemoValueChange,
                    backdrop = panelBackdrop,
                    secondaryColor = secondaryColor
                )
                DebugLiquidParameterSlider(
                    label = stringResource(R.string.debug_component_lab_liquid_parameter_blur),
                    value = blurDemoValue,
                    onValueChange = onBlurDemoValueChange,
                    backdrop = panelBackdrop,
                    secondaryColor = secondaryColor
                )
                DebugLiquidParameterSlider(
                    label = stringResource(R.string.debug_component_lab_liquid_parameter_refraction_height),
                    value = refractionHeightValue,
                    onValueChange = onRefractionHeightValueChange,
                    backdrop = panelBackdrop,
                    secondaryColor = secondaryColor
                )
                DebugLiquidParameterSlider(
                    label = stringResource(R.string.debug_component_lab_liquid_parameter_refraction),
                    value = refractionDemoValue,
                    onValueChange = onRefractionDemoValueChange,
                    backdrop = panelBackdrop,
                    secondaryColor = secondaryColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_parameter_chromatic),
                        color = secondaryColor,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1
                    )
                    AppLiquidPrimaryToggle(
                        selected = { chromaticAberrationEnabled },
                        onSelect = onChromaticAberrationChange,
                        backdrop = panelBackdrop
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugLiquidParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop,
    secondaryColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1
        )
        LiquidSlider(
            value = { value.coerceIn(0f, 1f) },
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}
