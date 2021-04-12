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
