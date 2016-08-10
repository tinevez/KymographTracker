package plugins.nchenouard.pathtracing;

public interface PathListener
{ 
	public abstract void refreshPath( PathEvent pathEvent, InteractiveMultipleDjikstraTracingESC interactiveMultipleDjikstraTracingESC, double[][] path);
}
