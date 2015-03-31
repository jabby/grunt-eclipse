package tern.eclipse.ide.grunt.core.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;

import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.Task;
import tern.eclipse.ide.grunt.internal.core.Logger;

public class GruntLaunchConfigurationDelegate extends
		LaunchConfigurationDelegate {

	public static final String ID = "tern.eclipse.ide.grunt.core.launch";

	private static final String TASK_LIST = ID + ".TASKLIST";
	private static final String TASK_TEXT = ID + ".TASKTEXT";
	private static final List<String> DEFAULT_TASK_LIST = Arrays
			.asList(new String[0]);
	private static final String DEFAULT_TASK_TEXT = "";
	private static final boolean DEFAULT_ENABLE_WORKSPACE_REFRESH = true;

	/*
	 * Of the two properties PROJECT and PROJECT_LOCATION at least one of them
	 * must be set to identify a Gradle project. If both are set, the PROJECT
	 * property is used and the other one is ignored.
	 */
	private static final String PROJECT = ID + ".PROJECT";

	// public static final String PROJECT_LOCATION = ID + ".LOCATION";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		List<String> cmdLine = new ArrayList<String>();
		GruntProject gruntProject = getProject(configuration);
		String taskName = getTasks(configuration);

		String file = gruntProject.getGruntFile().getLocation().toOSString();
		String filePath = file; // ResourcesPlugin.getWorkspace().getRoot()
		// .findMember(file).getLocation().toOSString();
		// path is relative, so can not found it.
		// cmdLine.add(filePath);

		cmdLine.add("node");
		// cmdLine.add("node_modules/grunt/lib/grunt");cmdLine.add("grunt");
		cmdLine.add("node_modules/grunt-cli/bin/grunt");
		cmdLine.add("--no-color");
		// cmdLine.add("--force");
		cmdLine.add(taskName);
		String[] cmds = {};
		cmds = cmdLine.toArray(cmds);

		File workingPath = null;
		String workingDirectory = configuration.getAttribute(
				Constants.ATTR_WORKING_DIRECTORY, "");
		if (workingDirectory.length() > 0) {
			// workingDirectory = VariablesUtil.resolveValue(workingDirectory);
			if (workingDirectory != null) {
				workingPath = new File(workingDirectory);
			}
		}
		if (workingPath == null) {
			workingPath = (new File(filePath)).getParentFile();
		}

		String[] envp = getEnvironmentVariables(configuration);
		Process p = DebugPlugin.exec(cmds, workingPath, envp);
		// no way to get private p.handle from java.lang.ProcessImpl
		RuntimeProcess process = (RuntimeProcess) DebugPlugin.newProcess(
				launch, p, "Grunt process");
	}

	private String[] getEnvironmentVariables(ILaunchConfiguration configuration)
			throws CoreException {
		return null;
	}

	public static ILaunchConfiguration getOrCreate(Task task) {
		return getOrCreate(task.getGruntProject(), task.getName());
	}

	public static ILaunchConfiguration getOrCreate(GruntProject project,
			String task) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = getLaunchConfigurationType();
		if (type != null) {
			try {
				ILaunchConfiguration[] configs = manager
						.getLaunchConfigurations(type);
				for (ILaunchConfiguration conf : configs) {
					GruntProject confProject = GruntLaunchConfigurationDelegate
							.getProject(conf);
					if (confProject == project) {
						List<String> tasks = GruntLaunchConfigurationDelegate
								.getTasksList(conf);
						if (tasks.size() == 1 && tasks.get(0).equals(task)) {
							return conf;
						}
					}
				}
			} catch (CoreException e) {
				Logger.logException(e);
			}
		}
		return createDefault(project, task, true);
	}

	/**
	 * Returns the gradle project associated with this launch configuration.
	 * This returns null if no project is associated with the configuration.
	 * 
	 * @return GruntProject associated with this launch configuration.
	 */
	public static GruntProject getProject(ILaunchConfiguration conf) {
		try {
			String projectName = conf.getAttribute(PROJECT, (String) null);
			if (projectName != null) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				if (project.exists()) {
					return GruntProject.getGruntProject(project);
				}
			}
			/*
			 * else { String projectLocString =
			 * conf.getAttribute(PROJECT_LOCATION, (String) null); if
			 * (projectLocString != null) { File projectLoc = new
			 * File(projectLocString); if (projectLoc.isDirectory()) { return
			 * GruntCore.create(projectLoc); } } }
			 */
		} catch (Exception e) {
			Logger.logException(e);
		}
		return null;
	}

	/**
	 * Sets the project associated with the given launch configuration. If
	 * project is null, or if the project doesn't
	 * 
	 * @param conf
	 * @param project
	 */
	public static void setProject(ILaunchConfigurationWorkingCopy conf,
			GruntProject project) {
		if (project == null) {
			conf.removeAttribute(PROJECT);
			// conf.removeAttribute(PROJECT_LOCATION);
		} else {
			String name = project.getName();
			if (name != null) {
				conf.setAttribute(PROJECT, project.getName());
				// conf.removeAttribute(PROJECT_LOCATION);
			} else {
				// Only use location if name is not known (typically this only
				// happens if the project hasn't been imported
				// into eclipse (yet).
				// conf.removeAttribute(PROJECT);
				// String location = project.getLocation().getAbsolutePath();
				// conf.setAttribute(PROJECT_LOCATION, location);
			}
		}
	}

	/**
	 * @return list of Grunt path strings identifying a set/list of tasks that
	 *         are selected for execution (by the user, doesn't include taks
	 *         executed automatically because of dependencies).
	 */
	@SuppressWarnings("unchecked")
	public static String getTasks(ILaunchConfiguration conf) {
		String tasksText = DEFAULT_TASK_TEXT;
		try {
			tasksText = conf.getAttribute(TASK_TEXT, DEFAULT_TASK_TEXT);
		} catch (CoreException e) {
			Logger.logException(e);
		}
		if (tasksText.isEmpty()) {
			try {
				StringBuilder sb = new StringBuilder();
				for (String task : (List<String>) conf.getAttribute(TASK_LIST,
						DEFAULT_TASK_LIST)) {
					sb.append(task);
					// sb.append('\n');
				}
				tasksText = sb.toString();
			} catch (CoreException e) {
				Logger.logException(e);
			}
		}
		return tasksText;
	}

	public static void setTasks(ILaunchConfigurationWorkingCopy conf,
			List<String> checked) {
		StringBuilder sb = new StringBuilder();
		for (String task : checked) {
			sb.append(task);
			// sb.append('\n');
		}
		conf.setAttribute(TASK_TEXT, sb.toString());
	}

	@SuppressWarnings("unchecked")
	public static List<String> getTasksList(ILaunchConfiguration conf) {
		String tasksText = DEFAULT_TASK_TEXT;
		try {
			tasksText = conf.getAttribute(TASK_TEXT, DEFAULT_TASK_TEXT);
		} catch (CoreException e) {
			Logger.logException(e);
		}
		if (tasksText.isEmpty()) {
			try {
				return conf.getAttribute(TASK_LIST, DEFAULT_TASK_LIST);
			} catch (CoreException e) {
				Logger.logException(e);
				return DEFAULT_TASK_LIST;
			}
		} else {
			return parseTasks(tasksText);
		}
	}

	private static List<String> parseTasks(String tasksText) {
		List<String> tasks = new ArrayList<String>();
		Matcher matcher = Pattern.compile("\\S+").matcher(tasksText); //$NON-NLS-1$
		while (matcher.find()) {
			tasks.add(matcher.group());
		}
		return tasks;
	}

	public static void setTasks(ILaunchConfigurationWorkingCopy conf,
			String tasksText) {
		conf.removeAttribute(TASK_LIST);
		conf.setAttribute(TASK_TEXT, tasksText);
	}

	/**
	 * Creates a Grunt launch configuration for a given gradle project with
	 * default values. This launch configuration will be a working copy (i.e.
	 * not saved) if the save parameter is false.
	 * <p>
	 * If the save parameter is true, then an attempt will be made to save the
	 * launch configuration before this method returns and a reference to the
	 * saved configuration will be returned instead of the working copy.
	 */
	public static ILaunchConfiguration createDefault(GruntProject project,
			boolean save) {
		ILaunchConfigurationWorkingCopy conf = null;
		try {
			conf = createDefault(project,
					LaunchUtil.generateConfigName(project.getDisplayName()));
			if (save) {
				return conf.doSave();
			}
		} catch (CoreException e) {
			Logger.logException(e);
		}
		return conf;
	}

	/**
	 * Creates a new launch configuration with a given name, for a give project.
	 * The configuration will be an empty 'base-line' configuration with no
	 * preselected tasks to execute.
	 * <p>
	 * Beware that the name passed in to this method must meet certain
	 * restrictions (e.g. it should not be the name of an existing
	 * configuration, and should not contain certain 'funny' characters that may
	 * interfere with the OS file system. A safe way to create a name that meets
	 * the requirements is to use the {@link LaunchUtil}.generateConfigName()
	 * method.
	 */
	public static ILaunchConfigurationWorkingCopy createDefault(
			GruntProject project, String name) throws CoreException {
		ILaunchConfigurationType lcType = getLaunchConfigurationType();
		ILaunchConfigurationWorkingCopy conf = lcType.newInstance(null, name);
		setProject(conf, project);
		enableWorkspaceRefresh(conf, DEFAULT_ENABLE_WORKSPACE_REFRESH);
		return conf;
	}

	private static void enableWorkspaceRefresh(
			ILaunchConfigurationWorkingCopy conf, boolean enable) {
		if (enable) {
			conf.setAttribute(RefreshUtil.ATTR_REFRESH_SCOPE,
					RefreshUtil.MEMENTO_WORKSPACE);
		} else {
			conf.removeAttribute(RefreshUtil.ATTR_REFRESH_SCOPE);
		}
	}

	/**
	 * Creates a Grunt launch configuration for a given gradle project with
	 * default values. This launch configuration will be a working copy (i.e.
	 * not saved) if the save parameter is false.
	 * <p>
	 * If the save parameter is true, then an attempt will be made to save the
	 * launch configuration before this method returns and a reference to the
	 * saved configuration will be returned instead of the working copy.
	 */
	public static ILaunchConfiguration createDefault(GruntProject project,
			String task, boolean save) {
		ILaunchConfigurationWorkingCopy conf = null;
		try {
			conf = createDefault(
					project,
					LaunchUtil.generateConfigName(project.getName() + " "
							+ task));
			setTasks(conf, parseTasks(task));
			if (save) {
				return conf.doSave();
			}
		} catch (CoreException e) {
			Logger.logException(e);
		}
		return conf;
	}

	private static ILaunchConfigurationType getLaunchConfigurationType() {
		return LaunchUtil.getLaunchManager().getLaunchConfigurationType(ID);
	}

}
