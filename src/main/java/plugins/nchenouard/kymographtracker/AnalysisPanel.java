package plugins.nchenouard.kymographtracker;

import icy.sequence.Sequence;

import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

class AnalysisPanel extends ActionPanel
{
	private static final long serialVersionUID = -8078104374329237756L;

	public AnalysisPanel() {
		description = "Advanced results analysis";
		node = new DefaultMutableTreeNode(description);
		this.add(new JLabel(description));
		this.setBorder(new TitledBorder(description));
	}
	
	@Override
	protected void changeSelectedSequence(Sequence sequence)
	{
		
	}
}