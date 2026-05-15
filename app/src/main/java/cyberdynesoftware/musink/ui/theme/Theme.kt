package cyberdynesoftware.musink.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

@Composable
fun MusinkTheme(
    content: @Composable () -> Unit
) {
    ThemeMMD(
        content = content
    )
}