package dev.lassen.herdr.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/** Remembers which herdr session this project's tool window last attached to. */
@Service(Service.Level.PROJECT)
@State(name = "HerdrSessionSettings", storages = [Storage("herdr-session.xml")])
class HerdrSessionState : PersistentStateComponent<HerdrSessionState.State> {

    class State {
        var sessionName: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object {
        fun getInstance(project: Project): HerdrSessionState =
            project.getService(HerdrSessionState::class.java)
    }
}
