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

fun initPlayer(context: Context): Player {
    val player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(5000)
        .setSeekForwardIncrementMs(5000)
        .build()

    player.addListener(object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            display("onTransition")
            display(player.currentMediaItemIndex.toString())

            if (player.hasPreviousMediaItem()) {
                highlightCurrentlyPlaying(fileIndex(player.previousMediaItemIndex), false)
            }
            highlightCurrentlyPlaying(fileIndex(player.currentMediaItemIndex), true)
        }
    })

    player.prepare()
    return player
}

fun fileIndex(songIndex: Int): Int {
    return mainList.indexOf(mainList.filter { it.isAudio }[songIndex])
}

fun play(item: FileItem) {
    player.setMediaItems(
        mainList
            .filter { it.isAudio }
            .map { MediaItem.fromUri(Uri.fromFile(it.path)) })
    repeat(mainList.indexOf(item)) { player.seekToNextMediaItem() }
    player.play()
    playing.value = true
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