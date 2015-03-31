package tern.eclipse.ide.grunt.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import tern.ITernFile;
import tern.eclipse.ide.core.IIDETernProject;
import tern.eclipse.ide.grunt.core.query.TernGruntTasksQuery;
import tern.eclipse.ide.grunt.internal.core.Logger;
import tern.server.protocol.IJSONObjectHelper;
import tern.server.protocol.completions.ITernCompletionCollector;
import tern.server.protocol.completions.TernCompletionProposalRec;

public class GruntFile implements IGruntNode, IGruntNodeExecutableProvider {

	public static final GruntFile[] EMPTY_FILE = new GruntFile[0];
	private final IFile gruntFile;
	private final GruntProject gruntProject;
	private boolean refreshTasks;
	private final Object lock = new Object();

	private final Map<String, Task> tasks;

	public GruntFile(IFile gruntFile, GruntProject gruntProject) {
		this.gruntFile = gruntFile;
		this.gruntProject = gruntProject;
		this.tasks = new LinkedHashMap<String, Task>();
		clear();
	}

	@Override
	public String getName() {
		return gruntFile.getName();
	}

	public void clear() {
		this.refreshTasks = true;
		this.tasks.clear();
	}

	public Task[] getTasks() {
		refreshIfNeeded();
		return tasks.values().toArray(Task.EMPTY_TASK);
	}

	public boolean hasTasks() {
		refreshIfNeeded();
		return tasks.size() > 0;
	}

	protected void refreshIfNeeded() {
		try {
			if (!refreshTasks) {
				return;
			}
			synchronized (lock) {
				if (!refreshTasks) {
					return;
				}
				tasks.clear();
				IIDETernProject ternProject = gruntProject.getTernProject();

				TernGruntTasksQuery query = new TernGruntTasksQuery();
				if (!gruntFile.exists()) {
					return;
				}
				ITernFile ternFile = ternProject.getFile(gruntFile);
				query.setFile(ternFile.getFileName());
				ternProject.request(query, ternFile,
						new ITernCompletionCollector() {

							@Override
							public void addProposal(
									TernCompletionProposalRec proposal,
									Object completion,
									IJSONObjectHelper jsonObjectHelper) {
								Task task = addTask(proposal.name);
								Iterable<Object> targets = jsonObjectHelper
										.getList(completion, "targets");
								if (targets != null) {
									for (Object target : targets) {
										String targetName = jsonObjectHelper
												.getText(target, "name");
										task.addTarget(targetName);
									}
								}
							}
						});
				refreshTasks = false;
			}
		} catch (Exception e) {
			Logger.logException("Error while refresh grunt tasks.", e);
		}
	}

	public Task addTask(String name) {
		Task task = new Task(name, this);
		this.tasks.put(task.getName(), task);
		return task;
	}

	public GruntProject getGruntProject() {
		return gruntProject;
	}

	public IFile getGruntFileResource() {
		return gruntFile;
	}

	public String getGruntFileName() {
		return gruntFile.getFullPath().toOSString();
	}

	@Override
	public IGruntNodeExecutable getExecutable() {
		return getTask("default");
	}

	public Task getTask(String name) {
		return tasks.get(name);
	}
}
