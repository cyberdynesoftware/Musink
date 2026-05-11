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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

var previousMediaItemIndex = -1

fun initPlayer(context: Context): Player {
    val player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(5000)
        .setSeekForwardIncrementMs(5000)
        .build()

    player.addListener(object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (currentSongPath.value == currentPath.value) {
                if (previousMediaItemIndex >= 0) {
                    highlightCurrentlyPlaying(fileIndex(previousMediaItemIndex), false)
                }
                previousMediaItemIndex = player.currentMediaItemIndex

                fileIndex(player.currentMediaItemIndex).let {
                    highlightCurrentlyPlaying(it, true)
                    if (lastVisibleItemIndex(listState.firstVisibleItemIndex) < it ||
                        it < listState.firstVisibleItemIndex) {
                        CoroutineScope(Dispatchers.Main).launch {
                            listState.scrollToItem(it)
                        }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playing.value = isPlaying
        }
    })

    player.prepare()
    return player
}

fun lastVisibleItemIndex(firstVisibleItemIndex: Int): Int {
    val numberOfVisibleItems = 8 // on my Kompakt
    return firstVisibleItemIndex + numberOfVisibleItems - 1
}

fun fileIndex(songIndex: Int): Int {
    return mainList.indexOf(mainList.filter { it.isAudio }[songIndex])
}

fun play(item: FileItem) {
    previousMediaItemIndex = -1
    player.setMediaItems(
        mainList
            .filter { it.isAudio }
            .map { MediaItem.fromUri(Uri.fromFile(it.path)) })
    repeat(mainList.indexOf(item)) { player.seekToNextMediaItem() }
    player.play()
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