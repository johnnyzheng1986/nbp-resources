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

package org.netbeans.modules.jumpto.common;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.annotations.common.NullUnknown;
import org.netbeans.spi.jumpto.support.AsyncDescriptor;
import org.netbeans.spi.jumpto.support.DescriptorChangeEvent;
import org.netbeans.spi.jumpto.support.DescriptorChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Pair;

/**
 * (copied from org.netbeans.modules.java.source.util.Models
 * @author Petr Hrebejk, Tomas Zezula
 */
public final class Models {

    private  Models() {
    }


    public static <T> ListModel fromList(
            @NonNull final List<? extends T> list,
            @NullAllowed final Filter<? super T> filter,
            @NullAllowed final Factory<? extends T, Pair<? extends T, ? extends T>> attrCopier) {
        return new ListListModel<>(list, filter, attrCopier);
    }

    /** Creates list model which translates the objects using a factory.
     */
    public static <T,P> ListModel translating( ListModel model, Factory<T,P> factory ) {
        return new TranslatingListModel<T,P>( model, factory );
    }

    public static <R,P> ListModel refreshable(
            @NonNull final ListModel<P> model,
            @NonNull Factory<R,Pair<P,Runnable>> convertor) {
        return new RefreshableListModel(model, convertor);
    }

    public static <T> MutableListModel<T> mutable(
            @NullAllowed final Comparator<? super T> comparator,
            @NullAllowed final Filter<? super T> filter) {
        return new MutableListModelImpl(comparator, filter);
    }

    @NonNull
    public static <T> Filter<T> chained(@NonNull final Filter<T>... filters) {
        return new ChainedFilter(filters);
    }

    // Exported types
    public interface MutableListModel<T> extends ListModel<T> {
        public void add(@NonNull Collection<? extends T> values);
        public void remove (@NonNull Collection<? extends T> values);
    }

    public interface Filter<T> {
        public boolean accept(@NonNull T item);
        public void addChangeListener(@NonNull ChangeListener listener);
        public void remmoveChangeListener(@NonNull ChangeListener listener);
    }

    // Private innerclasses ----------------------------------------------------

    private static final class ListListModel<T> extends AbstractListModel implements ChangeListener, DescriptorChangeListener<T> {

        private final List<T> list;
        private final Filter<? super T> filter;
        private final Factory<? extends T, Pair<? extends T, ? extends T>> attrCopier;
        private List<T> included;

        /** Creates a new instance of IteratorList */
        public ListListModel(
                @NonNull final List<? extends T> list,
                @NullAllowed final Filter<? super T> filter,
                @NullAllowed final Factory<? extends T, Pair<? extends T, ? extends T>> attrCopier) {
            //Defensive copy and add listenener
            this.list = new ArrayList<>(list.size());
            for (T item : list) {
                if (item instanceof AsyncDescriptor) {
                    ((AsyncDescriptor)item).addDescriptorChangeListener(this);
                }
                this.list.add(item);
            }
            this.included = this.list;
            this.filter = filter;
            if (this.filter != null) {
                this.filter.addChangeListener(this);
            }
            this.attrCopier = attrCopier;
        }

        // List implementataion ------------------------------------------------

        @Override
        public T getElementAt(int index) {
            assert SwingUtilities.isEventDispatchThread();
            return included.get( index );
        }

        @Override
        public  int getSize() {
            assert SwingUtilities.isEventDispatchThread();
            return included.size();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            filterData();
        }

        @Override
        public void descriptorChanged(@NonNull final DescriptorChangeEvent<T> event) {
            final T source  = (T) event.getSource();
            final Collection<? extends T> items = copyAttrs(source, filter(event.getReplacement()));
            ((AsyncDescriptor<T>)source).removeDescriptorChangeListener(this);
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    int listIndex = list.indexOf(source);
                    int includedIndex = included.indexOf(source);
                    if (listIndex >= 0) {
                        switch (items.size()) {
                            case 0:
                            {
                                list.remove(listIndex);
                                if (includedIndex >= 0) {
                                    if (included != list) {
                                        included.remove(includedIndex);
                                    }
                                    fireIntervalRemoved(ListListModel.this, includedIndex, includedIndex);
                                }
                                break;
                            }
                            case 1:
                            {
                                final T item = head(items);
                                list.set(listIndex, item);
                                if (includedIndex >= 0) {
                                    if (included != list) {
                                        included.set(includedIndex, item);
                                    }
                                    fireContentsChanged(ListListModel.this, includedIndex, includedIndex);
                                }
                                break;
                            }
                            default:
                            {
                                final T head = head(items);
                                final Collection<? extends T> tail = tail(items);
                                list.set(listIndex, head);
                                list.addAll(listIndex+1, tail);
                                if (includedIndex >= 0) {
                                    if (included != list) {
                                        included.set(includedIndex, head);
                                        included.addAll(includedIndex+1, tail);
                                    }
                                    fireContentsChanged(ListListModel.this, includedIndex, includedIndex);
                                    fireIntervalAdded(ListListModel.this, includedIndex+1, includedIndex+tail.size());
                                }
                            }
                        }
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeLater(r);
            }
        }

        private void filterData() {
            if (filter != null) {
                invokeInEDT( new Callable<Void>() {
                    @Override
                    public Void call() {
                        assert SwingUtilities.isEventDispatchThread();
                        final int oldSize = included.size();
                        final List<T> newIncluded = new ArrayList<>();
                        for (T item : list) {
                            if (filter.accept(item)) {
                                newIncluded.add(item);
                            }
                        }
                        included = newIncluded;
                        final int newSize = included.size();
                        fireContentsChanged(this, 0, Math.max(0,Math.min(oldSize - 1, newSize - 1)));
                        if (oldSize < newSize) {
                            fireIntervalAdded(this, oldSize, newSize - 1);
                        } else if (oldSize > newSize) {
                            fireIntervalRemoved(this, newSize, oldSize - 1);
                        }
                        return null;
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        @NullUnknown
        private T head(@NonNull final Collection<? extends T> c) {
            if (c instanceof List<?>) {
                return ((List<? extends T>) c).get(0);
            } else {
                return c.iterator().next();
            }
        }

        @SuppressWarnings("unchecked")
        @NonNull
        private Collection<? extends T> tail(@NonNull final Collection<? extends T> c) {
            if (c instanceof List<?>) {
                return ((List<? extends T>)c).subList(1, c.size());
            } else {
                final List<T> res = new ArrayList<>(c.size()-1);
                boolean add = false;
                for (T item : c) {
                    if (add) {
                        res.add(item);
                    } else {
                        add = true;
                    }
                }
                return res;
            }
        }

        @NonNull
        private Collection<? extends T> copyAttrs(
                @NonNull final T source,
                @NonNull final Collection<? extends T> c) {
            if (attrCopier != null) {
                for (T item : c) {
                    final T res = attrCopier.create(Pair.of(source, item));
                    assert res == item;
                }
            }
            return c;
        }

        @NonNull
        private Collection<? extends T> filter(@NonNull final Collection<? extends T> c) {
            if (filter == null) {
                return c;
            }
            final Collection<T> res = new ArrayList<>(c.size());
            for (T item : c) {
                if (filter.accept(item)) {
                    res.add(item);
                }
            }
            return res;
        }
    }

    private static class TranslatingListModel<T,P> implements ListModel {

        private Factory<T,P> factory;
        private ListModel listModel;


        /** Creates a new instance of IteratorList */
        public TranslatingListModel( ListModel model, Factory<T,P> factory ) {
            this.listModel = model;
            this.factory = factory;
        }

        // List implementataion ----------------------------------------------------

        //@SuppressWarnings("xlint")
        public T getElementAt(int index) {
            @SuppressWarnings("unchecked")
            P original = (P)listModel.getElementAt( index );
            return factory.create( original );
        }

        public int getSize() {
            return listModel.getSize();
        }

        public void removeListDataListener(javax.swing.event.ListDataListener l) {
            // Does nothing - unmodifiable
        }

        public void addListDataListener(javax.swing.event.ListDataListener l) {
            // Does nothing - unmodifiable
        }


    }

    private static final class RefreshableListModel<R,P> extends AbstractListModel implements ListDataListener {

        private final ListModel delegate;
        private final Factory<R,Pair<P,Runnable>> convertor;
        private final Map<P,R> cache;

        RefreshableListModel(
                @NonNull final ListModel delegate,
                @NonNull final Factory<R,Pair<P,Runnable>> convertor) {
            this.delegate = delegate;
            this.convertor = convertor;
            this.cache = new IdentityHashMap<>();
            delegate.addListDataListener(this);
        }

        @Override
        public int getSize() {
            return delegate.getSize();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object getElementAt(final int index) {
            if (index < 0 || index >= delegate.getSize()) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid index: %d, model size: %d.",    //NOI18M
                        index,
                        delegate.getSize()));
            }
            final P orig = (P) delegate.getElementAt(index);
            R result = cache.get(orig);
            if (result != null) {
                return result;
            }
            result = convertor.create(Pair.<P,Runnable>of(
                orig,
                new Runnable() {
                    @Override
                    public void run() {
                        int index = -1;
                        for (int i = 0; i < delegate.getSize(); i++) {
                            if (orig == delegate.getElementAt(i)) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            fireContentsChanged(RefreshableListModel.this, index, index);
                        }
                    }
                }));
            cache.put(orig,result);
            return result;
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            fireContentsChanged(this, e.getIndex0(), e.getIndex1());
        }
    }

    private static final class MutableListModelImpl<T> extends AbstractListModel<T> implements MutableListModel<T>, ChangeListener {

        private final Comparator<T> comparator;
        private final Filter<T> filter;
        private List<T> items;
        private List<T> included;

        MutableListModelImpl(
                @NullAllowed final Comparator<T> comparator,
                @NullAllowed final Filter<T> filter) {
            this.comparator = comparator;
            this.filter = filter;
            items = included = Collections.<T>emptyList();
            if (this.comparator instanceof StateFullComparator) {
                ((StateFullComparator)this.comparator).addChangeListener(this);
            }
            if (this.filter != null) {
                this.filter.addChangeListener(this);
            }
        }

        @Override
        public int getSize() {
            assert SwingUtilities.isEventDispatchThread();
            return included.size();
        }

        @Override
        public T getElementAt(int index) {
            assert SwingUtilities.isEventDispatchThread();
            return included.get(index);
        }

        @Override
        public void add(Collection<? extends T> values) {
            boolean success;
            do {
                final Pair<List<T>,List<T>> data = getData();
                data.second().addAll(values);
                if (comparator != null) {
                    Collections.sort(data.second(), comparator);
                }
                success = casData(data.first(), data.second());
            } while (!success);
        }

        @Override
        public void remove(Collection<? extends T> values) {
            boolean success;
            do {
                final Pair<List<T>,List<T>> data = getData();
                data.second().removeAll(values);
                success = casData(data.first(), data.second());
            } while (!success);
        }

        @Override
        public void stateChanged(@NonNull final ChangeEvent e) {
            final Object source = e.getSource();
            if (source == this.comparator) {
                add(Collections.<T>emptyList());
            } else if (source == this.filter) {
                filterData();
            }
        }

        private Pair<List<T>,List<T>> getData() {
            return invokeInEDT(new Callable<Pair<List<T>,List<T>>>() {
                @Override
                public Pair<List<T>, List<T>> call() throws Exception {
                    assert SwingUtilities.isEventDispatchThread();
                    final List<T> copy = new ArrayList<>(items);
                    return Pair.<List<T>,List<T>>of(items, copy);
                }
            });
        }

        private boolean casData(final List<T> expected, final List<T> update) {
            return invokeInEDT(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    assert SwingUtilities.isEventDispatchThread();
                    if (items == expected) {
                        int oldSize = items.size();
                        items = included = update;
                        int newSize = items.size();
                        fireContentsChanged(this, 0, Math.max(0, Math.min(oldSize - 1, newSize - 1)));
                        if (oldSize < newSize) {
                            fireIntervalAdded(this, oldSize, newSize - 1);
                        } else if (oldSize > newSize) {
                            fireIntervalRemoved(this, newSize, oldSize - 1);
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        private void filterData() {
            if (filter != null) {
                invokeInEDT(new Callable<Void>() {
                    @Override
                    public Void call() {
                        assert SwingUtilities.isEventDispatchThread();
                        final int oldSize = included.size();
                        List<T> newIncluded = new ArrayList<>(items.size());
                        for (T item : items) {
                            if (filter.accept(item)) {
                                newIncluded.add(item);
                            }
                        }
                        included = newIncluded;
                        final int newSize = included.size();
                        fireContentsChanged(this, 0, Math.max(0, Math.min(oldSize - 1, newSize - 1)));
                        if (oldSize < newSize) {
                            fireIntervalAdded(this, oldSize, newSize - 1);
                        } else if (oldSize > newSize) {
                            fireIntervalRemoved(this, newSize, oldSize - 1);
                        }
                        return null;
                    }
                });
            }
        }
    }

    private static final class ChainedFilter<T> implements Filter<T>, ChangeListener {

        private final ChangeSupport changeSupport;
        private final Collection<Filter<T>> filters;

        ChainedFilter(@NonNull final Filter<T>... filters) {
            this.changeSupport = new ChangeSupport(this);
            this.filters = Arrays.asList(filters);
            for (Filter<T> filter : this.filters) {
                filter.addChangeListener(this);
            }
        }

        @Override
        public boolean accept(@NonNull final T item) {
            for (Filter<T> filter : filters) {
                if (!filter.accept(item)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void addChangeListener(@NonNull final ChangeListener listener) {
            changeSupport.addChangeListener(listener);
        }

        @Override
        public void remmoveChangeListener(@NonNull final ChangeListener listener) {
            changeSupport.removeChangeListener(listener);
        }

        @Override
        public void stateChanged(@NonNull final ChangeEvent e) {
            changeSupport.fireChange();
        }
    }

    @NullUnknown
    private static <R> R invokeInEDT(@NonNull final Callable<R> call) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return call.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                final AtomicReference<R> res = new AtomicReference<>();
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            res.set(call.call());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                return res.get();
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
