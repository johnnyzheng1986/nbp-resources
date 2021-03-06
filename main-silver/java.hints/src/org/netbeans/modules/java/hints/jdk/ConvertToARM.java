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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.jdk;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.MatcherUtilities;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.jdk.ConvertToARM", description = "#DESC_org.netbeans.modules.java.hints.jdk.ConvertToARM", category="rules15", suppressWarnings="ConvertToTryWithResources")  //NOI18N
public class ConvertToARM {

    private static final Logger LOG = Logger.getLogger(ConvertToARMFix.class.getName());
    private static final SpecificationVersion JDK_17 = new SpecificationVersion("1.7"); //NOI18N
    
    private static final String AUTO_CLOSEABLE = "java.lang.AutoCloseable"; //NOI18N
    
    private static final String PTR_ENC_NONE_NO_TRY = "$CV $var = $init; $stms$; $var.close();";    //NOI18N
    private static final String PTR_ENC_NONE_NO_TRY_FIN = "final $CV $var = $init; $stms$; $var.close();";  //NOI18N
    private static final String PTR_ENC_NONE_TRY = "$CV $var = $init; try { $stms$; } catch $catches$ finally {$var.close(); $finstms$;}";  //NOI18N
    private static final String PTR_ENC_NONE_TRY_FIN = "final $CV $var = $init; try { $stms$; } catch $catches$ finally {$var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_NONE_TRY_NULL = "$CV $var = null; try { $var = $init; $stms$; } catch $catches$ finally {if ($var != null) $var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_NONE_TRY_NULL2 = "$CV $var = null; try { $var = $init; $stms$; } catch $catches$ finally {$var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_NONE_TRY_NULL2_SHADOW = "$CV_x $var_x = null; try { $var_x = $init_x; $stms_x$; } catch $catches_x$ finally {$var_x.close(); $finstms_x$;}"; //NOI18N
    
    // buggy code that actually does not close the AutoCloseable in case of an error
    private static final String PTR_ENC_ENCLOSED = "try { $CV $var = $init; $stms$; $var.close(); $suff$; } catch $catches$ finally { $finstms$; }"; // NOI18N
    private static final String PTR_ENC_ENCLOSED2 = "try { $CV $var = $init; $stms$; $var.close(); $suff$; } catch $catches$"; // NOI18N
    private static final String PTR_ENC_ENCLOSED3 = "try { $CV $var = $init; $stms$; $var.close(); $suff$; } finally { $finstms$; }"; // NOI18N
    private static final String PTR_ENC_ENCLOSED_FIN = "try { final $CV $var = $init; $stms$; $var.close(); $suff$; } catch $catches$ finally { $finstms$; }";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_FIN2 = "try { final $CV $var = $init; $stms$; $var.close(); $suff$; } catch $catches$";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_FIN3 = "try { final $CV $var = $init; $stms$; $var.close(); $suff$; } finally { $finstms$; }";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT = "$CV $var = $init; try { $stms$; $var.close(); $suff$; } catch $catches$ finally { $finstms$;}";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT2 = "$CV $var = $init; try { $stms$; $var.close(); $suff$; } catch $catches$ finally { $finstms$;}";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT3 = "$CV $var = $init; try { $stms$; $var.close(); $suff$; } finally { $finstms$;}";  //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT_FIN = "final $CV $var = $init; try { $stms$; $var.close(); $suff$; } catch $catches$ finally {$var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT_FIN2 = "final $CV $var = $init; try { $stms$; $var.close(); $suff$; } catch $catches$;"; //NOI18N
    private static final String PTR_ENC_ENCLOSED_INIT_FIN3 = "final $CV $var = $init; try { $stms$; $var.close(); $suff$; } finally {$var.close(); $finstms$;}"; //NOI18N

    private static final String PTR_ENC_OUT_NO_TRY = "$CV $var = $init; try($armres$) {$stms$;} $var.close();";    //NOI18N
    private static final String PTR_ENC_OUT_NO_TRY_SHADOW = "$CV_x $var_x = $init_x; try($armres_x$) {$stms_x$;} $var_s.close();";    //NOI18N
    private static final String PTR_ENC_OUT_NO_TRY_FIN = "final $CV $var = $init; try($armres$) {$stms$;} $var.close();";  //NOI18N
    private static final String PTR_ENC_OUT_NO_TRY_FIN_SHADOW = "final $CV_x $var_x = $init_x; try($armres_x$) {$stms_x$;} $var_x.close();";  //NOI18N
    private static final String PTR_ENC_OUT_TRY = "$CV $var = $init; try { try($armres$) {$stms$;} } catch $catches$ finally {$var.close(); $finstms$;}";  //NOI18N
    private static final String PTR_ENC_OUT_TRY_SHADOW = "$CV_x $var_x = $init_x; try { try($armres_x$) {$stms_x$;} } catch $catches_x$ finally {$var_x.close(); $finstms_x$;}";  //NOI18N
    private static final String PTR_ENC_OUT_TRY_FIN = "final $CV $var = $init; try { try($armres$) {$stms$;} } catch $catches$ finally {$var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_OUT_TRY_FIN_SHADOW = "final $CV_x $var_x = $init_x; try { try($armres_x$) {$stms_x$;} } catch $catches_x$ finally {$var_x.close(); $finstms_x$;}"; //NOI18N
    private static final String PTR_ENC_OUT_TRY_NULL = "$CV $var = null; try { $var = $init; try($armres$) {$stms$;} } catch $catches$ finally {if ($var != null) $var.close(); $finstms$;}"; //NOI18N
    private static final String PTR_ENC_OUT_TRY_NULL_SHADOW = "$CV_x $var_x = null; try { $var_x = $init_x; try($armres_x$) {$stms_x$;} } catch $catches_x$ finally {if ($var_x != null) $var_x.close(); $finstms_x$;}"; //NOI18N
       
    private static final String PTR_ENC_IN_NO_TRY = "try($armres$) {$CV $var = $init; $stms$; $var.close(); $suff$;} catch $catches$";
    private static final String PTR_ENC_IN_NO_TRY_SHADOW = "try($armres_x$) {$CV_x $var_x = $init_x; $stms_x$; $var_x.close();} catch $catches_x$";
    private static final String PTR_ENC_IN_NO_TRY2 = "try($armres$) {$CV $var = $init; $stms$; $var.close(); $suff$;} catch $catches$ finally {$finstms$;}";
    private static final String PTR_ENC_IN_NO_TRY2_SHADOW = "try($armres_x$) {$CV_x $var_x = $init_x; $stms_x$; $var_x.close();} catch $catches_x$ finally {$finstms_x$;}";
    private static final String PTR_ENC_IN_NO_TRY_FIN = "try($armres$) {final $CV $var = $init; $stms$; $var.close(); $suff$;} catch $catches$";
    private static final String PTR_ENC_IN_NO_TRY_FIN_SHADOW = "try($armres_x$) {final $CV_x $var_x = $init_x; $stms_x$; $var_x.close();} catch $catches_x$";
    private static final String PTR_ENC_IN_NO_TRY2_FIN = "try($armres$) {final $CV $var = $init; $stms$; $var.close(); $suff$;} catch $catches$ finally {$finstms$;}";
    private static final String PTR_ENC_IN_NO_TRY2_FIN_SHADOW = "try($armres_x$) {$CV_x $var_x = $init_x; $stms_x$; $var_x.close();} catch $catches_x$ finally {$finstms_x$;}";
    private static final String PTR_ENC_IN_TRY = "try($armres$) { $CV $var = $init; try { $stms$; } finally {$var.close();}} catch $catches$";  //NOI18N
    private static final String PTR_ENC_IN_TRY_SHADOW = "try($armres_x$) { $CV_x $var_x = $init_x; try { $stms_x$; } finally {$var_x.close();}} catch $catches_x$";  //NOI18N
    private static final String PTR_ENC_IN_TRY2 = "try($armres$) { $CV $var = $init; try { $stms$; } finally {$var.close();}} catch $catches$ finally {$finstms$;}";  //NOI18N
    private static final String PTR_ENC_IN_TRY2_SHADOW = "try($armres_x$) { $CV_x $var_x = $init_x; try { $stms_x$; } finally {$var_x.close();}} catch $catches_x$ finally {$finstms_x$;}";  //NOI18N    
    private static final String PTR_ENC_IN_TRY_FIN = "try($armres$) { final $CV $var = $init; try { $stms$; } finally {$var.close();}} catch $catches$";  //NOI18N
    private static final String PTR_ENC_IN_TRY_FIN_SHADOW = "try($armres_x$) { final $CV_x $var_x = $init_x; try { $stms_x$; } finally {$var_x.close();}} catch $catches_x$";  //NOI18N
    private static final String PTR_ENC_IN_TRY2_FIN = "try($armres$) { final $CV $var = $init; try { $stms$; } finally {$var.close();}} catch $catches$ finally {$finstms$;}";  //NOI18N
    private static final String PTR_ENC_IN_TRY2_FIN_SHADOW = "try($armres_x$) { final $CV_x $var_x = $init_x; try { $stms_x$; } finally {$var_x.close();}} catch $catches_x$ finally {$finstms_x$;}";  //NOI18N    
    private static final String PTR_ENC_IN_TRY_NULL = "try($armres$) { $CV $var = null; try { $var = $init; $stms$; } finally {if ($var != null) $var.close();}} catch $catches$"; //NOI18N
    private static final String PTR_ENC_IN_TRY_NULL_SHADOW = "try($armres_x$) { $CV_x $var_x = null; try { $var_x = $init_x; $stms_x$; } finally {if ($var_x != null) $var_x.close();}} catch $catches_x$"; //NOI18N
    private static final String PTR_ENC_IN_TRY_NULL2 = "try($armres$) { $CV $var = null; try { $var = $init; $stms$; } finally {if ($var != null) $var.close();}} catch $catches$ finally {$finstms$;}"; //NOI18N
    private static final String PTR_ENC_IN_TRY_NULL2_SHADOW = "try($armres_x$) { $CV_x $var_x = null; try { $var_x = $init_x; $stms_x$; } finally {if ($var_x != null) $var_x.close();}} catch $catches_x$ finally {$finstms_x$;}"; //NOI18N
    
    static boolean checkAutoCloseable = true;

    @NbBundle.Messages("FIX_ChangeTryToARM=Add resources to enclosing try")
    @TriggerPatterns({
            @TriggerPattern(value = PTR_ENC_ENCLOSED),
            @TriggerPattern(value = PTR_ENC_ENCLOSED2),
            @TriggerPattern(value = PTR_ENC_ENCLOSED3),
            
            @TriggerPattern(value = PTR_ENC_ENCLOSED_FIN),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_FIN2),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_FIN3),
            
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT2),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT3),
            
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT_FIN),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT_FIN2),
            @TriggerPattern(value = PTR_ENC_ENCLOSED_INIT_FIN3),
    })
    public static List<ErrorDescription> hintCorrectCode(final HintContext ctx) {
        return hintImpl(ctx, NestingKind.IN, "FIX_ChangeTryToARM");
    }

    @TriggerPatterns(
        {
            @TriggerPattern(value=PTR_ENC_NONE_NO_TRY),
            @TriggerPattern(value=PTR_ENC_NONE_NO_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_NONE_TRY),
            @TriggerPattern(value=PTR_ENC_NONE_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_NONE_TRY_NULL)
        }
    )
    public static List<ErrorDescription> hint11(final HintContext ctx) {
        return hint1Impl(ctx, false, "TXT_ConvertToARM");
    }

    @TriggerPatterns(
        {
            @TriggerPattern(value=PTR_ENC_NONE_TRY_NULL2)
        }
    )
    public static List<ErrorDescription> hint12(final HintContext ctx) {
        return hint1Impl(ctx, true);
    }

    public static List<ErrorDescription> hint1Impl(HintContext ctx, boolean secondRule) {
        return hint1Impl(ctx, secondRule, "TXT_ConvertToARM");
    }
    
    public static List<ErrorDescription> hint1Impl(HintContext ctx, boolean secondRule, String fixKey) {
        if (!MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_OUT_NO_TRY_SHADOW)     &&
            !MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_OUT_NO_TRY_FIN_SHADOW) &&
            !MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_OUT_TRY_SHADOW) &&
            !MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_OUT_TRY_FIN_SHADOW) &&
            !MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_OUT_TRY_NULL_SHADOW) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_NO_TRY_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_NO_TRY2_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_NO_TRY_FIN_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_NO_TRY2_FIN_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY2_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY_FIN_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY2_FIN_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY_NULL_SHADOW) && insideARM(ctx)) &&
            !(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), PTR_ENC_IN_TRY_NULL2_SHADOW) && insideARM(ctx))) {
            if (!secondRule && MatcherUtilities.matches(ctx, ctx.getPath(), PTR_ENC_NONE_TRY_NULL2_SHADOW)) {
                return Collections.<ErrorDescription>emptyList();
            } else {
                return hintImpl(ctx, NestingKind.NONE, fixKey);
            }
        } else {
            return Collections.<ErrorDescription>emptyList();
        }
    }

        
    @NbBundle.Messages("FIX_MergeTryResources=Add to enclosing try-with-resources")
    @TriggerPatterns(
        {
            @TriggerPattern(value=PTR_ENC_OUT_NO_TRY),
            @TriggerPattern(value=PTR_ENC_OUT_NO_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_OUT_TRY),
            @TriggerPattern(value=PTR_ENC_OUT_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_OUT_TRY_NULL)
        }
    )
    public static List<ErrorDescription> hint2(HintContext ctx) {
        return hintImpl(ctx, NestingKind.OUT, "FIX_MergeTryResources");
    }
    
    @TriggerPatterns(
        {
            @TriggerPattern(value=PTR_ENC_IN_NO_TRY),
            @TriggerPattern(value=PTR_ENC_IN_NO_TRY2),
            @TriggerPattern(value=PTR_ENC_IN_NO_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_IN_NO_TRY2_FIN),
            @TriggerPattern(value=PTR_ENC_IN_TRY),
            @TriggerPattern(value=PTR_ENC_IN_TRY2),
            @TriggerPattern(value=PTR_ENC_IN_TRY_FIN),
            @TriggerPattern(value=PTR_ENC_IN_TRY2_FIN),
            @TriggerPattern(value=PTR_ENC_IN_TRY_NULL),
            @TriggerPattern(value=PTR_ENC_IN_TRY_NULL2)
        }
    )
    public static List<ErrorDescription> hint3(HintContext ctx) {
        if (insideARM(ctx)) {
            return hintImpl(ctx, NestingKind.IN,  "FIX_MergeTryResources");
        } else {
            return Collections.<ErrorDescription>emptyList();
        }
    }       
    
    private static List<ErrorDescription> hintImpl(final HintContext ctx, final NestingKind nestingKind, String key) {
        Parameters.notNull("ctx", ctx); //NOI18N        
        final Map<String,TreePath> vars = ctx.getVariables();
        final TreePath varVar = vars.get("$var");    //NOI18N
        assert varVar != null;
        final TreePath typeVar = vars.get("$CV");    //NOI18N
        assert typeVar != null;
        final CompilationInfo info = ctx.getInfo();        
        final TypeMirror type = info.getTrees().getTypeMirror(typeVar);
        final List<ErrorDescription> result = new ArrayList<ErrorDescription>(1);
        if (type != null && type.getKind() == TypeKind.DECLARED) {
            final Element autoCloseable = info.getElements().getTypeElement(AUTO_CLOSEABLE);
            if (isSupportedSourceLevel(ctx.getInfo().getFileObject())  && (!checkAutoCloseable || (autoCloseable != null && info.getTypes().isSubtype(type, autoCloseable.asType())))) {
                final Map<String,Collection<? extends TreePath>> multiVars = ctx.getMultiVariables();
                final Collection<? extends TreePath> stms = multiVars.get("$stms$");    //NOI18N
                final Trees trees = ctx.getInfo().getTrees();
                final VariableElement resElement = (VariableElement) trees.getElement(varVar);
                if (resElement != null && !stms.isEmpty() && !isAssigned(resElement, stms, trees)) {
                    final Collection<? extends TreePath> tail;
                    
                    if (multiVars.containsKey("$suff$")) {
                        tail = multiVars.get("$suff$"); // NOI18N
                    } else {
                        tail = multiVars.get("$$2$");  //NOI18N
                    }
                    final Collection<? extends TreePath> usages = findResourceUsagesAfterClose(resElement, tail, varVar.getCompilationUnit(), trees);
                    final Collection<TreePath> cleanUpStatements = new LinkedList<TreePath>();
                    if (!hasNonCleanUpUsages(usages, cleanUpStatements) && !splitVariablesClash(stms, tail, trees)) {
                        result.add(ErrorDescriptionFactory.forName(
                            ctx,
                            varVar,
                            NbBundle.getMessage(ConvertToARM.class, "TXT_ConvertToARM"),
                        new ConvertToARMFix(
info,
key,
ctx.getPath(),
nestingKind,
varVar,
vars.get("$init"),              //NOI18N
multiVars.get("$armres$"),      //NOI18N
stms,
multiVars.get("$catches$"),     //NOI18N
multiVars.get("$finstms$"),     //NOI18N
tail,
cleanUpStatements).toEditorFix()
                        ));
                    }
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
    
    private static Collection<? extends TreePathHandle> wrap(CompilationInfo info, Collection<? extends TreePath> paths) {
        if (paths == null) return null;
        
        Collection<TreePathHandle> result = new ArrayList<TreePathHandle>(paths.size());
        
        for (TreePath tp : paths) {
            result.add(TreePathHandle.create(tp, info));
        }
        
        return result;
    }

    private static final Collection<? extends TreePath> UNRESOLVABLE_MARKER = new ArrayList<TreePath>();
    
    private static Collection<? extends TreePath> unwrap(CompilationInfo info, Collection<? extends TreePathHandle> paths) {
        if (paths == null) return null;
        
        Collection<TreePath> result = new ArrayList<TreePath>(paths.size());
        
        for (TreePathHandle tph : paths) {
            TreePath tp = tph.resolve(info);
            
            if (tp == null) {
                LOG.log(Level.FINE, "Cannot resolve TreePathHandle: {0}", tp.toString());
                return UNRESOLVABLE_MARKER;
            }
            
            result.add(tp);
        }
        
        return result;
    }
    private static final class ConvertToARMFix extends JavaFix {
        private final String bundleKey;
        private final NestingKind nestingKind;
        private final TreePathHandle initHandle;
        private final TreePathHandle varHandle;
        private final Collection<? extends TreePathHandle> armPathHandles;
        private final Collection<? extends TreePathHandle> statementsPathHandles;
        private final Collection<? extends TreePathHandle> catchesPathHandles;
        private final Collection<? extends TreePathHandle> finStatementsPathHandles;
        private final Collection<? extends TreePathHandle> tailHandle;
        private final Collection<? extends TreePathHandle> cleanUpStmsHandle;
        
        private ConvertToARMFix(
                final CompilationInfo info,
                final String bundleKey,
                final TreePath owner,
                final NestingKind nestignKind,
                final TreePath var,
                final TreePath init,
                final Collection<? extends TreePath> armPaths,
                final Collection<? extends TreePath> statements,
                final Collection<? extends TreePath> catches,
                final Collection<? extends TreePath> finStatementsPath,
                final Collection<? extends TreePath> tail,
                final Collection<? extends TreePath> cleanUpStms) {
            super(info, owner);
            this.bundleKey = bundleKey;
            this.nestingKind = nestignKind;
            this.varHandle = var != null ? TreePathHandle.create(var, info) : null;
            this.initHandle = init != null ? TreePathHandle.create(init, info) : null;
            this.armPathHandles = wrap(info, armPaths);
            this.statementsPathHandles = wrap(info, statements);
            this.catchesPathHandles = wrap(info, catches);
            this.finStatementsPathHandles = wrap(info, finStatementsPath);
            this.tailHandle = wrap(info, tail);
            this.cleanUpStmsHandle = wrap(info, cleanUpStms);
        }

        @Override
        public String getText() {
            return NbBundle.getMessage(ConvertToARM.class, bundleKey);
        }
        
        @Override
        protected void performRewrite(TransformationContext ctx) {
            final WorkingCopy wc = ctx.getWorkingCopy();
            final TreePath tp = ctx.getPath();
            final TreePath init = this.initHandle.resolve(wc);
            if (init == null) {
                LOG.log(Level.FINE, "Cannot resolve TreePathHandle: {0}", this.initHandle.toString());
                return ;
            }
            final TreePath var = this.varHandle.resolve(wc);
            if (var == null) {
                LOG.log(Level.FINE, "Cannot resolve TreePathHandle: {0}", this.varHandle.toString());
                return ;
            }
            final Collection<? extends TreePath> armPaths = unwrap(wc, this.armPathHandles);
            final Collection<? extends TreePath> statementsPaths = unwrap(wc, this.statementsPathHandles);
            final Collection<? extends TreePath> catchesPaths = unwrap(wc, this.catchesPathHandles);
            final Collection<? extends TreePath> finStatementsPath = unwrap(wc, this.finStatementsPathHandles);
            final Collection<? extends TreePath> tail = unwrap(wc, this.tailHandle);
            final Collection<? extends TreePath> cleanUpStms = unwrap(wc, this.cleanUpStmsHandle);
            final TreeMaker tm = wc.getTreeMaker();
            final GeneratorUtilities gen = GeneratorUtilities.get(wc);
            gen.importComments(tp.getLeaf(), wc.getCompilationUnit());
            final Set<StatementTree> nonNeededStms = new HashSet<StatementTree>();
            for (TreePath stm : cleanUpStms) {
                BlockTree owner = (BlockTree)stm.getParentPath().getLeaf();
                if (owner == tp.getLeaf()) {
                    nonNeededStms.add((StatementTree)stm.getLeaf());
                } else {
                    rewriteCopyComments(wc, owner,
                        tm.removeBlockStatement(
                            owner,
                            (StatementTree)stm.getLeaf()));
                }
            }
            if (nestingKind == NestingKind.NONE) {
                final List<? extends StatementTree> statements = ConvertToARMFix.<StatementTree>asList(statementsPaths);
                final List<VariableTree> additionalVars = new LinkedList<VariableTree>();
                final List<VariableTree> removedVars = new LinkedList<VariableTree>();
                if (tail != null && !tail.isEmpty()) {
                    final Collection<VariableTree> usedAfterCloseVarDecls = findVarsUsages(
                            findVariableDecls(statements, statementsPaths.isEmpty()? null : statementsPaths.iterator().next().getParentPath().getLeaf()),
                            ConvertToARMFix.<StatementTree>asList(tail),
                            tail.iterator().next().getCompilationUnit(),
                            wc.getTrees());                    
                    for (VariableTree vr : usedAfterCloseVarDecls) {
                        additionalVars.add(tm.Variable(
                                vr.getModifiers(),
                                vr.getName(),
                                vr.getType(),
                                null));
                        if (vr.getInitializer() != null) {
                            rewriteCopyComments(wc, vr,
                                tm.ExpressionStatement(tm.Assignment(
                                    tm.Identifier(vr.getName()),
                                    vr.getInitializer())));
                        } else {
                            removedVars.add(vr);
                        }
                    }
                }
                final List<StatementTree> filteredStatements = new LinkedList<StatementTree>(statements);
                filteredStatements.removeAll(removedVars);
                final BlockTree block = tm.Block(filteredStatements, false);
                final VariableTree varTree = addInit(wc,
                        removeFinal(wc, (VariableTree)var.getLeaf()),
                        (ExpressionTree)init.getLeaf());
                final TryTree tryTree = tm.Try(
                        Collections.singletonList(varTree),
                        block,
                        ConvertToARMFix.<CatchTree>asList(catchesPaths),
                        rewriteFinallyBlock(tm,finStatementsPath));
                rewriteCopyComments(wc, tp.getLeaf(), rewriteOwnerBlock(gen, 
                        tm,
                        ((BlockTree)tp.getLeaf()).getStatements(),
                        (StatementTree)var.getLeaf(),
                        additionalVars,
                        tryTree,
                        statements,
                        nonNeededStms));
            } else if (nestingKind == NestingKind.OUT) {
                final TryTree oldTry = findNestedARM(
                        ((BlockTree)tp.getLeaf()).getStatements(),
                        (StatementTree)var.getLeaf());
                if (oldTry == null) {
                    return;
                }
                final List<Tree> arm = new ArrayList<Tree>();                
                arm.add(addInit(wc,
                        removeFinal(wc, (VariableTree)var.getLeaf()),
                        (ExpressionTree)init.getLeaf()));
                arm.addAll(removeFinal(wc, ConvertToARMFix.<Tree>asList(armPaths)));                
                final TryTree newTry = tm.Try(
                        arm,
                        oldTry.getBlock(),
                        ConvertToARMFix.<CatchTree>asList(catchesPaths),
                        rewriteFinallyBlock(tm,finStatementsPath));
                rewriteCopyComments(wc, tp.getLeaf(), rewriteOwnerBlock(gen, 
                        tm,
                        ((BlockTree)tp.getLeaf()).getStatements(),
                        (StatementTree)var.getLeaf(),
                        Collections.<VariableTree>emptyList(),
                        newTry,
                        ConvertToARMFix.<StatementTree>asList(statementsPaths),
                        nonNeededStms));
            } else if (nestingKind == NestingKind.IN) {
                final TreePath oldTryPath = findEnclosingARMPath(var);
                if (oldTryPath == null) {
                    return;
                }
                final TryTree oldTry = (TryTree)oldTryPath.getLeaf();
                Tree rewriteTree = oldTry;
                final List<Tree> arm = new ArrayList<Tree>(removeFinal(wc, oldTry.getResources()));
                arm.add(addInit(wc,
                        removeFinal(wc, (VariableTree)var.getLeaf()),
                        (ExpressionTree)init.getLeaf()));
                final TryTree newTry = tm.Try(
                        arm,
                        tm.Block(ConvertToARMFix.<StatementTree>asList(statementsPaths), false),
                        oldTry.getCatches(),
                        oldTry.getFinallyBlock());
                Tree newTree = newTry;
                
                
                // some code needs to be moved AFTER the finally:
                if (tail != null && !tail.isEmpty()) {
                    TreePath parent = oldTryPath.getParentPath();
                    if (parent != null && parent.getLeaf().getKind() == Tree.Kind.BLOCK) {
                        // simpler: insert a statement after the (rewritten) tree:
                        BlockTree bt = (BlockTree)parent.getLeaf();
                        int index = bt.getStatements().indexOf(oldTry);
                        List<? extends StatementTree> stats = ConvertToARMFix.<StatementTree>asList(tail);
                        for (int i = 0; i < stats.size(); i++) {
                            bt = tm.insertBlockStatement(bt, index + 1 + i, stats.get(i));
                        }
                        rewriteCopyComments(wc, parent.getLeaf(), bt);
                    } else {
                        // worse: need to replace the old try with a block
                        List<StatementTree> stats = new ArrayList<>();
                        stats.add(newTry);
                        stats.addAll(ConvertToARMFix.<StatementTree>asList(statementsPaths));
                        
                        newTree = tm.Block(stats, false);
                    }
                }
                rewriteCopyComments(wc, rewriteTree, newTree);
            }            
        }
        
        @SuppressWarnings("unchecked")
        private static <R extends Tree> List<? extends R> asList(final Collection<? extends TreePath> data) {
            if (data == null) {
                return Collections.<R>emptyList();
            }
            final List<R> result = new ArrayList<R>(data.size());
            for (TreePath element : data) {
                result.add((R)element.getLeaf());
            }
            return result;
        }
        
        private static BlockTree rewriteOwnerBlock(
                final GeneratorUtilities gen,
                final TreeMaker tm,
                final List<? extends StatementTree> originalStatements,
                final StatementTree var,
                final List<? extends VariableTree> preVarDecls,
                final TryTree newTry,
                final List<? extends StatementTree> oldStms,
                final Set<? extends StatementTree> removeStms) {
            final List<StatementTree> statements = new ArrayList<StatementTree>(originalStatements.size());
            int state = 0;  //0 - ordinary,1 - replace by try, 2 - remove 
            final Set<Tree> toRemove = new HashSet<Tree>(oldStms);
            for (StatementTree statement : originalStatements) {
                if (removeStms.contains(statement)) {
                    continue;
                }
                if (var == statement) {
                    statements.addAll(preVarDecls);
                    state = 1;
                    continue;
                } else if (state == 1) {
                    state =  toRemove.contains(statement) || 
                            (statement.getKind() == Kind.TRY && 
                            ((TryTree)statement).getResources() != null &&
                            !((TryTree)statement).getResources().isEmpty())? 2 : 0;
                    gen.copyComments(statement, newTry, true);
                    gen.copyComments(statement, newTry, false);
                    if (statement.getKind() == Kind.TRY) {
                        // copy over the comments for the finally block, it has been regenerated
                        TryTree tt = (TryTree)statement;
                        if (tt.getFinallyBlock() != null) {
                            Tree nt = newTry.getFinallyBlock();
                            if (nt == null) {
                                nt = newTry;
                            }
                            gen.copyComments(tt.getFinallyBlock(), nt, true);
                            gen.copyComments(tt.getFinallyBlock(), nt, false);
                        }
                    }
                    statement = newTry;
                } else if (state == 2) {
                    if (!toRemove.contains(statement)) {
                        state = 0;
                    }
                    continue;
                }
                statements.add(statement);
            }
            return tm.Block(statements, false);
        }
        
        private static BlockTree rewriteFinallyBlock(
                final TreeMaker tm,
                final Collection<? extends TreePath> paths) {
            if (paths == null || paths.isEmpty()) {
                return null;
            }
            final List<StatementTree> statements = new ArrayList<StatementTree>(paths.size());
            for (TreePath stp : paths) {
                statements.add((StatementTree)stp.getLeaf());
            }
            final BlockTree result = tm.Block(statements, false);
            return result;
        }
    }
    
    private static void rewriteCopyComments(WorkingCopy wc, Tree from, Tree to) {
        GeneratorUtilities gen = GeneratorUtilities.get(wc);
        gen.copyComments(from, to, true);
        gen.copyComments(from, to, false);
        wc.rewrite(from, to);
    }

    private static VariableTree removeFinal(
            final WorkingCopy wc,
            final VariableTree varTree) {
        final ModifiersTree oldMods = varTree.getModifiers();
        if (oldMods != null && oldMods.getFlags().contains(Modifier.FINAL)) {
            final ModifiersTree newMods = wc.getTreeMaker().removeModifiersModifier(oldMods, Modifier.FINAL);
            rewriteCopyComments(wc, oldMods, newMods);
        }
        return varTree;
    }
    
    private static Collection<? extends Tree> removeFinal(
            final WorkingCopy wc,
            final Collection<? extends Tree> trees) {
        final List<Tree> result = new ArrayList<Tree>(trees.size());
        for (Tree vt : trees) {
            result.add(vt.getKind() == Kind.VARIABLE ? removeFinal(wc, (VariableTree)vt) : vt);
        }
        return result;
    }
    
    private static VariableTree addInit (
            final WorkingCopy wc,
            final VariableTree var,
            final ExpressionTree init) {
        final ExpressionTree currentInit = var.getInitializer();
        if (currentInit.getKind() == Kind.NULL_LITERAL) {
            final VariableTree newVar = wc.getTreeMaker().Variable(var.getModifiers(), var.getName(), var.getType(), init);
            rewriteCopyComments(wc, var, newVar);
            return newVar;
        } else {
            return var;
        }
    }
    
    private static TryTree findNestedARM(
            final Collection<? extends StatementTree> stms,
            final StatementTree var) {
        int state = 0;
        for (StatementTree stm : stms) {
            if (stm == var) {
                state = 1;
            } else if (state == 1) {
                if (stm.getKind() == Kind.TRY) {
                    final TryTree tryTree = (TryTree)stm;
                    if (tryTree.getResources() != null && !tryTree.getResources().isEmpty()) {
                        return tryTree;
                    } else {
                        final Iterator<? extends StatementTree> blkStms = tryTree.getBlock().getStatements().iterator();
                        if (blkStms.hasNext()) {
                            StatementTree bstm = blkStms.next();
                            if (bstm.getKind() == Kind.TRY) {
                                return (TryTree)bstm;
                            }
                            if (bstm.getKind() == Kind.EXPRESSION_STATEMENT && blkStms.hasNext()) {
                                bstm = blkStms.next();
                                if (bstm.getKind() == Kind.TRY) {
                                    return (TryTree)bstm;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        return null;
    }
    
    private static TryTree findEnclosingARM(final TreePath varPath) {
        TreePath path = findEnclosingARMPath(varPath);
        return path == null ? null : (TryTree)path.getLeaf();
    }
            
    private static TreePath findEnclosingARMPath(
            final TreePath varPath) {
        TreePath parent = varPath.getParentPath();
        if (parent == null || parent.getLeaf().getKind() != Kind.BLOCK) {
            return null;
        }
        parent = parent.getParentPath();
        if (parent == null || parent.getLeaf().getKind() != Kind.TRY) {
            return null;
        }        
        return parent;
    }
    
    private static boolean insideARM(final HintContext ctx) {
        final TryTree enc = findEnclosingARM(ctx.getVariables().get("$var"));   //NOI18N
        return enc != null && enc.getResources() != null && !enc.getResources().isEmpty();  
    }

    private static Collection<VariableTree> findVariableDecls(
            final List<? extends StatementTree> statements,
            final Tree parent) {
        final List<VariableTree> varDecls = new LinkedList<VariableTree>();
        for (StatementTree st : statements) {
            if (st.getKind() == Tree.Kind.VARIABLE) {
                varDecls.add((VariableTree)st);
            }
        }
        return varDecls;
    }

    private static Collection<VariableTree> findVarsUsages(
            final Collection<VariableTree> vars,
            final List<? extends StatementTree> stms,
            final CompilationUnitTree cu,
            final Trees trees) {
        final Map<Element,VariableTree> elms = new HashMap<Element,VariableTree>();
        for (VariableTree var : vars) {
            final Element elm = trees.getElement(trees.getPath(cu, var));
            if (elm != null) {
                elms.put(elm,var);
            }
        }
        final Set<VariableTree> result = new HashSet<VariableTree>();
        final TreeScanner<Void,Void> scanner = new TreeScanner<Void,Void>(){
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                final Element elm = trees.getElement(trees.getPath(cu, node));
                final VariableTree var = elms.get(elm);
                if (var != null) {
                    result.add(var);
                }
                return super.visitIdentifier(node, p);
            }
        };
        scanner.scan(stms, null);
        vars.retainAll(result);
        return vars;
    }

    private static Collection<? extends TreePath> findResourceUsagesAfterClose(
            final VariableElement resource,
            final Collection<? extends TreePath> statements,
            final CompilationUnitTree cu,
            final Trees trees) {
        final List<TreePath> usages = new LinkedList<TreePath>();
        if (statements != null) {
            final TreePathScanner<List<TreePath>,List<TreePath>> scanner = new TreePathScanner<List<TreePath>, List<TreePath>>() {
                @Override
                public List<TreePath> visitIdentifier(IdentifierTree node, List<TreePath> p) {
                    final TreePath path = getCurrentPath();
                    final Element element = trees.getElement(path);
                    if (element == resource) {
                        usages.add(path);
                    }
                    return super.visitIdentifier(node, p);
                }
            };
            for (TreePath st : statements) {
                scanner.scan(st, usages);
            }
        }
        return usages;
    }

    private static boolean hasNonCleanUpUsages(
            final Collection<? extends TreePath> usages,
            final Collection<? super TreePath> cleanupStatements) {
        for (TreePath usage : usages) {
            final TreePath parentPath = usage.getParentPath();
            final Tree parent = parentPath.getLeaf();
            if (parent.getKind() != Tree.Kind.ASSIGNMENT) {
                return true;
            }
            final AssignmentTree assign = (AssignmentTree) parent;
            if (assign.getVariable() != usage.getLeaf()) {
                return true;
            }
            if (assign.getExpression().getKind() != Tree.Kind.NULL_LITERAL) {
                return true;
            }
            final TreePath parentParent = parentPath.getParentPath();
            if (parentParent.getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
                return true;
            }
            cleanupStatements.add(parentParent);
        }
        return false;
    }

    private static boolean isAssigned(
            final Element what,
            final Iterable<? extends TreePath> where,
            final Trees trees) {
        TreePathScanner<Boolean, Void> scanner = new TreePathScanner<Boolean, Void>() {
            @Override public Boolean visitAssignment(AssignmentTree node, Void p) {
                if (trees.getElement(new TreePath(getCurrentPath(), node.getVariable())) == what) {
                    return true;
                }
                return super.visitAssignment(node, p);
            }
            @Override
            public Boolean reduce(Boolean r1, Boolean r2) {
                return r1 == Boolean.TRUE || r2 == Boolean.TRUE;
            }
        };
        
        for (TreePath usage : where) {
            if (scanner.scan(usage, null) == Boolean.TRUE) {
                return true;
            }
        }
        return false;
    }

    private static boolean splitVariablesClash(
            final Collection<? extends TreePath> statementsPaths,
            final Collection<? extends TreePath> tail,
            final Trees trees) {
        if (tail == null || tail.isEmpty()) return false;
        
        List<StatementTree> statements = new ArrayList<StatementTree>(statementsPaths.size());

        for (TreePath tp : statementsPaths) {
            statements.add((StatementTree) tp.getLeaf());
        }

        final Set<VariableTree> usedAfterCloseVarDecls = new HashSet<VariableTree>(findVarsUsages(
                findVariableDecls(statements, statementsPaths.isEmpty()? null : statementsPaths.iterator().next().getParentPath().getLeaf()),
                ConvertToARMFix.<StatementTree>asList(tail),
                tail.iterator().next().getCompilationUnit(),
                trees));

        final Set<String> usedAfterCloseVarNames = new HashSet<String>();

        for (VariableTree vt : usedAfterCloseVarDecls) {
            usedAfterCloseVarNames.add(vt.getName().toString());
        }
        
        TreeScanner<Boolean, Void> scanner = new TreeScanner<Boolean, Void>() {
            @Override public Boolean visitVariable(VariableTree node, Void p) {
                if (usedAfterCloseVarNames.contains(node.getName().toString()) && !usedAfterCloseVarDecls.contains(node)) {
                    return true;
                }
                return super.visitVariable(node, p);
            }
            @Override public Boolean reduce(Boolean r1, Boolean r2) {
                return r1 == Boolean.TRUE || r2 == Boolean.TRUE;
            }
        };

        return scanner.scan(statements, null) == Boolean.TRUE;
    }

    private static boolean isSupportedSourceLevel(final FileObject file) {
        if (file == null) {
            return false;
        }
        final String sl = SourceLevelQuery.getSourceLevel(file);
        if (sl == null) {
            return false;
        }
        return JDK_17.compareTo(new SpecificationVersion(sl)) <= 0;
    }
    
    private enum NestingKind {
        NONE,
        IN,
        OUT
    }
}
