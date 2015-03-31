package tern.eclipse.ide.grunt.core;

import tern.server.protocol.definition.ITernDefinitionCollector;

public class Task implements IGruntNode {

	public static final Task[] EMPTY_TASK = new Task[0];

	private final TasksContainer parent;
	private final String name;

	public Task(String name, TasksContainer parent) {
		this.name = name;
		this.parent = parent;
	}

	@Override
	public String getName() {
		return name;
	}

	public TasksContainer getParent() {
		return parent;
	}

	public GruntProject getGruntProject() {
		return getParent().getGruntProject();
	}

}
