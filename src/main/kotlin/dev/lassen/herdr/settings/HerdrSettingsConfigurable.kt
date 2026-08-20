package dev.lassen.herdr.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class HerdrSettingsConfigurable : Configurable {

    private val pathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }

    override fun getDisplayName(): String = "Herdr"

    override fun createComponent(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Path to herdr binary (leave empty to auto-detect):", pathField)
            .panel

    override fun isModified(): Boolean =
        pathField.text != HerdrSettingsState.getInstance().state.binaryPath

    override fun apply() {
        HerdrSettingsState.getInstance().state.binaryPath = pathField.text
    }

    override fun reset() {
        pathField.text = HerdrSettingsState.getInstance().state.binaryPath
    }
}
