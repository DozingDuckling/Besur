package dev.dozingduckling.besur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.dozingduckling.besur.ui.theme.BesurTheme

class MainActivity : ComponentActivity()
{
    private var music by mutableStateOf<List<Music>>(emptyList())
    private lateinit var musicRepository: MusicRepository

    private val musicPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    { granted ->
        if (granted)
        {
            loadMusic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        musicRepository = MusicRepository(contentResolver)

        setContent {
            BesurTheme {
                MusicList(music)
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

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
        {
            loadMusic()
        }
        else
        {
            musicPermissionLauncher.launch(permission)
        }
    }

    private fun loadMusic()
    {
        music = musicRepository.getMusic()
    }
}

@androidx.compose.runtime.Composable
fun MusicList(music: List<Music>)
{
    LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(music) { song ->
            Column {
                Text(song.title)
                Text(song.artist)
            }
        }
    }
}