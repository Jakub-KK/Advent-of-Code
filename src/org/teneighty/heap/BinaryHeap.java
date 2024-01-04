/*
 * $Id: BinaryHeap.java 39 2012-12-05 03:27:56Z syenkoc@gmail.com $
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

import java.util.Iterator;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * A binary heap implementation. A binary heap is basically a binary tree with
 * two additional properties:
 * <ol>
 * <li>The i<sup>th</sup> node in the tree is the child of the
 * (i/2)<sup>th</sup> node.</li>
 * <li>For every node in the tree except the root, the key of the node's parent
 * is less than or equal to it own.</li>
 * </ol>
 * This structure is maintained across inserts, extract-mins, deletes, and
 * decrease-keys in the following fashion:
 * <ul>
 * <li>For inserts, the we make the new node the "last" element of the heap and
 * swap it with it's parent, if necessary, to ensure the heap property holds.
 * This is generally called bubbling or percolating the element upwards.</li>
 * <li>For a decrease key operation, we do the reverse: We percolate (or bubble)
 * the affected element down the tree (by swapping with a child) until we
 * restore the heap property.</li>
 * <li>For deletes, we remove the specified node, designate the last element of
 * the heap as replacing the removed node, and percolate it down until the heap
 * order is restored.</li>
 * <li>Extract-min is nothing more than a special case of delete, wherein the
 * node to be deleted is "well known", in that it doesn't require an externally
 * supplied reference.</li>
 * </ul>
 * <p>
 * The collection-view methods of this class are backed by iterators over the
 * heap structure which are are <i>fail-fast</i>: If the heap is structurally
 * modified at any time after the iterator is created, the iterator throws a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * <p>
 * The collection-views returned by this class do not support the
 * <code>remove</code> and <code>add</code> family of operations. This is change
 * from earlier versions, in which the iterators (and thus the collection-views)
 * did support the <code>remove</code> family of operations. The binary heap is
 * unique in that can support such operations; for other heap implementations,
 * this operation is either technically infeasible or prohibitively expensive.
 * However, just because the binary heap <i>can</i> support it, doesn't mean it
 * <i>should</i> support it. A colleague of mine made the following fine points
 * and convinced me to change it:
 * <ul>
 * <li>It breaks the encapsulation of this class. Although not exposing the
 * internal structure directly, you are able to manipulate the internal
 * structure of this object indirectly; namely, through the iterator (and thus
 * collection-views thereby backed) objects returned by this heap. This is a
 * feature (or mis-feature, depending on your point of view) of many of the Java
 * Collections classes, so it seems appropriate here.</li>
 * <li>There's a nasty degree of symmetry loss, since the collection-view
 * objects returned by a <code>BinaryHeap</code> support the <code>remove</code>
 * family of operations (i.e. those implemented atop <code>remove</code>) but
 * not the <code>add</code> family. While it would be possible to implement the
 * <code>add</code> method, it would be done in a counter-intuitive way - the
 * user would have to externally allocate a "fake" {@link Heap.Entry}.
 * Furthermore, this entry would not be added directly to the heap, but rather
 * its key and value objects would be referenced by a newly inserted node.
 * Basically, it amounts to high degree of goofiness we should probably avoid.</li>
 * <li>None of the other heap implementations support the <code>remove</code>
 * operation in their iterators or collection-view objects. For consistency's
 * sake, this class should follow the precedent.</li>
 * </ul>
 * <p>
 * This class is not synchronized (by choice). You must ensure sequential access
 * externally, or you may damage instances of this class. Damage may be subtle
 * and difficult to detect, or it may be pronounced. You can use the
 * {@link Heaps#synchronizedHeap(Heap)} to obtain synchronized instances of this
 * class.
 * <p>
 * Also, unlike other heap implementations, this class implements the
 * {@link java.lang.Cloneable} interface.
 * 
 * @param <TKey> the key type.
 * @param <TValue> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 39 $ $Date: 2012-12-05 03:27:56 +0000 (Wed, 05 Dec 2012) $
 */
public class BinaryHeap<TKey, TValue>
	extends AbstractHeap<TKey, TValue>
	implements Serializable, Cloneable
{

	/**
	 * Serialization ID.
	 */
	private static final long serialVersionUID = 3874378L;

	/**
	 * The default minimum array size (capacity) of any binary heap ({@value} ).
	 */
	private static final int DEFAULT_HEAP_CAPACITY = 16;

	/**
	 * Comparator.
	 */
	private final Comparator<? super TKey> comp;

	/**
	 * The array of entries.
	 */
	private transient DynamicArray<BinaryHeapEntry<TKey, TValue>> heap;

	/**
	 * The size of this heap.
	 */
	private transient int size;

	/**
	 * The mod count.
	 */
	volatile transient int mod_count;

	/**
	 * Recommended capacity.
	 */
	private transient int rec_capacity;

	/**
	 * The constructor.
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
	public BinaryHeap()
	{
		this(null, DEFAULT_HEAP_CAPACITY);
	}

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
	 * 
	 * @param initial_capacity the initial capacity of this heap.
	 * @throws IllegalArgumentException If <code>initial_capacity</code> &lt; 0.
	 */
	public BinaryHeap(final int initial_capacity)
			throws IllegalArgumentException
	{
		this(null, initial_capacity);
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
	 * @param comp the comparator to use. A <code>null</code> means the keys
	 *            natural ordering will be used.
	 */
	public BinaryHeap(final Comparator<? super TKey> comp)
	{
		this(comp, DEFAULT_HEAP_CAPACITY);
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
	 * @param comp the comparator to use. A <code>null</code> means the keys
	 *            natural ordering will be used.
	 * @param initial_capacity the initial capacity of this heap.
	 * @throws IllegalArgumentException If <code>initial_capacity</code> &lt; 0.
	 */
	public BinaryHeap(final Comparator<? super TKey> comp,
			final int initial_capacity)
			throws IllegalArgumentException
	{
		super();

		if (initial_capacity < 0)
		{
			throw new IllegalArgumentException("Invalid initial capacity");
		}

		// Store comp.
		this.comp = comp;

		// Make backing array.
		rec_capacity = initial_capacity + 1;
		heap = new DynamicArray<BinaryHeapEntry<TKey, TValue>>(
				rec_capacity);
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
	 * Get the capacity of this heap.
	 * <p>
	 * This method is not specified by the {@link Heap} interface.
	 * 
	 * @return the capacity.
	 */
	public int getCapacity()
	{
		return heap.capacity();
	}
	
	/**
	 * @see org.teneighty.heap.Heap#holdsEntry(org.teneighty.heap.Heap.Entry)
	 */
	@Override
	public boolean holdsEntry(final Entry<TKey, TValue> e)
		throws NullPointerException
	{
		if (e == null)
		{
			throw new NullPointerException();
		}

		if (e.getClass().equals(BinaryHeapEntry.class) == false)
		{
			return false;
		}

		// Narrow.
		BinaryHeapEntry<TKey, TValue> bhe = (BinaryHeapEntry<TKey, TValue>) e;

		if (bhe.heap_index >= heap.capacity())
		{
			return false;
		}

		if (heap.get(bhe.heap_index) != bhe)
		{
			return false;
		}

		// And it's OK!
		return true;
	}

	/**
	 * @see org.teneighty.heap.Heap#insert(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Heap.Entry<TKey, TValue> insert(final TKey key, final TValue value)
		throws ClassCastException
	{
		// Make a new node.
		BinaryHeapEntry<TKey, TValue> node = new BinaryHeapEntry<TKey, TValue>(key, value);

		if (isEmpty() == false)
		{
			// Make, throw CCE
			compare(node, heap.get(1));
		}

		// Make sure backing array is big enough.
		ensureCapacityUp();

		// Find new index for insert.
		int index = ++size;

		// Store it.
		heap.set(index, node);
		node.heap_index = index;

		// Turn this nonsense back into a heap...
		heapify(index);

		// Inc mod.
		mod_count += 1;

		// Return new node.
		return node;
	}
	
	/**
	 * Heapify on the specified index.
	 * <p>
	 * Basically taken from CLR, but modified to be iterative instead of
	 * recursive. The code is slightly more bloated, but will (theoretically)
	 * run more quickly on very large heaps.
	 * <p>
	 * This method is capable of doing both up and down percolations. Note that
	 * in many implementations, these two methods are split; however, these
	 * implementations generally conform to the <code>PriorityQueue</code>
	 * interface, in which there is no <code>delete</code> operation. Since
	 * <code>delete</code> may need percolation up or down, it's easier to
	 * simply write this method to do both...
	 * 
	 * @param index the index on which to heapify.
	 */
	private void heapify(final int index)
	{
		// Now we have to heapify. Code based on/stolen from CLR
		int left = index;
		int right = index;
		int smallest = index;
		int at_node = index;
		int parent = index;

		// A useful temporary ref.
		BinaryHeapEntry<TKey, TValue> tmp = null;

		while (true)
		{
			parent = (at_node / 2);

			if (parent >= 1
					&& compare(heap.get(parent), heap
							.get(at_node)) > 0)
			{
				// Swap' em.
				tmp = heap.get(at_node);

				// Double ditto.
				heap.set(at_node, heap.get(parent));
				heap.get(at_node).heap_index = at_node;

				// Triple ditto.
				heap.set(parent, tmp);
				tmp.heap_index = parent;

				// Continue.
				at_node = parent;
				continue;
			}

			left = at_node << 1;
			right = left + 1;
			smallest = at_node;

			// Is the left child's priority smaller than its parent's???
			if (left <= size
					&& compare(heap.get(left), heap
							.get(smallest)) < 0)
			{
				smallest = left;
			}

			if (right <= size
					&& compare(heap.get(right), heap
							.get(smallest)) < 0)
			{
				smallest = right;
			}

			if (smallest == at_node)
			{
				// D.U.N. done.
				break;
			}

			// Otherwise, swap the parent and the child of smaller priority.
			tmp = heap.get(at_node);

			// SWAP!
			heap.set(at_node, heap.get(smallest));
			heap.get(at_node).heap_index = at_node;

			heap.set(smallest, tmp);
			tmp.heap_index = smallest;

			// Keep going up the heap.
			at_node = smallest;
		}
	}
	
	/**
	 * @see org.teneighty.heap.Heap#getMinimum()
	 */
	@Override
	public Heap.Entry<TKey, TValue> getMinimum()
		throws NoSuchElementException
	{
		if (isEmpty())
		{
			throw new NoSuchElementException();
		}

		return heap.get(1);
	}

	/**
	 * @see org.teneighty.heap.Heap#extractMinimum()
	 */
	@Override
	public Heap.Entry<TKey, TValue> extractMinimum()
		throws NoSuchElementException
	{
		if (isEmpty())
		{
			throw new NoSuchElementException();
		}

		// Find current min.
		BinaryHeapEntry<TKey, TValue> min = heap.get(1);

		// Delete it.
		delete(min);

		// Return old minimum
		return min;
	}

	/**
	 * @see org.teneighty.heap.Heap#decreaseKey(org.teneighty.heap.Heap.Entry, java.lang.Object)
	 */
	@Override
	public void decreaseKey(final Heap.Entry<TKey, TValue> e, final TKey k)
		throws IllegalArgumentException, ClassCastException,
		NullPointerException
	{
		if (holdsEntry(e) == false)
		{
			throw new IllegalArgumentException();
		}

		// Narrow.
		BinaryHeapEntry<TKey, TValue> entry = (BinaryHeapEntry<TKey, TValue>) e;

		// Might throw CCE if keys are not comparable.
		if (compareKeys(k, entry.getKey()) > 0)
		{
			// New key is bigger.
			throw new IllegalArgumentException();
		}

		// Set the new key.
		entry.setKey(k);

		// percolate up.
		heapify(entry.heap_index);

		// New mod count.
		mod_count += 1;
	}

	/**
	 * @see org.teneighty.heap.Heap#delete(org.teneighty.heap.Heap.Entry)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void delete(final Entry<TKey, TValue> e)
		throws IllegalArgumentException, NullPointerException
	{
		if (holdsEntry(e) == false)
		{
			throw new IllegalArgumentException();
		}

		// Narrow.
		BinaryHeapEntry<TKey, TValue> entry = (BinaryHeapEntry) e;

		if (size == 1)
		{
			// Special case.
			heap.set(1, null);
			size = 0;
			mod_count += 1;
			return;
		}

		if (entry.heap_index == size)
		{
			// Special case...
			heap.set(entry.heap_index, null);
			size -= 1;
		}
		else
		{
			// Grab last node to replace entry.
			heap.set(entry.heap_index, heap.get(size));
			heap.get(entry.heap_index).heap_index = entry.heap_index;

			// Clear old ref.
			heap.set(size, null);

			// Dec size
			size -= 1;

			// Heapify from affected node index.
			heapify(entry.heap_index);
		}

		// Update mod count.
		mod_count += 1;

		// Shrink backing array if needed.
		ensureCapacityDown();
	}

	/**
	 * @see org.teneighty.heap.Heap#union(org.teneighty.heap.Heap)
	 */
	@Override
	public void union(final Heap<TKey, TValue> other)
		throws ClassCastException, NullPointerException, IllegalArgumentException
	{
		if (other == null)
		{
			throw new NullPointerException();
		}

		if (other == this)
		{
			throw new IllegalArgumentException();
		}

		if (other.getClass().equals(BinaryHeap.class))
		{
			BinaryHeap<TKey, TValue> that = (BinaryHeap<TKey, TValue>) other;

			try
			{
				// alloc a new array.
				int new_size = size + that.size;
				heap.ensureCapacity(new_size + 1);

				// entry to union in.
				BinaryHeapEntry<TKey, TValue> thatEntry;

				// start copying and heapifying and stuff.
				for (int index = size + 1, jindex = 1; jindex <= that.size; jindex++, index++)
				{
					// set into heap - we have to set the heap index here,
					// because heapifying may not move the entry (and hence
					// set/reset the heap index).
					thatEntry = that.heap.get(jindex);
					thatEntry.heap_index = index;

					// copy element.
					heap.set(index, thatEntry);

					// percolate/bubble/heapify or whatever you want to call it.
					// remember, this works both up and down. See, this WAS a
					// good idea!
					heapify(index);
				}

				// set dumb fields and stuff.
				size = new_size;
				mod_count += 1;
			}
			finally
			{
				// clear the other guy.
				that.clear();
			}

		}
		else
		{
			throw new ClassCastException();
		}
	}

	/**
	 * Ensure backing array is big enough.
	 */
	private void ensureCapacityUp()
	{
		int new_capacity = getCapacity();

		if (getSize() >= (getCapacity() - 1))
		{
			// Double the space.
			new_capacity = getCapacity() * 2;
		}

		// Call ensure capacity.
		heap.ensureCapacity(new_capacity);
	}

	/**
	 * Ensure backing array isn't too big.
	 */
	private void ensureCapacityDown()
	{
		int new_capacity = getCapacity();

		if (getSize() == 0)
		{
			// Special case.
			new_capacity = rec_capacity;
		}
		else if (getSize() < (getCapacity() / 2))
		{
			// Half the current capacity.
			new_capacity = getCapacity() / 2;

			// Check for minimum.
			new_capacity = (new_capacity < rec_capacity ? rec_capacity
					: new_capacity);
		}

		// Call impl.
		heap.ensureCapacity(new_capacity);
	}

	/**
	 * @see org.teneighty.heap.Heap#clear()
	 */
	@Override
	public void clear()
	{
		size = 0;
		mod_count += 1;

		// Clear backing array, entry by entry!
		int index = 1;
		while (heap.get(index) != null)
		{
			heap.set(index, null);
		}

		// Reallocate back to original size.
		heap.reallocate(rec_capacity);
	}
	
	/**
	 * @see org.teneighty.heap.Heap#iterator()
	 */
	@Override
	public Iterator<Entry<TKey, TValue>> iterator()
	{
		return new EntryIterator();
	}

	/**
	 * Create and return a shallow clone of this heap.
	 * 
	 * @return a shallow copy of this heap.
	 * @see Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone()
	{
		try
		{
			// Create via super.
			BinaryHeap<TKey, TValue> clone = (BinaryHeap<TKey, TValue>) super.clone();

			// Make a copy of the underlying "heap" structure.
			clone.heap = new DynamicArray<BinaryHeapEntry<TKey, TValue>>(getCapacity());

			// Clone the stupid nodes.
			BinaryHeapEntry<TKey, TValue> tmp = null;
			for (int index = 1; index <= getSize(); index++)
			{
				tmp = heap.get(index);
				clone.heap.set(index, (BinaryHeapEntry<TKey, TValue>) tmp.clone());
			}

			// Reset mod count. Why? Why not!
			clone.mod_count = 0;

			// Finit!
			return clone;
		}
		catch (final CloneNotSupportedException cnse)
		{
			throw (InternalError) new InternalError("BinaryHeap supports the Cloneable interface").initCause(cnse);
		}
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
		// write comparator.
		out.defaultWriteObject();

		// Write "capacity" and size.
		out.writeInt(heap.capacity());
		out.writeInt(size);
		out.writeInt(rec_capacity);

		// Write key/value pairs.
		BinaryHeapEntry<TKey, TValue> tmp = null;
		for (int index = 1; index <= getSize(); index++)
		{
			tmp = heap.get(index);
			out.writeObject(tmp.getKey());
			out.writeObject(tmp.getValue());
		}
	}

	/**
	 * Deserialize and restore this object from the specified stream.
	 * <p>
	 * This method takes time <code>O(n)</code>, where <code>n</code> is the
	 * size of the heap.
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
		in.defaultReadObject();

		// Read old heap size.
		int capacity = in.readInt();
		size = in.readInt();
		rec_capacity = in.readInt();

		// Re-alloc heap.
		heap = new DynamicArray<BinaryHeapEntry<TKey, TValue>>(capacity);

		// Read keys and values.
		BinaryHeapEntry<TKey, TValue> entry = null;
		for (int index = 1; index <= size; index++)
		{
			// Create new entry.
			entry = new BinaryHeapEntry<TKey, TValue>((TKey) in.readObject(),
					(TValue) in.readObject());

			// Restore index...
			entry.heap_index = index;

			// Store int heap.
			heap.set(index, entry);
		}
	}

	/**
	 * Binary heap iterator class.
	 * <p>
	 * Cheats a little bit and touches the dynamic array and mod count fields of
	 * the enclosing heap.
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
		 * Private count.
		 */
		private int it_count;

		/**
		 * Iterator mod count.
		 */
		private final int it_mod_count;

		/**
		 * Constructor.
		 */
		EntryIterator()
		{
			super();

			// Array iterator.
			it_count = 1;

			// Copy mod count.
			it_mod_count = BinaryHeap.this.mod_count;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
			throws ConcurrentModificationException
		{
			if (BinaryHeap.this.mod_count != it_mod_count)
			{
				throw new ConcurrentModificationException();
			}

			return (it_count <= getSize());
		}
		
		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		@SuppressWarnings("synthetic-access")
		public Heap.Entry<TKey, TValue> next()
			throws NoSuchElementException, ConcurrentModificationException
		{
			if (hasNext() == false)
			{
				throw new NoSuchElementException("The iterator is empty");
			}

			// Return and advance.
			return BinaryHeap.this.heap.get(it_count++);
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
	 * Binary heap entry. Stores the entry's index in the heapified array.
	 * <p>
	 * This class also implements the {@link java.lang.Cloneable} interface
	 * (quite unlike most other heap entry implementations.
	 * 
	 * @param <TKey> the key type.
	 * @param <TValue> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 39 $ $Date: 2009-10-29 23:54:44 -0400 (Thu, 29 Oct
	 *          2009) $
	 */
	private static final class BinaryHeapEntry<TKey, TValue>
		extends AbstractHeap.AbstractHeapEntry<TKey, TValue>
		implements Cloneable, Serializable
	{

		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 23498234L;

		/**
		 * Index of this entry in containing heap's array.
		 * <p>
		 * Package protected because the enclosing heap needs to access it. This
		 * isn't such a big deal, because this class is already
		 * <code>private</code> (c.f. <code>mod_count</code>).
		 */
		transient int heap_index;

		/**
		 * Constructor.
		 * 
		 * @param key the key.
		 * @param value the value.
		 */
		BinaryHeapEntry(final TKey key, final TValue value)
		{
			super(key, value);
		}

		/**
		 * @see Object#clone()
		 */
		@Override
		public Object clone()
		{
			try
			{
				return super.clone();
			}
			catch (final CloneNotSupportedException cnse)
			{
				throw (InternalError) new InternalError(
						"BinaryHeapEntry supports the Cloneable interface")
						.initCause(cnse);
			}
		}

	}

}
