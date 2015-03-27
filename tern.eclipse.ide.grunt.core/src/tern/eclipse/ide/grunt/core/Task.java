package tern.eclipse.ide.grunt.core;

public class Task implements IGruntNode {

	public static final Task[] EMPTY_TASK = new Task[0];
	private final String name;

	public Task(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
