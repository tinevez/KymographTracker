package plugins.nchenouard.kymographtracker;

import icy.roi.ROI;
import icy.sequence.Sequence;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;

public class Util {


	static float shiftDrawX  = 0.5f;
	static float shiftDrawY  = 0.5f;

	static ROI convertPathToROI(Sequence sequence, ArrayList<double[][]> pathList)
	{
		return convertPathToSmoothedROI(sequence, pathList);				
//				double prevLastPosX = -1;
//				double prevLastPosY = -1;
//				ROI2DPolyLine polylineROI = null;
//				int k = 0;
//				if (!pathList.isEmpty())
//				{
//					for (double[][] path:pathList)
//					{
//						if (path[0][0] == prevLastPosX && path[0][1] == prevLastPosY) // segments start at the end of the previous one, so continue adding points to the existing roi
//						{
//							for (int i = 1; i < path.length; i ++)	
//							{
//								polylineROI.addPoint(new Point2D.Double(path[i][0] + shiftDrawX, path[i][1] + shiftDrawY), true);
//							}
//						}
//						else // start of the segment does not correspond to the end of the previous segment, so create a new roi
//						{
//							polylineROI = new ROI2DPolyLine(new Point2D.Double(path[0][0] + shiftDrawX, path[0][1] + shiftDrawY));
//							for (int i = 1; i < path.length; i ++)	
//							{
//								polylineROI.addPoint(new Point2D.Double(path[i][0] + shiftDrawX, path[i][1] + shiftDrawY), true);
//							}
//						}
//						prevLastPosX = path[path.length - 1][0];
//						prevLastPosY = path[path.length - 1][1];
//					}	
//				}
//				polylineROI.setSelected(false);
//				return polylineROI;
	}

	

	static ROI convertPathToSmoothedROI(Sequence sequence, ArrayList<double[][]> pathList)
	{		
		double prevLastPosX = -1;
		double prevLastPosY = -1;
		double length = 0;
//		SplineROI polylineROI = null;
		ROI2DPolyLine polylineROI = null;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y1 = new ArrayList<Double>();
		ArrayList<Double> y2 = new ArrayList<Double>();
		ArrayList<Double> w = new ArrayList<Double>();

		if (!pathList.isEmpty())
		{
			for (double[][] path:pathList)
			{
				for (int i = 0; i < path.length; i++)
				{
					if (!(prevLastPosX == path[i][0] && prevLastPosY == path[i][1]))
					{
						if (prevLastPosX != -1 && prevLastPosY != -1)
							length += Math.sqrt((path[i][0] - prevLastPosX)*(path[i][0] - prevLastPosX) + (path[i][1] - prevLastPosY)*(path[i][1] - prevLastPosY));
						x.add(length);
						y1.add(path[i][0]);
						y2.add(path[i][1]);
						prevLastPosX = path[i][0];
						prevLastPosY = path[i][1];						
						w.add(1d);
					}
				}
				prevLastPosX = path[path.length - 1][0];
				prevLastPosY = path[path.length - 1][1];
			}
			
			double[] xTab = new double[x.size()];
			double[] y1Tab = new double[x.size()];
			double[] y2Tab = new double[x.size()];
			double[] wTab = new double[x.size()];
			for (int i = 0; i < xTab.length; i++)
			{
				xTab[i] = x.get(i);
				y1Tab[i] = y1.get(i);
				y2Tab[i] = y2.get(i);
				wTab[i] = w.get(i);
			}

			double rho = 0.5d;
			CubicSmoothingSpline smoothingSpline1 = new CubicSmoothingSpline(xTab, y1Tab, wTab, rho);
			CubicSmoothingSpline smoothingSpline2 = new CubicSmoothingSpline(xTab, y2Tab, wTab, rho);
			double l = 0;
			while (l < length)
			{
				double xEval = smoothingSpline1.evaluate(l);
				double yEval = smoothingSpline2.evaluate(l);
				if (l == 0)
					polylineROI = new ROI2DPolyLine(new Point2D.Double(xEval + shiftDrawX, yEval + shiftDrawY));
				else
					polylineROI.addPoint(new Point2D.Double(xEval + shiftDrawX, yEval + shiftDrawY), true);					
				l += 1;
			}
//			polylineROI.setXSpline(smoothingSpline1);
//			polylineROI.setYSpline(smoothingSpline2);
//			polylineROI.setLength(length);
		}
		polylineROI.setSelected(false);
		return polylineROI;
	}



	public static CubicSmoothingSpline getXsplineFromROI(ROI2DShape roi)
	{
		ArrayList<Point2D> pointList = roi.getPoints();
		double prevLastPosX = -1;
		double prevLastPosY = -1;
		double length = 0;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y = new ArrayList<Double>();
		ArrayList<Double> w = new ArrayList<Double>();
		for (Point2D p:pointList)
		{
			double xx = p.getX();
			double yy = p.getY();
			if (!(prevLastPosX == xx && prevLastPosY == yy))
			{
				if (prevLastPosX != -1 && prevLastPosY != -1)
					length += Math.sqrt((xx - prevLastPosX)*(xx - prevLastPosX) + (yy - prevLastPosY)*(yy - prevLastPosY));
				x.add(length);
				y.add(xx);
				prevLastPosX = xx;
				prevLastPosY = yy;						
				w.add(1d);
			}
		}
		
		double[] xTab = new double[x.size()];
		double[] yTab = new double[x.size()];
		double[] wTab = new double[x.size()];
		for (int i = 0; i < xTab.length; i++)
		{
			xTab[i] = x.get(i);
			yTab[i] = y.get(i);
			wTab[i] = w.get(i);
		}
		double rho = 0.5d;
		CubicSmoothingSpline smoothingSpline = new CubicSmoothingSpline(xTab, yTab, wTab, rho);
		return smoothingSpline;
	}
	
	
	public static CubicSmoothingSpline getYsplineFromROI(ROI2DShape roi)
	{
		ArrayList<Point2D> pointList = roi.getPoints();
		double prevLastPosX = -1;
		double prevLastPosY = -1;
		double length = 0;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y = new ArrayList<Double>();
		ArrayList<Double> w = new ArrayList<Double>();
		for (Point2D p:pointList)
		{
			double xx = p.getX();
			double yy = p.getY();
			if (!(prevLastPosX == xx && prevLastPosY == yy))
			{
				if (prevLastPosX != -1 && prevLastPosY != -1)
					length += Math.sqrt((xx - prevLastPosX)*(xx - prevLastPosX) + (yy - prevLastPosY)*(yy - prevLastPosY));
				x.add(length);
				y.add(yy);
				prevLastPosX = xx;
				prevLastPosY = yy;						
				w.add(1d);
			}
		}
		
		double[] xTab = new double[x.size()];
		double[] yTab = new double[x.size()];
		double[] wTab = new double[x.size()];
		for (int i = 0; i < xTab.length; i++)
		{
			xTab[i] = x.get(i);
			yTab[i] = y.get(i);
			wTab[i] = w.get(i);
		}
		double rho = 0.5d;
		CubicSmoothingSpline smoothingSpline = new CubicSmoothingSpline(xTab, yTab, wTab, rho);
		return smoothingSpline;
	}

	public static double getSplineLength(ROI2DShape roi)
	{
		ArrayList<Point2D> pointList = roi.getPoints();
		double prevLastPosX = -1;
		double prevLastPosY = -1;
		double length = 0;
		for (Point2D p:pointList)
		{
			double xx = p.getX();
			double yy = p.getY();
			if (!(prevLastPosX == xx && prevLastPosY == yy))
			{
				if (prevLastPosX != -1 && prevLastPosY != -1)
					length += Math.sqrt((xx - prevLastPosX)*(xx - prevLastPosX) + (yy - prevLastPosY)*(yy - prevLastPosY));
				prevLastPosX = xx;
				prevLastPosY = yy;
			}
		}
		return length;
	}
}