package com.aela.mainboardoverride.ui

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R

/** Generated brushed chrome fills live, localized letters; offset edges give them relief. */
@Composable
internal fun MetalTitle(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 22.sp) {
    val texture = ImageBitmap.imageResource(R.drawable.metal_title_texture_v1)
    val brush = remember(texture) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader =
                BitmapShader(texture.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                    setLocalMatrix(Matrix().apply {
                        setScale(size.width / texture.width, size.height / texture.height)
                    })
                }
        }
    }
    val style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
        lineHeight = fontSize * 1.1f)
    Box(modifier) {
        Text(text, Modifier.offset(y = 2.dp).clearAndSetSemantics {}, color = Color(0xFF15201F), style = style)
        Text(text, Modifier.offset(y = (-1).dp).clearAndSetSemantics {}, color = Color(0xFFF3FFFF), style = style)
        Text(text, style = style.copy(brush = brush))
    }
}
