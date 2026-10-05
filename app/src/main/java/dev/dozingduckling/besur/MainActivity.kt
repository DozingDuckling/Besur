package dev.dozingduckling.besur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.dozingduckling.besur.ui.theme.BesurTheme

class MainActivity : ComponentActivity()
{
    private var music by mutableStateOf<List<Music>>(emptyList())
    private var currentSong by mutableStateOf<Music?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0L)
    private var duration by mutableStateOf(0L)
    private var showPlayer by mutableStateOf(false)
    private lateinit var musicRepository: MusicRepository
    private lateinit var player: ExoPlayer

    private val musicPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    { granted ->
        if (granted)
        {
            loadMusic()
        }
    }
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)

            progressHandler.postDelayed(this, 500)
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
                if (showPlayer) {
                    PlayerScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onPlayPauseClick = {
                                togglePlayPause()
                            },
                            onPreviousClick = {
                                playPreviousMusic()
                            },
                            onNextClick = {
                                playNextMusic()
                            },
                            onSeek = { position ->
                                player.seekTo(position)
                                currentPosition = position
                            }
                    )
                } else {
                    MusicList(
                            music = music,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onSongClick = { song ->
                                playMusic(music.indexOf(song))
                                showPlayer = true
                            },
                            onPlayPauseClick = {
                                togglePlayPause()
                            },
                            onPreviousClick = {
                                playPreviousMusic()
                            },
                            onNextClick = {
                                playNextMusic()
                            },
                            onSeek = { position ->
                                player.seekTo(position)
                                currentPosition = position
                            }
                    )
                }
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

    private fun playMusic(index: Int)
    {
        currentSong = music[index]

        player.setMediaItems(
                music.map { song -> MediaItem.fromUri(song.uri) },
                index,
            0L
        )

        player.prepare()
        player.play()

        isPlaying = true
        currentPosition = 0L
        duration = player.duration.coerceAtLeast(0L)
        progressHandler.post(progressUpdater)
    }

    private fun playPreviousMusic()
    {
        if (player.hasPreviousMediaItem())
        {
            player.seekToPreviousMediaItem()
            player.play()

            currentSong = music[player.currentMediaItemIndex]
            isPlaying = true
        }
    }

    private fun playNextMusic()
    {
        if (player.hasNextMediaItem())
        {
            player.seekToNextMediaItem()
            player.play()

            currentSong = music[player.currentMediaItemIndex]
            isPlaying = true
        }
    }

    private fun togglePlayPause()
    {
        if (player.isPlaying)
        {
            player.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
        }
        else
        {
            player.play()
            isPlaying = true
            progressHandler.post(progressUpdater)
        }
    }

    override fun onDestroy()
    {
        progressHandler.removeCallbacks(progressUpdater)
        player.release()
        super.onDestroy()
    }
}

@Composable
fun MusicList(
    music: List<Music>,
    currentSong: Music?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onSongClick: (Music) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit
)
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
        Column(
                modifier = Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.Gray)
            )
            Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = {
                        onSeek(it.toLong())
                    },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                    currentSong?.title ?: "Nothing Playing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 15.dp)
            )
            Text(
                    currentSong?.artist ?: "",
                    fontSize = 15.sp
            )
        }

        Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Button(onClick = onPreviousClick) {
                Text("Previous")
            }
            Button(onClick = onPlayPauseClick) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(onClick = onNextClick) {
                Text("Next")
            }
        }
    }
}

@Composable
fun PlayerScreen(
    currentSong: Music?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
                modifier = Modifier
                    .size(320.dp)
                    .background(Color.Gray)
        )

        Text(
                text = currentSong?.title ?: "Nothing Playing",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 25.dp)
        )

        Text(
                text = currentSong?.artist ?: "",
                fontSize = 16.sp
        )

        // Progress bar will go here

        // Playback controls will go here

        // Lyrics / Shuffle / Repeat will go here
    }
}