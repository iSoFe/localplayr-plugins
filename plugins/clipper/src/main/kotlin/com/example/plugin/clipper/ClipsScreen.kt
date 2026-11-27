package com.example.plugin.clipper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plugins.PluginHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val Accent = Color(0xFFFF7A59)

@Composable
fun ClipsScreen(
    host: PluginHost,
    store: ClipStore,
    playback: ClipPlayback,
    scope: CoroutineScope
) {
    val clips by store.clips.collectAsState()
    var renaming by remember { mutableStateOf<Clip?>(null) }
    var removing by remember { mutableStateOf<Clip?>(null) }
    var exporting by remember { mutableStateOf(setOf<String>()) }
    val library = host.libraryVideos()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    tr("Clips", "المقاطع"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    tr("Parts of your videos, saved to replay or export", "أجزاء من فيديوهاتك محفوظة لإعادة تشغيلها أو تصديرها"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (clips.isEmpty()) {
            item {
                ClipCardSurface {
                    Text(
                        tr("No clips yet", "لا توجد مقاطع بعد"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        tr(
                            "While a video plays, open the three-dots menu, choose \"Clip: set start\" where the part begins, then \"Clip: set end & save\" where it ends.",
                            "أثناء تشغيل الفيديو افتح قائمة النقاط الثلاث، اختر «المقطع: تحديد البداية» عند بداية الجزء ثم «المقطع: تحديد النهاية وحفظ» عند نهايته."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        items(clips, key = { it.id }) { clip ->
            val videoAvailable = library.any { it.id == clip.videoId }
            val busy = clip.id in exporting
            ClipCardSurface {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Accent.copy(alpha = 0.18f))
                            .border(1.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                    ) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            clip.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Pill("${formatTime(clip.startMs)} – ${formatTime(clip.endMs)}", highlighted = true)
                            Pill(formatTime(clip.durationMs))
                        }
                        if (!videoAvailable) {
                            Text(
                                tr("Original video not found", "الفيديو الأصلي غير موجود"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                ) {
                    ActionPill(
                        text = tr("Play", "تشغيل"),
                        icon = Icons.Filled.PlayArrow,
                        primary = true,
                        enabled = videoAvailable,
                        onClick = { playback.play(clip) }
                    )
                    ActionPill(
                        text = if (busy) tr("Saving…", "جارٍ الحفظ…") else tr("Save as video", "حفظ كفيديو"),
                        icon = Icons.Filled.Save,
                        primary = false,
                        enabled = !busy,
                        onClick = {
                            exporting = exporting + clip.id
                            host.toast(tr("Saving the clip…", "جارٍ حفظ المقطع…"))
                            scope.launch {
                                val result = ClipExporter.export(host.appContext, clip)
                                exporting = exporting - clip.id
                                result.fold(
                                    onSuccess = { path ->
                                        host.toast(tr("Saved to $path", "تم الحفظ في $path"))
                                    },
                                    onFailure = { error ->
                                        host.log("Export failed: ${error.message}")
                                        host.toast(
                                            tr("Could not save: ", "تعذّر الحفظ: ") + (error.message ?: error.javaClass.simpleName)
                                        )
                                    }
                                )
                            }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    RoundIcon(Icons.Filled.Edit, tr("Rename", "إعادة تسمية")) { renaming = clip }
                    RoundIcon(Icons.Filled.Delete, tr("Delete", "حذف"), tint = Color(0xFFFF8F8F)) { removing = clip }
                }
            }
        }
    }

    renaming?.let { clip ->
        var text by remember(clip.id) { mutableStateOf(clip.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Rename clip", "إعادة تسمية المقطع")) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) store.rename(clip.id, text.trim())
                    renaming = null
                }) { Text(tr("Save", "حفظ")) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Cancel", "إلغاء")) } }
        )
    }

    removing?.let { clip ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(tr("Delete clip", "حذف المقطع")) },
            text = {
                Text(
                    tr(
                        "Delete \"${clip.name}\"? The original video is not touched.",
                        "حذف «${clip.name}»؟ الفيديو الأصلي لن يتأثر."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.remove(clip.id)
                    removing = null
                }) { Text(tr("Delete", "حذف")) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(tr("Cancel", "إلغاء")) } }
        )
    }
}

@Composable
private fun ClipCardSurface(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Pill(text: String, highlighted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (highlighted) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (highlighted) Accent.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActionPill(text: String, icon: ImageVector, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val container = if (primary) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val content = if (primary) Color(0xFF0C0E13) else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = content, maxLines = 1)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, tint: Color? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}
