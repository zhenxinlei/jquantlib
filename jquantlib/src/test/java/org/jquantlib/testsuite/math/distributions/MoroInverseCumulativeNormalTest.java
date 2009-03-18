/*
 Copyright (C) 2008 Richard Gomes

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

package org.jquantlib.testsuite.math.distributions;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jquantlib.math.distributions.InverseCumulativeNormal;
import org.jquantlib.math.distributions.MoroInverseCumulativeNormal;
import org.junit.Test;

/**
 * 
 * @author Dominik Holenstein
 * 
 */


public class MoroInverseCumulativeNormalTest {

	private final static Logger logger = LoggerFactory
			.getLogger(MoroInverseCumulativeNormalTest.class);

	public MoroInverseCumulativeNormalTest() {
		logger.info("\n\n::::: " + this.getClass().getSimpleName() + " :::::");
	}

	// test with values generated by quantlib's (0.8.1) moro algorithm implementation
	@Test
	public void testInverseCumulativNormal() {
		// normal values generated by quantlib - digits down to 1e-16
		double[][] normal_testvalues = { { 0.01, -2.3263478739449690 },
				{ 0.1,-1.2815515632770349 }, { 0.2, -0.84162123489799423 },
				{ 0.3, -0.52440051190665271 }, { 0.4, -0.25334710332144356 },
				{ 0.5, 0.00000000000000000 }, { 0.6, 0.25334710332144356 },
				{ 0.7, 0.52440051190665249 }, { 0.8, 0.84162123489799467 },
				{ 0.9, 1.2815515632770349 }, { 0.99, 2.3263478739449690 } };

		MoroInverseCumulativeNormal icn = new MoroInverseCumulativeNormal();

		// test the normal values
		for (int i = 0; i < normal_testvalues.length; i++) {
			double x_position = normal_testvalues[i][0];
			double tolerance = 15.0e-3;//(Math.abs(x_position)<3.01) ? 1.0e-15: 1.0e-10;

			double normal_expected = normal_testvalues[i][1];
			double computed_normal = icn.evaluate(x_position);
			if (Math.abs(normal_expected - computed_normal) > tolerance) {
				fail("x_position " + x_position + " normal_expected: "
						+ normal_expected + " normal_computed: "
						+ computed_normal);
			}
		}
	}

	@Test
	public void testExtremes() {
		double z = -40;
		double tolerance = 1.0e-15;

		InverseCumulativeNormal icn = new InverseCumulativeNormal();

		// assertEquals(0, icn.evaluate(z),tolerance); --> not JUnit 4.4 conform
		if (Math.abs(icn.evaluate(z)) > tolerance) {
			fail("z: " + z + " expected: " + 0.0 + " realized: "
					+ icn.evaluate(z));
		}

		z = -10;
		// assertEquals(0, icn.evaluate(z),tolerance); --> not JUnit 4.4 conform
		if (Math.abs(icn.evaluate(z)) > tolerance) {
			fail("z: " + z + " expected: " + 0.0 + " realized: "
					+ icn.evaluate(z));
		}

		z = 10;
		// assertEquals(1.0, icn.evaluate(z),tolerance); --> not JUnit 4.4
		// conform
		if (Math.abs(icn.evaluate(z)) > (tolerance + 1.0)) {
			fail("z: " + z + " expected: " + 1.0 + " realized: "
					+ icn.evaluate(z));
		}

		z = 40;
		// assertEquals(1.0, icn.evaluate(z),tolerance); --> not JUnit 4.4
		// conform
		if (Math.abs(icn.evaluate(z)) > (tolerance + 1.0)) {
			fail("z: " + z + " expected: " + 1.0 + " realized: "
					+ icn.evaluate(z));
		}
	}

}
