/*
 * $Id: PairingHeap.java 39 2012-12-05 03:27:56Z syenkoc@gmail.com $
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A pairing heap implementation. A pairing heap is really just a tree that
 * maintains the heap invariant: Every entry has a key less than or equal to the
 * keys of its children. The pairing heap places no restrictions on the child
 * count or the heights of child trees at any point (c.f. a k-ary heap).
 * <p>
 * The tree structure is maintained in a somewhat funky way, so it warrants some
 * discussion. Every entry contains four references:
 * <ul>
 * <li>A child reference. This points to the leftmost child of the child list.</li>
 * <li>A next reference, which points to it right sibling. We maintain the
 * invariant, obviously, that sibling references have the same parent. A child
 * is the rightmost child iff <code>next</code> is <code>null</code>.</li>
 * <li>A previous reference. This reference serves a dual purpose. In the case
 * of a non-leftmost child, this points to the child to the left of itself in
 * the parent's child list. In the case of the left most child, however, we
 * maintain the invariant that <code>previous.child == this</code>. This dual
 * purposing of the previous reference is the source of some confusion, but it's
 * done with the intention of minimizing the amount of overhead needed per node
 * (and it works quite well).</li>
 * </ul>
 * <p>
 * The heap invariant is basically maintained by one method: <code>join()</code>
 * . This method takes two entries and makes the smaller the parent of the
 * larger. More precisely, the larger node becomes the leftmost child of the
 * smaller node; the nodes previously in the list are "shifted" to the right.
 * Below are brief descriptions of how each method works:
 * <ul>
 * <li>Insert works quite simply: We create a new entry for the given key value
 * pair, and link it to the root entry. Insert take only <code>O(1)</code>
 * worst-case time.</li>
 * <li>The union of two pairing heaps works quite similarly - the root of the
 * heap is the join of the two roots. It also works in <code>O(1)</code> time.</li>
 * <li>Extract min is also relatively simple, but significantly more expensive.
 * We first cut the links to the root's child. And set the root aside. Because
 * of the heap invariant, we know the next smallest entry must be in the child
 * list; unfortunately, the child list may be of size <code>O(n)</code>. We
 * essentially merge the whole child list into a new tree, at the root of which
 * will be the new smallest entry. The minimum/root is then set to the root of
 * this tree.</li>
 * <li>The decrease key method works as follows: We cut the node from the child
 * list (the only tricky part being dealing with the leftmost vs. non-leftmost
 * child stuff), update it's key, and then join it with the minimum node (with
 * the minimum becoming the smaller of the two). Decrease key take
 * <code>O(1)</code> time.</li>
 * <li>Delete is implemented atop decrease key and extract min, so also take
 * <code>O(n)</code> time. There are, of course, other ways of doing remove
 * (such as simply merging the children of the deleted entry and doing some
 * other cleanup), but this is simpler to implement and has the same complexity.
 * </li>
 * </ul>
 * <p>
 * As mentioned above, the <code>join()</code> method, and the way it merges
 * nodes together, is really the key to the whole <code>PairingHeap</code>
 * operation, so it warrants some discussion. The code here is capable of
 * performing both two-pass and multi-pass merging. By default, we use
 * multi-pass merging, simply because the unit tests have shown this to be
 * faster in more cases than two-pass merging. This is actually a different
 * result from the one obtained by real computer scientists, who did actual
 * research. As always, your mileage will vary depending on your algorithm,
 * problem size, etc. It is possible to examine the strategy used, and switch it
 * on the fly, using <code>getMergeStrategy</code> and
 * <code>setMergeStrategy</code> methods.
 * <p>
 * (In fact, there are almost a dozen different flavors of pairing heaps, many
 * of which are described in the original paper. The differences between them
 * are often small differences in the merging algorithm, but their performance,
 * both theoretically and experimentally, is basically the same. I have included
 * the two-pass and multi-pass versions here simply because they are the most
 * common. In the future, I may include other strategies as well.)
 * <p>
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
 * iteration order (for now, an Eulerian tour). This tour takes total time
 * <code>O(n)</code>, so serialization takes time <code>O(n)</code> well.
 * <code>readObject()</code> reads the tuples from the stream and re-inserts
 * them. The <code>insert()</code> method tales constant time, so
 * deserialization take <code>O(n)</code> time.
 * 
 * @param <TKey> the key type.
 * @param <TValue> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 39 $
 * @see "<i>The pairing heap: A new form of self-adjusting heap</i>, <u>Algorithmica</u>, 1, March 1986, 111-129, by M. Fredman, R. Sedgewick, R. Sleator, and R. Tarjan"
 * @see "Mark Weiss (1999) <i>Data Structures and Algorithm Analysis in Java</i>. Addison Wesley Professional"
 */
public class PairingHeap<TKey, TValue>
	extends AbstractLinkedHeap<TKey, TValue>
	implements Serializable
{

	/**
	 * Serial version hoo-ha.
	 */
	private static final long serialVersionUID = 2323423L;

	/**
	 * Comparator to use.
	 */
	private final Comparator<? super TKey> comp;

	/**
	 * The minimum entry.
	 */
	transient PairingHeapEntry<TKey, TValue> minimum;

	/**
	 * Merge strategy
	 */
	private MergeStrategy merge_type;
	
	/**
	 * The size of this heap.
	 */
	private transient int size;

	/**
	 * Heap reference.
	 */
	private transient HeapReference source;
	
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
	public PairingHeap()
	{
		this(null, MergeStrategy.MULTI);
	}

	/**
	 * Constructor.
	 * <p>
	 * The nodes of this heap will be ordered by their keys' <i>natural
	 * ordering</i>.
	 * 
	 * @param strat the merging strategy.
	 * @throws NullPointerException If <code>strat</code> is <code>null</code>.
	 */
	public PairingHeap(final MergeStrategy strat)
			throws NullPointerException
	{
		this(null, strat);
	}

	/**
	 * Constructor.
	 * 
	 * @param comp the comparator.
	 */
	public PairingHeap(final Comparator<? super TKey> comp)
	{
		this(comp, MergeStrategy.MULTI);
	}

	/**
	 * Constructor.
	 * 
	 * @param comp the comparator.
	 * @param ms the merge strategy.
	 * @throws NullPointerException If <code>ms</code> is <code>null</code>.
	 */
	public PairingHeap(final Comparator<? super TKey> comp,
			final MergeStrategy ms)
			throws NullPointerException
	{
		if (ms == null)
		{
			throw new NullPointerException();
		}

		// Store comp.
		this.comp = comp;
		source = new HeapReference(this);

		// Other stuff.
		size = 0;
		mod_count = 0;
		minimum = null;

		// Not user configurable...
		merge_type = ms;
	}

	/**
	 * Get the merge strategy.
	 * <p>
	 * This method is not specified by the {@link Heap} interface, obviously.
	 * 
	 * @return the strategy.
	 * @see #setMergeStrategy(org.teneighty.heap.PairingHeap.MergeStrategy)
	 */
	public MergeStrategy getMergeStrategy()
	{
		return merge_type;
	}

	/**
	 * Set the merge strategy.
	 * <p>
	 * This method is not specified in the {@link Heap} interface.
	 * 
	 * @param strat the new strategy.
	 * @see #getMergeStrategy()
	 */
	public void setMergeStrategy(final MergeStrategy strat)
	{
		merge_type = strat;
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
		if (e.getClass().equals(PairingHeapEntry.class) == false)
		{
			return false;
		}

		// Narrow.
		PairingHeapEntry<TKey, TValue> entry = (PairingHeapEntry<TKey, TValue>) e;

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
		PairingHeapEntry<TKey, TValue> entry = new PairingHeapEntry<TKey, TValue>(
				key, value, source);

		if (isEmpty())
		{
			// trivial case...
			minimum = entry;
		}
		else
		{
			minimum = join(minimum, entry);
			minimum.previous = null;
		}

		// Update stupid fields.
		size += 1;
		mod_count += 1;

		// Return new entry.
		return entry;
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

		if (other == this)
		{
			throw new IllegalArgumentException();
		}

		if (other.getClass().equals(PairingHeap.class))
		{
			try
			{
				// erased cast - hence we have to suppress unchecked.
				PairingHeap<TKey, TValue> ph = (PairingHeap<TKey, TValue>) other;

				// Find the new minimum.
				minimum = join(minimum, ph.minimum);

				// Update stuff.
				size += ph.size;
				mod_count += 1;

				// Retarget source pointing.
				ph.source.setHeap(this);
				ph.source = new HeapReference(ph);
			}
			finally
			{
				other.clear();
			}
		}
		else
		{
			throw new ClassCastException();
		}
	}
	
	/**
	 * Join together the specified entries together.
	 * 
	 * @param first the first entry.
	 * @param second the second entry.
	 * @return the result of linking the specified entries together, of course.
	 */
	private PairingHeapEntry<TKey, TValue> join(
			final PairingHeapEntry<TKey, TValue> first,
			final PairingHeapEntry<TKey, TValue> second)
	{
		if (second == null)
		{
			return first;
		}

		if (compare(first, second) >= 0)
		{
			// Make first the child of second.
			second.previous = first.previous;
			first.previous = second;
			first.next = second.child;
			if (first.next != null)
			{
				first.next.previous = first;
			}
			second.child = first;
			return second;
		}

		// Make second the child of first.
		second.previous = first;
		first.next = second.next;
		if (first.next != null)
		{
			first.next.previous = first;
		}

		second.next = first.child;
		if (second.next != null)
		{
			second.next.previous = second;
		}

		first.child = second;
		return first;
	}

	/**
	 * @see org.teneighty.heap.Heap#getMinimum()
	 */
	@Override
	public Entry<TKey, TValue> getMinimum()
		throws NoSuchElementException
	{
		if (isEmpty())
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
		if (isEmpty())
		{
			throw new NoSuchElementException();
		}

		PairingHeapEntry<TKey, TValue> old_min = minimum;

		// Extricate the specified entry from this heap.
		if (size == 1)
		{
			minimum = null;
		}
		else
		{
			minimum = merge(minimum.child);
			minimum.previous = null;
			minimum.next = null;
		}

		// Update lame fields.
		size -= 1;
		mod_count += 1;

		// Clear source.
		old_min.clearSourceReference();
		old_min.child = null;
		old_min.next = null;
		old_min.previous = null;

		return old_min;
	}

	/**
	 * @see org.teneighty.heap.Heap#decreaseKey(org.teneighty.heap.Heap.Entry, java.lang.Object)
	 */
	@Override
	public void decreaseKey(final Entry<TKey, TValue> e, final TKey key)
		throws IllegalArgumentException, ClassCastException,
		NullPointerException
	{
		if (holdsEntry(e) == false)
		{
			throw new IllegalArgumentException();
		}

		PairingHeapEntry<TKey, TValue> entry = (PairingHeapEntry<TKey, TValue>) e;

		// Check key.
		if (compareKeys(entry.getKey(), key) < 0)
		{
			throw new IllegalArgumentException();
		}

		// Update key.
		entry.setKey(key);

		// Re link
		relink(entry);

		// We made a change!
		mod_count += 1;
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
		PairingHeapEntry<TKey, TValue> entry = (PairingHeapEntry<TKey, TValue>) e;

		if (entry == minimum)
		{
			extractMinimum();
			return;
		}

		// Make it infinitely small.
		entry.is_infinite = true;

		// Relink to top!
		relink(entry);

		// Remove. Takes care of size stuff, mod count, all that jazz.
		extractMinimum();

		// Reset entry state.
		entry.is_infinite = false;
	}

	
	/**
	 * Relink the specified element, whose key has just been decreased.
	 * 
	 * @param entry the entry whose key have just been decreased.
	 */
	private void relink(final PairingHeapEntry<TKey, TValue> entry)
	{
		if (entry != minimum)
		{

			if (entry.next != null)
			{
				// Not rightmost. Remove from child list.
				entry.next.previous = entry.previous;
			}

			if (entry.previous.child == entry)
			{
				// It's the leftmost child.
				entry.previous.child = entry.next;
			}
			else
			{
				// It's some other lame node. Finish cut from child list.
				entry.previous.next = entry.next;
			}

			// entry is now fully cut from the child list.
			entry.next = null;

			// join with the root.
			minimum = join(entry, minimum);
			minimum.previous = null;
			minimum.next = null;
		}
	}

	/**
	 * Do a merge of the siblings of the specified node, based on the heap
	 * strategy.
	 * 
	 * @param consol the node to consolidate.
	 * @return the new root of all the siblings of <code>consol</code>.
	 */
	private PairingHeapEntry<TKey, TValue> merge(
			final PairingHeapEntry<TKey, TValue> consol)
	{
		switch (merge_type)
		{
			case TWO:
				return twoPassMerge(consol);
			case MULTI:
				return multiPassMerge(consol);
			default:
				// Umm... right...
				throw new InternalError();
		}
	}

	/**
	 * Do a two-pass merge on the siblings of the specified node.
	 * 
	 * @param consol the node to consolidate.
	 * @return the new root of all the siblings of <code>consol</code>.
	 */
	private PairingHeapEntry<TKey, TValue> twoPassMerge(
			final PairingHeapEntry<TKey, TValue> consol)
	{
		if (consol.next == null)
		{
			return (consol);
		}

		// list of entries.
		ArrayList<PairingHeapEntry<TKey, TValue>> entrylist = new ArrayList<PairingHeapEntry<TKey, TValue>>();

		// "Iterator" node.
		PairingHeapEntry<TKey, TValue> iter = consol;
		int count = 0;
		while (iter != null)
		{
			// Store in list.
			entrylist.add(iter);

			// Cut link.
			iter.previous.next = null;

			// Get next.
			iter = iter.next;
			count += 1;
		}

		// Merge going left to right.
		int index = 0;
		PairingHeapEntry<TKey, TValue> one = null;
		PairingHeapEntry<TKey, TValue> two = null;

		for (index = 0; (index + 1) < count; index += 2)
		{
			one = entrylist.get(index);
			two = entrylist.get((index + 1));

			entrylist.set(index, join(one, two));
		}

		int jindex = index - 2;

		if (jindex == (count - 3))
		{
			one = entrylist.get(jindex);
			two = entrylist.get((jindex + 2));

			entrylist.set(jindex, join(one, two));
		}

		// Merge right to left.
		while (jindex >= 2)
		{
			// grab two guys.
			one = entrylist.get(jindex);
			two = entrylist.get((jindex - 2));

			// Merge 'em
			entrylist.set((jindex - 2), join(two, one));
			jindex -= 2;
		}

		// Finit.
		return entrylist.get(0);
	}

	/**
	 * Do a multi pass merge on the siblings of the specified node.
	 * <p>
	 * I basically made this code up from a one sentence description, so it may
	 * not be 100% correct...
	 * 
	 * @param consol the node to consolidate.
	 * @return the new root of all the siblings of <code>consol</code>.
	 */
	private PairingHeapEntry<TKey, TValue> multiPassMerge(
			final PairingHeapEntry<TKey, TValue> consol)
	{
		LinkedList<PairingHeapEntry<TKey, TValue>> queue = new LinkedList<PairingHeapEntry<TKey, TValue>>();

		if (consol.next == null)
		{
			return consol;
		}

		// poor man's/hobo's/wino's interator.
		PairingHeapEntry<TKey, TValue> iter = consol;
		while (iter != null)
		{
			queue.addFirst(iter);

			// Cut link.
			iter.previous.next = null;
			iter = iter.next;
		}

		// Some nodes. And stuff.
		PairingHeapEntry<TKey, TValue> one = null;
		PairingHeapEntry<TKey, TValue> two = null;
		PairingHeapEntry<TKey, TValue> joined = null;

		// Combine adjacent trees in our "queue" - basically, we just
		// grab successive nodes off of the list, merge them together,
		// and stick them back on the end of the queue, repeating until
		// only one node is left (which is the element with the smallest
		// value among those in the queue).

		while (queue.size() > 1)
		{
			// Have to be at least two...
			one = queue.removeFirst();
			two = queue.removeFirst();

			if (one.next == null)
			{
				joined = join(one, two);
			}
			else
			{
				joined = join(two, one);
			}

			// Append to queue.
			queue.addLast(joined);
		}

		// DONE!
		return queue.removeFirst();
	}

	/**
	 * @see org.teneighty.heap.Heap#clear()
	 */
	@Override
	public void clear()
	{
		// Update happy fields.
		minimum = null;
		size = 0;
		mod_count += 1;

		// Clear source heap and recreate heap refrence.
		source.clearHeap();
		source = new HeapReference(this);
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
				// whole helluva lot we can do about that.
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
	 * This method takes time <code>O(n log n)</code> where <code>n</code> is
	 * the size this heap.
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
		int rsize = in.readInt();

		// Create new ref object.
		source = new HeapReference(this);

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
	 * @version $Revision: 39 $
	 */
	private class EntryIterator
		extends Object
		implements Iterator<Heap.Entry<TKey, TValue>>
	{

		/**
		 * The next node.
		 */
		private PairingHeapEntry<TKey, TValue> next;

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

			// Copy mod count.
			my_mod_count = PairingHeap.this.mod_count;

			// Get minimum.
			next = PairingHeap.this.minimum;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
		{
			if (my_mod_count != PairingHeap.this.mod_count)
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

			// Get eulerian successor so forth.
			PairingHeapEntry<TKey, TValue> tmp = next;
			next = getEulerianSuccessor(tmp);
			return tmp;
		}

		/**
		 * Get the successor to the specified node, in the context of taking an
		 * Eulerian tour over the heap.
		 * <p>
		 * This method takes amortized <code>O(1)</code> time but worst-case
		 * <code>O(n)</code> time. I think, but my analysis of algorithms is a
		 * wee bit rusty.
		 * 
		 * @param entry the entry for which to get the Eulerian successor.
		 * @return the Eulerian successor.
		 */
		private PairingHeapEntry<TKey, TValue> getEulerianSuccessor(
				PairingHeapEntry<TKey, TValue> entry)
		{
			if (entry.child != null)
			{
				return entry.child;
			}

			if (entry.next != null)
			{
				return entry.next;
			}

			// at this point, we have to walk back "up and to the right" across
			// the tree until we find a non-left-most node with a non-null
			// next node (or until we hit the root).

			while (true)
			{
				if (entry == PairingHeap.this.minimum)
				{
					// this is the end of the tour.
					return null;
				}

				if (entry.previous.child == entry)
				{
					// this is a leftmost node.
					if (entry.previous.next != null)
					{
						return entry.previous.next;
					}

					// walk up - previous points to the parent.
					entry = entry.previous;
				}
				else
				{
					// walk right.
					entry = entry.previous;
				}
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
	 * Pairing heap entry...
	 * 
	 * @param <TKey> the key type.
	 * @param <TValue> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 39 $
	 */
	private static final class PairingHeapEntry<TKey, TValue>
		extends AbstractLinkedHeap.AbstractLinkedHeapEntry<TKey, TValue>
		implements Serializable
	{

		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 2984L;

		/**
		 * The left child.
		 */
		transient PairingHeapEntry<TKey, TValue> child;

		/**
		 * The next sibling
		 */
		transient PairingHeapEntry<TKey, TValue> next;

		/**
		 * The previous/parent node.
		 * <p>
		 * We maintain the invariant that a node is the leftmost child iff
		 * <code>previous.child == this</code>. Otherwise, the previous
		 * pointer is used in such a way that
		 * <code>next.previous == this</code>.
		 */
		transient PairingHeapEntry<TKey, TValue> previous;

		/**
		 * Constructor.
		 * 
		 * @param key the key.
		 * @param val the value.
		 * @param source the source.
		 */
		PairingHeapEntry(final TKey key, final TValue val,
				final HeapReference source)
		{
			super(key, val, source);
		}

	}

	/**
	 * Pairing heap merge strategy enumeration.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 39 $
	 */
	public static enum MergeStrategy
	{

		/**
		 * The two-pass strategy
		 */
		TWO("Two pass merge"),

		/**
		 * Multi pass
		 */
		MULTI("Multi-pass merge");

		/**
		 * Serial verison
		 */
		private static final long serialVersionUID = 40234L;

		/**
		 * Description
		 */
		private String desc;

		/**
		 * Initializer/constructor.
		 * 
		 * @param d the description.
		 */
		private MergeStrategy(final String d)
		{
			desc = d;
		}

		/**
		 * Get the description
		 * 
		 * @return the description.
		 */
		public String getDescription()
		{
			return desc;
		}

	}

}
