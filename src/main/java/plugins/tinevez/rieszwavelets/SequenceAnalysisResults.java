package plugins.tinevez.rieszwavelets;

import java.util.ArrayList;


/**
 * 
 * Set of Riesz-wavelet coefficients for an image sequence.
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 */

public class SequenceAnalysisResults
{
	protected String sequenceName = ""; // name of the analyzed sequence
	protected ArrayList<Integer> analyzedTimes = new ArrayList<Integer>(); // time indices of analyzed frames
	ArrayList<RieszWaveletCoefficients> coefficientList = new ArrayList<RieszWaveletCoefficients>(); // Riesz-wavelet coefficients corresponding to analyzed frames
	
	/**
	 * Create empty storage structure, but set the sequence name
	 * @param sequenceName name of the analyzed sequence
	 * */
	public SequenceAnalysisResults(String sequenceName)
	{
		this.sequenceName = sequenceName;
	}
	
	/**
	 * Get the name of the analyzed sequence
	 * @return name of the analyzed sequence
	 * */
	public String getSequenceName()
	{
		return sequenceName;
	}
	
	/**
	 * Get all the Riesz-wavelet coefficients
	 * @return Riesz-wavelet coefficients for all the analyzed frames
	 * */
	public ArrayList<RieszWaveletCoefficients> getAllResults()
	{
		return new ArrayList<RieszWaveletCoefficients>(coefficientList);
	}
	
	/**
	 * Get the indices of the analyzed frames
	 * @return time indices of analyzed frames
	 * */
	public ArrayList<Integer> getAllAnalyzedTimesResults()
	{
		return new ArrayList<Integer>(analyzedTimes);
	}
	
	/**
	 * Store some Riesz-wavelet coefficients for a given time frame
	 * @param t time index of the analyzed frame
	 * @param result Riesz-wavelet coefficients
	 * */
	public void setResult(int t, RieszWaveletCoefficients result)
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
			coefficientList.add(result);
		}
		else
		{
			coefficientList.set(idx, result);
		}
	}
	/**
	 * Get the Riesz wavelet coefficients corresponding to a given frame
	 * @param t index
	 * @return Riesz wavelet coefficients at time t
	 * */
	public RieszWaveletCoefficients getResult(int t)
	{
		int cnt = 0;
		for (int t2:analyzedTimes)
		{
			if (t2 == t)
				return coefficientList.get(cnt);
			cnt++;
		}
		return null;
	}

	/**
	 * Reset the storage structure and initialize it with a set of coefficients chronologically ordered.
	 * @param results set of Riesz-wavelet coefficients to store
	 * */
	public void setAllResults(ArrayList<RieszWaveletCoefficients> results)
	{
		this.coefficientList.clear();
		this.analyzedTimes.clear();
		this.coefficientList.addAll(results);
		for (int t = 0; t < results.size(); t++)
			this.analyzedTimes.add(t);
	}
}
