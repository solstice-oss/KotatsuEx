package org.koitharu.kotatsu.stats.ui.details

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.settings.compose.DropSauceTheme

@AndroidEntryPoint
class StatsDetailsActivityKEx : AppCompatActivity() {

    private val viewModel: StatsDetailsViewModelKEx by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DropSauceTheme {
                StatsDetailsScreenKEx(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
