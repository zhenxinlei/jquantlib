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

package org.jquantlib.exercise;

import java.util.List;

import org.jquantlib.util.Date;

/**
 * A Bermudan option can only be exercised at a set of fixed dates.
 *
 * @todo it would be nice to have a way for making a Bermudan with
 * one exercise date equivalent to an European
 * 
 * @author Richard Gomes
 */
public class BermudanExercise extends EarlyExercise {

// XXX	
//	/**
//	 * Constructs a BermudanExercise with a list of exercise dates and the default payoff
//	 * 
//	 * @note In the very special case when the list of dates contains only one date, the BermudanExercise behaves
//	 * like an EuropeanExercise.
//	 * 
//	 * @note When there's a single expiry date, this constructor assumes that <i>there will be no payoff at expiry date</i>.
//	 * If this is not the desired behavior, use {@link BermudanExercise#BermudanExercise(List, boolean)} instead.
//	 * 
//	 * @param dates is a list of exercise dates. If the list contains only one date, a BermundanExercise behaves like an EuropeanExercise.
//	 * @throws IllegalArgumentException if the list is null or empty
//	 * 
//	 * @see EuropeanExercise
//	 * @see BermudanExercise#BermudanExercise(List, boolean)
//	 */
//	public BermudanExercise(final int[] dates) {
//		this(dates, false);
//	}
//
//	
//	/**
//	 * Constructs a BermudanExercise with a list of exercise dates and the default payoff
//	 * 
//	 * @note In the very special case when the list of dates contains only one date, the BermudanExercise behaves
//	 * like an EuropeanExercise.
//	 * 
//	 * @param dates is a list of exercise dates. If the list contains only one date, a BermundanExercise behaves like an EuropeanExercise.
//	 * @param payoffAtExpiry is <code>true</code> if payoffs are expected to happen on exercise dates
//	 * @throws IllegalArgumentException if the list is null or empty
//	 * 
//	 * @see EuropeanExercise
//	 */
//	public BermudanExercise(final Date[] dates, boolean payoffAtExpiry) {
//		super(Exercise.Type.Bermudan, payoffAtExpiry);
//		if (dates==null) throw new NullPointerException();
//		if (dates.length==0) throw new IllegalArgumentException("exercise dates is empty");
//		if (dates.length==1) {
//			super.setType(Exercise.Type.European);
//			super.setPayoffAtExpiry(false);
//		}
//		for (int i=0; i<dates.length; i++) {
//			super.dates.add(dates[i]);
//		}
//	}
	

	/**
	 * Constructs a BermudanExercise with a list of exercise dates and the default payoff
	 * 
	 * @note In the very special case when the list of dates contains only one date, the BermudanExercise behaves
	 * like an EuropeanExercise.
	 * 
	 * @note When there's a single expiry date, this constructor assumes that <i>there will be no payoff at expiry date</i>.
	 * If this is not the desired behavior, use {@link BermudanExercise#BermudanExercise(List, boolean)} instead.
	 * 
	 * @param dates is a list of exercise dates. If the list contains only one date, a BermundanExercise behaves like an EuropeanExercise.
	 * @throws IllegalArgumentException if the list is null or empty
	 * 
	 * @see EuropeanExercise
	 * @see BermudanExercise#BermudanExercise(List, boolean)
	 */
	public BermudanExercise(final Date[] dates) {
		this(dates, false);
	}

	
	/**
	 * Constructs a BermudanExercise with a list of exercise dates and the default payoff
	 * 
	 * @note In the very special case when the list of dates contains only one date, the BermudanExercise behaves
	 * like an EuropeanExercise.
	 * 
	 * @param dates is a list of exercise dates. If the list contains only one date, a BermundanExercise behaves like an EuropeanExercise.
	 * @param payoffAtExpiry is <code>true</code> if payoffs are expected to happen on exercise dates
	 * @throws IllegalArgumentException if the list is null or empty
	 * 
	 * @see EuropeanExercise
	 */
	public BermudanExercise(final Date[] dates, boolean payoffAtExpiry) {
		super(Exercise.Type.Bermudan, payoffAtExpiry);
		if (dates==null) throw new NullPointerException();
		if (dates.length==0) throw new IllegalArgumentException("exercise dates is empty");
		if (dates.length==1) {
			super.setType(Exercise.Type.European);
			super.setPayoffAtExpiry(false);
		}
		for (int i=0; i<dates.length; i++) {
			super.addDate(dates[i]);
		}
	}
	
}