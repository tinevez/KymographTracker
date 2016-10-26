package plugins.tinevez.rieszwavelets;

/**
 * 
 * Standard types of generalized Riesz frames
 * 
 * For a mathematical description see:
 * 
 * A Unifying Parametric Framework for 2D Steerable Wavelet Transforms
 * Unser, M. and Chenouard, N.
 * SIAM Journal on Imaging Sciences 2013 6:1, 102-135 
 * 
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 * @date 2014-05-25
 * @license gpl v3.0
 */

public enum StandardRieszFrames {
	Riesz,
	Simoncelli,
	CircularHarmonics,
	Prolate,
	ProlateUniSided,
	Slepian,
	Gradient,
	Monogenic,
	Hessian,
}
