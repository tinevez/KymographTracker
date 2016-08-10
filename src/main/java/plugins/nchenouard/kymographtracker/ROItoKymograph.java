package plugins.nchenouard.kymographtracker;

import java.awt.geom.PathIterator;
import java.util.ArrayList;

import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.isotropicwavelets.IsotropicWaveletType;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;
import plugins.nchenouard.rieszwavelets.HarmonicTypes;
import plugins.nchenouard.rieszwavelets.RieszConfig;
import plugins.nchenouard.rieszwavelets.RieszGeneralization;
import plugins.nchenouard.rieszwavelets.RieszWaveletCoefficients;
import plugins.nchenouard.rieszwavelets.RieszWaveletConfig;
import plugins.nchenouard.rieszwavelets.StandardRieszFrames;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;


public class ROItoKymograph{

	Thread computeThread;
	double resamplingStep = 1;
	double diskRadius = 2;

	public ROItoKymograph(){}

	public ROItoKymograph(double diskRadius, double resamplingStep)
	{
		this.diskRadius = diskRadius;
		this.resamplingStep = resamplingStep;
	}

	public ArrayList<double[]> resamplePositions( ArrayList<double[]> positions, double step)
	{
		ArrayList<double[]> resampledPositions = new ArrayList<double[]>();
		if (!positions.isEmpty())
		{
			double prevX = positions.get(0)[0];
			double prevY = positions.get(0)[1];
			double t = 0;
			//resampledPositions.add(new double[]{prevX, prevY});
			int i = 1;
			double currentStep = step;
			while (i < positions.size())
			{
				double x = positions.get(i)[0];
				double y = positions.get(i)[1];
				double currentX = prevX;
				double currentY = prevY;
				double intervalSize = Math.sqrt((prevX - x)*(prevX - x) + (prevY - y)*(prevY - y));
				double nextPostT = t + intervalSize;
				while(t + currentStep <= nextPostT)
				{
					t = t + currentStep;
					currentX = prevX + (1 - (nextPostT - t)/intervalSize)*(x - prevX);
					currentY = prevY + (1 - (nextPostT - t)/intervalSize)*(y - prevY);
					resampledPositions.add(new double[]{currentX, currentY});
					currentStep = step; // reset the current step size to the default one
				}
				prevX = currentX;
				prevY = currentY;
				// then change the step size to get to the next control point
				currentStep = step -  Math.sqrt((prevX - x)*(prevX - x) + (prevY - y)*(prevY - y));
				t = t + Math.sqrt((prevX - x)*(prevX - x) + (prevY - y)*(prevY - y));
				prevX = x;
				prevY = y;
				i++;
			}
		}
		return resampledPositions;
	}

	public ArrayList<double[]> computeSamplingPositions(Sequence sequence, ROI2DShape roi, boolean shiftFromView)
	{
		double shiftX = 0.0d;
		double shiftY = 0.0d;
		if (shiftFromView)
		{
			shiftX = -0.5d;
			shiftY = -0.5d;
		}
		PathIterator pathIterator = roi.getPathIterator(null);
		double[] coords = new double[6];
		samplingPositions = new ArrayList<double[]>();
		while(!pathIterator.isDone())
		{
			int segType = pathIterator.currentSegment(coords);
			switch(segType)
			{
			case PathIterator.SEG_CLOSE:
				break;
			case PathIterator.SEG_CUBICTO:
				break;
			case PathIterator.SEG_LINETO:
				samplingPositions.add(new double[]{coords[0] + shiftX, coords[1] + shiftY});
				break;
			case PathIterator.SEG_MOVETO:
				samplingPositions.add(new double[]{coords[0] + shiftX, coords[1] + shiftY});
				break;
			case PathIterator.SEG_QUADTO:
				break;
			}
			pathIterator.next();
		}
		samplingPositions = resamplePositions(samplingPositions, resamplingStep);
		return samplingPositions;
	}


	public Sequence[] separateKymograph(Sequence kymographSeq)
	{
		// separate anterograde and retrograde traces

		// compute the Simoncelli's wavelet representation of the sequence
		int height = kymographSeq.getSizeY();
		int width = kymographSeq.getSizeX();
		boolean isRealImage = true;		
		// Wavelets
		int numScales = 4;
		boolean prefilter = false;
		IsotropicWaveletType waveletType = IsotropicWaveletType.Simoncelli;
		// Riesz transform
		int order = 6;
		HarmonicTypes harmonicType = HarmonicTypes.even;
		boolean prepareRieszFilters = true;

		RieszWaveletConfig config = new RieszWaveletConfig(width, height, isRealImage, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicType, false);		
		ArrayList<RieszConfig> rieszConfigList = config.getRieszConfigurations();

		// compute generalization
		ArrayList<RieszGeneralization> generalizationList = new ArrayList<RieszGeneralization>();
		for (int i = 0; i < numScales; i++)
		{
			RieszGeneralization rieszGeneralization = new RieszGeneralization(StandardRieszFrames.Simoncelli, rieszConfigList.get(i));
			generalizationList.add(rieszGeneralization);
		}

		// apply the riesz transforms to the wavelet scales
		double[] image = kymographSeq.getDataXYAsDouble(0, 0, 0);
		RieszWaveletCoefficients anteroCoefficients  = config.multiscaleRieszAnalysisInFourier(image, width, height, generalizationList);

		ArrayList<double[][]> retroBands = new ArrayList<double[][]>(anteroCoefficients.getNumScales());
		for (int i = 0; i < anteroCoefficients.getNumScales(); i++)
		{
			double[][] anteroCoeffs = anteroCoefficients.getRieszBandsAtScale(i);
			double[][] retroCoeffs = new double[anteroCoeffs.length][];
			for (int j = 0; j < anteroCoeffs.length; j++)
				retroCoeffs[j] = anteroCoeffs[j].clone();
			retroBands.add(retroCoeffs);
		}
		double[] retroHPResidual = null;
		if (anteroCoefficients.getHPResidual() != null)
			retroHPResidual = anteroCoefficients.getHPResidual().clone();
		double[] retroLPResidual = anteroCoefficients.getLPResidual().clone();
		RieszWaveletCoefficients retroCoefficients  = new RieszWaveletCoefficients(anteroCoefficients.getConfig(), generalizationList, retroBands, retroHPResidual, retroLPResidual, anteroCoefficients.getPadX(), anteroCoefficients.getPadY());
		// set to 0 half of the bands
		for (int i = 0; i < retroCoefficients.getNumScales(); i++)
		{
			double[][] retroCoeffs = retroCoefficients.getRieszBandsAtScale(i);
			for (int j = 0; j < retroCoeffs.length/2; j++)
				for (int k = 0; k < retroCoeffs[j].length; k++)
					retroCoeffs[j][k] = 0;
		}
		// diminish the lowPass residual
		for (int k = 0; k < retroLPResidual.length; k++)
			retroLPResidual[k] /= 2; 

		// reconstruct image from coefficients
		Sequence retroSeq = new Sequence();
		double[] reconstructedImage = config.multiscaleRieszSynthesisInFourier(retroCoefficients, width, height);
		retroSeq.addImage(0, new IcyBufferedImage(width, height, reconstructedImage));		

		for (int i = 0; i < anteroCoefficients.getNumScales(); i++)
		{
			double[][] anteroCoeffs = anteroCoefficients.getRieszBandsAtScale(i);
			for (int j = ((int) anteroCoeffs.length/2) + 1; j < anteroCoeffs.length; j++)
				for (int k = 0; k < anteroCoeffs[j].length; k++)
					anteroCoeffs[j][k] = 0;
		}
		// diminish the lowPass residual
		double[] anteroLPresidual = anteroCoefficients.getLPResidual();
		for (int k = 0; k < anteroLPresidual.length; k++)
			anteroLPresidual[k] /= 2; 
		Sequence anteroSeq = new Sequence();
		double[] reconstructedImage2 = config.multiscaleRieszSynthesisInFourier(anteroCoefficients, width, height);
		anteroSeq.addImage(0, new IcyBufferedImage(width, height, reconstructedImage2));		

		return new Sequence[]{kymographSeq, anteroSeq, retroSeq};
	}

	public Sequence[] getAnteroRetroKymographSequence(Sequence sequence, double length, CubicSmoothingSpline xSpline, CubicSmoothingSpline ySpline)
	{

		Sequence kymographSeq = getKymographSequence(sequence, length, xSpline, ySpline);
		return separateKymograph(kymographSeq);
	}

	public Sequence[] getAnteroRetroKymographSequenceFromDisks(Sequence sequence, ArrayList<double[]> samplingPositions)
	{
		Sequence kymographSeq = getKymographSequenceFromDisks(sequence, samplingPositions);
		return separateKymograph(kymographSeq);
	}	

	ArrayList<double[]> samplingPositions;
	public Sequence getKymographSequence(Sequence sequence, double length, CubicSmoothingSpline xSpline, CubicSmoothingSpline ySpline)
	{
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		samplingPositions = new ArrayList<double[]>();
		//		ArrayList<double[]> samplingPositions = new ArrayList<double[]>();
		//		ArrayList<double[]> orthogonalVectors = new ArrayList<double[]>();
		double l = 0;
		while (l < length)
		{
			//			samplingPositions.add(new double[]{xSpline.evaluate(l), ySpline.evaluate(l)});
			//			orthogonalVectors.add(new double[]{ySpline.derivative(l), -xSpline.derivative(l)});
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = xSpline.evaluate(l);
			double y = ySpline.evaluate(l);
			samplingPositions.add(new double[]{x, y});
			double dx = xSpline.derivative(l);
			double dy = ySpline.derivative(l);
			double ux = dy/Math.sqrt(dx*dx + dy*dy);
			double uy = -dx/Math.sqrt(dx*dx + dy*dy);
			double tt = -diskRadius;
			while (tt <= diskRadius)
			{
				int xx = (int) Math.round(x + tt*ux);
				int yy = (int) Math.round(y + tt*uy);
				if (xx >= 0 && xx < sequence.getSizeX() && yy >= 0 && yy < sequence.getSizeY())
				{
					mask.add(new int[]{xx, yy});
				}
				tt += 1d;
			}
			masks.add(mask);			
			l += resamplingStep;
		}
		Sequence kymographSeq = new Sequence();
		kymographSeq.setImage(0, 0, new IcyBufferedImage(masks.size(), sequence.getSizeT(), 1, DataType.DOUBLE));
		double[] tabValues = kymographSeq.getImage(0, 0, 0).getDataXYAsDouble(0);
		for (int t = 0; t < sequence.getSizeT(); t++)
		{
			//for (double[] p:positions)
			int cnt = 0;
			for (ArrayList<int[]> mask:masks)
			{
				//tabValues[cnt + t*kymographSeq.getSizeX()] = sequence.getData(t, 0, 0, (int)Math.round(p[1]), (int)Math.round(p[0]));
				double sum = 0;
				for (int[] m:mask)
					sum += sequence.getData(t, 0, 0, m[1], m[0]);
				if (mask.size() > 1)
					sum = sum/mask.size();
				tabValues[cnt + t*kymographSeq.getSizeX()] = sum;
				cnt ++;
			}
		}
		kymographSeq.dataChanged();
		return kymographSeq;
	}

	public Sequence getKymographSequenceFromDisks(Sequence sequence, ArrayList<double[]> samplingPositions)
	{
		// for each position create a binary disk to average in
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		for (double[] p:samplingPositions)
		{
			int minX = (int)Math.floor(p[0] - diskRadius);
			if (minX < 0)
				minX = 0;
			if (minX > sequence.getSizeX())
				minX = sequence.getSizeX()-1;
			int maxX = (int)Math.ceil(p[0] + diskRadius);
			if (maxX < 0)
				maxX = 0;
			if (maxX >= sequence.getSizeX())
				maxX = sequence.getSizeX()-1;
			int minY = (int)Math.floor(p[1] - diskRadius);
			if (minY < 0)
				minY = 0;
			if (minY > sequence.getSizeY())
				minY = sequence.getSizeY()-1;
			int maxY = (int)Math.ceil(p[1] + diskRadius);
			if (maxY < 0)
				maxY = 0;
			if (maxY >= sequence.getSizeY())
				maxY = sequence.getSizeY()-1;
			ArrayList<int[]> mask = new ArrayList<int[]>();
			for ( int y = minY; y <= maxY; y++)
				for ( int x = minX; x <= maxX; x++)
				{
					if ( (p[0] - x) * (p[0] - x) + (p[1] - y) *(p[1] - y) <= diskRadius*diskRadius)
						mask.add(new int[]{x, y});
				}
			masks.add(mask);
		}
		Sequence kymographSeq = new Sequence();
		kymographSeq.setImage(0, 0, new IcyBufferedImage(samplingPositions.size(), sequence.getSizeT(), 1, DataType.DOUBLE));
		double[] tabValues = kymographSeq.getImage(0, 0, 0).getDataXYAsDouble(0);
		for (int t = 0; t < sequence.getSizeT(); t++)
		{
			//for (double[] p:positions)
			int cnt = 0;
			for (ArrayList<int[]> mask:masks)
			{
				//tabValues[cnt + t*kymographSeq.getSizeX()] = sequence.getData(t, 0, 0, (int)Math.round(p[1]), (int)Math.round(p[0]));
				double sum = 0;
				for (int[] m:mask)
					sum += sequence.getData(t, 0, 0, m[1], m[0]);
				if (mask.size() > 1)
					sum = sum/mask.size();
				tabValues[cnt + t*kymographSeq.getSizeX()] = sum;
				cnt ++;
			}
		}
		kymographSeq.dataChanged();
		return kymographSeq;		
	}	

	class CreateKymographThread extends Thread
	{
		ArrayList<ROI> rois;
		Sequence seq;

		protected CreateKymographThread(ArrayList<ROI> rois, Sequence seq)
		{
			this.rois = rois;
			this.seq = seq;
		}

		@Override
		public void run()
		{
			AnnounceFrame announce = new AnnounceFrame("Create of Kymograph images started");
			ArrayList<ROI> selectedROIs = new ArrayList<ROI>();
			for (ROI roi:rois)
			{
				if (roi instanceof ROI2DShape && roi.isSelected())
					selectedROIs.add(roi);
			}
			if (selectedROIs.isEmpty())
				for (ROI roi:rois)
				{
					if (roi instanceof ROI2DShape)
						selectedROIs.add(roi);
				}
			if (selectedROIs.isEmpty())
				return;
			ProgressFrame bar = new ProgressFrame("Processing started");
			bar.setLength(selectedROIs.size());			
			int cnt = 0;
			for (ROI roi:selectedROIs)
			{
				bar.setMessage("Processing roi "+roi.getName());
				bar.setPosition(cnt);
				//				ArrayList<double[]> samplingPositions = computeSamplingPositions(seq, (ROI2DShape)roi, true);
				//				Sequence kymograph = getKymographSequence(seq, samplingPositions);

				Sequence kymograph = null;
//				if (roi instanceof SplineROI)
//				{
//					SplineROI roiSpline = (SplineROI) roi;
//					kymograph = getKymographSequence(seq, roiSpline.length, roiSpline.xSpline, roiSpline.ySpline);		
//				}
//				else
				{
					CubicSmoothingSpline xSpline = Util.getXsplineFromROI((ROI2DShape) roi);
					CubicSmoothingSpline ySpline = Util.getYsplineFromROI((ROI2DShape) roi);
					double length = Util.getSplineLength((ROI2DShape) roi);
					kymograph = getKymographSequence(seq, length, xSpline, ySpline);
				}

				kymograph.setName(seq.getName()+"_"+roi.getName()+"_kymograph");
				Icy.getMainInterface().addSequence(kymograph);
			}
			bar.close();
			announce.close();
		}
	}
}
