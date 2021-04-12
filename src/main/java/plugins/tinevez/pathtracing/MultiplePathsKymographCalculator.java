package plugins.tinevez.pathtracing;

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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

import icy.gui.component.sequence.SequenceChooser;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.image.IcyBufferedImage;
import icy.painter.Painter;
import icy.plugin.abstract_.PluginActionable;
import icy.roi.ROI2DPolyLine;
import icy.sequence.Sequence;
import icy.type.DataType;

public class MultiplePathsKymographCalculator extends PluginActionable
{
	private SequenceChooser sequenceChooser = new SequenceChooser();
	IcyFrame mainFrame;

	double alpha = 0.001;

	Thread projectionThread;
	
	@Override
	public void run()
	{
		this.mainFrame = new IcyFrame("Multiple path tracking", true, true);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		mainPanel.add(sequenceChooser, c);
		c.gridy++;

		JButton maxProjectionButton = new JButton("Max projection");
		maxProjectionButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				projectionThread = new Thread()
				{
					@Override
					public void run()
					{
						projectSequence(sequenceChooser.getSelectedSequence());
					}
				};
				projectionThread.start();
			}});
		mainPanel.add(maxProjectionButton, c);
		c.gridy++;
		
		JButton addPathButton = new JButton("Add new path");
		addPathButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				addPath();
			}}
				);
		mainPanel.add(addPathButton, c);
		c.gridy++;

		JButton convertToROIButton = new JButton("ConvertToROIButton");
		convertToROIButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				convertPathToROI();
			}}
				);
		mainPanel.add(convertToROIButton, c);
		c.gridy++;

		mainFrame.setContentPane(mainPanel);
		mainFrame.pack();
		mainFrame.addToMainDesktopPane();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.requestFocus();

		mainFrame.addFrameListener(new IcyFrameListener(){

			@Override
			public void icyFrameOpened(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameClosing(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameClosed(IcyFrameEvent e) {
				for (Sequence seq:getSequences())
				{
					for (Painter tracer:seq.getPainters(InteractiveMultipleDjikstraTracing.class))
					{
						seq.removePainter(tracer);
					}
				}
			}

			@Override
			public void icyFrameIconified(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameDeiconified(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameActivated(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameDeactivated(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameInternalized(IcyFrameEvent e) {
			}

			@Override
			public void icyFrameExternalized(IcyFrameEvent e) {
			}});
	}


	private void addPath()
	{
		Sequence seq = sequenceChooser.getSelectedSequence();
		if (seq != null)
		{
			if (seq.getPainters(InteractiveMultipleDjikstraTracing.class).isEmpty())
			{
				InteractiveMultipleDjikstraTracing tracer = new InteractiveMultipleDjikstraTracing(seq, alpha, false);
				seq.addPainter(tracer);
			}
		}
	}

	private void convertPathToROI()
	{
		Sequence seq = sequenceChooser.getSelectedSequence();
		if (seq != null)
		{
			int cntROI = seq.getROICount(ROI2DPolyLine.class);
			for (Painter tracer:seq.getPainters(InteractiveMultipleDjikstraTracing.class))
			{
				ArrayList<double[][]> pathList = ((InteractiveMultipleDjikstraTracing)tracer).getOptimalPathCopy();
				double prevLastPosX = -1;
				double prevLastPosY = -1;
				ROI2DPolyLine polylineROI = null;
				if (!pathList.isEmpty())
				{
					for (double[][] path:pathList)
					{
						if (path[0][0] == prevLastPosX && path[0][1] == prevLastPosY)
						{
							for (int i = 1; i < path.length; i ++)	
							{
								polylineROI.addPointAt(new Point2D.Double(path[i][0], path[i][1]), false);
							}
						}
						else
						{
							cntROI++;
							polylineROI = new ROI2DPolyLine(new Point2D.Double(path[0][0], path[0][1]));
							polylineROI.setName("axon "+cntROI);
							for (Sequence[] seqPair:projSeqList)
							{
								if (seqPair[0] == seq)
									seqPair[1].addROI(polylineROI);
								if (seqPair[1] == seq)
									seqPair[0].addROI(polylineROI);
							}
							seq.addROI(polylineROI);
							for (int i = 1; i < path.length; i ++)	
							{
								polylineROI.addPointAt(new Point2D.Double(path[i][0], path[i][1]), false);
							}
						}
						prevLastPosX = path[path.length - 1][0];
						prevLastPosY = path[path.length - 1][1];
					}
				}
				seq.removePainter(tracer);
			}
		}
	}

	ArrayList<Sequence[]> projSeqList = new ArrayList<Sequence[]>();
	private void projectSequence(Sequence seq)
	{
		if (seq != null)
		{
			Sequence projectionSequence = new Sequence("Sum "+seq.getName());
			projectionSequence.setImage(0, 0, new IcyBufferedImage(seq.getSizeX(), seq.getSizeY(), 1, DataType.DOUBLE));
			double[] tabValues = projectionSequence.getImage(0, 0, 0).getDataXYAsDouble(0);
			for (int y = 0; y < seq.getSizeY(); y++)
				for (int x = 0; x < seq.getSizeX(); x++)
				{
					double maxVal = seq.getData(0, 0, 0, y, x);
					for (int t = 1; t < seq.getSizeT(); t++)
						if (maxVal < seq.getData(t, 0, 0, y, x))
							maxVal = seq.getData(t, 0, 0, y, x);
					tabValues[x + y*seq.getWidth()] = maxVal;
				}
			projectionSequence.dataChanged();
			addSequence(projectionSequence);
			projSeqList.add(new Sequence[]{seq, projectionSequence});
		}
	}
}
