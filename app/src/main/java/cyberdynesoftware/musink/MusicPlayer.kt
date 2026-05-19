package cyberdynesoftware.musink

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.ui.text.font.FontWeight
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
                if (currentSongPath.value == currentPath.value) {
                    display("previous: $previousMediaItemIndex")
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

//class PlayerListener: Player.Listener {
//    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//        if (currentSongPath.value == currentPath.value) {
//            display("previous: $previousMediaItemIndex")
//            if (previousMediaItemIndex >= 0) {
//                highlightCurrentlyPlaying(fileItemIndex(previousMediaItemIndex), false)
//            }
//            previousMediaItemIndex = player.currentMediaItemIndex
//
//            fileItemIndex(player.currentMediaItemIndex).let {
//                highlightCurrentlyPlaying(it, true)
//                if (lastVisibleItemIndex(mainListState.firstVisibleItemIndex) < it ||
//                    it < mainListState.firstVisibleItemIndex
//                ) {
//                    scrollToItem(it)
//                }
//            }
//        }
//    }
//
//    override fun onIsPlayingChanged(isPlaying: Boolean) {
//        playing.value = isPlaying
//    }
//}
//
//fun initPlayer(context: Context): Player {
//    val player = ExoPlayer.Builder(context)
//        .setSeekBackIncrementMs(5000)
//        .setSeekForwardIncrementMs(5000)
//        .build()
//
//    player.addListener(PlayerListener())
//
//    player.prepare()
//    return player
//}

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
    return mainList.indexOf(mainList.filter { it.isAudio }[mediaItemIndex])
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
    player?.seekToDefaultPosition(mainList.filter { it.isAudio }.indexOf(item))
    player?.play()
}

fun highlightCurrentlyPlaying(index: Int, isPlaying: Boolean) {
    if (0 <= index && index < mainList.size) {
        val item = mainList[index]
        mainList[index] = FileItem(
            item.label,
            if (isPlaying) Icons.Default.Audiotrack else Icons.Default.AudioFile,
            item.path,
            true,
            if (isPlaying) FontWeight.Bold else null
        )
    }
}