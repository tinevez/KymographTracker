package plugins.nchenouard.rieszwavelets;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.util.ArrayList;

/**
 * 
 * Riesz-wavelet transform coefficients
 * 
 * For a description of the transforms, see:
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

public class RieszWaveletCoefficients
{
	RieszWaveletConfig rieszWaveletConfig; // configuration of the circular harmonics used for generalized Riesz transform
	ArrayList<RieszGeneralization> generalizationList; // set of generalization configurations for wavelet scales
	double[] hpResidual; // high frequency residual
	double[] lpResidual; // low frequency residual
	ArrayList<double[][]> rieszWaveletBandMultiscale; // set of Riesz-wavelet coefficients. Each item of the array corresponds to one scale of the wavelet transform.
	int padX; // x direction zero-padding size
	int padY; // y direction zero-padding size

	public RieszWaveletCoefficients(RieszWaveletConfig config, ArrayList<RieszGeneralization> generalizationList, ArrayList<double[][]> bands, double[] hpResidual, double[] lpResidual, int padX, int padY)
	{
		this.rieszWaveletConfig = config;
		this.generalizationList = generalizationList;
		this.hpResidual = hpResidual;
		this.lpResidual = lpResidual;
		this.rieszWaveletBandMultiscale = bands;
		this.padX = padX;
		this.padY = padY;
	}
	
	/**
	 * Duplicate coefficients
	 * */
	public RieszWaveletCoefficients duplicateCoefficients()
	{
		ArrayList<RieszGeneralization> generalizationList2 = new ArrayList<RieszGeneralization>(generalizationList);
		ArrayList<double[][]> bands2 = new ArrayList<double[][]>(rieszWaveletBandMultiscale.size());
		for (int i = 0; i < rieszWaveletBandMultiscale.size(); i++)
		{
			double[][] b = new double[rieszWaveletBandMultiscale.get(i).length][];
			for (int j = 0; j < b.length; j++)
				b[j] = rieszWaveletBandMultiscale.get(i)[j].clone();
			bands2.add(b);
		}
		double[] hpResidual2 = null;
		if (hpResidual != null)
			hpResidual2 = hpResidual.clone();
		double[] lpResidual2 = null;
		if (lpResidual != null)
			lpResidual2 = lpResidual.clone();
		return new RieszWaveletCoefficients(rieszWaveletConfig, generalizationList2, bands2, hpResidual2, lpResidual2, padX, padY);
	}
	
	/**
	 * Get the Riesz coefficients at a given wavelet scale
	 * @param scale scale of the wavelet decomposition
	 * @return set of Riesz-wavelet coefficients
	 * */
	public double[][] getRieszBandsAtScale(int scale)
	{
		return rieszWaveletBandMultiscale.get(scale);
	}
	
	/**
	 * Get the high frequency residual
	 * @return high frequency residual as a 1D array
	 * */
	public double[] getHPResidual()
	{
		return hpResidual;
	}

	/**
	 * Get the low frequency residual
	 * @return low frequency residual as a 1D array
	 * */
	public double[] getLPResidual()
	{
		return lpResidual;
	}
	
	/**
	 * Set the Riesz coefficients at a given scale
	 * @param scale scale of the wavelet decomposition
	 * @param coefficients Riesz coefficients
	 * */
	public void setRieszBandsAtScale(int scale, double[][] coefficients)
	{
		this.rieszWaveletBandMultiscale.set(scale, coefficients);
	}
	
	/**
	 * Set the high frequency residual
	 * @param residual high frequency residual as a 1D array
	 * */
	public void setLPResidual(double[] residual)
	{
		lpResidual = residual;
	}

	/**
	 * Set the low frequency residual
	 * @param residual low frequency residual as a 1D array
	 * */
	public void setHPResidual(double[] residual)
	{
		hpResidual = residual;
	}
	
	/**
	 * Get the configuration of the Riesz-wavelet frame
	 * @return configuration of the Riesz-wavelet frame
	 * */
	public RieszWaveletConfig getConfig()
	{
		return rieszWaveletConfig;
	}

	/**
	 * Display the Riesz-wavelet coefficient at a given scale as a ICY sequence of image
	 * @param scale scale of the wavelet decomposition
	 * */
	public void displayCoefficients(int scale)
	{
		RieszGeneralization generalization = generalizationList.get(scale);
		double[][] rieszWaveletBand = getRieszBandsAtScale(scale);
		if (generalization.realCoefficients)
		{
			Sequence realSequence = new Sequence();
			realSequence.setName("Real part at scale"+ scale);
			for (int j = 0; j < rieszWaveletBand.length; j++)
			{
				realSequence.setImage(0, j, new IcyBufferedImage(rieszWaveletConfig.waveletsConfig.getScaleWidth(scale), rieszWaveletConfig.waveletsConfig.getScaleHeight(scale), rieszWaveletBand[j]));
			}
			Icy.getMainInterface().addSequence(realSequence);
		}
		else
		{
			Sequence realSequence = new Sequence();
			realSequence.setName("Real part at scale "+scale);
			Sequence imaginarySequence = new Sequence();
			imaginarySequence.setName("Imaginary part at scale "+scale);

			for (int j = 0; j < rieszWaveletBand.length; j++)
			{
				double[] realPart = new double[rieszWaveletBand[j].length/2];
				double[] imaginaryPart = new double[rieszWaveletBand[j].length/2];
				for (int t = 0; t < realPart.length; t++)
				{
					realPart[t] = rieszWaveletBand[j][2*t];
					imaginaryPart[t] = rieszWaveletBand[j][2*t + 1];;
				}
				realSequence.addImage(j, new IcyBufferedImage(rieszWaveletConfig.waveletsConfig.getScaleWidth(scale), rieszWaveletConfig.waveletsConfig.getScaleHeight(scale), realPart));
				imaginarySequence.addImage(j, new IcyBufferedImage(rieszWaveletConfig.waveletsConfig.getScaleWidth(scale), rieszWaveletConfig.waveletsConfig.getScaleHeight(scale), imaginaryPart));
			}
			Icy.getMainInterface().addSequence(realSequence);
			Icy.getMainInterface().addSequence(imaginarySequence);
		}
	}

	/**
	 * Get the number of scales of the Riesz-wavelet decomposition
	 * @return number of scales
	 * */
	public int getNumScales() {
		return rieszWaveletConfig.getNumScales();
	}
	
	/**
	 * Get the zero-padding size in x direction
	 * @return zero-padding size in x direction
	 * */
	public int getPadX()
	{
		return padX;
	}

	/**
	 * Get the zero-padding size in y direction
	 * @return zero-padding size in y direction
	 * */
	public int getPadY()
	{
		return padY;
	}
	
	/**
	 * Get the set of Riesz generalization configuration for wavelet scales
	 * @return array of Riesz generalization configuration, one per wavelet scale.
	 * */
	public ArrayList<RieszGeneralization> getGeneralizationList()
	{
		return new ArrayList<RieszGeneralization>(generalizationList);
	}
}
