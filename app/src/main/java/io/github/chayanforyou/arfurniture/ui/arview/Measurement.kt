package io.github.chayanforyou.arfurniture.ui.arview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ModelMeasurements(
    width: Float,
    height: Float,
    depth: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "Height: ${String.format("%.1f", height)}m",
            fontSize = 14.sp
        )
        Text(
            text = "Width: ${String.format("%.1f", width)}m",
            fontSize = 14.sp
        )
        Text(
            text = "Depth: ${String.format("%.1f", depth)}m",
            fontSize = 14.sp
        )
    }
}