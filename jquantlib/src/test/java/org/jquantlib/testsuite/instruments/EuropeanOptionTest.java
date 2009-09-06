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
 Copyright (C) 2003, 2007 Ferdinando Ametrano
 Copyright (C) 2003, 2007 StatPro Italia srl

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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jquantlib.Configuration;
import org.jquantlib.QL;
import org.jquantlib.Settings;
import org.jquantlib.daycounters.Actual360;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.EuropeanExercise;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.AssetOrNothingPayoff;
import org.jquantlib.instruments.CashOrNothingPayoff;
import org.jquantlib.instruments.EuropeanOption;
import org.jquantlib.instruments.GapPayoff;
import org.jquantlib.instruments.Option;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.instruments.StrikedTypePayoff;
import org.jquantlib.instruments.VanillaOption;
import org.jquantlib.instruments.Option.Type;
import org.jquantlib.lang.annotation.NonNegative;
import org.jquantlib.methods.lattices.AdditiveEQPBinomialTree;
import org.jquantlib.methods.lattices.CoxRossRubinstein;
import org.jquantlib.methods.lattices.JarrowRudd;
import org.jquantlib.methods.lattices.Joshi4;
import org.jquantlib.methods.lattices.LeisenReimer;
import org.jquantlib.methods.lattices.Tian;
import org.jquantlib.methods.lattices.Trigeorgis;
import org.jquantlib.pricingengines.AnalyticEuropeanEngine;
import org.jquantlib.pricingengines.PricingEngine;
import org.jquantlib.pricingengines.vanilla.BinomialVanillaEngine;
import org.jquantlib.pricingengines.vanilla.IntegralEngine;
import org.jquantlib.pricingengines.vanilla.finitedifferences.FDEuropeanEngine;
import org.jquantlib.processes.BlackScholesMertonProcess;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;
import org.jquantlib.processes.StochasticProcess;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.termstructures.BlackVolTermStructure;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.testsuite.util.Flag;
import org.jquantlib.testsuite.util.Utilities;
import org.jquantlib.time.Period;
import org.jquantlib.util.Date;
import org.jquantlib.util.StopClock;
import org.junit.Test;


/**
 * European Options test suite
 *
 * @author Richard Gomes
 */
public class EuropeanOptionTest {

    private final Settings settings;
    private final Date today;

    public EuropeanOptionTest() {
        QL.info("\n\n::::: "+this.getClass().getSimpleName()+" :::::");
        this.settings = Configuration.getSystemConfiguration(null).getGlobalSettings();
        this.today = settings.getEvaluationDate();
    }


    private static class EuropeanOptionData {
        private final Option.Type type;            // option type
        private final /*@Price*/ double strike;    // option strike price
        private final double s;                    // spot // FIXME: any specific @annotation?
        private final /*@Price*/ double  q;        // dividend
        private final /*@Rate*/ double  r;         // risk-free rate
        private final /*@Time*/ double  t;         // time to maturity
        private final /*@Volatility*/ double v;    // volatility
        private final /*@Price*/ double result;    // expected result
        private final double tol;                  // tolerance // FIXME: any specific @annotation?

        public EuropeanOptionData(
                final Option.Type type,
                /*@Price*/ final double strike,
                final double s, /*@Price*/ final double  q,
                /*@Rate*/ final double  r,
                /*@Time*/ final double  t,
                /*@Volatility*/ final double v,
                /*@Price*/ final double result,
                final double tol) {
            this.type = type;
            this.strike = strike;
            this.s = s;
            this.q = q;
            this.r = r;
            this.t = t;
            this.v = v;
            this.result = result;
            this.tol = tol;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('[');
            sb.append(type).append(", ");
            sb.append(strike).append(", ");
            sb.append(s).append(", ");
            sb.append(q).append(", ");
            sb.append(r).append(", ");
            sb.append(t).append(", ");
            sb.append(v).append(", ");
            sb.append(result).append(", ");
            sb.append(tol);
            sb.append(']');
            return sb.toString();
        }
    }

    private enum EngineType {
        Analytic,
        JR, CRR, EQP, TGEO, TIAN, LR, JOSHI,
        FiniteDifferences,
        Integral,
        PseudoMonteCarlo, QuasiMonteCarlo; }




    private VanillaOption makeOption(
            final StrikedTypePayoff payoff,
            final Exercise exercise,
            final Handle<SimpleQuote> u,
            final Handle<YieldTermStructure> q,
            final Handle<YieldTermStructure> r,
            final Handle<BlackVolTermStructure> vol,
            final EngineType engineType,
            final int binomialSteps,
            final int samples) {

        PricingEngine engine = null;
        final GeneralizedBlackScholesProcess stochProcess = new BlackScholesMertonProcess(u, q, r, vol);

        switch (engineType) {
        case Analytic:
            engine = new AnalyticEuropeanEngine();
            break;
        case JR:
            engine = new BinomialVanillaEngine<JarrowRudd>(binomialSteps) {};
            break;
        case CRR:
            engine = new BinomialVanillaEngine<CoxRossRubinstein>(binomialSteps) {};
            break;
        case EQP:
            engine = new BinomialVanillaEngine<AdditiveEQPBinomialTree>(binomialSteps) {};
            break;
        case TGEO:
            engine = new BinomialVanillaEngine<Trigeorgis>(binomialSteps) {};
            break;
        case TIAN:
            engine = new BinomialVanillaEngine<Tian>(binomialSteps) {};
            break;
        case LR:
            engine = new BinomialVanillaEngine<LeisenReimer>(binomialSteps) {};
            break;
        case JOSHI:
            engine = new BinomialVanillaEngine<Joshi4>(binomialSteps) {};
            break;
        case FiniteDifferences:
            engine = new FDEuropeanEngine(stochProcess, binomialSteps,samples);
            break;
        case Integral:
            engine = new IntegralEngine();
            break;
            //        case PseudoMonteCarlo:
            //          engine = MakeMCEuropeanEngine<PseudoRandom>().withSteps(1)
            //                                                       .withSamples(samples)
            //                                                       .withSeed(42);
            //          break;
            //        case QuasiMonteCarlo:
            //          engine = MakeMCEuropeanEngine<LowDiscrepancy>().withSteps(1)
            //                                                         .withSamples(samples);
            //          break;
        default:
            throw new UnsupportedOperationException("unknown engine type: "+engineType);
        }

        return new EuropeanOption(stochProcess, payoff, exercise, engine);
    }




    //  std::string engineTypeToString(EngineType type) {
    //      switch (type) {
    //        case Analytic:
    //          return "analytic";
    //        case JR:
    //          return "Jarrow-Rudd";
    //        case CRR:
    //          return "Cox-Ross-Rubinstein";
    //        case EQP:
    //          return "EQP";
    //        case TGEO:
    //          return "Trigeorgis";
    //        case TIAN:
    //          return "Tian";
    //        case LR:
    //          return "LeisenReimer";
    //        case JOSHI:
    //          return "Joshi";
    //        case FiniteDifferences:
    //          return "FiniteDifferences";
    //      case Integral:
    //          return "Integral";
    //        case PseudoMonteCarlo:
    //          return "MonteCarlo";
    //        case QuasiMonteCarlo:
    //          return "Quasi-MonteCarlo";
    //        default:
    //          QL_FAIL("unknown engine type");
    //      }
    //  }

    private int timeToDays(/*@Time*/ final double t) {
        return (int) (t*360+0.5);
    }


    @Test
    public void testValues() {

        QL.info("Testing European option values...");

        /**
         *  The data below are from "Option pricing formulas", E.G. Haug, McGraw-Hill 1998
         */
        final EuropeanOptionData values[] = new EuropeanOptionData[] {
                // pag 2-8
                //                              type,     strike,   spot,    q,    r,    t,  vol,   value,    tol
                new EuropeanOptionData( Option.Type.CALL,  65.00,  60.00, 0.00, 0.08, 0.25, 0.30,  2.1334, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,   95.00, 100.00, 0.05, 0.10, 0.50, 0.20,  2.4648, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,   19.00,  19.00, 0.10, 0.10, 0.75, 0.28,  1.7011, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL,  19.00,  19.00, 0.10, 0.10, 0.75, 0.28,  1.7011, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL,   1.60,   1.56, 0.08, 0.06, 0.50, 0.12,  0.0291, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,   70.00,  75.00, 0.05, 0.10, 0.50, 0.35,  4.0870, 1.0e-4),
                // pag 24
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.10, 0.15,  0.0205, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.10, 0.15,  1.8734, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.10, 0.15,  9.9413, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.10, 0.25,  0.3150, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.10, 0.25,  3.1217, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.10, 0.25, 10.3556, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.10, 0.35,  0.9474, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.10, 0.35,  4.3693, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.10, 0.35, 11.1381, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.50, 0.15,  0.8069, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.50, 0.15,  4.0232, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.50, 0.15, 10.5769, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.50, 0.25,  2.7026, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.50, 0.25,  6.6997, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.50, 0.25, 12.7857, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00,  90.00, 0.10, 0.10, 0.50, 0.35,  4.9329, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 100.00, 0.10, 0.10, 0.50, 0.35,  9.3679, 1.0e-4),
                new EuropeanOptionData( Option.Type.CALL, 100.00, 110.00, 0.10, 0.10, 0.50, 0.35, 15.3086, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.10, 0.15,  9.9210, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.10, 0.15,  1.8734, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.10, 0.15,  0.0408, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.10, 0.25, 10.2155, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.10, 0.25,  3.1217, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.10, 0.25,  0.4551, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.10, 0.35, 10.8479, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.10, 0.35,  4.3693, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.10, 0.35,  1.2376, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.50, 0.15, 10.3192, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.50, 0.15,  4.0232, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.50, 0.15,  1.0646, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.50, 0.25, 12.2149, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.50, 0.25,  6.6997, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.50, 0.25,  3.2734, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00,  90.00, 0.10, 0.10, 0.50, 0.35, 14.4452, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 100.00, 0.10, 0.10, 0.50, 0.35,  9.3679, 1.0e-4),
                new EuropeanOptionData( Option.Type.PUT,  100.00, 110.00, 0.10, 0.10, 0.50, 0.35,  5.7963, 1.0e-4),
                // pag 27
                new EuropeanOptionData( Option.Type.CALL,  40.00,  42.00, 0.08, 0.04, 0.75, 0.35,  5.0975, 1.0e-4)
        };

        final DayCounter dc = Actual360.getDayCounter();

        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, qRate, dc));
        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(today, vol, dc));
        final PricingEngine engine = new AnalyticEuropeanEngine();

		final StopClock clock = new StopClock();
        clock.reset();
        clock.startClock();

        for (int i=0; i<values.length-1; i++) {

            QL.debug(values[i].toString());

            final StrikedTypePayoff payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
            final Date exDate = today.getDateAfter( timeToDays(values[i].t) );
            final Exercise exercise = new EuropeanExercise(exDate);

            spot. getLink().setValue(values[i].s);
            qRate.getLink().setValue(values[i].q);
            rRate.getLink().setValue(values[i].r);
            vol.  getLink().setValue(values[i].v);

            final StochasticProcess process = new BlackScholesMertonProcess(spot, qTS, rTS, volTS);

            final EuropeanOption option = new EuropeanOption(process, payoff, exercise, engine);

            final double calculated = option.getNPV();
            final double error = Math.abs(calculated-values[i].result);
            final double tolerance = values[i].tol;

            final StringBuilder sb = new StringBuilder();
            sb.append("error ").append(error).append(" .gt. tolerance ").append(tolerance).append('\n');
            sb.append("    calculated ").append(calculated).append('\n');
            sb.append("    type ").append(values[i].type).append('\n');
            sb.append("    strike ").append(values[i].strike).append('\n');
            sb.append("    s ").append(values[i].s).append('\n');
            sb.append("    q ").append(values[i].q).append('\n');
            sb.append("    r ").append(values[i].r).append('\n');
            sb.append("    t ").append(values[i].t).append('\n');
            sb.append("    v ").append(values[i].v).append('\n');
            sb.append("    result ").append(values[i].result).append('\n');
            sb.append("    tol ").append(values[i].tol); // .append('\n');

            if (error<=tolerance)
                QL.info(" error="+error);
            else
                fail(exercise + " " + payoff.optionType() + " option with " + payoff + " payoff:\n"
                        + "    spot value:       " + values[i].s + "\n"
                        + "    strike:           " + payoff.strike() + "\n"
                        + "    dividend yield:   " + values[i].q + "\n"
                        + "    risk-free rate:   " + values[i].r + "\n"
                        + "    reference date:   " + today + "\n"
                        + "    maturity:         " + values[i].t + "\n"
                        + "    volatility:       " + values[i].v + "\n\n"
                        + "    expected:         " + values[i].result + "\n"
                        + "    calculated:       " + calculated + "\n"
                        + "    error:            " + error + "\n"
                        + "    tolerance:        " + tolerance);
        }
        clock.stopClock();
        clock.log();
    }

    @Test
    public void testGreekValues(){
        QL.info("Testing European option greek values...");

        //
        // The data below are from "Option pricing formulas", E.G. Haug, McGraw-Hill 1998 pag 11-16
        //

        final EuropeanOptionData values[] = {
                //        type, strike,   spot,    q,    r,        t,  vol,  value delta
                new EuropeanOptionData(Option.Type.CALL, 100.00, 105.00, 0.10, 0.10, 0.500000, 0.36,  0.5946, 0),
                new EuropeanOptionData(Option.Type.PUT,  100.00, 105.00, 0.10, 0.10, 0.500000, 0.36, -0.3566, 0),
                new EuropeanOptionData(Option.Type.PUT,100.00, 105.00, 0.10, 0.10, 0.500000, 0.36, -4.8775, 0 ),
                new EuropeanOptionData(Option.Type.CALL, 60.00,  55.00, 0.00, 0.10, 0.750000, 0.30,  0.0278, 0 ),
                new EuropeanOptionData(Option.Type.PUT, 60.00,  55.00, 0.00, 0.10, 0.750000, 0.30,  0.0278, 0 ),
                new EuropeanOptionData(Option.Type.CALL,  60.00,  55.00, 0.00, 0.10, 0.750000, 0.30, 18.9358, 0),
                new EuropeanOptionData(Option.Type.PUT,   60.00,  55.00, 0.00, 0.10, 0.750000, 0.30, 18.9358, 0 ),
                new EuropeanOptionData(Option.Type.PUT,  405.00, 430.00, 0.05, 0.07, 1.0/12.0, 0.20,-31.1924, 0),
                new EuropeanOptionData(Option.Type.PUT,  405.00, 430.00, 0.05, 0.07, 1.0/12.0, 0.20, -0.0855, 0),
                new EuropeanOptionData(Option.Type.CALL,  75.00,  72.00, 0.00, 0.09, 1.000000, 0.19, 38.7325, 0),
                new EuropeanOptionData(Option.Type.PUT,  490.00, 500.00, 0.05, 0.08, 0.250000, 0.15, 42.2254, 0)
        };

        final DayCounter dc = Actual360.getDayCounter();
        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, qRate, dc));

        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(today, vol, dc));
        final PricingEngine engine = new AnalyticEuropeanEngine();
        final Handle<StochasticProcess> stochProcess = new Handle<StochasticProcess>(new BlackScholesMertonProcess(spot, qTS, rTS, volTS));

        StrikedTypePayoff payoff;
        Date exDate;
        Exercise exercise;
        Handle<VanillaOption> option;
        double calculated;
        final double tolerance = 1e-4;
        double error;

        int i = -1;

        // testing delta 1
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);

        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().delta();
        error = Math.abs(calculated - values[i].result);

        if (error > tolerance)
            REPORT_FAILURE("delta", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing delta 2
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().delta();
        error = Math.abs(calculated - values[i].result);
        if(error>tolerance)
            REPORT_FAILURE("delta", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing elasticity
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().elasticity();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("elasticity", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        // testing gamma 1
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().gamma();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("gamma", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        // testing gamma 2
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().gamma();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("gamma", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing vega 1
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().vega();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("vega", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing vega 2
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().vega();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("vega", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing theta
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().theta();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("theta", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);


        //testing theta per day
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().thetaPerDay();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("theta per day", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);


        //testing rho
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().rho();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("rho", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);

        //testing dividend rho
        i++;
        payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);
        exDate = today.getDateAfter(timeToDays(values[i].t));
        exercise = new EuropeanExercise(exDate);
        spot.getLink().setValue(values[i].s);
        qRate.getLink().setValue(values[i].q);
        rRate.getLink().setValue(values[i].r);
        vol.getLink().setValue(values[i].v);
        option = new Handle<VanillaOption>(new EuropeanOption(stochProcess.getLink(), payoff, exercise, engine));
        calculated = option.getLink().dividendRho();
        error = Math.abs(Math.abs(calculated - values[i].result));
        if(error>tolerance)
            REPORT_FAILURE("dividend rho", payoff, exercise, values[i].s, values[i].q, values[i].r, today, values[i].v,
                    values[i].result, calculated, error, tolerance);
    }


    @Test
    public void testGreeks() {
        QL.info("Testing analytic European option greeks...");

        final Map<String,Double> tolerance = new HashMap<String, Double>();
        tolerance.put("delta",  1.0e-5);
        tolerance.put("gamma",  1.0e-5);
        tolerance.put("theta",  1.0e-5);
        tolerance.put("rho",    1.0e-5);
        tolerance.put("divRho", 1.0e-5);
        tolerance.put("vega",   1.0e-5);

        final Map<String,Double> expected = new HashMap<String, Double>();
        final Map<String,Double> calculated = new HashMap<String, Double>();

        final Option.Type types[] = { Option.Type.CALL, Option.Type.PUT };
        final double strikes[] = { 50.0, 99.5, 100.0, 100.5, 150.0 };
        final double underlyings[] = { 100.0 };
        final double qRates[] = { 0.04, 0.05, 0.06 };
        final double rRates[] = { 0.01, 0.05, 0.15 };
        final double residualTimes[] = { 1.0, 2.0 };
        final double vols[] = { 0.11, 0.50, 1.20 };

        final DayCounter dc = Actual360.getDayCounter();

        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, qRate, dc));
        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(today, vol, dc));

        StrikedTypePayoff payoff = null;

        for (final Type type : types)
            for (final double strike : strikes)
                for (final double residualTime : residualTimes) {

                    final Date exDate = today.getDateAfter( timeToDays(residualTime) ); //TODO: code review
                    final Exercise exercise = new EuropeanExercise(exDate);

                    for (int kk=0; kk<4; kk++) {
                        // option to check
                        if (kk==0)
                            payoff = new PlainVanillaPayoff(type, strike);
                        else if (kk==1)
                            //FIXME check constructor
                            payoff = new CashOrNothingPayoff(type, strike, 100);
                        else if (kk==2)
                            payoff = new AssetOrNothingPayoff(type, strike);
                        else if (kk==3)
                            payoff = new GapPayoff(type, strike, 100);

                        final StochasticProcess process = new BlackScholesMertonProcess(spot, qTS, rTS, volTS);

                        if (payoff==null) throw new IllegalArgumentException();
                        final EuropeanOption option = new EuropeanOption(process, payoff, exercise, new AnalyticEuropeanEngine());


                        for (final double u : underlyings)
                            for (final double q : qRates)
                                for (final double r : rRates)
                                    for (final double v : vols) {
                                        //something wrong here for vanilla payoff?
                                        spot.getLink().setValue(u);
                                        qRate.getLink().setValue(q);
                                        rRate.getLink().setValue(r);
                                        vol.getLink().setValue(v);

                                        final double value = option.getNPV();
                                        calculated.put("delta", option.delta());
                                        calculated.put("gamma", option.gamma());
                                        calculated.put("theta", option.theta());
                                        calculated.put("rho", option.rho());
                                        calculated.put("divRho", option.dividendRho());
                                        calculated.put("vega", option.vega());

                                        if (value > spot.getLink().op()*1.0e-5) {
                                            // perturb spot and get delta and gamma
                                            final double du = u*1.0e-4;
                                            spot.getLink().setValue(u+du);
                                            double value_p = option.getNPV();
                                            final double delta_p = option.delta();
                                            spot.getLink().setValue(u-du);

                                            double value_m = option.getNPV();
                                            final double delta_m = option.delta();
                                            spot.getLink().setValue(u);
                                            expected.put("delta", (value_p - value_m)/(2*du));
                                            expected.put("gamma", (delta_p - delta_m)/(2*du));

                                            // perturb rates and get rho and dividend rho
                                            final double dr = r*1.0e-4;
                                            rRate.getLink().setValue(r+dr);
                                            value_p = option.getNPV();
                                            rRate.getLink().setValue(r-dr);
                                            value_m = option.getNPV();
                                            rRate.getLink().setValue(r);
                                            expected.put("rho", (value_p - value_m)/(2*dr));

                                            final double dq = q*1.0e-4;
                                            qRate.getLink().setValue(q+dq);
                                            value_p = option.getNPV();
                                            qRate.getLink().setValue(q-dq);
                                            value_m = option.getNPV();
                                            qRate.getLink().setValue(q);
                                            expected.put("divRho",(value_p - value_m)/(2*dq));

                                            // perturb volatility and get vega
                                            final double dv = v*1.0e-4;
                                            vol.getLink().setValue(v+dv);
                                            value_p = option.getNPV();
                                            vol.getLink().setValue(v-dv);
                                            value_m = option.getNPV();
                                            vol.getLink().setValue(v);
                                            expected.put("vega",(value_p - value_m)/(2*dv));

                                            // perturb date and get theta
                                            final Date yesterday = today.getPreviousDay();
                                            final Date tomorrow  = today.getNextDay();
                                            final double dT = dc.yearFraction(yesterday, tomorrow);
                                            settings.setEvaluationDate(yesterday);
                                            value_m = option.getNPV();
                                            settings.setEvaluationDate(tomorrow);
                                            value_p = option.getNPV();
                                            expected.put("theta", (value_p - value_m)/dT);

                                            settings.setEvaluationDate(today);

                                            // compare
                                            for (final Entry<String, Double> it: calculated.entrySet()){

                                                final String greek = it.getKey();
                                                final Double expct = expected.get(greek);
                                                final Double calcl = calculated.get(greek);
                                                final Double tol   = tolerance.get(greek);

                                                final double error = Utilities.relativeError(expct,calcl,u);
                                                if (error>tol)
                                                    REPORT_FAILURE(greek, payoff, exercise, u, q, r, today, v, expct, calcl, error, tol);

                                            }
                                        }
                                    }
                    }
                }

    }


    @Test
    public void testImpliedVol() {

        QL.info("Testing European option implied volatility...");

        final int maxEvaluations = 100;
        final double tolerance = 1.0e-6;

        // test options
        final Option.Type types[] = { Option.Type.CALL, Option.Type.PUT };
        final double strikes[] = { 90.0, 99.5, 100.0, 100.5, 110.0 };
        final int lengths[] = { 36, 180, 360, 1080 };

        // test data
        final double underlyings[] = { 90.0, 95.0, 99.9, 100.0, 100.1, 105.0, 110.0 };
        final double qRates[] = { 0.01, 0.05, 0.10 };
        final double rRates[] = { 0.01, 0.05, 0.10 };
        final double vols[] = { 0.01, 0.20, 0.30, 0.70, 0.90 };

        final DayCounter dc = Actual360.getDayCounter();
        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(today, vol, dc));
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today,qRate, dc));
        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));

        for (final Type type : types)
            for (final double strike2 : strikes)
                for (final int length : lengths) {
                    // option to check
                    final Date exDate = today.getDateAfter( length );
                    final Exercise exercise = new EuropeanExercise(exDate);
                    final StrikedTypePayoff payoff = new PlainVanillaPayoff(type, strike2);
                    final VanillaOption option = makeOption(payoff, exercise, spot, qTS, rTS, volTS, EngineType.Analytic, 0, 0);

                    for (final double u : underlyings)
                        for (final double q : qRates)
                            for (final double r : rRates)
                                for (final double v : vols) {
                                    spot.getLink().setValue(u);
                                    qRate.getLink().setValue(q);
                                    rRate.getLink().setValue(r);
                                    vol.getLink().setValue(v);

                                    final double value = option.getNPV();
                                    double implVol = 0.0; // just to remove a warning...
                                    if (value != 0.0) {
                                        // shift guess somehow
                                        vol.getLink().setValue(v*0.5);
                                        if (Math.abs(value-option.getNPV()) <= 1.0e-12)
                                            // flat price vs vol --- pointless (and
                                            // numerically unstable) to solve
                                            continue;

                                        implVol = option.impliedVolatility(value, 1.0e-6, 200, 1.0e-8, 4.0);

                                        if (Math.abs(implVol-v) > tolerance) {
                                            // the difference might not matter
                                            vol.getLink().setValue(implVol);
                                            final double value2 = option.getNPV();
                                            final double error = Utilities.relativeError(value,value2,u);
                                            if (error > tolerance)
                                                fail(
                                                        type + " option :\n"
                                                        + "    spot value:          " + u + "\n"
                                                        + "    strike:              "
                                                        + strike2 + "\n"
                                                        + "    dividend yield:      "
                                                        + (q) + "\n"
                                                        + "    risk-free rate:      "
                                                        + (r) + "\n"
                                                        + "    maturity:            "
                                                        + exDate + "\n\n"
                                                        + "    original volatility: "
                                                        + (v) + "\n"
                                                        + "    price:               "
                                                        + value + "\n"
                                                        + "    implied volatility:  "
                                                        + (implVol)
                                                        + "\n"
                                                        + "    corresponding price: "
                                                        + value2 + "\n"
                                                        + "    error:               " + error);
                                        }
                                    }
                                }
                }
    }

    @Test
    public void testImpliedVolContainment(){
        QL.info("Testing self-containment of implied volatility calculation... running");

        final int maxEvaluations = 100;
        final double tolerance = 1.0e-6;

        // test options

        final DayCounter dc = Actual360.getDayCounter();

        //does not really have to be a handle
        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(100.0));
        final Handle<Quote> underlying = new Handle<Quote>(spot.getLink());
        //does not really have to be a handle
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.05));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, qRate, dc));
        //does not really have to be a handle
        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.03));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));
        //does not really have to be a handle
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.20));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(vol, dc));

        final Date exerciseDate = today.getDateAfter(Period.ONE_YEAR_FORWARD);
        final Exercise exercise = new EuropeanExercise(exerciseDate);
        final StrikedTypePayoff payoff = new PlainVanillaPayoff(Option.Type.CALL, 100);

        final StochasticProcess process = new BlackScholesMertonProcess(underlying, qTS, rTS, volTS);

        // link to the same stochastic process, which shouldn't be changed
        // by calling methods of either option

        final VanillaOption option1 = new EuropeanOption(process, payoff, exercise);
        final VanillaOption option2 = new EuropeanOption(process, payoff, exercise);

        // test
        final double refValue = option2.getNPV();

        final Flag f = new Flag();
        option2.addObserver(f);

        option1.impliedVolatility(refValue*1.5, tolerance, maxEvaluations);

        if (f.isUp())
            fail("implied volatility calculation triggered a change in another instrument");

        option2.recalculate();
        if (Math.abs(option2.getNPV() - refValue) >= 1.0e-8)
            fail("implied volatility calculation changed the value "
                    + "of another instrument: \n"
                    + "previous value: " + refValue + "\n"
                    + "current value:  " + option2.getNPV());

        vol.getLink().setValue(vol.getLink().op()*1.5);

        if (!f.isUp())
            fail("volatility change not notified");

        if (Math.abs(option2.getNPV() - refValue) <= 1.0e-8)
            fail("volatility change did not cause the value to change");
    }



    private void testEngineConsistency(final EngineType engine,
            final int binomialSteps, final int samples,
            final Map<String, Double> tolerance) {

        testEngineConsistency(engine, binomialSteps, samples, tolerance, false);
    }

    private void testEngineConsistency(final EngineType engine,
            final int binomialSteps, final int samples,
            final Map<String, Double> tolerance, final boolean testGreeks) {

        // QL_TEST_START_TIMING

        final Map<String, Double> calculated = new HashMap<String, Double>();
        final Map<String, Double> expected = new HashMap<String, Double>();

        // test options
        final Option.Type types[] = { Option.Type.CALL, Option.Type.PUT };
        final double strikes[] = { 75.0, 100.0, 125.0 };
        final int lengths[] = { 1 };

        // test data
        final double underlyings[] = { 100.0 };
        final double /* @Rate */qRates[] = { 0.00, 0.05 };
        final double /* @Rate */rRates[] = { 0.01, 0.05, 0.15 };
        final double /* @Volatility */vols[] = { 0.11, 0.50, 1.20 };

        final DayCounter dc = Actual360.getDayCounter();

        final Handle<SimpleQuote> spot = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<SimpleQuote> qRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> qTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, qRate, dc));
        final Handle<SimpleQuote> rRate = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<YieldTermStructure> rTS = new Handle<YieldTermStructure>(Utilities.flatRate(today, rRate, dc));
        final Handle<SimpleQuote> vol = new Handle<SimpleQuote>(new SimpleQuote(0.0));
        final Handle<BlackVolTermStructure> volTS = new Handle<BlackVolTermStructure>(Utilities.flatVol(today, vol, dc));

        for (final Type type : types)
            for (final double strike3 : strikes)
                for (final int length2 : lengths) {

                    final Date exDate = today.getDateAfter(timeToDays(length2));
                    final Exercise exercise = new EuropeanExercise(exDate);

                    final StrikedTypePayoff payoff = new PlainVanillaPayoff(type, strike3);

                    // reference option
                    final VanillaOption refOption = makeOption(payoff, exercise, spot, qTS, rTS, volTS, EngineType.Analytic, 0, 0);
                    // option to check
                    final VanillaOption option = makeOption(payoff, exercise, spot, qTS, rTS, volTS, engine, binomialSteps, samples);

                    for (final double u : underlyings)
                        for (final double q : qRates)
                            for (final double r : rRates)
                                for (final double v : vols) {
                                    spot.getLink().setValue(u);
                                    qRate.getLink().setValue(q);
                                    rRate.getLink().setValue(r);
                                    vol.getLink().setValue(v);

                                    expected.clear();
                                    calculated.clear();

                                    final double refNPV = refOption.getNPV();
                                    final double optNPV = option.getNPV();

                                    expected.put("value", refNPV);
                                    calculated.put("value", optNPV);

                                    if (testGreeks && option.getNPV() > spot.getLink().op() * 1.0e-5) {
                                        expected.put("delta", refOption.delta());
                                        expected.put("gamma", refOption.gamma());
                                        expected.put("theta", refOption.theta());
                                        calculated.put("delta", option.delta());
                                        calculated.put("gamma", option.gamma());
                                        calculated.put("theta", option.theta());
                                    }

                                    for (final Entry<String, Double> entry : calculated.entrySet()) {
                                        final String greek = entry.getKey();
                                        final double expct = expected.get(greek), calcl = calculated.get(greek), tol = tolerance.get(greek);
                                        final double error = Utilities.relativeError(expct, calcl, u);
                                        if (error > tol)
                                            REPORT_FAILURE(greek, payoff, exercise, u, q, r, today, v, expct, calcl, error, tol);
                                    }
                                }
                }
    }


    @Test
    public void testJRBinomialEngines() {

        QL.info("Testing JR binomial European engines against analytic results...");

        final EngineType engine = EngineType.JR;
        final int timeSteps = 251;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 0.002);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testCRRBinomialEngines() {

        QL.info("Testing CRR binomial European engines against analytic results...");

        final EngineType engine = EngineType.CRR;
        final int timeSteps = 501;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 0.002);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testEQPBinomialEngines() {

        QL.info("Testing EQP binomial European engines against analytic results...");

        final EngineType engine = EngineType.EQP;
        final int timeSteps = 501;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 0.02);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testTGEOBinomialEngines() {

        QL.info("Testing TGEO binomial European engines against analytic results...");

        final EngineType engine = EngineType.TGEO;
        final int timeSteps = 251;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 0.002);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testTIANBinomialEngines() {

        QL.info("Testing TIAN binomial European engines against analytic results...");

        final EngineType engine = EngineType.TIAN;
        final int timeSteps = 251;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 0.002);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testLRBinomialEngines() {

        QL.info("Testing LR binomial European engines against analytic results...");

        final EngineType engine = EngineType.LR;
        final int timeSteps = 251;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 1.0e-6);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testJOSHIBinomialEngines() {

        QL.info("Testing Joshi binomial European engines against analytic results...");

        final EngineType engine = EngineType.JOSHI;
        final int timeSteps = 251;
        final int samples = 0;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 1.0e-7);
        relativeTol.put("delta", 1.0e-3);
        relativeTol.put("gamma", 1.0e-4);
        relativeTol.put("theta", 0.03);
        testEngineConsistency(engine, timeSteps, samples, relativeTol, true);
    }


    @Test
    public void testFdEngines() {

        QL.info("Testing finite-differences European engines against analytic results...");

        final EngineType engine = EngineType.FiniteDifferences;
        final @NonNegative int timeSteps = 300;
        final @NonNegative int gridPoints = 300;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(4);
        relativeTol.put("value", 1.0e-4);
        relativeTol.put("delta", 1.0e-6);
        relativeTol.put("gamma", 1.0e-6);
        relativeTol.put("theta", 1.0e-4);
        testEngineConsistency(engine, timeSteps, gridPoints, relativeTol, true);
    }


    @Test
    public void testIntegralEngines() {

        QL.info("Testing integral engines against analytic results...");


        final EngineType engine = EngineType.Integral;
        final int timeSteps = 300;
        final int gridPoints = 300;
        final Map<String,Double> relativeTol = new HashMap<String, Double>(1);
        relativeTol.put("value", 0.0001);
        testEngineConsistency(engine, timeSteps, gridPoints, relativeTol);
    }


    //
    //  void EuropeanOptionTest::testMcEngines() {
    //
    //      BOOST_MESSAGE("Testing Monte Carlo European engines "
    //                    "against analytic results...");
    //
    //      EngineType engine = PseudoMonteCarlo;
    //      Size steps = Null<Size>();
    //      Size samples = 40000;
    //      std::map<std::string,Real> relativeTol;
    //      relativeTol["value"] = 0.01;
    //      testEngineConsistency(engine,steps,samples,relativeTol);
    //  }

    //  void EuropeanOptionTest::testQmcEngines() {
    //
    //      BOOST_MESSAGE("Testing Quasi Monte Carlo European engines "
    //                    "against analytic results...");
    //
    //      EngineType engine = QuasiMonteCarlo;
    //      Size steps = Null<Size>();
    //      Size samples = 4095; // 2^12-1
    //      std::map<std::string,Real> relativeTol;
    //      relativeTol["value"] = 0.01;
    //      testEngineConsistency(engine,steps,samples,relativeTol);
    //  }

    //  void EuropeanOptionTest::testPriceCurve() {
    //
    //      BOOST_MESSAGE("Testing European price curves...");
    //
    //      /* The data below are from
    //         "Option pricing formulas", E.G. Haug, McGraw-Hill 1998
    //      */
    //      EuropeanOptionData values[] = {
    //        // pag 2-8
    //        //        type, strike,   spot,    q,    r,    t,  vol,   value
    //        { Option.Type.Call,  65.00,  60.00, 0.00, 0.08, 0.25, 0.30,  2.1334, 0.0},
    //        { Option.Type.Put,   95.00, 100.00, 0.05, 0.10, 0.50, 0.20,  2.4648, 0.0},
    //      };
    //
    //      DayCounter dc = Actual360();
    //      Date today = Date::todaysDate();
    //      Size timeSteps = 300;
    //      Size gridPoints = 300;
    //
    //      boost::shared_ptr<SimpleQuote> spot(new SimpleQuote(0.0));
    //      boost::shared_ptr<SimpleQuote> qRate(new SimpleQuote(0.0));
    //      boost::shared_ptr<YieldTermStructure> qTS = flatRate(today, qRate, dc);
    //      boost::shared_ptr<SimpleQuote> rRate(new SimpleQuote(0.0));
    //      boost::shared_ptr<YieldTermStructure> rTS = flatRate(today, rRate, dc);
    //      boost::shared_ptr<SimpleQuote> vol(new SimpleQuote(0.0));
    //      boost::shared_ptr<BlackVolTermStructure> volTS = flatVol(today, vol, dc);
    //      boost::shared_ptr<PricingEngine>
    //          engine(new FDEuropeanEngine(timeSteps, gridPoints));
    //
    //      for (Size i=0; i<LENGTH(values); i++) {
    //
    //          boost::shared_ptr<StrikedTypePayoff> payoff(new
    //              PlainVanillaPayoff(values[i].type, values[i].strike));
    //          Date exDate = today + timeToDays(values[i].t);
    //          boost::shared_ptr<Exercise> exercise(new EuropeanExercise(exDate));
    //
    //          spot ->setValue(values[i].s);
    //          qRate->setValue(values[i].q);
    //          rRate->setValue(values[i].r);
    //          vol  ->setValue(values[i].v);
    //
    //          boost::shared_ptr<StochasticProcess> stochProcess(new
    //              BlackScholesMertonProcess(Handle<Quote>(spot),
    //                                        Handle<YieldTermStructure>(qTS),
    //                                        Handle<YieldTermStructure>(rTS),
    //                                        Handle<BlackVolTermStructure>(volTS)));
    //
    //          EuropeanOption option(stochProcess, payoff, exercise, engine);
    //          SampledCurve price_curve = option.result<SampledCurve>("priceCurve");
    //          if (price_curve.empty()) {
    //              REPORT_FAILURE("no price curve", payoff, exercise, values[i].s,
    //                             values[i].q, values[i].r, today,
    //                             values[i].v, values[i].result, 0.0,
    //                             0.0, 0.0);
    //              continue;
    //          }
    //
    //          // Ignore the end points
    //          Size start = price_curve.size() / 4;
    //          Size end = price_curve.size() * 3 / 4;
    //          for (Size i=start; i < end; i++) {
    //              spot->setValue(price_curve.gridValue(i));
    //              boost::shared_ptr<StochasticProcess> stochProcess1(
    //                        new BlackScholesMertonProcess(
    //                                         Handle<Quote>(spot),
    //                                         Handle<YieldTermStructure>(qTS),
    //                                         Handle<YieldTermStructure>(rTS),
    //                                         Handle<BlackVolTermStructure>(volTS)));
    //
    //              EuropeanOption option1(stochProcess, payoff, exercise, engine);
    //              Real calculated = option1.NPV();
    //              Real error = std::fabs(calculated-price_curve.value(i));
    //              Real tolerance = 1e-3;
    //              if (error>tolerance) {
    //                  REPORT_FAILURE("price curve error", payoff, exercise,
    //                                 price_curve.gridValue(i),
    //                                 values[i].q, values[i].r, today,
    //                                 values[i].v,
    //                                 price_curve.value(i), calculated,
    //                                 error, tolerance);
    //                  break;
    //              }
    //          }
    //      }
    //
    //  }


    private void REPORT_FAILURE(final String greekName, final StrikedTypePayoff payoff, final Exercise exercise,
            final double s, final double q, final double r, final Date today,
            final double v, final double expected, final double calculated, final double error, final double tolerance) {

        final StringBuilder sb = new StringBuilder();
        sb.append(exercise).append(" ");
        sb.append(payoff.optionType()).append(" option with ");
        sb.append(payoff.getClass().getName() + " payoff:\n");
        sb.append("    spot value:       " + s + "\n");

        sb.append("    strike:           " + payoff.strike() + "\n");
        sb.append("    dividend yield:   " + q + "\n");
        sb.append("    risk-free rate:   " + r + "\n");
        sb.append("    reference date:   " + today + "\n");
        sb.append("    maturity:         " + exercise.lastDate() + "\n");
        sb.append("    volatility:       " + v + "\n\n");
        sb.append("    expected " + greekName + ":   " + expected + "\n" );
        sb.append("    calculated " + greekName + ": " + calculated + "\n");
        sb.append("    error:            " + error + "\n");
        sb.append("    tolerance:        " + tolerance);
        fail(sb.toString());
    }

}
