package com.dlmaster.deeplink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dlmaster.util.CommandDownloader

class ThunderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.toString()?.let { CommandDownloader.smartDownload(it, this) }
        finish()
    }
}
