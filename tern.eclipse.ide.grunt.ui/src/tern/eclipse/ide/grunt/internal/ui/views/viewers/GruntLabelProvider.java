/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tern.eclipse.ide.grunt.internal.ui.views.viewers;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import tern.eclipse.ide.grunt.core.GruntFile;
import tern.eclipse.ide.grunt.core.IGruntNode;
import tern.eclipse.ide.grunt.core.Target;
import tern.eclipse.ide.grunt.core.Task;
import tern.eclipse.ide.grunt.internal.ui.ImageResource;

public class GruntLabelProvider extends StyledCellLabelProvider implements
		IColorProvider, IStyledLabelProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof GruntFile) {
			return ImageResource.getImage(ImageResource.IMG_GRUNTFILE);
		} else if (element instanceof Task) {
			Task task = (Task) element;
			return task.isDefault() ? ImageResource
					.getImage(ImageResource.IMG_TASK_DEFAULT) : ImageResource
					.getImage(ImageResource.IMG_TASK);
		} else if (element instanceof Target) {
			return ImageResource.getImage(ImageResource.IMG_TARGET);
		}
		return null;
	}

	@Override
	public void update(ViewerCell cell) {
		Object obj = cell.getElement();
		StyledString str = getStyledText(obj);
		cell.setText(str.toString());
		cell.setStyleRanges(str.getStyleRanges());
		cell.setImage(getImage(obj));
	}

	@Override
	public Color getForeground(Object node) {
		if (node instanceof Task && ((Task) node).isDefault()) {
			return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		}
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof GruntFile) {
			GruntFile node = (GruntFile) element;
			StyledString buff = new StyledString(node.getName());
			IFile gruntFile = node.getGruntFileResource();
			if (gruntFile != null) {
				buff.append("  "); //$NON-NLS-1$
				buff.append('[', StyledString.DECORATIONS_STYLER);
				buff.append(gruntFile.getFullPath().makeRelative().toString(),
						StyledString.DECORATIONS_STYLER);
				buff.append(']', StyledString.DECORATIONS_STYLER);
			}
			return buff;
		} else if (element instanceof IGruntNode) {
			return new StyledString(((IGruntNode) element).getName());
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}
}
