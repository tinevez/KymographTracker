package plugins.tinevez.rieszwavelets;

import icy.gui.component.pool.SwimmingObjectChooser;
import icy.swimmingPool.SwimmingObject;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import plugins.nchenouard.isotropicwavelets.WaveletFilterSet;


/**
 * 
 * Panel for manually selecting Riesz-wavelet coefficients to process in ICY SwimmingPool. Those should be stored in SequenceAnalysisResults objects.
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 */

public class CoefficientSelectionPanel extends JPanel
{
	private static final long serialVersionUID = 7698432379479738937L;
	CoefficientsTableModel coeffTableModel;
	JTable coefficientTable;
	SwimmingObjectChooser resultsBox;

	public CoefficientSelectionPanel()
	{
		this.setLayout(new BorderLayout());
		resultsBox = new SwimmingObjectChooser(SequenceAnalysisResults.class);
		this.add(resultsBox, BorderLayout.NORTH);
		resultsBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				updateResultsTable();
			}
		});

		coeffTableModel = new CoefficientsTableModel();
		coefficientTable = new JTable(coeffTableModel);
		JScrollPane scrollTablePane = new JScrollPane(coefficientTable);
		this.add(scrollTablePane, BorderLayout.CENTER);
	}

	/**
	 * Update the table of coefficients using objects in the SwimmingPool of type SequenceAnalysisResults
	 * */
	void updateResultsTable()
	{
		Object selectedItem = resultsBox.getSelectedItem();
		if (selectedItem != null)
		{
			SequenceAnalysisResults results = (SequenceAnalysisResults) ((SwimmingObject) selectedItem).getObject();
			coeffTableModel.setResults(results);
		}
		else
			coeffTableModel.setResults(null);
	}

	/**
	 * 
	 * Table for managing Riesz-wavelet coefficients stored in SequenceAnalysisResults objects
	 * */
	public class CoefficientsTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = -4810024326298005026L;
		SequenceAnalysisResults results = null;
		String[] columnNames = new String[]{"Sequence name", "Frames", "Generalization", "Harmonics", "Harmonics type", "Scales", "Wavelet profile", "Filter high frequencies", "Isotropic padding", "Padding X", "Padding Y"};
		String[] valueList = new String[]{"", "", "", "","","","","","","",""};

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		};

		public void setResults(SequenceAnalysisResults r)
		{
			results = r ;
			if (results == null)
			{
				for (int i = 0; i < valueList.length; i++)
					valueList[i] = "";
			}
			else
			{
				valueList[0] = results.getSequenceName();
				valueList[1] = Integer.toString(results.getAllAnalyzedTimesResults().size());
				RieszWaveletCoefficients rwc = results.getAllResults().get(0);
				RieszWaveletConfig config = rwc.getConfig();
				WaveletFilterSet filters = config.getWaveletFilterSet();

				ArrayList<RieszGeneralization> rieszGeneralizationList = rwc.getGeneralizationList();
				ArrayList<StandardRieszFrames> rieszFrameList = new ArrayList<StandardRieszFrames>();
				for (RieszGeneralization rg:rieszGeneralizationList)
					rieszFrameList.add(rg.getRieszFrame());
				boolean sameItem = true;
				StandardRieszFrames rieszFrame = rieszFrameList.get(0);
				for (StandardRieszFrames srf:rieszFrameList)
					if (srf != rieszFrame)
					{
						sameItem = false;
						break;
					}
				String rieszFrameString = "";
				if (sameItem)
					rieszFrameString = rieszFrame.toString();
				else
				{
					for (StandardRieszFrames srf:rieszFrameList)
						rieszFrameString = rieszFrameString.concat(srf.toString()+"; ");
				}
				valueList[2] = rieszFrameString;

				ArrayList<RieszConfig> rieszConfigList = config.getRieszConfigurations();
				ArrayList<int[]> harmonicList = new ArrayList<int[]>();
				ArrayList<HarmonicTypes> harmonicsTypeList = new ArrayList<HarmonicTypes>();
				for (RieszConfig rc:rieszConfigList)
				{
					harmonicList.add(rc.harmonics);
					harmonicsTypeList.add(rc.harmonicType);
				}
				sameItem = true;
				int[] harmonics = harmonicList.get(0);
				for (int[] h:harmonicList)
				{
					if (harmonics.length != h.length)
					{
						sameItem = false;
						break;
					}
					for (int i = 0; i < h.length; i++)
						if (h[i] != harmonics[i])
						{
							sameItem = false;
							break;
						}
				}
				String harmonicsString = "";
				if (sameItem)
				{
					harmonicsString = "[";
					for (int j = 0; j < harmonics.length; j++ )
						harmonicsString = harmonicsString + harmonics[j]+ ", ";
					harmonicsString = harmonicsString + "]";
				}
				else
				{
					for (int[] h:harmonicList)
					{
						harmonicsString = harmonicsString + "[";
						for (int j = 0; j < harmonics.length; j++ )
							harmonicsString = harmonicsString + h[j] + ", ";
						harmonicsString = harmonicsString + "]; ";
					}
				}
				valueList[3] = harmonicsString;

				sameItem = true;
				HarmonicTypes harmonicsType = harmonicsTypeList.get(0);
				for (HarmonicTypes ht:harmonicsTypeList)
				{
					if (ht != harmonicsType)
					{
						sameItem = false;
						break;
					}
				}
				String harmonicTypeString = "";
				if (sameItem)
					harmonicTypeString = harmonicsType.toString();
				else
				{
					for (HarmonicTypes ht:harmonicsTypeList)
						harmonicTypeString = harmonicTypeString.concat(ht.toString()+"; ");
				}
				valueList[4] = harmonicTypeString;
				valueList[5] = Integer.toString(filters.getNumScales());
				valueList[6] = filters.getWaveletType().toString();
				valueList[7] = Boolean.toString(filters.isPrefilter());
				valueList[8] = Boolean.toString(filters.isIsotropicPadding());
				valueList[9] = Integer.toString(rwc.getPadX());
				valueList[10] = Integer.toString(rwc.getPadY());
			}
			this.fireTableDataChanged();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
		public Object getValueAt(int arg0, int arg1) {
			return valueList[arg1];
		}		
	}
}