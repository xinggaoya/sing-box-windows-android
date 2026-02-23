package cn.moncn.sing_box_windows.v2.domain.settings

import cn.moncn.sing_box_windows.config.AppSettings
import cn.moncn.sing_box_windows.update.GitHubRelease
import cn.moncn.sing_box_windows.update.UpdateState
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SettingsGateway {
    val updateStateFlow: Flow<UpdateState>

    suspend fun loadSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)

    fun checkUpdate(manual: Boolean = true)
    fun downloadUpdate(release: GitHubRelease)
    fun installUpdate(apkFile: File): Boolean
    fun canInstallPackage(): Boolean
    fun openInstallPermissionSettings()
}
