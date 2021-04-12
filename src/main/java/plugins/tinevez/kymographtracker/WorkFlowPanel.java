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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import icy.sequence.Sequence;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

public class WorkFlowPanel extends ActionPanel
{
	private static final long serialVersionUID = -628508865966579403L;
	Sequence selectedSequence = null;
	JTextArea sequenceLabel;
	String noSequenceMessage = "<strong><font color=#FF0000>No sequence is selected.\n Please select first a sequence.</font></strong>";
	String kymoExtractionMessage = "<b>Kymograph extraction</b>: trace extraction paths in the sequence to analyze and convert them to ROIs, or directly trace (multiple or single line) ROIs. Kymographs are generated according to pixel values along the ROIs.";
	String kymoTrackingMessage = "<b>Kymograph tracking</b>: trace paths or ROIs in the kymograph images and convert them to tracks.";
	String worflowMessage = "The general workflow for kymograph tracking is the following: <ol> <li>"
			+ kymoExtractionMessage+ "</li> <li>"+ kymoTrackingMessage+"</li></ol>";
	JEditorPane editorPane;
	JButton startWorkFlowButton;
	
	public WorkFlowPanel() {
		description = "Workflow";
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
		
//		sequenceLabel = new JTextArea(noSequenceMessage);
//		sequenceLabel.setEditable(false);
//		northPanel.add(sequenceLabel, c);
//		c.gridy++;
		
		editorPane = new JEditorPane();
		editorPane.setContentType("text/html");
		editorPane.setEditable(false);
		northPanel.add(editorPane, c);
		refreshText();
		
		JPanel southPanel = new JPanel(new BorderLayout());
		startWorkFlowButton = new JButton("Start with kymograph extraction");
		southPanel.add(startWorkFlowButton, BorderLayout.CENTER);
		this.add(southPanel, BorderLayout.SOUTH);
	}

	public void enableGUI(boolean enable) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void changeSelectedSequence(Sequence sequence)
	{
		this.selectedSequence = sequence;
		refreshText();
	}
	
	public void refreshText()
	{
		if (selectedSequence == null)
			editorPane.setText(noSequenceMessage +"<br>"+ worflowMessage);
		else
			editorPane.setText("<font color=#00FF00>Selected sequence: "+selectedSequence.getName() +"</font><br>"+ worflowMessage);			
	}
}
