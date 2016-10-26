package plugins.tinevez.kymographtracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.util.XMLUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class ResultImportPanel extends ActionPanel
{
	private static final long serialVersionUID = 2908249721576764010L;

    Sequence selectedSequence;
	
	public ResultImportPanel() {
		description = "Import previous work";
		node = new DefaultMutableTreeNode(description);
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new GridLayout(2, 1));
		this.add(northPanel, BorderLayout.NORTH);

		JEditorPane descriptionPane = new JEditorPane();
		//originalTracingDescription.setPreferredSize(new Dimension(100, 100));
		descriptionPane.setContentType("text/html");
		descriptionPane.setEditable(false);
		descriptionPane.setText("Load a .xml file containing kymograph analysis results and which was generated with the 'export results' function.");
		descriptionPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		descriptionPane.setEditable(false);
		northPanel.add(descriptionPane);
	
		JButton importButton = new JButton(description);
		importButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				importResults();
			}
		});
		northPanel.add(importButton);
	}
	
	@Override
	protected void changeSelectedSequence(Sequence sequence)
	{
		this.selectedSequence = sequence;
	}
	
	public void importResults()
	{
		Sequence seq = null;
		if (selectedSequence == null)
		{
			new AnnounceFrame("Need to load a sequence before loading results.");
		}
		else
		{
			seq = selectedSequence;
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
			fileChooser.setName("Input xml file selection");
			int returnVal = fileChooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				loadResultsFromFile(file, seq);
			}
		}
	}
	
	protected void loadResultsFromFile(File file, Sequence seq)
	{
		Document document = XMLUtil.loadDocument( file );
		Element root = XMLUtil.getRootElement( document );
		if ( root == null )
		{
			throw new IllegalArgumentException( "can't find: <root> tag." );
		}
		Element resultsElement = XMLUtil.getElements( root , ResultExportPanel.KYMOGRAPH_RESULTS ).get( 0 );
		if ( resultsElement == null )
		{
			throw new IllegalArgumentException( "can't find: <root><"+ ResultExportPanel.KYMOGRAPH_RESULTS +"> tag." ) ;		
		}
		ArrayList<KymographExtractionResult> loadedResults = KymographExtractionResult.loadResultsFromXML(resultsElement, seq, file.getParentFile());
		if (loadedResults != null)
			for (KymographExtractionResult ker:loadedResults)
				Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(ker));
	}
}