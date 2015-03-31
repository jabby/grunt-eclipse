package tern.eclipse.ide.grunt.core;

public interface IGruntNodeExecutable extends IGruntNode {

	GruntProject getGruntProject();

	GruntFile getGruntFile();

	String getExecutableName();

}
