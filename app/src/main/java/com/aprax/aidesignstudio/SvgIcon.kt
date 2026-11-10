package com.aprax.aidesignstudio

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG

/** Draws a bundled Lucide-style SVG asset. Unknown names safely render nothing. */
@Composable
fun SvgIcon(name: String, modifier: Modifier = Modifier, iconSize: Dp = 20.dp) {
    val context = LocalContext.current
    val picture = remember(name) {
        runCatching { SVG.getFromAsset(context.assets, "icons/$name.svg").renderToPicture() }.getOrNull()
    }
    Canvas(modifier = modifier.size(iconSize)) {
        val svg = picture ?: return@Canvas
        val target = RectF(0f, 0f, this.size.width, this.size.height)
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(svg, target) }
    }
}
