package tern.eclipse.ide.grunt.core.query;

import tern.server.protocol.TernQuery;

public class TernGruntTaskQuery extends TernQuery {

	public TernGruntTaskQuery(String name) {
		super("grunt-task");
		super.add("name", name);
	}

}
