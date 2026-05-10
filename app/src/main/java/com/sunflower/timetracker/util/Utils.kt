package com.sunflower.timetracker.util


fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}

fun formatDurationShort(ms: Long): String {
    val totalMinutes = ms / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatHours(ms: Long): String {
    val hours = ms / 3600000.0
    return "%.1fh".format(hours)
}

//fun formatDateTime(epochMs: Long): String {
//    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
//    return sdf.format(java.util.Date(epochMs))
//}

fun formatTime(epochMs: Long): String {
    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(epochMs))
}

fun formatDate(epochMs: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(epochMs))
}

fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16).toInt()
        if (cleaned.length == 6) {
            androidx.compose.ui.graphics.Color(0xFF000000.toInt() or colorInt)
        } else {
            androidx.compose.ui.graphics.Color(colorInt)
        }
    } catch (_: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }
}