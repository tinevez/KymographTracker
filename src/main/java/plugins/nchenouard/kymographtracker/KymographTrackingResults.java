package plugins.nchenouard.kymographtracker;

import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.util.XMLUtil;

import java.util.ArrayList;

import org.w3c.dom.Element;

import plugins.fab.trackmanager.TrackGroup;
import plugins.fab.trackmanager.TrackSegment;
import plugins.nchenouard.spot.Detection;

public class KymographTrackingResults {
	ArrayList<ArrayList<double[]>> tracks1D;
	TrackGroup tracks2D;

	public static String KYMO_TRACKING_RESULTS = "KymographTrackingResults";
	public static String KYMO_TRACK = "KymographTrack";
	public static String TRACKS = "Tracks";
	public static String TIME_LIST = "TimeList";
	public static String POSITION_LIST = "PositionList";
	public static String POSITION_LISTX = "PositionListX";
	public static String POSITION_LISTY = "PositionListY";

	
	public static void generateDefaultNode(Element node)
	{
		XMLUtil.setElement(node, KYMO_TRACKING_RESULTS); // empty element
	}

	public void saveToXML(Element node)
	{
		final Element nodeKymoResults =   XMLUtil.setElement(node, KYMO_TRACKING_RESULTS);
		for (int i = 0; i < tracks1D.size(); i ++)
		{
			final Element nodeTrack = XMLUtil.addElement(nodeKymoResults, KYMO_TRACK);
			String timeString = "";
			String positionString = "";
			for (int j = 0; j < tracks1D.get(i).size(); j++)
			{
				timeString = timeString.concat(tracks1D.get(i).get(j)[0]+", ");
				positionString = positionString.concat(tracks1D.get(i).get(j)[1]+", ");
			}
			XMLUtil.setAttributeValue(nodeTrack, TIME_LIST, timeString);
			XMLUtil.setAttributeValue(nodeTrack, POSITION_LIST, positionString);
			String positionStringX = "";
			String positionStringY = "";
			for (int j = 0; j < tracks2D.getTrackSegmentList().get(i).getDetectionList().size(); j++)
			{
				positionStringX = positionStringX.concat(tracks2D.getTrackSegmentList().get(i).getDetectionAt(j).getX()+", ");
				positionStringY = positionStringY.concat(tracks2D.getTrackSegmentList().get(i).getDetectionAt(j).getY()+", ");
			}
			XMLUtil.setAttributeValue(nodeTrack, POSITION_LISTX, positionStringX);
			XMLUtil.setAttributeValue(nodeTrack, POSITION_LISTY, positionStringY);
		}

	}

	public static KymographTrackingResults loadFromXML(Element node, Sequence sourceSequence, String trackGroupName)
	{
		Element e = XMLUtil.getElement(node, KYMO_TRACKING_RESULTS);
		if (e == null)
			return null;
		else
		{
			KymographTrackingResults r = new KymographTrackingResults();
			r.tracks1D = new ArrayList<ArrayList<double[]>>();
			r.tracks2D = new TrackGroup(sourceSequence);
			r.tracks2D.setDescription(trackGroupName);
			ArrayList<Element> trackElements = XMLUtil.getElements(e, KYMO_TRACK);
			for (Element te:trackElements)
			{
				String s = XMLUtil.getAttributeValue(te, TIME_LIST, "");
				String[] ss = s.split(",");
				ArrayList<Double> tList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
					if (!ss[i].trim().isEmpty())
						tList.add(Double.valueOf(ss[i]));
				s = XMLUtil.getAttributeValue(te, POSITION_LIST, "");
				ss = s.split(",");
				ArrayList<Double> pList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
				{
					if (!ss[i].trim().isEmpty())
						pList.add(Double.valueOf(ss[i]));
				}
				ArrayList<double[]> trk1D = new ArrayList<double[]>();
				for (int i = 0; i < tList.size(); i++)
					trk1D.add(new double[]{tList.get(i), pList.get(i)});
				r.tracks1D.add(trk1D);
				
				s = XMLUtil.getAttributeValue(te, POSITION_LISTX, "");
				ss = s.split(",");
				ArrayList<Double> xList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
					if (!ss[i].trim().isEmpty())
						xList.add(Double.valueOf(ss[i]));
				s = XMLUtil.getAttributeValue(te, POSITION_LISTY, "");
				ss = s.split(",");
				ArrayList<Double> yList = new ArrayList<Double>();
				for (int i = 0; i < ss.length; i++)
					if (!ss[i].trim().isEmpty())
						yList.add(Double.valueOf(ss[i]));
				TrackSegment ts = new TrackSegment();
				for (int i = 0; i < xList.size(); i++)
					ts.addDetection(new Detection(xList.get(i), yList.get(i), 0, tList.get(i).intValue()));
				r.tracks2D.addTrackSegment(ts);
			}
			// read 2D tracks
			Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(r.tracks2D));
			return r;
		}
	}
}