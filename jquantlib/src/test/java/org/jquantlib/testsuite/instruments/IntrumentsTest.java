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

/*
 Copyright (C) 2003 RiskMap srl

 This file is part of QuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://quantlib.org/

 QuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <quantlib-dev@lists.sf.net>. The license is also available online at
 <http://quantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
*/

package org.jquantlib.testsuite.instruments;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.jquantlib.instruments.Instrument;
import org.jquantlib.instruments.Stock;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.RelinkableHandle;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.testsuite.util.Flag;
import org.junit.Test;

public class IntrumentsTest {

    private final static Logger logger = Logger.getLogger(IntrumentsTest.class);      public IntrumentsTest() {
		logger.info("\n\n::::: "+this.getClass().getSimpleName()+" :::::");
	}
	
	@Test
	public void testObservable() {

	    logger.info("Testing observability of instruments...");


	    SimpleQuote me1 = new SimpleQuote(0.0);
	    RelinkableHandle<Quote>  h = new RelinkableHandle<Quote>(me1);
	    Instrument s = new Stock(h);

	    Flag f = new Flag();
	    s.addObserver(f); //f.registerWith(s);
	    
	    s.getNPV();
	    me1.setValue(3.14);
	    if (!f.isUp())
	    	fail("Observer was not notified of instrument change");
	    
	    s.getNPV();
	    f.lower();
	    SimpleQuote me2 = new SimpleQuote(0.0);

	    h.setLink(me2);
	    if (!f.isUp())
	    	fail("Observer was not notified of instrument change");

	    f.lower();
	    s.freeze();
	    s.getNPV();
	    me2.setValue(2.71);
	    if (f.isUp())
	    	fail("Observer was notified of frozen instrument change");
	    
	    s.getNPV();
	    s.unfreeze();
	    if (!f.isUp())
	    	fail("Observer was not notified of instrument change");
	}

}
