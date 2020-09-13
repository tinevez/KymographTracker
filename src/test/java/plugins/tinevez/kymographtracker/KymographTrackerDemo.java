package plugins.tinevez.kymographtracker;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.file.Loader;
import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.sequence.Sequence;

public class KymographTrackerDemo
{

	public static void main( final String[] args ) throws InvocationTargetException, InterruptedException
	{
		// Launch the application.
		Icy.main( args );

		// Load an image.
		final String imagePath = "samples/IFT81-mNG puro 06.tif";
		final Sequence sequence = Loader.loadSequence( imagePath, 0, true );

		// Display the images.
		SwingUtilities.invokeAndWait( () -> {
			new Viewer( sequence );
		} );

		new KymographTracker2().run();
	}
}
