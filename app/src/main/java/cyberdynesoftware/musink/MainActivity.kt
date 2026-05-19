package cyberdynesoftware.musink

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarDefaultsMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import cyberdynesoftware.musink.ui.theme.MusinkTheme
import java.io.File

var player: Player? = null

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkStoragePermission()
        navTo(startingDirectory())
        initBluetoothProfileProxy(baseContext)
        requestBluetoothPermission()
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


    fun requestBluetoothPermission() {
        if (!checkBluetoothPermission(baseContext)) {
            requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 17)
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

    override fun onStart() {
        super.onStart()
        val sessionToken =
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            { player = controllerFuture.get() },
            MoreExecutors.directExecutor(),
        )
    }
}

fun display(msg: String) {
    Log.d("--- MusinK ---", msg)
}

val currentDirectory = mutableStateOf(File("/"))
val currentlyPlayingDirectory = mutableStateOf(File("/"))
val playing = mutableStateOf(false)
val currentDirectoryContentsList = mutableStateListOf<FileItem>()

fun navTo(path: File) {
    currentDirectory.value = path
    currentDirectoryContentsList.clear()
    currentDirectoryContentsList.addAll(listDirectory(path))
    scrollToItem(0)
}

val mainListState = LazyListState()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val snackbarHostState = remember { SnackbarHostStateMMD() }

    BackHandler(true) {
        currentDirectory.value.parentFile?.let { parent ->
            if (parent.canRead()) {
                navTo(parent)
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
            CurrentDirectoryContent(modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .weight(1f)
                .imePadding())
            HorizontalDividerMMD(color = TopAppBarDefaultsMMD.dividerColor, thickness = 1.dp)
            PlayerControlButtonRow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(snackbarHostState: SnackbarHostStateMMD, modifier: Modifier = Modifier) {
    TopAppBarMMD(
        title = { TextMMD(stringResource(R.string.app_name)) },
        navigationIcon = { HeadphoneButton(modifier) },
        actions = {
            HomeButton(modifier, snackbarHostState)
            OptionsMenu()
        }
    )
}


@Composable
fun OptionsMenu() {
    var expanded by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }

        DropdownMenuMMD(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-8).dp, y = 0.dp)
        ) {
            StorageDropDownMenuItem(internalStorage(stringResource(R.string.internal_storage))) { expanded = false }
            removableStorage(LocalContext.current).forEach {
                StorageDropDownMenuItem(it) { expanded = false }
            }
            DashedDivider()
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(R.string.shuffle)) },
                onClick = { expanded = false },
                leadingIcon = {
                    Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle))
                },
                trailingIcon = {
                    SwitchMMD(
                        shuffleEnabled,
                        onCheckedChange = {
                            shuffleEnabled = it
                            player?.shuffleModeEnabled = it
                        }
                    )
                }
            )
        }
    }
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