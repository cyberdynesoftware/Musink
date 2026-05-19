package cyberdynesoftware.musink

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch
import java.io.File

data class FileItem(
    val label: String,
    val icon: ImageVector,
    val path: File,
    val isAudio: Boolean,
    var fontWeight: FontWeight?
)

fun internalStorage(label: String): FileItem {
    return FileItem(
        label,
        Icons.Default.Storage,
        Environment.getExternalStorageDirectory(),
        false,
        null
    )
}

fun removableStorage(context: Context): List<FileItem> {
    val result = mutableListOf<FileItem>()
    val storageManager = context.getSystemService(STORAGE_SERVICE) as StorageManager
    for (volume in storageManager.storageVolumes.drop(1)) {
        result.add(
            FileItem(
                volume.getDescription(context),
                Icons.Default.SdCard,
                File(Environment.getStorageDirectory().path + "/" + volume.mediaStoreVolumeName?.uppercase()),
                false,
                null
            )
        )
    }
    return result
}

fun listDirectory(file: File): List<FileItem> {
    val result = mutableListOf<FileItem>()
    file.listFiles()?.forEach { dirEntry ->
        if (!dirEntry.isHidden) {
            result.add(
                FileItem(
                    dirEntry.name,
                    iconFor(dirEntry),
                    dirEntry,
                    isAudioFile(dirEntry),
                    null
                )
            )
        }
    }
    result.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    return result
}

fun iconFor(file: File): ImageVector {
    return if (file.isDirectory) {
        Icons.Default.Folder
    } else if (isAudioFile(file)) {
        Icons.Default.AudioFile
    } else {
        Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

fun isAudioFile(file: File): Boolean {
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
    //mimeType?.let { display(it) }
    return mimeType?.startsWith("audio") == true
}

@Composable
fun CurrentDirectoryContent(modifier: Modifier) {
    val prefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    LazyColumnMMD(
        modifier = modifier,
        state = mainListState
    ) {
        itemsIndexed(currentDirectoryContentsList) { index, item ->
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (item.path.isDirectory) {
                                navTo(item.path)
                            } else if (item.isAudio) {
                                if (currentlyPlayingDirectory.value == currentDirectory.value) {
                                    play(item)
                                } else {
                                    currentlyPlayingDirectory.value = currentDirectory.value
                                    updatePlaylist(currentDirectoryContentsList)
                                    play(item)
                                }
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
                    softWrap = false,
                    fontWeight = item.fontWeight
                )
                DropdownMenuMMD(
                    expanded = showContextMenu && selectedIndex == index,
                    onDismissRequest = { showContextMenu = false },
                ) {
                    DropdownMenuItemMMD(
                        text = { TextMMD(stringResource(R.string.set_home)) },
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
            if (index < currentDirectoryContentsList.size - 1) {
                DashedDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun HomeButton(modifier: Modifier, snackbarHostState: SnackbarHostStateMMD) {
    val scope = rememberCoroutineScope()
    val prefs = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val homeNotSet = stringResource(R.string.home_not_set)

    IconButton(
        onClick = {
            val home = prefs.getString("home", null)
            if (home == null) {
                scope.launch { snackbarHostState.showSnackbar(homeNotSet) }
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