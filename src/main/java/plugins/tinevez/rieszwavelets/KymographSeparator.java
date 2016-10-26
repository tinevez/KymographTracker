package plugins.tinevez.rieszwavelets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.preferences.ApplicationPreferences;
import icy.preferences.GeneralPreferences;
import icy.sequence.Sequence;
import plugins.nchenouard.isotropicwavelets.IsotropicWaveletType;

public class KymographSeparator
{

	public static Sequence[] separateKymograph( final Sequence kymographSeq )
	{
		// separate anterograde and retrograde traces

		// compute the Simoncelli's wavelet representation of the sequence
		final int height = kymographSeq.getSizeY();
		final int width = kymographSeq.getSizeX();
		final boolean isRealImage = true;
		// Wavelets
		final int numScales = 4;
		final boolean prefilter = false;
		final IsotropicWaveletType waveletType = IsotropicWaveletType.Simoncelli;
		// Riesz transform
		final int order = 6;
		final HarmonicTypes harmonicType = HarmonicTypes.even;
		final boolean prepareRieszFilters = true;

		final RieszWaveletConfig config = new RieszWaveletConfig(
				width, height,
				isRealImage,
				numScales,
				waveletType,
				prefilter,
				prepareRieszFilters,
				order,
				harmonicType,
				false );
		final ArrayList< RieszConfig > rieszConfigList = config.getRieszConfigurations();

		// compute generalization
		final ArrayList< RieszGeneralization > generalizationList = new ArrayList< RieszGeneralization >();
		for ( int i = 0; i < numScales; i++ )
		{
			final RieszGeneralization rieszGeneralization = new RieszGeneralization(
					StandardRieszFrames.Simoncelli, rieszConfigList.get( i ) );
			generalizationList.add( rieszGeneralization );
		}

		// apply the riesz transforms to the wavelet scales
		final double[] image = kymographSeq.getDataXYAsDouble( 0, 0, 0 );
		final RieszWaveletCoefficients anteroCoefficients = config.multiscaleRieszAnalysisInFourier(
				image, width, height,
				generalizationList );

		final ArrayList< double[][] > retroBands = new ArrayList< double[][] >( anteroCoefficients.getNumScales() );
		for ( int i = 0; i < anteroCoefficients.getNumScales(); i++ )
		{
			final double[][] anteroCoeffs = anteroCoefficients.getRieszBandsAtScale( i );
			final double[][] retroCoeffs = new double[ anteroCoeffs.length ][];
			for ( int j = 0; j < anteroCoeffs.length; j++ )
				retroCoeffs[ j ] = anteroCoeffs[ j ].clone();
			retroBands.add( retroCoeffs );
		}
		double[] retroHPResidual = null;
		if ( anteroCoefficients.getHPResidual() != null )
			retroHPResidual = anteroCoefficients.getHPResidual().clone();
		final double[] retroLPResidual = anteroCoefficients.getLPResidual().clone();

		final RieszWaveletCoefficients retroCoefficients = new RieszWaveletCoefficients(
				anteroCoefficients.getConfig(),
				generalizationList,
				retroBands,
				retroHPResidual, retroLPResidual,
				anteroCoefficients.getPadX(), anteroCoefficients.getPadY() );

		// set to 0 half of the bands
		for ( int i = 0; i < retroCoefficients.getNumScales(); i++ )
		{
			final double[][] retroCoeffs = retroCoefficients.getRieszBandsAtScale( i );

			// Remove first band at theta = 0º;
			for ( int k = 0; k < retroCoeffs[ 0 ].length; k++ )
				retroCoeffs[ 0 ][ k ] = 0;

			// Remove first half bands.
			for ( int j = 0; j < retroCoeffs.length / 2; j++ )
				for ( int k = 0; k < retroCoeffs[ j ].length; k++ )
					retroCoeffs[ j ][ k ] = 0;
		}

		// Remove the lowPass residual
		for ( int k = 0; k < retroLPResidual.length; k++ )
			retroLPResidual[ k ] = 0.;

		// reconstruct image from coefficients
		final Sequence retroSeq = new Sequence();
		final double[] reconstructedImage = config.multiscaleRieszSynthesisInFourier( retroCoefficients, width, height );
		retroSeq.addImage( 0, new IcyBufferedImage( width, height, reconstructedImage ) );

		// Ensure the image does not have negative pixels.
		double minRetro = Double.POSITIVE_INFINITY;
		for ( final double d : reconstructedImage )
			if ( d < minRetro )
				minRetro = d;
		for ( int i = 0; i < reconstructedImage.length; i++ )
		{
			final double d = reconstructedImage[ i ];
			reconstructedImage[ i ] = d + minRetro + Float.MIN_NORMAL;
		}

		for ( int i = 0; i < anteroCoefficients.getNumScales(); i++ )
		{
			final double[][] anteroCoeffs = anteroCoefficients.getRieszBandsAtScale( i );

			// Remove first band at theta = 0º;
			for ( int k = 0; k < anteroCoeffs[ 0 ].length; k++ )
				anteroCoeffs[ 0 ][ k ] = 0;

			for ( int j = ( anteroCoeffs.length / 2 ) + 1; j < anteroCoeffs.length; j++ )
				for ( int k = 0; k < anteroCoeffs[ j ].length; k++ )
					anteroCoeffs[ j ][ k ] = 0;
		}

		// Remove the lowPass residual
		final double[] anteroLPresidual = anteroCoefficients.getLPResidual();
		for ( int k = 0; k < anteroLPresidual.length; k++ )
			anteroLPresidual[ k ] = 0.;

		final Sequence anteroSeq = new Sequence();
		final double[] reconstructedImage2 = config.multiscaleRieszSynthesisInFourier( anteroCoefficients, width, height );

		// Ensure the image does not have negative pixels.
		double minAntero = Double.POSITIVE_INFINITY;
		for ( final double d : reconstructedImage2 )
			if ( d < minAntero )
				minAntero = d;
		for ( int i = 0; i < reconstructedImage2.length; i++ )
		{
			final double d = reconstructedImage2[ i ];
			reconstructedImage2[ i ] = d + minAntero + Float.MIN_NORMAL;
		}

		anteroSeq.addImage( 0, new IcyBufferedImage( width, height, reconstructedImage2 ) );

		// Generate filtered kymograph
		for ( int i = 0; i < anteroCoefficients.getNumScales(); i++ )
		{
			final double[][] anteroCoeffs = anteroCoefficients.getRieszBandsAtScale( i );
			final double[][] retroCoeffs = retroCoefficients.getRieszBandsAtScale( i );
			for ( int j = ( anteroCoeffs.length / 2 ); j < anteroCoeffs.length; j++ )
				for ( int k = 0; k < anteroCoeffs[ j ].length; k++ )
					anteroCoeffs[ j ][ k ] = retroCoeffs[ j ][ k ];
		}

		final Sequence filteredSeq = new Sequence();
		final double[] reconstructedImage3 = config.multiscaleRieszSynthesisInFourier( anteroCoefficients, width, height );
		filteredSeq.addImage( 0, new IcyBufferedImage( width, height, reconstructedImage3 ) );

		return new Sequence[] { filteredSeq, anteroSeq, retroSeq };
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args ) throws UnsupportedFormatException, IOException
	{
		ApplicationPreferences.load();
		GeneralPreferences.load();
		GeneralPreferences.setAutomaticUpdate( false );
		Icy.main( new String[] { "--nosplash", "--disableJCL" } );

		final File file = new File( "samples/Path_1_kymograph.tif" );
		final IcyBufferedImage colorImage = Loader.loadImage( file.getAbsolutePath() );
		final IcyBufferedImage image = colorImage.getImage( 0 );

		final Sequence seq = new Sequence( "source", image );
		final Sequence[] sequences = KymographSeparator.separateKymograph( seq );

		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				new Viewer( seq );
				for ( final Sequence sequence : sequences )
				{
					new Viewer( sequence );
				}
			}
		} );

	}
}
