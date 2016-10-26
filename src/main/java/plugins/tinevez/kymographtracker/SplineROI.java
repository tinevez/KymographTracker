package plugins.tinevez.kymographtracker;

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
