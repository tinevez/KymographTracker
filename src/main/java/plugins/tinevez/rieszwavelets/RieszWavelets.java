package plugins.tinevez.rieszwavelets;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.type.collection.array.ArrayUtil;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import plugins.nchenouard.isotropicwavelets.IsotropicWaveletType;
import plugins.nchenouard.isotropicwavelets.WaveletConfigPanel;
import plugins.nchenouard.isotropicwavelets.WaveletFilterSet;


/**
 * Tools for 2D steerable wavelets transforms.
 * We provide a parametric framework for various types of steerable wavelets, including Riesz wavelets and Simoncelli's pyramid.
 * A complete description of our 2D steerable wavelets is provided in:
 * 
 * A Unifying Parametric Framework for 2D Steerable Wavelet Transforms
 * Unser, M. and Chenouard, N.
 * SIAM Journal on Imaging Sciences 2013 6:1, 102-135 
 *  
 *  Please cite the above reference in scientific communications upon use of these tools.
 * 
 * @author Nicolas Chenouard (nicolas.chenouard.dev@gmail.com)
 * @version 1.0
 */

public class RieszWavelets extends PluginActionable
{
	IcyFrame mainFrame;
	JTabbedPane mainPanel;

	String computeWavCoeffString = "Compute coefficients from focused image";
	String selectWavCoeffString = "Select existing wavelet coefficients";
	JComboBox<String> waveletSelectionBox = new JComboBox<String>(new String[]{computeWavCoeffString, selectWavCoeffString});
	JPanel waveletPanel;	
	CardLayout wavCoeffCards = new CardLayout();
	CoefficientSelectionPanel wavCoeffSelectionPanel;
	WaveletConfigPanel wavCoeffConfigPanel;
	RieszConfigurationPanel rieszConfigPanel;
	CoefficientSelectionPanel coefficientSelectionPanel;
	JButton processImageButton;
	JButton processSequenceButton;
	JCheckBox displayCoefficientsBox;
	JButton displayCoefficientsButton;
	JButton reconstructButton;

//	String useMonogenicAnalysis = "Compute angles with monogenic analysis";
//	String useExistingAngleResults = "Use existing angle results";
//	JComboBox<String> angleBox = new JComboBox<String>(new String[]{useMonogenicAnalysis, useExistingAngleResults});
	JButton steerCoefficientsButton;
	JButton steerCoefficientsBackButton;
	CoefficientSelectionPanel steeringCoefficientSelectionPanel;
	SpinnerNumberModel monogenicRegularizationModel;
	CustomChooser angleChooser;

	AnnounceFrame announceFrame;

	Thread runningThread;

	@Override
	public void run() {
		generateGUI();
	}

	/**
	 * Create the GUI of the plugin
	 * */
	private void generateGUI()
	{
		mainFrame = new IcyFrame("Riesz-wavelets", true, true, false, true);

		JTabbedPane mainPanel = new JTabbedPane();
		mainFrame.setContentPane(mainPanel);

		JPanel analysisPanel = new JPanel(new GridBagLayout());
		JPanel analysisPanel2 = new JPanel(new BorderLayout());
		analysisPanel2.add(analysisPanel, BorderLayout.CENTER);
		mainPanel.addTab("Wavelet Analysis", analysisPanel2);
		GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(2, 2, 2, 2);

		JPanel waveletPanel = new JPanel(new BorderLayout());
		analysisPanel.add(waveletPanel, c);

		JPanel northPanel = new JPanel(new GridBagLayout());
		waveletPanel.add(northPanel, BorderLayout.NORTH);
		c.gridy = 0;
		northPanel.add(new JLabel("Wavelet coefficients source"), c);
		c.gridy++;
		northPanel.add(waveletSelectionBox, c);

		final JPanel centerPanel = new JPanel(wavCoeffCards);
		waveletPanel.add(centerPanel, BorderLayout.CENTER);

		wavCoeffSelectionPanel = new CoefficientSelectionPanel();
		wavCoeffSelectionPanel.setBorder(new TitledBorder("Wavelet coefficients selection"));
		centerPanel.add(wavCoeffSelectionPanel, selectWavCoeffString);

		wavCoeffConfigPanel = new WaveletConfigPanel();
		wavCoeffConfigPanel.setBorder(new TitledBorder("Wavelet analysis configuration"));
		centerPanel.add(wavCoeffConfigPanel, computeWavCoeffString);

		waveletSelectionBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				wavCoeffCards.show(centerPanel, waveletSelectionBox.getSelectedItem().toString());
			}});
		wavCoeffCards.show(centerPanel, waveletSelectionBox.getSelectedItem().toString());


		rieszConfigPanel = new RieszConfigurationPanel();
		analysisPanel.add(rieszConfigPanel, c);
		c.gridy++;

		JPanel actionPanel = new JPanel(new GridLayout(3, 1));
		analysisPanel2.add(actionPanel, BorderLayout.SOUTH);

		displayCoefficientsBox = new JCheckBox("Display coefficients");
		displayCoefficientsBox.setSelected(true);
		actionPanel.add(displayCoefficientsBox);

		processImageButton = new JButton("Process the focused image");
		processImageButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				processImage();
				//testTransform();
			}});
		actionPanel.add(processImageButton);

		processSequenceButton = new JButton("Process the whole sequence");
		actionPanel.add(processSequenceButton);
		processSequenceButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				processSequence();
			}});

		JPanel reconstructionPanel = new JPanel(new BorderLayout());
		coefficientSelectionPanel = new CoefficientSelectionPanel();
		reconstructionPanel.add(coefficientSelectionPanel, BorderLayout.CENTER);
		JPanel reconstructionSouthPanel = new JPanel(new GridLayout(2, 1));
		displayCoefficientsButton = new JButton("Display Coefficients");
		displayCoefficientsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				displayCurrentCoefficients();
			}
		});
		reconstructionSouthPanel.add(displayCoefficientsButton);
		reconstructButton = new JButton("Reconstruct image");
		reconstructButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reconstructCurrentCoefficients();
			}
		});
		reconstructionSouthPanel.add(reconstructButton);
		reconstructionPanel.add(reconstructionSouthPanel, BorderLayout.SOUTH);

		mainPanel.addTab("Reconstruction", reconstructionPanel);

		JPanel steeringPanel = new JPanel(new BorderLayout());
		mainPanel.addTab("Coefficient steering", steeringPanel);

		JPanel steeringCenterPanel = new JPanel(new GridLayout(2, 1));
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(2, 2, 2, 2);
		steeringPanel.add(steeringCenterPanel, BorderLayout.CENTER);

		steeringCoefficientSelectionPanel = new CoefficientSelectionPanel();
		steeringCoefficientSelectionPanel.setBorder(new TitledBorder("Riesz-wavelet coefficient selection"));
		steeringCenterPanel.add(steeringCoefficientSelectionPanel);
		c.gridy++;

		JPanel anglePanel = new JPanel(new BorderLayout());
		anglePanel.setBorder(new TitledBorder("Rotation angle selection"));
//		JPanel anglePanelNorth = new JPanel(new GridLayout(2, 1));
//		anglePanelNorth.add(new JLabel("Select angle data:"));
//		anglePanelNorth.add(angleBox);
//		anglePanel.add(anglePanelNorth, BorderLayout.NORTH);

//		final CardLayout angleCardLayout = new CardLayout();
//		final JPanel angleCardPanel = new JPanel(angleCardLayout);//TODO
//		anglePanel.add(angleCardPanel, BorderLayout.CENTER);

//		JPanel monogenicPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridheight = 1;
		c2.gridwidth = 1;
		c2.gridx = 0;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		c2.weighty = 1;
		c2.insets = new Insets(2, 2, 2, 2);
//		monogenicPanel.add(new JLabel("Monogenic regularization parameter:"), c2);
//		c2.gridy++;
//		monogenicRegularizationModel = new SpinnerNumberModel(1.5, 0.1, 10, 0.1);
//		JSpinner regularizationSpinner = new JSpinner(monogenicRegularizationModel);
//		monogenicPanel.add(regularizationSpinner, c2);
//		c2.gridy++;

//		angleCardPanel.add(monogenicPanel, useMonogenicAnalysis);
		JPanel angleSelectionCardPanel = new JPanel(new GridBagLayout());
		c2.gridy = 0;
		angleSelectionCardPanel.add(new JLabel("Use existing angles"), c2);
		c2.gridy++;
		angleChooser = new CustomChooser(AngleSequence.class);
		angleSelectionCardPanel.add(angleChooser, c2);
		c2.gridy++;
		angleSelectionCardPanel.add(new JLabel("Use the Monogenic Analysis plugin if empty."), c2);
//		angleCardPanel.add(angleSelectionCardPanel);
//		angleCardPanel.add(angleSelectionCardPanel, useExistingAngleResults);

//		angleBox.addItemListener(new ItemListener() {
//			@Override
//			public void itemStateChanged(ItemEvent arg0) {
//				angleCardLayout.show(angleCardPanel, angleBox.getSelectedItem().toString());
//			}
//		});
		
		anglePanel.setLayout(new BorderLayout());
		anglePanel.add(angleSelectionCardPanel, BorderLayout.CENTER);
		
		steeringCenterPanel.add(anglePanel);
		c.gridy++;

		JPanel steeringSouthPanel = new JPanel(new GridLayout(2, 1));
		steeringPanel.add(steeringSouthPanel, BorderLayout.SOUTH);
		steerCoefficientsButton = new JButton("Steer coefficients");
		steerCoefficientsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				steerCoefficients(true);
			}
		});
		steerCoefficientsBackButton = new JButton("Steer coefficients back");
		steerCoefficientsBackButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				steerCoefficients(false);
			}
		});
		steeringSouthPanel.add(steerCoefficientsButton);
		steeringSouthPanel.add(steerCoefficientsBackButton);

		mainFrame.setPreferredSize(new Dimension(400, 500));
		mainFrame.pack();
		addIcyFrame(mainFrame);
		mainFrame.setVisible(true);
	}

	
	/**
	 * Steer coefficients according to an angle map
	 * @param forward true if coefficients are to be steered forward, false for backward steering.
	 * */
	private void steerCoefficients(final boolean forward)
	{
		Object selectedItem = steeringCoefficientSelectionPanel.resultsBox.getSelectedItem();
		if (selectedItem != null)
		{
			final SequenceAnalysisResults results = (SequenceAnalysisResults) ((SwimmingObject) selectedItem).getObject();
//			final boolean isMonogenic = angleBox.getSelectedItem().toString().equals(useMonogenicAnalysis);
			final AngleSequence sequenceAngles = (AngleSequence) angleChooser.getSelectedObject();

//			Sequence sequenceForMonogenicAnalysis = null;
//			if (!isMonogenic)
//			{
//				sequenceAngles = (AngleSequence) angleChooser.getSelectedObject();
				if(sequenceAngles == null)
					return;
//			}
//			else
//			{
//				String seqName = results.getSequenceName();
//				Sequence seqToAnalyze = null;
//				for (Sequence seq:getSequences())
//				{
//					if (seq.getName().equals(seqName))
//					{
//						seqToAnalyze = seq;
//						break;
//					}
//				}
//				if (seqToAnalyze == null)
//				{
//					System.err.println("The sequence associated to the RieszWavelet coefficients is required to compute the angles of rotation with the monogenic analysis");
//					return;
//				}
//				sequenceForMonogenicAnalysis = seqToAnalyze;
//			}
//			final MonogenicSequenceAnalysisResults angles = sequenceAngles;
//			final Sequence seqToAnalyzeMonogenic = sequenceForMonogenicAnalysis;
//			final double sigma = monogenicRegularizationModel.getNumber().doubleValue();

			computationStarted();
			runningThread = new Thread()
			{
				@Override
				public void run()
				{
//					MonogenicSequenceAnalysisResults monogenicResults;
//					if (isMonogenic)
//					{
//						int numScales = results.getAllResults().get(0).getNumScales();
//						IsotropicWaveletType waveletType = results.getAllResults().get(0).getConfig().getWaveletFilterSet().getWaveletType();
//						boolean prefilter = results.getAllResults().get(0).getConfig().getWaveletFilterSet().isPrefilter();
//						monogenicResults = MonogenicAnalysis.computeMonogenicValues(seqToAnalyzeMonogenic, numScales, waveletType, prefilter, sigma, true, false, false);
//					}
//					else
//					{
//						monogenicResults = angles;
//					}

					ArrayList<Integer> analyzedTimes = results.getAllAnalyzedTimesResults();
					for (int t:analyzedTimes)
					{
						RieszWaveletCoefficients coefficients = results.getResult(t);
//						MonogenicAnalysisResult a  = monogenicResults.getResult(t);
						ArrayList<double[]> angleList = sequenceAngles.getAngles(t);
//						ArrayList<double[]> angleList = new ArrayList<double[]>();
//						for (int scale = 0; scale < coefficients.getNumScales(); scale++)
//							angleList.add(a.angle.get(scale));
						coefficients.getConfig().steerCoefficients(coefficients, angleList, forward);
					}
					displayCoefficients(results);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {computationEnded();}
					});
				}
			};
			runningThread.start();
		}
	}

	/**
	 * Reconstruct an image from coefficients selected in the GUI
	 * */
	private void reconstructCurrentCoefficients()
	{
		Object selectedItem = coefficientSelectionPanel.resultsBox.getSelectedItem();
		if (selectedItem != null)
		{
			final SequenceAnalysisResults results = (SequenceAnalysisResults) ((SwimmingObject) selectedItem).getObject();
			computationStarted();
			runningThread = new Thread()
			{
				@Override
				public void run()
				{
					ArrayList<RieszWaveletCoefficients> coeffList = results.getAllResults();
					ArrayList<Integer> timeList = results.getAllAnalyzedTimesResults();

					if (coeffList.isEmpty())
						return;	

					final HashMap<RieszWaveletCoefficients, Integer> mapResTime = new HashMap<RieszWaveletCoefficients, Integer>();
					for (int cnt = 0; cnt < coeffList.size(); cnt++)
						mapResTime.put(coeffList.get(cnt), timeList.get(cnt));
					Collections.sort(coeffList, new Comparator<RieszWaveletCoefficients>() {
						@Override
						public int compare(RieszWaveletCoefficients o1,
								RieszWaveletCoefficients o2) {
							return mapResTime.get(o1).compareTo(mapResTime.get(o2));
						}
					});

					// reconstruct image from coefficients
					Sequence reconstructedSeq = new Sequence("Reconstructed Riesz-wavelet image");
					for (int i = 0; i < coeffList.size(); i++)
					{
						RieszWaveletCoefficients coeffs = coeffList.get(i);
						RieszWaveletConfig config = coeffs.getConfig();

						int width =  config.getWaveletFilterSet().getWidth();
						int height =  config.getWaveletFilterSet().getHeight();
						double[] reconstructedImage = config.multiscaleRieszSynthesisInFourier(coeffs, width, height);
						reconstructedSeq.addImage(i, new IcyBufferedImage(width, height, reconstructedImage));
					}
					Icy.getMainInterface().addSequence(reconstructedSeq);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {computationEnded();}
					});
				}
			};
			runningThread.start();
		}
	}

	/**
	 * Display coefficients selected in the GUI as ICY sequences
	 * */
	private void displayCurrentCoefficients()
	{
		Object selectedItem = coefficientSelectionPanel.resultsBox.getSelectedItem();
		if (selectedItem != null)
		{
			final SequenceAnalysisResults results = (SequenceAnalysisResults) ((SwimmingObject) selectedItem).getObject();
			computationStarted();

			runningThread = new Thread()
			{
				@Override
				public void run()
				{
					displayCoefficients(results);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {computationEnded();}
					});
				}
			};
			runningThread.start();
		}
	}

	/**
	 * Compute Riesz-wavelet coefficients for the active image
	 * */
	private void processImage()
	{
		final Sequence seq = getActiveSequence();
		if ( seq != null)
		{
			final int t = seq.getFirstViewer().getPositionT();
			final int width = seq.getSizeX();
			final int height = seq.getSizeY();

			// Wavelet transform
			final int numScales = wavCoeffConfigPanel.getNumScales();
			final boolean prefilter = wavCoeffConfigPanel.isPrefilter();
			final IsotropicWaveletType waveletType = wavCoeffConfigPanel.getWaveletType();
			final boolean isotropicPadding = wavCoeffConfigPanel.isIsotropicPadding();

			// Riesz transform
			final int order = rieszConfigPanel.getOrder();
			final HarmonicTypes harmonicType = rieszConfigPanel.getHarmonicType();
			final boolean prepareRieszFilters = true;
			final boolean isReal = true;
			final StandardRieszFrames rieszFrame = rieszConfigPanel.getStandardRiezGeneralization();
			final boolean isDisplayCoefficients = displayCoefficientsBox.isSelected();

			computationStarted();

			runningThread = new Thread()
			{
				@Override
				public void run()
				{
					RieszWaveletConfig config = new RieszWaveletConfig(width, height, isReal, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicType, isotropicPadding);
					ArrayList<RieszGeneralization> generalizationList = new ArrayList<RieszGeneralization>();
					for (int i = 0; i < config.rieszConfigList.size(); i++)
					{
						RieszGeneralization rieszGeneralization = new RieszGeneralization(rieszFrame, config.rieszConfigList.get(i));
						generalizationList.add(rieszGeneralization);
					}	
					double[] image =  (double[])ArrayUtil.arrayToDoubleArray(seq.getImage(t, 0, 0).getDataXY(0), seq.isSignedDataType());
					RieszWaveletCoefficients coefficients  = config.multiscaleRieszAnalysisInFourier(image, width, height, generalizationList);
					SequenceAnalysisResults results = new SequenceAnalysisResults(seq.getName());
					results.setResult(t, coefficients);
					Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(results, "Riesz-wavelet coefficients for frame of "+results.getSequenceName()));

					if (isDisplayCoefficients)
					{
						for (int n = 0; n < numScales; n++)
							coefficients.displayCoefficients(n);
					}

					SwingUtilities.invokeLater(new Runnable() {						
						@Override
						public void run() {computationEnded();}
					});
				}
			};
			runningThread.start();			
		}
	}

	/**
	 * Compute Riesz-wavelet coefficients for the active sequence of images
	 * */
	private void processSequence()
	{
		final Sequence seq = getActiveSequence();
		if ( seq != null)
		{
			final int width = seq.getSizeX();
			final int height = seq.getSizeY();

			// Wavelet transform
			final int numScales = wavCoeffConfigPanel.getNumScales();
			final boolean prefilter = wavCoeffConfigPanel.isPrefilter();
			final IsotropicWaveletType waveletType = wavCoeffConfigPanel.getWaveletType();
			final boolean isotropicPadding = wavCoeffConfigPanel.isIsotropicPadding();

			// Riesz transform
			final int order = rieszConfigPanel.getOrder();
			final HarmonicTypes harmonicType = rieszConfigPanel.getHarmonicType();
			final boolean prepareRieszFilters = true;
			final boolean isReal = true;
			final StandardRieszFrames rieszFrame = rieszConfigPanel.getStandardRiezGeneralization();
			final boolean isDisplayCoefficients = displayCoefficientsBox.isSelected();

			computationStarted();

			runningThread = new Thread()
			{
				@Override
				public void run()
				{
					RieszWaveletConfig config = new RieszWaveletConfig(width, height, isReal, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicType, isotropicPadding);
					ArrayList<RieszGeneralization> generalizationList = new ArrayList<RieszGeneralization>();
					for (int i = 0; i < config.rieszConfigList.size(); i++)
					{
						RieszGeneralization rieszGeneralization = new RieszGeneralization(rieszFrame, config.rieszConfigList.get(i));
						generalizationList.add(rieszGeneralization);
					}
					SequenceAnalysisResults results = new SequenceAnalysisResults(seq.getName());
					for (int t = 0; t < seq.getSizeT(); t++)
					{
						double[] image =  (double[])ArrayUtil.arrayToDoubleArray(seq.getImage(t, 0, 0).getDataXY(0), seq.isSignedDataType());
						RieszWaveletCoefficients coefficients  = config.multiscaleRieszAnalysisInFourier(image, width, height, generalizationList);
						results.setResult(t, coefficients);
					}
					Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(results, "Riesz-wavelet coefficients for "+results.getSequenceName()));

					if (isDisplayCoefficients)
						displayCoefficients(results);
					SwingUtilities.invokeLater(new Runnable() {						
						@Override
						public void run() {computationEnded();}
					});
				}
			};
			runningThread.start();			
		}
	}

	/**
	 * Disable GUI during coefficients computation
	 * */
	private void computationStarted() {
		processImageButton.setEnabled(false);
		processSequenceButton.setEnabled(false);
		displayCoefficientsButton.setEnabled(false);
		reconstructButton.setEnabled(false);
		announceFrame = new AnnounceFrame("Computation started");
	}

	/**
	 * Re-enable GUI after coefficients computation has ended
	 * */
	private void computationEnded() {
		processImageButton.setEnabled(true);
		processSequenceButton.setEnabled(true);
		displayCoefficientsButton.setEnabled(true);
		reconstructButton.setEnabled(true);
		announceFrame.close();
	}

	/**
	 * Quick test of the transform library
	 * */
	private static void testTransform()
	{
		// get the sequence to analyze
		Sequence seq = Icy.getMainInterface().getActiveSequence();
		if (seq == null)
			return;
		int height = seq.getSizeY();
		int width = seq.getSizeX();
		boolean isRealImage = true;

		// set up the transform

		// Wavelets
		int numScales = 4;
		boolean prefilter = true;
		IsotropicWaveletType waveletType = IsotropicWaveletType.Simoncelli;

		// Riesz transform
		int order = 3;
		HarmonicTypes harmonicType = HarmonicTypes.odd;
		boolean prepareRieszFilters = true;

		RieszWaveletConfig config = new RieszWaveletConfig(width, height, isRealImage, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicType);
		config.displayRieszFilters(0);

		ArrayList<RieszGeneralization> generalizationList = new ArrayList<RieszGeneralization>();
		for (int i = 0; i < config.rieszConfigList.size(); i++)
		{
			RieszGeneralization rieszGeneralization = new RieszGeneralization(StandardRieszFrames.CircularHarmonics, config.rieszConfigList.get(i));
			generalizationList.add(rieszGeneralization);
			//	rieszGeneralization.printForwardMatrix();
			//	rieszGeneralization.printBackwardMatrix();
		}

		// apply the riesz transforms to the wavelet scales
		double[] image =  (double[])ArrayUtil.arrayToDoubleArray(seq.getImage(0, 0, 0).getDataXY(0), seq.isSignedDataType());
		RieszWaveletCoefficients coefficients  = config.multiscaleRieszAnalysisInFourier(image, width, height, generalizationList);
		coefficients.displayCoefficients(0);

		// reconstruct image from coefficients
		double[] reconstructedImage = config.multiscaleRieszSynthesisInFourier(coefficients, width, height);
		Sequence reconstructedSeq = new Sequence("Reconstructed Riesz-wavelet image");
		reconstructedSeq.addImage(0, new IcyBufferedImage(width, height, reconstructedImage));
		Icy.getMainInterface().addSequence(reconstructedSeq);
	}

	/**
	 * Display a set of coefficients as ICY sequences of images
	 * */
	public void displayCoefficients(SequenceAnalysisResults results)
	{
		ArrayList<RieszWaveletCoefficients> coeffList = results.getAllResults();
		ArrayList<Integer> timeList = results.getAllAnalyzedTimesResults();

		if (coeffList.isEmpty())
			return;	

		int numScales = coeffList.get(0).getNumScales();
		final HashMap<RieszWaveletCoefficients, Integer> mapResTime = new HashMap<RieszWaveletCoefficients, Integer>();
		for (int cnt = 0; cnt < coeffList.size(); cnt++)
			mapResTime.put(coeffList.get(cnt), timeList.get(cnt));
		Collections.sort(coeffList, new Comparator<RieszWaveletCoefficients>() {
			@Override
			public int compare(RieszWaveletCoefficients o1,
					RieszWaveletCoefficients o2) {
				return mapResTime.get(o1).compareTo(mapResTime.get(o2));
			}
		});

		// create output sequences
		ArrayList<Sequence> waveletBandsSequenceList = new ArrayList<Sequence>(numScales);
		for (int scale = 0; scale < numScales; scale++)
		{
			Sequence scaleSeq = new Sequence();
			scaleSeq.setName("scale "+ scale);
			waveletBandsSequenceList.add(scaleSeq);
			Icy.getMainInterface().addSequence(scaleSeq);
		}

		//		Sequence lpSequence = new Sequence();
		//		lpSequence.setName("Low frequency residual");
		//		Icy.getMainInterface().addSequence(lpSequence);
		//
		//		Sequence hpSequence = null;
		//		if (coeffList.get(0).getHPResidual() != null)
		//		{
		//			hpSequence = new Sequence();
		//			hpSequence.setName("High frequency residual");
		//			Icy.getMainInterface().addSequence(hpSequence);
		//		}
		for (int t = 0; t < coeffList.size(); t++)
		{
			RieszWaveletCoefficients coefficients = coeffList.get(t);
			WaveletFilterSet waveletConfig = coefficients.getConfig().getWaveletFilterSet();
			for (int scale = 0; scale < numScales; scale++)
			{	
				double[][] dataTab = coefficients.getRieszBandsAtScale(scale);
				for (int k = 0; k < dataTab.length; k++)
				{
					IcyBufferedImage bandImage = new IcyBufferedImage(waveletConfig.getScaleWidth(scale), waveletConfig.getScaleHeight(scale), dataTab[k]);
					Sequence scaleSeq = waveletBandsSequenceList.get(scale);
					scaleSeq.setImage(t, k, bandImage);
				}
			}
			//			IcyBufferedImage lpImage = new IcyBufferedImage(waveletConfig.getLPWidth(), waveletConfig.getLPHeight(), coefficients.getLPResidual());
			//			lpSequence.addImage(t, lpImage);
			//			if (hpSequence != null)
			//			{
			//				IcyBufferedImage hpImage = new IcyBufferedImage(waveletConfig.getHPWidth(), waveletConfig.getHPHeight(), coefficients.getHPResidual());
			//				hpSequence.addImage(t, hpImage);
			//			}
		}
	}
	
}
