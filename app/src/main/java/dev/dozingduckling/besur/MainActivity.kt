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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
                    MainScreen(
                            music = music,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onSongClick = { song ->
                                playMusic(music.indexOf(song))
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
                            },
                            onMiniPlayerClick = {
                                showPlayer = true
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
        if (currentSong == music[index] && isPlaying)
        {
            return
        }

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
fun MainScreen(
    music: List<Music>,
    currentSong: Music?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onSongClick: (Music) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onMiniPlayerClick: () -> Unit
)
{
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp)
    )
    {
        SongList(
                music = music,
                onSongClick = onSongClick,
                modifier = Modifier.weight(1f)
        )

        MiniPlayer(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeek = onSeek,
                onOpenPlayer = {
                    if (currentSong != null)
                        onMiniPlayerClick()
                }
        )
    }
}


@Composable
fun SongList(
    music: List<Music>,
    onSongClick: (Music) -> Unit,
    modifier: Modifier = Modifier
)
{
    LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(15.dp)
    )
    {
        items(music) { song ->
            Column(
                    modifier = Modifier.clickable {
                        onSongClick(song)
                    }
            )
            {
                Text(song.title)
                Text(song.artist)
            }
        }
    }
}


@Composable
fun MiniPlayer(
    currentSong: Music?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenPlayer: () -> Unit
)
{
    val context = LocalContext.current
    val albumArt = remember(currentSong?.albumArtUri) {
        loadAlbumArt(context, currentSong?.albumArtUri)
    }

    Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFCCCCCC))
                .clickable { onOpenPlayer() }
                .padding(12.dp)
    )
    {
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        )
        {
            if (albumArt != null)
            {
                Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                )
            }
            else
            {
                Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Gray)
                )
            }

            Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
            )
            {
                Text(
                        text = currentSong?.title ?: "Playing Nothing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                Text(
                        text = currentSong?.artist ?: "...",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(40.dp)
            )
            {
                Icon(
                        painter = painterResource(R.drawable.ic_previous),
                        contentDescription = "Previous"
                )
            }
            IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(44.dp)
            )
            {
                Icon(
                        painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause
                                else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(40.dp)
            )
            {
                Icon(
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Next"
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Slider(
                value = currentPosition.toFloat().coerceIn(
                        0f,
                        duration.coerceAtLeast(1L).toFloat()
                ),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.height(25.dp)
        )
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
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (albumArt != null)
        {
            Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
            )
        }
        else
        {
            Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Gray)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
                text = currentSong?.title ?: "Nothing Playing",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 25.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
                text = currentSong?.artist ?: "",
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
        )
        Spacer(modifier = Modifier.weight(1f))
        Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth()
        )
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                    formatTime(currentPosition),
                    fontSize = 15.sp
            )
            Text(
                    formatTime(duration),
                    fontSize = 15.sp
            )
        }

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
                Icon(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.ic_previous),
                        contentDescription = "Previous"
                )
            }
            IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(100.dp)
            )
            {
                Icon(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f),
                        painter = painterResource(
                                if (isPlaying)
                                    R.drawable.ic_pause
                                else
                                    R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(100.dp)
            )
            {
                Icon(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Next"
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

fun formatTime(milliseconds: Long): String
{
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
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

@Preview(
        showBackground = true,
        showSystemUi = true,
        device = "spec:width=1080px,height=2222px,dpi=416"
)
@Composable
fun MainScreenPreview()
{
    BesurTheme {
        MainScreen(
                music = listOf(
                        Music(
                                id = 1,
                                title = "Song Name One",
                                artist = "Artist One",
                                album = "Album One",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 2,
                                title = "Song Name Two",
                                artist = "Artist Two",
                                album = "Album Two",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 3,
                                title = "Song Name Three",
                                artist = "Artist Three",
                                album = "Album Three",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 4,
                                title = "Song Name Four",
                                artist = "Artist Four",
                                album = "Album Four",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 5,
                                title = "Song Name Four",
                                artist = "Artist Four",
                                album = "Album Four",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 6,
                                title = "Song Name Four",
                                artist = "Artist Four",
                                album = "Album Four",
                                uri = "",
                                albumArtUri = null
                        )
                ),
                currentSong = null,
                isPlaying = false,
                currentPosition = 60000L,
                duration = 240000L,
                onSongClick = {},
                onPlayPauseClick = {},
                onPreviousClick = {},
                onNextClick = {},
                onSeek = {},
                onMiniPlayerClick = {}
        )
    }
}

@Preview(
        showBackground = true,
        showSystemUi = true,
        device = "spec:width=1080px,height=2222px,dpi=416"
)
@Composable
fun PlayerScreenPreview()
{
    BesurTheme {
        PlayerScreen(
                currentSong = Music(
                        id = 1,
                        title = "Song Name",
                        artist = "Artist Name",
                        album = "Album Name",
                        uri = "",
                        albumArtUri = null
                ),
                isPlaying = false,
                currentPosition = 60000L,
                duration = 240000L,
                onBackClick = {},
                onPlayPauseClick = {},
                onPreviousClick = {},
                onNextClick = {},
                onSeek = {}
        )
    }
}