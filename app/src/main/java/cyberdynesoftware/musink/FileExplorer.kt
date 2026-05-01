package cyberdynesoftware.musink

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

data class FileItem(
    val label: String,
    val icon: ImageVector,
    val path: File
)

fun internalStorage(): FileItem {
    return FileItem(
        "Internal Storage",
        Icons.Default.Storage,
        Environment.getExternalStorageDirectory()
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
                File(Environment.getStorageDirectory().path + "/" + volume.mediaStoreVolumeName?.uppercase())
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
                    if (dirEntry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                    dirEntry
                )
            )
        }
    }
    result.sortBy { it.label }
    return result
}