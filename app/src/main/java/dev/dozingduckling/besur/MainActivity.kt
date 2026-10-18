package dev.dozingduckling.besur

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.dozingduckling.besur.ui.theme.BesurTheme

class MainActivity : ComponentActivity() {
    private var music by mutableStateOf<List<Music>>(emptyList())
    private var currentSong by mutableStateOf<Music?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)
    private var showPlayer by mutableStateOf(false)
    private lateinit var musicRepository: MusicRepository
    private lateinit var player: ExoPlayer

    private val musicPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
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
                            onSongClick = { song ->
                                playMusic(music.indexOf(song))
                            },
                            onPlayPauseClick = {
                                togglePlayPause()
                            },
                            onNextClick = {
                                playNextMusic()
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

    private fun requestMusicPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusic()
        } else {
            musicPermissionLauncher.launch(permission)
        }
    }

    private fun loadMusic() {
        music = musicRepository.getMusic()
    }

    private fun playMusic(index: Int) {
        if (currentSong == music[index] && isPlaying) {
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

    private fun playPreviousMusic() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()

            currentSong = music[player.currentMediaItemIndex]
            isPlaying = true
        }
    }

    private fun playNextMusic() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()

            currentSong = music[player.currentMediaItemIndex]
            isPlaying = true
        }
    }

    private fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
        } else {
            player.play()
            isPlaying = true
            progressHandler.post(progressUpdater)
        }
    }

    override fun onDestroy() {
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
    onSongClick: (Music) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onMiniPlayerClick: () -> Unit
) {
    Box(
            modifier = Modifier.fillMaxSize()
    ) {
        SongList(
                music = music,
                onSongClick = onSongClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 10.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
        )

        MiniPlayer(
                currentSong = currentSong,
                isPlaying = isPlaying,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onOpenPlayer = {
                    if (currentSong != null) onMiniPlayerClick()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .navigationBarsPadding()
        )
    }
}

@Composable
fun SongList(
    music: List<Music>,
    onSongClick: (Music) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(15.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(music, key = { it.id }) { song ->
            Row(
                    modifier = Modifier.clickable {
                        onSongClick(song)
                    }
            ) {
                AlbumArtwork(
                        uri = song.uri,
                        albumId = song.albumId,
                        targetSizePx = 256,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                )
                Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 15.dp)
                ) {
                    Text(song.title)
                    Text(song.artist)
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    currentSong: Music?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFDDDDDD))
                .clickable { onOpenPlayer() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtwork(
                uri = currentSong?.uri,
                albumId = currentSong?.albumId,
                targetSizePx = 256,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
        )
        Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
        ) {
            Text(
                    text = currentSong?.title ?: "Playing Nothing",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    text = currentSong?.artist ?: "...",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(40.dp)
            ) {
                Icon(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f),
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
            ) {
                Icon(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Next"
                )
            }
        }
    }
}

@Composable
fun AlbumArtwork(uri: String?, albumId: Long?, targetSizePx: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val albumArt by produceState<Bitmap?>(
            initialValue = null,
            key1 = uri,
            key2 = albumId,
            key3 = targetSizePx
    ) {
        value = AlbumArtLoader.load(context, uri, albumId, targetSizePx)
    }

    albumArt?.let {
        Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = modifier
        )
    } ?: Box(modifier = modifier.background(Color.Gray))
}

@Preview(
        showBackground = true,
        showSystemUi = true,
        device = "spec:width=1080px,height=2222px,dpi=416"
)
@Composable
fun MainScreenPreview() {
    BesurTheme {
        MainScreen(
                music = listOf(
                        Music(
                                id = 1,
                                albumId = 1,
                                title = "Song Name One",
                                artist = "Artist One",
                                album = "Album One",
                                uri = "",
                                albumArtUri = null
                        ),
                        Music(
                                id = 2,
                                albumId = 2,
                                title = "Song Name Two",
                                artist = "Artist Two",
                                album = "Album Two",
                                uri = "",
                                albumArtUri = null
                        )
                ),
                currentSong = null,
                isPlaying = false,
                onSongClick = {},
                onPlayPauseClick = {},
                onNextClick = {},
                onMiniPlayerClick = {}
        )
    }
}