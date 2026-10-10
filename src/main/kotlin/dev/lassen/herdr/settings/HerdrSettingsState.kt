package dev.lassen.herdr.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "HerdrSettings", storages = [Storage("herdr.xml")])
class HerdrSettingsState : PersistentStateComponent<HerdrSettingsState.State> {

    class State {
        var binaryPath: String = ""
        var showAgentsList: Boolean = false
        var askToSuspendOnClose: Boolean = true
        var shortcutNoticeShown: Boolean = false
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object {
        fun getInstance(): HerdrSettingsState =
            ApplicationManager.getApplication().getService(HerdrSettingsState::class.java)
    }
}
