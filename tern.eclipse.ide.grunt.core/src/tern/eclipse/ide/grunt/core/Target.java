package tern.eclipse.ide.grunt.core;

public class Target implements IGruntNode, IGruntNodeExecutable, IGruntNodeExecutableProvider {

	public static final Target[] EMPTY_TARGET = new Target[0];

	private final String name;
	private final Task parent;

	private final String executableName;

	public Target(String name, Task parent) {
		this.name = name;
		this.parent = parent;
		this.executableName = new StringBuilder(parent.getName()).append(":")
				.append(name).toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getExecutableName() {
		return executableName;
	}

	@Override
	public GruntProject getGruntProject() {
		return parent.getGruntProject();
	}
	
	@Override
	public IGruntNodeExecutable getExecutable() {
		return this;
	}
	
	@Override
	public GruntFile getGruntFile() {
		return parent.getGruntFile();
	}
}
