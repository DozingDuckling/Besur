package dev.dozingduckling.besur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.dozingduckling.besur.ui.theme.BesurTheme

class MainActivity : ComponentActivity()
{
    private var music by mutableStateOf<List<Music>>(emptyList())
    private var isPlaying by mutableStateOf(false)
    private lateinit var musicRepository: MusicRepository
    private lateinit var player: ExoPlayer

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
        player = ExoPlayer.Builder(this).build()

        setContent {
            BesurTheme {
                MusicList(
                        music = music,
                        isPlaying = isPlaying,
                        onSongClick = { song -> playMusic(song) },
                        onPlayPauseClick = { tooglePlayPause() }
                )
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

    private fun playMusic(song: Music)
    {
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
        isPlaying = true
    }

    private fun tooglePlayPause()
    {
        if (player.isPlaying)
        {
            player.pause()
            isPlaying = false
        }
        else
        {
            player.play()
            isPlaying = true
        }
    }

    override fun onDestroy()
    {
        player.release()
        super.onDestroy()
    }
}

@Composable
fun MusicList(music: List<Music>, isPlaying: Boolean, onSongClick: (Music) -> Unit, onPlayPauseClick: () -> Unit)
{
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
    ) {
        LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            items(music) { song ->
                Column(
                        modifier = Modifier.clickable { onSongClick(song) }
                ) {
                    Text(song.title)
                    Text(song.artist)
                }
            }
        }
        Button(
                onClick = onPlayPauseClick
        ) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    }
}