package com.aela.mainboardoverride.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R

@Composable
internal fun SettingsScreen(state: GameUiState, actions: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            val packageManager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull()
    }
    CircuitBackground {
        AppWindow(header = { ArtHeader(stringResource(R.string.settings), onBack) }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // --- Sección 1: Ajustes de juego ---
                SettingsPanel(
                    stringResource(R.string.settings_game),
                    Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.language), color = Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TerminalButton(
                            stringResource(R.string.spanish), { actions.setLanguage("es") },
                            Modifier.weight(1f), primary = state.preferences.language == "es",
                        )
                        TerminalButton(
                            stringResource(R.string.english), { actions.setLanguage("en") },
                            Modifier.weight(1f), primary = state.preferences.language == "en",
                        )
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactSwitch(stringResource(R.string.audio), state.preferences.audioEnabled, actions::setAudio, Modifier.fillMaxWidth())
                        CompactSwitch(stringResource(R.string.vibration), state.preferences.vibrationEnabled, actions::setVibration, Modifier.fillMaxWidth())
                        CompactSwitch(stringResource(R.string.reduced_motion), state.preferences.reducedMotion, actions::setReducedMotion, Modifier.fillMaxWidth())
                        CompactSwitch(stringResource(R.string.context_help), state.preferences.contextHelpEnabled, actions::setContextHelpEnabled, Modifier.fillMaxWidth())
                    }
                }

                // --- Sección 2: Volumen y Sistema ---
                SettingsPanel(
                    stringResource(R.string.settings_volume),
                    Modifier.fillMaxWidth()
                ) {
                    VolumeSlider(stringResource(R.string.music_volume), state.preferences.musicVolume, actions::setMusicVolume)
                    VolumeSlider(stringResource(R.string.sfx_volume), state.preferences.sfxVolume, actions::setSfxVolume)
                }

                // --- Sección 3: Créditos y Versión (se queda donde está) ---
                AboutStrip(versionName, state)
            }
        }
    }
}

/** Terminal-styled panel with a cyan border, used to split settings content into bounded panes. */
@Composable
private fun SettingsPanel(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Panel,
        border = BorderStroke(1.dp, Cyan.copy(alpha = .35f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            HorizontalDivider(color = Muted.copy(alpha = .3f))
            content()
        }
    }
}

/** Half-width switch row so the four toggles fit a two-column grid without overflow. */
@Composable
private fun CompactSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White, fontSize = 11.sp)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = Color.White)
            Text(
                "${(value * 100).toInt()}%",
                color = Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}

/** Full-width bottom strip with version, credit balance and the decorative system line. */
@Composable
private fun AboutStrip(versionName: String?, state: GameUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Panel,
        border = BorderStroke(1.dp, Cyan.copy(alpha = .25f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            versionName?.let {
                Text(
                    stringResource(R.string.app_version, it),
                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                )
            }
            Text(
                stringResource(R.string.credit_balance, state.preferences.credits),
                color = Warning, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            )
            Text(
                stringResource(R.string.about_decor),
                color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            )
        }
    }
}