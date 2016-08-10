package plugins.nchenouard.pathtracing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import icy.canvas.IcyCanvas;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.collection.array.ArrayUtil;

public class InteractiveMultipleDjikstraTracing extends Overlay
{
	// TODO: modify cost to account for the level of directionality of each
	// pixel

	Sequence seq = null;

	int width;

	int height;

	double[] dataSave = null;

	double[][] optimalPath = null;

	int[] pathMap = null;

	final boolean verticalPath;

	boolean mapReady = false;

	double maxIntensity;

	ArrayList< Thread > mapThreadList = new ArrayList< Thread >();

	ArrayList< Thread > pathThreadList = new ArrayList< Thread >();

	Vector< Thread > runningThreads = new Vector< Thread >();

	double alpha;

	private boolean isEnabled = true;

	private boolean paused = false;

	private ArrayList< PathListener > pathListenerList;

	int xInit;

	int yInit;

	int xFinal;

	int yFinal;

	public Lock mapThreadListLock = new ReentrantLock();

	public Lock pathThreadListLock = new ReentrantLock();

	public Lock pathLock = new ReentrantLock();

	public Lock initLock = new ReentrantLock();

	public Lock dataSaveLock = new ReentrantLock();

	public Lock distanceMapLock = new ReentrantLock();

	public Lock visitedLock = new ReentrantLock();

	public Condition visitedCondition = visitedLock.newCondition();

	private DrawingState state;

	long secondClickTime = 0l;

	public enum DrawingState
	{
		RESET, FIRST_CLICKED, SECOND_CLICKED, SECOND_DOUBLE_CLICKED
	}

	public InteractiveMultipleDjikstraTracing( final Sequence seq ) throws IllegalArgumentException
	{
		this( seq, 0.0001, false );
	}

	public InteractiveMultipleDjikstraTracing( final Sequence seq, final double alpha, final boolean verticalPath ) throws IllegalArgumentException
	{
		super( "Path tracer" );
		this.verticalPath = verticalPath;
		if ( seq != null )
		{
			state = DrawingState.RESET;
			this.seq = seq;
			if ( seq.getSizeT() > 1 || seq.getSizeZ() > 1 )
			{
				// throw new IllegalArgumentException("Sorry but the
				// ShortestPath algoritm can only process a 2D image. You should
				// extract first a single image of interest from the focused
				// sequence.");
				new AnnounceFrame( "The tracing algorithm will account for the first image of the spatio-temporal volume only", 7 );
			}
			if ( alpha < 0 )
				throw new IllegalArgumentException( "Regularization parameter should be positive in InteractiveDjisktraTracing" );
			else
				this.alpha = alpha;
			width = seq.getWidth();
			height = seq.getHeight();
			final int component = 0;
			final IcyBufferedImage image = seq.getFirstImage();
			dataSaveLock.lock();
			dataSave = ( double[] ) ArrayUtil.arrayToDoubleArray( image.getDataXY( component ), image.isSignedDataType() );
			maxIntensity = 0;
			for ( int i = 0; i < dataSave.length; i++ )
				if ( dataSave[ i ] > maxIntensity )
					maxIntensity = dataSave[ i ];
			dataSaveLock.unlock();
			pathListenerList = new ArrayList< PathListener >();
		}
		else
			throw new IllegalArgumentException( "Sequence is null in InteractiveDjisktraTracing" );
	}

	/**
	 * Add a listener to the changes of path
	 */
	public void addPathLister( final PathListener listener )
	{
		pathListenerList.add( listener );
	}

	/**
	 * Remove a listener to the changes of path
	 */
	public void removePathLister( final PathListener listener )
	{
		pathListenerList.remove( listener );
	}

	@Override
	public void paint( final Graphics2D g2d, final Sequence sequence, final IcyCanvas canvas )
	{
		final Color tempLineColor = Color.YELLOW;
		final Color lineColor = Color.RED;
		final Color extremityColor = Color.BLUE;
		final float shiftDrawX = 0.5f;
		final float shiftDrawY = 0.5f;

		if ( isEnabled && !paused )
		{
			if ( optimalPath != null )
			{
				g2d.setStroke( new BasicStroke( 0.5f ) );
				try
				{
					pathLock.lock();
					{
						for ( int cnt = 0; cnt < optimalPath.length; cnt++ )
						{
							if ( cnt > 0 )
							{
								g2d.setColor( tempLineColor );
								final Line2D.Float l = new Line2D.Float( ( ( int ) optimalPath[ cnt ][ 0 ] ) + shiftDrawX, ( ( int ) optimalPath[ cnt ][ 1 ] ) + shiftDrawY, ( ( int ) optimalPath[ cnt - 1 ][ 0 ] ) + shiftDrawX, ( ( int ) optimalPath[ cnt - 1 ][ 1 ] ) + shiftDrawY );
								g2d.draw( l );
							}
						}
					}
				}
				finally
				{
					pathLock.unlock();
				}
				if ( !optimalPathList.isEmpty() )
				{
					for ( int i = 0; i < optimalPathList.size(); i++ )
					{
						final double[][] path = optimalPathList.get( i );
						for ( int cnt = 0; cnt < path.length; cnt++ )
						{
							if ( cnt > 0 )
							{
								g2d.setColor( lineColor );
								final Line2D.Float l = new Line2D.Float( ( ( int ) path[ cnt ][ 0 ] ) + shiftDrawX, ( ( int ) path[ cnt ][ 1 ] ) + shiftDrawY, ( ( int ) path[ cnt - 1 ][ 0 ] ) + shiftDrawX, ( ( int ) path[ cnt - 1 ][ 1 ] ) + shiftDrawY );
								g2d.draw( l );
							}
						}
					}
				}
			}
			final Point p = new Point( ( int ) canvas.getMouseImagePosX(), ( int ) canvas.getMouseImagePosY() );
			if ( p != null )
			{
				g2d.setStroke( new BasicStroke( 0.5f ) );
				g2d.setColor( extremityColor );
				Line2D.Float l = new Line2D.Float( p.x - 1 + shiftDrawX, p.y - 1 + shiftDrawY, p.x + 1 + shiftDrawX, p.y + 1 + shiftDrawY );
				g2d.draw( l );
				l = new Line2D.Float( p.x - 1 + shiftDrawX, p.y + 1 + shiftDrawY, p.x + 1 + shiftDrawX, p.y - 1 + shiftDrawY );
				g2d.draw( l );
			}
		}
	}

	@Override
	public void mousePressed( final MouseEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{}

	@Override
	public void mouseReleased( final MouseEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{}

	Thread storePathThread;

	private void finalizePath( final int xF, final int yF, final boolean addNewPathThread )
	{
		if ( mapReady )
		{
			// stop all the path computation threads and launch a new one for
			// the clicked point
			pathThreadListLock.lock();
			for ( final Thread thr : pathThreadList )
				( ( StoppableThread ) thr ).stopThread();
			pathThreadListLock.unlock();
			initLock.lock();
			xFinal = xF;
			yFinal = yF;
			storePathThread = new ComputeFinalPathThread( xInit, yInit, xFinal, yFinal, addNewPathThread );
			initLock.unlock();
			// pathThreadListLock.lock();
			// pathThreadList.add(thr);
			// pathThreadListLock.unlock();
			storePathThread.start();
		}
		else // wait the map to compute the pixel of interest
		{
			pathThreadListLock.lock();
			for ( final Thread thr : pathThreadList )
				( ( StoppableThread ) thr ).stopThread();
			pathThreadListLock.unlock();
			initLock.lock();
			xFinal = xF;
			yFinal = yF;
			storePathThread = new ComputeFinalPathThreadWaiting( xInit, yInit, xFinal, yFinal, addNewPathThread );
			initLock.unlock();
			// pathThreadListLock.lock();
			// pathThreadList.add(thr);
			// pathThreadListLock.unlock();
			storePathThread.start();
		}
	}

	private void initNewMapThread( final int xi, final int yi )
	{
		// stop the map and path computing threads as there is a new initial
		// point
		mapThreadListLock.lock();
		for ( final Thread t : mapThreadList )
			( ( StoppableThread ) t ).stopThread();
		mapThreadListLock.unlock();

		pathThreadListLock.lock();
		for ( final Thread t : pathThreadList )
			( ( StoppableThread ) t ).stopThread();
		pathThreadListLock.unlock();

		// init new map and path computation threads from the clicked points
		initLock.lock();
		xInit = xi;
		yInit = yi;
		Thread thr = null;
		if ( verticalPath )
			thr = new ComputeMapThreadToTop( xInit, yInit );
		else
			thr = new ComputeMapThreadBidirectional( xInit, yInit );
		initLock.unlock();

		mapReady = false;
		mapThreadListLock.lock();
		mapThreadList.add( thr );
		mapThreadListLock.unlock();
		thr.start();
	}

	@Override
	public void mouseClick( final MouseEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{
		if ( isEnabled && !paused )
		{
			switch ( state )
			{
			case RESET:
			case SECOND_DOUBLE_CLICKED:
			{
				final int xi = ( int ) Math.round( imagePoint.getX() );
				final int yi = ( int ) Math.round( imagePoint.getY() );
				if ( xi >= 0 && xi < width && yi >= 0 && yi < height )
				{
					initNewMapThread( xi, yi );
					state = DrawingState.FIRST_CLICKED;
				}
				break;
			}
			case SECOND_CLICKED:
				final long t = System.currentTimeMillis();
				if ( t - secondClickTime < 1000 )
				{
					// double clicked: do not add new path then
					final int xF = ( int ) Math.round( imagePoint.getX() );
					final int yF = ( int ) Math.round( imagePoint.getY() );
					if ( xF >= 0 && xF < width && yF >= 0 && yF < height )
					{
						paused = true;
						finalizePath( xF, yF, false );
						state = DrawingState.SECOND_DOUBLE_CLICKED;
					}
					break;
				}
				// else act as first clicked
			case FIRST_CLICKED:
			{
				final int xF = ( int ) Math.round( imagePoint.getX() );
				final int yF = ( int ) Math.round( imagePoint.getY() );
				if ( xF >= 0 && xF < width && yF >= 0 && yF < height )
				{
					paused = true;
					secondClickTime = System.currentTimeMillis();
					finalizePath( xF, yF, true );
					state = DrawingState.SECOND_CLICKED;
				}
			}
			}
		}
	}

	@Override
	public void mouseMove( final MouseEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{
		if ( isEnabled && !paused )
			if ( state == DrawingState.FIRST_CLICKED || state == DrawingState.SECOND_CLICKED )
			{
				if ( mapReady )
				{
					pathThreadListLock.lock();
					for ( final Thread t : pathThreadList )
						( ( StoppableThread ) t ).stopThread();
					// pathThreadListLock.unlock();
					final int xF = ( int ) Math.round( imagePoint.getX() );
					final int yF = ( int ) Math.round( imagePoint.getY() );
					if ( xF >= 0 && xF < width && yF >= 0 && yF < height )
					{
						if ( !verticalPath || yF > yInit )
						{
							xFinal = xF;
							yFinal = yF;
							initLock.lock();
							final Thread thr = new ComputePathThread( xInit, yInit, xFinal, yFinal );
							initLock.unlock();
							// pathThreadListLock.lock();
							pathThreadList.add( thr );
							// pathThreadListLock.unlock();
							thr.start();
						}
					}
					pathThreadListLock.unlock();
				}
				else // wait the map to compute the pixel of interest
				{
					pathThreadListLock.lock();
					for ( final Thread t : pathThreadList )
						( ( StoppableThread ) t ).stopThread();
					// pathThreadListLock.unlock();
					final int xF = ( int ) Math.round( imagePoint.getX() );
					final int yF = ( int ) Math.round( imagePoint.getY() );
					if ( xF >= 0 && xF < width && yF >= 0 && yF < height )
					{
						xFinal = xF;
						yFinal = yF;
						initLock.lock();
						final Thread thr = new ComputePathThreadWaiting( xInit, yInit, xFinal, yFinal );
						initLock.unlock();
						// pathThreadListLock.lock();
						pathThreadList.add( thr );
						// pathThreadListLock.unlock();
						thr.start();
					}
					pathThreadListLock.unlock();
				}
			}
			else
				seq.overlayChanged( this );
	}

	@Override
	public void mouseDrag( final MouseEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{}

	@Override
	public void keyPressed( final KeyEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{}

	@Override
	public void keyReleased( final KeyEvent e, final Point2D imagePoint, final IcyCanvas canvas )
	{}

	public Number getIntensityAt( final double x, final double y )
	{
		if ( dataSave != null && x >= 0 && x < width && y >= 0 && y < height )
		{
			return dataSave[ ( int ) Math.round( x ) + ( int ) Math.round( y ) * width ];
		}
		else
			return -1;
	}

	// public double[][] getOptimalPathCopy() {
	// if (optimalPath == null)
	// return null;
	// pathLock.lock();
	// //clone optimalPath
	// final double[][] pathClone = new double[optimalPath.length][];
	// for (int i = 0; i< optimalPath.length; i++)
	// {
	// pathClone[i] = optimalPath[i].clone();
	// }
	// pathLock.unlock();
	// return pathClone;
	// }

	public ArrayList< double[][] > getOptimalPathCopy()
	{
		final ArrayList< double[][] > copy = new ArrayList< double[][] >();
		pathLock.lock();
		for ( final double[][] path : optimalPathList )
			copy.add( path.clone() );
		pathLock.unlock();
		return copy;
	}

	public void enable()
	{
		isEnabled = true;
		seq.overlayChanged( this );
	}

	public void disable()
	{
		isEnabled = false;
		seq.overlayChanged( this );
	}

	ArrayList< double[][] > optimalPathList = new ArrayList< double[][] >();

	private void storeOptimalPath()
	{
		pathLock.lock();
		if ( optimalPath != null )
			optimalPathList.add( optimalPath );
		pathLock.unlock();
	}

	abstract class ComputeMapThread extends StoppableThread
	{

		int xInit;

		int yInit;

		double[] dist = new double[ width * height ];

		int[] prev = new int[ width * height ];

		boolean[] visited = new boolean[ width * height ];

		double[] data = null;

		boolean run = true;

		public ComputeMapThread( final int xInit, final int yInit )
		{
			this.xInit = xInit;
			this.yInit = yInit;
			dataSaveLock.lock();
			data = new double[ dataSave.length ];
			System.arraycopy( dataSave, 0, data, 0, data.length );
			dataSaveLock.unlock();
		}

		@Override
		abstract public void run();

		public double dist( final double i1, final double i2 )
		{
			// return Math.abs(i1-i2);
			// int cost = 0;
			// if (i1 > i2)
			// return 2;
			// else
			// return 1;
			// return 1+Math.max(i1-i2, 0);
			return maxIntensity / ( i2 + alpha );
		}

		protected void test( final double currentDist, final double refI, final int currentIdx, final int idx )
		{
			if ( !visited[ idx ] )
			{
				final double d = currentDist + dist( refI, data[ idx ] );
				if ( d < dist[ idx ] )
				{
					dist[ idx ] = d;
					prev[ idx ] = currentIdx;
				}
			}
		}

		@Override
		public void stopThread()
		{
			run = false;
		}
	}

	class ComputeMapThreadBidirectional extends ComputeMapThread
	{
		public ComputeMapThreadBidirectional( final int xInit, final int yInit )
		{
			super( xInit, yInit );
		}

		@Override
		public void run()
		{
			for ( int u = 0; u < dist.length; u++ )
			{
				dist[ u ] = Double.MAX_VALUE;
				// visited[u] = false;
			}
			final int idxInit = yInit * width + xInit;

			dist[ idxInit ] = 0;

			int currentIdx = idxInit;
			int currentX = xInit;
			int currentY = yInit;

			boolean stop = false;

			while ( !stop && run )
			{
				final double ival = data[ currentIdx ];
				final double currentDist = dist[ currentIdx ];

				if ( currentX > 0 )
				{
					test( currentDist, ival, currentIdx, currentIdx - 1 );

					if ( currentY > 0 )
					{
						test( currentDist, ival, currentIdx, currentIdx - width - 1 );
						test( currentDist, ival, currentIdx, currentIdx - width );
					}
					if ( currentY < height - 1 )
					{
						test( currentDist, ival, currentIdx, currentIdx + width - 1 );
						test( currentDist, ival, currentIdx, currentIdx + width );
					}
					if ( currentX < width - 1 )
					{
						test( currentDist, ival, currentIdx, currentIdx + 1 );
						if ( currentY > 0 )
						{
							test( currentDist, ival, currentIdx, currentIdx - width + 1 );
						}
						if ( currentY < height - 1 )
						{
							test( currentDist, ival, currentIdx, currentIdx + width + 1 );
						}
					}
				}
				else
				{
					if ( currentX < width - 1 )
					{
						test( currentDist, ival, currentIdx, currentIdx + 1 );
						if ( currentY > 0 )
						{
							test( currentDist, ival, currentIdx, currentIdx - width + 1 );
							test( currentDist, ival, currentIdx, currentIdx - width );
						}
						if ( currentY < height - 1 )
						{
							test( currentDist, ival, currentIdx, currentIdx + width + 1 );
							test( currentDist, ival, currentIdx, currentIdx + width );
						}
					}
					else
					{
						if ( currentY > 0 )
						{
							test( currentDist, ival, currentIdx, currentIdx - width );
						}
						if ( currentY < height - 1 )
						{
							test( currentDist, ival, currentIdx, currentIdx + width );
						}
					}
				}

				visited[ currentIdx ] = true;
				visitedLock.lock();
				visitedCondition.signalAll();
				visitedLock.unlock();
				// search for the next node to consider
				double minDist = Double.MAX_VALUE;
				int minIdx = -1;
				for ( int cnt = 0; cnt < dist.length; cnt++ )
				{
					if ( ( !visited[ cnt ] ) && ( ( dist[ cnt ] < minDist ) || minIdx == -1 ) )
					{
						minDist = dist[ cnt ];
						minIdx = cnt;
					}
				}
				if ( minIdx == -1 )
				{
					stop = true;
				}
				else
				{
					currentIdx = minIdx;
					currentY = currentIdx / width;
					currentX = currentIdx - width * currentY;
				}
			}
			if ( run )
			{
				distanceMapLock.lock();
				pathMap = prev;
				distanceMapLock.unlock();
				mapReady = true;
			}
			mapThreadListLock.lock();
			mapThreadList.remove( this );
			mapThreadListLock.unlock();
			seq.overlayChanged( InteractiveMultipleDjikstraTracing.this );
		}
	}

	class ComputeMapThreadToTop extends ComputeMapThread
	{
		public ComputeMapThreadToTop( final int xInit, final int yInit )
		{
			super( xInit, yInit );
		}

		@Override
		public void run()
		{
			for ( int u = 0; u < dist.length; u++ )
				dist[ u ] = Double.MAX_VALUE;
			final int idxInit = yInit * width + xInit;

			dist[ idxInit ] = 0;

			int currentIdx = idxInit;
			int currentX = xInit;
			int currentY = yInit;

			boolean stop = false;

			while ( !stop && run )
			{
				final double ival = data[ currentIdx ];
				final double currentDist = dist[ currentIdx ];

				if ( currentX > 0 )
				{
					test( currentDist, ival, currentIdx, currentIdx - 1 ); // test
																			// at
																			// location
																			// currentX
																			// -
																			// 1;

					if ( currentY < height - 1 ) // test at location currentY +
													// 1
					{
						test( currentDist, ival, currentIdx, currentIdx + width - 1 );
						test( currentDist, ival, currentIdx, currentIdx + width );
					}
					if ( currentX < width - 1 ) // test at location currentX +
												// 1;
					{
						test( currentDist, ival, currentIdx, currentIdx + 1 );
						if ( currentY < height - 1 ) // test at location
														// currentY + 1
						{
							test( currentDist, ival, currentIdx, currentIdx + width + 1 );
						}
					}
				}
				else
				{
					if ( currentX < width - 1 )
					{
						test( currentDist, ival, currentIdx, currentIdx + 1 );
						if ( currentY < height - 1 )
						{
							test( currentDist, ival, currentIdx, currentIdx + width + 1 );
							test( currentDist, ival, currentIdx, currentIdx + width );
						}
					}
					else
					{
						if ( currentY < height - 1 )
						{
							test( currentDist, ival, currentIdx, currentIdx + width );
						}
					}
				}

				visited[ currentIdx ] = true;
				visitedLock.lock();
				visitedCondition.signalAll();
				visitedLock.unlock();
				// search for the next node to consider
				double minDist = Double.MAX_VALUE - 1;
				int minIdx = -1;
				for ( int cnt = 0; cnt < dist.length; cnt++ )
				{
					if ( ( !visited[ cnt ] ) && ( ( dist[ cnt ] < minDist ) || minIdx == -1 ) )
					{
						minDist = dist[ cnt ];
						minIdx = cnt;
					}
				}
				if ( minIdx == -1 )
				{
					stop = true;
				}
				else
				{
					currentIdx = minIdx;
					currentY = currentIdx / width;
					currentX = currentIdx - width * currentY;
				}
			}
			if ( run )
			{
				distanceMapLock.lock();
				pathMap = prev;
				distanceMapLock.unlock();
				mapReady = true;
			}
			mapThreadListLock.lock();
			mapThreadList.remove( this );
			mapThreadListLock.unlock();
			seq.overlayChanged( InteractiveMultipleDjikstraTracing.this );
		}
	}

	class ComputeFinalPathThread extends ComputePathThread
	{

		boolean addNewMapThread = false;

		public ComputeFinalPathThread( final int xInit, final int yInit, final int xFinal, final int yFinal, final boolean addNewPathThread )
		{
			super( xInit, yInit, xFinal, yFinal );
			this.addNewMapThread = addNewPathThread;
		}

		@Override
		public void run()
		{
			try
			{
				super.runFunction();
				storeOptimalPath();
				if ( addNewMapThread )
				{
					initNewMapThread( xFinal, yFinal );
				}
			}
			finally
			{
				paused = false;
			}
			// pathThreadListLock.lock();
			// pathThreadList.remove(this);
			// pathThreadListLock.unlock();
		}
	}

	class ComputePathThread extends StoppableThread implements ActionListener
	{
		int xInit;

		int yInit;

		int xFinal;

		int yFinal;

		boolean run = true;

		public ComputePathThread( final int xInit, final int yInit, final int xFinal, final int yFinal )
		{
			this.xInit = xInit;
			this.yInit = yInit;
			this.xFinal = xFinal;
			this.yFinal = yFinal;
		}

		private void runFunction()
		{

			distanceMapLock.lock();
			final int[] prev = new int[ pathMap.length ];
			System.arraycopy( pathMap, 0, prev, 0, prev.length );
			distanceMapLock.unlock();
			final int width = seq.getWidth();
			final int idxInit = xInit + yInit * width;
			final int idxFinal = xFinal + yFinal * width;

			final ArrayList< Integer > path = new ArrayList< Integer >();
			int idx = idxFinal;
			path.add( new Integer( idxFinal ) );
			while ( idx != idxInit && !isInterrupted() && run )
			{
				idx = prev[ idx ];
				path.add( new Integer( idx ) );
			}
			final int pathLength = path.size();
			final double[][] pathTab = new double[ pathLength ][ 2 ];
			for ( int cnt = 0; cnt < pathLength; cnt++ )
			{
				final int tmp = path.get( pathLength - cnt - 1 );
				pathTab[ cnt ] = new double[] { tmp % width, tmp / width };
			}
			if ( run )
			{
				pathLock.lock();
				optimalPath = pathTab;
				if ( !pathListenerList.isEmpty() )
				{
					// clone optimalPath
					final double[][] pathClone = new double[ optimalPath.length ][];
					for ( int i = 0; i < optimalPath.length; i++ )
					{
						pathClone[ i ] = optimalPath[ i ].clone();
					}
					if ( state == DrawingState.SECOND_DOUBLE_CLICKED )
					{
						final FireListenersThread thr = new FireListenersThread( pathClone, PathEvent.FINAL_PATH );
						thr.start();
					}
					else
					{
						final FireListenersThread thr = new FireListenersThread( pathClone, PathEvent.TEMPORARY_PATH );
						thr.start();
					}
				}
				pathLock.unlock();
				seq.overlayChanged( InteractiveMultipleDjikstraTracing.this );
			}
		}

		@Override
		public void run()
		{
			runFunction();
			pathThreadListLock.lock();
			pathThreadList.remove( this );
			pathThreadListLock.unlock();
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			interrupt();
		}

		@Override
		public void stopThread()
		{
			run = false;
		}
	}

	class ComputeFinalPathThreadWaiting extends ComputePathThreadWaiting
	{
		boolean addNewMapThread = false;

		public ComputeFinalPathThreadWaiting( final int xInit, final int yInit, final int xFinal, final int yFinal, final boolean addNewMapThread )
		{
			super( xInit, yInit, xFinal, yFinal );
			this.addNewMapThread = addNewMapThread;
		}

		@Override
		public void run()
		{
			try
			{
				super.runFunction();
				storeOptimalPath();
				if ( addNewMapThread )
				{
					initNewMapThread( xFinal, yFinal );
				}
			}
			finally
			{
				paused = false;
			} ;
			// pathThreadListLock.lock();
			// pathThreadList.remove(this);
			// pathThreadListLock.unlock();
		}
	}

	class ComputePathThreadWaiting extends StoppableThread implements ActionListener
	{
		int xInit;

		int yInit;

		int xFinal;

		int yFinal;

		boolean run = true;

		public ComputePathThreadWaiting( final int xInit, final int yInit, final int xFinal, final int yFinal )
		{
			this.xInit = xInit;
			this.yInit = yInit;
			this.xFinal = xFinal;
			this.yFinal = yFinal;
		}

		private void runFunction()
		{
			boolean found = false;
			boolean wait = false;
			final int idxInit = xInit + yInit * width;
			final int idxFinal = xFinal + yFinal * width;
			ArrayList< Thread > threadList = new ArrayList< Thread >();
			final int[] prev = new int[ width * height ];
			while ( run && !found )
			{
				mapThreadListLock.lock();
				threadList = new ArrayList< Thread >();
				threadList.addAll( mapThreadList );
				mapThreadListLock.unlock();
				for ( final Thread thr : threadList )
				{
					final ComputeMapThread mapThread = ( ComputeMapThread ) thr;
					if ( mapThread.run && mapThread.xInit == xInit && mapThread.yInit == mapThread.yInit )
					{
						if ( mapThread.visited[ idxFinal ] )
						{
							visitedLock.lock();
							System.arraycopy( mapThread.prev, 0, prev, 0, mapThread.prev.length );
							found = true;
							visitedLock.unlock();
						}
						else
							wait = true;
						break;
					}
					if ( wait )
					{
						visitedLock.lock();
						try
						{
							visitedCondition.await(); // we could put a timer to
														// force it to retry
						}
						catch ( final InterruptedException e )
						{
							e.printStackTrace();
						}
						finally
						{
							visitedLock.unlock();
						}
					}
				}
			}
			final ArrayList< Integer > path = new ArrayList< Integer >();
			int idx = idxFinal;
			path.add( new Integer( idxFinal ) );
			while ( idx != idxInit && !isInterrupted() && run )
			{
				idx = prev[ idx ];
				path.add( new Integer( idx ) );
			}
			final int pathLength = path.size();
			final double[][] pathTab = new double[ pathLength ][ 2 ];
			for ( int cnt = 0; cnt < pathLength; cnt++ )
			{
				final int tmp = path.get( pathLength - cnt - 1 );
				pathTab[ cnt ] = new double[] { tmp % width, tmp / width };
			}
			if ( run )
			{
				pathLock.lock();
				optimalPath = pathTab;
				if ( !pathListenerList.isEmpty() )
				{
					// clone optimalPath
					final double[][] pathClone = new double[ optimalPath.length ][];
					for ( int i = 0; i < optimalPath.length; i++ )
					{
						pathClone[ i ] = optimalPath[ i ].clone();
					}
					if ( state == DrawingState.SECOND_DOUBLE_CLICKED )
					{
						final FireListenersThread thr = new FireListenersThread( pathClone, PathEvent.FINAL_PATH );
						thr.start();
					}
					else
					{
						final FireListenersThread thr = new FireListenersThread( pathClone, PathEvent.TEMPORARY_PATH );
						thr.start();
					}
				}
				pathLock.unlock();
				seq.overlayChanged( InteractiveMultipleDjikstraTracing.this );
			}
		}

		@Override
		public void run()
		{
			runFunction();
			pathThreadListLock.lock();
			pathThreadList.remove( this );
			pathThreadListLock.unlock();
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			interrupt();
		}

		@Override
		public void stopThread()
		{
			run = false;
		}
	}

	class FireListenersThread extends Thread
	{
		double[][] path;

		PathEvent pathEvent;

		public FireListenersThread( final double[][] path, final PathEvent event )
		{
			runningThreads.add( this );
			this.path = path;
			this.pathEvent = event;
		}

		@Override
		public void run()
		{
			for ( final PathListener listener : pathListenerList )
				listener.refreshPath( pathEvent, InteractiveMultipleDjikstraTracing.this, path );
			runningThreads.remove( this );
		}
	}

	abstract class StoppableThread extends Thread
	{
		public abstract void stopThread();
	}
}
