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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.editor.guards;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.guards.GuardedSection;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.InteriorSection;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.netbeans.spi.editor.guards.support.AbstractGuardedSectionsProvider;

/**
 *
 * @author Jan Pokorsky
 */
public final class GuardedSectionsImpl {
    /** Table of the guarded sections. Keys are the names of the sections
    * and values are the GuardedSection classes. The table is null till
    * while document is not in the memory.
    */
    Map<String, GuardedSectionImpl> sections = new HashMap<String, GuardedSectionImpl>(10);

    final GuardedEditorSupport editor;
    
    private NewLine newLineType;
    
    /** Creates a new instance of GuardedDocument */
    public GuardedSectionsImpl(GuardedEditorSupport ces) {
        this.editor = ces;
    }
    
    public Reader createGuardedReader(AbstractGuardedSectionsProvider gr, InputStream stream, Charset encoding) {
        GuardedReader greader = new GuardedReader(gr, stream, false, encoding, this);
        
        Document doc = getDocument();
        if (doc.getProperty(GuardedSectionManager.class) == null) {
            GuardedSectionManager api = GuardsAccessor.DEFAULT.createGuardedSections(this);
            doc.putProperty(GuardedSectionManager.class, api);
        }
        return greader;
    }
    
    public Writer createGuardedWriter(AbstractGuardedSectionsProvider gw, OutputStream stream, Charset encoding) {
        OutputStream os = new NewLineOutputStream(stream, newLineType);
        if (sections != null) {
            List<GuardedSection> list = new ArrayList<GuardedSection>(getGuardedSections());
            if (list.size() > 0) {
                GuardedWriter writer = new GuardedWriter(gw, os, list, encoding);
                return writer;
            }
        }
        Writer w;
        if (encoding == null)
            w = new OutputStreamWriter(os);
        else
            w = new OutputStreamWriter(os, encoding);
        return w;
        
    }
    
    public StyledDocument getDocument() {
        return this.editor.getDocument();
    }

    /** Try to find the section of the given name.
    * @param name the name of the looked-for section
    * @return the found guarded section or <code>null</code> if there is no section
    *         of the given name
    */
    public GuardedSection findSection(String name) {
        StyledDocument doc = this.editor.getDocument();
        synchronized(sections) {
            GuardedSectionImpl gsi = sections.get(name);
            if (gsi != null) {
                return gsi.guard;
            }
        }
        return null;
    }

    public Set<GuardedSection> getGuardedSections() {
        StyledDocument doc = this.editor.getDocument();
        synchronized(this.sections) {
            Set<GuardedSection> sortedGuards =  new TreeSet<GuardedSection>(new GuardedPositionComparator());
            for (GuardedSectionImpl gsi: this.sections.values()) {
                sortedGuards.add(gsi.guard);
            }
            return sortedGuards;
        }
    }
    
    public SimpleSection createSimpleSectionObject(String name, PositionBounds bounds) {
        return (SimpleSection) createSimpleSectionImpl(name, bounds).guard;
    }
    
    public InteriorSection createInteriorSectionObject(String name, PositionBounds header, PositionBounds body, PositionBounds footer) {
        return (InteriorSection) createInteriorSectionImpl(name, header, body, footer).guard;
    }

    public SimpleSection createSimpleSection(Position pos, String name) throws BadLocationException {
        checkNewSection(pos, name);
        return doCreateSimpleSection(pos, name);
    }

    public InteriorSection createInteriorSection(Position pos, String name) throws BadLocationException {
        checkNewSection(pos, name);
        return doCreateInteriorSection(pos, name);
    }
    
    private SimpleSection doCreateSimpleSection(final Position pos, final String name)
        throws /*IllegalArgumentException,*/ BadLocationException  {
        
        StyledDocument loadedDoc = null;
        loadedDoc = this.editor.getDocument();
        final StyledDocument doc = loadedDoc;
        final SimpleSectionImpl[] sect = new SimpleSectionImpl[1];
        final BadLocationException[] blex = new BadLocationException[1];
        
        Runnable r = new Runnable() {
            public void run() {
                try {
                    int where = pos.getOffset();
                    doc.insertString(where, "\n \n", null); // NOI18N
                    sect[0] = createSimpleSectionImpl(name, PositionBounds.create(where + 1, where + 2, GuardedSectionsImpl.this));
                    sect[0].markGuarded(doc);
                } catch (BadLocationException ex) {
                    blex[0] = ex;
                }
            }
        };
        doRunAtomic(doc, r);
        if (blex[0] == null) {
            synchronized (this.sections) {
                sections.put(name, sect[0]);
                return (SimpleSection) sect[0].guard;
            }
        } else {
            throw (BadLocationException) new BadLocationException(
                    "wrong offset", blex[0].offsetRequested() // NOI18N
                    ).initCause(blex[0]);
        }

    }
    
    static void doRunAtomic(Document doc, Runnable r) {
        AtomicLockDocument ald = LineDocumentUtils.asRequired(doc, AtomicLockDocument.class);
        ald.runAtomic(r);
    }
    
    /** Create new interior guarded section at a specified place.
     * @param pos section to create the new one after
     * @param name the name of the new section
     * @exception IllegalArgumentException if the name is already in use
     * @exception BadLocationException if it is not possible to create a
     *            new guarded section here
     */
    private InteriorSection doCreateInteriorSection(final Position pos,
            final String name)
            throws IllegalArgumentException, BadLocationException {
        StyledDocument loadedDoc = null;
        loadedDoc = this.editor.getDocument();
        
        final StyledDocument doc = loadedDoc;
        final InteriorSectionImpl[] sect = new InteriorSectionImpl[1];
        final BadLocationException[] blex = new BadLocationException[1];
        
        Runnable r = new Runnable() {
            public void run() {
                try {
                    int where = pos.getOffset();
                    doc.insertString(where, "\n \n \n \n", null); // NOI18N
                    sect[0] = createInteriorSectionImpl(
                            name,
                            PositionBounds.create(where + 1, where + 2, GuardedSectionsImpl.this),
                            PositionBounds.createBodyBounds(where + 3, where + 4, GuardedSectionsImpl.this),
                            PositionBounds.create(where + 5, where + 6, GuardedSectionsImpl.this)
                            );
                    sections.put(sect[0].getName(), sect[0]);
                    sect[0].markGuarded(doc);
                } catch (BadLocationException ex) {
                    blex[0] = ex;
                }
            }
        };
        doRunAtomic(doc, r);
        
        if (blex[0] == null) {
            synchronized (this.sections) {
                sections.put(name, sect[0]);
                return (InteriorSection) sect[0].guard;
            }
        } else {
            throw (BadLocationException) new BadLocationException(
                    "wrong offset", blex[0].offsetRequested() // NOI18N
                    ).initCause(blex[0]);
        }
    }
    
    // package
    
    AbstractGuardedSectionsProvider gr;
    
    /** Takes the section descriptors from the GuardedReader and
    * fills the table 'sections', also marks as guarded all sections
    * in the given document.
    * @param is Where to take the guarded section descriptions.
    * @param doc Where to mark guarded.
    */
    void fillSections(AbstractGuardedSectionsProvider gr, List<GuardedSection> l, NewLine newLineType) {
        this.gr = GuardsSupportAccessor.DEFAULT.isUseReadersWritersOnSet(gr) ? gr : null;
        this.newLineType = newLineType;
        // XXX this should invalidate removed GS instances
        // XXX maybe would be useful to map new list to old list to keep track of valid instances as much as possible
        // XXX synchronize
        this.sections.clear();
        
        for (GuardedSection gs: l) {
            try {
                GuardedSectionImpl gsi = GuardsAccessor.DEFAULT.getImpl(gs);
                gsi.resolvePositions();
                sections.put(gs.getName(), gsi);
                StyledDocument doc = getDocument();
                gsi.markGuarded(doc);
            } catch (BadLocationException ex) {
                Logger.getLogger(GuardedSectionsImpl.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }
        }
    }

    // private
    
    private SimpleSectionImpl createSimpleSectionImpl(String name, PositionBounds bounds) {
        SimpleSectionImpl sect = new SimpleSectionImpl(name, bounds, this);
        GuardsAccessor.DEFAULT.createSimpleSection(sect);
        return sect;
    }
    
    private InteriorSectionImpl createInteriorSectionImpl(
            String name, PositionBounds header, PositionBounds body, PositionBounds footer) {
        
        InteriorSectionImpl sect;
        sect = new InteriorSectionImpl(name, header, body, footer, this);
        GuardsAccessor.DEFAULT.createInteriorSection(sect);
        return sect;
    }

    private void checkNewSection(Position p, String name) {
        synchronized (sections) {
            checkOverlap(p);
            GuardedSectionImpl gs = sections.get(name);
            if (gs != null) {
                throw new IllegalArgumentException("name exists"); // NOI18N
            }
        }
    }
    
    private void checkOverlap(Position p) throws IllegalArgumentException {
        for (GuardedSectionImpl gs: this.sections.values()) {
            if (gs.contains(p, false))
                throw new IllegalArgumentException("Sections overlap"); // NOI18N
        }
    }
    
    /** This stream is used for changing the new line delimiters.
     * It replaces the '\n' by '\n', '\r' or "\r\n"
     */
    private static class NewLineOutputStream extends OutputStream {
        /** Underlying stream. */
        OutputStream stream;
        
        /** The type of new line delimiter */
        NewLine newLineType;
        
        /** Creates new stream.
         * @param stream Underlaying stream
         * @param newLineType The type of new line delimiter
         */
        public NewLineOutputStream(OutputStream stream, NewLine newLineType) {
            this.stream = stream;
            this.newLineType = newLineType;
        }
        
        /** Write one character.
         * @param b char to write.
         */
        public void write(int b) throws IOException {
            if (b == '\n') {
                switch (newLineType) {
                    case R:
                        stream.write('\r');
                        break;
                    case RN:
                        stream.write('\r');
                    case N:
                        stream.write('\n');
                        break;
                }
            } else {
                stream.write(b);
            }
        }

        public void close() throws IOException {
            super.close();
            this.stream.close();
        }

        public void flush() throws IOException {
            this.stream.flush();
        }
    }
    
}
