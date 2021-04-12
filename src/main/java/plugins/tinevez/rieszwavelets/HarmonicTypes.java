package plugins.tinevez.rieszwavelets;

/**
 * Types of harmonics for circular harmonic transforms
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

public enum HarmonicTypes {
	 even,// even order harmonics
     odd, // odd order harmonics
     custom, // user-defined harmonics
     complete, // odd and even order harmonics
     positive // positive order harmonics
}
