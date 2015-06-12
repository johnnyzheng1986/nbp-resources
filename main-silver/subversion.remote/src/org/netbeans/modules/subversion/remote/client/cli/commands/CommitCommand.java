/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.subversion.remote.client.cli.commands;

import java.io.IOException;
import org.netbeans.modules.subversion.remote.api.ISVNNotifyListener;
import org.netbeans.modules.subversion.remote.api.SVNRevision;
import org.netbeans.modules.subversion.remote.client.cli.Parser.Line;
import org.netbeans.modules.subversion.remote.client.cli.SvnCommand;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.openide.filesystems.FileSystem;

/**
 *
 * @author Tomas Stupka
 */
public class CommitCommand extends SvnCommand {

    private final boolean keepLocks;
    private final boolean recursive;
    private final String message;
    private final VCSFileProxy[] files;
    private long revision = SVNRevision.INVALID_REVISION.getNumber();

    public CommitCommand(FileSystem fileSystem, VCSFileProxy[] files, boolean keepLocks, boolean recursive, String message) {
        super(fileSystem);
        this.keepLocks = keepLocks;
        this.recursive = recursive;
        this.message = message;
        this.files = files;
    }
       
    @Override
    protected ISVNNotifyListener.Command getCommand() {
        return ISVNNotifyListener.Command.COMMIT;
    }
    
    @Override
    public void prepareCommand(Arguments arguments) throws IOException {        
        arguments.add("commit"); //NOI18N
        if (keepLocks) {
            arguments.add("--no-unlock"); //NOI18N
        }
        if(!recursive) {
            arguments.add("-N"); //NOI18N
        }
        arguments.addMessage(message);	        
        arguments.addFileArguments(files);
        setCommandWorkingDirectory(files);
    }

    public long getRevision() {
        return revision;
    }

    @Override
    protected void notify(Line line) {
        if(line.getRevision() != -1) {
//          XXX can't rely on this - see also update cmd                        
//            if(revision != -1) {
//                Subversion.LOG.warning(
//                        "Revision notified more times : " + revision + ", " + 
//                        line.getRevision() + " for command " + getStringCommand());                
//            }
            revision = line.getRevision();            
        }
    }            
    
}