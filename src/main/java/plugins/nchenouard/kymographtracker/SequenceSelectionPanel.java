package plugins.nchenouard.kymographtracker;

import icy.gui.component.sequence.SequenceChooser;
import icy.gui.component.sequence.SequenceChooser.SequenceChooserListener;
import icy.sequence.Sequence;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

class SequenceSelectionPanel extends ActionPanel
{
	/**
	 * 
	 */
	private KymographTracker kymographTracker;
	private static final long serialVersionUID = 802043406982206717L;
	SequenceChooser sequenceChooser;
	JButton startWorkFlowButton;
	
	public SequenceSelectionPanel(KymographTracker kymographTracker) {
		this.kymographTracker = kymographTracker;
		description = "Image sequence selection";
		node = new DefaultMutableTreeNode(description);
		this.setBorder(new TitledBorder(description));
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new GridBagLayout());
		this.add(northPanel, BorderLayout.NORTH);

		GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.0;
		c.insets = new Insets(2, 2, 2, 2);

		JTextArea nDescription = new JTextArea("Select the image sequence to process. Only the first channel will be processed.");
		nDescription.setEditable(false);
		nDescription.setLineWrap(true);
		nDescription.setWrapStyleWord(true);
		northPanel.add(nDescription, c);
		c.gridy++;

		sequenceChooser = new SequenceChooser();
		northPanel.add(sequenceChooser, c);

		sequenceChooser.addListener(new SequenceChooserListener(){
			@Override
			public void sequenceChanged(Sequence sequence) {
				SequenceSelectionPanel.this.kymographTracker.changeSelectedSequence(sequence);
			}});
		this.kymographTracker.changeSelectedSequence(sequenceChooser.getSelectedSequence());
		
		JPanel southPanel = new JPanel(new BorderLayout());
		startWorkFlowButton = new JButton("Start kymograph tracking workflow");
		southPanel.add(startWorkFlowButton, BorderLayout.CENTER);
		this.add(southPanel, BorderLayout.SOUTH);
	
	}
	
	@Override
	protected void changeSelectedSequence(Sequence sequence)
	{
		
	}
}