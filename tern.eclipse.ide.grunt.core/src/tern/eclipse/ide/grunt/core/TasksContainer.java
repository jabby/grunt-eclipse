package tern.eclipse.ide.grunt.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import tern.ITernFile;
import tern.angular.AngularType;
import tern.angular.modules.Directive;
import tern.angular.modules.DirectiveValue;
import tern.angular.modules.Module;
import tern.angular.protocol.completions.TernAngularCompletionsQuery;
import tern.eclipse.ide.core.IIDETernProject;
import tern.eclipse.ide.core.TernCorePlugin;
import tern.eclipse.ide.grunt.internal.core.GruntCoreMessages;
import tern.eclipse.ide.grunt.internal.core.Logger;
import tern.eclipse.ide.grunt.internal.core.TernGruntTaskQuery;
import tern.server.protocol.IJSONObjectHelper;
import tern.server.protocol.completions.ITernCompletionCollector;
import tern.server.protocol.completions.TernCompletionProposalRec;
import tern.utils.StringUtils;

public class TasksContainer implements IGruntNode {

	private final GruntProject gruntProject;
	private boolean refreshDirectives;
	private final Object lock = new Object();

	private final List<Task> tasks;

	public TasksContainer(GruntProject gruntProject) {
		this.gruntProject = gruntProject;
		this.tasks = new ArrayList<Task>();
		clear();
	}

	@Override
	public String getName() {
		return GruntCoreMessages.Tasks_label;
	}

	public void clear() {
		this.refreshDirectives = true;
		this.tasks.clear();
	}

	public Task[] getTasks() {
		refreshIfNeeded();
		return tasks.toArray(Task.EMPTY_TASK);
	}

	public boolean hasTasks() {
		refreshIfNeeded();
		return tasks.size() > 0;
	}

	protected void refreshIfNeeded() {
		try {
			if (!refreshDirectives) {
				return;
			}
			synchronized (lock) {
				if (!refreshDirectives) {
					return;
				}
				tasks.clear();
				IIDETernProject ternProject = gruntProject.getTernProject();

				TernGruntTaskQuery query = new TernGruntTaskQuery();
				IFile gruntFile = gruntProject.getGruntFile();
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
								tasks.add(new Task(proposal.name));							
							}
						});
				refreshDirectives = false;
			}
		} catch (Exception e) {
			Logger.logException(
					"Error while refresh grunt tasks.", e);
		}
	}
}
