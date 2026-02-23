package cn.moncn.sing_box_windows.v2.domain.settings

import cn.moncn.sing_box_windows.config.AppSettings
import cn.moncn.sing_box_windows.update.UpdateState
import kotlinx.coroutines.flow.Flow

interface SettingsGateway {
    val updateStateFlow: Flow<UpdateState>

    suspend fun loadSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)

    fun checkUpdate(manual: Boolean = true)
}
