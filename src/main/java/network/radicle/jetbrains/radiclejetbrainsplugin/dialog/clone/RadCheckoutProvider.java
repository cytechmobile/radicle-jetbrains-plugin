package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.google.common.base.Strings;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RadCheckoutProvider implements CheckoutProvider {

    @Override
    public @NotNull VcsCloneComponent buildVcsCloneComponent(@NotNull Project project, @NotNull ModalityState modalityState,
                                                             @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
        return new RadUrl(project);
    }

    @Override
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {

    }

    @Override
    public @NonNls String getVcsName() {
        return RadicleBundle.message("radicle");
    }

    public class RadUrl implements VcsCloneComponent {

        protected JPanel mainPanel;
        protected TextFieldWithBrowseButton directoryField;
        protected JBTextField urlField;
        private VcsCloneDialogComponentStateListener dialogListener;
        private Project project;

        public RadUrl(Project project) {
            this.project = project;
            urlField = new JBTextField();
            directoryField = new TextFieldWithBrowseButton();
            directoryField.addBrowseFolderListener(RadicleBundle.message("selectDirectory"), "", null,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor());
            directoryField.getTextField().getDocument().addDocumentListener(new TextFieldListener());
        }

        @Override
        public void doClone(@NotNull CheckoutProvider.Listener listener) {
            if (!BasicAction.isCliPathConfigured(project)) {
                return ;
            }
            CloneUtil.doClone(listener,project, urlField.getText(),null,directoryField.getText());
        }

        @NotNull
        @Override
        public List<ValidationInfo> doValidateAll() {
            var list = new ArrayList<ValidationInfo>();
            ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.getText(),
                    directoryField.getTextField()));
            return list;
        }

        @NotNull
        @Override
        public JComponent getView() {
            var panel = new JPanel();
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            panel.setLayout(gridbag);
            c.fill = GridBagConstraints.HORIZONTAL;
            var urlLabel = new JBLabel(RadicleBundle.message("urn"));
            c.gridx = 0;
            c.gridy = 0;
            gridbag.setConstraints(urlLabel, c);
            panel.add(urlLabel);
            urlField.getDocument().addDocumentListener(new TextFieldListener());
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            gridbag.setConstraints(urlField, c);
            panel.add(urlField);
            var dirLabel = new JBLabel(RadicleBundle.message("directory"));
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 0;
            gridbag.setConstraints(dirLabel, c);
            panel.add(dirLabel);
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            gridbag.setConstraints(directoryField, c);
            panel.add(directoryField);
            panel.setBorder(JBUI.Borders.empty(0, 14));
            mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(panel,BorderLayout.NORTH);

            return mainPanel;
        }

        @Override
        public boolean isOkEnabled() {
            return false;
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return null;
        }

        @Override
        public void onComponentSelected(@NotNull VcsCloneDialogComponentStateListener vcsCloneDialogComponentStateListener) {
           dialogListener = vcsCloneDialogComponentStateListener;
           vcsCloneDialogComponentStateListener.onOkActionEnabled(false);
        }

        @Nls
        @NotNull
        @Override
        public String getOkButtonText() {
            return RadicleBundle.message("clone");
        }

        @Override
        public void dispose() {
        }

        private class TextFieldListener extends DocumentAdapter {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (dialogListener == null) {
                    return ;
                }

               if (!Strings.isNullOrEmpty(directoryField.getText()) && !Strings.isNullOrEmpty(urlField.getText())) {
                   dialogListener.onOkActionEnabled(true);
               } else {
                   dialogListener.onOkActionEnabled(false);
               }
            }
        }
    }

}
