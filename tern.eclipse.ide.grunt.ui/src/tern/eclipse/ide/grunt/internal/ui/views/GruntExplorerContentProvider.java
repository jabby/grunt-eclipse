package tern.eclipse.ide.grunt.internal.ui.views;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.TasksContainer;

public class GruntExplorerContentProvider implements ITreeContentProvider {

	private static final Object[] EMPTY_OBJECT = new Object[0];

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GruntProject) {
			return ((GruntProject) parentElement).getChildren();
		} else if (parentElement instanceof TasksContainer) {
			return ((TasksContainer) parentElement).getTasks();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof GruntProject) {
			return true;
		} else if (element instanceof TasksContainer) {
			return ((TasksContainer) element).hasTasks();
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			return GruntProject.getGruntProjects();
		}
		return EMPTY_OBJECT;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

}
