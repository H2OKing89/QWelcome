package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Cyberpunk-styled top app bar wrapping Material 3 [TopAppBar].
 *
 * Confines @OptIn(ExperimentalMaterial3Api::class) to this component so that
 * screens using the top bar do not need the annotation themselves.
 *
 * Defaults to a transparent container with [MaterialTheme.colorScheme.onBackground]
 * content colors, matching the cyberpunk backdrop design.
 *
 * @param title                      Title composable.
 * @param modifier                   Modifier applied to the [TopAppBar].
 * @param navigationIcon             Optional navigation icon composable.
 * @param actions                    Optional row of action icons.
 * @param containerColor             Background color (transparent by default for backdrop).
 * @param titleContentColor          Color for the title content.
 * @param navigationIconContentColor Color for the navigation icon.
 * @param actionIconContentColor     Color for action icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = Color.Transparent,
    titleContentColor: Color = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor: Color = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor: Color = MaterialTheme.colorScheme.onBackground
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor,
            navigationIconContentColor = navigationIconContentColor,
            actionIconContentColor = actionIconContentColor
        )
    )
}
