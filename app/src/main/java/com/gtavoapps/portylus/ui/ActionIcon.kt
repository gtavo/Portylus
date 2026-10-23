package com.gtavoapps.portylus.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Small native vectors; no icon-font or extra dependency. */
@Composable fun ActionIcon(kind: String, enabled: Boolean = true, tint: Color? = null) {
    val color = (tint ?: MaterialTheme.colorScheme.onSurface).copy(alpha = if (enabled) 1f else 0.35f)
    Canvas(Modifier.size(22.dp)) {
        val u = size.width / 24f
        val line = Stroke(1.8f * u, cap = StrokeCap.Round)
        when (kind) {
            "undo", "redo" -> {
                fun x(v: Float) = if (kind == "redo") 24f - v else v
                val path = Path().apply {
                    moveTo(x(8f)*u,5f*u); lineTo(x(3f)*u,10f*u); lineTo(x(8f)*u,15f*u)
                    moveTo(x(3f)*u,10f*u); lineTo(x(15f)*u,10f*u)
                    cubicTo(x(22f)*u,10f*u,x(22f)*u,20f*u,x(15f)*u,20f*u)
                }
                drawPath(path,color,style=line)
            }
            "back" -> {
                val path=Path().apply { moveTo(15f*u,4f*u);lineTo(7f*u,12f*u);lineTo(15f*u,20f*u) }
                drawPath(path,color,style=line)
            }
            "pen" -> {
                val path=Path().apply {
                    moveTo(15f*u,3f*u); lineTo(21f*u,9f*u); lineTo(10f*u,20f*u)
                    lineTo(3f*u,21f*u); lineTo(4f*u,14f*u); close()
                    moveTo(12f*u,6f*u); lineTo(18f*u,12f*u)
                    moveTo(4f*u,14f*u); lineTo(10f*u,20f*u)
                }
                drawPath(path,color,style=line)
            }
            "highlighter" -> {
                val path=Path().apply {
                    moveTo(14f*u,3f*u); lineTo(21f*u,10f*u); lineTo(13f*u,18f*u)
                    lineTo(6f*u,11f*u); close()
                    moveTo(6f*u,11f*u); lineTo(3f*u,17f*u); lineTo(7f*u,21f*u); lineTo(13f*u,18f*u)
                    moveTo(11f*u,6f*u); lineTo(18f*u,13f*u)
                }
                drawPath(path,color,style=line)
                drawLine(color,Offset(3f*u,22f*u),Offset(15f*u,22f*u),strokeWidth=2f*u,cap=StrokeCap.Round)
            }
            "selectText" -> {
                val path=Path().apply {
                    moveTo(7f*u,7f*u);lineTo(17f*u,7f*u);moveTo(12f*u,7f*u);lineTo(12f*u,17f*u)
                    moveTo(9f*u,17f*u);lineTo(15f*u,17f*u)
                    moveTo(3f*u,7f*u);lineTo(3f*u,3f*u);lineTo(7f*u,3f*u)
                    moveTo(17f*u,3f*u);lineTo(21f*u,3f*u);lineTo(21f*u,7f*u)
                    moveTo(3f*u,17f*u);lineTo(3f*u,21f*u);lineTo(7f*u,21f*u)
                    moveTo(17f*u,21f*u);lineTo(21f*u,21f*u);lineTo(21f*u,17f*u)
                }
                drawPath(path,color,style=line)
            }
            "eraser" -> {
                val path=Path().apply {
                    moveTo(14f*u,3f*u);lineTo(22f*u,11f*u);lineTo(12f*u,21f*u)
                    lineTo(6f*u,21f*u);lineTo(2f*u,17f*u);close()
                    moveTo(8f*u,11f*u);lineTo(16f*u,19f*u)
                    moveTo(12f*u,21f*u);lineTo(22f*u,21f*u)
                }
                drawPath(path,color,style=line)
            }
            "settings" -> {
                for ((y,x) in listOf(6f to 9f,12f to 16f,18f to 7f)) {
                    drawLine(color,Offset(3f*u,y*u),Offset(21f*u,y*u),strokeWidth=1.8f*u,cap=StrokeCap.Round)
                    drawCircle(color,2.5f*u,Offset(x*u,y*u),style=Stroke(1.8f*u))
                }
            }
            "hide", "show" -> {
                val path=Path().apply {
                    val start=if(kind=="hide") 15f else 9f
                    val end=if(kind=="hide") 9f else 15f
                    moveTo(5f*u,start*u);lineTo(12f*u,end*u);lineTo(19f*u,start*u)
                    moveTo(4f*u,3f*u);lineTo(20f*u,3f*u)
                }
                drawPath(path,color,style=line)
            }
            "save" -> {
                val path=Path().apply {
                    moveTo(4f*u,3f*u);lineTo(17f*u,3f*u);lineTo(21f*u,7f*u)
                    lineTo(21f*u,21f*u);lineTo(3f*u,21f*u);lineTo(3f*u,3f*u);close()
                    moveTo(7f*u,3f*u);lineTo(7f*u,9f*u);lineTo(16f*u,9f*u);lineTo(16f*u,3f*u)
                    moveTo(7f*u,21f*u);lineTo(7f*u,14f*u);lineTo(17f*u,14f*u);lineTo(17f*u,21f*u)
                }
                drawPath(path,color,style=line)
            }
            "folder" -> {
                val path = Path().apply {
                    moveTo(3f*u,6f*u); lineTo(10f*u,6f*u); lineTo(12f*u,9f*u)
                    lineTo(21f*u,9f*u); lineTo(21f*u,20f*u); lineTo(3f*u,20f*u); close()
                }
                drawPath(path,color,style=line)
            }
            "star", "starFilled" -> {
                val path = Path().apply {
                    for (i in 0..9) {
                        val angle = -Math.PI/2 + i*Math.PI/5
                        val radius = (if(i%2==0) 10f else 4.5f)*u
                        val x = 12f*u + kotlin.math.cos(angle).toFloat()*radius
                        val y = 12f*u + kotlin.math.sin(angle).toFloat()*radius
                        if(i==0) moveTo(x,y) else lineTo(x,y)
                    }
                    close()
                }
                if(kind=="starFilled") drawPath(path,color) else drawPath(path,color,style=line)
            }
            "chevron" -> {
                val path=Path().apply { moveTo(9f*u,7f*u);lineTo(14f*u,12f*u);lineTo(9f*u,17f*u) }
                drawPath(path,color,style=line)
            }
            "down" -> {
                val path=Path().apply { moveTo(7f*u,9f*u);lineTo(12f*u,14f*u);lineTo(17f*u,9f*u) }
                drawPath(path,color,style=line)
            }
            "sort" -> {
                drawLine(color,Offset(4f*u,6f*u),Offset(20f*u,6f*u),strokeWidth=1.8f*u,cap=StrokeCap.Round)
                drawLine(color,Offset(7f*u,12f*u),Offset(17f*u,12f*u),strokeWidth=1.8f*u,cap=StrokeCap.Round)
                drawLine(color,Offset(10f*u,18f*u),Offset(14f*u,18f*u),strokeWidth=1.8f*u,cap=StrokeCap.Round)
            }
            else -> for(y in listOf(5f,12f,19f)) drawCircle(color,1.7f*u,Offset(12f*u,y*u))
        }
    }
}
