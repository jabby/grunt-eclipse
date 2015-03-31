/**
 *  Copyright (c) 2013-2014 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package tern.eclipse.ide.grunt.internal.ui.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import tern.eclipse.ide.grunt.internal.ui.GruntUIMessages;
import tern.eclipse.ide.grunt.internal.ui.views.GruntView;

/**
 * This action opens in an editor the selected element of the tree of the
 * angular explorer if the element can be opened.
 * 
 */
public class GoToDefinitionAction extends Action {

	private final GruntView explorer;

	public GoToDefinitionAction(GruntView explorer) {
		this.explorer = explorer;
		super.setText(GruntUIMessages.GoToDefinitionAction_text);
		super.setToolTipText(GruntUIMessages.GoToDefinitionAction_tooltip);
		// super.setImageDescriptor(ImageResource
		// .getImageDescriptor(ImageResource.IMG_ELCL_GOTO_DEF));
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) explorer
				.getViewer().getSelection();
		if (!selection.isEmpty()) {
			Object firstSelection = selection.getFirstElement();
			explorer.tryOpenInEditor(firstSelection);
		}
	}
}
