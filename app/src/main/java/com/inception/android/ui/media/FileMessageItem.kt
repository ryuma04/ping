package com.inception.android.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inception.android.R
import com.inception.android.features.file.FileUtils
import com.inception.android.model.InceptionFilePacket

/**
 * Modern chat-style file message display with document type icon,
 * sanitized filename, formatted size, and an Open action button.
 */
@Composable
fun FileMessageItem(
    packet: InceptionFilePacket,
    filePath: String? = null,
    onFileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val sanitizedFileName = remember(packet.fileName) {
        FileUtils.sanitizeFileName(packet.fileName)
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .clickable {
                showDialog = true
                onFileClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MIME-specific file icon
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = stringResource(R.string.cd_file),
                tint = getFileIconColor(sanitizedFileName),
                modifier = Modifier.size(36.dp)
            )

            // File metadata: sanitized name, size, type badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sanitizedFileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = FileUtils.formatFileSize(packet.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FileTypeBadge(mimeType = packet.mimeType, fileName = sanitizedFileName)
                }
            }

            // Explicit Open action button
            FilledTonalButton(
                onClick = {
                    showDialog = true
                    onFileClick()
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.file_viewer_open),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // File viewer / save dialog
    if (showDialog) {
        FileViewerDialog(
            packet = packet,
            directFilePath = filePath,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Small badge showing file type
 */
@Composable
private fun FileTypeBadge(mimeType: String, fileName: String = "") {
    val ext = fileName.substringAfterLast(".", "").lowercase()
    val (text, color) = when {
        mimeType.startsWith("application/pdf") || ext == "pdf" -> "PDF" to Color(0xFFDC2626)
        ext == "docx" || ext == "doc" || mimeType.contains("wordprocessingml") -> "DOC" to Color(0xFF1D4ED8)
        ext == "xlsx" || ext == "xls" || mimeType.contains("spreadsheetml") -> "XLS" to Color(0xFF059669)
        ext == "csv" || mimeType == "text/csv" -> "CSV" to Color(0xFF0D9488)
        ext == "md" || mimeType == "text/markdown" -> "MD" to Color(0xFF4F46E5)
        mimeType.startsWith("text/") || ext == "txt" -> "TXT" to Color(0xFF059669)
        mimeType.startsWith("image/") -> "IMG" to Color(0xFF7C3AED)
        mimeType.startsWith("audio/") -> "AUD" to Color(0xFFEA580C)
        mimeType.startsWith("video/") -> "VID" to Color(0xFF2563EB)
        mimeType.contains("zip") || mimeType.contains("rar") || ext in listOf("zip", "rar", "7z", "tar", "gz") -> "ZIP" to Color(0xFF7C2D12)
        else -> "FILE" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Get appropriate icon color based on file extension
 */
private fun getFileIconColor(fileName: String): Color {
    val extension = fileName.substringAfterLast(".", "").lowercase()
    return when (extension) {
        "pdf" -> Color(0xFFDC2626) // Red
        "doc", "docx" -> Color(0xFF1D4ED8) // Blue
        "xls", "xlsx" -> Color(0xFF059669) // Green
        "csv" -> Color(0xFF0D9488) // Teal
        "ppt", "pptx" -> Color(0xFFEA580C) // Orange
        "txt", "json", "xml", "md" -> Color(0xFF7C3AED) // Purple
        "jpg", "jpeg", "png", "gif", "webp" -> Color(0xFF2563EB) // Blue
        "mp3", "wav", "m4a", "ogg" -> Color(0xFFEA580C) // Orange
        "mp4", "avi", "mov", "mkv" -> Color(0xFFDC2626) // Red
        "zip", "rar", "7z", "gz", "tar" -> Color(0xFF7C2D12) // Brown
        else -> Color(0xFF6B7280) // Gray
    }
}
