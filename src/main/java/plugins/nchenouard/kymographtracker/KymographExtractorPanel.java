package plugins.nchenouard.kymographtracker;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.type.DataType;
import icy.type.collection.array.ArrayUtil;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.isotropicwavelets.IsotropicWaveletType;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;
import plugins.nchenouard.pathtracing.InteractiveMultipleDjikstraTracingESC;
import plugins.nchenouard.pathtracing.InteractiveMultipleTracing;
import plugins.nchenouard.pathtracing.PathEvent;
import plugins.nchenouard.pathtracing.PathListener;
import plugins.nchenouard.rieszwavelets.HarmonicTypes;
import plugins.nchenouard.rieszwavelets.RieszConfig;
import plugins.nchenouard.rieszwavelets.RieszGeneralization;
import plugins.nchenouard.rieszwavelets.RieszWaveletCoefficients;
import plugins.nchenouard.rieszwavelets.RieszWaveletConfig;
import plugins.nchenouard.rieszwavelets.StandardRieszFrames;

class KymographExtractorPanel extends ActionPanel implements PathListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3741312080376160439L;

	JLabel tracingChoiceLabel;

	JRadioButton useOriginalSequenceButton;

	JRadioButton useOriginalSequenceROIButton;

	JRadioButton traceInMaxProjectionButton;

	JRadioButton traceInFilteredProjectionButton;

	ButtonGroup buttonGroup = new ButtonGroup();

	JPanel descriptionPanel;

	JPanel originalTracingPanel;

	JEditorPane originalTracingDescription;

	JPanel maxTracingPanel;

	JEditorPane maxTracingDescription;

	JPanel roiTracingPanel;

	JEditorPane roiTrackingDescription;

	JCheckBox separateAnteroRetroBox;

	JButton extractKymographsButton;

	JButton startTrackingButton;

	NumberFormat diskFormat = NumberFormat.getNumberInstance();

	JFormattedTextField diskRadiusField = new JFormattedTextField( diskFormat );

	Sequence selectedSequence = null;

	Sequence maxProjectionSequence = null;

	Sequence filteredProjectionSequence;

	double alpha = 0.01;

	boolean isEnabled = true;

	public KymographExtractorPanel( final boolean isEnabled )
	{
		description = "Kymograph Extraction";
		node = new DefaultMutableTreeNode( description );
		this.setBorder( new TitledBorder( description ) );

		this.setLayout( new BorderLayout() );

		final JPanel northPanel = new JPanel( new GridBagLayout() );
		this.add( northPanel, BorderLayout.NORTH );

		final GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.0;
		c.insets = new Insets( 2, 2, 2, 2 );

		tracingChoiceLabel = new JLabel( "Kymograph extraction path:" );
		northPanel.add( tracingChoiceLabel, c );
		c.gridy++;

		useOriginalSequenceROIButton = new JRadioButton( "use ROIs in the original sequence,", true );
		useOriginalSequenceROIButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				if ( useOriginalSequenceROIButton.isSelected() )
					selectedButtonChanged( useOriginalSequenceROIButton );
			}
		} );
		northPanel.add( useOriginalSequenceROIButton, c );
		c.gridy++;

		northPanel.add( new JLabel( "or trace a path:" ), c );
		c.gridy++;

		useOriginalSequenceButton = new JRadioButton( "in the original sequence,", false );
		useOriginalSequenceButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				if ( useOriginalSequenceButton.isSelected() )
					selectedButtonChanged( useOriginalSequenceButton );
			}
		} );
		northPanel.add( useOriginalSequenceButton, c );
		c.gridy++;

		// traceInMaxProjectionButton = new JRadioButton("in the maximum
		// projection of the sequence,", false);
		// traceInMaxProjectionButton.addActionListener(new ActionListener() {
		// @Override
		// public void actionPerformed(ActionEvent arg0) {
		// if (traceInMaxProjectionButton.isSelected())
		// selectedButtonChanged(traceInMaxProjectionButton);
		// }
		// });
		// northPanel.add(traceInMaxProjectionButton, c);
		// c.gridy ++;

		traceInFilteredProjectionButton = new JRadioButton( "in an enhanced projection of the sequence.", false );
		traceInFilteredProjectionButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				if ( traceInFilteredProjectionButton.isSelected() )
					selectedButtonChanged( traceInFilteredProjectionButton );
			}
		} );
		northPanel.add( traceInFilteredProjectionButton, c );
		c.gridy++;

		buttonGroup.add( useOriginalSequenceButton );
		buttonGroup.add( traceInMaxProjectionButton );
		buttonGroup.add( traceInFilteredProjectionButton );
		buttonGroup.add( useOriginalSequenceROIButton );
		useOriginalSequenceROIButton.setSelected( true );

		roiTracingPanel = new JPanel( new BorderLayout() );
		roiTrackingDescription = new JEditorPane();
		// roiTrackingDescription.setPreferredSize(new Dimension(100, 100));
		roiTrackingDescription.setContentType( "text/html" );
		roiTrackingDescription.setEditable( false );
		roiTrackingDescription.setText( "Use the <strong>ROIs in the current sequence</strong> as the paths along which to extract the kymographs." );
		roiTrackingDescription.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
		roiTrackingDescription.setEditable( false );
		roiTracingPanel.add( roiTrackingDescription, BorderLayout.CENTER );

		originalTracingPanel = new JPanel( new BorderLayout() );
		originalTracingDescription = new JEditorPane();
		// originalTracingDescription.setPreferredSize(new Dimension(100, 100));
		originalTracingDescription.setContentType( "text/html" );
		originalTracingDescription.setEditable( false );
		originalTracingDescription.setText( "<strong>Trace extraction paths</strong> in the sequence to analyze. Multiple paths can be traced." );
		originalTracingDescription.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
		originalTracingDescription.setEditable( false );
		originalTracingPanel.add( originalTracingDescription, BorderLayout.CENTER );

		maxTracingPanel = new JPanel( new BorderLayout() );
		maxTracingDescription = new JEditorPane();

		maxTracingDescription.setEditable( false );
		maxTracingDescription.setContentType( "text/html" );
		// maxTracingDescription.setPreferredSize(new Dimension(100, 100));
		maxTracingDescription.setEditable( false );
		maxTracingDescription.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
		maxTracingDescription.setText( "<strong>Trace extraction paths</strong> in an enhanced projection of the sequence. Multiple paths can be traced." );
		maxTracingPanel.add( maxTracingDescription, BorderLayout.CENTER );

		descriptionPanel = new JPanel();
		northPanel.add( descriptionPanel, c );
		c.gridy++;
		descriptionPanel.setLayout( new CardLayout() );
		descriptionPanel.add( originalTracingPanel, useOriginalSequenceButton.getText() );
		// descriptionPanel.add(maxTracingPanel,
		// traceInMaxProjectionButton.getText());
		descriptionPanel.add( maxTracingPanel, traceInFilteredProjectionButton.getText() );
		descriptionPanel.add( roiTracingPanel, useOriginalSequenceROIButton.getText() );

		separateAnteroRetroBox = new JCheckBox( "Split anterograde and retrograde" );
		separateAnteroRetroBox.setToolTipText( "Separate anterograde and retrograde moving particles in distinct kymographs" );
		northPanel.add( separateAnteroRetroBox, c );
		c.gridy++;

		northPanel.add( new JLabel( "Radius of the averaging area:" ), c );
		c.gridy++;
		diskRadiusField.setValue( 2 );
		northPanel.add( diskRadiusField, c );
		c.gridy++;

		final JPanel southPanel = new JPanel( new GridLayout( 2, 1 ) );

		extractKymographsButton = new JButton( "Extract kymographs" );
		southPanel.add( extractKymographsButton );
		c.gridy++;

		extractKymographsButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				extractKymographs();
			}
		} );

		startTrackingButton = new JButton( "Trace tracks in kymographs" );
		southPanel.add( startTrackingButton, BorderLayout.CENTER );
		this.add( southPanel, BorderLayout.SOUTH );

		selectedButtonChanged( useOriginalSequenceROIButton );
		enableGUI( isEnabled );
	}

	private void selectedButtonChanged( final JRadioButton selectedButton )
	{
		( ( CardLayout ) descriptionPanel.getLayout() ).show( descriptionPanel, selectedButton.getText() );
		stopTracers();
		startTracers();
	}

	public void enableGUI( final boolean enable )
	{
		if ( enable != isEnabled )
		{
			isEnabled = enable;
			// tracingChoiceLabel.setEnabled(enable);
			// useOriginalSequenceButton.setEnabled(enable);
			// useOriginalSequenceROIButton.setEnabled(enable);
			// traceInMaxProjectionButton.setEnabled(enable);
			// originalTracingDescription.setEnabled(enable);
			// maxTracingDescription.setEnabled(enable);
			// roiTrackingDescription.setEnabled(enable);
			// separateAnteroRetroBox.setEnabled(enable);
			// extractKymographsButton.setEnabled(enable);
			// startTrackingButton.setEnabled(enable);
			if ( !enable ) // stop tracing activities
				stopTracers();
			else
				startTracers();
		}
	}

	private void stopTracers()
	{
		// remove previous tracing painters from sequence
		if ( selectedSequence != null )
		{
			for ( final Overlay painter : selectedSequence.getOverlays( InteractiveMultipleDjikstraTracingESC.class ) )
			{
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).removePathLister( this );
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).stopTracing();
				selectedSequence.removeOverlay( painter );
			}
		}
		if ( maxProjectionSequence != null )
		{
			for ( final Overlay painter : maxProjectionSequence.getOverlays( InteractiveMultipleDjikstraTracingESC.class ) )
			{
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).removePathLister( this );
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).stopTracing();
				maxProjectionSequence.removeOverlay( painter );
			}
		}
		if ( filteredProjectionSequence != null )
		{
			for ( final Overlay painter : filteredProjectionSequence.getOverlays( InteractiveMultipleDjikstraTracingESC.class ) )
			{
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).removePathLister( this );
				( ( InteractiveMultipleDjikstraTracingESC ) painter ).stopTracing();
				filteredProjectionSequence.removeOverlay( painter );
			}
		}
	}

	private void startTracers()
	{
		// add tracing painters to newly selected sequence
		// if (isEnabled && selectedSequence != null)
		if ( selectedSequence != null )
		{
			if ( useOriginalSequenceButton.isSelected() )
			{
				final InteractiveMultipleDjikstraTracingESC tracer = new InteractiveMultipleDjikstraTracingESC( selectedSequence, alpha, false );
				selectedSequence.addOverlay( tracer );
				tracer.addPathLister( this );
			}
			maxProjectionSequence = null;
			// if (traceInMaxProjectionButton.isSelected())
			// {
			// maxProjectionSequence =
			// getMaxProjectionSequence(selectedSequence);
			// Icy.getMainInterface().addSequence(maxProjectionSequence);
			// InteractiveMultipleDjikstraTracingESC tracer = new
			// InteractiveMultipleDjikstraTracingESC(selectedSequence, alpha,
			// false);
			// maxProjectionSequence.addPainter(tracer);
			// tracer.addPathLister(this);
			// }
			filteredProjectionSequence = null;
			if ( traceInFilteredProjectionButton.isSelected() )
			{
//				filteredProjectionSequence = getFilteredProjectionSequenceWithRiesz(selectedSequence);
				filteredProjectionSequence = getMaxMinDiffProjectionSequence( selectedSequence );

				Icy.getMainInterface().addSequence( filteredProjectionSequence );
				final InteractiveMultipleDjikstraTracingESC tracer = new InteractiveMultipleDjikstraTracingESC( filteredProjectionSequence, alpha, false );
				filteredProjectionSequence.addOverlay( tracer );
				tracer.addPathLister( this );
			}
		}
	}

	@Override
	protected void changeSelectedSequence( final Sequence sequence )
	{
		stopTracers();
		selectedSequence = sequence;
		startTracers();
	}

	Sequence getMaxProjectionSequence( final Sequence seq )
	{
		Sequence projectionSequence = null;
		if ( seq != null )
		{
			projectionSequence = new Sequence( "Projection_" + seq.getName() );
			projectionSequence.setImage( 0, 0, new IcyBufferedImage( seq.getSizeX(), seq.getSizeY(), 1, DataType.DOUBLE ) );
			final double[] tabValues = projectionSequence.getImage( 0, 0, 0 ).getDataXYAsDouble( 0 );
			// for (int y = 0; y < seq.getSizeY(); y++)
			// for (int x = 0; x < seq.getSizeX(); x++)
			// {
			// double maxVal = seq.getData(0, 0, 0, y, x);
			// for (int t = 1; t < seq.getSizeT(); t++)
			// if (maxVal < seq.getData(t, 0, 0, y, x))
			// maxVal = seq.getData(t, 0, 0, y, x);
			// tabValues[x + y*seq.getWidth()] = maxVal;
			// }
			for ( int t = 0; t < seq.getSizeT(); t++ )
			{
				final double[] image = ( double[] ) ArrayUtil.arrayToDoubleArray( seq.getImage( t, 0, 0 ).getDataXY( 0 ), seq.isSignedDataType() );
				for ( int i = 0; i < tabValues.length; i++ )
					if ( image[ i ] > tabValues[ i ] )
						tabValues[ i ] = image[ i ];
			}
			projectionSequence.dataChanged();
		}
		return projectionSequence;
	}

	Sequence getMaxMinDiffProjectionSequence( final Sequence seq )
	{
		Sequence projectionSequence = null;
		if ( seq != null )
		{
			projectionSequence = new Sequence( "Projection_" + seq.getName() );
			projectionSequence.setImage( 0, 0, new IcyBufferedImage( seq.getSizeX(), seq.getSizeY(), 1, DataType.DOUBLE ) );
			final double[] tabValues = projectionSequence.getImage( 0, 0, 0 ).getDataXYAsDouble( 0 );
			final double[] minValues = tabValues.clone();
			for ( int t = 0; t < seq.getSizeT(); t++ )
			{
				final double[] image = ( double[] ) ArrayUtil.arrayToDoubleArray( seq.getImage( t, 0, 0 ).getDataXY( 0 ), seq.isSignedDataType() );
				for ( int i = 0; i < tabValues.length; i++ )
					if ( image[ i ] > tabValues[ i ] )
						tabValues[ i ] = image[ i ];
					else if ( image[ i ] < minValues[ i ] )
						minValues[ i ] = image[ i ];
			}
			for ( int i = 0; i < tabValues[ i ]; i++ )
				tabValues[ i ] -= minValues[ i ];

			projectionSequence.dataChanged();
		}
		return projectionSequence;
	}

	Sequence getFilteredProjectionSequenceWithRiesz( final Sequence seq )
	{

		// compute the Simoncelli's wavelet representation of the sequence
		final int height = seq.getSizeY();
		final int width = seq.getSizeX();
		final boolean isRealImage = true;
		// Wavelets
		int numScales = 4;
		int tmpNumScales = 0;
		for ( int i = 0; i < numScales; i++ )
		{
			if ( height < Math.pow( 2, i ) || width < Math.pow( 2, i ) )
			{
				break;
			}
			tmpNumScales = i;
		}
		numScales = tmpNumScales;

		final boolean prefilter = false;
		final IsotropicWaveletType waveletType = IsotropicWaveletType.Simoncelli;
		// Riesz transform
		final int order = 8;
		final HarmonicTypes harmonicType = HarmonicTypes.even;
		final boolean prepareRieszFilters = true;

		final RieszWaveletConfig config = new RieszWaveletConfig( width, height, isRealImage, numScales, waveletType, prefilter, prepareRieszFilters, order, harmonicType, false );
		final ArrayList< RieszConfig > rieszConfigList = config.getRieszConfigurations();

		// compute generalization
		final ArrayList< RieszGeneralization > generalizationList = new ArrayList< RieszGeneralization >();
		for ( int i = 0; i < numScales; i++ )
		{
			final RieszGeneralization rieszGeneralization = new RieszGeneralization( StandardRieszFrames.Simoncelli, rieszConfigList.get( i ) );
			generalizationList.add( rieszGeneralization );
		}

		// get the max projection of the sequence
		final double[] tabValues = ( double[] ) ArrayUtil.arrayToDoubleArray( seq.getImage( 0, 0, 0 ).getDataXY( 0 ), seq.isSignedDataType() );
		for ( int t = 1; t < seq.getSizeT(); t++ )
		{
			final double[] image = ( double[] ) ArrayUtil.arrayToDoubleArray( seq.getImage( t, 0, 0 ).getDataXY( 0 ), seq.isSignedDataType() );
			for ( int i = 0; i < tabValues.length; i++ )
				if ( image[ i ] > tabValues[ i ] )
					tabValues[ i ] = image[ i ];
		}

		// apply the riesz transforms to the wavelet scales
		final RieszWaveletCoefficients coefficients = config.multiscaleRieszAnalysisInFourier( tabValues, width, height, generalizationList );
		// keep only the 25 percent coefficients in each band
		final double[] hpR = coefficients.getHPResidual();
		if ( hpR != null )
		{
			final double[] tmp = hpR.clone();
			for ( int i = 0; i < tmp.length; i++ )
				tmp[ i ] = tmp[ i ] * tmp[ i ];
			Arrays.sort( tmp );
			final double threshold = tmp[ ( int ) ( tmp.length * 0.75 ) ];
			for ( int i = 0; i < tmp.length; i++ )
				if ( hpR[ i ] * hpR[ i ] < threshold )
					hpR[ i ] = 0;
		}
		final double[] lpR = coefficients.getLPResidual();
		for ( int i = 0; i < lpR.length; i++ )
			lpR[ i ] = 0;
		for ( int scale = 0; scale < coefficients.getNumScales(); scale++ )
		{
			final double[][] c = coefficients.getRieszBandsAtScale( scale );
			for ( int b = 0; b < c.length; b++ )
			{
				final double[] tmp = c[ b ].clone();
				for ( int i = 0; i < tmp.length; i++ )
					tmp[ i ] = tmp[ i ] * tmp[ i ];
				Arrays.sort( tmp );
				final double threshold = tmp[ ( int ) ( tmp.length * 0.75 ) ];
				for ( int i = 0; i < tmp.length; i++ )
					if ( c[ b ][ i ] * c[ b ][ i ] < threshold )
						c[ b ][ i ] = 0;
			}
		}
		// reconstruct image from coefficients
		final Sequence recSeq = new Sequence();
		final double[] reconstructedImage = config.multiscaleRieszSynthesisInFourier( coefficients, width, height );
		double minV = reconstructedImage[ 0 ];
		for ( int i = 0; i < reconstructedImage.length; i++ )
			if ( reconstructedImage[ i ] < minV )
				minV = reconstructedImage[ i ];
		for ( int i = 0; i < reconstructedImage.length; i++ )
			reconstructedImage[ i ] -= minV;
		recSeq.addImage( 0, new IcyBufferedImage( width, height, reconstructedImage ) );
		return recSeq;
	}

	Sequence getFilteredProjectionSequence( final Sequence seq )
	{
		// compute pixelwise variance over time windows of length 50 and keep
		// the maximum value
		Sequence projectionSequence = null;
		if ( seq != null )
		{
			final double[] maxVar = new double[ seq.getSizeX() * seq.getSizeY() ];
			final double[] sum = new double[ seq.getSizeX() * seq.getSizeY() ];
			final double[] sumSq = new double[ seq.getSizeX() * seq.getSizeY() ];

			final int windowLength = 20;
			for ( int t1 = 0; t1 < seq.getSizeT(); t1 += windowLength / 2 )
			{
				for ( int i = 0; i < sum.length; i++ )
				{
					sum[ i ] = 0;
					sumSq[ i ] = 0;
				}
				int t2 = t1 + windowLength;
				if ( t2 > seq.getSizeT() )
					t2 = seq.getSizeT();
				for ( int t = t1; t < t2; t++ )
				{
					final double[] image = ( double[] ) ArrayUtil.arrayToDoubleArray( seq.getImage( t, 0, 0 ).getDataXY( 0 ), seq.isSignedDataType() );
					for ( int i = 0; i < image.length; i++ )
					{
						sum[ i ] += image[ i ];
						sumSq[ i ] += image[ i ] * image[ i ];
					}
				}
				for ( int i = 0; i < sum.length; i++ )
				{
					final double var = sumSq[ i ] / ( t2 - t1 ) - sum[ i ] * sum[ i ] / ( ( t2 - t1 ) * ( t2 - t1 ) );
					if ( var > maxVar[ i ] )
						maxVar[ i ] = var;
				}
			}
			for ( int i = 0; i < maxVar.length; i++ )
			{
				maxVar[ i ] = Math.sqrt( maxVar[ i ] );
			}
			projectionSequence = new Sequence( "Filtered projection_" + seq.getName() );
			projectionSequence.addImage( 0, new IcyBufferedImage( seq.getSizeX(), seq.getSizeY(), maxVar ) );
		}
		return projectionSequence;
	}

	@Override
	public void refreshPath( final PathEvent event, final Object source, final double[][] path )
	{
		if ( event == PathEvent.FINAL_PATH )
		{
			// convert to a ROI in the original sequence
			final InteractiveMultipleTracing tracer = ( InteractiveMultipleTracing ) source;
			final ArrayList< double[][] > paths = tracer.getOptimalPathCopy();
			final ROI roi = Util.convertPathToROI( selectedSequence, paths );
			int cntROI = 1;
			boolean notFound = false;
			String name = "Path_" + cntROI;
			while ( !notFound )
			{
				notFound = true;
				name = "Path_" + cntROI;
				for ( final ROI r : selectedSequence.getROIs() )
				{
					if ( r.getName().equals( name ) )
						notFound = false;
				}
				cntROI++;
			}
			roi.setName( name );
			selectedSequence.addROI( roi );
		}
	}

	protected void extractKymographs()
	{
		if ( selectedSequence != null )
		{
			double diskRadius = 2;
			try
			{
				diskRadius = diskFormat.parse( diskRadiusField.getText() ).doubleValue();
			}
			catch ( final ParseException e )
			{
				e.printStackTrace();
				return;
			}
			diskRadius = Math.max( 1e-6, diskRadius );
			final ROItoKymograph extractor = new ROItoKymograph( diskRadius, 1 );
			for ( final ROI2D roi : selectedSequence.getROI2Ds() )
			{
				if ( roi instanceof ROI2DShape )
				{
					// ArrayList<double[]> samplingPositions =
					// extractor.computeSamplingPositions(selectedSequence,
					// (ROI2DShape) roi, true);
					if ( separateAnteroRetroBox.isSelected() )
					{
						// Sequence[] kymographs =
						// extractor.getAnteroRetroKymographSequenceFromDisks(selectedSequence,
						// samplingPositions);

						Sequence[] kymographs = null;
						// if (roi instanceof SplineROI)
						// {
						// SplineROI roiSpline = (SplineROI) roi;
						// kymographs =
						// extractor.getAnteroRetroKymographSequence(selectedSequence,
						// roiSpline.length, roiSpline.xSpline,
						// roiSpline.ySpline);
						// }
						final CubicSmoothingSpline xSpline = Util.getXsplineFromROI( ( ROI2DShape ) roi );
						final CubicSmoothingSpline ySpline = Util.getYsplineFromROI( ( ROI2DShape ) roi );
						final double length = Util.getSplineLength( ( ROI2DShape ) roi );
						kymographs = extractor.getAnteroRetroKymographSequence( selectedSequence, length, xSpline, ySpline );

						final Sequence kymo = kymographs[ 0 ];
						kymo.setName( roi.getName() + "_kymograph" );
						Icy.getMainInterface().addSequence( kymo );

						final Sequence anteroKymo = kymographs[ 1 ];
						anteroKymo.setName( roi.getName() + "_anteroKymograph" );
						Icy.getMainInterface().addSequence( anteroKymo );

						final Sequence retroKymo = kymographs[ 2 ];
						retroKymo.setName( roi.getName() + "_retroKymograph" );
						Icy.getMainInterface().addSequence( retroKymo );
						final KymographExtractionResult result = new KymographExtractionResult();
						result.roi = roi;
						result.setKymograph( kymo );
						result.anterogradeRetrogradeSeparation = true;
						result.sourceSequence = selectedSequence;
						result.setAnterogradeKymograph( anteroKymo );
						result.setRetrogradeKymograph( retroKymo );
						result.samplingPositions = extractor.samplingPositions;
						Icy.getMainInterface().getSwimmingPool().add( new SwimmingObject( result ) );
					}
					else
					{
						Sequence kymograph = null;
						final CubicSmoothingSpline xSpline = Util.getXsplineFromROI( ( ROI2DShape ) roi );
						final CubicSmoothingSpline ySpline = Util.getYsplineFromROI( ( ROI2DShape ) roi );
						final double length = Util.getSplineLength( ( ROI2DShape ) roi );
						kymograph = extractor.getKymographSequence( selectedSequence, length, xSpline, ySpline );

						kymograph.setName( roi.getName() + "_kymograph" );
						Icy.getMainInterface().addSequence( kymograph );
						final KymographExtractionResult result = new KymographExtractionResult();
						result.roi = roi;
						result.setKymograph( kymograph );
						result.anterogradeRetrogradeSeparation = false;
						result.sourceSequence = selectedSequence;
						result.samplingPositions = extractor.samplingPositions;
						Icy.getMainInterface().getSwimmingPool().add( new SwimmingObject( result ) );
					}
				}
			}
		}
	}

	// @Override
	// public void refreshPath(
	// PathEvent event,
	// InteractiveMultipleDjikstraTracing source,
	// double[][] path) {
	// if (event == PathEvent.FINAL_PATH)
	// {
	// // convert to a ROI in the original sequence
	// ArrayList<double[][]> paths = source.getOptimalPathCopy();
	// ROI roi = Util.convertPathToROI(selectedSequence, paths);
	// int cntROI = 1;
	// boolean notFound = false;
	// String name = "Path_" + cntROI;
	// while(!notFound)
	// {
	// notFound = true;
	// name = "Path_" + cntROI;
	// for (ROI r:selectedSequence.getROIs())
	// {
	// if (r.getName().equals(name))
	// notFound = false;
	// }
	// cntROI ++;
	// }
	// roi.setName(name);
	// selectedSequence.addROI(roi);
	// }
	// }

	class ControlPointPainter extends Overlay
	{
		ArrayList< double[] > points;

		public ControlPointPainter( final ArrayList< double[] > points )
		{
			super( "Control point painter" );
			this.points = points;
		}

		@Override
		public void paint( final Graphics2D g, final Sequence sequence, final IcyCanvas canvas )
		{
			if ( points != null )
			{
				for ( final double[] p : points )
				{
					g.setColor( Color.red );
					g.setStroke( new BasicStroke( 1f ) );
					g.draw( new Line2D.Double( p[ 0 ] - 2f + 0.5, p[ 1 ] + 0.5, p[ 0 ] + 2f + 0.5, p[ 1 ] + 0.5 ) );
					g.draw( new Line2D.Double( p[ 0 ] + 0.5, p[ 1 ] - 2f + 0.5, p[ 0 ] + 0.5, p[ 1 ] + 2f + 0.5 ) );
				}
			}
		}
	}
}
