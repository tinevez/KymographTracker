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
