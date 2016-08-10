package plugins.nchenouard.rieszwavelets;

import java.util.ArrayList;


public class AngleSequence {

	protected String sequenceName = "";
	protected ArrayList<Integer> analyzedTimes = new ArrayList<Integer>();
	protected ArrayList<ArrayList<double[]>> angles = new ArrayList<ArrayList<double[]>>();

	ArrayList<Integer> bandHeight;
	ArrayList<Integer> bandWidth;
	int numScales;

	public int getBandHeightAtScale(int scale)
	{
		return bandHeight.get(scale);
	}

	public int getBandWidthAtScale(int scale)
	{
		return bandWidth.get(scale);
	}

	public int getNumScales()
	{
		return numScales;
	}

	public AngleSequence(String sequenceName, int numScales,ArrayList<Integer> bandHeight, ArrayList<Integer> bandWidth)
	{
		this.sequenceName = sequenceName;
		this.numScales = numScales;
		this.bandHeight = bandHeight;
		this.bandWidth = bandWidth;
	}
	public String getSequenceName()
	{
		return sequenceName;
	}

	public ArrayList<ArrayList<double[]>> getAllAngles()
	{
		ArrayList<ArrayList<double[]>> res = new ArrayList<ArrayList<double[]>>();
		res.addAll(angles);
		return res;
	}

	public ArrayList<Integer> getAllAnalyzedTimes()
	{
		return new ArrayList<Integer>(analyzedTimes);
	}

	public void setAngles(int t, ArrayList<double[]> anglesT)
	{
		int idx = -1;
		int cnt = 0;
		for (int t2:analyzedTimes)
		{
			if (t2 == t)
			{
				idx = cnt;
				break;
			}
			cnt++;
		}
		if (idx == -1)
		{
			analyzedTimes.add(t);
			angles.add(anglesT);
		}
		else
		{
			angles.set(idx, anglesT);
		}
	}

	public ArrayList<double[]> getAngles(int t)
	{
		int cnt = 0;
		for (int t2:analyzedTimes)
		{
			if (t2 == t)
				return angles.get(cnt);
			cnt++;
		}
		return null;
	}

	public void setAllAngles(ArrayList<ArrayList<double[]>> results)
	{
		this.angles.clear();
		this.analyzedTimes.clear();
		this.angles.addAll(results);
		for (int t = 0; t < results.size(); t++)
			this.analyzedTimes.add(t);
	}

}
