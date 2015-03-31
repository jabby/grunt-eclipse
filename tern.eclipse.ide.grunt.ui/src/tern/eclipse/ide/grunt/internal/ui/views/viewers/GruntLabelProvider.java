package tern.eclipse.ide.grunt.internal.ui.views.viewers;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import tern.eclipse.ide.grunt.core.GruntFile;
import tern.eclipse.ide.grunt.core.IGruntNode;
import tern.eclipse.ide.grunt.core.Target;
import tern.eclipse.ide.grunt.core.Task;
import tern.eclipse.ide.grunt.internal.ui.ImageResource;

public class GruntLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof IGruntNode) {
			return ((IGruntNode) element).getName();
		}
		return super.getText(element);
	}

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
		return super.getImage(element);
	}
}
