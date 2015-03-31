package tern.eclipse.ide.grunt.internal.ui.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;

import tern.eclipse.ide.grunt.core.GruntProject;

/**
 * A drop adapter which adds files to the Grunt view.
 */
public class GruntViewDropAdapter extends DropTargetAdapter {

	private GruntView view;

	/**
	 * Creates a new drop adapter for the given Grunt view.
	 * 
	 * @param view
	 *            the view which dropped files will be added to
	 */
	public GruntViewDropAdapter(GruntView view) {
		this.view = view;
	}

	/**
	 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public void drop(DropTargetEvent event) {
		Object data = event.data;
		if (data instanceof String[]) {
			final String[] strings = (String[]) data;
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					for (int i = 0; i < strings.length; i++) {
						processString(strings[i]);
					}
				}
			});
		}
	}

	/**
	 * Attempts to process the given string as a path to an Gruntfile.js file.
	 * If the string is determined to be a path to a Gruntfile.js file in the
	 * workspace, that file is added to the Grunt view.
	 * 
	 * @param gruntFileName
	 *            the string to process
	 */
	private void processString(String gruntFileName) {
		System.err.println(gruntFileName);
		/*IFile gruntFile = GruntUtil.getFileForLocation(gruntFileName, null);
		if (!GruntUtil.isKnownGruntFile(gruntFile)) {
			return;
		}
		String name = gruntFile.getFullPath().toString();
		GruntProject[] existingProjects = view.getProjects();
		for (int j = 0; j < existingProjects.length; j++) {
			GruntProjectNodeProxy existingProject = (GruntProjectNodeProxy) existingProjects[j];
			if (existingProject.getBuildFileName().equals(name)) {
				// Don't parse projects that have already been added.
				return;
			}
		}
		GruntProjectNode project = new GruntProjectNodeProxy(name);
		view.addProject(project);*/
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragEnter(org.eclipse.swt.dnd.
	 * DropTargetEvent)
	 */
	public void dragEnter(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragEnter(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse
	 * .swt.dnd.DropTargetEvent)
	 */
	public void dragOperationChanged(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragOperationChanged(event);
	}
}