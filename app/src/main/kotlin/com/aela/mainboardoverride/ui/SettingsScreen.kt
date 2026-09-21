package com.aela.mainboardoverride.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.GameUiState
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
        Column(Modifier.fillMaxSize().padding(36.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.settings), color = Terminal, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.language), color = Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalButton(stringResource(R.string.spanish), { actions.setLanguage("es") }, Modifier.weight(1f), state.preferences.language == "es")
                TerminalButton(stringResource(R.string.english), { actions.setLanguage("en") }, Modifier.weight(1f), state.preferences.language == "en")
            }
            SettingSwitch(stringResource(R.string.audio), state.preferences.audioEnabled, actions::setAudio)
            VolumeSlider(stringResource(R.string.music_volume), state.preferences.musicVolume, actions::setMusicVolume)
            VolumeSlider(stringResource(R.string.sfx_volume), state.preferences.sfxVolume, actions::setSfxVolume)
            SettingSwitch(stringResource(R.string.vibration), state.preferences.vibrationEnabled, actions::setVibration)
            SettingSwitch(stringResource(R.string.reduced_motion), state.preferences.reducedMotion, actions::setReducedMotion)
            SettingSwitch(stringResource(R.string.context_help), state.preferences.contextHelpEnabled, actions::setContextHelpEnabled)
            Spacer(Modifier.height(20.dp))
            SmallButton(stringResource(R.string.back), onBack)
            versionName?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_version, it),
                    color = Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White)
        Switch(checked = value, onCheckedChange = onChange)
    }
    HorizontalDivider(color = Muted.copy(alpha = .3f))
}

@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
    HorizontalDivider(color = Muted.copy(alpha = .3f))
}
