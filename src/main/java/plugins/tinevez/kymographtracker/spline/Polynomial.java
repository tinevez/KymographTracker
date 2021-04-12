package plugins.tinevez.kymographtracker.spline;

/*-
 * #%L
 * KymographTracker2
 * %%
 * Copyright (C) 2016 - 2021 Nicolas Chenouard, Jean-Yves Tinevez
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */



/*
 * Class:        Polynomial
 * Description:  
 * Environment:  Java
 * Software:     SSJ 
 * Copyright (C) 2001  Pierre L'Ecuyer and Universit� de Montr�al
 * Organization: DIRO, Universit� de Montr�al
 * @author       �ric Buist
 * @since

 * SSJ is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or
 * any later version.

 * SSJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * A copy of the GNU General Public License is available at
   <a href="http://www.gnu.org/licenses">GPL licence site</a>.
 */


/**
 * Represents a polynomial of degree <SPAN CLASS="MATH"><I>n</I></SPAN> in power form. Such a polynomial is of
 * the form
 * 
 * <DIV CLASS="mathdisplay">
 * <I>p</I>(<I>x</I>) = <I>c</I><SUB>0</SUB> + <I>c</I><SUB>1</SUB><I>x</I> + <SUP> ... </SUP> + <I>c</I><SUB>n</SUB><I>x</I><SUP>n</SUP>,
 * </DIV>
 * where 
 * <SPAN CLASS="MATH"><I>c</I><SUB>0</SUB>,&#8230;, <I>c</I><SUB>n</SUB></SPAN> are the coefficients of the polynomial.
 * 
 */
public class Polynomial implements Cloneable {
   private double[] coeff;


   /**
    * Constructs a new polynomial with coefficients <TT>coeff</TT>. The value of
    *  <TT>coeff[i]</TT> in this array corresponds to <SPAN CLASS="MATH"><I>c</I><SUB>i</SUB></SPAN>.
    * 
    * @param coeff the coefficients of the polynomial.
    * 
    *    @exception NullPointerException if <TT>coeff</TT> is <TT>null</TT>.
    * 
    *    @exception IllegalArgumentException if the length of <TT>coeff</TT> is 0.
    * 
    * 
    */
   public Polynomial (double... coeff) {
      if (coeff == null)
         throw new NullPointerException ();
      if (coeff.length == 0)
         throw new IllegalArgumentException (
               "At least one coefficient is needed");
      this.coeff = coeff.clone ();
   }


   /**
    * Returns the degree of this polynomial.
    * 
    * @return the degree of this polynomial.
    * 
    */
   public int getDegree () {
      return coeff.length - 1;
   }


   /**
    * Returns an array containing the coefficients of the polynomial.
    * 
    * @return the array of coefficients.
    * 
    */
   public double[] getCoefficients () {
      return coeff.clone ();
   }


   /**
    * Returns the <SPAN CLASS="MATH"><I>i</I></SPAN>th coefficient of the polynomial.
    *
    * @param i the index of the coefficient
    * @return the value of the ith coefficient
    * 
    */
   public double getCoefficient (int i) {
      return coeff[i];
   }


   /**
    * Sets the array of coefficients of this polynomial to <TT>coeff</TT>.
    * 
    * @param coeff the new array of coefficients.
    * 
    *    @exception NullPointerException if <TT>coeff</TT> is <TT>null</TT>.
    * 
    *    @exception IllegalArgumentException if the length of <TT>coeff</TT> is 0.
    * 
    * 
    */
   public void setCoefficients (double... coeff) {
      if (coeff == null)
         throw new NullPointerException ();
      if (coeff.length == 0)
         throw new IllegalArgumentException (
               "At least one coefficient is needed");
      this.coeff = coeff.clone ();
   }


   public double evaluate (double x) {
      double res = coeff[coeff.length - 1];
      for (int i = coeff.length - 2; i >= 0; i--)
         res = coeff[i] + x * res;
      return res;
   }

   public double derivative (double x) {
      return derivative (x, 1);
   }

   public double derivative (double x, int n) {
      if (n < 0)
         throw new IllegalArgumentException ("n < 0");
      if (n == 0)
         return evaluate (x);
      if (n >= coeff.length)
         return 0;
//      double res = coeff[coeff.length - 1]*(coeff.length - 1);
//      for (int i = coeff.length - 2; i >= n; i--)
//         res = i*(coeff[i] + x * res);
      double res = getCoeffDer (coeff.length - 1, n);
      for (int i = coeff.length - 2; i >= n; i--)
         res = getCoeffDer (i, n) + x * res;
      return res;
   }

   /**
    * Returns a polynomial corresponding to the <SPAN CLASS="MATH"><I>n</I></SPAN>th derivative of
    * this polynomial.
    * 
    * @param n the degree of the derivative.
    * 
    *    @return the derivative.
    * 
    */
   public Polynomial derivativePolynomial (int n) {
      if (n < 0)
         throw new IllegalArgumentException ("n < 0");
      if (n == 0)
         return this;
      if (n >= coeff.length)
         return new Polynomial (0);
      final double[] coeffDer = new double[coeff.length - n];
      for (int i = coeff.length - 1; i >= n; i--)
         coeffDer[i - n] = getCoeffDer (i, n);
      return new Polynomial (coeffDer);
   }


   private double getCoeffDer (int i, int n) {
      double coeffDer = coeff[i];
      for (int j = i; j > i - n; j--)
         coeffDer *= j;
      return coeffDer;
   }

   public double integral (double a, double b) {
      return integralA0 (b) - integralA0 (a);
   }

   private double integralA0 (double u) {
      final int n = coeff.length - 1;
      double res = u * coeff[n] / (n + 1);
      for (int i = coeff.length - 2; i >= 0; i--)
         res = coeff[i] * u / (i + 1) + u * res;
      return res;
   }

   /**
    * Returns a polynomial representing the integral of this polynomial.
    *  This integral is of the form
    * 
    * <DIV CLASS="mathdisplay">
    * &int;<I>p</I>(<I>x</I>)<I>dx</I> = <I>c</I> + <I>c</I><SUB>0</SUB><I>x</I> + <IMG
    *  SRC="Polynomialimg1.png"
    *  ALT="$\displaystyle {\frac{{c_1 x^2}}{2}}$"> + <SUP> ... </SUP> + <IMG
    *  SRC="Polynomialimg2.png"
    *  ALT="$\displaystyle {\frac{{c_n x^{n+1}}}{{n+1}}}$">,
    * </DIV>
    * where <SPAN CLASS="MATH"><I>c</I></SPAN> is a user-defined constant.
    * 
    * @param c the constant for the integral.
    * 
    *    @return the polynomial representing the integral.
    * 
    */
   public Polynomial integralPolynomial (double c) {
      final double[] coeffInt = new double[coeff.length + 1];
      coeffInt[0] = c;
      for (int i = 0; i < coeff.length; i++)
         coeffInt[i + 1] = coeff[i] / (i + 1);
      return new Polynomial (coeffInt);
   }

   @Override
   public Polynomial clone () {
      Polynomial pol;
      try {
         pol = (Polynomial) super.clone ();
      }
      catch (final CloneNotSupportedException cne) {
         throw new IllegalStateException ("Clone not supported");
      }
      pol.coeff = coeff.clone ();
      return pol;
   }
}
