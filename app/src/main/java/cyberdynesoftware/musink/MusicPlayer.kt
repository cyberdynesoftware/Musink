package cyberdynesoftware.musink

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

var previousMediaItemIndex = -1

class PlaybackService : MediaSessionService() {
    var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (currentlyPlayingDirectory.value == currentDirectory.value) {
                    if (previousMediaItemIndex >= 0) {
                        highlightCurrentlyPlaying(fileItemIndex(previousMediaItemIndex), false)
                    }
                    previousMediaItemIndex = player.currentMediaItemIndex

                    fileItemIndex(player.currentMediaItemIndex).let {
                        highlightCurrentlyPlaying(it, true)
                        if (lastVisibleItemIndex(mainListState.firstVisibleItemIndex) < it ||
                            it < mainListState.firstVisibleItemIndex
                        ) {
                            scrollToItem(it)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing.value = isPlaying
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}

fun scrollToItem(index: Int) {
    CoroutineScope(Dispatchers.Main).launch {
        mainListState.scrollToItem(index)
    }
}

fun lastVisibleItemIndex(firstVisibleItemIndex: Int): Int {
    val numberOfVisibleItems = 8 // on my Kompakt
    return firstVisibleItemIndex + numberOfVisibleItems - 1
}

fun fileItemIndex(mediaItemIndex: Int): Int {
    return currentDirectoryContentsList.indexOf(currentDirectoryContentsList.filter { it.isAudio }[mediaItemIndex])
}

fun updatePlaylist(items: List<FileItem>) {
    previousMediaItemIndex = -1
    player?.setMediaItems(
        items
            .filter { it.isAudio }
            .map { MediaItem.fromUri(Uri.fromFile(it.path)) }
    )
}

fun play(item: FileItem) {
    player?.seekToDefaultPosition(currentDirectoryContentsList.filter { it.isAudio }.indexOf(item))
    player?.play()
}

fun highlightCurrentlyPlaying(index: Int, isPlaying: Boolean) {
    if (0 <= index && index < currentDirectoryContentsList.size) {
        val item = currentDirectoryContentsList[index]
        currentDirectoryContentsList[index] = FileItem(
            item.label,
            if (isPlaying) Icons.Default.Audiotrack else Icons.Default.AudioFile,
            item.path,
            true,
            if (isPlaying) FontWeight.Bold else null
        )
    }
}

@Composable
fun PlayerControlButtonRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = {
                player?.seekToPreviousMediaItem()
            }
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "previous",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(
            onClick = {
                player?.seekBack()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Replay5,
                contentDescription = "replay",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(
            onClick = {
                if (player?.isPlaying == true) {
                    player?.pause()
                } else {
                    player?.play()
                }
            }
        ) {
            if (playing.value) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "play",
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "play",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        IconButton(
            onClick = {
                player?.seekForward()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Forward5,
                contentDescription = "forward",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(
            onClick = {
                player?.seekToNextMediaItem()
            }
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "next",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}