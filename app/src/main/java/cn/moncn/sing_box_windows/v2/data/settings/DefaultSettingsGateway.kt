package cn.moncn.sing_box_windows.v2.data.settings

import android.content.Context
import cn.moncn.sing_box_windows.config.AppSettings
import cn.moncn.sing_box_windows.config.ConfigRepository
import cn.moncn.sing_box_windows.config.SettingsRepository
import cn.moncn.sing_box_windows.update.GitHubRelease
import cn.moncn.sing_box_windows.update.UpdateManager
import cn.moncn.sing_box_windows.update.UpdateState
import cn.moncn.sing_box_windows.update.UpdateStore
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway
import kotlinx.coroutines.flow.Flow
import java.io.File

class DefaultSettingsGateway(
    context: Context
) : SettingsGateway {
    private val appContext = context.applicationContext
    private val updateManager by lazy { UpdateManager.getInstance(appContext) }

    override val updateStateFlow: Flow<UpdateState> = UpdateStore.stateFlow

    override suspend fun loadSettings(): AppSettings {
        return SettingsRepository.load(appContext)
    }

    override suspend fun saveSettings(settings: AppSettings) {
        SettingsRepository.save(appContext, settings)
        ConfigRepository.applySettings(appContext, settings)
    }

    override fun checkUpdate(manual: Boolean) {
        updateManager.checkUpdate(isManual = manual)
    }

    override fun downloadUpdate(release: GitHubRelease) {
        updateManager.downloadUpdate(release)
    }

    override fun installUpdate(apkFile: File): Boolean {
        return updateManager.installUpdate(apkFile)
    }

    override fun canInstallPackage(): Boolean {
        return updateManager.canInstallPackage()
    }

    override fun openInstallPermissionSettings() {
        updateManager.openInstallPermissionSettings()
    }
}
