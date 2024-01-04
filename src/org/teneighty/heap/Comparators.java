/*
 * $Id$
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
import java.util.Comparator;

/**
 * Contains various static methods for dealing with comparators.
 * 
 * @author Fran Lattanzio
 * @version $Revision$
 */
public final class Comparators
{

	/**
	 * Invert the action of the specified comparator.
	 * 
	 * @param <TCompare> The type of the comparator.
	 * @param comp The comparator.
	 * @return An inverted comparator.
	 * @throws NullPointerException If <code>code</code> is <code>null</code>.
	 */
	public <TCompare> Comparator<TCompare> invertComparator(final Comparator<TCompare> comp)
		throws NullPointerException
	{
		if (comp == null)
		{
			throw new NullPointerException("comp");
		}

		return new InvertedComparator<TCompare>(comp);
	}

	/**
	 * An inverted comparator.
	 * 
	 * @param <TCompare> the comparator type.
	 * @author Fran Lattanzio
	 * @version $Revision: 6 $
	 */
	private static final class InvertedComparator<TCompare>
		extends Object
		implements Comparator<TCompare>, Serializable
	{

		/**
		 * Serial version UID.
		 */
		private static final long serialVersionUID = 238473L;

		/**
		 * The backing comparator.
		 */
		private Comparator<TCompare> comp;

		/**
		 * Constructor.
		 * 
		 * @param comp the comparator.
		 */
		InvertedComparator(final Comparator<TCompare> comp)
		{
			super();

			// Store comparator.
			this.comp = comp;
		}

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final TCompare o1, final TCompare o2)
		{
			return comp.compare(o2, o1);
		}

		/**
		 * Compare for equality.
		 * 
		 * @param other the other object.
		 * @return true if equal.
		 * @see Object#equals(Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(final Object other)
		{
			if (other == null)
			{
				return (false);
			}

			if (other == this)
			{
				return (true);
			}

			if (other.getClass().equals(InvertedComparator.class))
			{
				InvertedComparator<TCompare> that = (InvertedComparator<TCompare>) other;
				return comp.equals(that.comp);
			}

			return false;
		}
		
		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			return comp.hashCode();
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return String.format("Inverse of %1$s", comp.toString());
		}

	}

}
