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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.gui.main.GlobalSequenceListener;
import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.math.MathUtil;
import icy.math.UnitUtil;
import icy.math.UnitUtil.UnitPrefix;
import icy.painter.Overlay;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginDaemon;
import icy.preferences.XMLPreferences;
import icy.sequence.Sequence;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;
import plugins.adufour.vars.gui.model.IntegerRangeModel;
import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarColor;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarInteger;
import plugins.kernel.canvas.VtkCanvas;
import plugins.tprovoost.scale.ScaleBarLocation;
import plugins.tprovoost.scale.ScaleBarOverlay;

/**
 * Copied and adapted from the plugins.tprovoost.Scale class built by authors
 * Thomas Provoost, Stephane Dallongeville, Alexandre Dufour
 */
public class KymographScaleBar extends Plugin implements PluginDaemon, GlobalSequenceListener
{

	private static XMLPreferences preferences = null;

	public static class KymographScaleBarOverlay extends Overlay
	{
		private static final double[] scaleRoundedFactors = {
				1.0D,
				2.0D,
				3.0D,
				4.0D,
				5.0D,
				6.0D,
				7.0D,
				8.0D,
				9.0D,
				10.0D,
				20.0D,
				30.0D,
				40.0D,
				50.0D,
				60.0D,
				70.0D,
				80.0D,
				90.0D,
				100.0D,
				200.0D,
				300.0D,
				400.0D,
				500.0D,
				600.0D,
				700.0D,
				800.0D,
				900.0D };

		/**
		 * Scale bar color (default: white)
		 */
		private final VarColor color = new VarColor( "Color", new Color( preferences.getInt( "color", new Color( 255, 255, 255, 255 ).getRGB() ) ) )
		{
			@Override
			public void setValue( final Color newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putInt( "color", newValue.getRGB() );

				painterChanged();
			}
		};

		/**
		 * Scale bar location (default: bottom left)
		 */
		private final VarEnum< ScaleBarLocation > location = new VarEnum< ScaleBarLocation >( "Location",
				ScaleBarLocation.valueOf( preferences.get( "location", ScaleBarLocation.IMAGE_BOTTOM_LEFT.name() ) ) )
		{
			@Override
			public void setValue( final ScaleBarLocation newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.put( "location", newValue.name() );

				painterChanged();
			}
		};

		/**
		 * Displays the text over the scale bar
		 */
		private final VarBoolean showText = new VarBoolean( "Display size", preferences.getBoolean( "showText", true ) )
		{
			@Override
			public void setValue( final Boolean newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putBoolean( "showText", newValue );

				painterChanged();
			}
		};

		/**
		 * Spatial scale bar size (default: 10 units)
		 */
		private final VarInteger spaceSize = new VarInteger( "Spatial size", preferences.getInt( "size", 10 ) )
		{
			@Override
			public void setValue( final Integer newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putInt( "size", newValue );

				painterChanged();
			}
		};

		/**
		 * Time scale bar size (default: 10 units)
		 */
		private final VarInteger timeSize = new VarInteger( "Time size", preferences.getInt( "timeSize", 10 ) )
		{
			@Override
			public void setValue( final Integer newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putInt( "timeSize", newValue );

				painterChanged();
			}
		};

		/**
		 * Scale bar size (default: 10 units)
		 */
		private final VarInteger thickness = new VarInteger( "Thickness", preferences.getInt( "thickness", 5 ) )
		{
			@Override
			public void setValue( final Integer newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putInt( "thickness", newValue );

				painterChanged();
			}
		};

		/**
		 * Spatial scale bar unit (default: microns)
		 */
		private final VarEnum< UnitPrefix > spaceUnit = new VarEnum< UnitUtil.UnitPrefix >( "Unit", UnitPrefix.valueOf( preferences.get( "unit", UnitPrefix.MICRO.name() ) ) )
		{
			@Override
			public void setValue( final UnitPrefix newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.put( "unit", newValue.name() );

				painterChanged();
			}
		};

		/**
		 * Spatial scale bar unit (default: seconds)
		 */
		private final VarEnum< UnitPrefix > timeUnit = new VarEnum< UnitUtil.UnitPrefix >( "Time unit", UnitPrefix.valueOf( preferences.get( "timeUnit", UnitPrefix.NONE.name() ) ) )
		{
			@Override
			public void setValue( final UnitPrefix newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.put( "unit", newValue.name() );

				painterChanged();
			}
		};

		/**
		 * Auto-adjust the size and unit of the scale bar
		 */
		private final VarBoolean autoSize = new VarBoolean( "Auto-adjust size", preferences.getBoolean( "autoSize", false ) )
		{
			@Override
			public void setValue( final Boolean newValue )
			{
				if ( getValue().equals( newValue ) )
					return;

				super.setValue( newValue );

				preferences.putBoolean( "autoSize", newValue );

				painterChanged();

			}
		};

		private JPanel optionPanel;

		private final Path2D.Double line = new Path2D.Double();

		public KymographScaleBarOverlay()
		{
			super( "Kymograph scale-bar", OverlayPriority.TOPMOST );

			final Integer currentSpaceSize = spaceSize.getValue();
			spaceSize.setDefaultEditorModel( new IntegerRangeModel( 10, 1, 999, 1 ) );
			spaceSize.setValue( currentSpaceSize );

			final Integer currentTimeSize = timeSize.getValue();
			timeSize.setDefaultEditorModel( new IntegerRangeModel( 10, 1, 999, 1 ) );
			timeSize.setValue( currentTimeSize );

			final Integer currentThickness = thickness.getValue();
			thickness.setDefaultEditorModel( new IntegerRangeModel( 5, 1, 20, 1 ) );
			thickness.setValue( currentThickness );

			// do graphical stuff to the graphical thread
			ThreadUtil.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					initOptionPanel();
				}
			} );
		}

		private void initOptionPanel()
		{
			optionPanel = new JPanel( new GridBagLayout() );

			for ( final Var< ? > variable : new Var< ? >[] { location, color, autoSize, spaceSize, timeSize, thickness, showText } )
			{
				final GridBagConstraints gbc = new GridBagConstraints();

				gbc.insets = new Insets( 2, 10, 2, 5 );
				gbc.fill = GridBagConstraints.BOTH;
				optionPanel.add( new JLabel( variable.getName() ), gbc );

				// special case: show unit next to size
				if ( variable == spaceSize )
				{
					gbc.weightx = 0.5;

					final SwingVarEditor< ? > editor = ( SwingVarEditor< ? > ) variable.createVarEditor( true );
					optionPanel.add( editor.getEditorComponent(), gbc );

					gbc.gridwidth = GridBagConstraints.REMAINDER;

					final SwingVarEditor< ? > unitEditor = ( SwingVarEditor< ? > ) spaceUnit.createVarEditor( true );
					optionPanel.add( unitEditor.getEditorComponent(), gbc );
				}
				else if ( variable == timeSize )
				{
					gbc.weightx = 0.5;

					final SwingVarEditor< ? > editor = ( SwingVarEditor< ? > ) variable.createVarEditor( true );
					optionPanel.add( editor.getEditorComponent(), gbc );

					gbc.gridwidth = GridBagConstraints.REMAINDER;

					final SwingVarEditor< ? > unitEditor = ( SwingVarEditor< ? > ) timeUnit.createVarEditor( true );
					optionPanel.add( unitEditor.getEditorComponent(), gbc );
				}
				else
				{
					gbc.weightx = 1;
					gbc.gridwidth = GridBagConstraints.REMAINDER;
					final SwingVarEditor< ? > editor = ( SwingVarEditor< ? > ) variable.createVarEditor( true );
					optionPanel.add( editor.getEditorComponent(), gbc );
				}
			}
		}

		boolean init = false;

		@Override
		public void paint( final Graphics2D g, final Sequence sequence, final IcyCanvas canvas )
		{
			if ( ( canvas instanceof VtkCanvas ) || g == null || !( canvas instanceof IcyCanvas2D ) )
				return;

			final IcyCanvas2D c2 = ( IcyCanvas2D ) canvas;

			final Graphics2D g2 = ( Graphics2D ) g.create();

			g2.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );

			g2.setColor( color.getValue() );

			if ( autoSize.getValue() )
			{
				int sizeW = sequence.getSizeX() / 7;

				// don't draw a scale bar larger than the image itself
				if ( location.getValue().isRelativeToImage() )
					sizeW = ( int ) Math.min( sizeW, sequence.getSizeX() * canvas.getScaleX() * 0.9 );

				final double valueReal = ( sizeW * sequence.getPixelSizeX() / canvas.getScaleX() );
				final UnitPrefix bestUnit = UnitUtil.getBestUnit( valueReal * 0.1, UnitUtil.UnitPrefix.MICRO, 1 );
				final double valueRealBestUnit = UnitUtil.getValueInUnit( valueReal, UnitPrefix.MICRO, bestUnit );

				final double closestScale = MathUtil.closest( valueRealBestUnit, scaleRoundedFactors );
				spaceSize.setValue( ( int ) closestScale );
				spaceUnit.setValue( bestUnit );

				int sizeH = sequence.getSizeY() / 7;

				// don't draw a scale bar larger than the image itself
				if ( location.getValue().isRelativeToImage() )
					sizeH = ( int ) Math.min( sizeH, sequence.getSizeY() * canvas.getScaleY() * 0.9 );

				final double timeValueReal = ( sizeH * sequence.getTimeInterval() / canvas.getScaleY() );
				final UnitPrefix timeBestUnit = UnitUtil.getBestUnit( timeValueReal * 0.1, UnitUtil.UnitPrefix.NONE, 1 );
				final double timeValueRealBestUnit = UnitUtil.getValueInUnit( timeValueReal, UnitPrefix.NONE, timeBestUnit );

				final double timeClosestScale = MathUtil.closest( timeValueRealBestUnit, scaleRoundedFactors );
				timeSize.setValue( ( int ) timeClosestScale );
				timeUnit.setValue( timeBestUnit );

			}

			final String spaceText = "" + spaceSize.getValue() + spaceUnit.getValue() + "m";
			final String timeText = "" + timeSize.getValue() + timeUnit.getValue() + "s";

			final double spaceLength = UnitUtil.getValueInUnit( spaceSize.getValue(), spaceUnit.getValue(), UnitPrefix.MICRO ) / sequence.getPixelSizeX();
			final double timeLength = UnitUtil.getValueInUnit( timeSize.getValue(), timeUnit.getValue(), UnitPrefix.NONE ) / sequence.getTimeInterval();

			float finalThickness = this.thickness.getValue().floatValue();

			final double xc;
			final double yc;
			final double xs;
			final double ys;
			final double xt;
			final double yt;

			switch ( location.getValue() )
			{
			default:
			case VIEWER_BOTTOM_LEFT:
				g2.transform( c2.getInverseTransform() );

				xc = canvas.getCanvasSizeX() * 0.05;
				yc = canvas.getCanvasSizeY() * 0.95;
				xs = xc + spaceLength * c2.getScaleX();
				ys = yc;
				xt = xc;
				yt = yc - timeLength * c2.getScaleY();

				break;

			case IMAGE_BOTTOM_LEFT:

				xc = sequence.getSizeX() * 0.05;
				yc = sequence.getSizeY() * 0.95;
				xs = xc + spaceLength;
				ys = yc;
				xt = xc;
				yt = yc - timeLength;

				finalThickness = ( float ) c2.canvasToImageLogDeltaX( ( int ) finalThickness );
				break;

			case VIEWER_BOTTOM_RIGHT:
				g2.transform( c2.getInverseTransform() );

				xc = canvas.getCanvasSizeX() * 0.95;
				yc = canvas.getCanvasSizeY() * 0.95;
				xs = xc - spaceLength * c2.getScaleX();
				ys = yc;
				xt = xc;
				yt = yc - timeLength * c2.getScaleY();

				break;

			case IMAGE_BOTTOM_RIGHT:

				xc = sequence.getSizeX() * 0.95;
				yc = sequence.getSizeY() * 0.95;
				xs = xc - spaceLength;
				ys = yc;
				xt = xc;
				yt = yc - timeLength;

				finalThickness = ( float ) c2.canvasToImageLogDeltaX( ( int ) finalThickness );
				break;

			case VIEWER_TOP_LEFT:
				g2.transform( c2.getInverseTransform() );

				xc = canvas.getCanvasSizeX() * 0.05;
				yc = canvas.getCanvasSizeY() * 0.05;
				xs = xc + spaceLength * c2.getScaleX();
				ys = yc;
				xt = xc;
				yt = yc + timeLength * c2.getScaleY();

				break;

			case IMAGE_TOP_LEFT:

				xc = sequence.getSizeX() * 0.05;
				yc = sequence.getSizeY() * 0.05;
				xs = xc + spaceLength;
				ys = yc;
				xt = xc;
				yt = yc + timeLength;

				finalThickness = ( float ) c2.canvasToImageLogDeltaX( ( int ) finalThickness );
				break;

			case VIEWER_TOP_RIGHT:
				g2.transform( c2.getInverseTransform() );

				xc = canvas.getCanvasSizeX() * 0.95;
				yc = canvas.getCanvasSizeY() * 0.05;
				xs = xc - spaceLength * c2.getScaleX();
				ys = yc;
				xt = xc;
				yt = yc + timeLength * c2.getScaleY();

				break;

			case IMAGE_TOP_RIGHT:

				xc = sequence.getSizeX() * 0.95;
				xs = xc - spaceLength;
				yc = sequence.getSizeY() * 0.05;
				ys = yc;
				xt = xc;
				yt = yc + timeLength;

				finalThickness = ( float ) c2.canvasToImageLogDeltaX( ( int ) finalThickness );
				break;
			}

			line.reset();
			line.moveTo( xs, ys );
			line.lineTo( xc, yc );
			line.lineTo( xt, yt );
			g2.setStroke( new BasicStroke( finalThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ) );
			g2.draw( line );

			if ( showText.getValue() )
			{
				g2.setFont( g2.getFont().deriveFont( Font.BOLD, finalThickness * 3 ) );
				final int spaceTextwidth = g2.getFontMetrics().stringWidth( spaceText );
				final int timeTextwidth = g2.getFontMetrics().stringWidth( timeText );
				final int textHeight = g2.getFontMetrics().getHeight();

				final double xst = ( ( xs - xc ) > 0 )
						? xs + 1.
						: xs - 1. - spaceTextwidth;
				final double yst = yc + textHeight / 2 + finalThickness / 2.;

				final double xtt = xc - timeTextwidth / 2;
				final double ytt = ( ( yt - yc ) > 0 )
						? yt + 1. + textHeight
						: yt - 1.;

				g2.drawString( spaceText, ( float ) xst, ( float ) yst );
				g2.drawString( timeText, ( float ) xtt, ( float ) ytt );
			}

			g2.dispose();
		}

		@Override
		public JPanel getOptionsPanel()
		{
			return optionPanel;
		}

		/**
		 * @return <code>true</code> if the scale bar automatically adjusts its
		 *         size and unit
		 */
		public boolean getAutoSize()
		{
			return autoSize.getValue();
		}

		/**
		 * @return the color of the scale bar
		 */
		public Color getColor()
		{
			return color.getValue();
		}

		/**
		 * @return the location of the scale bar on the image
		 */
		public ScaleBarLocation getLocation()
		{
			return location.getValue();
		}

		/**
		 * @return the scale bar size (in metric units as defined by the pixel
		 *         size)
		 */
		public double getSize()
		{
			return spaceSize.getValue();
		}

		/**
		 * @return whether the scale bar's size is currently displayed next to
		 *         it
		 */
		public boolean getTextDisplay()
		{
			return showText.getValue();
		}

		/**
		 * Sets whether the scale should automatically guess its optimal size
		 * and unit
		 * 
		 * @param autoSize whether the scale should automatically guess its optimal size
		 * and unit
		 */
		public void setAutoSize( final boolean autoSize )
		{
			this.autoSize.setValue( autoSize );
		}

		/**
		 * Sets the color of the scale bar
		 * 
		 * @param color
		 *            the new color of the scale bar
		 */
		public void setColor( final Color color )
		{
			this.color.setValue( color );
		}

		/**
		 * Sets the location of the scale bar
		 * 
		 * @param location
		 *            the new location of the scale bar
		 */
		public void setLocation( final ScaleBarLocation location )
		{
			this.location.setValue( location );
		}

		/**
		 * Sets whether the scale bar's size should appear next to it
		 * 
		 * @param displayText whether the scale bar's size should appear next to it
		 */
		public void setTextDisplay( final boolean displayText )
		{
			this.showText.setValue( displayText );
		}

		/**
		 * Sets the size of the scale bar in metric units, as defined by the
		 * pixel size
		 * 
		 * @param size
		 *            the new size of the scale bar
		 */
		public void setSize( final int size )
		{
			this.spaceSize.setValue( size );
		}

		/**
		 * Sets the scale bar unit
		 * 
		 * @param unit
		 *            the new unit
		 */
		public void setUnit( final UnitPrefix unit )
		{
			this.spaceUnit.setValue( unit );
		}
	}

	@Override
	public void init()
	{
		if ( preferences == null )
			preferences = getPreferencesRoot();

		Icy.getMainInterface().addGlobalSequenceListener( this );
	}

	@Override
	public void run()
	{}

	@Override
	public void stop()
	{
		for ( final Sequence s : getSequences() )
			removeScaleBarFrom( s );

		Icy.getMainInterface().removeGlobalSequenceListener( this );
	}

	@Override
	public void sequenceOpened( final Sequence sequence )
	{}

	@Override
	public void sequenceClosed( final Sequence sequence )
	{
		removeScaleBarFrom( sequence );
	}

	public static KymographScaleBarOverlay addScaleBarTo( final Sequence sequence )
	{
		if ( sequence == null )
			throw new IcyHandledException( "Cannot add the kymograph scale bar: no sequence specified" );

		if ( sequence.hasOverlay( KymographScaleBarOverlay.class ) )
			return sequence.getOverlays( KymographScaleBarOverlay.class ).get( 0 );

		// Remove 'normal' scale bar if any.
		for ( final Overlay o : sequence.getOverlays( ScaleBarOverlay.class ) )
			sequence.removeOverlay( o );

		final KymographScaleBarOverlay overlay = new KymographScaleBarOverlay();

		sequence.addOverlay( overlay );

		// Try to detect if the sequence is a screenshot (via its name)
		// => hide the overlay by default (but don't remove it)
		final String name = sequence.getName().toLowerCase();
		if ( name.startsWith( "rendering" ) || name.startsWith( "screen shot" ) )
		{
			for ( final Viewer viewer : sequence.getViewers() )
				viewer.getCanvas().getLayer( overlay ).setVisible( false );
		}

		return overlay;
	}

	public static void removeScaleBarFrom( final Sequence sequence )
	{
		if ( sequence == null )
			throw new IcyHandledException( "Cannot remove the kymograph scale bar: no sequence specified" );

		for ( final Overlay o : sequence.getOverlays( KymographScaleBarOverlay.class ) )
			sequence.removeOverlay( o );
	}
}
