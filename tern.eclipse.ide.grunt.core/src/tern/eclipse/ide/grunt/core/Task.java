package tern.eclipse.ide.grunt.core;

import java.util.ArrayList;
import java.util.List;

public class Task implements IGruntNode, IGruntNodeExecutable,
		IGruntNodeExecutableProvider {

	public static final Task[] EMPTY_TASK = new Task[0];

	private final GruntFile parent;
	private final String name;
	private final List<Target> targets;

	public Task(String name, GruntFile parent) {
		this.name = name;
		this.parent = parent;
		this.targets = new ArrayList<Target>();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getExecutableName() {
		return getName();
	}

	@Override
	public GruntFile getGruntFile() {
		return parent;
	}

	public GruntProject getGruntProject() {
		return getGruntFile().getGruntProject();
	}

	public boolean isDefault() {
		return "default".equals(getName());
	}

	public Target addTarget(String name) {
		Target target = new Target(name, this);
		targets.add(target);
		return target;
	}

	public Target[] getTargets() {
		return targets.toArray(Target.EMPTY_TARGET);
	}

	public boolean hasTargets() {
		return !targets.isEmpty();
	}

	@Override
	public IGruntNodeExecutable getExecutable() {
		return this;
	}
}
