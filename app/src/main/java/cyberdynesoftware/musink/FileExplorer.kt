package cyberdynesoftware.musink

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

data class FileItem(
    val label: String,
    val icon: ImageVector,
    val path: File,
    val audio: Boolean
)

fun internalStorage(): FileItem {
    return FileItem(
        "Internal Storage",
        Icons.Default.Storage,
        Environment.getExternalStorageDirectory(),
        false
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
                false
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
                    isAudioFile(dirEntry)
                )
            )
        }
    }
    result.sortBy { it.label }
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

fun display(msg: String) {
    Log.d("--- MusinK ---", msg)
}