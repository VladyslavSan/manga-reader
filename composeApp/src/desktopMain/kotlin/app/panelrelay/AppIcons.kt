package app.panelrelay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SidebarIcon = ImageVector.Builder("Sidebar", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(5f, 3f); lineTo(19f, 3f); quadTo(21f, 3f, 21f, 5f)
        lineTo(21f, 19f); quadTo(21f, 21f, 19f, 21f)
        lineTo(5f, 21f); quadTo(3f, 21f, 3f, 19f)
        lineTo(3f, 5f); quadTo(3f, 3f, 5f, 3f); close()
        moveTo(9f, 3f); lineTo(9f, 21f)
    }
}.build()

internal val SettingsIcon = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(4f, 7f); lineTo(9f, 7f); moveTo(13f, 7f); lineTo(20f, 7f)
        moveTo(9f, 4f); lineTo(13f, 4f); lineTo(13f, 10f); lineTo(9f, 10f); close()
        moveTo(4f, 17f); lineTo(13f, 17f); moveTo(17f, 17f); lineTo(20f, 17f)
        moveTo(13f, 14f); lineTo(17f, 14f); lineTo(17f, 20f); lineTo(13f, 20f); close()
    }
}.build()

internal val ToolbarIcon = ImageVector.Builder("Toolbar", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(3f, 19f); close()
        moveTo(3f, 10f); lineTo(21f, 10f)
    }
}.build()
