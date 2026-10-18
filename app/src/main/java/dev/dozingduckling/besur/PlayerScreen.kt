
package dev.dozingduckling.besur

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
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
import dev.dozingduckling.besur.ui.theme.BesurTheme

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
    val albumArt = produceState<Bitmap?>(
            initialValue = null,
            currentSong?.uri,
            currentSong?.albumId
    ) {
        value = AlbumArtLoader.load(
                context = context,
                audioUri = currentSong?.uri,
                albumId = currentSong?.albumId,
                targetSize = 512
        )
    }.value

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
                    contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (albumArt != null) {
            Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
            Text(formatTime(currentPosition), fontSize = 15.sp)
            Text(formatTime(duration), fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(100.dp)
            ) {
                Icon(
                        painter = painterResource(R.drawable.ic_previous),
                        contentDescription = "Previous",
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .aspectRatio(1f)
                )
            }

            IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(100.dp)
            ) {
                Icon(
                        painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause
                                else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                )
            }

            IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(100.dp)
            ) {
                Icon(
                        painter = painterResource(R.drawable.ic_next),
                        contentDescription = "Next",
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .aspectRatio(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

@Preview(
        showBackground = true,
        showSystemUi = true,
        device = "spec:width=1080px,height=2222px,dpi=416"
)
@Composable
fun PlayerScreenPreview() {
    BesurTheme {
        PlayerScreen(
                currentSong = Music(
                        id = 1,
                        albumId = 1,
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