/*
 * $Id: LeftistHeap.java 39 2012-12-05 03:27:56Z syenkoc@gmail.com $
 * 
 * Copyright (c) 2005-2013 Fran Lattanzio
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.teneighty.heap;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This class implements a leftist heap. A leftist heap is a binary tree which
 * maintains the heap invariant: The key of parent is always smaller than a key
 * of a child. The interesting part of a leftist heap, however, is that unlike,
 * say, binary heaps, which attempt to maintain balance, a leftist tries to be
 * very unbalanced. In fact, it is extremely unbalanced toward the left.
 * <p>
 * To make the notion of unbalancedness more precise, we introduce the notions
 * of null paths and null path lengths. If we consider some entry in a leftist
 * tree, call it <code>e</code>, a null path of <code>e</code> is a shortest
 * path to a leaf in the three. Null path length, then, is simply the length of
 * this path. In a leftist tree, we maintain the invariant for every node that
 * all null paths being by traversing the right node. (Note that for a leaf
 * node, we define the null path to be 0.)
 * <p>
 * The two methods <code>link</code> and <code>linkLeft</code> do the work of
 * maintaing both the heap invariant and the unbalanced invariant. The
 * <code>link</code> method links two (previously unrelated) nodes together,
 * such that the smaller of the two becomes the new parent. <code>link</code>
 * first finds the smaller of the two, then delegates to <code>linkLeft</code>,
 * which attempts to assign a child node as the left subtree of it's new parent.
 * If this is not possible (and we can determine this by looking at the null
 * path lengths), the child becomes part of the right subtree.
 * <p>
 * Most of the operations of this class are quite simple, and we described them
 * in brief detail:
 * <ul>
 * <li>Inserting a node involves creating a new entry (i.e. a tree of size one)
 * and linking it to the current minimum (root) of the tree, which takes
 * <code>O(log n)</code> time.</li>
 * <li>The union of two leftist trees is very similar - we simply union the link
 * the roots of the two heap together. This, again, takes <code>O(log n)</code>
 * time.</li>
 * <li>Extract min works by replacing the current root by the <code>link</code>
 * of its left and right subtrees.</li>
 * <li>For a decrease key call, we first cut target node from the tree, via the
 * <code>cut</code> method. The hard work here is finding a replacement for the
 * target node and repairing the damage done to the neighboring entries. We find
 * a replacement by linking the target node's left and right children, and
 * replacing the target node with this result. This can result in the parent
 * violating the null path length invariant, and if so we swap the left and
 * right children. Finally, we reset the target entry's key and link it to the
 * root.</li>
 * <li>Deletion is basically the same as a decrease key, except that the final
 * step is not resetting target's key and relinking it to the root, but rather
 * destroying it.</li>
 * </ul>
 * The collection-view methods of this class are backed by iterators over the
 * heap structure which are <i>fail-fast</i>: If the heap is structurally
 * modified at any time after the iterator is created, the iterator throws a
 * <code>ConcurrentModificationException</code>. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * The collection-views returned by this class do not support the
 * <code>remove()</code> operation.
 * <p>
 * This class is not synchronized (by choice). You must ensure sequential access
 * externally, or you may damage instances of this class. Damage may be subtle
 * and difficult to detect, or it may be pronounced. You can use
 * {@link org.teneighty.heap.Heaps#synchronizedHeap(Heap)} to obtain
 * synchronized instances of this class.
 * <p>
 * Like all other heaps, the serialization mechanism of this class does not
 * serialize the full tree structure to the stream. For
 * <code>writeObject()</code>, the key/value pairs are simply written out in
 * iteration order. This tour takes total time <code>O(n)</code>, so
 * serialization takes time <code>O(n)</code> well. <code>readObject()</code>
 * reads the tuples from the stream and re-inserts them. The
 * <code>insert()</code> method uses constant time, so deserialization take
 * <code>O(n log n)</code> time.
 * 
 * @param <TKey> the key type.
 * @param <TValue> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 39 $ $Date: 2012-12-05 03:27:56 +0000 (Wed, 05 Dec 2012) $
 */
public class LeftistHeap<TKey, TValue>
	extends AbstractLinkedHeap<TKey, TValue>
	implements Serializable
{

	/**
	 * The serial version nonsense.
	 */
	private static final long serialVersionUID = 574934853L;

	/**
	 * Comparator.
	 */
	private final Comparator<? super TKey> comp;

	/**
	 * The root/minimum node.
	 */
	transient LeftistHeapEntry<TKey, TValue> minimum;

	/**
	 * The size of this heap.
	 */
	private transient int size;

	/**
	 * The heap reference.
	 */
	private transient HeapReference source_heap;
	
	/**
	 * The mod count.
	 */
	transient volatile int mod_count;

	/**
	 * Constructor.
	 * <p>
	 * The nodes of this heap will be ordered by their keys' <i>natural
	 * ordering</i>.
	 * <p>
	 * The keys of all nodes inserted into the heap must implement the
	 * <code>Comparable</code> interface. Furthermore, all such keys must be
	 * <i>mutually comparable</i>:<code>k1.compareTo(k2)</code> must not throw a
	 * <code>ClassCastException</code> for any elements <code>k1</code> and
	 * <code>k2</code> in the heap.
	 */
	public LeftistHeap()
	{
		this(null);
	}

	/**
	 * Constructor.
	 * <p>
	 * The keys of all nodes inserted into the heap must be <i>mutually
	 * comparable</i> by the given <code>Comparator</code>:
	 * <code>comparator.compare(k1,k2)</code> must not throw a
	 * <code>ClassCastException</code> for any keys <code>k1</code> and
	 * <code>k2</code> in the heap.
	 * 
	 * @param comp the comparator to use. A <code>null</code> means the keys'
	 *            natural ordering will be used.
	 */
	public LeftistHeap(final Comparator<? super TKey> comp)
	{
		super();

		// Store comparator.
		this.comp = comp;

		// Create heap source reference.
		source_heap = new HeapReference(this);

		// Set fields, initially.
		minimum = null;
		size = 0;
		mod_count = 0;
	}

	/**
	 * @see org.teneighty.heap.Heap#getComparator()
	 */
	@Override
	public Comparator<? super TKey> getComparator()
	{
		return comp;
	}

	/**
	 * @see org.teneighty.heap.Heap#getSize()
	 */
	@Override
	public int getSize()
	{
		return size;
	}
	
	/**
	 * @see org.teneighty.heap.Heap#holdsEntry(org.teneighty.heap.Heap.Entry)
	 */
	@Override
	public boolean holdsEntry(final Heap.Entry<TKey, TValue> e)
		throws NullPointerException
	{
		if (e == null)
		{
			throw new NullPointerException();
		}

		// Obvious check.
		if (e.getClass().equals(LeftistHeapEntry.class) == false)
		{
			return false;
		}

		// Narrow.
		LeftistHeapEntry<TKey, TValue> entry = (LeftistHeapEntry<TKey, TValue>) e;

		// Use reference trickery.
		return entry.isContainedBy(this);
	}

	/**
	 * @see org.teneighty.heap.Heap#insert(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Entry<TKey, TValue> insert(final TKey key, final TValue value)
		throws ClassCastException, NullPointerException
	{
		LeftistHeapEntry<TKey, TValue> lhe = new LeftistHeapEntry<TKey, TValue>(
				key, value, source_heap);

		// Link new entry with current minimum.
		minimum = link(minimum, lhe);

		// Increment size, etc.
		size += 1;
		mod_count += 1;

		// Ok, done.
		return lhe;
	}

	/**
	 * Link the specified entries, returning the entry which forms the new
	 * parent
	 * of the linked nodes.
	 * <p>
	 * Either <code>e1</code> or <code>e2</code> may be <code>null</code>; if
	 * both are, this method will throw a big fat exception (and this class has
	 * a serious programming error).
	 * 
	 * @param e1 the first entry to link.
	 * @param e2 the second entry to link.
	 * @return the entry which is now the parent of tree containing both
	 *         <code>e1</code> and <code>e2</code>.
	 */
	private LeftistHeapEntry<TKey, TValue> link(
			final LeftistHeapEntry<TKey, TValue> e1,
			final LeftistHeapEntry<TKey, TValue> e2)
	{
		if (e1 == null)
		{
			// Simple case: There's nothing to which to link!
			return e2;
		}
		else if (e2 == null)
		{
			// Same as above, but different.
			return e1;
		}
		else if (compare(e1, e2) < 0)
		{
			linkLeft(e1, e2);
			return e1;
		}
		else
		{
			linkLeft(e2, e1);
			return e2;
		}
	}

	/**
	 * Link <code>newleft</code> with parent, making <code>newleft</code> the
	 * new left side of <code>parent</code> (if possible). Otherwise,
	 * <code>newleft</code> will be linked with <code>parent</code>'s right
	 * side.
	 * 
	 * @param parent the parent node.
	 * @param newleft the new left side.
	 * @throws NullPointerException If <code>parent</code> or
	 *             <code>newleft</code> are <code>null</code>.
	 */
	private void linkLeft(final LeftistHeapEntry<TKey, TValue> parent,
			final LeftistHeapEntry<TKey, TValue> newleft)
		throws NullPointerException
	{
		if (parent.left == null)
		{
			// The easy case...
			parent.left = newleft;
			newleft.parent = parent;
		}
		else
		{
			// The annoying case.

			// First, link the parent's right and the new left (which isn't so
			// left anymore).
			LeftistHeapEntry<TKey, TValue> newright = link(parent.right,
					newleft);

			// Set dumb references.
			parent.right = newright;
			newright.parent = parent;

			// Compare the null path lengths - we will want the larger null path
			// length on the left.
			if (parent.right.nullPathLength > parent.left.nullPathLength)
			{
				// Swap them!
				LeftistHeapEntry<TKey, TValue> happy = parent.right;
				parent.right = parent.left;
				parent.left = happy;
			}

			// Set the parent's null path length.
			parent.nullPathLength = (parent.right.nullPathLength + 1);
		}
	}
	
	/**
	 * @see org.teneighty.heap.Heap#union(org.teneighty.heap.Heap)
	 */
	@Override
	public void union(final Heap<TKey, TValue> other)
		throws ClassCastException, NullPointerException,
		IllegalArgumentException
	{
		if (other == null)
		{
			throw new NullPointerException();
		}

		if (this == other)
		{
			throw new IllegalArgumentException();
		}

		if (other.isEmpty())
		{
			return;
		}

		if (other.getClass().equals(LeftistHeap.class))
		{
			LeftistHeap<TKey, TValue> that = (LeftistHeap<TKey, TValue>) other;

			try
			{
				// Link the root nodes... Easy enough, right? Lame javac hack
				// here. Avert your eyes...
				minimum = link(minimum, that.minimum);

				// Update stuff.
				size += that.size;
				mod_count += 1;

				// Adopt all children.
				that.source_heap.setHeap(this);

				// New heap reference for other heap.
				that.source_heap = new HeapReference(that);
			}
			finally
			{
				// Clear the other heap...
				that.clear();
			}
		}
		else
		{
			throw new ClassCastException();
		}
	}
	
	/**
	 * @see org.teneighty.heap.Heap#getMinimum()
	 */
	@Override
	public Entry<TKey, TValue> getMinimum()
		throws NoSuchElementException
	{
		if (minimum == null)
		{
			throw new NoSuchElementException();
		}

		return minimum;
	}

	/**
	 * @see org.teneighty.heap.Heap#extractMinimum()
	 */
	@Override
	public Entry<TKey, TValue> extractMinimum()
		throws NoSuchElementException
	{
		if (minimum == null)
		{
			throw new NoSuchElementException();
		}

		// Temp pointer...
		LeftistHeapEntry<TKey, TValue> min = minimum;

		// Replace the minimum.
		minimum = link(min.left, min.right);

		if (minimum != null)
		{
			// Clear parent pointer, if necessary.
			minimum.parent = null;
		}

		// Dec size, etc.
		size -= 1;
		mod_count += 1;

		// Clear source pointers.
		min.clearSourceReference();
		min.right = null;
		min.left = null;
		min.parent = null;

		// Ok, finit.
		return min;
	}

	/**
	 * @see org.teneighty.heap.Heap#decreaseKey(org.teneighty.heap.Heap.Entry, java.lang.Object)
	 */
	@Override
	public void decreaseKey(final Heap.Entry<TKey, TValue> e, final TKey k)
		throws IllegalArgumentException, ClassCastException
	{
		// Check and cast.
		if (holdsEntry(e) == false)
		{
			throw new IllegalArgumentException();
		}

		// Narrow.
		LeftistHeapEntry<TKey, TValue> x = (LeftistHeapEntry<TKey, TValue>) e;

		// Check key... May throw class cast as well.
		if (compareKeys(k, x.getKey()) > 0)
		{
			throw new IllegalArgumentException();
		}

		if (x == minimum)
		{
			// Very easy case.
			x.setKey(k);
			return;
		}

		// Cut the node from the heap.
		cut(x);

		// Store the new key value.
		x.setKey(k);

		// Merge node with minimum.
		minimum = link(minimum, x);
	}
	
	/**
	 * @see org.teneighty.heap.Heap#delete(org.teneighty.heap.Heap.Entry)
	 */
	@Override
	public void delete(final Heap.Entry<TKey, TValue> e)
		throws IllegalArgumentException, NullPointerException
	{
		// Check and cast.
		if (holdsEntry(e) == false)
		{
			throw new IllegalArgumentException();
		}

		// Narrow.
		LeftistHeapEntry<TKey, TValue> entry = (LeftistHeapEntry<TKey, TValue>) e;

		if (entry == minimum)
		{
			// Easy case.
			extractMinimum();
			return;
		}

		// Cut the entry from this heap.
		cut(entry);

		// Dec size, etc.
		size -= 1;
		mod_count += 1;

		// Clear source reference.
		entry.clearSourceReference();
	}

	/**
	 * Cut the specified node from this heap.
	 * <p>
	 * The specified node cannot be the root.
	 * 
	 * @param entry the entry to cut.
	 */
	private void cut(final LeftistHeapEntry<TKey, TValue> entry)
	{
		// Which side are we replacing?
		boolean left = (entry.parent.left == entry);

		// Find the replacemnet.
		LeftistHeapEntry<TKey, TValue> replacement = link(entry.left,
				entry.right);

		// Definitely not null...
		LeftistHeapEntry<TKey, TValue> parent = entry.parent;

		// Actually replace.
		if (left)
		{
			parent.left = replacement;
		}
		else
		{
			parent.right = replacement;
		}

		// Set parent.
		if (replacement != null)
		{
			replacement.parent = parent;
		}

		if (parent.right != null && parent.left == null)
		{
			// Easy case - parent has no left (which must be replacement, which
			// must
			// also have been null, but why check stuff we know is true?)
			LeftistHeapEntry<TKey, TValue> happy = parent.right;
			parent.right = parent.left;
			parent.left = happy;
			parent.nullPathLength = 0;
		}
		else if (parent.right != null && parent.left != null
				&& parent.right.nullPathLength > parent.left.nullPathLength)
		{
			// Swap them!
			LeftistHeapEntry<TKey, TValue> happy = parent.right;
			parent.right = parent.left;
			parent.left = happy;
			parent.nullPathLength = (parent.right.nullPathLength + 1);
		}
		else
		{
			// Parent has no right child.
			parent.nullPathLength = 0;
		}

		// Clear the node pointers.
		entry.right = null;
		entry.left = null;
		entry.parent = null;
	}

	/**
	 * @see org.teneighty.heap.Heap#clear()
	 */
	@Override
	public void clear()
	{
		// Clear all the basic fields.
		minimum = null;
		size = 0;

		// I think this qualifies as a modification.
		mod_count += 1;

		// Clear source heap and recreate heap refrence.
		source_heap.clearHeap();
		source_heap = new HeapReference(this);
	}
	
	/**
	 * @see org.teneighty.heap.Heap#iterator()
	 */
	@Override
	public Iterator<Heap.Entry<TKey, TValue>> iterator()
	{
		return new EntryIterator();
	}
	
	/**
	 * Serialize the object to the specified output stream.
	 * <p>
	 * This method takes time <code>O(n)</code> where <code>n</code> is the size
	 * this heap.
	 * 
	 * @param out the stream to which to serialize this object.
	 * @throws IOException If this object cannot be serialized.
	 */
	private void writeObject(final ObjectOutputStream out)
		throws IOException
	{
		// Write non-transient fields.
		out.defaultWriteObject();
		
		// write the size.
		out.writeInt(size);

		// Write out all key/value pairs.
		Iterator<Heap.Entry<TKey, TValue>> it = new EntryIterator();
		Heap.Entry<TKey, TValue> et = null;
		while (it.hasNext())
		{
			try
			{
				et = it.next();

				// May result in NotSerializableExceptions, but we there's not a
				// wholehelluva lot we can do about that.
				out.writeObject(et.getKey());
				out.writeObject(et.getValue());
			}
			catch (final ConcurrentModificationException cme)
			{
				// User's fault.
				throw (IOException) new IOException(
						"Heap structure changed during serialization")
						.initCause(cme);
			}
		}
	}

	/**
	 * Deserialize the restore this object from the specified stream.
	 * <p>
	 * This method takes time <code>O(n)</code> where <code>n</code> is the size
	 * this heap.
	 * 
	 * @param in the stream from which to read data.
	 * @throws IOException If this object cannot properly read from the
	 *             specified
	 *             stream.
	 * @throws ClassNotFoundException If deserialization tries to classload an
	 *             undefined class.
	 */
	@SuppressWarnings("unchecked")
	private void readObject(final ObjectInputStream in)
		throws IOException, ClassNotFoundException
	{
		// Read non-transient fields.
		in.defaultReadObject();

		// read the size.
		int rsize = in.readInt();

		// Create new ref object.
		source_heap = new HeapReference(this);

		// Read and insert all the keys and values.
		TKey key;
		TValue value;
		for (int index = 0; index < rsize; index++)
		{
			key = (TKey) in.readObject();
			value = (TValue) in.readObject();
			insert(key, value);
		}
	}

	/**
	 * Entry iterator class.
	 * <p>
	 * This iterator does not support the <code>remove()</code> operation. Any
	 * call to <code>remove()</code> will fail with a
	 * <code>UnsupportedOperationException</code>.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 39 $ $Date: 2009-10-29 23:54:44 -0400 (Thu, 29 Oct
	 *          2009) $
	 */
	private final class EntryIterator
		extends Object
		implements Iterator<Heap.Entry<TKey, TValue>>
	{

		/**
		 * The next entry.
		 */
		private LeftistHeapEntry<TKey, TValue> next;

		/**
		 * The mod count.
		 */
		private final int my_mod_count;

		/**
		 * Constructor.
		 */
		EntryIterator()
		{
			super();

			// Pay no attention to this line.
			next = LeftistHeap.this.minimum;

			// Traverse down to leftmost.
			if (next != null)
			{
				while (next.left != null)
				{
					next = next.left;
				}
			}

			// Copy mod count.
			my_mod_count = LeftistHeap.this.mod_count;
		}
		
		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
			throws ConcurrentModificationException
		{
			if (my_mod_count != LeftistHeap.this.mod_count)
			{
				throw new ConcurrentModificationException();
			}

			return (next != null);
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Heap.Entry<TKey, TValue> next()
			throws NoSuchElementException, ConcurrentModificationException
		{
			if (hasNext() == false)
			{
				throw new NoSuchElementException();
			}

			// Get the next node.
			LeftistHeapEntry<TKey, TValue> n = next;
			next = getSuccessor(next);
			return n;
		}

		/**
		 * Returns the successor of the specified Entry, or <code>null</code> if
		 * none exists.
		 * 
		 * @param entry the entry.
		 * @return the next node or <code>null</code>.
		 */
		private LeftistHeapEntry<TKey, TValue> getSuccessor(
				final LeftistHeapEntry<TKey, TValue> entry)
		{
			if (entry == null)
			{
				return null;
			}
			else if (entry.right != null)
			{
				LeftistHeapEntry<TKey, TValue> p = entry.right;
				while (p.left != null)
				{
					p = p.left;
				}
				return p;
			}
			else
			{
				LeftistHeapEntry<TKey, TValue> p = entry.parent;
				LeftistHeapEntry<TKey, TValue> ch = entry;
				while (p != null && ch == p.right)
				{
					ch = p;
					p = p.parent;
				}

				return p;
			}
		}
		
		/**
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove()
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Leftist heap entry.
	 * 
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 39 $ $Date: 2009-10-29 23:54:44 -0400 (Thu, 29 Oct
	 *          2009) $
	 */
	private static final class LeftistHeapEntry<K, V>
		extends AbstractLinkedHeap.AbstractLinkedHeapEntry<K, V>
		implements Serializable
	{

		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 547584523L;

		/**
		 * The parent node.
		 */
		transient LeftistHeapEntry<K, V> left;

		/**
		 * The sibling node.
		 */
		transient LeftistHeapEntry<K, V> right;

		/**
		 * The parent node.
		 */
		transient LeftistHeapEntry<K, V> parent;

		/**
		 * The null path length.
		 */
		transient int nullPathLength;

		/**
		 * Constructor.
		 * 
		 * @param key the key.
		 * @param value the value.
		 * @param ref the creating containing heap.
		 */
		LeftistHeapEntry(final K key, final V value, final HeapReference ref)
		{
			super(key, value, ref);
		}

	}

}
