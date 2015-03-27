package tern.eclipse.ide.grunt.internal.ui.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.internal.ui.views.actions.RefreshExplorerAction;

public class GruntExplorerView extends ViewPart {

	private TreeViewer viewer;
	private RefreshExplorerAction refreshAction;

	@Override
	public void createPartControl(Composite parent) {
		FillLayout layout = new FillLayout();
		parent.setLayout(layout);

		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		viewer.setContentProvider(new GruntExplorerContentProvider());
		viewer.setLabelProvider(new GruntExplorerLabelProvider());

		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		viewer.expandAll();
		
		registerActions();
	}

	/**
	 * Refresh the tree of the view.
	 * 
	 * @param updateLabels
	 *            true if only labels of each tree item must be refreshed and
	 *            false otherwise.
	 */
	public void refreshTree(boolean updateLabels) {
		if (!updateLabels) {
			GruntProject[] projects = GruntProject.getGruntProjects();
			for (int i = 0; i < projects.length; i++) {
				projects[i].getTasksContainer().clear();
			}
		}
		Object[] expandedElements = viewer.getExpandedElements();
		TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();
		viewer.refresh(updateLabels);
		viewer.setExpandedElements(expandedElements);
		viewer.setExpandedTreePaths(expandedTreePaths);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}

	@Override
	public void setFocus() {

	}

	public void registerActions() {
		IToolBarManager manager = getViewSite().getActionBars()
				.getToolBarManager();
		this.refreshAction = new RefreshExplorerAction(this);
		manager.add(refreshAction);
	}

}
