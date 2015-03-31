package tern.eclipse.ide.grunt.internal.ui.views.viewers;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.IGruntNode;
import tern.eclipse.ide.grunt.core.Task;
import tern.eclipse.ide.grunt.internal.ui.ImageResource;

public class GruntLabelProvider extends LabelProvider {

	private final static WorkbenchLabelProvider WORKBENCH_LABEL_PROVIDER = new WorkbenchLabelProvider();

	@Override
	public String getText(Object element) {
		if (element instanceof IGruntNode) {
			return ((IGruntNode) element).getName();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof GruntProject) {
			return WORKBENCH_LABEL_PROVIDER.getImage(((GruntProject) element)
					.getTernProject().getProject());
		} else if (element instanceof Task) {
			return ImageResource.getImage(ImageResource.IMG_TASK);
		}
		return super.getImage(element);
	}
}
