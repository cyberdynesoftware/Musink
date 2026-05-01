package cyberdynesoftware.musink

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.core.net.toUri
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarDefaultsMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import cyberdynesoftware.musink.ui.theme.MusinkTheme
import java.io.File

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkStoragePermission()
        navTo(Environment.getExternalStorageDirectory())
        enableEdgeToEdge()
        setContent {
            MusinkTheme {
                MainContent()
                //ListsScreen()
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
    BackHandler(true) {
        currentPath.value.parentFile?.let {
            navTo(it)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar() }
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
                items(mainList) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                if (it.path.isDirectory) {
                                    navTo(it.path)
                                }
                            }
                    ) {
                        Icon(it.icon, contentDescription = "icon", Modifier.padding(end = 8.dp))
                        Text(it.label, textAlign = TextAlign.Center, maxLines = 1)
                    }
                    DashedDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            HorizontalDividerMMD(color = TopAppBarDefaultsMMD.dividerColor, thickness = 1.dp)
            ButtonRow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    TopAppBarMMD(
        title = { Text(stringResource(R.string.app_name)) },
        navigationIcon = {
            Box(modifier = modifier.padding(4.dp)) {
                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "back",
                        modifier = modifier.size(24.dp)
                    )
                }
            }
        },
        actions = {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "home",
                modifier = modifier
                    .padding(8.dp)
                    .size(24.dp)
            )
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
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "previous",
            modifier = Modifier.size(32.dp)
        )
        Icon(
            imageVector = Icons.Default.Replay5,
            contentDescription = "replay",
            modifier = Modifier.size(32.dp)
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "play",
            modifier = Modifier.size(32.dp)
        )
        Icon(
            imageVector = Icons.Default.Forward5,
            contentDescription = "forward",
            modifier = Modifier.size(32.dp)
        )
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "next",
            modifier = Modifier.size(32.dp)
        )
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
            DropdownMenuItemMMD(
                text = { Text("Favorites") },
                onClick = { expanded = false },
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = "favorites")
                },
                trailingIcon = {
                    SwitchMMD(
                        false,
                        onCheckedChange = {

                        }
                    )
                }
            )
            DropdownMenuItemMMD(
                text = { Text("Shuffle") },
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
        text = { Text(it.label) },
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