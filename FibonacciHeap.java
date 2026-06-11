/*
 * (C) Copyright 2014-2016, by Dimitrios Michail
 *
 * JHeaps Library
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jheaps.tree;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.jheaps.AddressableHeap;
import org.jheaps.MergeableAddressableHeap;
import org.jheaps.annotations.ConstantTime;
import org.jheaps.annotations.LogarithmicTime;

/**
 * Fibonacci heaps. The heap is sorted according to the {@linkplain Comparable
 * natural ordering} of its keys, or by a {@link Comparator} provided at heap
 * creation time, depending on which constructor is used.
 *
 * <p>
 * This implementation provides amortized O(1) time for operations that do not
 * involve deleting an element such as {@code insert}, and {@code decreaseKey}.
 * Operation {@code findMin} is worst-case O(1). Operations {@code deleteMin}
 * and {@code delete} are amortized O(log(n)). The operation {@code meld} is
 * also amortized O(1).
 * 
 * <p>
 * All the above bounds, however, assume that the user does not perform
 * cascading melds on heaps such as:
 * 
 * <pre>
 * d.meld(e);
 * c.meld(d);
 * b.meld(c);
 * a.meld(b);
 * </pre>
 * 
 * The above scenario, although efficiently supported by using union-find with
 * path compression, invalidates the claimed bounds.
 *
 * <p>
 * Note that the ordering maintained by a Fibonacci heap, like any heap, and
 * whether or not an explicit comparator is provided, must be <em>consistent
 * with {@code equals}</em> if this heap is to correctly implement the
 * {@code AddressableHeap} interface. (See {@code Comparable} or
 * {@code Comparator} for a precise definition of <em>consistent with
 * equals</em>.) This is so because the {@code AddressableHeap} interface is
 * defined in terms of the {@code equals} operation, but a Fibonacci heap
 * performs all key comparisons using its {@code compareTo} (or {@code compare})
 * method, so two keys that are deemed equal by this method are, from the
 * standpoint of the Fibonacci heap, equal. The behavior of a heap <em>is</em>
 * well-defined even if its ordering is inconsistent with {@code equals}; it
 * just fails to obey the general contract of the {@code AddressableHeap}
 * interface.
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a heap concurrently, and at least one of the threads
 * modifies the heap structurally, it <em>must</em> be synchronized externally.
 * (A structural modification is any operation that adds or deletes one or more
 * elements or changing the key of some element.) This is typically accomplished
 * by synchronizing on some object that naturally encapsulates the heap.
 *
 * @param <K>
 *            the type of keys maintained by this heap
 * @param <V>
 *            the type of values maintained by this heap
 *
 * @author Dimitrios Michail
 *
 * @see PairingHeap
 * @see CostlessMeldPairingHeap
 */
public class FibonacciHeap<K, V> implements MergeableAddressableHeap<K, V>, Serializable {

    private final static long serialVersionUID = 1;

    /**
     * Size of consolidation auxiliary array. Computed for number of elements
     * equal to {@link Long#MAX_VALUE}.
     */
    private final static int AUX_CONSOLIDATE_ARRAY_SIZE = 91;

    /**
     * The comparator used to maintain order in this heap, or null if it uses
     * the natural ordering of its keys.
     *
     * @serial
     */
    private final Comparator<? super K> comparator;

    /**
     * The root with the minimum key
     */
    private Node<K, V> minRoot;

    /**
     * Number of roots in the root list
     */
    private int roots;

    /**
     * Size of the heap
     */
    private long size;

    /**
     * Auxiliary array for consolidation
     */
    private Node<K, V>[] aux;

    /**
     * Used to reference the current heap or some other heap in case of melding,
     * so that handles remain valid even after a meld, without having to iterate
     * over them.
     * 
     * In order to avoid maintaining a full-fledged union-find data structure,
     * we disallow a heap to be used in melding more than once. We use however,
     * path-compression in case of cascading melds, that it, a handle moves from
     * one heap to another and then another.
     */
    protected FibonacciHeap<K, V> other;

    /**
     * Constructs a new, empty heap, using the natural ordering of its keys. All
     * keys inserted into the heap must implement the {@link Comparable}
     * interface. Furthermore, all such keys must be <em>mutually
     * comparable</em>: {@code k1.compareTo(k2)} must not throw a
     * {@code ClassCastException} for any keys {@code k1} and {@code k2} in the
     * heap. If the user attempts to put a key into the heap that violates this
     * constraint (for example, the user attempts to put a string key into a
     * heap whose keys are integers), the {@code insert(Object key)} call will
     * throw a {@code ClassCastException}.
     */
    @ConstantTime
    public FibonacciHeap() {
        this(null);
    }

    /**
     * Constructs a new, empty heap, ordered according to the given comparator.
     * All keys inserted into the heap must be <em>mutually comparable</em> by
     * the given comparator: {@code comparator.compare(k1,
     * k2)} must not throw a {@code ClassCastException} for any keys {@code k1}
     * and {@code k2} in the heap. If the user attempts to put a key into the
     * heap that violates this constraint, the {@code insert(Object key)} call
     * will throw a {@code ClassCastException}.
     *
     * @param comparator
     *            the comparator that will be used to order this heap. If
     *            {@code null}, the {@linkplain Comparable natural ordering} of
     *            the keys will be used.
     */
    @ConstantTime
    @SuppressWarnings("unchecked")
    public FibonacciHeap(Comparator<? super K> comparator) {
        this.minRoot = null;
        this.roots = 0;
        this.comparator = comparator;
        this.size = 0;
        this.aux = (Node<K, V>[]) Array.newInstance(Node.class, AUX_CONSOLIDATE_ARRAY_SIZE);
        this.other = this;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException
     *             if the heap has already been used in the right hand side of a
     *             meld
     */
    @Override
    @ConstantTime(amortized = true)
    public AddressableHeap.Handle<K, V> insert(K key, V value) {
        if (other != this) {
            throw new IllegalStateException("A heap cannot be used after a meld");
        }
        if (key == null) {
            throw new NullPointerException("Null keys not permitted");
        }
        Node<K, V> n = new Node<K, V>(this, key, value);
        addToRootList(n);
        size++;
        return n;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException
     *             if the heap has already been used in the right hand side of a
     *             meld
     */
    @Override
    @ConstantTime(amortized = true)
    public AddressableHeap.Handle<K, V> insert(K key) {
        return insert(key, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ConstantTime(amortized = true)
    public AddressableHeap.Handle<K, V> findMin() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return minRoot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @LogarithmicTime(amortized = true)
    public AddressableHeap.Handle<K, V> deleteMin() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        Node<K, V> z = minRoot;

        // move z children into root list
        Node<K, V> x = z.child;
        while (x != null) {
            Node<K, V> nextX = (x.next == x) ? null : x.next;

            // clear parent
            x.parent = null;

            // remove from child list
            x.prev.next = x.next;
            x.next.prev = x.prev;

            // add to root list
            x.next = minRoot.next;
            x.prev = minRoot;
            minRoot.next = x;
            x.next.prev = x;
            roots++;

            // advance
            x = nextX;
        }
        z.degree = 0;
        z.child = null;

        // remove z from root list
        z.prev.next = z.next;
        z.next.prev = z.prev;
        roots--;

        // decrease size
        size--;

        // update minimum root
        if (z == z.next) {
            minRoot = null;
        } else {
            minRoot = z.next;
            consolidate();
        }

        // clear other fields
        z.next = null;
        z.prev = null;

        return z;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ConstantTime
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ConstantTime
    public long size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ConstantTime
    public void clear() {
        minRoot = null;
        roots = 0;
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ConstantTime(amortized = true)
    @SuppressWarnings("unchecked")
    public void meld(MergeableAddressableHeap<K, V> other) {
        FibonacciHeap<K, V> h = (FibonacciHeap<K, V>) other;

        // check same comparator
        if (comparator != null) {
            if (h.comparator == null || !h.comparator.equals(comparator)) {
                throw new IllegalArgumentException("Cannot meld heaps using different comparators!");
            }
        } else if (h.comparator != null) {
            throw new IllegalArgumentException("Cannot meld heaps using different comparators!");
        }

        if (h.other != h) {
            throw new IllegalStateException("A heap cannot be used after a meld.");
        }

        if (size == 0) {
            // copy the other
            minRoot = h.minRoot;
        } else if (h.size != 0) {
            // concatenate root lists
            Node<K, V> h11 = minRoot;
            Node<K, V> h12 = h11.next;
            Node<K, V> h21 = h.minRoot;
            Node<K, V> h22 = h21.next;
            h11.next = h22;
            h22.prev = h11;
            h21.next = h12;
            h12.prev = h21;

            // find new minimum
            if ((comparator == null && ((Comparable<? super K>) h.minRoot.key).compareTo(minRoot.key) < 0)
                    || (comparator != null && comparator.compare(h.minRoot.key, minRoot.key) < 0)) {
                minRoot = h.minRoot;
            }
        }
        roots += h.roots;
        size += h.size;

        // clear other
        h.size = 0;
        h.minRoot = null;
        h.roots = 0;

        // take ownership
        h.other = this;
    }

    // --------------------------------------------------------------------
    static class Node<K, V> implements AddressableHeap.Handle<K, V>, Serializable {

        private final static long serialVersionUID = 1;

        /*
         * We maintain explicitly the belonging heap, instead of using an inner
         * class due to possible cascading melding.
         */
        FibonacciHeap<K, V> heap;

        K key;
        V value;
        Node<K, V> parent; // parent
        Node<K, V> child; // any child
        Node<K, V> next; // younger sibling
        Node<K, V> prev; // older sibling
        int degree; // number of children
        boolean mark; // marked or not

        Node(FibonacciHeap<K, V> heap, K key, V value) {
            this.heap = heap;
            this.key = key;
            this.value = value;
            this.parent = null;
            this.child = null;
            this.next = null;
            this.prev = null;
            this.degree = 0;
            this.mark = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(V value) {
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @ConstantTime(amortized = true)
        public void decreaseKey(K newKey) {
            FibonacciHeap<K, V> h = getOwner();
            if (h.comparator == null) {
                h.decreaseKey(this, newKey);
            } else {
                h.decreaseKeyWithComparator(this, newKey);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @LogarithmicTime(amortized = true)
        public void delete() {
            if (this.next == null) {
                throw new IllegalArgumentException("Invalid handle!");
            }
            FibonacciHeap<K, V> h = getOwner();
            h.forceDecreaseKeyToMinimum(this);
            h.deleteMin();
        }

        /*
         * Get the owner heap of the handle. This is union-find with
         * path-compression between heaps.
         */
        FibonacciHeap<K, V> getOwner() {
            if (heap.other != heap) {
                // find root
                FibonacciHeap<K, V> root = heap;
                while (root != root.other) {
                    root = root.other;
                }
                // path-compression
                FibonacciHeap<K, V> cur = heap;
                while (cur.other != root) {
                    FibonacciHeap<K, V> next = cur.other;
                    cur.other = root;
                    cur = next;
                }
                heap = root;
            }
            return heap;
        }

    }

    /*
     * Decrease the key of a node.
     */
    @SuppressWarnings("unchecked")
    private void decreaseKey(Node<K, V> n, K newKey) {
        int c = ((Comparable<? super K>) newKey).compareTo(n.key);
        if (c > 0) {
            throw new IllegalArgumentException("Keys can only be decreased!");
        }
        n.key = newKey;
        if (c == 0) {
            return;
        }

        if (n.next == null) {
            throw new IllegalArgumentException("Invalid handle!");
        }

        // if not root and heap order violation
        Node<K, V> y = n.parent;
        if (y != null && ((Comparable<? super K>) n.key).compareTo(y.key) < 0) {
            cut(n, y);
            cascadingCut(y);
        }

        // update minimum root
        if (((Comparable<? super K>) n.key).compareTo(minRoot.key) < 0) {
            minRoot = n;
        }
    }

    /*
     * Decrease the key of a node.
     */
    private void decreaseKeyWithComparator(Node<K, V> n, K newKey) {
        int c = comparator.compare(newKey, n.key);
        if (c > 0) {
            throw new IllegalArgumentException("Keys can only be decreased!");
        }
        n.key = newKey;
        if (c == 0) {
            return;
        }

        if (n.next == null) {
            throw new IllegalArgumentException("Invalid handle!");
        }

        // if not root and heap order violation
        Node<K, V> y = n.parent;
        if (y != null && comparator.compare(n.key, y.key) < 0) {
            cut(n, y);
            cascadingCut(y);
        }

        // update minimum root
        if (comparator.compare(n.key, minRoot.key) < 0) {
            minRoot = n;
        }
    }

    /*
     * Decrease the key of a node to the minimum. Helper function for performing
     * a delete operation. Does not change the node's actual key, but behaves as
     * the key is the minimum key in the heap.
     */
    private void forceDecreaseKeyToMinimum(Node<K, V> n) {
        // if not root
        Node<K, V> y = n.parent;
        if (y != null) {
            cut(n, y);
            cascadingCut(y);
        }
        minRoot = n;
    }

    /*
     * Consolidate: Make sure each root tree has a distinct degree.
     */
    @SuppressWarnings("unchecked")
    private void consolidate() {
        int maxDegree = -1;

        // for each node in root list
        int numRoots = roots;
        Node<K, V> x = minRoot;
        while (numRoots > 0) {
            Node<K, V> nextX = x.next;
            int d = x.degree;

            while (true) {
                Node<K, V> y = aux[d];
                if (y == null) {
                    break;
                }

                // make sure x's key is smaller
                int c;
                if (comparator == null) {
                    c = ((Comparable<? super K>) y.key).compareTo(x.key);
                } else {
                    c = comparator.compare(y.key, x.key);
                }
                if (c < 0) {
                    Node<K, V> tmp = x;
                    x = y;
                    y = tmp;
                }

                // make y a child of x
                link(y, x);

                aux[d] = null;
                d++;
            }

            // store result
            aux[d] = x;

            // keep track of max degree
            if (d > maxDegree) {
                maxDegree = d;
            }

            // advance
            x = nextX;
            numRoots--;
        }

        // recreate root list and find minimum root
        minRoot = null;
        roots = 0;
        for (int i = 0; i <= maxDegree; i++) {
            if (aux[i] != null) {
                addToRootList(aux[i]);
                aux[i] = null;
            }
        }
    }

    /*
     * Remove node y from the root list and make it a child of x. Degree of x
     * increases by 1 and y is unmarked if marked.
     */
    private void link(Node<K, V> y, Node<K, V> x) {
        // remove from root list
        y.prev.next = y.next;
        y.next.prev = y.prev;

        // one less root
        roots--;

        // clear if marked
        y.mark = false;

        // hang as x's child
        x.degree++;
        y.parent = x;

        Node<K, V> child = x.child;
        if (child == null) {
            x.child = y;
            y.next = y;
            y.prev = y;
        } else {
            y.prev = child;
            y.next = child.next;
            child.next = y;
            y.next.prev = y;
        }
    }

    /*
     * Cut the link between x and its parent y making x a root.
     */
    private void cut(Node<K, V> x, Node<K, V> y) {
        // remove x from child list of y
        x.prev.next = x.next;
        x.next.prev = x.prev;
        y.degree--;
        if (y.degree == 0) {
            y.child = null;
        } else if (y.child == x) {
            y.child = x.next;
        }

        // add x to the root list
        x.parent = null;
        addToRootList(x);

        // clear if marked
        x.mark = false;
    }

    /*
     * Cascading cut until a root or an unmarked node is found.
     */
    private void cascadingCut(Node<K, V> y) {
        Node<K, V> z;
        while ((z = y.parent) != null) {
            if (!y.mark) {
                y.mark = true;
                break;
            }
            cut(y, z);
            y = z;
        }
    }

    /*
     * Add a node to the root list and update the minimum.
     */
    @SuppressWarnings("unchecked")
    private void addToRootList(Node<K, V> n) {
        if (minRoot == null) {
            n.next = n;
            n.prev = n;
            minRoot = n;
            roots = 1;
        } else {
            n.next = minRoot.next;
            n.prev = minRoot;
            minRoot.next.prev = n;
            minRoot.next = n;

            int c;
            if (comparator == null) {
                c = ((Comparable<? super K>) n.key).compareTo(minRoot.key);
            } else {
                c = comparator.compare(n.key, minRoot.key);
            }
            if (c < 0) {
                minRoot = n;
            }
            roots++;
        }
    }
}
