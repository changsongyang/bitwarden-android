package com.x8bit.bitwarden.ui.platform.components.appbar.action

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.color.bitwardenMenuItemColors
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Represents a composable overflow item specifically tailored for Bitwarden's UI.
 *
 * This composable wraps an [IconButton] with an "overflow" icon, typically used to
 * indicate more actions available that are not immediately visible on the interface.
 * The item is centrally aligned within a predefined [Box] of size 24.dp.
 *
 * @param menuItemDataList The list of [OverflowMenuItemData] that will populate the overflow
 * dropdown menu.
 */
@Composable
fun BitwardenOverflowActionItem(
    menuItemDataList: ImmutableList<OverflowMenuItemData>,
    modifier: Modifier = Modifier,
    @DrawableRes vectorIconRes: Int = R.drawable.ic_ellipsis_vertical,
    contentDescription: String = stringResource(id = R.string.more),
    testTag: String? = "HeaderBarOptionsButton",
) {
    if (menuItemDataList.isEmpty()) return
    var isOverflowMenuVisible by rememberSaveable { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        BitwardenStandardIconButton(
            vectorIconRes = vectorIconRes,
            contentDescription = contentDescription,
            onClick = { isOverflowMenuVisible = !isOverflowMenuVisible },
            modifier = Modifier.nullableTestTag(tag = testTag),
        )
        DropdownMenu(
            shape = BitwardenTheme.shapes.menu,
            expanded = isOverflowMenuVisible,
            onDismissRequest = { isOverflowMenuVisible = false },
            offset = DpOffset(x = (-12).dp, y = 0.dp),
            containerColor = BitwardenTheme.colorScheme.background.primary,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("FloatingOptionsContent")
                .widthIn(
                    min = 112.dp,
                    max = 280.dp,
                ),
            content = {
                menuItemDataList.forEach { dropdownMenuItemData ->
                    DropdownMenuItem(
                        modifier = Modifier.testTag("FloatingOptionsItem"),
                        colors = bitwardenMenuItemColors(
                            textColor = dropdownMenuItemData
                                .color
                                .takeUnless { it == Color.Unspecified }
                                ?: BitwardenTheme.colorScheme.text.primary,
                        ),
                        enabled = dropdownMenuItemData.isEnabled,
                        text = {
                            Text(
                                text = dropdownMenuItemData.text,
                                style = BitwardenTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("FloatingOptionsItemName"),
                            )
                        },
                        onClick = {
                            isOverflowMenuVisible = false
                            dropdownMenuItemData.onClick()
                        },
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenOverflowActionItem_preview() {
    BitwardenTheme {
        BitwardenOverflowActionItem(
            menuItemDataList = persistentListOf(
                OverflowMenuItemData(
                    text = "Test",
                    onClick = {},
                ),
            ),
        )
    }
}

/**
 * Data used to populate one row of an overflow dropdown menu.
 *
 * @param text The text displayed for the item in the menu.
 * @param onClick A callback for when the menu item is clicked.
 * @param isEnabled Indicates that this overflow item is enabled or not.
 * @param color The color of the content.
 */
data class OverflowMenuItemData(
    val text: String,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
    val color: Color = Color.Unspecified,
)
