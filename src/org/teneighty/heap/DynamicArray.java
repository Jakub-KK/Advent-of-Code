/*
 * $Id: DynamicArray.java 39 2012-12-05 03:27:56Z syenkoc@gmail.com $
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

import java.io.Serializable;

/**
 * A lame little helper class: Provides a real dynamic array functionality,
 * unlike the POS known as <code>Vector</code>.
 * 
 * @param <TElement> the element type.
 * @author Fran Lattanzio
 * @version $Revision: 39 $
 */
final class DynamicArray<TElement>
	extends Object
	implements Serializable
{

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 874234L;

	/**
	 * Backing array of schmutz.
	 */
	private Object[] data;

	/**
	 * Constructor.
	 * 
	 * @param cap the capacity.
	 */
	DynamicArray(final int cap)
	{
		super();

		// Create data array
		data = new Object[cap];
	}

	/**
	 * Get the capacity of this array.
	 * 
	 * @return the capacity.
	 */
	int capacity()
	{
		return data.length;
	}

	/**
	 * Ensure the specified capacity.
	 * 
	 * @param new_capacity the capacity to ensure.
	 */
	void ensureCapacity(final int new_capacity)
	{
		if (new_capacity != capacity())
		{
			// Re-alloc all the crap.
			Object[] new_data = new Object[new_capacity];

			// Copy everything, except 0th index.
			System.arraycopy(data, 1, new_data, 1, Math.min(
					new_data.length, data.length) - 1);

			// Set new stuff.
			data = new_data;
		}
	}

	/**
	 * Get the element at the specified index.
	 * 
	 * @param index the index to get.
	 * @return the element at <code>index</code>.
	 * @throws ArrayIndexOutOfBoundsException If <code>index</code> is out of
	 *             bounds.
	 */
	@SuppressWarnings("unchecked")
	TElement get(final int index)
		throws ArrayIndexOutOfBoundsException
	{
		return (TElement) data[index];
	}

	/**
	 * Set the value at the specified index.
	 * 
	 * @param index the index.
	 * @param val the new value.
	 * @throws ArrayIndexOutOfBoundsException If <code>index</code> is out of
	 *             bounds.
	 */
	void set(final int index, final TElement val)
		throws ArrayIndexOutOfBoundsException
	{
		data[index] = val;
	}

	/**
	 * Clear this object.
	 * <p>
	 * Simply re-allocs the backing array.
	 * 
	 * @param cap the new capacity.
	 */
	void reallocate(final int cap)
	{
		data = new Object[cap];
	}

}
