package fr.tflag.droidboost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.tflag.droidboost.ui.DroidBoostRoot
import fr.tflag.droidboost.ui.theme.DroidBoostTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DroidBoostApp).container
        setContent {
            DroidBoostTheme {
                DroidBoostRoot(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Shizuku may have been started or stopped while the app was in the background.
        (application as DroidBoostApp).container.shell.refresh()
    }
}
