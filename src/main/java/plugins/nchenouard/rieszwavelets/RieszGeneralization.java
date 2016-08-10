package plugins.nchenouard.rieszwavelets;

import java.util.ArrayList;


/**
 * 
 * Riesz transform generalization based on linear combination of circular harmonics
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

public class RieszGeneralization
{
	StandardRieszFrames frameType; // type of standard generalization for the Riesz transform
	double[][] weightMatrixForward; // weight of the linear combination of circular harmonics defining the generalized Riesz transform
	double[][] weightMatrixBackward; // weight of the linear combination of Riesz bands for the backward transform	
	boolean realCoefficients; // true if coefficients are real for a real input, false otherwise

	/**
	 * Initialize the Riesz generalization based on a standard Riesz frame and a given configuration of the Riesz transform
	 * @param frameType standard generalization for the Riesz transform
	 * @param config configuration for the circular harmonics used by the generalized Riesz transform
	 * */
	public RieszGeneralization(StandardRieszFrames frameType, RieszConfig config)
	{
		this.frameType = frameType;

		switch(frameType)
		{
		case CircularHarmonics:
		{
			weightMatrixForward = new double[config.numChannels][2*config.numChannels];
			weightMatrixBackward = weightMatrixForward; // tight frame
			for (int i = 0; i < config.numChannels; i++)
				weightMatrixForward[i][2*i] = 1d/Math.sqrt(config.numChannels);
			realCoefficients = false;
		}
		break;
		case Gradient:
		{
			if (config.order != 1 || config.harmonicType != HarmonicTypes.odd)
			{
				throw new IllegalArgumentException("Gradient frame requires Riesz of order 1 and odd harmonics");
			}
			// U = [1j/2 -1/2; 1j/2 1/2];
			weightMatrixForward = new double[config.numChannels][2*config.numChannels];
			weightMatrixForward[0][0] = 0;
			weightMatrixForward[0][1] = 0.5d;
			weightMatrixForward[0][2] = -0.5d;
			weightMatrixForward[0][3] = 0;
			weightMatrixForward[1][0] = 0;
			weightMatrixForward[1][1] = 0.5d;
			weightMatrixForward[1][2] = 0.5d;
			weightMatrixForward[1][3] = 0;
			weightMatrixBackward = transposeComplexMatrix(weightMatrixForward);
			realCoefficients = true;
		}
		break;
		case Hessian:
		{
			//U = [-1/4 -1/2 -1/4; -1j/(2*sqrt(2)) 0 1j/(2*sqrt(2)); 1/4 -1/2 1/4];
			if (config.order != 2 || config.harmonicType != HarmonicTypes.even)
			{
				throw new IllegalArgumentException("Hessian frame requires Riesz of order 2 and even harmonics");
			}
			weightMatrixForward = new double[config.numChannels][2*config.numChannels];
			weightMatrixForward[0][0] = -1d/4;
			weightMatrixForward[0][1] = 0;
			weightMatrixForward[0][2] = -1d/2;
			weightMatrixForward[0][3] = 0;
			weightMatrixForward[0][4] = -1d/4;
			weightMatrixForward[0][5] = 0;
			weightMatrixForward[1][0] = 0;
			weightMatrixForward[1][1] = -1d/(2*Math.sqrt(2));
			weightMatrixForward[1][2] = 0;
			weightMatrixForward[1][3] = 0;
			weightMatrixForward[1][4] = 0;
			weightMatrixForward[1][5] = 1d/(2*Math.sqrt(2));
			weightMatrixForward[2][0] = 1d/4;
			weightMatrixForward[2][1] = 0;
			weightMatrixForward[2][2] = -1d/2;
			weightMatrixForward[2][3] = 0;
			weightMatrixForward[2][4] = 1d/4;
			weightMatrixForward[2][5] = 0;

			weightMatrixBackward = transposeComplexMatrix(weightMatrixForward);
			realCoefficients = true;
		}
		break;
		case Monogenic:
		{
			// U = [0 1 0; -1j/2 0 -1j/2; 1/2 0 -1/2]/sqrt(2);
			if (config.order != 1 || config.harmonicType != HarmonicTypes.complete)
			{
				throw new IllegalArgumentException("Monogenic frame requires Riesz of order 1 and complete harmonics");
			}
			weightMatrixForward = new double[config.numChannels][2*config.numChannels];
			weightMatrixForward[0][0] = 0;
			weightMatrixForward[0][1] = 0;
			weightMatrixForward[0][2] = 1d/Math.sqrt(2);
			weightMatrixForward[0][3] = 0;
			weightMatrixForward[0][4] = 0;
			weightMatrixForward[0][5] = 0;
			weightMatrixForward[1][0] = 0;
			weightMatrixForward[1][1] = -1d/(2*Math.sqrt(2));
			weightMatrixForward[1][2] = 0;
			weightMatrixForward[1][3] = 0;
			weightMatrixForward[1][4] = 0;
			weightMatrixForward[1][5] = -1d/(2*Math.sqrt(2));
			weightMatrixForward[2][0] = 1d/(2*Math.sqrt(2));
			weightMatrixForward[2][1] = 0;
			weightMatrixForward[2][2] = 0;
			weightMatrixForward[2][3] = 0;
			weightMatrixForward[2][4] = -1d/(2*Math.sqrt(2));
			weightMatrixForward[2][5] = 0;

			weightMatrixBackward = transposeComplexMatrix(weightMatrixForward);
			realCoefficients = false;
		}
		break;
		case Prolate:
			throw new IllegalArgumentException("Prolate Riesz wavelets not implemented yet in the library");
		case ProlateUniSided:
			throw new IllegalArgumentException("Prolate Riesz wavelets not implemented yet in the library");
		case Riesz:
		{
			switch(config.harmonicType)
			{
			case even:
				this.weightMatrixForward = getRieszForwardMatrix(config.order, config.harmonics);
				realCoefficients = true;
				break;
			case odd:
				this.weightMatrixForward = getRieszForwardMatrix(config.order, config.harmonics);
				realCoefficients = true;
				break;
			case complete:
			{
				int orderEven = config.order - (config.order % 2);
				ArrayList<Integer> evenHarmonics = new ArrayList<Integer>();
				ArrayList<Integer> oddHarmonics = new ArrayList<Integer>();
				for (int i = 0; i < config.harmonics.length; i++)
					if (Math.abs(config.harmonics[i])%2 == 0)
						evenHarmonics.add(config.harmonics[i]);
					else
						oddHarmonics.add(config.harmonics[i]);
				int[] evenHarmonicsTab = new int[evenHarmonics.size()];
				for (int i = 0; i <evenHarmonics.size(); i++)
					evenHarmonicsTab[i] = evenHarmonics.get(i);
				int[] oddHarmonicsTab = new int[oddHarmonics.size()];
				for (int i = 0; i <oddHarmonics.size(); i++)
					oddHarmonicsTab[i] = oddHarmonics.get(i);
				double[][] evenMatrix = getRieszForwardMatrix(orderEven, evenHarmonicsTab);
				int orderOdd = config.order + (config.order % 2) - 1;
				double[][] oddMatrix = getRieszForwardMatrix(orderOdd, oddHarmonicsTab);
				this.weightMatrixForward = new double[config.numChannels][2*config.numChannels];
				int idxEven = 0;
				int idxOdd = 0;
				for (int i = 0; i < config.harmonics.length; i++)
				{
					if (Math.abs(config.harmonics[i])%2 == 0)
					{
						int idxEven2 = 0;
						for (int j = 0; j < config.harmonics.length; j++)
						{
							if (Math.abs(config.harmonics[j])%2 == 0)
							{
								this.weightMatrixForward[i][2*j] = evenMatrix[idxEven][2*idxEven2];
								this.weightMatrixForward[i][2*j + 1] = evenMatrix[idxEven][2*idxEven2 + 1];		
								idxEven2 ++;
							}
						}
						idxEven ++;
					}
					else
					{
						int idxOdd2 = 0;
						for (int j = 0; j < config.harmonics.length; j++)
						{
							if (Math.abs(config.harmonics[j])%2 != 0)
							{
								this.weightMatrixForward[i][2*j] = oddMatrix[idxOdd][2*idxOdd2];
								this.weightMatrixForward[i][2*j + 1] = oddMatrix[idxOdd][2*idxOdd2 + 1];		
								idxOdd2 ++;
							}
						}
						idxOdd ++;
					}
				}
				for (int i = 0; i < weightMatrixForward.length; i++)
					for (int j = 0; j < weightMatrixForward[i].length; j++)
						weightMatrixForward[i][j] = weightMatrixForward[i][j]/Math.sqrt(2);
				realCoefficients = true;
			}
			break;
			default:
				throw new IllegalArgumentException("This type of harmonics is not supported for Riesz frames");
			}
			weightMatrixBackward = transposeComplexMatrix(weightMatrixForward);
		}
		break;
		case Simoncelli:
		{
			switch(config.harmonicType)
			{
			case even:
				this.weightMatrixForward = getSimoncelliForwardMatrix(config.order, config.harmonics);
				realCoefficients = true;
				break;
			case odd:
				this.weightMatrixForward = getSimoncelliForwardMatrix(config.order, config.harmonics);
				realCoefficients = true;
				break;
			case complete:
			{
				int orderEven = config.order - (config.order % 2);
				ArrayList<Integer> evenHarmonics = new ArrayList<Integer>();
				ArrayList<Integer> oddHarmonics = new ArrayList<Integer>();
				for (int i = 0; i < config.harmonics.length; i++)
					if (Math.abs(config.harmonics[i])%2 == 0)
						evenHarmonics.add(config.harmonics[i]);
					else
						oddHarmonics.add(config.harmonics[i]);
				int[] evenHarmonicsTab = new int[evenHarmonics.size()];
				for (int i = 0; i <evenHarmonics.size(); i++)
					evenHarmonicsTab[i] = evenHarmonics.get(i);
				int[] oddHarmonicsTab = new int[oddHarmonics.size()];
				for (int i = 0; i <oddHarmonics.size(); i++)
					oddHarmonicsTab[i] = oddHarmonics.get(i);
				double[][] evenMatrix = getSimoncelliForwardMatrix(orderEven, evenHarmonicsTab);
				int orderOdd = config.order + (config.order % 2) - 1;
				double[][] oddMatrix = getSimoncelliForwardMatrix(orderOdd, oddHarmonicsTab);
				this.weightMatrixForward = new double[config.numChannels][2*config.numChannels];
				int idxEven = 0;
				int idxOdd = 0;
				for (int i = 0; i < config.harmonics.length; i++)
				{
					if (Math.abs(config.harmonics[i])%2 == 0)
					{
						int idxEven2 = 0;
						for (int j = 0; j < config.harmonics.length; j++)
						{
							if (Math.abs(config.harmonics[j])%2 == 0)
							{
								this.weightMatrixForward[i][2*j] = evenMatrix[idxEven][2*idxEven2];
								this.weightMatrixForward[i][2*j + 1] = evenMatrix[idxEven][2*idxEven2 + 1];		
								idxEven2 ++;
							}
						}
						idxEven ++;
					}
					else
					{
						int idxOdd2 = 0;
						for (int j = 0; j < config.harmonics.length; j++)
						{
							if (Math.abs(config.harmonics[j])%2 != 0)
							{
								this.weightMatrixForward[i][2*j] = oddMatrix[idxOdd][2*idxOdd2];
								this.weightMatrixForward[i][2*j + 1] = oddMatrix[idxOdd][2*idxOdd2 + 1];		
								idxOdd2 ++;
							}
						}
						idxOdd ++;
					}
				}
				for (int i = 0; i < weightMatrixForward.length; i++)
					for (int j = 0; j < weightMatrixForward[i].length; j++)
						weightMatrixForward[i][j] = weightMatrixForward[i][j]/Math.sqrt(2);
				realCoefficients = true;
			}
			default:
				break;
			}
			weightMatrixBackward = transposeComplexMatrix(weightMatrixForward);
			break;
		}
		case Slepian:
			throw new IllegalArgumentException("Slepian Riesz wavelets not implemented yet in the library");
		}
	}
	/**
	 * Compute factorial of an integer n
	 * @param n
	 * @return factorial of n
	 * */
	private long factorial(int n) {
		long factorial = 1;
		for (int i = 2; i <= n; i++)
			factorial *= i;
		return factorial;
	}

	/**
	 * Display the coefficients of the linear combination of circular harmonics defining the forward generalized Riesz transform
	 * */
	public void printForwardMatrix()
	{
		System.out.println("Forward generalization matrix:");
		if (weightMatrixForward == null)
			System.out.println("null");
		else
		{
			for (int i = 0; i < weightMatrixForward.length; i++)
			{
				for (int j = 0; j < weightMatrixForward[i].length/2; j++)
				{
					System.out.print(weightMatrixForward[i][2*j]+" + "+weightMatrixForward[i][2*j + 1]+"j |");
				}
				System.out.println();
			}
		}
	}

	/**
	 * Display the coefficients of the linear combination of circular harmonics defining the backward generalized Riesz transform
	 * */
	public void printBackwardMatrix()
	{
		System.out.println("Backward generalization matrix:");
		if (weightMatrixBackward == null)
			System.out.println("null");
		else
		{
			for (int i = 0; i < weightMatrixBackward.length; i++)
			{
				for (int j = 0; j < weightMatrixBackward[i].length/2; j++)
				{
					System.out.print(weightMatrixBackward[i][2*j]+" + "+weightMatrixBackward[i][2*j + 1]+"j |");
				}
				System.out.println();
			}
		}
	}

	/**
	 * Compute the complex transpose of a complex matrix
	 * @param matrix input complex matrix as a 2D array. Each subarray corresponds to one line of the matrix
	 * @return the complex transpose of the input matrix
	 * */
	public double[][] transposeComplexMatrix(double[][] matrix) {
		double[][] transposeMatrix = new double[matrix[0].length/2][matrix.length*2];
		for (int i = 0; i < transposeMatrix.length; i++)
			for (int j = 0; j < transposeMatrix[i].length/2; j++)
			{
				transposeMatrix[j][2*i] = matrix[i][2*j];
				transposeMatrix[j][2*i + 1] = -matrix[i][2*j + 1];
			}
		return transposeMatrix;
	}

	/**
	 * Get the forward generalization matrix for the Riesz transform
	 * @param order max order of the circular harmonics
	 * @param harmonics set of harmonics
	 * @return matrix for linear combination of the circular harmonics
	 * */
	public double[][] getRieszForwardMatrix(int order, int[] harmonics)
	{
		double[][] matrix = new double[harmonics.length][2*harmonics.length];
		long factorialOrder = factorial(order);
		int n = (int)(order/2);
		for (int n1 = 0; n1 <= order; n1++)
		{
			int n2 = order - n1;
			long factN1 = factorial(n1);
			long factN2 = factorial(n2);
			//fact = (-1j/2)^order*sqrt(factorial(order)/(factorial(n1)*factorial(n2)))/(1j^n2) % for odd
			double fact1 = Math.sqrt(factorialOrder/(factN1 * factN2))/Math.pow(-2d, order);
			double factR1 = 0;
			double factI1 = 0;
			//switch ((order - n2) % 4)
			switch (n1 % 4)
			{
			case 0:
				factR1 = fact1;
				break;
			case 1:
				factI1 = fact1;
				break;
			case 2:
				factR1 = -fact1;
				break;
			case 3:
				factI1 = -fact1;
				break;
			}
			for (int n1b = 0; n1b <= n1; n1b++)
			{
				long factN1b = factorial(n1b);
				long factN1c = factorial(n1 - n1b);
				for (int n2b = 0; n2b <= n2; n2b++)
				{
					long factN2b = factorial(n2b);
					long factN2c = factorial(n2 - n2b);
					//factb = (-1)^(n2-n2b)*factorial(n1)*factorial(n2)/(factorial(n1b)*factorial(n1-n1b)*factorial(n2b)*factorial(n2-n2b));
					double factb = factN1*factN2/(factN1b*factN1c*factN2b*factN2c);
					if ((n2 - n2b) % 2 > 0) // *(-1)^(n2 - n2b)
						factb = - factb;
					int h;
					if (order % 2 == 0) // even harmonics
						// h = 2*(n1b+n2b-n);
						h = 2*(n1b + n2b - n); // harmonics
					else
						// h = 2*(n1b+n2b-n)-1;
						h = 2*(n1b + n2b - n) - 1; // harmonics
					// U(harmonics==h, n1+1) = U(harmonics==h, n1+1) + fact*factb;
					int idx = -1;
					for (int i = 0; i < harmonics.length; i++)
					{
						if (harmonics[i] == h)
						{
							idx = i;
							break;
						}
					}
					matrix[idx][2*n1] += factR1*factb;
					matrix[idx][2*n1 + 1] += factI1*factb;							
				}
			}
		}
		return matrix;	
	}

	/**
	 * Get the forward generalization matrix for the Simoncelli's pyramid
	 * @param order max order of the circular harmonics
	 * @param harmonics set of harmonics
	 * @return matrix for linear combination of the circular harmonics
	 * */
	public double[][] getSimoncelliForwardMatrix(int order, int[] harmonics)
	{
		int numChannels = harmonics.length;
		int Np = (int) Math.floor(order/2);
		long factOrder = factorial(order);
		double[][] matrix = new double[harmonics.length][2*harmonics.length];
		for (int j = 0; j < numChannels; j++)
		{
			// thetaj = pi*(j-1)/numChannels + pi/2;
			double thetaj = Math.PI*j/numChannels + Math.PI/2;			
			for (int i = 0; i < numChannels; i++)
			{
				// npp = harmonics(i);
				int npp = harmonics[i];
				double fact = 0;
				if (npp % 2 == 0)
					fact = factOrder/(factorial(Np + (int)(npp/2))*factorial(Np - (int)(npp/2)));
				// fact = factorial(order)/(factorial(Np + npp/2)*factorial(Np - npp/2)); // even
				else
					fact = factOrder/(factorial(Np + (int)((npp + 1)/2))*factorial(Np - (int)((npp - 1)/2)));
				// fact = factorial(2*Np+1)/(factorial(Np + (npp+1)/2)*factorial(Np - (npp-1)/2)); // odd
				
//		        U(i, j) = ((-1j)^order)*fact*exp(1j*npp*thetaj);
				switch (order%4)
				{
				case 0:
					matrix[i][2*j] = fact*Math.cos(npp*thetaj);
					matrix[i][2*j + 1] = fact*Math.sin(npp*thetaj);
					break;
				case 1:
					matrix[i][2*j] = fact*Math.sin(npp*thetaj);
					matrix[i][2*j + 1] = -fact*Math.cos(npp*thetaj);
					break;
				case 2:
					matrix[i][2*j] = -fact*Math.cos(npp*thetaj);
					matrix[i][2*j + 1] = -fact*Math.sin(npp*thetaj);
					break;
				case 3:
					matrix[i][2*j] = -fact*Math.sin(npp*thetaj);
					matrix[i][2*j + 1] = fact*Math.cos(npp*thetaj);
					break;
				}
			}
		}
		// normalize
		double sumSq = 0;
		for (int i = 0; i < matrix.length; i++)
			for (int j = 0; j < matrix[i].length; j++)
				sumSq += matrix[i][j]*matrix[i][j];
		sumSq = Math.sqrt(sumSq);
		for (int i = 0; i < matrix.length; i++)
			for (int j = 0; j < matrix[i].length; j++)
				matrix[i][j] = matrix[i][j]/sumSq;
		return matrix;
		// Even
//		harmonics = -order:2:order;
//		Np = order/2;
//		U = zeros(numChannels, numChannels);
//		for j = 1:numChannels;
//		    thetaj = pi*(j-1)/numChannels + pi/2;
//		    for i=1:numChannels
//		        npp = harmonics(i);
//		        fact = factorial(order)/(factorial(Np + npp/2)*factorial(Np - npp/2));
//		        U(i, j) = ((-1j)^order)*fact*exp(1j*npp*thetaj);
//		    end
//		end
//		U = U/sqrt(real(trace(U*U')));% rescale to be tight frame
		
		// Odd
//		Np = floor(order/2);
//		harmonics = (-2*Np-1):2:(2*Np+1);
//		U = zeros(numChannels, numChannels);
//		for j = 1:numChannels,
//		    thetaj = pi*(j-1)/numChannels + pi/2;
//		    for i=1:numChannels
//		        npp = harmonics(i);
//		        fact = factorial(2*Np+1)/(factorial(Np + (npp+1)/2)*factorial(Np - (npp-1)/2));
//		        U(i, j) = ((-1j)^order)*fact*exp(1j*npp*thetaj);
//		    end
//		end
//		U = U/sqrt(real(trace(U*U')));% rescale to be tight frame
	}

	/**
	 * Combine Riesz coefficients in the Fourier domain according to the forward generalization matrix
	 * @param bands Riesz coefficients in the Fourier domain. One subarray per band.
	 * @return combined Riesz coefficients in the Fourier domain
	 * 
	 * */
	public double[][] combineBandsForwardComplex(double[][] bands)
	{
		return combineBandsComplex(bands, weightMatrixForward);
	}

	/**
	 * Combine Riesz coefficients in the Fourier domain according to the backward generalization matrix
	 * @param bands Riesz coefficients in the Fourier domain. One subarray per band.
	 * @return combined Riesz coefficients in the Fourier domain
	 * 
	 * */
	public double[][] combineBandsBackwardComplex(double[][] bands)
	{
		return combineBandsComplex(bands, weightMatrixBackward);
	}

	/**
	 * Combine Riesz coefficients in the Fourier domain according to a given mixing matrix
	 * @param bands Riesz coefficients in the Fourier domain. One subarray per band.
	 * @param weightMatrix matrix defining weights for the linear combination of Riesz bands
	 * @return combined Riesz coefficients in the Fourier domain
	 * 
	 * */
	public double[][] combineBandsComplex(double[][] bands, double[][] weightMatrix)
	{
		int numCoefficients = bands[0].length/2;
		double[][] projectedBands = new double[weightMatrix[0].length/2][numCoefficients*2];
		for (int i = 0; i < projectedBands.length; i++)
		{
			for (int t = 0; t < numCoefficients; t++ )
			{
				double val = 0;
				for (int j = 0; j < bands.length; j++)
					val += bands[j][2*t]*weightMatrix[j][2*i] - bands[j][2*t + 1]*weightMatrix[j][2*i + 1];
				projectedBands[i][2*t] = val;
				val = 0;
				for (int j = 0; j < bands.length; j++)
					val += bands[j][2*t]*weightMatrix[j][2*i + 1] + bands[j][2*t + 1]*weightMatrix[j][2*i];
				projectedBands[i][2*t + 1] = val;

			}
		}
		return projectedBands;
	}
	
	/**
	 * Get the standard Riesz generalization
	 * @return standard Riesz generalization
	 * */
	public StandardRieszFrames getRieszFrame()
	{
		return frameType;
	}
}
