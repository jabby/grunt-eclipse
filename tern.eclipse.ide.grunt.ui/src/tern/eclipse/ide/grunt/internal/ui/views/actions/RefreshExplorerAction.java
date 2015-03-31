/**
 *  Copyright (c) 2015 Angelo ZERR.
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

import tern.eclipse.ide.grunt.internal.ui.GruntUIMessages;
import tern.eclipse.ide.grunt.internal.ui.ImageResource;
import tern.eclipse.ide.grunt.internal.ui.views.GruntView;

/**
 * This action refresh the explorer tree.
 * 
 */
public class RefreshExplorerAction extends Action {

	private final GruntView explorer;

	public RefreshExplorerAction(GruntView explorer) {
		this.explorer = explorer;
		super.setText(GruntUIMessages.RefreshExplorerAction_text);
		super.setToolTipText(GruntUIMessages.RefreshExplorerAction_tooltip);
		super.setImageDescriptor(ImageResource
				.getImageDescriptor(ImageResource.IMG_ELCL_REFRESH));
	}

	@Override
	public void run() {
		explorer.refreshTree(false);
	}

}
