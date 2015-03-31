package tern.eclipse.ide.grunt.internal.ui.views.viewers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.internal.views.markers.TasksView;

import tern.eclipse.ide.grunt.core.GruntFile;
import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.Task;

public class GruntContentProvider implements ITreeContentProvider {

	private static final Object[] EMPTY_OBJECT = new Object[0];

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GruntProject) {
			return ((GruntProject) parentElement).getGruntFiles();
		} else if (parentElement instanceof GruntFile) {
			return ((GruntFile) parentElement).getTasks();
		} else if (parentElement instanceof Task) {
			return ((Task) parentElement).getTargets();
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
		} else if (element instanceof GruntFile) {
			return ((GruntFile) element).hasTasks();
		} else if (element instanceof Task) {
			return ((Task) element).hasTargets();
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			Collection<GruntFile> files = new ArrayList<GruntFile>();
			GruntProject[] projects = GruntProject.getGruntProjects();
			for (int i = 0; i < projects.length; i++) {
				files.addAll(Arrays.asList(projects[i].getGruntFiles()));
			}
			return files.toArray(GruntFile.EMPTY_FILE);
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
