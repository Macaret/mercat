package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.common.BluePill
import com.smartcodeapps.constructionprizewheel.ui.common.GlossyPillButton
import com.smartcodeapps.constructionprizewheel.ui.common.NavyPill
import com.smartcodeapps.constructionprizewheel.ui.common.RedPill
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

/** Sections shown in the right-hand panel of the admin layout. */
enum class AdminSection { PRIZES, HISTORY, SETTINGS }

object AdminLayoutTestTags {
    const val MENU_PRIZES = "admin_menu_prizes"
    const val MENU_HISTORY = "admin_menu_history"
    const val MENU_SETTINGS = "admin_menu_settings"
    const val MENU_EXIT = "admin_menu_exit"
}

private val SidebarColor = Color(0xFF13233D)
private val ContentPanelColor = Color(0xFFF4F7FB)
private val MENU_WIDTH = 260.dp
private val MENU_BUTTON_HEIGHT = 64.dp

/**
 * Two-pane admin layout: a left menu (logo, section buttons, exit) and a right panel
 * that shows the selected section's content.
 */
@Composable
fun AdminLayout(
    selected: AdminSection,
    onSelect: (AdminSection) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(SidebarColor)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(MENU_WIDTH).fillMaxHeight()
        ) {
            Image(
                painter = painterResource(R.drawable.mercat_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(top = 8.dp).height(96.dp)
            )
            Spacer(Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                MenuItem(
                    text = stringResource(R.string.prizes_button),
                    selected = selected == AdminSection.PRIZES,
                    tag = AdminLayoutTestTags.MENU_PRIZES,
                    onClick = { onSelect(AdminSection.PRIZES) }
                )
                MenuItem(
                    text = stringResource(R.string.history_button),
                    selected = selected == AdminSection.HISTORY,
                    tag = AdminLayoutTestTags.MENU_HISTORY,
                    onClick = { onSelect(AdminSection.HISTORY) }
                )
                MenuItem(
                    text = stringResource(R.string.settings_button),
                    selected = selected == AdminSection.SETTINGS,
                    tag = AdminLayoutTestTags.MENU_SETTINGS,
                    onClick = { onSelect(AdminSection.SETTINGS) }
                )
            }
            GlossyPillButton(
                text = stringResource(R.string.exit_to_wheel_button),
                onClick = onExit,
                palette = RedPill,
                textSize = 18.sp,
                modifier = Modifier.height(MENU_BUTTON_HEIGHT).testTag(AdminLayoutTestTags.MENU_EXIT)
            )
        }

        Spacer(Modifier.width(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(ContentPanelColor)
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MenuItem(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    GlossyPillButton(
        text = text,
        onClick = onClick,
        palette = if (selected) BluePill else NavyPill,
        textSize = 18.sp,
        modifier = Modifier.height(MENU_BUTTON_HEIGHT).testTag(tag)
    )
}

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun AdminLayoutPreview() {
    ConstructionPrizeWheelTheme {
        AdminLayout(
            selected = AdminSection.PRIZES,
            onSelect = {},
            onExit = {}
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Content")
            }
        }
    }
}
