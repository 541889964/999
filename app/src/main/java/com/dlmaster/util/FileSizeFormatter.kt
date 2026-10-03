package com.dlmaster.util

object FileSizeFormatter {
    fun fmt(bytes: Long): String {
        if (bytes <= 0) return "--"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else    -> "$bytes B"
        }
    }
}
