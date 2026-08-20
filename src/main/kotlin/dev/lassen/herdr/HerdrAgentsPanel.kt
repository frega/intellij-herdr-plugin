package dev.lassen.herdr

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

private val STATUS_PRIORITY = mapOf(
    "blocked" to 0,
    "done" to 1,
    "working" to 2,
    "unknown" to 3,
    "idle" to 4,
)

private const val POLL_INTERVAL_MS = 4000
private val ATTENTION_STATUSES = setOf("blocked", "done")

/** Flat list of every herdr agent across running sessions, sorted by attention: blocked > done > working > unknown > idle. */
class HerdrAgentsPanel(
    private val project: Project,
    private val binaryPath: String,
    private val onAgentChosen: (HerdrCli.Agent) -> Unit,
) : JPanel(BorderLayout()), Disposable {

    private val model = DefaultListModel<HerdrCli.Agent>()
    private val list = JBList(model).apply {
        cellRenderer = ListCellRenderer<HerdrCli.Agent> { jList, value, _, isSelected, _ ->
            renderAgent(value, jList.background, jList.selectionBackground, isSelected)
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) selectedValue?.let(onAgentChosen)
            }
        })
    }

    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private var lastStatusByPane: Map<String, String> = emptyMap()

    init {
        add(JBScrollPane(list), BorderLayout.CENTER)
        schedulePoll()
    }

    private fun schedulePoll() {
        if (alarm.isDisposed) return
        alarm.addRequest(::poll, POLL_INTERVAL_MS)
    }

    private fun poll() {
        val agents = HerdrCli.listAllAgents(binaryPath).sortedBy { STATUS_PRIORITY[it.status] ?: 5 }
        notifyAttentionChanges(agents)
        lastStatusByPane = agents.associate { it.paneId to it.status }

        ApplicationManager.getApplication().invokeLater {
            model.clear()
            agents.forEach(model::addElement)
        }
        schedulePoll()
    }

    private fun notifyAttentionChanges(agents: List<HerdrCli.Agent>) {
        val newlyNeedingAttention = agents.filter { agent ->
            agent.status in ATTENTION_STATUSES && lastStatusByPane[agent.paneId] != agent.status
        }
        if (newlyNeedingAttention.isEmpty()) return

        ApplicationManager.getApplication().invokeLater {
            val group = NotificationGroupManager.getInstance().getNotificationGroup("Herdr")
            for (agent in newlyNeedingAttention) {
                group.createNotification("${agent.title} is now ${agent.status}", NotificationType.INFORMATION)
                    .notify(project)
            }
        }
    }

    private fun renderAgent(
        agent: HerdrCli.Agent,
        defaultBackground: java.awt.Color,
        selectionBackground: java.awt.Color,
        isSelected: Boolean,
    ): Component = SimpleColoredComponent().apply {
        append("${agent.status} ", statusAttributes(agent.status))
        append(agent.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        append("  ·  ${agent.sessionName}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        background = if (isSelected) selectionBackground else defaultBackground
    }

    private fun statusAttributes(status: String) = when (status) {
        "blocked" -> SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, java.awt.Color(0xC0392B))
        "done" -> SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, java.awt.Color(0x27AE60))
        "working" -> SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, java.awt.Color(0x2980B9))
        else -> SimpleTextAttributes.GRAYED_ATTRIBUTES
    }

    override fun dispose() {
        alarm.cancelAllRequests()
    }
}
