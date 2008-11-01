/*
 Copyright (C) 2007 Richard Gomes

 This source code is release under the BSD License.
 
 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the JQuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */

package org.jquantlib.testsuite.patterns;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.jquantlib.util.TypedVisitable;
import org.jquantlib.util.TypedVisitor;
import org.jquantlib.util.Visitor;
import org.junit.Test;

public class VisitorTest {

    private final static Logger logger = Logger.getLogger(VisitorTest.class);

	public VisitorTest() {
		logger.info("\n\n::::: "+this.getClass().getSimpleName()+" :::::");
	}
	
	@Test
	public void testTypedVisitorPattern() {
		
		NumberTypedVisitable  numberTypedVisitable  = new NumberTypedVisitable();
		DoubleTypedVisitable  doubleTypedVisitable  = new DoubleTypedVisitable();
		IntegerTypedVisitable integerTypedVisitable = new IntegerTypedVisitable();
		
		NumberTypedVisitor  numberTypedVisitor  = new NumberTypedVisitor();
		DoubleTypedVisitor  doubleTypedVisitor  = new DoubleTypedVisitor();
		IntegerTypedVisitor integerTypedVisitor = new IntegerTypedVisitor();
		
		// these tests must pass
		numberTypedVisitable.accept(numberTypedVisitor);
		doubleTypedVisitable.accept(numberTypedVisitor);
		doubleTypedVisitable.accept(doubleTypedVisitor);
		integerTypedVisitable.accept(numberTypedVisitor);
		integerTypedVisitable.accept(integerTypedVisitor);

		// the following must fail
		try {
			numberTypedVisitable.accept(doubleTypedVisitor);
			fail("numberTypedVisitable.accept(doubleTypedVisitor) should fail!");
		} catch (Exception e) {
			// OK, as expected
		}

		// the following must fail
		try {
			numberTypedVisitable.accept(integerTypedVisitor);
			fail("numberTypedVisitable.accept(integerTypedVisitor) should fail");
		} catch (Exception e) {
			// OK, as expected
		}
		
		// the following must fail
		try {
			doubleTypedVisitable.accept(integerTypedVisitor);
			fail("doubleTypedVisitable.accept(integerTypedVisitor) should fail");
		} catch (Exception e) {
			// OK, as expected
		}
		
		// the following must fail
		try {
			integerTypedVisitable.accept(doubleTypedVisitor);
			fail("integerTypedVisitable.accept(doubleTypedVisitor) should fail");
		} catch (Exception e) {
			// OK, as expected
		}
		
	}
	
	private class NumberTypedVisitable implements TypedVisitable<Number> {
		@Override
		public void accept(TypedVisitor<Number> v) {
			Visitor<Number> visitor = (v!=null) ? v.getVisitor(Number.class) : null;
			if (visitor!=null) 
				visitor.visit(1.0);
			else
				throw new IllegalArgumentException("not a Number visitor");
		}
	}
	
	
	private class DoubleTypedVisitable extends NumberTypedVisitable  /* implements TypedVisitable<Double> */ {
		private double data = 5.6;
		
		@Override
		public void accept(TypedVisitor<Number> v) {
			Visitor<Number> visitor = (v!=null) ? v.getVisitor(Double.class) : null;
			if (visitor!=null) 
				visitor.visit(data);
			else
				super.accept(v);
		}
	}
	
	
	private class IntegerTypedVisitable extends NumberTypedVisitable  /* implements TypedVisitable<Double> */ {
		private int data = 3456;
		
		@Override
		public void accept(TypedVisitor<Number> v) {
			Visitor<Number> visitor = (v!=null) ? v.getVisitor(Integer.class) : null;
			if (visitor!=null) 
				visitor.visit(data);
			else
				super.accept(v);
		}
	}
	
	
	
	//
	// declare TypedVisitors and corresponding Visitors
	//
	
	private class NumberTypedVisitor implements TypedVisitor<Number> {
		@Override
		public Visitor<Number> getVisitor(Class<? extends Number> klass) {
			return (klass==Number.class) ? visitor : null;
		}
		
		//
		// composition pattern to an inner Visitor<Number>
		//
		
		private NumberVisitor visitor = new NumberVisitor();
		
		private class NumberVisitor implements Visitor<Number> {
			@Override
			public void visit(Number o) {
				logger.info("Number :: "+o);
			}
		}
	}
	
	
	private class DoubleTypedVisitor extends NumberTypedVisitor {
		@Override
		public Visitor<Number> getVisitor(Class<? extends Number> klass) {
			return (klass==Double.class) ? visitor : null;
		}
		
		//
		// composition pattern to an inner Visitor<Double>
		//
		
		private DoubleVisitor visitor = new DoubleVisitor();
		
		private class DoubleVisitor implements Visitor<Number> {
			@Override
			public void visit(Number o) {
				Double obj = (Double)o;
				logger.info("Double :: "+obj);
			}
		}
	}
	
	private class IntegerTypedVisitor extends NumberTypedVisitor {
		@Override
		public Visitor<Number> getVisitor(Class<? extends Number> klass) {
			return (klass==Integer.class) ? visitor : null;
		}
		
		//
		// composition pattern to an inner Visitor<Double>
		//
		
		private IntegerVisitor visitor = new IntegerVisitor();
		
		private class IntegerVisitor implements Visitor<Number> {
			@Override
			public void visit(Number o) {
				Integer obj = (Integer)o;
				logger.info("Integer :: "+obj);
			}
		}
	}
	
}
