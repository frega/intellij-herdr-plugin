package dev.lassen.herdr.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import dev.lassen.herdr.HerdrBinaryLocator
import dev.lassen.herdr.HerdrToolWindowFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class HerdrSettingsConfigurable : Configurable {

    private val statusLabel = JBLabel()
    private val pathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val showAgentsListCheckBox = JBCheckBox("Show Agents list tab")

    override fun getDisplayName(): String = "Herdr"

    override fun createComponent(): JComponent {
        val form = FormBuilder.createFormBuilder()
            .addComponent(statusLabel)
            .addLabeledComponent("Path to herdr binary (leave empty to auto-detect):", pathField)
            .addComponent(showAgentsListCheckBox)
            .panel

        // Pin content to the top instead of letting it be vertically centered in the settings page.
        return JPanel(BorderLayout()).apply {
            add(form, BorderLayout.NORTH)
        }
    }

    override fun isModified(): Boolean =
        pathField.text != HerdrSettingsState.getInstance().state.binaryPath ||
            showAgentsListCheckBox.isSelected != HerdrSettingsState.getInstance().state.showAgentsList

    override fun apply() {
        HerdrSettingsState.getInstance().state.binaryPath = pathField.text
        HerdrSettingsState.getInstance().state.showAgentsList = showAgentsListCheckBox.isSelected
        updateStatusLabel()
        HerdrToolWindowFactory.syncAgentsTabForAllProjects()
    }

    override fun reset() {
        pathField.text = HerdrSettingsState.getInstance().state.binaryPath
        showAgentsListCheckBox.isSelected = HerdrSettingsState.getInstance().state.showAgentsList
        updateStatusLabel()
    }

    private fun updateStatusLabel() {
        val resolved = HerdrBinaryLocator.resolve(pathField.text)
        statusLabel.text = if (resolved != null) {
            "We were able to determine \"$resolved\" as the path to your herdr binary."
        } else {
            "We were not able to determine the path to your herdr binary. Set it below."
        }
    }
}
