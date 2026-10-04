package com.dlmaster.util
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
object PermissionHelper {
    private const val REQ = 1001; private const val REQ_N = 1002
    fun requestBase(a: Activity) {
        val p = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(a, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) p += Manifest.permission.READ_MEDIA_AUDIO
        } else {
            if (ContextCompat.checkSelfPermission(a, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) p += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (p.isNotEmpty()) try { ActivityCompat.requestPermissions(a, p.toTypedArray(), REQ) } catch (_: Throwable) {}
    }
    fun requestNotifications(a: Activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(a, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                try { ActivityCompat.requestPermissions(a, arrayOf("android.permission.POST_NOTIFICATIONS"), REQ_N) } catch (_: Throwable) {}
        }
    }
}
