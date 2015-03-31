package tern.eclipse.ide.grunt.core;

import org.eclipse.core.resources.IFile;

public class GruntFile {

	private final IFile file;

	public GruntFile(IFile file) {
		this.file = file;
	}

	public IFile getFile() {
		return file;
	}
}
