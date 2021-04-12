package plugins.tinevez.rieszwavelets;

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

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;

/**
 * 
 * Configuration of the Riesz transform.
 * 
 * For a description, see:
 * 
 * A Unifying Parametric Framework for 2D Steerable Wavelet Transforms
 * Unser, M. and Chenouard, N.
 * SIAM Journal on Imaging Sciences 2013 6:1, 102-135 
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 */

public class RieszConfig
{
	int order; // maximum order of the harmonics
	double[][] filters; // filters for the different harmonics, defined in the Fourier domain
	int numChannels; // number of channels
	HarmonicTypes harmonicType; // type of harmonics used
	int[] harmonics; // harmonics
	int width; // width of the filters
	int height; // height of the filters

	/**
	 * Initialize the Riesz configuration object. Filters are precomputed.
	 * 
	 * @param order maximum order of the harmonics
	 * @param harmonicTypes type of harmonics used
	 * @param width width of the filters
	 * @param height height of the filters
	 * 
	 * */
	RieszConfig(int order, HarmonicTypes harmonicTypes, int width, int height)
	{
		this.order = Math.abs(order);
		this.harmonicType = harmonicTypes;
		this.width = width;
		this.height = height;
		fillHarmonics();
		prepareFiltersFFT();
	}

	/**
	 * Initialize the Riesz configuration object. Filters are not precomputed.
	 * 
	 * @param order maximum order of the harmonics
	 * @param harmonicTypes type of harmonics used
	 * 
	 * */
	RieszConfig(int order, HarmonicTypes harmonicTypes)
	{
		this.order = order;
		this.harmonicType = harmonicTypes;
		fillHarmonics();
	}

	/**
	 * Initialize the Riesz configuration object with cutom harmonics. Filters are not precomputed.
	 * @param harmonics set of harmonics to use for the transform
	 * @param width width of the filters
	 * @param height height of the filters
	 * */
	RieszConfig(int[] harmonics, int width, int height)
	{
		//TODO custom harmonics
		throw new IllegalArgumentException("Custom harmonics not yet supported");
	}

	/**
	 * Compute the set of harmonics based on the maximum order and the type of harmonics selected
	 * */
	private void fillHarmonics()
	{
		switch (this.harmonicType)
		{
		case even:
		{
			if ((this.order % 2) > 0)
			{
				throw new IllegalArgumentException("Order of Riesz filter must be even for even-type harmonics.");
			}
			else
			{
				numChannels = order + 1;
				this.harmonics = new int[numChannels];
				int k = 0;
				for (int o = - order; o <= order; o+=2)
				{
					harmonics[k] = o;
					k++;
				}
			}
			break;
		}
		case odd:
		{
			if ((this.order % 2) == 0)
			{
				throw new IllegalArgumentException("Order of Riesz filter must be odd for odd-type harmonics.");
			}
			else
			{
				numChannels = order + 1;
				this.harmonics = new int[numChannels];
				int k = 0;
				for (int o = - order; o <= order; o+=2)
				{
					harmonics[k] = o;
					k++;
				}
			}
			break;
		}
		case complete:
		{
			numChannels = 2*order + 1;
			this.harmonics = new int[numChannels];
			int k = 0;
			for (int o = - order; o <= order; o++)
			{
				harmonics[k] = o;
				k++;
			}
			break;
		}
		case positive:
		{
			numChannels = order + 1;
			this.harmonics = new int[numChannels];
			int k = 0;
			for (int o = 0; o <= order; o++)
			{
				harmonics[k] = o;
				k++;
			}
			break;
		}
		default:
			throw new IllegalArgumentException("Unable to use this constructor for RieszConfig with custom harmonics");
		}
	}

	/**
	 * Compute the FFT of the filters for the set of harmonics
	 * */
	protected void prepareFiltersFFT()
	{
		filters = new double[numChannels][width*height*2];
		// build the angle map
		double[] angles = new double[width*height];

		int cX = (int) Math.ceil((width + 1)/2);
		int cY = (int) Math.ceil((height + 1)/2);
		//double normX = (double)(cX*cX);
		//double normY = (double)(cY*cY);
		// y = 0
		for (int x = 0; x < cX; x++)
			angles[x] = 0;
		for (int x = cX; x < width; x++)
			angles[x] = Math.PI;

		for (int y = 1; y < cY; y++)
		{
			// x = 0
			angles[y*width] = Math.PI/2;
			for (int x = 1; x < cX; x++)
			{
				angles[x + y*width] = Math.atan2(((double) y)/height, ((double) x)/width);
			}
			for (int x = cX; x < width; x++)
			{
				angles[x + y*width] = Math.atan2(((double)y)/height, ((double)(x - width))/width);        			
			}
		}
		for (int y = cY; y < height; y++)
		{
			// x  = 0
			angles[y*width] = -Math.PI/2;
			for (int x = 1; x < cX; x++)
			{
				angles[x + y*width] = Math.atan2(((double)(y - height))/height, ((double) x)/width);
			}
			for (int x = cX; x < width; x++)
			{
				angles[x + y*width] = Math.atan2(((double)(y - height))/height, ((double)(x - width))/width);   			
			}
		}

		//		Sequence angleSequence = new Sequence();
		//		angleSequence.setName("Angles");
		//		angleSequence.addImage(0, new IcyBufferedImage(width, height, angles));
		//		Icy.getMainInterface().addSequence(angleSequence);
		for (int c = 0; c < numChannels; c++)
		{
			double h = harmonics[c];
			double[] values = filters[c];
			for (int i = 0; i < angles.length; i++)
			{
				values[2*i] = (double) Math.cos(h*angles[i]);
				values[2*i + 1] = (double) Math.sin(h*angles[i]);    			
			}
		}
	}

	/**
	 * Compute generalized Riesz transform coefficients for a 2D image
	 * @param image the 2D image to analyze as a 1D array
	 * @param generalization the generalization based on a linear combination the circular harmonics
	 * @return a set of 2D images as 1D array, each of which corresponds to one band of the generalized Riesz transform
	 * */
	public double[][] analysis(double[] image, RieszGeneralization generalization)
	{
		return analysis(image, generalization, false, false);
	}

	/**
	 * Compute generalized Riesz transform coefficients for a 2D image with input and output possibly in the Fourier domain
	 * @param image the 2D image to analyze as a 1D array
	 * @param generalization the generalization based on a linear combination the circular harmonics
	 * @param inputInFourier true if Fourier domain representation of the image is provided as input, false if it is in space domain
	 * @param outputInFourier true if output coefficients are provided in Fourier domain, false for space domain
	 * @return a set of 2D images as 1D array, each of which corresponds to one band of the generalized Riesz transform
	 * */
	public double[][] analysis(double[] image, RieszGeneralization generalization, boolean inputInFourier, boolean outputInFourier)
	{
		double[] dataFFT;
		if (inputInFourier)
		{
			dataFFT = image;
		}
		else
		{
			// compute the FFT of the image
			dataFFT = new double[width*height*2];
			for (int i = 0; i < (width*height); i++)
				dataFFT[2*i] = image[i];
			DoubleFFT_2D fft = new DoubleFFT_2D(height, width);
			fft.complexForward(dataFFT);
		}
		// apply Riesz transform
		double[][] bandFFT = new double[numChannels][width*height*2];
		for (int i = 0; i < numChannels; i++)
		{
			for (int t = 0; t < (width*height); t++)
			{
				bandFFT[i][2*t] = dataFFT[2*t]*filters[i][2*t] - dataFFT[2*t + 1]*filters[i][2*t + 1];
				bandFFT[i][2*t + 1] = dataFFT[2*t]*filters[i][2*t  + 1] + dataFFT[2*t + 1]*filters[i][2*t];
			}
		}
		// combine bands
		bandFFT = generalization.combineBandsForwardComplex(bandFFT);
		//generalization.printForwardMatrix();
		if (outputInFourier)
		{
			return bandFFT;
		}
		else
		{
			// compute the inverse FFT of each band
			if (generalization.realCoefficients)
			{
				DoubleFFT_2D fft = new DoubleFFT_2D(height, width);
				double[][] bands = new double[bandFFT.length][width*height];
				for (int i = 0; i < bandFFT.length; i++)
				{
					fft.complexInverse(bandFFT[i], true);
					for (int j = 0; j < width*height; j++)
					{
						bands[i][j] = bandFFT[i][2*j];
					}
				}
				return bands;
			}
			else
			{
				DoubleFFT_2D fft = new DoubleFFT_2D(height, width);
				for (int i = 0; i < bandFFT.length; i++)
					fft.complexInverse(bandFFT[i], true);
				return bandFFT;
			}
		}
	}

	/**
	 * Reconstruct a 2D image from a set of Riesz coefficients
	 * @param rieszBands set of Riesz coefficients
	 * @param generalization the Riesz generalization used for computing the coefficients
	 * @param inputInFourier true if Riesz coefficients are provided in the Fourier domain, false else.
	 * @param outputInFourier true if the reconstructed image is to be returned in the Fourier domain, false if the space representation is returned.
	 * @return reconstructed image as a 1D array
	 * */
	public double[] synthesis(double[][] rieszBands, RieszGeneralization generalization, boolean inputInFourier, boolean outputInFourier)
	{
		double[][] rieszBandsFFT;
		// project to Fourier domain
		if (inputInFourier)
		{
			rieszBandsFFT = rieszBands;
		}
		else
		{
			rieszBandsFFT = new double[rieszBands.length][2*width*height];
			for (int i = 0; i < rieszBands.length; i++)
			{
				DoubleFFT_2D fft = new DoubleFFT_2D(height, width);
				for (int j = 0; j < width*height; j++)
					rieszBandsFFT[i][2*j] = rieszBands[i][j];
				fft.complexForward(rieszBandsFFT[i]);
			}
		}
		// project bands with backward generalization matrix
		rieszBandsFFT = generalization.combineBandsBackwardComplex(rieszBandsFFT);
		// apply inverse Riesz transform
		double[] reconstructionFFT = new double[width*height*2];
		for (int i = 0; i < numChannels; i++)
		{
			for (int t = 0; t < (width*height); t++)
			{
				reconstructionFFT[2*t] += rieszBandsFFT[i][2*t]*filters[i][2*t] + rieszBandsFFT[i][2*t + 1]*filters[i][2*t + 1]; // complex conjugate filter
				reconstructionFFT[2*t + 1] += -rieszBandsFFT[i][2*t]*filters[i][2*t  + 1] + rieszBandsFFT[i][2*t + 1]*filters[i][2*t]; // complex conjugate filter
			}
		}
		double[] reconstruction;
		// project to Fourier domain
		if (outputInFourier)
		{
			reconstruction = reconstructionFFT;
		}
		else
		{
			DoubleFFT_2D fft = new DoubleFFT_2D(height, width);
			fft.complexInverse(reconstructionFFT, true);
			reconstruction = new double[width*height];
			for (int t = 0; t < (width*height); t++)
				reconstruction[t] = reconstructionFFT[2*t];
		}
		return reconstruction;
	}

	/**
	 * Steer a set of coefficients according to a set of angles
	 * @param rieszBands the set of coefficients to steer
	 * @param generalization the Riesz generalization used for computing the coefficients
	 * @param angles an array corresponding to the angles according to which to steer each vector of Riesz coefficients. They are expressed in radians.
	 * @param forward true if the steering is forward (rotation with angle 'angles'), else for backward (rotation with angle '-angles')
	 * */
	public void steerCoefficients(double[][] rieszBands, RieszGeneralization generalization, double[] angles, boolean forward)
	{
		if (generalization.realCoefficients)
		{
			double[][] complexBands = new double[rieszBands.length][];
			for (int i = 0; i < complexBands.length; i++)
			{
				complexBands[i] = new double[rieszBands[i].length*2];
				for (int j = 0; j < rieszBands[i].length; j++)
					complexBands[i][2*j] = rieszBands[i][j];
			}
			complexBands = generalization.combineBandsBackwardComplex(complexBands);
			// steer coefficients
			double c, s, r, i;
			for (int b = 0; b < complexBands.length; b++)
			{
				//exp(1i*harmonics(n)*ang{sc});
				int harmonic = this.harmonics[b];
				if (forward)
					for (int k = 0; k < angles.length; k++)
					{
						r = complexBands[b][2*k];
						i  = complexBands[b][2*k + 1];
						c = Math.cos(harmonic*angles[k]);
						s = Math.sin(harmonic*angles[k]);
						complexBands[b][2*k] = r*c - i*s;
						complexBands[b][2*k + 1] = r*s + i*c;
					}
				else
					for (int k = 0; k < angles.length; k++)
					{
						r = complexBands[b][2*k];
						i  = complexBands[b][2*k + 1];
						c = Math.cos(-harmonic*angles[k]);
						s = Math.sin(-harmonic*angles[k]);
						complexBands[b][2*k] = r*c - i*s;
						complexBands[b][2*k + 1] = r*s + i*c;
					}
			}
			// project bands with backward generalization matrix
			complexBands = generalization.combineBandsForwardComplex(complexBands);
			for (int b = 0; b < rieszBands.length; b++)
				for (int k = 0; k < rieszBands[b].length; k++)
					rieszBands[b][k] = complexBands[b][2*k];
		}
		else
		{
			rieszBands = generalization.combineBandsBackwardComplex(rieszBands);
			// steer coefficients
			double c, s, r, i;
			for (int b = 0; b < rieszBands.length; b++)
			{
				//exp(1i*harmonics(n)*ang{sc});
				int harmonic = this.harmonics[b];
				if (forward)
					for (int k = 0; k < angles.length; k++)
					{
						r = rieszBands[b][2*k];
						i  = rieszBands[b][2*k + 1];
						c = Math.cos(harmonic*angles[k]);
						s = Math.sin(harmonic*angles[k]);
						rieszBands[b][2*k] = r*c - i*s;
						rieszBands[b][2*k + 1] = r*s + i*c;
					}
				else
					for (int k = 0; k < angles.length; k++)
					{
						r = rieszBands[b][2*k];
						i  = rieszBands[b][2*k + 1];
						c = Math.cos(-harmonic*angles[k]);
						s = Math.sin(-harmonic*angles[k]);
						rieszBands[b][2*k] = r*c - i*s;
						rieszBands[b][2*k + 1] = r*s + i*c;
					}
			}	
			double[][] combinedBands = generalization.combineBandsForwardComplex(rieszBands);
			for (int b = 0; b < rieszBands.length; b++)
				rieszBands[b] = combinedBands[b];

		}		
	}
}
