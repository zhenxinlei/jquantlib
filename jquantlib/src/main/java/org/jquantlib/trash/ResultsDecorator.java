/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquantlib-dev@lists.sf.net>. The license is also available online at
 <http://jquantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the originating copyright notice follows below.
 */

package org.jquantlib.trash;

import org.jquantlib.pricingengines.results.Results;

// FIXME: add comments
@Deprecated
public abstract class ResultsDecorator<T extends Results> implements Results {

	private T delegate;
	
	protected ResultsDecorator(final T Results) {
		delegate = Results;
	}
	
	public void reset() /* @ReadOnly */ {
		if (delegate != null) {
			delegate.reset();
		}
	}
	
	
	//
	// protected methods
	//
	
	protected T getDelegate() {
		if (delegate == null) return null;
		return (T) delegate;
	}

	
	//
	// public methods
	//
	
	final public Object findClass(final Class klass) {
		// verify Class of this instance
		Class myClass = this.getClass();
		if (klass==myClass) return klass.cast(this);
		
		// obtain interfaces of this instance
		Class[] myInterfaces = klass.getInterfaces();
		for (Class myInterface : myInterfaces) {
			if (klass==myInterface) return klass.cast(this);
		}
		
		// try to find class using a delegate
		Object obj = getDelegate();
		if (obj instanceof ResultsDecorator) {
			ResultsDecorator decorator = (ResultsDecorator)obj;
			return decorator.findClass(klass);
		}
		
		throw new ClassCastException("Could not find class");
	}
	
}