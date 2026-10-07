package dev.dozingduckling.besur

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.dozingduckling.besur.ui.theme.BesurTheme
import androidx.core.net.toUri

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
                            onBackClick = {
                                showPlayer = false
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
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val albumArt = remember(currentSong?.albumArtUri) {
        loadAlbumArt(context, currentSong?.albumArtUri)
    }

    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
                onClick = onBackClick
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (albumArt != null)
        {
            Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
            )
        }
        else
        {
            Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .background(Color.Gray)
            )
        }
        Text(
                text = currentSong?.title ?: "Nothing Playing",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 25.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
                text = currentSong?.artist ?: "",
                fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        )
        {
            IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(100.dp)
            )
            {
                // TODO: previous icon
            }
            IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(100.dp)
            )
            {
                // TODO: play pause icon, isPlaying check
            }
            IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(100.dp)
            )
            {
                // TODO: next icon
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview()
{
    BesurTheme {
        PlayerScreen(
                currentSong = Music(
                        id = 1,
                        title = "Sample Song",
                        artist = "Sample Artist",
                        album = "Sample Album",
                        uri = "",
                        albumArtUri = null
                ),
                isPlaying = false,
                currentPosition = 4L,
                duration = 10L,
                onBackClick = {},
                onPlayPauseClick = {},
                onPreviousClick = {},
                onNextClick = {},
                onSeek = {}
        )
    }
}

fun loadAlbumArt(context: Context, uri: String?): Bitmap?
{
    if (uri == null)
        return null

    return try
    {
        context.contentResolver.openInputStream(uri.toUri())?.use {
            BitmapFactory.decodeStream(it)
        }
    }
    catch (_: Exception)
    {
        null
    }
}