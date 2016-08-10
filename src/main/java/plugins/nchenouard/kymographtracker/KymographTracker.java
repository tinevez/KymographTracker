package plugins.nchenouard.kymographtracker;

import icy.gui.frame.IcyFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

public class KymographTracker extends PluginActionable
{
	Sequence selectedSequence = null;

	// GUI
	JPanel mainPanel;
	IcyFrame mainFrame;
	JPanel centerPanel;

	ArrayList<ActionPanel> actionPanels;

	private void generateGUI()
	{
		mainPanel = new JPanel();
		mainFrame = new IcyFrame("Kymograph Tracker", true, true, false, true);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
		actionPanels = new ArrayList<ActionPanel>();

		SequenceSelectionPanel sequencePanel = new SequenceSelectionPanel(this);
		root.add(sequencePanel.node);
		actionPanels.add(sequencePanel);
		sequencePanel.startWorkFlowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				for (ActionPanel p:actionPanels)
				{
					if (p instanceof WorkFlowPanel)
					{
						CardLayout cl = (CardLayout)(centerPanel.getLayout());
						cl.show(centerPanel, p.description);	
					}
//					if (p instanceof KymographExtractorPanel)
//					{
//						((KymographExtractorPanel) p).enableGUI(false);
//					}
//					if (p instanceof KymographTrackerPanel)
//					{
//						((KymographTrackerPanel) p).enableGUI(false);
//					}
				}
			}});	
		
		WorkFlowPanel workFlowPanel = new WorkFlowPanel();
		DefaultMutableTreeNode workFlowNode = workFlowPanel.node;
		root.add( workFlowNode);
		workFlowPanel.startWorkFlowButton.addActionListener(new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			for (ActionPanel p:actionPanels)
			{
				if (p instanceof KymographExtractorPanel)
				{
					CardLayout cl = (CardLayout)(centerPanel.getLayout());
					cl.show(centerPanel, p.description);
//					((KymographExtractorPanel) p).enableGUI(selectedSequence != null);
				}
//				if (p instanceof KymographTrackerPanel)
//				{
//					((KymographTrackerPanel) p).enableGUI(false);
//				}
			}
		}});
		actionPanels.add(workFlowPanel);
		
		KymographExtractorPanel extractorPanel = new KymographExtractorPanel(false);
		workFlowNode.add(extractorPanel.node);
		actionPanels.add(extractorPanel);
		extractorPanel.startTrackingButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				for (ActionPanel p:actionPanels)
				{
//					if (p instanceof WorkFlowPanel)
//					{
//						
//					}
//					if (p instanceof KymographExtractorPanel)
//					{
//						((KymographExtractorPanel) p).enableGUI(false);
//					}
					if (p instanceof KymographTrackerPanel)
					{
						CardLayout cl = (CardLayout)(centerPanel.getLayout());
						cl.show(centerPanel, p.description);	
					}
				}
			}});	
		
		
		KymographTrackerPanel trackerPanel = new KymographTrackerPanel();
		workFlowNode.add(trackerPanel.node);
		actionPanels.add(trackerPanel);

		ResultExportPanel exportPanel = new ResultExportPanel();
		workFlowNode.add(exportPanel.node);
		actionPanels.add(exportPanel);


		ResultImportPanel importPanel = new ResultImportPanel();
		root.add(importPanel.node);
		actionPanels.add(importPanel);

//		AnalysisPanel analysisPanel = new AnalysisPanel();
//		root.add(analysisPanel.node);
//		actionPanels.add(analysisPanel);

		final JTree tree = new JTree( root );
		tree.setRootVisible( false );
		for ( int row = 0 ; row < tree.getRowCount() ; row++) // Expand tree
			tree.expandRow( row );	    
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setOpaque(true);
		JScrollPane treeScrollPane = new JScrollPane(tree);

		mainPanel.setLayout(new BorderLayout());

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(treeScrollPane, BorderLayout.CENTER);
		mainPanel.add(leftPanel, BorderLayout.WEST);

		centerPanel = new JPanel(new CardLayout());
		for (ActionPanel p:actionPanels)
			centerPanel.add(p, p.description);
		tree.addTreeSelectionListener(new TreeSelectionListener(){
			@Override
			public void valueChanged(TreeSelectionEvent event) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				refreshCenterPanel(node);
			}});

		mainPanel.add(centerPanel, BorderLayout.CENTER);
		refreshCenterPanel(actionPanels.get(0).node);

		mainFrame.setContentPane(mainPanel);
		mainFrame.setPreferredSize(new Dimension(600, 500));
		mainFrame.pack();
		addIcyFrame(mainFrame);
		mainFrame.setVisible(true);

		changeSelectedSequence(sequencePanel.sequenceChooser.getSelectedSequence());
	}

	private void refreshCenterPanel(DefaultMutableTreeNode node)
	{
		CardLayout cl = (CardLayout)(centerPanel.getLayout());
		for (ActionPanel p:actionPanels)
		{
			if (node == p.node)
				cl.show(centerPanel, p.description);
//			if (p instanceof KymographExtractorPanel)
//			{
//				((KymographExtractorPanel) p).enableGUI(node == p.node && selectedSequence != null);
//			}
//			if (p instanceof KymographTrackerPanel)
//			{
//				((KymographTrackerPanel) p).enableGUI(node == p.node && selectedSequence != null);
//			}
		}
	}

	protected void changeSelectedSequence(Sequence sequence)
	{
		this.selectedSequence = sequence;
		for (ActionPanel p:actionPanels)
			p.changeSelectedSequence(sequence);
	}

	@Override
	public void run() {
		generateGUI();
	}

//	protected void enableWorkFlow(boolean enable)
//	{
////		for (ActionPanel p:actionPanels)
//		{
//			//if (p instanceof WorkFlowPanel)
//			//	((WorkFlowPanel) p).enableGUI(enable);
//		}
//	}
}