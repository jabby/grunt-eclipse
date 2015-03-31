package tern.eclipse.ide.grunt.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import tern.eclipse.ide.core.IIDETernProject;
import tern.eclipse.ide.core.TernCorePlugin;
import tern.eclipse.ide.grunt.internal.core.GruntCorePlugin;
import tern.eclipse.ide.grunt.internal.core.Logger;
import tern.server.ITernServer;
import tern.server.TernPlugin;
import tern.server.TernServerAdapter;

public class GruntProject implements IGruntNode {

	public static final GruntProject[] EMPTY_PROJECT = new GruntProject[0];
	public static final String GRUNT_FILE = "Gruntfile.js";

	private static final String GRUNT_PROJECT = GruntProject.class.getName();

	private final IIDETernProject ternProject;
	private final Collection<GruntFile> gruntFiles;

	GruntProject(IIDETernProject ternProject) throws CoreException {
		this.ternProject = ternProject;
		this.gruntFiles = new ArrayList<GruntFile>();
		ternProject.setData(GRUNT_PROJECT, this);
		ternProject.addServerListener(new TernServerAdapter() {
			@Override
			public void onStop(ITernServer server) {
				gruntFiles.clear();
			}
		});
	}

	/**
	 * Return true if the given project have Grunt nature and false otherwise.
	 * 
	 * @param project
	 *            Eclipse project.
	 * @return true if the given project have Grunt nature and false otherwise.
	 */
	public static boolean hasGruntNature(IProject project) {
		if (project.isAccessible()) {
			try {
				return (TernCorePlugin.hasTernNature(project) && TernCorePlugin
						.getTernProject(project).hasPlugin(TernPlugin.grunt));
			} catch (CoreException e) {
				Logger.logException("Error Grunt project", e);
			}
		}
		return false;
	}

	/**
	 * Returns the Grunt project from the given Eclipse project.
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 *             if the given project has not Grunt nature.
	 */
	public static GruntProject getGruntProject(IProject project)
			throws CoreException {
		if (!hasGruntNature(project)) {
			throw new CoreException(new Status(IStatus.ERROR,
					GruntCorePlugin.PLUGIN_ID, "The project "
							+ project.getName() + " is not a grunt project."));
		}
		IIDETernProject ternProject = TernCorePlugin.getTernProject(project);
		GruntProject gruntProject = ternProject.getData(GRUNT_PROJECT);
		if (gruntProject == null) {
			gruntProject = new GruntProject(ternProject);
		}
		return gruntProject;
	}

	public static GruntProject[] getGruntProjects() {
		List<GruntProject> gruntProjects = new ArrayList<GruntProject>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (IProject project : root.getProjects()) {
			if (GruntProject.hasGruntNature(project)) {
				try {
					gruntProjects.add(GruntProject.getGruntProject(project));
				} catch (CoreException e) {
					Logger.logException("Error while getting grunt ptoject", e);
				}
			}
		}
		return gruntProjects.toArray(GruntProject.EMPTY_PROJECT);
	}

	public GruntFile[] getGruntFiles() {
		return gruntFiles.toArray(GruntFile.EMPTY_FILE);
	}

	public GruntFile addGruntFile(IFile gruntFile) {
		GruntFile file = new GruntFile(gruntFile, this);
		addGruntFile(file);
		return file;
	}

	public void addGruntFile(GruntFile file) {
		gruntFiles.add(file);
	}

	public void removeGruntFile(GruntFile file) {
		gruntFiles.remove(file);
	}

	@Override
	public String getName() {
		return getTernProject().getProject().getName();
	}

	public IIDETernProject getTernProject() {
		return ternProject;
	}

	public String getDisplayName() {
		return getTernProject().getProject().getName();
	}

	public void clearFiles() {
		gruntFiles.clear();
	}

	public void clearTasks() {
		for (GruntFile file : gruntFiles) {
			file.clear();
		}
	}

	public static IFile getFileForLocation(String path) {
		if (path == null) {
			return null;
		}
		IPath filePath = new Path(path);
		IFile file = null;
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocation(filePath);
		if (files.length > 0) {
			return files[0];
		}
		return null;
	}

	public boolean hasGruntFile(IFile file) {
		for (GruntFile gruntFile : gruntFiles) {
			if (file.equals(gruntFile.getFile())) {
				return true;
			}
		}
		return false;
	}

}
