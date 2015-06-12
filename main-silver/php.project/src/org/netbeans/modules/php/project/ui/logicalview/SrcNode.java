/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.project.ui.logicalview;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.modules.php.project.PhpProject;
import org.netbeans.modules.php.project.ProjectPropertiesSupport;
import org.netbeans.modules.php.project.ui.actions.DebugFileCommand;
import org.netbeans.modules.php.project.ui.actions.DownloadCommand;
import org.netbeans.modules.php.project.ui.actions.RunFileCommand;
import org.netbeans.modules.php.project.ui.actions.RunTestCommand;
import org.netbeans.modules.php.project.ui.actions.RunTestsCommand;
import org.netbeans.modules.php.project.ui.actions.SyncCommand;
import org.netbeans.modules.php.project.ui.actions.UploadCommand;
import org.netbeans.modules.php.project.ui.actions.support.CommandUtils;
import org.netbeans.modules.php.project.ui.customizer.CompositePanelProviderImpl;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.FileSensitiveActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.actions.FileSystemAction;
import org.openide.actions.FindAction;
import org.openide.actions.PasteAction;
import org.openide.actions.ToolsAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFilter;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Radek Matous
 */
@org.netbeans.api.annotations.common.SuppressWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public class SrcNode extends FilterNode {
    @StaticResource
    static final String PACKAGE_BADGE_IMAGE = "org/netbeans/modules/php/project/ui/resources/packageBadge.gif"; // NOI18N
    @StaticResource
    static final String WEB_ROOT_BADGE_IMAGE = "org/netbeans/modules/php/project/ui/resources/webRootBadge.gif"; // NOI18N
    private final PhpProject project;
    private final boolean isTest;
    private final FileObject fo;

    /**
     * creates source root node based on specified DataFolder.
     * Uses specified name.
     */
    SrcNode(PhpProject project, DataFolder folder, DataFilter filter, String name, boolean isTest) {
        this(project, folder, new FilterNode(folder.getNodeDelegate(), folder.createNodeChildren(filter)), name, isTest);
    }

    private SrcNode(final PhpProject project, DataFolder folder, final FilterNode node, String name, final boolean isTest) {
        super(node, org.openide.nodes.Children.createLazy(new Callable<org.openide.nodes.Children>() {
            @Override
            public org.openide.nodes.Children call() throws Exception {
                return new FolderChildren(project, node, isTest);
            }
        }), new ProxyLookup(folder.getNodeDelegate().getLookup()));

        this.project = project;
        this.isTest = isTest;
        fo = folder.getPrimaryFile();

        disableDelegation(DELEGATE_GET_DISPLAY_NAME | DELEGATE_SET_DISPLAY_NAME | DELEGATE_GET_SHORT_DESCRIPTION | DELEGATE_GET_ACTIONS);
        setDisplayName(name);
    }

    @Override
    public String getShortDescription() {
        return FileUtil.getFileDisplayName(fo);
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.mergeImages(super.getIcon(type), ImageUtilities.loadImage(PACKAGE_BADGE_IMAGE, false), 7, 7);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return ImageUtilities.mergeImages(super.getOpenedIcon(type), ImageUtilities.loadImage(PACKAGE_BADGE_IMAGE, false), 7, 7);
    }

    @Override
    public boolean canCopy() {
        return false;
    }

    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canRename() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(CommonProjectActions.newFileAction());
        actions.add(null);
        if (!isTest) {
            actions.add(FileSensitiveActions.fileCommandAction(DownloadCommand.ID, DownloadCommand.DISPLAY_NAME, null));
            actions.add(FileSensitiveActions.fileCommandAction(UploadCommand.ID, UploadCommand.DISPLAY_NAME, null));
            actions.add(FileSensitiveActions.fileCommandAction(SyncCommand.ID, SyncCommand.DISPLAY_NAME, null));
            actions.add(null);
        } else {
            // #252010
            if (project.getTestRoots().getRoots().length > 1) {
                actions.add(ProjectSensitiveActions.projectCommandAction(RunTestsCommand.ID, RunTestsCommand.DISPLAY_NAME, null));
                actions.add(null);
            }
        }
        actions.add(SystemAction.get(FileSystemAction.class));
        actions.add(null);
        actions.add(SystemAction.get(FindAction.class));
        actions.add(null);
        actions.add(SystemAction.get(PasteAction.class));
        actions.add(null);
        actions.add(SystemAction.get(ToolsAction.class));
        actions.add(null);
        // customizer - open sources for source node, testing for test node
        Action customizeAction = null;
        if (isTest) {
            customizeAction = new PhpLogicalViewProvider.CustomizeProjectAction(project, CompositePanelProviderImpl.TESTING);
        } else {
            customizeAction = CommonProjectActions.customizeProjectAction();
        }
        if (customizeAction != null) {
            actions.add(customizeAction);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    static final Action[] COMMON_ACTIONS = new Action[]{
        null,
        FileSensitiveActions.fileCommandAction(DownloadCommand.ID, DownloadCommand.DISPLAY_NAME, null),
        FileSensitiveActions.fileCommandAction(UploadCommand.ID, UploadCommand.DISPLAY_NAME, null),
        FileSensitiveActions.fileCommandAction(SyncCommand.ID, SyncCommand.DISPLAY_NAME, null),
    };

    public static Action createDownloadAction() {
        return COMMON_ACTIONS[1];
    }
    public static Action createUploadAction() {
        return COMMON_ACTIONS[2];
    }
    public static Action createSynchronizeAction() {
        return COMMON_ACTIONS[3];
    }

    /**
     * Children for node that represents folder (SrcNode or PackageNode)
     */
    static class FolderChildren extends FilterNode.Children {
        // common actions for both PackageNode and ObjectNode (equals has to be the same)
        private final PhpProject project;
        private final boolean isTest;

        FolderChildren(PhpProject project, final Node originalNode, boolean isTest) {
            super(originalNode);
            this.project = project;
            this.isTest = isTest;
        }

        @Override
        protected Node[] createNodes(Node key) {
            return super.createNodes(key);
        }

        @Override
        protected Node copyNode(final Node originalNode) {
            FileObject fo = originalNode.getLookup().lookup(FileObject.class);
            if (fo == null) {
                // #201301 - what to do now?
                Logger.getLogger(FolderChildren.class.getName()).log(Level.WARNING, "No fileobject found for node: {0}", originalNode);
                return super.copyNode(originalNode);
            }
            if (fo.isFolder()) {
                return new PackageNode(project, originalNode, isTest);
            }
            return new ObjectNode(originalNode, isTest);
        }
    }

    @org.netbeans.api.annotations.common.SuppressWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    private static final class PackageNode extends FilterNode {

        private final PhpProject project;
        private final boolean isTest;
        private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (PhpProject.PROP_WEB_ROOT.equals(propertyName)) {
                    FileObject folder = getOriginal().getLookup().lookup(FileObject.class);
                    if (folder.equals(evt.getOldValue()) || folder.equals(evt.getNewValue())) {
                        fireIconChange();
                        fireOpenedIconChange();
                    }
                }
            }
        };


        public PackageNode(PhpProject project, final Node originalNode, boolean isTest) {
            super(originalNode, new FolderChildren(project, originalNode, isTest),
                    new ProxyLookup(originalNode.getLookup()));
            this.project = project;
            this.isTest = isTest;

            ProjectPropertiesSupport.addWeakProjectPropertyChangeListener(project, propertyChangeListener);
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> actions = new ArrayList<>();
            actions.addAll(Arrays.asList(getOriginal().getActions(context)));
            Action[] commonActions = getCommonActions();
            // find first separator and add actions there
            int idx = actions.indexOf(null);
            for (int i = 0; i < commonActions.length; i++) {
                if (idx >= 0 && idx + commonActions.length < actions.size()) {
                    //put on the proper place after paste
                    actions.add(idx + i + 1, commonActions[i]);
                } else {
                    //else put at the tail
                    actions.add(commonActions[i]);
                }
            }
            return actions.toArray(new Action[actions.size()]);
        }

        @Override
        public Image getIcon(int type) {
            FileObject folder = getOriginal().getLookup().lookup(FileObject.class);
            if (folder.equals(ProjectPropertiesSupport.getWebRootDirectory(project))
                    && !folder.equals(ProjectPropertiesSupport.getSourcesDirectory(project))) {
                return ImageUtilities.mergeImages(super.getIcon(type), ImageUtilities.loadImage(WEB_ROOT_BADGE_IMAGE, false), 7, 7);
            }
            return super.getIcon(type);
        }

        @Override
        public Image getOpenedIcon(int type) {
            FileObject folder = getOriginal().getLookup().lookup(FileObject.class);
            if (folder.equals(ProjectPropertiesSupport.getWebRootDirectory(project))
                    && !folder.equals(ProjectPropertiesSupport.getSourcesDirectory(project))) {
                return ImageUtilities.mergeImages(super.getOpenedIcon(type), ImageUtilities.loadImage(WEB_ROOT_BADGE_IMAGE, false), 7, 7);
            }
            return super.getOpenedIcon(type);
        }

        private Action[] getCommonActions() {
            if (isTest) {
                return new Action[] {
                    ProjectSensitiveActions.projectCommandAction(RunTestsCommand.ID, RunTestsCommand.DISPLAY_NAME, null),
                    null,
                };
            }
            Action[] actions = new Action[COMMON_ACTIONS.length + 1];
            System.arraycopy(COMMON_ACTIONS, 0, actions, 0, COMMON_ACTIONS.length);
            actions[actions.length - 1] = null;
            return actions;
        }

    }

    @org.netbeans.api.annotations.common.SuppressWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    private static final class ObjectNode extends FilterNode {
        private final Node originalNode;
        private final boolean isTest;

        public ObjectNode(final Node originalNode, boolean isTest) {
            super(originalNode);
            this.originalNode = originalNode;
            this.isTest = isTest;
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> actions = new ArrayList<>();
            actions.addAll(Arrays.asList(getOriginal().getActions(context)));
            // find first separator and add actions there
            int idx = actions.indexOf(null);
            Action[] toAdd = getCommonActions();
            for (int i = 0; i < toAdd.length; i++) {
                if (idx >= 0 && idx + toAdd.length < actions.size()) {
                    //put on the proper place after rename
                    actions.add(idx + i + 1, toAdd[i]);
                } else {
                    //else put at the tail
                    actions.add(toAdd[i]);
                }
            }
            //#143782 find usages on php file has no sense
            for (Iterator<Action> it = actions.iterator(); it.hasNext();) {
                Action action = it.next();
                //hard code string WhereUsedAction chosen not need to depend on refactoring
                //just for this minority issue
                if (action != null
                        && action.getClass().getName().indexOf("WhereUsedAction") != -1) { // NOI18N
                    it.remove();
                    break;
                }
            }
            return actions.toArray(new Action[actions.size()]);
        }

        private Action[] getCommonActions() {
            List<Action> toAdd = new ArrayList<>();
            if (CommandUtils.isPhpOrHtmlFile(getFileObject())) {
                // not available for multiple selected nodes => create new instance every time
                toAdd.add(null);
                toAdd.add(ProjectSensitiveActions.projectCommandAction(RunFileCommand.ID, RunFileCommand.DISPLAY_NAME, null));
                toAdd.add(ProjectSensitiveActions.projectCommandAction(DebugFileCommand.ID, DebugFileCommand.DISPLAY_NAME, null));
                if (!isTest) {
                    toAdd.add(ProjectSensitiveActions.projectCommandAction(RunTestCommand.ID, RunTestCommand.DISPLAY_NAME, null));
                }
            }

            List<Action> actions = new ArrayList<>(COMMON_ACTIONS.length + toAdd.size());
            actions.addAll(toAdd);
            if (!isTest) {
                actions.addAll(Arrays.asList(COMMON_ACTIONS));
            }
            actions.add(null);

            return actions.toArray(new Action[actions.size()]);
        }

        private FileObject getFileObject() {
            FileObject fileObject = originalNode.getLookup().lookup(FileObject.class);
            if (fileObject != null) {
                return fileObject;
            }
            // just fallback, should not happen
            DataObject dataObject = originalNode.getLookup().lookup(DataObject.class);
            assert dataObject != null;
            fileObject = dataObject.getPrimaryFile();
            assert fileObject != null;
            return fileObject;
        }
    }

}