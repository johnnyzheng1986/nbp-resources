package org.netbeans.eclipse.formatter.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

public class EclipseFormatterPanel extends javax.swing.JPanel {

    private String absolutePath;
    private EclipseFormatterOptionsPanelController controller;
    private final Project project;

    public EclipseFormatterPanel(final EclipseFormatterOptionsPanelController controller, Project p) {
        this.controller = controller;
        this.project = p;
        initComponents();
        enableUI();
        formatterLocField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                controller.changed();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                controller.changed();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                controller.changed();
            }
        });
        enablementCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (enablementCheckbox.isSelected()&&enablementCheckbox.getText().equals("Override global Eclipse formatter")){
                    netbeansCheckbox.setSelected(false);
                    enableUI();
                }
            }
        });
        netbeansCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (netbeansCheckbox.isSelected()&&netbeansCheckbox.getText().equals("Use NetBeans Formatting")){
                    enablementCheckbox.setSelected(false);
                    enableUI();
                    controller.changed();
                }
            }
        });
    }

    public JTextField getFormatterLocField() {
        return formatterLocField;
    }
    
    public JCheckBox getEnabledCheckbox() {
        return enablementCheckbox;
    }
    
    public JCheckBox getNetBeansCheckbox() {
        return netbeansCheckbox;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        enablementCheckbox = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        formatterLocField = new javax.swing.JTextField();
        errorLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        previewPane = new javax.swing.JTextArea();
        netbeansCheckbox = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(enablementCheckbox, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.enablementCheckbox.text")); // NOI18N
        enablementCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enablementCheckboxActionPerformed(evt);
            }
        });

        jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.jLabel1.text")); // NOI18N

        formatterLocField.setText(org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.formatterLocField.text")); // NOI18N

        errorLabel.setForeground(new java.awt.Color(255, 51, 51));
        org.openide.awt.Mnemonics.setLocalizedText(errorLabel, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.errorLabel.text")); // NOI18N
        errorLabel.setToolTipText(org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.errorLabel.toolTipText")); // NOI18N

        previewPane.setEditable(false);
        previewPane.setBackground(new java.awt.Color(255, 255, 255));
        previewPane.setColumns(20);
        previewPane.setRows(5);
        jScrollPane2.setViewportView(previewPane);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(formatterLocField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton)
                        .addGap(7, 7, 7))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE)
                    .addComponent(errorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(formatterLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorLabel)
                .addGap(8, 8, 8))
        );

        org.openide.awt.Mnemonics.setLocalizedText(netbeansCheckbox, org.openide.util.NbBundle.getMessage(EclipseFormatterPanel.class, "EclipseFormatterPanel.netbeansCheckbox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(enablementCheckbox)
                            .addComponent(netbeansCheckbox))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enablementCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(netbeansCheckbox)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void enablementCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enablementCheckboxActionPerformed
        enableUI();
        errorLabel.setText("");
        controller.changed();
    }//GEN-LAST:event_enablementCheckboxActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        //The default dir to use if no value is stored
        File home = new File(System.getProperty("user.home"));
        //Now build a file chooser and invoke the dialog in one line of code
        //"user-dir" is our unique key
        File toAdd = new FileChooserBuilder("user-dir").setFilesOnly(true).setTitle("Open File").
        setDefaultWorkingDirectory(home).setApproveText("Open").showOpenDialog();
        //Result will be null if the user clicked cancel or closed the dialog w/o OK
        if (toAdd != null && toAdd.getName().endsWith("xml")) {
            absolutePath = toAdd.getAbsolutePath();
            formatterLocField.setText(absolutePath);
            formatterLocField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    absolutePath = formatterLocField.getText();
                    try {
                        previewPane.setText(FileUtil.toFileObject(FileUtil.normalizeFile(new File(absolutePath))).asText());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
            try {
                previewPane.setText(FileUtil.toFileObject(FileUtil.normalizeFile(new File(absolutePath))).asText());
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    void load() {
        String globalEclipseFormatterLocation = NbPreferences.forModule(EclipseFormatterPanel.class).get("globalEclipseFormatterLocation", "<no Eclipse formatter defined>");
        boolean isGlobalEclipseFormatterEnabled = NbPreferences.forModule(EclipseFormatterPanel.class).getBoolean("isGlobalEclipseFormatterEnabled", false);
        if (project != null) {
            String localEclipseFormatteLocation;
            boolean isLocalEclipseFormatterEnabled;
            boolean isLocalNetBeansFormatterEnabled;
            Preferences projectPrefs = ProjectUtils.getPreferences(project, IndentUtils.class, true);
            localEclipseFormatteLocation = projectPrefs.get("localEclipseFormatterLocation", "<no Eclipse formatter defined>");
            isLocalEclipseFormatterEnabled = projectPrefs.getBoolean("isLocalEclipseFormatterEnabled", false);
            isLocalNetBeansFormatterEnabled = projectPrefs.getBoolean("isLocalNetBeansFormatterEnabled", false);
            if (isGlobalEclipseFormatterEnabled && !isLocalEclipseFormatterEnabled) {
                localEclipseFormatteLocation = globalEclipseFormatterLocation;
            }
            formatterLocField.setText(localEclipseFormatteLocation);
            netbeansCheckbox.setSelected(isLocalNetBeansFormatterEnabled);
            if (netbeansCheckbox.isSelected()) {
                enablementCheckbox.setSelected(false);
            } else {
                enablementCheckbox.setSelected(isLocalEclipseFormatterEnabled);
            }
            if (!localEclipseFormatteLocation.equals("<no Eclipse formatter defined>")) {
                if (FileUtil.normalizeFile(new File(localEclipseFormatteLocation)).exists()) {
                    try {
                        previewPane.setText(FileUtil.toFileObject(FileUtil.normalizeFile(new File(localEclipseFormatteLocation))).asText());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            } else if (!globalEclipseFormatterLocation.equals("<no Eclipse formatter defined>")) {
                if (FileUtil.normalizeFile(new File(globalEclipseFormatterLocation)).exists()) {
                    try {
                        previewPane.setText(FileUtil.toFileObject(FileUtil.normalizeFile(new File(globalEclipseFormatterLocation))).asText());
                        formatterLocField.setText(globalEclipseFormatterLocation);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
            netbeansCheckbox.setText("Use NetBeans Formatting");
            enablementCheckbox.setText("Override global Eclipse formatter");
            enableUI();
        } else {
            //If no Project in context, i.e., in Options window:
            if (isGlobalEclipseFormatterEnabled) {
                if (FileUtil.normalizeFile(new File(globalEclipseFormatterLocation)).exists()) {
                    try {
                        previewPane.setText(FileUtil.toFileObject(FileUtil.normalizeFile(new File(globalEclipseFormatterLocation))).asText());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
            formatterLocField.setText(globalEclipseFormatterLocation);
            enablementCheckbox.setSelected(isGlobalEclipseFormatterEnabled);
            netbeansCheckbox.setText("Show Debug Information");
            enablementCheckbox.setText("Define global Eclipse formatter");
            enableUI();
        }
    }

    void store() {
        NbPreferences.forModule(EclipseFormatterPanel.class).put("globalEclipseFormatterLocation", formatterLocField.getText());
        NbPreferences.forModule(EclipseFormatterPanel.class).putBoolean("isGlobalEclipseFormatterEnabled", enablementCheckbox.isSelected());
        if (netbeansCheckbox.getText().equals("Show Debug Information")){
            NbPreferences.forModule(EclipseFormatterPanel.class).putBoolean("globalEclipseFormatterDebug", netbeansCheckbox.isSelected());
        }
    }

    boolean valid() {
        if (netbeansCheckbox.isSelected()){
            errorLabel.setText("");
            return true;
        }
        else if (enablementCheckbox.isSelected()) {
            if (new File(formatterLocField.getText()).exists() && new File(formatterLocField.getText()).getName().endsWith("xml")) {
                errorLabel.setText("");
                return true;
            } else {
                errorLabel.setText("OK button disabled until the Eclipse formatter is defined or disabled!");
                return false;
            }
        } else {
            errorLabel.setText("");
            return true;
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JCheckBox enablementCheckbox;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JTextField formatterLocField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JCheckBox netbeansCheckbox;
    private javax.swing.JTextArea previewPane;
    // End of variables declaration//GEN-END:variables

    private void enableUI() {
        jLabel1.setEnabled(enablementCheckbox.isSelected() ? true : false);
        jLabel2.setEnabled(enablementCheckbox.isSelected() ? true : false);
        browseButton.setEnabled(enablementCheckbox.isSelected() ? true : false);
        formatterLocField.setEnabled(enablementCheckbox.isSelected() ? true : false);
        previewPane.setEnabled(enablementCheckbox.isSelected() ? true : false);
    }

}