package cyberdynesoftware.musink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarDefaultsMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import cyberdynesoftware.musink.ui.theme.MusinkTheme
import kotlinx.coroutines.launch
import java.io.File

lateinit var player: ExoPlayer
val playing = mutableStateOf(false)

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(baseContext).build()
        player.prepare()
        checkStoragePermission()
        navTo(startingDirectory())
        enableEdgeToEdge()
        setContent {
            MusinkTheme {
                MainContent()
            }
        }
    }

    fun checkStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.let {
                startActivity(Intent(it).apply { data = "package:$packageName".toUri() })
            }
        }
    }

    fun startingDirectory(): File {
        val home = getSharedPreferences("prefs", MODE_PRIVATE).getString("home", null)
        return if (home == null) {
            Environment.getExternalStorageDirectory()
        } else {
            File(home)
        }
    }
}

val currentPath = mutableStateOf(File("/"))
val mainList = mutableStateListOf<FileItem>()

fun navTo(path: File) {
    currentPath.value = path
    mainList.clear()
    mainList.addAll(listDirectory(path))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val prefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostStateMMD() }

    BackHandler(true) {
        currentPath.value.parentFile?.let {
            if (it.canRead()) {
                navTo(it)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(snackbarHostState) },
        snackbarHost = { SnackbarHostMMD(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
//            TextMMD(
//                modifier = Modifier
//                    .consumeWindowInsets(innerPadding)
//                    .imePadding(),
//                text = currentPath.value.path,
//                maxLines = 1
//            )
            LazyColumnMMD(
                modifier = Modifier
                    .consumeWindowInsets(innerPadding)
                    .weight(1f)
                    .imePadding()
            ) {
                itemsIndexed(mainList) { index, item ->
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (item.path.isDirectory) {
                                        navTo(item.path)
                                    } else if (item.isAudio) {
                                        play(item)
                                    }
                                },
                                onLongClick = {
                                    if (item.path.isDirectory) {
                                        selectedIndex = index
                                        showContextMenu = true
                                    }
                                }
                            )
                    ) {
                        Icon(item.icon, contentDescription = "icon", Modifier.padding(end = 8.dp))
                        TextMMD(
                            item.label,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                        DropdownMenuMMD(
                            expanded = showContextMenu && selectedIndex == index,
                            onDismissRequest = { showContextMenu = false },
                        ) {
                            DropdownMenuItemMMD(
                                text = { TextMMD("Set as home") },
                                onClick = {
                                    showContextMenu = false
                                    prefs.edit {
                                        putString("home", item.path.path)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = "home")
                                }
                            )
                        }
                    }
                    if (index < mainList.size - 1) {
                        DashedDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            HorizontalDividerMMD(color = TopAppBarDefaultsMMD.dividerColor, thickness = 1.dp)
            ButtonRow()
        }
    }
}

fun play(item: FileItem) {
    player.setMediaItems(mainList.map { MediaItem.fromUri(Uri.fromFile(it.path)) })
    repeat(mainList.indexOf(item)) {
        player.seekToNextMediaItem()
    }
    player.play()
    playing.value = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(snackbarHostState: SnackbarHostStateMMD, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val prefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    TopAppBarMMD(
        title = { TextMMD(stringResource(R.string.app_name)) },
        navigationIcon = {
            Box(modifier = modifier.padding(4.dp)) {
                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "back",
                        modifier = modifier.size(32.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    val home = prefs.getString("home", null)
                    if (home == null) {
                        scope.launch { snackbarHostState.showSnackbar("Home not set. Set with a long click.") }
                    } else {
                        navTo(File(home))
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "home",
                    modifier = modifier.size(32.dp)
                )
            }
            OptionsMenu()
        }
    )
}

@Composable
fun ButtonRow() {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = {
                player.seekToPreviousMediaItem()
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
                if (player.isPlaying) {
                    player.pause()
                    playing.value = false
                } else {
                    player.play()
                    playing.value = true
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
                player.seekToNextMediaItem()
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

@Composable
fun OptionsMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }

        DropdownMenuMMD(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-8).dp, y = 0.dp)
        ) {
            StorageDropDownMenuItem(internalStorage()) { expanded = false }
            removableStorage(LocalContext.current).forEach {
                StorageDropDownMenuItem(it) { expanded = false }
            }
            DashedDivider()
//            DropdownMenuItemMMD(
//                text = { Text("Favorites") },
//                onClick = { expanded = false },
//                leadingIcon = {
//                    Icon(Icons.Default.Star, contentDescription = "favorites")
//                },
//                trailingIcon = {
//                    SwitchMMD(
//                        false,
//                        onCheckedChange = {
//
//                        }
//                    )
//                }
//            )
            DropdownMenuItemMMD(
                text = { TextMMD("Shuffle") },
                onClick = { expanded = false },
                leadingIcon = {
                    Icon(Icons.Default.Shuffle, contentDescription = "shuffle")
                },
                trailingIcon = {
                    SwitchMMD(
                        false,
                        onCheckedChange = {

                        }
                    )
                }
            )
        }
    }
}

@Composable
fun StorageDropDownMenuItem(it: FileItem, callback: () -> Unit) {
    DropdownMenuItemMMD(
        text = { TextMMD(it.label) },
        onClick = {
            callback()
            navTo(it.path)
        },
        leadingIcon = {
            Icon(
                it.icon,
                it.label
            )
        }
    )
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
    thickness: Dp = 2.dp,
    dashWidth: Dp = 2.dp,
    dashGap: Dp = 2.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashWidth.toPx(), dashGap.toPx()), 0f
            )
        )
    }
}