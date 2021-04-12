package plugins.tinevez.kymographtracker;

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

import java.awt.geom.Point2D;

import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.tinevez.kymographtracker.spline.CubicSmoothingSpline;


public class SplineROI extends ROI2DPolyLine
{
	CubicSmoothingSpline xSpline;
	CubicSmoothingSpline ySpline;
	double length;

	public SplineROI(Point2D firstPoint)
	{
		super(firstPoint);
	}
	
	public void setXSpline(CubicSmoothingSpline spline)
	{
		this.xSpline = spline;
	}

	public void setYSpline(CubicSmoothingSpline spline)
	{
		this.ySpline = spline;
	}
	
	public CubicSmoothingSpline getXSpline()
	{
		return xSpline;
	}
	
	public CubicSmoothingSpline getYSpline()
	{
		return ySpline;
	}
	
	public void setLength(double l)
	{
		this.length = l;
	}
	
	public double getLength()
	{
		return length;
	}
}
