package dev.dozingduckling.besur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import dev.dozingduckling.besur.ui.theme.BesurTheme

class MainActivity : ComponentActivity()
{
    private val musicPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted)
            {
                // TODO: scan for music
            }
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BesurTheme {
                Text("Besur")
            }
        }

        requestMusicPermission()
    }

    private fun requestMusicPermission()
    {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            Manifest.permission.READ_MEDIA_AUDIO
        }
        else
        {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED
        )
        {
            musicPermissionLauncher.launch(permission)
        }
    }
}