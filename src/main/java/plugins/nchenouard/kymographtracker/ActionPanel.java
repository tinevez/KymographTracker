package plugins.nchenouard.kymographtracker;

import icy.sequence.Sequence;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

abstract class ActionPanel extends JPanel
{
	private static final long serialVersionUID = -3905933291897435226L;
	String description;
	DefaultMutableTreeNode node;
	
	abstract void changeSelectedSequence(Sequence sequence);
}