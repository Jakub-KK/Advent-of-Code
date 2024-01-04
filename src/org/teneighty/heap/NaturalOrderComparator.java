/*
 * $Id: NaturalOrderComparator.java 39 2012-12-05 03:27:56Z syenkoc@gmail.com $
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
import java.io.Serializable;

/**
 * A natural order comparator.
 * 
 * @param <T> the comparator type.
 * @author Fran Lattanzio
 * @version $Revision: 39 $
 */
public class NaturalOrderComparator<T extends Object & Comparable<? super T>>
	extends Object
	implements Comparator<T>, Serializable
{

	/**
	 * Serial version.
	 */
	private static final long serialVersionUID = 4583457L;

	/**
	 * Constructor.
	 */
	public NaturalOrderComparator()
	{
		super();
	}

	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(final T o1, final T o2)
		throws NullPointerException
	{
		if (o1 == null || o2 == null)
		{
			throw new NullPointerException();
		}

		return o1.compareTo(o2);
	}

	/**
	 * Check the specified object for equality.
	 * <p>
	 * We return <code>true</code> if other has the same type as this object and
	 * <code>false</code> otherwise. (This is only reasonable definition of
	 * semantic equality for a truly stateless class).
	 * 
	 * @param other the other object.
	 * @return <code>true</code> if <code>other</code> is of the same class as
	 *         this object; <code>false</code> otherwise.
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(final Object other)
	{
		if (other == null)
		{
			return false;
		}

		if (this == other)
		{
			return true;
		}

		return getClass().equals(other.getClass());
	}

	/**
	 * Get the hashcode inline with equals.
	 * <p>
	 * In accordance with the definition of equals, this returns a constant.
	 * 
	 * @return The hashcode.
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return 1;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getName();
	}

}
