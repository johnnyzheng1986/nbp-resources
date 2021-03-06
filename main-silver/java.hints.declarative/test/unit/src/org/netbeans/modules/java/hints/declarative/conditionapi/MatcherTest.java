/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.declarative.conditionapi;

import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import org.netbeans.modules.java.hints.spiimpl.SPIAccessor;
import org.netbeans.modules.java.hints.spiimpl.TestBase;
import org.netbeans.modules.java.hints.spiimpl.options.HintsSettings;

/**
 *
 * @author lahvac
 */
public class MatcherTest extends TestBase {

    public MatcherTest(String name) {
        super(name);
    }

    public void testReferencedInNoNPEForMissingTrees() throws Exception {
        String code = "package test; public class Test { private void test() { | if (true) System.err.println(); } private int a^aa;}";
        int pos = code.indexOf("|");
        
        code = code.replaceAll(Pattern.quote("|"), "");

        int varpos = code.indexOf("^");
        
        code = code.replaceAll(Pattern.quote("^"), "");

        prepareTest("test/Test.java", code);

        TreePath tp = info.getTreeUtilities().pathFor(pos);
        TreePath var = info.getTreeUtilities().pathFor(varpos);
        Map<String, TreePath> variables = Collections.singletonMap("$1", var);
        Map<String, Collection<? extends TreePath>> multiVariables = Collections.<String, Collection<? extends TreePath>>singletonMap("$2$", Arrays.asList(tp));
        Map<String, String> variables2Names = Collections.emptyMap();
        Context ctx = new Context(SPIAccessor.getINSTANCE().createHintContext(info, HintsSettings.getGlobalSettings(), null, null, variables, multiVariables, variables2Names));

        new Matcher(ctx).referencedIn(new Variable("$1"), new Variable("$2$"));
    }

}
