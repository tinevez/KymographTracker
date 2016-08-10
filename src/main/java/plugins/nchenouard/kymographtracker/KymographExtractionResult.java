package plugins.nchenouard.kymographtracker;

import icy.file.FileUtil;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.main.GlobalSequenceListener;
import icy.main.Icy;
import icy.roi.ROI;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.sequence.SequenceListener;
import icy.util.XMLUtil;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Element;

public class KymographExtractionResult implements SequenceListener, GlobalSequenceListener{
	ROI roi;
	Sequence sourceSequence;
	private Sequence kymograph;
	private Sequence anterogradeKymograph;
	private Sequence retrogradeKymograph;

	ArrayList<ROI> kymoROIs = new ArrayList<ROI>();
	ArrayList<ROI> anteroKymoROIs = new ArrayList<ROI>();
	ArrayList<ROI> retroKymoROIs = new ArrayList<ROI>();

	boolean anterogradeRetrogradeSeparation;
	ArrayList<double[]> samplingPositions;

	KymographTrackingResults trackingResults;
	KymographTrackingResults anterogradeTrackingResults;
	KymographTrackingResults retrogradeTrackingResults;

	public static String KYMO_RESULT = "KymographTrackingResults";

	public static String SEQUENCE_NAME = "SequenceName";
	public static String SAMPLING_POSITIONS = "SamplingPositions";
	public static String SAMPLING_X = "SamplingPositionsX";
	public static String SAMPLING_Y = "SamplingPositionsY";


	public static String IS_ANTERO_RETRO_SPLIT = "AnteroRetroSplit";

	public static String BIDIRECTIONAL_TRACKING = "BidirectionalTracking";
	public static String ANTEROGRADE_TRACKING = "AnterogradeTracking";
	public static String RETROGRADE_TRACKING = "RetrogradeTracking";

	public static String TRACKING_RESULTS = "KymoTrackingResults";	
	public static String KYMO_SEQUENCE_NAME = "KymoSequenceName";

	public static String NULL_ELEMENT = "null";

	public KymographExtractionResult()
	{
		Icy.getMainInterface().addGlobalSequenceListener(this);
	}

	public void saveToXML(Element node, File saveDir, int idx)
	{
		final Element nodeKymoResults =   XMLUtil.addElement(node, KYMO_RESULT);
		if (sourceSequence == null || sourceSequence.getName() == null)
			XMLUtil.setAttributeValue(nodeKymoResults, SEQUENCE_NAME, NULL_ELEMENT);
		else
			XMLUtil.setAttributeValue(nodeKymoResults, SEQUENCE_NAME, sourceSequence.getName());
		XMLUtil.setAttributeBooleanValue(nodeKymoResults, IS_ANTERO_RETRO_SPLIT, anterogradeRetrogradeSeparation);
		final Element samplingNode =   XMLUtil.setElement(nodeKymoResults, SAMPLING_POSITIONS);
		String samplingPos = "";
		for (double[] d:samplingPositions)
			samplingPos = samplingPos.concat(Double.toString(d[0]) +", ");		
		XMLUtil.setAttributeValue(samplingNode, SAMPLING_X, samplingPos);
		samplingPos = "";
		for (double[] d:samplingPositions)
			samplingPos = samplingPos.concat(Double.toString(d[1]) +", ");			
		XMLUtil.setAttributeValue(samplingNode, SAMPLING_Y, samplingPos);		

		// bidirectional results
		final Element bidirectionalNode =   XMLUtil.setElement(nodeKymoResults, BIDIRECTIONAL_TRACKING);
		if (kymograph == null || kymograph.getName() == null)
			XMLUtil.setAttributeValue(bidirectionalNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
		else
		{
			if (kymograph.getFilename() == null || kymograph.getFilename().isEmpty())
			{
				File fKymo = new File(saveDir.getAbsolutePath().concat(FileUtil.separator + sourceSequence.getName() + "_kymo" + idx + ".tif"));
				Saver.save(kymograph, fKymo);
				XMLUtil.setAttributeValue(bidirectionalNode, KYMO_SEQUENCE_NAME, fKymo.getName());
			}
		}
		if (trackingResults == null)
			KymographTrackingResults.generateDefaultNode(bidirectionalNode);
		else
			trackingResults.saveToXML(bidirectionalNode);
		// anterograde results		
		final Element anterogradeNode =   XMLUtil.setElement(nodeKymoResults, ANTEROGRADE_TRACKING);
		if (anterogradeKymograph == null || anterogradeKymograph.getName() == null)
			XMLUtil.setAttributeValue(anterogradeNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
		else
		{
			File fKymo = new File(saveDir.getAbsolutePath().concat(FileUtil.separator + sourceSequence.getName() + "_anteroKymo.tif"));
			Saver.save(anterogradeKymograph, fKymo);
			XMLUtil.setAttributeValue(anterogradeNode, KYMO_SEQUENCE_NAME, fKymo.getName());
		}
		if (anterogradeTrackingResults == null)
			KymographTrackingResults.generateDefaultNode(anterogradeNode);
		else
			anterogradeTrackingResults.saveToXML(anterogradeNode);
		// retrograde results
		final Element retrogradeNode =   XMLUtil.setElement(nodeKymoResults, RETROGRADE_TRACKING);
		if (retrogradeKymograph == null || retrogradeKymograph.getName() == null)
			XMLUtil.setAttributeValue(retrogradeNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
		else
		{
			File fKymo = new File(saveDir.getAbsolutePath().concat(FileUtil.separator + sourceSequence.getName() + "_retroKymo.tif"));
			Saver.save(retrogradeKymograph, fKymo);
			XMLUtil.setAttributeValue(retrogradeNode, KYMO_SEQUENCE_NAME, fKymo.getName());
		}
		if (retrogradeTrackingResults == null)
			KymographTrackingResults.generateDefaultNode(retrogradeNode);
		else
			retrogradeTrackingResults.saveToXML(retrogradeNode);		
	}

	public static ArrayList<KymographExtractionResult> loadResultsFromXML(Element resultsElement, Sequence sourceSequence, File resultsDir)
	{	
		ArrayList<KymographExtractionResult> results = new ArrayList<KymographExtractionResult>();
		ArrayList<Element> elements = XMLUtil.getElements(resultsElement, KYMO_RESULT);
		for (Element e:elements)
		{
			KymographExtractionResult r = new KymographExtractionResult();
			r.sourceSequence = sourceSequence;
			r.anterogradeRetrogradeSeparation = XMLUtil.getAttributeBooleanValue(e, IS_ANTERO_RETRO_SPLIT, r.anterogradeRetrogradeSeparation);
			// load sampling positions and create corresponding ROI in the source sequence
			Element samplingNode = XMLUtil.getElement(e, SAMPLING_POSITIONS);
			if (samplingNode != null)
			{
				String samplingPos = XMLUtil.getAttributeValue(samplingNode, SAMPLING_X, "");
				String[] ss = samplingPos.split(",");
				ArrayList<Double> xList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
				{
					if (!ss[i].trim().isEmpty())
						xList.add(Double.valueOf(ss[i]));
				}
				samplingPos = XMLUtil.getAttributeValue(samplingNode, SAMPLING_Y, "");
				ss = samplingPos.split(",");
				ArrayList<Double> yList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
				{
					if (!ss[i].trim().isEmpty())
						yList.add(Double.valueOf(ss[i]));
				}
				r.samplingPositions = new ArrayList<double[]>();
				ArrayList<Point2D> samplingPositionList = new ArrayList<Point2D>();
				for (int i = 0; i < xList.size(); i++)
				{
					r.samplingPositions.add(new double[]{xList.get(i), yList.get(i)});
					samplingPositionList.add(new Point2D.Double(xList.get(i), yList.get(i)));
				}
				// create the corresponding ROI in the source sequence
				ROI2DPolyLine roi = new ROI2DPolyLine(samplingPositionList);
				r.roi = roi;
				sourceSequence.addROI(r.roi);
			}
			else
			{
				System.out.println("Could not load sampling positions for for KymographExtractionResult object, node "+ SAMPLING_POSITIONS + " not found in XML tree");
				return null;
			}			
			// bidirectional results
			Element bidirectionalNode = XMLUtil.getElement(e, BIDIRECTIONAL_TRACKING);
			if (bidirectionalNode != null)
			{
				String kymoSequenceName = XMLUtil.getAttributeValue(bidirectionalNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
				// now try to load the kymograph sequence
				if (!kymoSequenceName.equals(NULL_ELEMENT))
				{
					//					File f = new File(resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName);
					//					r.kymograph = Loader.loadSequence(f, 0, true);
					Sequence kymoSequence = Loader.loadSequence(resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName, 0, true);
					r.setKymograph(kymoSequence);
					r.roi.setName(kymoSequenceName);
				}
				r.trackingResults = KymographTrackingResults.loadFromXML(bidirectionalNode, sourceSequence, r.roi.getName() + "_tracks");
			}
			else
			{
				System.out.println("Could not load bidirectional results for KymographExtractionResult object, node "+BIDIRECTIONAL_TRACKING + " not found in XML tree");
			}
			// anterograde tracks
			Element anterogradeNode = XMLUtil.getElement(e, ANTEROGRADE_TRACKING);
			if (anterogradeNode != null)
			{
				String kymoSequenceName = XMLUtil.getAttributeValue(anterogradeNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
				// now try to load the kymograph sequence
				if (!kymoSequenceName.equals(NULL_ELEMENT))
				{
					r.setAnterogradeKymograph(Loader.loadSequence(resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName, 0, true));
					//					File f = new File((resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName));
					//					r.anterogradeKymograph = Loader.loadSequence(f, 0, true);
					r.roi.setName(kymoSequenceName);
				}
				r.anterogradeTrackingResults = KymographTrackingResults.loadFromXML(anterogradeNode, sourceSequence, "anterogradeKymoTracks");
			}
			else
			{
				System.out.println("Could not load anterograde results for KymographExtractionResult object, node "+ ANTEROGRADE_TRACKING + " not found in XML tree");
			}
			// retrograde results
			Element retrogradeNode = XMLUtil.getElement(e, RETROGRADE_TRACKING);
			if (retrogradeNode != null)
			{
				String kymoSequenceName = XMLUtil.getAttributeValue(retrogradeNode, KYMO_SEQUENCE_NAME, NULL_ELEMENT);
				// now try to load the kymograph sequence
				if (!kymoSequenceName.equals(NULL_ELEMENT))
				{
					//					File f = new File((resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName));
					//					r.retrogradeKymograph = Loader.loadSequence(f, 0, true);
					r.setRetrogradeKymograph(Loader.loadSequence(resultsDir.getAbsolutePath()+ FileUtil.separator + kymoSequenceName, 0, true));					
					r.roi.setName(kymoSequenceName);
				}
				r.retrogradeTrackingResults = KymographTrackingResults.loadFromXML(retrogradeNode, sourceSequence, "retrogradeKymoTracks");
			}
			else
			{
				System.out.println("Could not load retrograde results for KymographExtractionResult object, node "+ RETROGRADE_TRACKING + " not found in XML tree");
			}
			results.add(r);
		}
		return results;
	}

	public Sequence getKymograph() {
		return kymograph;
	}

	public void setKymograph(Sequence kymograph)
	{
		this.kymoROIs.clear();
		this.kymograph = kymograph;
		if (this.kymograph != null)
			this.kymograph.addListener(this);
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent)
	{
//		System.out.println(sequenceEvent.getSourceType()+" "+sequenceEvent.getType() + sequenceEvent.getSequence().getROI2Ds().size());
		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI && (sequenceEvent.getType() == SequenceEventType.ADDED || sequenceEvent.getType() == SequenceEventType.REMOVED))
		{
			Sequence sequence = sequenceEvent.getSequence();
			if (sequence == kymograph)
			{
				kymoROIs.clear();
				kymoROIs.addAll(sequence.getROI2Ds());
			}
			if (sequence == anterogradeKymograph)
			{
				anteroKymoROIs.clear();
				anteroKymoROIs.addAll(sequence.getROIs());
			}
			if (sequence == retrogradeKymograph)
			{
				retroKymoROIs.clear();
				retroKymoROIs.addAll(sequence.getROIs());
			}
		}
	}

	@Override
	public void sequenceClosed(Sequence sequence)
	{

	}

	public Sequence getAnterogradeKymograph() {
		return anterogradeKymograph;
	}

	public void setAnterogradeKymograph(Sequence anterogradeKymograph) {
		this.anteroKymoROIs.clear();
		this.anterogradeKymograph = anterogradeKymograph;
		if (this.anterogradeKymograph != null)
			this.anterogradeKymograph.addListener(this);
	}

	public Sequence getRetrogradeKymograph() {
		return retrogradeKymograph;
	}

	public void setRetrogradeKymograph(Sequence retrogradeKymograph) {
		this.retroKymoROIs.clear();
		this.retrogradeKymograph = retrogradeKymograph;
		if (this.retrogradeKymograph != null)
			this.retrogradeKymograph.addListener(this);

	}

	@Override
	public void sequenceOpened(Sequence sequence) {
		if (sequence!=null)
		{
			if (sequence == kymograph)
			{
				ArrayList<ROI> currentRois = sequence.getROIs();
				ArrayList<ROI> toAddRois = new ArrayList<ROI>(kymoROIs);
				for (ROI roi:toAddRois)
				{
					if (!currentRois.contains(roi))
					{
						sequence.addROI(roi);
					}
				}
				kymoROIs.clear();
				kymoROIs.addAll(sequence.getROI2Ds());
			}
			if (sequence == retrogradeKymograph)
			{
				ArrayList<ROI> currentRois = sequence.getROIs();
				ArrayList<ROI> toAddRois = new ArrayList<ROI>(retroKymoROIs);
				for (ROI roi:toAddRois)
				{
					if (!currentRois.contains(roi))
					{
						sequence.addROI(roi);
					}
				}
				retroKymoROIs.clear();
				retroKymoROIs.addAll(sequence.getROI2Ds());
			}
			if (sequence == anterogradeKymograph)
			{
				ArrayList<ROI> currentRois = sequence.getROIs();
				ArrayList<ROI> toAddRois = new ArrayList<ROI>(anteroKymoROIs);
				for (ROI roi:toAddRois)
				{
					if (!currentRois.contains(roi))
					{
						sequence.addROI(roi);
					}
				}
				anteroKymoROIs.clear();
				anteroKymoROIs.addAll(sequence.getROI2Ds());	
			}

		}
	}
}