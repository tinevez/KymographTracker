package plugins.tinevez.kymographtracker;

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
	private KymographTracker2 kymographTracker;
	private static final long serialVersionUID = 802043406982206717L;
	SequenceChooser sequenceChooser;
	JButton startWorkFlowButton;
	
	public SequenceSelectionPanel(KymographTracker2 kymographTracker) {
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
