package plugins.tinevez.pathtracing;

public interface PathListener
{ 
	public abstract void refreshPath( PathEvent pathEvent, Object source, double[][] path );
}
