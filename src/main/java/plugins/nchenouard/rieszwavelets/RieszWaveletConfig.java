package plugins.nchenouard.rieszwavelets;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.util.ArrayList;

import plugins.nchenouard.isotropicwavelets.IsotropicWaveletTransform;
import plugins.nchenouard.isotropicwavelets.IsotropicWaveletType;
import plugins.nchenouard.isotropicwavelets.WaveletAnalysisResults;
import plugins.nchenouard.isotropicwavelets.WaveletFilterSet;

/**
 * 
 * Configuration of the Riesz-wavelet frame
 * 
 * For a mathematical description see:
 * 
 * A Unifying Parametric Framework for 2D Steerable Wavelet Transforms
 * Unser, M. and Chenouard, N.
 * SIAM Journal on Imaging Sciences 2013 6:1, 102-135 
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 * @date 2014-05-25
 * @license gpl v3.0
 */

public class RieszWaveletConfig
{
	int width; // width of the analyzed image
	int height; // height of the analyzed image
	boolean isRealImage; // true if the analyzed image is real
	WaveletFilterSet waveletsConfig; // configuration of the wavelet transform
	ArrayList<RieszConfig> rieszConfigList; // Riesz frame configuration for each wavelet scale

	/**
	 * Standard constructor for the Riesz-wavelet frame configuration with similar Riesz transform for each wavelet scale
	 * 
	 * @param width width of the analyzed image
	 * @param height height of the analyzed image
	 * @param isRealImage true if the analyzed image is real
	 * @param numScales number of wavelet scales for the multiscale decomposition
	 * @param waveletType type of radial profile for the wavelet transform
	 * @param prefilter true if low frequency prefiltering is required
	 * @param prepareRieszFilters true if Riesz filters are to be precomputed and stored
	 * @param order maximum order of the circular harmonics
	 * @param harmonicTypes type of harmonics for the circular harmonics
	 * 
	 * */
	public RieszWaveletConfig(int width, int height, boolean isRealImage, int numScales, IsotropicWaveletType waveletType, boolean prefilter, boolean prepareRieszFilters, int order, HarmonicTypes harmonicTypes)
	{
		this(width, height, isRealImage, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicTypes, true);
	}

	/**
	 * Standard constructor for the Riesz-wavelet frame configuration with similar Riesz transform for each wavelet scale, with optional zero-padding to make input image isotropic.
	 * 
	 * @param width width of the analyzed image
	 * @param height height of the analyzed image
	 * @param isRealImage true if the analyzed image is real
	 * @param numScales number of wavelet scales for the multiscale decomposition
	 * @param waveletType type of radial profile for the wavelet transform
	 * @param prefilter true if low frequency prefiltering is required
	 * @param prepareRieszFilters true if Riesz filters are to be precomputed and stored
	 * @param order maximum order of the circular harmonics
	 * @param harmonicTypes type of harmonics for the circular harmonics
	 * @param isotropicPadding true if zero-padding is to be used for making input image isotropic
	 * */
	public RieszWaveletConfig(int width, int height, boolean isRealImage, int numScales, IsotropicWaveletType waveletType, boolean prefilter, boolean prepareRieszFilters, int order, HarmonicTypes harmonicTypes, boolean isotropicPadding)
	{
		this.width = width;
		this.height = height;
		this.isRealImage = isRealImage;
		if (!isRealImage)
		{
			throw new IllegalArgumentException("Complex images are not yet supported in this version of the wavelet library");
		}
		waveletsConfig = new WaveletFilterSet(waveletType, numScales, prefilter, width, height, isotropicPadding);
		rieszConfigList = new ArrayList<RieszConfig>();
		for (int i = 0; i < numScales; i++)
		{
			if (prepareRieszFilters)
				rieszConfigList.add(new RieszConfig(order, harmonicTypes, waveletsConfig.getScaleWidth(i), waveletsConfig.getScaleHeight(i)));
			else
				rieszConfigList.add(new RieszConfig(order, harmonicTypes));
		}
	}

	/**
	 * Display Riesz transform filters at a given scale as an ICY sequence of images
	 * @param scale scale of the wavelet decomposition
	 * */
	public void displayRieszFilters(int scale)
	{
		RieszConfig config = rieszConfigList.get(scale);
		if (config.filters != null)
		{
			Sequence realSequence = new Sequence();
			realSequence.setName("Real part");
			Sequence imaginarySequence = new Sequence();
			imaginarySequence.setName("Imaginary part");
			for (int c = 0; c < config.numChannels; c++)
			{
				double[] realPart = new double[config.width*config.height];
				double[] imaginaryPart = new double[config.width*config.height];
				for (int i = 0; i < realPart.length; i++)
				{
					realPart[i] = config.filters[c][2*i];
					imaginaryPart[i] = config.filters[c][2*i + 1];
				}
				realSequence.addImage(c, new IcyBufferedImage(waveletsConfig.getScaleWidth(scale), waveletsConfig.getScaleHeight(scale), realPart));
				imaginarySequence.addImage(c, new IcyBufferedImage(waveletsConfig.getScaleWidth(scale), waveletsConfig.getScaleHeight(scale), imaginaryPart));
			}
			Icy.getMainInterface().addSequence(realSequence);
			Icy.getMainInterface().addSequence(imaginarySequence);
		}
		else throw new IllegalArgumentException("Cannot show Riesz filters if they have not been prepared");
	}

	/**
	 * Perform generalized Riesz-wavelet analysis of a 2D image
	 * @param image 2D image to analyze
	 * @param width width of the image
	 * @param height height of the image
	 * @param generalizationList Riesz generalization configuration for each wavelet scale
	 * @return set of Riesz-wavelet coefficients
	 * */
	public RieszWaveletCoefficients multiscaleRieszAnalysis(double[] image, int width, int height, ArrayList<RieszGeneralization> generalizationList)
	{
		// perform wavelet analysis first
		WaveletAnalysisResults waveletProjection = IsotropicWaveletTransform.isotropicBandlimitedAnalysis(image, width, height, waveletsConfig);
		// perform Riesz analysis for each scale
		ArrayList<double[][]> coefficients = new ArrayList<double[][]>(waveletProjection.getNumScales());
		for (int i = 0; i < waveletProjection.getNumScales(); i++)
		{
			double[] band = waveletProjection.getWaveletBand(i);
			RieszConfig rieszConfig = rieszConfigList.get(i);
			RieszGeneralization generalization = generalizationList.get(i);
			double[][] rieszWaveletBand = rieszConfig.analysis(band, generalization);
			coefficients.add(rieszWaveletBand);
		}
		return new RieszWaveletCoefficients(this, generalizationList, coefficients, waveletProjection.getHPResidual(), waveletProjection.getLPResidual(), waveletProjection.getPadX(), waveletProjection.getPadY());
	}
	
	
	/**
	 * Perform generalized Riesz-wavelet analysis of a 2D image in the Fourier domain
	 * @param image Fourier representation of the 2D image to analyze
	 * @param width width of the image
	 * @param height height of the image
	 * @param generalizationList Riesz generalization configuration for each wavelet scale
	 * @return set of Riesz-wavelet coefficients
	 * */
	public RieszWaveletCoefficients multiscaleRieszAnalysisInFourier(double[] image, int width, int height, ArrayList<RieszGeneralization> generalizationList)
	{
		// perform wavelet analysis first
		WaveletAnalysisResults waveletProjection = IsotropicWaveletTransform.isotropicBandlimitedAnalysis(image, width, height, waveletsConfig, true);
		// perform Riesz analysis for each scale
		ArrayList<double[][]> coefficients = new ArrayList<double[][]>(waveletProjection.getNumScales());
		for (int i = 0; i < waveletProjection.getNumScales(); i++)
		{
			double[] band = waveletProjection.getWaveletBand(i);
			RieszConfig rieszConfig = rieszConfigList.get(i);
			RieszGeneralization generalization = generalizationList.get(i);
			double[][] rieszWaveletBand = rieszConfig.analysis(band, generalization, true, false);
			coefficients.add(rieszWaveletBand);
		}
		return new RieszWaveletCoefficients(this, generalizationList, coefficients, waveletProjection.getHPResidual(), waveletProjection.getLPResidual(), waveletProjection.getPadX(), waveletProjection.getPadY());
	}

	/**
	 * Synthesize a 2D image from as set of Riesz-wavelet coefficients
	 * @param coefficients
	 * @param width width of the image
	 * @param height height of the image
	 * */	
	public double[] multiscaleRieszSynthesisInFourier(RieszWaveletCoefficients coefficients, int width, int height)
	{
		double[][] waveletBands = new double[coefficients.getNumScales()][];
		double[] lpResidual = coefficients.getLPResidual();
		double[] hpResidual = coefficients.getHPResidual();
		
		for (int i = 0; i < this.getNumScales(); i++)
		{
			double[][] rieszBands = coefficients.getRieszBandsAtScale(i);
			RieszConfig rieszConfig = rieszConfigList.get(i);
			RieszGeneralization generalization = coefficients.generalizationList.get(i);
			waveletBands[i] = rieszConfig.synthesis(rieszBands, generalization, false, true);
//			Sequence seq = new Sequence("scale "+i);
//			seq.addImage(0, new IcyBufferedImage(rieszConfig.width, rieszConfig.height, waveletBands[i]));
//			Icy.getMainInterface().addSequence(seq);
		}
		WaveletAnalysisResults waveletCoefficients = new WaveletAnalysisResults(waveletBands, lpResidual, hpResidual, coefficients.rieszWaveletConfig.waveletsConfig, true);
		double[] reconstruction = IsotropicWaveletTransform.isotropicBandlimitedSynthesis(waveletCoefficients);
		if (waveletCoefficients.getWaveletFilters().getHeight() > height || waveletCoefficients.getWaveletFilters().getWidth() > width)
			reconstruction = IsotropicWaveletTransform.unpadImage(reconstruction, width, height, waveletCoefficients.getWaveletFilters().getWidth(), waveletCoefficients.getWaveletFilters().getHeight(), coefficients.getPadX(), coefficients.getPadY());
		return reconstruction;
	}
	
	/**
	 * Steer Riesz-wavelet coefficients according to a spatial map of angles
	 * @param coefficients Riesz-wavelet coefficients to steer
	 * @param angleList list of angles for rotation. One 1D array of angles, in radians, per wavelet scale. The 1D array corresponds to a 2D map.
	 * @param forward true for forward steering (rotation with angle 'angles'), else backward steering (rotation with angle '-angles').
	 * */
	public void steerCoefficients(RieszWaveletCoefficients coefficients, ArrayList<double[]> angleList, boolean forward)
	{
		for (int i = 0; i < this.getNumScales(); i++)
		{
			double[][] rieszBands = coefficients.getRieszBandsAtScale(i);
			RieszConfig rieszConfig = rieszConfigList.get(i);
			RieszGeneralization generalization = coefficients.generalizationList.get(i);
			rieszConfig.steerCoefficients(rieszBands, generalization, angleList.get(i), forward);
		}
	}

	/**
	 * Get the number of scales for the wavelet analysis
	 * @return number of wavelet scales
	 * */
	public int getNumScales() {
		return this.waveletsConfig.getNumScales();
	}
	
	/**
	 * Get the configurations for Riesz transform of each wavelet scale
	 * @return list of Riesz transform configurations, one per scale
	 * */
	public  ArrayList<RieszConfig> getRieszConfigurations()
	{
		return new ArrayList<RieszConfig>(this.rieszConfigList);
	}
	
	/**
	 * Get the filters for the wavelet transform
	 * @return filters for the wavelet transform
	 * */
	public WaveletFilterSet getWaveletFilterSet()
	{
		return waveletsConfig;
	}
}