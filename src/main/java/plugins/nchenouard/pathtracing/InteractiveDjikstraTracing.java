package plugins.nchenouard.pathtracing;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.collection.array.ArrayUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Interactive tracing painter that uses the Djikstra minimal path algorithm
 * The user is clicking in the sequence and the plug in automatically displays the shortest path
 * between the clicked point and the mouse 
 * 
 * @author nicolas chenouard
 * */

public class InteractiveDjikstraTracing extends Overlay
{
	Sequence seq = null;
	int width;
	int height;
	double[] dataSave = null;
	double[][] optimalPath = null;
	int[] pathMap = null;
	boolean verticalPath = false;
	boolean mapReady = false;
	double maxIntensity;
	ArrayList<Thread> mapThreadList = new ArrayList<Thread>();
	ArrayList<Thread> pathThreadList = new ArrayList<Thread>();
	Vector<Thread> runningThreads = new Vector<Thread>();
	double alpha;
	private boolean isEnabled = true;

	private ArrayList<PathListener> pathListenerList;

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
	public enum DrawingState
	{
		RESET, FIRST_CLICKED, SECOND_CLICKED,
	}

	public InteractiveDjikstraTracing(Sequence seq) throws IllegalArgumentException
	{
		this(seq, 0.0001);
	}

	public InteractiveDjikstraTracing(Sequence seq, double alpha) throws IllegalArgumentException
	{
		super("Tracer");
		if (seq != null)
		{
			state = DrawingState.RESET;
			this.seq = seq;
			if (seq.getSizeT() > 1 || seq.getSizeZ() > 1)
			{
				throw new IllegalArgumentException("Sorry but the ShortestPath algoritm can only process a 2D image. You should extract first a single image of interest from the focused sequence.");
			}
			if (alpha < 0)
				throw new IllegalArgumentException("Regularization parameter should be positive in InteractiveDjisktraTracing");
			else
				this.alpha = alpha;
			width = seq.getWidth();
			height = seq.getHeight();
			int component = 0;
			IcyBufferedImage image = seq.getFirstImage();
			dataSaveLock.lock();
			dataSave = (double[])ArrayUtil.arrayToDoubleArray(image.getDataXY(component), image.isSignedDataType() );
			maxIntensity = 0;
			for (int i = 0; i < dataSave.length; i++)
				if(dataSave[i]>maxIntensity)
					maxIntensity = dataSave[i];
			dataSaveLock.unlock();
			pathListenerList = new ArrayList<PathListener>();
		}
		else
			throw new IllegalArgumentException("Sequence is null in InteractiveDjisktraTracing");
	}

	/**
	 * Add a listener to the changes of path
	 * */
	public void addPathLister(PathListener listener)
	{
		pathListenerList.add(listener);
	}

	/**
	 * Remove a listener to the changes of path
	 * */
	public void removePathLister(PathListener listener)
	{
		pathListenerList.remove(listener);
	}

	abstract class StoppableThread extends Thread
	{
		public abstract void stopThread();
	}

	class ComputePathThread extends StoppableThread implements ActionListener
	{
		int xInit;
		int yInit;
		int xFinal;
		int yFinal;

		boolean run = true;

		public ComputePathThread(int xInit, int yInit, int xFinal, int yFinal)
		{
			this.xInit = xInit;
			this.yInit = yInit;
			this.xFinal = xFinal;
			this.yFinal = yFinal;
		}

		@Override
		public void run()
		{
			distanceMapLock.lock();
			int[] prev = new int[pathMap.length];
			System.arraycopy(pathMap, 0, prev, 0, prev.length);
			distanceMapLock.unlock();
			int width = seq.getWidth();
			int idxInit = xInit + yInit*width;
			int idxFinal = xFinal + yFinal*width;

			ArrayList<Integer> path = new ArrayList<Integer>();
			int idx = idxFinal;
			path.add(new Integer(idxFinal));
			while(idx!=idxInit && !isInterrupted() && run)
			{
				idx=prev[idx];
				path.add(new Integer(idx));
			}
			int pathLength = path.size();
			double[][] pathTab = new double[pathLength][2];
			for (int cnt = 0; cnt<pathLength; cnt++)
			{
				int tmp = path.get(pathLength-cnt-1);
				pathTab[cnt] = new double[]{tmp%width,(int)(tmp/width)};
			}
			if (run)
			{
				pathLock.lock();
				optimalPath = pathTab;
				if (!pathListenerList.isEmpty())
				{
					//clone optimalPath
					final double[][] pathClone = new double[optimalPath.length][];
					for (int i = 0; i<optimalPath.length; i++)
					{
						pathClone[i] = optimalPath[i].clone();
					}
					if (state==DrawingState.SECOND_CLICKED)
					{
						FireListenersThread thr = new FireListenersThread(pathClone, PathEvent.FINAL_PATH);
						thr.start();
					}
					else
					{
						FireListenersThread thr = new FireListenersThread(pathClone, PathEvent.TEMPORARY_PATH);
						thr.start();
					}
				}
				pathLock.unlock();
				seq.overlayChanged(InteractiveDjikstraTracing.this);
			}
			pathThreadListLock.lock();
			pathThreadList.remove(this);
			pathThreadListLock.unlock();
		}

		public void actionPerformed(ActionEvent e){
			interrupt();
		}

		@Override
		public void stopThread() {
			run = false;
		}
	}

	class ComputePathThreadWaiting extends StoppableThread implements ActionListener
	{
		int xInit;
		int yInit;
		int xFinal;
		int yFinal;

		boolean run = true;

		public ComputePathThreadWaiting(int xInit, int yInit, int xFinal, int yFinal)
		{
			this.xInit = xInit;
			this.yInit = yInit;
			this.xFinal = xFinal;
			this.yFinal = yFinal;
		}

		@Override
		public void run()
		{
			boolean found = false;
			boolean wait = false;
			int idxInit = xInit + yInit*width;
			int idxFinal = xFinal + yFinal*width;
			ArrayList<Thread> threadList = new ArrayList<Thread>();
			int[] prev = new int[width*height];
			while(run && !found)
			{
				mapThreadListLock.lock();
				threadList = new ArrayList<Thread>();
				threadList.addAll(mapThreadList);
				mapThreadListLock.unlock();
				for (Thread thr:threadList)
				{
					ComputeMapThread mapThread = (ComputeMapThread) thr;
					if(mapThread.run && mapThread.xInit==xInit && mapThread.yInit == mapThread.yInit)
					{
						if (mapThread.visited[idxFinal])
						{
							visitedLock.lock();
							System.arraycopy(mapThread.prev, 0, prev, 0, mapThread.prev.length);
							found = true;
							visitedLock.unlock();
						}
						else
							wait=true;
						break;
					}
					if (wait)
					{
						visitedLock.lock();	
						try{
							visitedCondition.await();//we could put a timer to force it to retry
						}
						catch(InterruptedException e)
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
			ArrayList<Integer> path = new ArrayList<Integer>();
			int idx = idxFinal;
			path.add(new Integer(idxFinal));
			while(idx!=idxInit && !isInterrupted() && run)
			{
				idx=prev[idx];
				path.add(new Integer(idx));
			}
			int pathLength = path.size();
			double[][] pathTab = new double[pathLength][2];
			for (int cnt = 0; cnt<pathLength; cnt++)
			{
				int tmp = path.get(pathLength-cnt-1);
				pathTab[cnt] = new double[]{tmp%width,(int)(tmp/width)};
			}
			if (run)
			{
				pathLock.lock();
				optimalPath = pathTab;
				if (!pathListenerList.isEmpty())
				{
					//clone optimalPath
					final double[][] pathClone = new double[optimalPath.length][];
					for (int i = 0; i<optimalPath.length; i++)
					{
						pathClone[i] = optimalPath[i].clone();
					}
					if (state==DrawingState.SECOND_CLICKED)
					{
						FireListenersThread thr = new FireListenersThread(pathClone, PathEvent.FINAL_PATH);
						thr.start();
					}
					else
					{
						FireListenersThread thr = new FireListenersThread(pathClone, PathEvent.TEMPORARY_PATH);
						thr.start();
					}
				}
				pathLock.unlock();
				//seq.beginUpdate();
				seq.overlayChanged(InteractiveDjikstraTracing.this);
			}
			pathThreadListLock.lock();
			pathThreadList.remove(this);
			pathThreadListLock.unlock();
		}

		public void actionPerformed(ActionEvent e){
			interrupt();
		}

		@Override
		public void stopThread() {
			run = false;
		}
	}

	class ComputeMapThread extends StoppableThread
	{
		int xInit;
		int yInit;

		double[] dist = new double[width*height];
		int[] prev  = new int[width*height];
		boolean[] visited = new boolean[width*height];
		double[] data = null;

		boolean run = true;

		public ComputeMapThread(int xInit, int yInit)
		{
			this.xInit = xInit;
			this.yInit = yInit;
			dataSaveLock.lock();
			data = new double[dataSave.length];
			System.arraycopy(dataSave, 0, data, 0, data.length);
			dataSaveLock.lock();
		}


		@Override
		public void run()
		{	
			for (int u = 0; u < dist.length; u++)
			{
				dist[u] = Double.MAX_VALUE;
				//visited[u] = false;
			}
			int idxInit = yInit*width+xInit;

			dist[idxInit] = 0;

			int currentIdx = idxInit;
			int currentX = xInit;
			int currentY = yInit;

			boolean stop = false;

			while (!stop && run)
			{
				double ival = data[currentIdx];
				double currentDist = dist[currentIdx];

				if (currentX>0)
				{
					test(currentDist, ival, currentIdx, currentIdx-1);

					if (currentY>0)
					{
						test(currentDist, ival, currentIdx, currentIdx-width-1);
						test(currentDist, ival, currentIdx, currentIdx-width);
					}
					if (currentY < height-1)
					{
						test(currentDist, ival, currentIdx, currentIdx+width-1);
						test(currentDist, ival, currentIdx, currentIdx+width);
					}	
					if (currentX<width-1)
					{
						test(currentDist, ival, currentIdx, currentIdx+1);
						if (currentY>0)
						{
							test(currentDist, ival, currentIdx, currentIdx-width+1);
						}
						if (currentY < height-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+width+1);
						}
					}
				}
				else
				{
					if (currentX<width-1)
					{
						test(currentDist, ival, currentIdx, currentIdx+1);
						if (currentY>0)
						{
							test(currentDist, ival, currentIdx, currentIdx-width+1);
							test(currentDist, ival, currentIdx, currentIdx-width);
						}
						if (currentY < height-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+width+1);
							test(currentDist, ival, currentIdx, currentIdx+width);
						}
					}
					else
					{
						if (currentY>0)
						{
							test(currentDist, ival, currentIdx, currentIdx-width);
						}
						if (currentY<height-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+width);
						}
					}
				}

				visited[currentIdx] = true;
				visitedLock.lock();
				visitedCondition.signalAll();
				visitedLock.unlock();
				//search for the next node to consider
				double minDist = Double.MAX_VALUE;
				int minIdx = -1;
				for (int cnt = 0; cnt<dist.length; cnt++)
				{
					if ((!visited[cnt]) && ((dist[cnt]<minDist) || minIdx==-1))
					{
						minDist = dist[cnt];
						minIdx = cnt;
					}
				}
				if (minIdx == -1)
				{
					stop = true;
				}
				else
				{
					currentIdx = minIdx;
					currentY = (int)(currentIdx/width);
					currentX = currentIdx - width*currentY;
				}
			}
			if (run)
			{
				distanceMapLock.lock();
				pathMap = prev;
				distanceMapLock.unlock();
				mapReady = true;
			}
			mapThreadListLock.lock();
			mapThreadList.remove(this);
			mapThreadListLock.unlock();
			//seq.beginUpdate();
			seq.overlayChanged(InteractiveDjikstraTracing.this);
		}

		public double dist(double i1, double i2)
		{
			//return Math.abs(i1-i2);
			//int cost = 0;
			//			if (i1 > i2)
			//				return 2;
			//			else
			//				return 1;
			//			return 1+Math.max(i1-i2, 0);
			return maxIntensity/(i2+alpha);
		}

		private void test(double currentDist, double refI, int currentIdx, int idx)
		{
			if (!visited[idx])
			{
				double d = currentDist + dist(refI, data[idx]);
				if (d < dist[idx])
				{
					dist[idx] = d;
					prev[idx] = currentIdx;
				}
			}
		}


		@Override
		public void stopThread()
		{
			run = false;
		}
	}

	class ComputeMapThreadToTop extends StoppableThread
	{
		int xInit;
		int yInit;

		double[] dist = null;
		int[] prev  = null;
		boolean[] visited = null;
		double[] data = null;

		boolean run = true;

		public ComputeMapThreadToTop(int xInit, int yInit)
		{
			this.xInit = xInit;
			this.yInit = yInit;
			dataSaveLock.lock();
			data = new double[dataSave.length];
			System.arraycopy(dataSave, 0, data, 0, data.length);
			dataSaveLock.lock();
		}


		@Override
		public void run()
		{	
			int width = seq.getWidth();
			int height = seq.getHeight();

			dist = new double[width*height];
			prev  =new int[width*height];
			visited = new boolean[width*height];
			for (int u = 0; u < dist.length; u++)
			{
				dist[u] = Double.MAX_VALUE;
				visited[u] = false;
			}
			int idxInit = yInit*width+xInit;

			dist[idxInit] = 0;

			int currentIdx = idxInit;
			int currentX = xInit;
			int currentY = yInit;

			boolean stop = false;

			while (!stop && run)
			{
				double ival = data[currentIdx];
				double currentDist = dist[currentIdx];

				if (currentY>yInit) //consider only pixels on the top
				{
					if (currentX>0)
						test(currentDist, ival, currentIdx, currentIdx-1);
					if(currentX<width-1)
						test(currentDist, ival, currentIdx, currentIdx+1);
					if (currentY <  height-1)
					{
						test(currentDist, ival, currentIdx, currentIdx+width);
						if (currentX>0)
							test(currentDist, ival, currentIdx, currentIdx+width-1);
						if(currentX<width-1)
							test(currentDist, ival, currentIdx, currentIdx+width+1);
					}
				}
				else if (currentY<yInit) //consider only pixels to the bottom
				{
					if (currentX>0)
						test(currentDist, ival, currentIdx, currentIdx-1);
					if(currentX<width-1)
						test(currentDist, ival, currentIdx, currentIdx+1);
					if (currentY > 0)
					{
						test(currentDist, ival, currentIdx, currentIdx-width);
						if (currentX>0)
							test(currentDist, ival, currentIdx, currentIdx-width-1);
						if(currentX<width-1)
							test(currentDist, ival, currentIdx, currentIdx-width+1);
					}
				}
				else //currentY = yInit -> 8 connexity
				{
					if (currentX>0)
					{
						test(currentDist, ival, currentIdx, currentIdx-1);

						if (currentY>0)
						{
							test(currentDist, ival, currentIdx, currentIdx-width-1);
							test(currentDist, ival, currentIdx, currentIdx-width);
						}
						if (currentY < height-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+width-1);
							test(currentDist, ival, currentIdx, currentIdx+width);
						}	
						if (currentX<width-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+1);
							if (currentY>0)
							{
								test(currentDist, ival, currentIdx, currentIdx-width+1);
							}
							if (currentY < height-1)
							{
								test(currentDist, ival, currentIdx, currentIdx+width+1);
							}
						}
					}
					else
					{
						if (currentX<width-1)
						{
							test(currentDist, ival, currentIdx, currentIdx+1);
							if (currentY>0)
							{
								test(currentDist, ival, currentIdx, currentIdx-width+1);
								test(currentDist, ival, currentIdx, currentIdx-width);
							}
							if (currentY < height-1)
							{
								test(currentDist, ival, currentIdx, currentIdx+width+1);
								test(currentDist, ival, currentIdx, currentIdx+width);
							}
						}
						else
						{
							if (currentY>0)
							{
								test(currentDist, ival, currentIdx, currentIdx-width);
							}
							if (currentY<height-1)
							{
								test(currentDist, ival, currentIdx, currentIdx+width);
							}
						}
					}
				}

				visited[currentIdx] = true;

				//search for the next node to consider
				double minDist = Double.MAX_VALUE;
				int minIdx = -1;
				for (int cnt = 0; cnt<dist.length; cnt++)
				{
					if ((!visited[cnt]) && ((dist[cnt]<minDist) || minIdx==-1))
					{
						minDist = dist[cnt];
						minIdx = cnt;
					}
				}
				if (minIdx == -1)
				{
					stop = true;
				}
				else
				{
					currentIdx = minIdx;
					currentY = (int)(currentIdx/width);
					currentX = currentIdx - width*currentY;
				}
			}
			if (run)
			{
				distanceMapLock.lock();
				pathMap = prev;
				distanceMapLock.unlock();
				mapReady = true;
			}
			mapThreadListLock.lock();
			mapThreadList.remove(this);
			mapThreadListLock.unlock();
			//seq.beginUpdate();
			seq.overlayChanged(InteractiveDjikstraTracing.this);
		}

		public double dist(double i1, double i2)
		{
			//return Math.abs(i1-i2);
			//int cost = 0;
			//			if (i1 > i2)
			//				return 2;
			//			else
			//				return 1;
			//			return 1+Math.max(i1-i2, 0);
			return maxIntensity/(i2+alpha);
		}

		private void test(double currentDist, double refI, int currentIdx, int idx)
		{
			if (!visited[idx])
			{
				double d = currentDist + dist(refI, data[idx]);
				if (d < dist[idx])
				{
					dist[idx] = d;
					prev[idx] = currentIdx;
				}
			}
		}
		@Override
		public void stopThread()
		{
			run = false;
		}
	}

	@Override
	public void paint(Graphics2D g2d, Sequence sequence, IcyCanvas canvas){
		if (isEnabled && optimalPath!=null)
		{
			g2d.setStroke(new BasicStroke(0.5f));
			g2d.setColor(Color.yellow);
			pathLock.lock();
			{
				for (int cnt = 0; cnt<optimalPath.length; cnt++)
				{
					Ellipse2D ell = new Ellipse2D.Float(((int)optimalPath[cnt][0])-0.25f, ((int)optimalPath[cnt][1])-0.25f, 0.5f, 0.5f);
					g2d.fill(ell);
					if(cnt>0)
					{
						Line2D.Float l = new Line2D.Float(((int)optimalPath[cnt][0]), ((int)optimalPath[cnt][1]),((int)optimalPath[cnt-1][0]), ((int)optimalPath[cnt-1][1]));
						g2d.draw(l);
					}
				}
			}
			pathLock.unlock();
		}
	}

	@Override
	public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {}

	@Override
	public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		if (isEnabled)
			switch (state)
			{
			case RESET:
			case SECOND_CLICKED:
				mapThreadListLock.lock();
				for (Thread t:mapThreadList)
					((StoppableThread)t).stopThread();
				mapThreadListLock.unlock();
				pathThreadListLock.lock();
				for (Thread t:pathThreadList)
					((StoppableThread)t).stopThread();
				pathThreadListLock.unlock();
				initLock.lock();
				int xi = (int)Math.round(imagePoint.getX());
				int yi = (int)Math.round(imagePoint.getY());
				if (xi>=0 && xi < width && yi>=0 && yi<height)
				{
					xInit = xi;
					yInit = yi;
					Thread thr = null;
					if(verticalPath)
						thr = new ComputeMapThreadToTop(xInit, yInit);
					else
						thr = new ComputeMapThread(xInit, yInit);
					initLock.unlock();
					mapReady = false;
					mapThreadListLock.lock();
					mapThreadList.add(thr);
					mapThreadListLock.unlock();
					thr.start();
					state = DrawingState.FIRST_CLICKED;
				}
				break;
			case FIRST_CLICKED:
				if (mapReady)
				{
					pathThreadListLock.lock();
					for (Thread t:pathThreadList)
						((StoppableThread)t).stopThread();
					pathThreadListLock.unlock();
					int xF = (int)Math.round(imagePoint.getX());
					int yF = (int)Math.round(imagePoint.getY());
					if (xF>=0 && xF<width && yF>=0 && yF < height)
					{
						xFinal = xF;
						yFinal = yF;
						initLock.lock();
						Thread thr = new ComputePathThread(xInit, yInit, xFinal, yFinal);
						initLock.unlock();
						pathThreadListLock.lock();
						pathThreadList.add(thr);
						pathThreadListLock.unlock();
						thr.start();
					}
				}
				else // wait the map to compute the pixel of interest
				{
					pathThreadListLock.lock();
					for (Thread t:pathThreadList)
						((StoppableThread)t).stopThread();
					pathThreadListLock.unlock();
					int xF = (int)Math.round(imagePoint.getX());
					int yF = (int)Math.round(imagePoint.getY());
					if (xF>=0 && xF<width && yF>=0 && yF < height)
					{
						xFinal = xF;
						yFinal = yF;
						initLock.lock();
						Thread thr = new ComputePathThreadWaiting(xInit, yInit, xFinal, yFinal);
						initLock.unlock();
						pathThreadListLock.lock();
						pathThreadList.add(thr);
						pathThreadListLock.unlock();
						thr.start();
					}
				}
				state = DrawingState.SECOND_CLICKED;
				break;
			}
	}
	@Override
	public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas)
	{
		if (isEnabled)
			if (state == DrawingState.FIRST_CLICKED)
			{
				if (mapReady)
				{
					pathThreadListLock.lock();
					for (Thread t:pathThreadList)
						((StoppableThread)t).stopThread();
					pathThreadListLock.unlock();
					int xF = (int)Math.round(imagePoint.getX());
					int yF = (int)Math.round(imagePoint.getY());
					if (xF>=0 && xF<width && yF>=0 && yF < height)
					{
						xFinal = xF;
						yFinal = yF;
						initLock.lock();
						Thread thr = new ComputePathThread(xInit, yInit, xFinal, yFinal);
						initLock.unlock();
						pathThreadListLock.lock();
						pathThreadList.add(thr);
						pathThreadListLock.unlock();
						thr.start();
					}
				}
				else // wait the map to compute the pixel of interest
				{
					pathThreadListLock.lock();
					for (Thread t:pathThreadList)
						((StoppableThread)t).stopThread();
					pathThreadListLock.unlock();
					int xF = (int)Math.round(imagePoint.getX());
					int yF = (int)Math.round(imagePoint.getY());
					if (xF>=0 && xF<width && yF>=0 && yF < height)
					{
						xFinal = xF;
						yFinal = yF;
						initLock.lock();
						Thread thr = new ComputePathThreadWaiting(xInit, yInit, xFinal, yFinal);
						initLock.unlock();
						pathThreadListLock.lock();
						pathThreadList.add(thr);
						pathThreadListLock.unlock();
						thr.start();
					}
				}
			}	
	}
	@Override
	public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {}

	@Override
	public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {}

	@Override
	public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {}

	public Number getIntensityAt(double x, double y) {
		if (dataSave!=null && x>=0 && x<width && y >=0 && y < height)
		{
			return dataSave[(int)Math.round(x)+(int)Math.round(y)*width];
		}
		else
			return -1;
	}

	class FireListenersThread extends Thread
	{
		double[][] path;
		PathEvent pathEvent;

		public FireListenersThread(final double[][] path, final PathEvent event)
		{
			runningThreads.add(this);
			this.path = path;
			this.pathEvent = event;
		}

		@Override
		public void run()
		{
			for (PathListener listener:pathListenerList)
				listener.refreshPath(pathEvent, InteractiveDjikstraTracing.this, path);
			runningThreads.remove(this);
		}
	}

	public double[][] getOptimalPathCopy() {
		if (optimalPath==null)
			return null;
		pathLock.lock();
		//clone optimalPath
		final double[][] pathClone = new double[optimalPath.length][];
		for (int i = 0; i<optimalPath.length; i++)
		{
			pathClone[i] = optimalPath[i].clone();
		}
		pathLock.unlock();
		return pathClone;
	}

	public void enable()
	{
		isEnabled = true;
		seq.overlayChanged(this);
	}

	public void disable()
	{
		isEnabled = false;
		seq.overlayChanged(this);
	}
}
