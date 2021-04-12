package plugins.tinevez.rieszwavelets;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


/**
 * 
 * Panel for setting up the parameters of customs generalized Riesz transforms
 * 
 * For a description of the transforms, see:
 * 
 * A Unifying Parametric Framework for 2D Steerable Wavelet Transforms
 * Unser, M. and Chenouard, N.
 * SIAM Journal on Imaging Sciences 2013 6:1, 102-135 
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 */

public class RieszConfigurationPanel extends JPanel
{
	private static final long serialVersionUID = 6350888646593158987L;
	SpinnerNumberModel orderSpinnerModel;
	JComboBox<HarmonicTypes> harmonicTypeBox;
	JComboBox<StandardRieszFrames> rieszFrameBox;
	
	int order = 3;
	HarmonicTypes harmonicType = HarmonicTypes.odd;
	StandardRieszFrames rieszFrame = StandardRieszFrames.Riesz;
	/**
	 * Generate the GUI
	 * */
	public RieszConfigurationPanel()
	{
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		
		this.add(new JLabel("Riesz transform configuration"), c);
		c.gridy++;
		
		this.add(new JLabel("Transform order"), c);
		c.gridy++;
		orderSpinnerModel = new SpinnerNumberModel(order, 0, 20, 1);
		JSpinner orderSpinner = new JSpinner(orderSpinnerModel);
		this.add(orderSpinner, c);
		c.gridy++;

		this.add(new JLabel("Harmonic type"), c);
		c.gridy++;
		harmonicTypeBox = new JComboBox<HarmonicTypes>(HarmonicTypes.values());
		harmonicTypeBox.setSelectedItem(harmonicType);
		this.add(harmonicTypeBox, c);
		c.gridy++;
		
		this.add(new JLabel("Riesz generalization"), c);
		c.gridy++;
		rieszFrameBox = new JComboBox<StandardRieszFrames>(StandardRieszFrames.values());
		rieszFrameBox.setSelectedItem(rieszFrame);
		this.add(rieszFrameBox, c);
		c.gridy++;
	}
	/**
	 * Get the maximum order of the circular harmonics defining the generalized Riesz transform, as selected in the GUI
	 * @return maximum order for the circular harmonics
	 * */
	public int getOrder()
	{
		return orderSpinnerModel.getNumber().intValue();
	}

	/**
	 * Get the type of circular harmonics to be used for the  for the generalized Riesz transform, as specified in the GUI
	 * */
	public HarmonicTypes getHarmonicType()
	{
		return (HarmonicTypes) harmonicTypeBox.getSelectedItem();
	}
	/**
	 * Get the type of standard generalization for the Riesz transform selected in the GUI
	 * @return standard generalization for the Riesz transform
	 * */
	public StandardRieszFrames getStandardRiezGeneralization()
	{
		return (StandardRieszFrames) rieszFrameBox.getSelectedItem();
	}
}
