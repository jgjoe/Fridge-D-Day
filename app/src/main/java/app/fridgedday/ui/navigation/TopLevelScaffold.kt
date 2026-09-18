package app.fridgedday.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private val FloatingActionSeparation = 16.dp
internal val AddFoodActionLane: Dp = AddFoodButtonSize + FloatingActionSeparation
internal val LocalTopLevelBottomContentInset = compositionLocalOf { 0.dp }
internal val TopLevelSidePadding = 16.dp

/** Shared shell for Today, Record, and Settings. */
@Composable
fun TopLevelScaffold(
    selected: TopLevelDestination,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    showAddAction: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val bottomContentInset = if (showAddAction) AddFoodActionLane else 0.dp

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BrandHeader(onSearchClick = onSearchClick) },
        bottomBar = {
            AppBottomBar(
                selected = selected,
                onDestinationSelected = { destination ->
                    navController.navigateToTopLevel(destination)
                }
            )
        },
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = bottomContentInset)) { snackbarHost() }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalTopLevelBottomContentInset provides bottomContentInset
            ) {
                content(paddingValues)
            }
            if (showAddAction) {
                AddFoodButton(
                    onClick = { navController.navigateToScan() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = TopLevelSidePadding,
                            bottom = paddingValues.calculateBottomPadding() + FloatingActionSeparation
                        )
                )
            }
        }
    }
}

/** Compact brand row shared by all three top-level destinations. */
@Composable
private fun BrandHeader(onSearchClick: (() -> Unit)?) {
    val searchInteractionSource = remember { MutableInteractionSource() }
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = TopLevelSidePadding, end = 8.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLeafMark()
            Text(
                text = "오늘도 신선",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .semantics { heading() }
            )
            if (onSearchClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onSearchClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "식품 검색"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

/** Search stays absent at rest and occupies one bounded row only while active. */
@Composable
internal fun TopLevelSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text("이름으로 검색") },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = closeInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            if (value.isEmpty()) onClose() else onValueChange("")
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = if (value.isEmpty()) "검색 닫기" else "검색어 지우기"
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TopLevelSidePadding, vertical = 6.dp)
    )
}

@Composable
internal fun GroupSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        content = { Column(content = content) }
    )
}

@Composable
internal fun GroupHairline(insetStart: Dp = TopLevelSidePadding) {
    HorizontalDivider(
        modifier = Modifier.padding(start = insetStart),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.navigateToScan() {
    navigate(Destinations.SCAN) { launchSingleTop = true }
}
