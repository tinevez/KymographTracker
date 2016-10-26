package plugins.tinevez.pathtracing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.component.sequence.SequenceChooser;
import icy.gui.frame.TitledFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.painter.Painter;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;

public class PathTracing extends PluginActionable
{
	private TitledFrame mainFrame;

	private SequenceChooser sequenceChooser;

	private IntensityGraphPanel intensityGraphPanel;

	private TracingOptionPanel tracingOptPanel;

	int minLineSize = 100;

	double defaultAlpha = 0.0001;

	@Override
	public void run()
	{
		final JPanel mainPanel = new JPanel();
		mainFrame = GuiUtil.generateTitleFrame( "Path tracing", mainPanel, new Dimension( 300, 70 ), true, true, true, true );
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.PAGE_AXIS ) );

		final JPanel sequencePanel = new JPanel();
		sequencePanel.setLayout( new BoxLayout( sequencePanel, BoxLayout.LINE_AXIS ) );
		// sequencePanel.add(new JLabel("Processed sequence"));
		sequenceChooser = new SequenceChooser();
		sequencePanel.add( sequenceChooser );
		sequencePanel.setBorder( new TitledBorder( "Sequence to process" ) );
		sequenceChooser.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				sequenceChanged( sequenceChooser.getSelectedSequence() );
			}
		} );
		mainPanel.add( sequencePanel );

		tracingOptPanel = new TracingOptionPanel();
		mainPanel.add( tracingOptPanel );

		intensityGraphPanel = new IntensityGraphPanel();
		intensityGraphPanel.drawFinalPath = true;
		intensityGraphPanel.drawTemporaryPath = true;
		mainPanel.add( intensityGraphPanel );

		mainFrame.pack();
		mainFrame.addToMainDesktopPane();
		mainFrame.center();
		mainFrame.setVisible( true );
		mainFrame.requestFocus();
	}

	public void sequenceChanged( final Sequence sequence )
	{
		if ( sequence != null )
		{
			InteractiveDjikstraTracing tracer = null;
			for ( final Painter p : sequence.getPainters() )
			{
				if ( p instanceof InteractiveDjikstraTracing )
				{
					tracer = ( InteractiveDjikstraTracing ) p;
					intensityGraphPanel.listenToTracer( tracer );
					break;
				}
			}
			if ( tracer == null )
			{
				try
				{
					tracer = new InteractiveDjikstraTracing( sequence, tracingOptPanel.getAlpha() );
					sequence.addPainter( tracer );
					intensityGraphPanel.listenToTracer( tracer );
				}
				catch ( final Exception e )
				{
					new AnnounceFrame( "The initialiation of the tracer has failed. Please refer to the log" );
					e.printStackTrace();
				}
			}
		}
	}

	public void changeTracersAlpha( final double newAlpha )
	{
		for ( final Sequence seq : getSequences() )
		{
			for ( final Painter p : seq.getPainters() )
			{
				if ( p instanceof InteractiveDjikstraTracing )
				{
					seq.removePainter( p );
					final InteractiveDjikstraTracing tracer = new InteractiveDjikstraTracing( seq, newAlpha );
					seq.addPainter( tracer );
					if ( intensityGraphPanel.tracer == p )
					{
						intensityGraphPanel.listenToTracer( tracer );
						tracer.enable();
					}
					else
						tracer.disable();
					break;
				}
			}
		}
	}

	class IntensityGraphPanel extends JPanel implements PathListener
	{
		/**
		 * panel displaying the intensity along a traced path auto-refreshing
		 */
		private static final long serialVersionUID = 1421822401789996232L;

		XYDataset datest = null;

		ChartPanel chartPanel = null;

		InteractiveDjikstraTracing tracer = null;

		JCheckBox drawTemporaryPathBox;

		JCheckBox disableDrawingBox;

		public boolean drawTemporaryPath = true;

		public boolean drawFinalPath = true;

		JLabel lengthLabel = new JLabel();

		JLabel meanIntensityLabel = new JLabel();

		JLabel varianceIntensityLabel = new JLabel();

		public IntensityGraphPanel()
		{
			super();
			resetChart();
			this.setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
			this.add( chartPanel );

			final JPanel infoPanel = new JPanel( new FlowLayout( FlowLayout.LEADING ) );
			infoPanel.add( lengthLabel );
			infoPanel.add( meanIntensityLabel );
			infoPanel.add( varianceIntensityLabel );
			this.add( infoPanel );

			final JPanel boxPanel = new JPanel( new FlowLayout( FlowLayout.LEADING ) );
			disableDrawingBox = new JCheckBox( "Disable intensity drawing" );
			disableDrawingBox.setSelected( false );
			disableDrawingBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					drawTemporaryPath = ( !disableDrawingBox.isSelected() ) && drawTemporaryPathBox.isSelected();
					drawFinalPath = ( !disableDrawingBox.isSelected() );
					refreshPath( tracer );
				}
			} );
			drawTemporaryPathBox = new JCheckBox( "Draw temprory path inensity" );
			drawTemporaryPathBox.setSelected( true );
			drawTemporaryPathBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					drawTemporaryPath = ( !disableDrawingBox.isSelected() ) && drawTemporaryPathBox.isSelected();
					refreshPath( tracer );
				}
			} );
			boxPanel.add( disableDrawingBox );
			boxPanel.add( drawTemporaryPathBox );
			this.add( boxPanel );
		}

		private void displayIntensityPath( final double[][] path )
		{
			if ( tracer != null )
			{
				final XYSeries series = new XYSeries( "Path" );
				Number v;
				double sum = 0;
				double sum2 = 0;
				lengthLabel.setText( "Length = " + path.length );
				for ( int i = 0; i < path.length; i++ )
				{
					v = tracer.getIntensityAt( path[ i ][ 0 ], path[ i ][ 1 ] );
					series.add( i, v );
					sum += v.doubleValue();
					sum2 += ( v.doubleValue() * v.doubleValue() );
				}
				if ( path.length > 0 )
				{
					meanIntensityLabel.setText( "Mean intensity = " + ( sum / path.length ) );
					varianceIntensityLabel.setText( "Intensity variance = " + ( sum2 / path.length - Math.pow( sum / path.length, 2 ) ) );
				}
				for ( int i = series.getItemCount() + 1; i < minLineSize; i++ )
				{
					series.add( i, 0 );
				}
				final XYSeriesCollection seriescollection = new XYSeriesCollection();
				seriescollection.addSeries( series );
				final JFreeChart jfreechart = ChartFactory.createXYLineChart( "Intensity along the path", "Position on the path",
						"Intensity value", seriescollection, PlotOrientation.VERTICAL, false, true, false );
				if ( chartPanel == null )
					chartPanel = new ChartPanel( jfreechart );
				else
					chartPanel.setChart( jfreechart );
			}
			else
				resetChart();

		}

		public void listenToTracer( final InteractiveDjikstraTracing tracer )
		{
			if ( this.tracer != null )
			{
				this.tracer.removePathLister( this );
				this.tracer.disable();
			}
			this.tracer = tracer;
			if ( tracer != null )
			{
				tracer.addPathLister( this );
				tracer.enable();
				refreshPath( tracer );
			}
		}

		public void resetChart()
		{
			datest = null;
			final XYSeries series = new XYSeries( "Path" );
			final XYSeriesCollection seriescollection = new XYSeriesCollection();
			for ( int i = 1; i < minLineSize; i++ )
			{
				series.add( i, 0 );
			}
			seriescollection.addSeries( series );
			final JFreeChart jfreechart = ChartFactory.createXYLineChart( "Intensity along the path", "Position on the path",
					"Intensity value", seriescollection, PlotOrientation.VERTICAL, false, true, false );
			if ( chartPanel == null )
				chartPanel = new ChartPanel( jfreechart );
			else
				chartPanel.setChart( jfreechart );
			lengthLabel.setText( "" );
			meanIntensityLabel.setText( "" );
			varianceIntensityLabel.setText( "" );
		}

		@Override
		public void refreshPath( final PathEvent event, final Object source, final double[][] path )
		{
			if ( event == PathEvent.TEMPORARY_PATH && drawTemporaryPath )
			{
				displayIntensityPath( path );
			}
			else if ( event == PathEvent.FINAL_PATH && drawFinalPath )
			{
				displayIntensityPath( path );
			}
			else if ( event == PathEvent.RESET_PATH )
			{
				resetChart();
			}
		}

		private void refreshPath( final InteractiveDjikstraTracing tracer )
		{
			if ( tracer != null )
			{
				final double[][] path = tracer.getOptimalPathCopy();
				if ( path != null )
				{
					displayIntensityPath( path );
				}
				else
					resetChart();
			}
			else
				resetChart();
		}
	}

	public class TracingOptionPanel extends JPanel implements CaretListener, ActionListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1942232576879610509L;

		JTextField alphaTextField;

		JLabel errorLabel;

		double alpha;

		JButton applyButton;

		public TracingOptionPanel()
		{
			super();
			this.setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			this.add( new JLabel( "Trade-off between pixel intensity and path length criteria" ) );

			alphaTextField = new JTextField( Double.toString( defaultAlpha ) );
			this.add( alphaTextField );
			alphaTextField.addCaretListener( this );

			errorLabel = new JLabel( "" );
			this.add( errorLabel );

			applyButton = new JButton( "apply" );
			applyButton.addActionListener( this );
			this.add( applyButton );
		}

		public double getAlpha()
		{
			double threshold;
			try
			{
				threshold = Double.parseDouble( alphaTextField.getText() );
				if ( errorLabel.getText().length() > 0 )
				{
					errorLabel.setText( "" );
				}
			}
			catch ( final NumberFormatException nfe )
			{
				threshold = defaultAlpha;
				errorLabel.setText( "value not recognized" );
			}
			if ( threshold < 0 )
			{
				errorLabel.setText( "value should be positive" );
				threshold = defaultAlpha;
			}
			return threshold;
		}

		@Override
		public void caretUpdate( final CaretEvent e )
		{
			getAlpha();
		}

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			changeTracersAlpha( getAlpha() );
		}
	}
}
