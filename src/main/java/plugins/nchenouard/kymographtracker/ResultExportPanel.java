package plugins.nchenouard.kymographtracker;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPoolEvent;
import icy.swimmingPool.SwimmingPoolListener;
import icy.util.XMLUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.Label;
import jxl.write.WriteException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class ResultExportPanel extends ActionPanel implements SwimmingPoolListener
{
	private static final long serialVersionUID = -4840457556328742543L;

    private JTable kymoTable;
	private KymographTableModel kymoTableModel;
	
    Sequence selectedSequence;
    
	public static String KYMOGRAPH_RESULTS = "MultipleKymographTrackingResults";
    
	public ResultExportPanel() {
		description = "Results output";
		node = new DefaultMutableTreeNode(description);
		this.add(new JLabel(description));
		this.setBorder(new TitledBorder(description));
		this.setLayout(new BorderLayout());
		
		kymoTableModel = new KymographTableModel();
		kymoTable = new JTable(kymoTableModel);
		kymoTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.add(new JScrollPane(kymoTable), BorderLayout.CENTER);
		
		JButton exportButton = new JButton("Export results");
		this.add(exportButton, BorderLayout.SOUTH);
		exportButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveResults();
			}
		});		
		
		Icy.getMainInterface().getSwimmingPool().addListener(this);
	}

	public void enableGUI(boolean enable) {}

	@Override
	protected void changeSelectedSequence(Sequence sequence)
	{
		this.selectedSequence = sequence;
		refreshResults();
	}
	
	private void refreshResults() {
		kymoTableModel.clearTable();
		if (selectedSequence != null)
		{
			for (SwimmingObject obj:Icy.getMainInterface().getSwimmingPool().getObjects(KymographExtractionResult.class))
			{
				KymographExtractionResult result = (KymographExtractionResult)obj.getObject();
				if (result.sourceSequence == selectedSequence)
				{
					kymoTableModel.addResult(result);
				}
			}
		}
		refreshGUI();		
	}

	protected void refreshGUI()
	{
		if (kymoTableModel.results.isEmpty())
		{
//			titleLabel.setText(titleEmptyMessage);
			enableGUI(false);
		}
		else
		{
			enableGUI(true);
//			titleLabel.setText(titleEmptyMessage);
		}
		kymoTable.updateUI();
	}
	
	protected void saveResults()
	{
		ArrayList<KymographExtractionResult> results = kymoTableModel.getSelectedResults();
		if (results.isEmpty())
			return;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter() {			
			@Override
			public String getDescription() {
				return "XML file filter";
			}
			@Override
			public boolean accept(File file) {
				if (file.isDirectory())
					return true;
				return file.getName().endsWith(".xml");
			}
		});
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setName("Output xml file selection");
		int returnVal = fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (!file.getName().endsWith(".xml"))
				file = new File(file.getAbsolutePath().concat(".xml"));
			
			Document document = XMLUtil.createDocument( true );
			Element resultsElement = document.createElement(KYMOGRAPH_RESULTS);
			XMLUtil.getRootElement( document ).appendChild(resultsElement);
			int idx = 0;
			for (KymographExtractionResult r:results)
				r.saveToXML(resultsElement, file.getParentFile(), idx++);
			XMLUtil.saveDocument( document , file );

			// save results in an excel file, one sheet per kymograph
			File XLSFile = new File(file.getParent().concat("/trackingResultsSummary.xls"));
			WritableWorkbook workbook = null;
			try {
				WorkbookSettings wbSettings = new WorkbookSettings();
			    wbSettings.setLocale(new Locale("en", "EN"));
			    workbook = Workbook.createWorkbook(XLSFile, wbSettings);
			} catch (IOException e) {
				e.printStackTrace();
				new AnnounceFrame("Error creating XLS file. Resuls saving aborted!");
				return;
			}			
			try {
				int cntR = 0;
				for (KymographExtractionResult r:results)
				{
					if (r.trackingResults != null && r.trackingResults.tracks1D!= null && !r.trackingResults.tracks1D.isEmpty())
					{
						ArrayList<ArrayList<double[]>> tracks1D = r.trackingResults.tracks1D;
//						xlsManager.createNewPage(r.roi.getName());
						WritableSheet sheet = workbook.createSheet(r.roi.getName(), cntR);
						cntR++;
						int col = 0;
						int cnt = 0;
						for (ArrayList<double[]> dList : tracks1D)
						{
//							xlsManager.setLabel( col , 0 , "track "+cnt+" t" );
//							xlsManager.setLabel( col + 1 , 0 , "track "+cnt+" pos" );
							sheet.addCell(new Label(col , 0 , "track "+cnt+" t" ));
							sheet.addCell(new Label(col + 1 , 0 , "track "+cnt+" pos" ));

							int cnt2 = 1;
							for (double[] d:dList)
							{
//								xlsManager.setLabel( col , cnt2 , Double.toString(d[0]));
//								xlsManager.setLabel( col + 1 , cnt2 , Double.toString(d[1]));								
								sheet.addCell(new Number( col , cnt2 , d[0]));
								sheet.addCell(new Number( col + 1 , cnt2 , d[1]));
								cnt2 ++;
							}
							col += 2;
							cnt ++;
						}
					}
					if (r.retrogradeTrackingResults != null && r.retrogradeTrackingResults.tracks1D != null && !r.retrogradeTrackingResults.tracks1D.isEmpty())
					{
						ArrayList<ArrayList<double[]>> tracks1D = r.retrogradeTrackingResults.tracks1D;
						WritableSheet sheet = workbook.createSheet(r.roi.getName()  + "_retrograde", cntR);
						cntR++;

						int col = 0;
						int cnt = 0;
						for (ArrayList<double[]> dList : tracks1D)
						{
//							xlsManager.setLabel( col , 0 , "track "+cnt+" t" );
//							xlsManager.setLabel( col + 1 , 0 , "track "+cnt+" pos" );
							sheet.addCell(new Label(col , 0 , "track "+cnt+" t"));
							sheet.addCell(new Label(col + 1 , 0 , "track "+cnt+" pos"));							
							int cnt2 = 1;
							for (double[] d:dList)
							{
//								xlsManager.setLabel( col , cnt2 , Double.toString(d[0]));
//								xlsManager.setLabel( col + 1 , cnt2 , Double.toString(d[1]));								
								sheet.addCell(new Number(col, cnt2, d[0]));
								sheet.addCell(new Number(col + 1, cnt2, d[1]));
								cnt2 ++;
							}
							col += 2;
							cnt ++;
						}
					}
					if (r.anterogradeTrackingResults != null && r.anterogradeTrackingResults.tracks1D != null && !r.anterogradeTrackingResults.tracks1D.isEmpty())
					{
						ArrayList<ArrayList<double[]>> tracks1D = r.anterogradeTrackingResults.tracks1D;
//						xlsManager.createNewPage(r.roi.getName() + "_anterograde");
						WritableSheet sheet = workbook.createSheet(r.roi.getName()  + "_anterograde", cntR);
						cntR++;

						int col = 0;
						int cnt = 0;
						for (ArrayList<double[]> dList : tracks1D)
						{
//							xlsManager.setLabel( col , 0 , "track "+cnt+" t" );
//							xlsManager.setLabel( col + 1 , 0 , "track "+cnt+" pos" );
							sheet.addCell(new Label(col, 0, "track "+cnt+" t"));
							sheet.addCell(new Label(col + 1, 0, "track "+cnt+" pos"));
							
							int cnt2 = 1;
							for (double[] d:dList)
							{
//								xlsManager.setLabel( col , cnt2 , Double.toString(d[0]));
//								xlsManager.setLabel( col + 1 , cnt2 , Double.toString(d[1]));
								sheet.addCell(new Number(col, cnt2, d[0]));
								sheet.addCell(new Number(col + 1, cnt2, d[1]));
								cnt2 ++;
							}
							col += 2;
							cnt ++;
						}
					}
				}
//				xlsManager.SaveAndClose();
			} catch (jxl.write.WriteException e) {
				e.printStackTrace();
			}
			try {
				if (workbook.getNumberOfSheets() > 0)
					workbook.write();
				workbook.close();
			} catch (WriteException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class KymographTableModel extends DefaultTableModel
	{
		/**
		 * 
		 */
		
		private static final long serialVersionUID = -5487686557114899956L;
		ArrayList<KymographExtractionResult> results = new ArrayList<KymographExtractionResult>();		
		ArrayList<Boolean> selection = new ArrayList<Boolean>();
		
		protected void refreshTable()
		{
			this.fireTableDataChanged();
		}
		
		public void clearTable() {
			results.clear();
			selection.clear();
		}

		protected void addResult(KymographExtractionResult result)
		{
			results.add(result);
			selection.add(new Boolean(true));
			refreshTable();
		}
		
		@Override
		public int getColumnCount() {
			return 2;
		}
		
		@Override
		public String getColumnName(int column)
		{
			if (column == 0)
				return "Selection";
			else
				return "Kymographs";
		};

		@Override
        public Class getColumnClass(int column) {
			if (column == 0)
				return Boolean.class;
			else
				return String.class;
		}
		
		@Override
		public int getRowCount() {
			if (results == null)
					return 1;
			return results.size();
		}

		@Override
		public Object getValueAt(int r, int c)
		{
			if (results == null)
				return null;
			if (r >= getRowCount() || c >= getColumnCount())
				return null;
//			if (c > 1)
//				return null;
			if (c == 0)
				return selection.get(r);
			else
				if (results.get(r).roi != null)
					return results.get(r).roi.getName();
				else
					return "null";
		}

		public void removeResult(KymographExtractionResult result) {
			int idx = results.indexOf(result);
			if (idx >= 0)
			{
				results.remove(idx);
				selection.remove(idx);
			}
			this.fireTableRowsDeleted(idx, idx);			
		}
		public ArrayList<KymographExtractionResult> getSelectedResults()
		{
			ArrayList<KymographExtractionResult> selectedList = new ArrayList<KymographExtractionResult>();
			for (int k = 0; k < selection.size(); k++)
			{
				if (selection.get(k))
					selectedList.add(results.get(k));
			}
			return selectedList;
		}
	}
	

	@Override
	public void swimmingPoolChangeEvent(SwimmingPoolEvent event) {
		if (event.getResult().getObject() instanceof KymographExtractionResult)
		{
			KymographExtractionResult result = (KymographExtractionResult)event.getResult().getObject();
			if (result.sourceSequence == selectedSequence)
			{
				switch (event.getType())
				{
				case ELEMENT_ADDED:
					kymoTableModel.addResult(result);
					break;
				case ELEMENT_REMOVED:
					kymoTableModel.removeResult(result);
					break;
				}
				refreshGUI();
			}
		}
	}
}