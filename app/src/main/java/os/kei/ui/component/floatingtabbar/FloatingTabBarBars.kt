@file:OptIn(ExperimentalSharedTransitionApi::class)

/*
 * Adapted from compose-floating-tab-bar.
 * Copyright 2025 Elyes Mansour.
 * SPDX-License-Identifier: Apache-2.0
 */

package os.kei.ui.component.floatingtabbar

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
internal fun SharedTransitionScope.InlineBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    onInlineTabClick: () -> Unit,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)
    val standaloneTab = scope.standaloneTab
    val hasInlineTab = inlineTab != null
    val hasStandaloneTab = standaloneTab != null

    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (tabGroupRef, accessoryRef, standaloneTabRef) = createRefs()

        if (hasInlineTab) {
            InlineTab(
                inlineTab = inlineTab,
                onInlineTabClick = onInlineTabClick,
                shapes = shapes,
                sizes = sizes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier.constrainAs(tabGroupRef) {
                    start.linkTo(parent.start)
                    centerVerticallyTo(parent)
                }
            )
        }

        if (accessory != null) {
            InlineAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.constrainAs(accessoryRef) {
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                    centerVerticallyTo(parent)
                    when {
                        hasInlineTab && hasStandaloneTab -> {
                            start.linkTo(tabGroupRef.end, sizes.componentSpacing)
                            end.linkTo(standaloneTabRef.start, sizes.componentSpacing)
                        }
                        hasInlineTab -> {
                            start.linkTo(tabGroupRef.end, sizes.componentSpacing)
                            end.linkTo(parent.end)
                        }
                        hasStandaloneTab -> {
                            start.linkTo(parent.start)
                            end.linkTo(standaloneTabRef.start, sizes.componentSpacing)
                        }
                        else -> {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                    }
                }
            )
        }

        if (hasStandaloneTab) {
            InlineStandaloneTab(
                standaloneTab = standaloneTab,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier.constrainAs(standaloneTabRef) {
                    width = Dimension.ratio("1:1")
                    end.linkTo(parent.end)
                    if (hasInlineTab) {
                        height = Dimension.fillToConstraints
                        centerVerticallyTo(tabGroupRef)
                    }
                }
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.InlineTab(
    inlineTab: FloatingTabBarTab,
    onInlineTabClick: () -> Unit,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    Box(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.tabBarShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.tabBarShape
            )
            .clip(shapes.tabBarShape)
            .then(tabBarContentModifier)
            .clickable(
                onClick = {
                    onInlineTabClick()
                    inlineTab.onClick()
                },
                indication = inlineTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(sizes.tabInlineContentPadding)
    ) {
        Tab(
            icon = {
                Box(
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState("tab#${inlineTab.key}-icon"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 1f
                    )
                ) {
                    inlineTab.icon()
                }
            },
            title = { inlineTab.title() },
            isInline = true
        )
    }
}

@Composable
private fun SharedTransitionScope.InlineStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    shapes: FloatingTabBarShapes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = true,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.inlineElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier)
            .clickable(
                onClick = standaloneTab.onClick,
                indication = standaloneTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
    )
}

@Composable
private fun SharedTransitionScope.InlineAccessory(
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    accessory?.let { accessory ->
        Box(
            modifier = modifier
                .then(
                    if (isAccessoryShared) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState("accessory"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    } else {
                        Modifier.animateEnterExitAccessory(
                            sharedTransitionScope = this,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                )
        ) {
            accessory(
                Modifier
                    .fillMaxSize()
                    .shadow(
                        shape = shapes.accessoryShape,
                        elevation = elevations.inlineElevation
                    )
                    .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                    .clip(shapes.accessoryShape),
                animatedVisibilityScope
            )
        }
    }
}

@Composable
internal fun SharedTransitionScope.ExpandedBar(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    elevations: FloatingTabBarElevations,
    tabBarContentModifier: Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val standaloneTab = scope.standaloneTab
    val hasStandaloneTab = standaloneTab != null
    val hasTabGroup = scope.tabs.isNotEmpty()

    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (accessoryRef, tabGroupRef, standaloneTabRef) = createRefs()

        if (accessory != null) {
            ExpandedAccessory(
                accessory = accessory,
                isAccessoryShared = isAccessoryShared,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.constrainAs(accessoryRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
            )
        }

        if (hasTabGroup) {
            ExpandedTabs(
                scope = scope,
                selectedTabKey = selectedTabKey,
                shapes = shapes,
                sizes = sizes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier
                    .constrainAs(tabGroupRef) {
                        width = Dimension.fillToConstraints
                        start.linkTo(parent.start)
                        if (hasStandaloneTab) {
                            end.linkTo(standaloneTabRef.start, margin = sizes.componentSpacing)
                        } else {
                            end.linkTo(parent.end)
                        }
                        if (accessory != null) {
                            top.linkTo(accessoryRef.bottom, margin = sizes.componentSpacing)
                        }
                        horizontalBias = 0f
                    }
                    .wrapContentWidth(align = Alignment.Start)
            )
        }

        if (hasStandaloneTab) {
            ExpandedStandaloneTab(
                standaloneTab = standaloneTab,
                shapes = shapes,
                colors = colors,
                elevations = elevations,
                animatedVisibilityScope = animatedVisibilityScope,
                tabBarContentModifier = tabBarContentModifier,
                modifier = Modifier.constrainAs(standaloneTabRef) {
                    width = Dimension.ratio("1:1")
                    end.linkTo(parent.end)
                    if (hasTabGroup) {
                        height = Dimension.fillToConstraints
                        centerVerticallyTo(tabGroupRef)
                    } else if (accessory != null) {
                        top.linkTo(accessoryRef.bottom, margin = sizes.componentSpacing)
                    }
                }
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedAccessory(
    accessory: @Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit,
    isAccessoryShared: Boolean,
    colors: FloatingTabBarColors,
    shapes: FloatingTabBarShapes,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .then(
                if (isAccessoryShared) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState("accessory"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                } else {
                    Modifier.animateEnterExitAccessory(
                        sharedTransitionScope = this,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            )
    ) {
        accessory(
            Modifier
                .shadow(
                    shape = shapes.accessoryShape,
                    elevation = elevations.expandedElevation
                )
                .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
                .clip(shapes.accessoryShape),
            animatedVisibilityScope
        )
    }
}
@Composable
private fun SharedTransitionScope.ExpandedTabs(
    scope: FloatingTabBarScopeImpl,
    selectedTabKey: Any?,
    shapes: FloatingTabBarShapes,
    sizes: FloatingTabBarSizes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    val inlineTab = scope.getInlineTab(selectedTabKey)

    Row(
        horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing),
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.tabBarShape,
                elevation = elevations.expandedElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.tabBarShape
            )
            .clip(shapes.tabBarShape)
            .then(tabBarContentModifier)
            .padding(sizes.tabBarContentPadding)
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .animateContentSize()
    ) {
        scope.tabs.forEach { tab ->
            Tab(
                icon = {
                    Box(
                        modifier = if (tab.key == inlineTab?.key) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState("tab#${tab.key}-icon"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                zIndexInOverlay = 1f
                            )
                        } else {
                            Modifier.animateEnterExitTab(
                                sharedTransitionScope = this@ExpandedTabs,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    ) {
                        tab.icon()
                    }
                },
                title = {
                    Box(
                        Modifier.animateEnterExitTab(
                            sharedTransitionScope = this@ExpandedTabs,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    ) {
                        tab.title()
                    }
                },
                isInline = false,
                modifier = Modifier
                    .skipToLookaheadSize()
                    .clip(shapes.tabShape)
                    .clickable(
                        onClick = tab.onClick,
                        indication = tab.indication?.invoke(),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(sizes.tabExpandedContentPadding)
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ExpandedStandaloneTab(
    standaloneTab: FloatingTabBarTab,
    shapes: FloatingTabBarShapes,
    colors: FloatingTabBarColors,
    elevations: FloatingTabBarElevations,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    tabBarContentModifier: Modifier
) {
    Tab(
        icon = standaloneTab.icon,
        title = standaloneTab.title,
        isInline = false,
        isStandalone = true,
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState("standaloneTab"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
            )
            .shadow(
                shape = shapes.standaloneTabShape,
                elevation = elevations.expandedElevation
            )
            .background(
                color = colors.backgroundColor,
                shape = shapes.standaloneTabShape
            )
            .clip(shapes.standaloneTabShape)
            .then(tabBarContentModifier)
            .clickable(
                onClick = standaloneTab.onClick,
                indication = standaloneTab.indication?.invoke(),
                interactionSource = remember { MutableInteractionSource() }
            )
    )
}

@Composable
private fun Tab(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    isInline: Boolean,
    modifier: Modifier = Modifier,
    isStandalone: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        icon()
        if (!isStandalone && !isInline) {
            title()
        }
    }
}

/**
 * A custom modifier that provides smooth enter/exit animations without clipping shadows or other content.
 * This is an alternative to animateEnterExit that uses renderInSharedTransitionScopeOverlay to prevent clipping.
 */
@Composable
private fun Modifier.animateEnterExitAccessory(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val animatedAlpha by transition.animateFloat { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        this@animateEnterExitAccessory
            .renderInSharedTransitionScopeOverlay()
            .graphicsLayer(
                compositingStrategy = CompositingStrategy.ModulateAlpha,
                alpha = animatedAlpha
            )
    }
}

/**
 * A custom modifier that provides smooth enter/exit animations with fade and blur effects.
 */
@Composable
private fun Modifier.animateEnterExitTab(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    with(animatedVisibilityScope) {
        val enterStartFraction = 0.35f
        val enterEndFraction = 0.9f
        val durationMs = 210

        val animatedAlpha by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        0f atFraction enterStartFraction using FastOutSlowInEasing
                        1f atFraction enterEndFraction
                    }
                }
            }
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 1f
                else -> 0f
            }
        }

        val blurRadius = with(LocalDensity.current) { 18.dp.toPx() }
        val animatedBlur by transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = durationMs
                    if (targetState == EnterExitState.Visible) {
                        blurRadius atFraction enterStartFraction using FastOutSlowInEasing
                        0f atFraction enterEndFraction
                    }
                }
            }
        ) { targetState ->
            when (targetState) {
                EnterExitState.Visible -> 0f
                else -> blurRadius
            }
        }

        graphicsLayer {
            alpha = animatedAlpha
            renderEffect = BlurEffect(
                radiusX = animatedBlur,
                radiusY = animatedBlur
            )
        }
    }
}
