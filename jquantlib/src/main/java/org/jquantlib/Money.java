/*
 Copyright (C) 2009 Ueli Hofstetter

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
package org.jquantlib;


import org.jquantlib.currencies.ExchangeRateManager;
import org.jquantlib.math.Closeness;

/**
 * Cash amount in a given currency.
 */
public class Money {

    // static fields FIXME: ugly
    public static ConversionType conversionType;
    public static Currency baseCurrency;

    // enums
    public enum ConversionType {
        NoConversion, /*   do not perform conversions */
        BaseCurrencyConversion, /*
                                 *   convert both operands to the base currency before converting
                                 */
        AutomatedConversion
        /*
         *  return the result in the currency of the first operand
         */
    };

    // class fields
    private/* Decimal */double value_;
    private Currency currency_;

    // constructors
    public Money() {
        this.value_ = (0.0);
    }

    public Money(final Currency currency, /* Decimal */double value) {
        this.value_ = (value);
        this.currency_ = (currency);
    }

    public Money(/* Decimal */double value, final Currency currency) {
        this.value_ = (value);
        this.currency_ = (currency.clone());
    }

    public Money clone() {
        Money money = new Money();
        money.currency_ = currency_.clone();
        money.value_ = value_;
        return money;
    }

    // accessors
    public Currency currency() {
        return currency_;
    }

    public/* Decimal */double value() {
        return value_;
    }

    public Money rounded() {
        return new Money(currency_.rounding().operator(value_), currency_);
    }


    // class based operators

    // +() //FIXME: this looks like a mistake in c++
    public Money positiveValue() {
        return new Money(currency_, value_);
    }

    // -()
    public Money negativeValue() {
        return new Money(-value_, currency_);
    }

    // *=
    public Money mulAssign(/* Decimal */double x) {
        value_ *= x;
        return this;
    }

    // /=
    public Money divAssign(/* Decimal */double x) {
        value_ /= x;
        return this;
    }

    // static operators

    // +
    public Money add(final Money money) {
        Money tmp = clone();
        tmp.addAssign(money);
        return tmp;
    }

    // -
    public Money sub(final Money money) {
        Money tmp = clone();
        tmp.subAssign(money);
        return tmp;
    }

    // *
    public Money mul(/* Decimal */double x) {
        Money tmp = clone();
        tmp.mulAssign(x);
        return tmp;
    }

    public Money div(/* Decimal */double x) {
        Money tmp = clone();
        tmp.value_ /= x;
        return tmp;
    }

    public boolean notEquals(final Money money) {
        // eating dogfood
        return !(this.equals(money));
    }

    public boolean greater(final Money money) {
        return money.greater(this);

    }

    public boolean greaterEqual(final Money money) {
        return money.greaterEqual(this);
    }

    // FIXME: suspicious....
    public Money operatorMultiply(/* Decimal */double value, final Currency c) {
        return new Money(value, c);
    }

    public static Money multiple(final Currency c, /* Decimal */double value) {
        return new Money(value, c);
    }

    public static Money multiple( /* Decimal */double value, final Currency c) {
        return new Money(value, c);
    }

    public void convertTo(final Currency target) {
        if (currency().notEquals(target)) {
            ExchangeRate rate = ExchangeRateManager.getInstance().lookup(currency(), target);
            // FIXME ... evt. Money should be modified in ExchangeRate directly
            Money money = rate.exchange(this).rounded();
            this.currency_ = money.currency_;
            this.value_ = money.value_;
        }
    }

    public void convertToBase() {
        assert (!Money.baseCurrency.empty()) : "no base currency set";
        convertTo(Money.baseCurrency);
    }

    // +=(const Money& m)
    public Money addAssign(final Money money) {
        if (this.currency_.equals(money.currency_)) {
            this.value_ += money.value_;
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            this.convertToBase();
            Money tmp = money.clone();
            tmp.convertToBase();
            // recursive invocation
            this.addAssign(tmp);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            tmp.convertTo(currency_);
            // recursive invocation
            this.addAssign(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
        return this;
    }

    // Money& Money::operator-=(const Money& m)
    public Money subAssign(final Money money) {
        if (currency_.equals(money.currency_)) {
            value_ -= money.value_;
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            this.convertToBase();
            Money tmp = money.clone();
            tmp.convertToBase();
            // recursive ...
            this.subAssign(tmp);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            tmp.convertTo(currency_);
            this.subAssign(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
        return this;
    }

    // Decimal operator/
    public double div(final Money money) {
        if (currency().equals(money.currency())) {
            return value_ / money.value();
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money.clone();
            tmp2.convertToBase();
            // recursive
            return this.div(tmp2);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            tmp.convertTo(money.currency());
            // recursive
            return this.div(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public boolean equals(final Money money) {
        if (currency().equals(money.currency())) {
            return value() == money.value();
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money.clone();
            tmp2.convertToBase();
            // recursive...
            return tmp1.equals(tmp2);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            tmp.convertTo(this.currency());
            return this.equals(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public boolean less(final Money money) {
        if (this.currency().equals(money.currency())) {
            return value() < money.value();
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money;
            tmp2.convertToBase();
            return tmp1.less(tmp2);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money;
            tmp.convertTo(currency());
            return this.less(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public boolean lessEquals(final Money money) {
        if (currency().equals(money.currency())) {
            return value() <= money.value();
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money;
            tmp2.convertToBase();
            return tmp1.less(tmp2);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            ;
            tmp.convertTo(this.currency());
            return this.less(tmp);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public boolean close(final Money money, /* Size */int n) {
        if (currency().equals(money.currency())) {
            return Closeness.isClose(value(), money.value(), n);
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money.clone();
            tmp2.convertToBase();
            return tmp1.close(tmp2, n);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money.clone();
            tmp.convertTo(this.currency());
            return this.close(tmp, n);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public boolean close_enough(final Money money, /* Size */int n) {
        if (currency().equals(money.currency())) {
            return Closeness.isCloseEnough(value(), money.value(), n);
        } else if (Money.conversionType == Money.ConversionType.BaseCurrencyConversion) {
            Money tmp1 = this.clone();
            tmp1.convertToBase();
            Money tmp2 = money;
            tmp2.convertToBase();
            return tmp1.close_enough(tmp2, n);
        } else if (Money.conversionType == Money.ConversionType.AutomatedConversion) {
            Money tmp = money;
            tmp.convertTo(currency());
            return this.close_enough(tmp, n);
        } else {
            throw new AssertionError("currency mismatch and no conversion specified");
        }
    }

    public String toString() {
        // TODO: check how to handle formatting...
        return rounded().value() + " " + currency().symbol() + "(" + currency().code() + ")";
    }

}
