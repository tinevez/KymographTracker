package plugins.tinevez.kymographtracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;

import icy.main.Icy;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPoolEvent;
import icy.swimmingPool.SwimmingPoolListener;
import plugins.fab.trackmanager.TrackGroup;
import plugins.fab.trackmanager.TrackManager;
import plugins.fab.trackmanager.TrackSegment;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.spot.Detection;
import plugins.tinevez.pathtracing.InteractiveMultipleDjikstraTracingESC;
import plugins.tinevez.pathtracing.InteractiveMultipleTracing;
import plugins.tinevez.pathtracing.PathEvent;
import plugins.tinevez.pathtracing.PathListener;

class KymographTrackerPanel extends ActionPanel implements SwimmingPoolListener, PathListener
{
	private static final long serialVersionUID = -3195348117784660674L;

	ArrayList< KymographExtractionResult > results = new ArrayList< KymographExtractionResult >();

	Sequence selectedSequence = null;

	JTable table;

	JTable tableTracks;

	KymographResultsTableModel kymographTableModel;

	KymographTracksTableModel tracksTableModel;

	JTextArea titleLabel;

	String titleMessage = "Kymographs in the SwimmingPool for the selected sequence:";

	String titleEmptyMessage = "No Kymograph in the SwimmingPool for the selected sequence. Use the methods in the extraction tab first.";

	JButton removeButton;

	JButton buildTracksButton;

	JButton convertToTracksButton;

	JButton showButton;

	double alpha = 0.01;

	public KymographTrackerPanel()
	{
		description = "Kymograph analysis and tracking";
		node = new DefaultMutableTreeNode( description );

		this.setBorder( new TitledBorder( description ) );
		this.setLayout( new BorderLayout() );
		final JTabbedPane tabbedPane = new JTabbedPane();
		this.setLayout( new BorderLayout() );
		this.add( tabbedPane, BorderLayout.CENTER );

		titleLabel = new JTextArea( titleMessage );
		// titleLabel.setEditable(false);
		// titleLabel.setLineWrap(true);
		// titleLabel.setWrapStyleWord(true);
		// this.add(titleLabel, BorderLayout.NORTH);

		final JPanel extractionPanel = new JPanel( new BorderLayout() );
		tabbedPane.add( extractionPanel, "Track creation" );
		final JPanel explorationPanel = new JPanel( new BorderLayout() );
		tabbedPane.add( explorationPanel, "Track visualization" );

		kymographTableModel = new KymographResultsTableModel();
		table = new JTable( kymographTableModel );
		for ( int i = 0; i < table.getColumnCount(); i++ )
		{
			final TableColumn col = table.getColumnModel().getColumn( i );
			col.setPreferredWidth( 100 );
		}
		table.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		final JPanel tmpPanel = new JPanel();
		tmpPanel.setLayout( new BorderLayout() );
		tmpPanel.add( table.getTableHeader(), BorderLayout.PAGE_START );
		tmpPanel.add( table, BorderLayout.CENTER );
		final JScrollPane scrollPane = new JScrollPane( tmpPanel );
		scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		extractionPanel.add( scrollPane, BorderLayout.CENTER );

		final JPanel southPanel1 = new JPanel( new GridBagLayout() );
		final GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.0;
		c.insets = new Insets( 2, 2, 2, 2 );

		final JEditorPane extractionDescriptionPane = new JEditorPane();
		extractionDescriptionPane.setContentType( "text/html" );
		extractionDescriptionPane.setEditable( false );
		extractionDescriptionPane.setText( "Create tracks in the kymographs selected above using ROIs by either directly <strong>creating ROIs</strong> or <strong>tracing paths</strong> in kymograph images, and then <strong>converting ROIs to tracks</strong>.</ul>" );
		extractionDescriptionPane.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
		extractionDescriptionPane.setEditable( false );
		southPanel1.add( extractionDescriptionPane, c );
		c.gridy++;

		buildTracksButton = new JButton( "Trace tracks as paths" );
		southPanel1.add( buildTracksButton, c );
		buildTracksButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				initTracers();
			}
		} );
		c.gridy++;

		convertToTracksButton = new JButton( "Convert ROIs to tracks" );
		southPanel1.add( convertToTracksButton, c );
		convertToTracksButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				convertROIsToTracks();
				tabbedPane.setSelectedComponent( explorationPanel );
			}
		} );
		c.gridy++;

		showButton = new JButton( "Show kymographs" );
		southPanel1.add( showButton, c );
		showButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				showKymographs();
			}
		} );
		c.gridy++;

		removeButton = new JButton( "Delete kymograph" );
		southPanel1.add( removeButton, c );
		removeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				deleteKymographs();
			}
		} );
		c.gridy++;
		extractionPanel.add( southPanel1, BorderLayout.SOUTH );

		// track exploration
		tracksTableModel = new KymographTracksTableModel();
		tableTracks = new JTable( tracksTableModel );
		for ( int i = 0; i < tableTracks.getColumnCount(); i++ )
		{
			final TableColumn col = tableTracks.getColumnModel().getColumn( i );
			col.setPreferredWidth( 50 );
		}
		tableTracks.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

		// JPanel centerPanel2 = new JPanel(new BorderLayout());
		explorationPanel.add( new JScrollPane( tableTracks ), BorderLayout.CENTER );

		final JPanel southPanel2 = new JPanel( new GridBagLayout() );
		final GridBagConstraints c2 = new GridBagConstraints();
		c2.gridheight = 1;
		c2.gridwidth = 1;
		c2.gridx = 0;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		c2.weighty = 0.0;
		c2.insets = new Insets( 2, 2, 2, 2 );

		final JButton deleteTracksButton = new JButton( "Delete tracks" );
		deleteTracksButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				deleteSelectedTracks();
			}
		} );
		southPanel2.add( deleteTracksButton, c2 );
		c2.gridy++;
		final JButton show2DTracksButton = new JButton( "Show 2D tracks" );
		show2DTracksButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				show2DTracks();
			}
		} );
		southPanel2.add( show2DTracksButton, c2 );
		c2.gridy++;
		explorationPanel.add( southPanel2, BorderLayout.SOUTH );

		Icy.getMainInterface().getSwimmingPool().addListener( this );
		refreshResults();
	}

	public void enableGUI( final boolean enable )
	{
//		table.setEnabled(enable);
//		titleLabel.setEnabled(enable);
//		removeButton.setEnabled(enable);
//		buildTracksButton.setEnabled(enable);
//		convertToTracksButton.setEnabled(enable);
	}

	void showKymographs()
	{
		final int numSelectedRows = table.getSelectedRowCount();
		if ( numSelectedRows < 1 )
		{
			JOptionPane.showMessageDialog( this,
					"Please select some kymographs to process in the results table.",
					"Warning",
					JOptionPane.WARNING_MESSAGE );
			return;
		}
		final int[] selectedRows = table.getSelectedRows();
		final ArrayList< KymographExtractionResult > toShow = new ArrayList< KymographExtractionResult >();
		for ( final int i : selectedRows )
			toShow.add( kymographTableModel.getResult( i ) );
		for ( final KymographExtractionResult kymo : toShow )
		{
			if ( !Icy.getMainInterface().getSequences().contains( kymo.getKymograph() ) )
				Icy.getMainInterface().addSequence( kymo.getKymograph() );
			if ( kymo.getAnterogradeKymograph() != null )
			{
				if ( !Icy.getMainInterface().getSequences().contains( kymo.getAnterogradeKymograph() ) )
					Icy.getMainInterface().addSequence( kymo.getAnterogradeKymograph() );
			}

			if ( kymo.getRetrogradeKymograph() != null )
			{
				if ( !Icy.getMainInterface().getSequences().contains( kymo.getRetrogradeKymograph() ) )
					Icy.getMainInterface().addSequence( kymo.getRetrogradeKymograph() );
			}
		}
	}

	void initTracers()
	{
		final int numSelectedRows = table.getSelectedRowCount();
		if ( numSelectedRows < 1 )
		{
			JOptionPane.showMessageDialog( this,
					"Please select some kymographs to process in the results table.",
					"Warning",
					JOptionPane.WARNING_MESSAGE );
			return;
		}
		final int[] selectedRows = table.getSelectedRows();
		final ArrayList< KymographExtractionResult > toConvert = new ArrayList< KymographExtractionResult >();
		for ( final int i : selectedRows )
			toConvert.add( kymographTableModel.getResult( i ) );
		for ( final KymographExtractionResult kymo : toConvert )
		{
			if ( !Icy.getMainInterface().getSequences().contains( kymo.getKymograph() ) )
				Icy.getMainInterface().addSequence( kymo.getKymograph() );
			final InteractiveMultipleDjikstraTracingESC tracer = new InteractiveMultipleDjikstraTracingESC( kymo.getKymograph(), alpha, true );
			kymo.getKymograph().addOverlay( tracer );

			if ( kymo.getAnterogradeKymograph() != null )
			{
				if ( !Icy.getMainInterface().getSequences().contains( kymo.getAnterogradeKymograph() ) )
					Icy.getMainInterface().addSequence( kymo.getAnterogradeKymograph() );
				final InteractiveMultipleDjikstraTracingESC anteroTracer = new InteractiveMultipleDjikstraTracingESC( kymo.getAnterogradeKymograph(), alpha, true );
				kymo.getAnterogradeKymograph().addOverlay( anteroTracer );
				anteroTracer.addPathLister( this );
			}

			if ( kymo.getRetrogradeKymograph() != null )
			{
				if ( !Icy.getMainInterface().getSequences().contains( kymo.getRetrogradeKymograph() ) )
					Icy.getMainInterface().addSequence( kymo.getRetrogradeKymograph() );
				final InteractiveMultipleDjikstraTracingESC retroTracer = new InteractiveMultipleDjikstraTracingESC( kymo.getRetrogradeKymograph(), alpha, true );
				kymo.getRetrogradeKymograph().addOverlay( retroTracer );
				retroTracer.addPathLister( this );
			}
			tracer.addPathLister( this );
		}
	}

	void convertROIsToTracks()
	{
		final int numSelectedRows = table.getSelectedRowCount();
		if ( numSelectedRows < 1 )
		{
			JOptionPane.showMessageDialog( this,
					"Please select some kymographs to process in the results table.",
					"Warning",
					JOptionPane.WARNING_MESSAGE );
			return;
		}
		final int[] selectedRows = table.getSelectedRows();
		final ArrayList< KymographExtractionResult > toConvert = new ArrayList< KymographExtractionResult >();
		for ( final int i : selectedRows )
			toConvert.add( kymographTableModel.getResult( i ) );
		for ( final KymographExtractionResult kymo : toConvert )
		{
			if ( kymo.getKymograph() != null )
			{
				kymo.trackingResults = new KymographTrackingResults();
				if ( kymo.trackingResults.tracks1D == null )
					kymo.trackingResults.tracks1D = new ArrayList< ArrayList< double[] > >();
				if ( kymo.trackingResults.tracks2D == null )
				{
					kymo.trackingResults.tracks2D = new TrackGroup( kymo.sourceSequence );
					kymo.trackingResults.tracks2D.setDescription( kymo.roi.getName() + "_tracks" );
					Icy.getMainInterface().getSwimmingPool().add( new SwimmingObject( kymo.trackingResults.tracks2D ) );
				}
				// resample positions from contained ROIs
				for ( final ROI2D roi : kymo.getKymograph().getROI2Ds() )
				{
					if ( roi instanceof ROI2DShape )
					{
						// resample roi positions with unit steps
						final ArrayList< double[] > positions = getUnitStepPositions( ( ROI2DShape ) roi );
						// compute 2D+T track segments
						kymo.trackingResults.tracks1D.add( positions );
						final TrackSegment track = computeTrackFromKymograph( kymo, positions );
						kymo.trackingResults.tracks2D.addTrackSegment( track );
					}
				}
			}
			if ( kymo.getAnterogradeKymograph() != null )
			{
				kymo.anterogradeTrackingResults = new KymographTrackingResults();
				if ( kymo.anterogradeTrackingResults.tracks1D == null )
					kymo.anterogradeTrackingResults.tracks1D = new ArrayList< ArrayList< double[] > >();
				if ( kymo.anterogradeTrackingResults.tracks2D == null )
				{
					kymo.anterogradeTrackingResults.tracks2D = new TrackGroup( kymo.sourceSequence );
					kymo.anterogradeTrackingResults.tracks2D.setDescription( kymo.roi.getName() + "_anteroTracks" );
					Icy.getMainInterface().getSwimmingPool().add( new SwimmingObject( kymo.anterogradeTrackingResults.tracks2D ) );
				}
				// resample positions from contained ROIs
				for ( final ROI2D roi : kymo.getAnterogradeKymograph().getROI2Ds() )
				{
					if ( roi instanceof ROI2DShape )
					{
						// resample roi positions with unit steps
						final ArrayList< double[] > positions = getUnitStepPositions( ( ROI2DShape ) roi );
						// compute 2D+T track segments
						kymo.anterogradeTrackingResults.tracks1D.add( positions );
						final TrackSegment track = computeTrackFromKymograph( kymo, positions );
						kymo.anterogradeTrackingResults.tracks2D.addTrackSegment( track );
					}
				}
			}
			if ( kymo.getRetrogradeKymograph() != null )
			{

				kymo.retrogradeTrackingResults = new KymographTrackingResults();
				if ( kymo.retrogradeTrackingResults.tracks1D == null )
					kymo.retrogradeTrackingResults.tracks1D = new ArrayList< ArrayList< double[] > >();
				if ( kymo.retrogradeTrackingResults.tracks2D == null )
				{
					kymo.retrogradeTrackingResults.tracks2D = new TrackGroup( kymo.sourceSequence );
					kymo.retrogradeTrackingResults.tracks2D.setDescription( kymo.roi.getName() + "_retroTracks" );
					Icy.getMainInterface().getSwimmingPool().add( new SwimmingObject( kymo.retrogradeTrackingResults.tracks2D ) );
				}
				// resample positions from contained ROIs
				for ( final ROI2D roi : kymo.getRetrogradeKymograph().getROI2Ds() )
				{
					if ( roi instanceof ROI2DShape )
					{
						// resample roi positions with unit steps
						final ArrayList< double[] > positions = getUnitStepPositions( ( ROI2DShape ) roi );
						// compute 2D+T track segments
						kymo.retrogradeTrackingResults.tracks1D.add( positions );
						final TrackSegment track = computeTrackFromKymograph( kymo, positions );
						kymo.retrogradeTrackingResults.tracks2D.addTrackSegment( track );
					}
				}

			}
			tracksTableModel.refreshTable();
		}
	}

	private TrackSegment computeTrackFromKymograph( final KymographExtractionResult kymo, final ArrayList< double[] > positions )
	{
		final TrackSegment ts = new TrackSegment();
		final ArrayList< double[] > samplingPositions = kymo.samplingPositions;
		// resample 1D+T positions in the 2D+T space
		for ( final double p[] : positions )
		{
			final double c = p[ 1 ]; // position on the path
			// linear interpolation between sampling positions in 2D
			final int c0 = ( int ) Math.floor( c );
			final int c1 = ( int ) Math.ceil( c );
			if ( c1 < samplingPositions.size() && c0 < samplingPositions.size() )
			{
				if ( c0 == c1 )
				{
					final double x = samplingPositions.get( c0 )[ 0 ];
					final double y = samplingPositions.get( c0 )[ 1 ];
					ts.addDetection( new Detection( x, y, 0, ( int ) Math.round( p[ 0 ] ) ) );
				}
				else
				{
					final double x0 = samplingPositions.get( c0 )[ 0 ];
					final double y0 = samplingPositions.get( c0 )[ 1 ];
					final double x1 = samplingPositions.get( c1 )[ 0 ];
					final double y1 = samplingPositions.get( c1 )[ 1 ];
					final double x = x0 + ( c - c0 ) * ( x1 - x0 );
					final double y = y0 + ( c - c0 ) * ( y1 - y0 );
					ts.addDetection( new Detection( x, y, 0, ( int ) Math.round( p[ 0 ] ) ) );
				}
			}
		}
		return ts;
	}

	public ArrayList< double[] > getUnitStepPositions( final ROI2DShape roi )
	{
		final PathIterator pathIterator = roi.getPathIterator( null );
		final double[] coords = new double[ 6 ];
		ArrayList< double[] > positions = new ArrayList< double[] >();
		while ( !pathIterator.isDone() )
		{
			final int segType = pathIterator.currentSegment( coords );
			switch ( segType )
			{
			case PathIterator.SEG_CLOSE:
				break;
			case PathIterator.SEG_CUBICTO:
				break;
			case PathIterator.SEG_LINETO:
				positions.add( new double[] { coords[ 0 ], coords[ 1 ] } );
				break;
			case PathIterator.SEG_MOVETO:
				positions.add( new double[] { coords[ 0 ], coords[ 1 ] } );
				break;
			case PathIterator.SEG_QUADTO:
				break;
			}
			pathIterator.next();
		}
		// average positions that have the same y (time) axis coordinate
		final ArrayList< double[] > averagedPositions = new ArrayList< double[] >();
		double prevY = -Double.MAX_VALUE;
		double sumX = 0;
		int cntX = 0;
		for ( final double[] p : positions )
		{
			if ( p[ 1 ] == prevY )
			{
				sumX += p[ 0 ];
				cntX++;
			}
			else
			{
				// add the previous averaged position
				if ( cntX > 0 )
					averagedPositions.add( new double[] { sumX / cntX, prevY } );
				prevY = p[ 1 ];
				sumX = p[ 0 ];
				cntX = 1;
			}
		}
		if ( cntX > 0 )
			averagedPositions.add( new double[] { sumX / cntX, prevY } );

		// need to resample positions with unit step
		positions = resamplePositions( averagedPositions );
		return positions;
	}

	public ArrayList< double[] > resamplePositions( final ArrayList< double[] > positions )
	{
		// resample using linear interpolation
		// TODO: use more refined interpolators

		ArrayList< double[] > resampledPositions = new ArrayList< double[] >();
		for ( final double[] p : positions )
			resampledPositions.add( new double[] { p[ 0 ], Math.round( p[ 1 ] ) } );

		// then fill missing positions and remove duplicate ones

		final ArrayList< double[] > resampledPositions2 = new ArrayList< double[] >();
		if ( !positions.isEmpty() )
		{
			// check the extremities to find the first position
			if ( resampledPositions.get( 0 )[ 1 ] > resampledPositions.get( positions.size() - 1 )[ 1 ] )
			{
				// revert order of positions
				final ArrayList< double[] > tmp = new ArrayList< double[] >();
				for ( int i = 0; i < resampledPositions.size(); i++ )
					tmp.add( resampledPositions.get( resampledPositions.size() - i - 1 ) );
				resampledPositions = tmp;
			}

			int t = ( int ) resampledPositions.get( 0 )[ 1 ];
			double prevPos = resampledPositions.get( 0 )[ 0 ];
			resampledPositions2.add( new double[] { t, prevPos } );
			int numSameT = 1;
			for ( int i = 1; i < resampledPositions.size(); i++ )
			{
				if ( resampledPositions.get( i )[ 1 ] == t + 1 )
				{
					resampledPositions2.add( new double[] { resampledPositions.get( i )[ 1 ], resampledPositions.get( i )[ 0 ] } );
					t = t + 1;
					prevPos = resampledPositions.get( i )[ 0 ];
					numSameT = 1;
				}
				else
				{
					if ( resampledPositions.get( i )[ 1 ] > t + 1 )
					{
						// linear interpolation between missing t
						final double nextPos = resampledPositions.get( i )[ 0 ];
						final double nextT = resampledPositions.get( i )[ 1 ];
						for ( int t2 = t + 1; t2 < nextT; t2++ )
						{
							resampledPositions2.add( new double[] { t2, prevPos + ( nextPos - prevPos ) * ( t2 - t ) / ( nextT - t ) } );
						}
						resampledPositions2.add( new double[] { ( int ) nextT, nextPos } );
						t = ( int ) nextT;
						prevPos = nextPos;
						numSameT = 1;
					}
					else if ( resampledPositions.get( i )[ 1 ] == t )
					{
						numSameT++;
						final double[] prevPosition = resampledPositions2.get( resampledPositions2.size() - 1 );
						prevPosition[ 1 ] = ( prevPosition[ 1 ] * ( numSameT - 1 ) + resampledPositions.get( i )[ 0 ] ) / numSameT;
						prevPos = prevPosition[ 1 ];
					}
				}
			}
		}
		return resampledPositions2;
	}

	void deleteKymographs()
	{
		final int numSelectedRows = table.getSelectedRowCount();
		if ( numSelectedRows < 1 )
		{
			JOptionPane.showMessageDialog( this,
					"Please select some kymographs to process in the results table.",
					"Warning",
					JOptionPane.WARNING_MESSAGE );
			return;
		}
		final Object[] options = { "Delete selected kymographs", "Cancel" };
		final int n = JOptionPane.showOptionDialog( this,
				"This will permanently delete the selected kymograph.\n Do you really want to proceed?",
				"Kymograph deletion",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[ 1 ] );
		if ( n == 1 )
			return;
		final int[] selectedRows = table.getSelectedRows();
		final ArrayList< KymographExtractionResult > toRemove = new ArrayList< KymographExtractionResult >();
		for ( final int i : selectedRows )
			toRemove.add( kymographTableModel.getResult( i ) );
		for ( final KymographExtractionResult r : toRemove )
		{
			final ArrayList< SwimmingObject > swimmingOjects = Icy.getMainInterface().getSwimmingPool().getObjects( KymographExtractionResult.class );
			for ( final SwimmingObject o : swimmingOjects )
			{
				if ( o.getObject() == r )
				{
					Icy.getMainInterface().getSwimmingPool().remove( o );
					break;
				}
			}
		}
	}

	void deleteSelectedTracks()
	{
		final int numSelectedRows = tableTracks.getSelectedRowCount();
		if ( numSelectedRows < 1 )
		{
			JOptionPane.showMessageDialog( this,
					"Please select some tracks in the results table.",
					"Warning",
					JOptionPane.WARNING_MESSAGE );
			return;
		}
		final Object[] options = { "Delete selected tracks", "Cancel" };
		final int n = JOptionPane.showOptionDialog( this,
				"This will permanently delete the selected tracks.\n Do you really want to proceed?",
				"Kymograph deletion",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[ 1 ] );
		if ( n == 1 )
			return;
		final int[] selectedRows = table.getSelectedRows();
		final ArrayList< TrackSegment > toRemove = new ArrayList< TrackSegment >();
		for ( final int i : selectedRows )
			toRemove.add( tracksTableModel.getTrackSegmentAt( i ) );
		for ( final TrackSegment ts : toRemove )
			tracksTableModel.removeTrackSegment( ts );
	}

	void show2DTracks()
	{
		final TrackManager tm = new TrackManager();
		tm.setDisplaySequence( selectedSequence );
	}

	@Override
	protected void changeSelectedSequence( final Sequence sequence )
	{
		this.selectedSequence = sequence;
		refreshResults();
	}

	protected void refreshResults()
	{
		results.clear();
		kymographTableModel.clearTable();
		tracksTableModel.clearTable();
		if ( selectedSequence != null )
		{
			for ( final SwimmingObject obj : Icy.getMainInterface().getSwimmingPool().getObjects( KymographExtractionResult.class ) )
			{
				final KymographExtractionResult result = ( KymographExtractionResult ) obj.getObject();
				if ( result.sourceSequence == selectedSequence )
				{
					results.add( result );
					kymographTableModel.addResult( result );
					tracksTableModel.addResult( result );
				}
			}
		}
		refreshGUI();
	}

	protected void refreshGUI()
	{
		if ( results.isEmpty() )
		{
			titleLabel.setText( titleEmptyMessage );
			enableGUI( false );
		}
		else
		{
			enableGUI( true );
			titleLabel.setText( titleEmptyMessage );
		}
		table.updateUI();
	}

	@Override
	public void swimmingPoolChangeEvent( final SwimmingPoolEvent event )
	{
		// test for new kymograph in the swimming pool
		if ( event.getResult().getObject() instanceof KymographExtractionResult )
		{
			final KymographExtractionResult result = ( KymographExtractionResult ) event.getResult().getObject();
			if ( result.sourceSequence == selectedSequence )
			{
				switch ( event.getType() )
				{
				case ELEMENT_ADDED:
					results.add( result );
					kymographTableModel.addResult( result );
					tracksTableModel.addResult( result );
					break;
				case ELEMENT_REMOVED:
					results.remove( result );
					kymographTableModel.removeResult( result );
					tracksTableModel.removeResult( result );
					break;
				}
				refreshGUI();
			}
		}
	}

	class KymographResultsTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = -1614208982278312037L;

		String[] columnNames = { "ROI name", "Kymograph names", "Anterograde/retrograde splitting", "Path length", "First endpoint", "Last endpoint" };

		ArrayList< String[] > tableData = new ArrayList< String[] >();

		ArrayList< KymographExtractionResult > tableResults = new ArrayList< KymographExtractionResult >();

		protected void addResult( final KymographExtractionResult result )
		{
			tableResults.add( result );
			final String[] line = new String[ getColumnCount() ];
			if ( result.roi == null )
				line[ 0 ] = "null";
			else
				line[ 0 ] = result.roi.getName();
			if ( result.getKymograph() == null )
				line[ 1 ] = "null";
			else
				line[ 1 ] = result.getKymograph().getName();
			if ( result.anterogradeRetrogradeSeparation )
			{
				line[ 1 ] = line[ 1 ].concat( "; " + result.getAnterogradeKymograph().getName() );
				line[ 1 ] = line[ 1 ].concat( "; " + result.getRetrogradeKymograph().getName() );
			}
			line[ 2 ] = String.valueOf( result.anterogradeRetrogradeSeparation );
			line[ 3 ] = String.valueOf( result.getKymograph().getSizeX() );
			if ( result.samplingPositions != null && !result.samplingPositions.isEmpty() )
			{
				line[ 4 ] = "{" + result.samplingPositions.get( 0 )[ 0 ] + ", " + result.samplingPositions.get( 0 )[ 1 ] + "}";
				line[ 5 ] = "{" + result.samplingPositions.get( result.samplingPositions.size() - 1 )[ 0 ] + ", " + result.samplingPositions.get( result.samplingPositions.size() - 1 )[ 1 ] + "}";
			}
			else
			{
				line[ 4 ] = "{,}";
				line[ 5 ] = "{,}";
			}
			tableData.add( line );
			this.fireTableRowsInserted( tableData.size(), tableData.size() );
		}

		public void clearTable()
		{
			tableResults.clear();
			tableData.clear();
		}

		protected void removeResult( final KymographExtractionResult result )
		{
			final int idx = tableResults.indexOf( result );
			if ( idx >= 0 )
			{
				tableResults.remove( idx );
				tableData.remove( idx );
			}
			this.fireTableRowsDeleted( idx, idx );
		}

		protected KymographExtractionResult getResult( final int rowIndex )
		{
			return tableResults.get( rowIndex );
		}

		@Override
		public String getColumnName( final int column )
		{
			return columnNames[ column ];
		};

		@Override
		public boolean isCellEditable( final int arg0, final int arg1 )
		{
			return false;
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public Class< String > getColumnClass( final int c )
		{
			return String.class;
		}

		@Override
		public int getRowCount()
		{
			return tableData.size();
		}

		@Override
		public Object getValueAt( final int arg0, final int arg1 )
		{
			if ( tableData.size() <= arg0 )
				return null;
			return tableData.get( arg0 )[ arg1 ];
		}
	}

	class KymographTracksTableModel extends AbstractTableModel
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -7098962063097100347L;

		String[] columnNames = { "Track ID", "Kymograph name", "Direction", "Mean speed (pixel/frame)" };

		ArrayList< String[] > tableData = new ArrayList< String[] >();

		ArrayList< TrackSegment > trackSegments = new ArrayList< TrackSegment >();

		ArrayList< KymographExtractionResult > tableResults = new ArrayList< KymographExtractionResult >();

		protected void refreshTable()
		{
			clearTable();
			for ( final KymographExtractionResult result : tableResults )
			{
				if ( result.trackingResults != null )
				{
					if ( result.trackingResults.tracks2D != null )
					{
						for ( int i = 0; i < result.trackingResults.tracks2D.getTrackSegmentList().size(); i++ )
						{
							final TrackSegment ts = result.trackingResults.tracks2D.getTrackSegmentList().get( i );
							final String[] line = new String[ getColumnCount() ];
							line[ 0 ] = String.valueOf( ts.getId() );
							line[ 1 ] = result.getKymograph().getName();
							line[ 2 ] = "bidirectional";
							final ArrayList< double[] > positions1D = result.trackingResults.tracks1D.get( i );
							line[ 3 ] = String.valueOf( ( positions1D.get( positions1D.size() - 1 )[ 1 ] - positions1D.get( 0 )[ 1 ] ) / positions1D.size() );
							tableData.add( line );
							trackSegments.add( ts );
						}
					}
				}
				if ( result.anterogradeTrackingResults != null )
				{
					for ( int i = 0; i < result.anterogradeTrackingResults.tracks2D.getTrackSegmentList().size(); i++ )
					{
						final TrackSegment ts = result.anterogradeTrackingResults.tracks2D.getTrackSegmentList().get( i );
						final String[] line = new String[ getColumnCount() ];
						line[ 0 ] = String.valueOf( ts.getId() );
						line[ 1 ] = result.getKymograph().getName();
						line[ 2 ] = "anterograde";
						final ArrayList< double[] > positions1D = result.anterogradeTrackingResults.tracks1D.get( i );
						line[ 3 ] = String.valueOf( ( positions1D.get( positions1D.size() - 1 )[ 1 ] - positions1D.get( 0 )[ 1 ] ) / positions1D.size() );
						tableData.add( line );
						trackSegments.add( ts );
					}
				}
				if ( result.retrogradeTrackingResults != null )
				{
					for ( int i = 0; i < result.retrogradeTrackingResults.tracks2D.getTrackSegmentList().size(); i++ )
					{
						final TrackSegment ts = result.retrogradeTrackingResults.tracks2D.getTrackSegmentList().get( i );
						final String[] line = new String[ getColumnCount() ];
						line[ 0 ] = String.valueOf( ts.getId() );
						line[ 1 ] = result.getKymograph().getName();
						line[ 2 ] = "retrograde";
						final ArrayList< double[] > positions1D = result.retrogradeTrackingResults.tracks1D.get( i );
						line[ 3 ] = String.valueOf( ( positions1D.get( positions1D.size() - 1 )[ 1 ] - positions1D.get( 0 )[ 1 ] ) / positions1D.size() );
						tableData.add( line );
						trackSegments.add( ts );
					}
				}
			}
			this.fireTableDataChanged();
		}

		public void removeResult( final KymographExtractionResult result )
		{
			final int idx = tableResults.indexOf( result );
			if ( idx >= 0 )
			{
				tableResults.remove( idx );
			}
			refreshTable();
		}

		public TrackSegment getTrackSegmentAt( final int i )
		{
			if ( i < trackSegments.size() )
			{
				return trackSegments.get( i );
			}
			else
				throw new IllegalArgumentException( "Index exceeds table size" );
		}

		protected void addResult( final KymographExtractionResult result )
		{
			tableResults.add( result );
			refreshTable();
		}

		public void clearTable()
		{
			tableData.clear();
			trackSegments.clear();
		}

		protected void removeTrackSegment( final TrackSegment ts )
		{
			// int idx = trackSegments.indexOf(ts);
			// if (idx >= 0)
			// {
			// tableResults.remove(idx);
			// }
			for ( final KymographExtractionResult result : tableResults )
			{
				if ( result.trackingResults != null && result.trackingResults.tracks2D != null )
					result.trackingResults.tracks2D.removeTrackSegment( ts );
				if ( result.anterogradeTrackingResults != null && result.anterogradeTrackingResults.tracks2D != null )
					result.anterogradeTrackingResults.tracks2D.removeTrackSegment( ts );
				if ( result.retrogradeTrackingResults != null && result.retrogradeTrackingResults.tracks2D != null )
					result.retrogradeTrackingResults.tracks2D.removeTrackSegment( ts );
			}
			refreshTable();
		}

		@Override
		public String getColumnName( final int column )
		{
			return columnNames[ column ];
		};

		@Override
		public boolean isCellEditable( final int arg0, final int arg1 )
		{
			return false;
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public Class< String > getColumnClass( final int c )
		{
			return String.class;
		}

		@Override
		public int getRowCount()
		{
			return tableData.size();
		}

		@Override
		public Object getValueAt( final int arg0, final int arg1 )
		{
			if ( tableData.size() <= arg0 )
				return null;
			return tableData.get( arg0 )[ arg1 ];
		}
	}

	@Override
	public void refreshPath( final PathEvent event, final Object source, final double[][] path )
	{
		if ( event == PathEvent.FINAL_PATH )
		{
			Sequence sequence = null;
			for ( final Sequence seq : Icy.getMainInterface().getSequences() )
			{
				if ( seq.getOverlays().contains( source ) )
					sequence = seq;
			}
			if ( sequence == null )
				return;
			// convert to a ROI in the original sequence

			final InteractiveMultipleTracing tracer = ( InteractiveMultipleTracing ) source;
			final ArrayList< double[][] > paths = tracer.getOptimalPathCopy();
			final ROI roi = Util.convertPathToROI( sequence, paths );
			int cntROI = 1;
			boolean notFound = false;
			String name = "Path_" + cntROI;
			while ( !notFound )
			{
				notFound = true;
				name = "Track_" + cntROI;
				for ( final ROI r : sequence.getROIs() )
				{
					if ( r.getName().equals( name ) )
						notFound = false;
				}
				cntROI++;
			}
			roi.setName( name );
			sequence.addROI( roi );
		}
	}
}
