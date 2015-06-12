/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.javacard.templates;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 *
 * @author  Anki R. Nelaturu
 */
public class ErrorPanel extends javax.swing.JPanel implements WizardDescriptor.Panel {

    /** Creates new form ErrorPanel */
    public ErrorPanel(String typeName) {
        initComponents();
        typeNameLabel.setText(typeName);
    }
    
    @Override
    public String getName() {
        return "Error";
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        typeNameLabel = new javax.swing.JLabel();

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setForeground(javax.swing.UIManager.getDefaults().getColor("nb.errorForeground"));
        jLabel1.setText(org.openide.util.NbBundle.getMessage(ErrorPanel.class, "ErrorPanel.jLabel1.text")); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, javax.swing.UIManager.getDefaults().getColor("nb.errorForeground")));

        jLabel2.setText(org.openide.util.NbBundle.getMessage(ErrorPanel.class, "ErrorPanel.jLabel2.text")); // NOI18N

        typeNameLabel.setText(org.openide.util.NbBundle.getMessage(ErrorPanel.class, "ErrorPanel.typeNameLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(typeNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE))
                    .addComponent(jLabel1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(typeNameLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel typeNameLabel;
    // End of variables declaration//GEN-END:variables

    public Component getComponent() {
        return this;
    }

    public HelpCtx getHelp() {
        return null;
    }

    public void readSettings(Object arg0) {
        //
    }

    public void storeSettings(Object arg0) {
        //
    }

    public void addChangeListener(ChangeListener arg0) {
        //
    }

    public void removeChangeListener(ChangeListener arg0) {
        //
    }

}