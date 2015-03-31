package tern.eclipse.ide.grunt.internal.ui.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import tern.ITernFile;
import tern.eclipse.ide.core.IIDETernProject;
import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.Task;
import tern.eclipse.ide.grunt.core.launch.GruntLaunchConfigurationDelegate;
import tern.eclipse.ide.grunt.core.query.TernGruntTaskQuery;
import tern.eclipse.ide.grunt.internal.ui.GruntUIMessages;
import tern.eclipse.ide.grunt.internal.ui.GruntUIPlugin;
import tern.eclipse.ide.grunt.internal.ui.Logger;
import tern.eclipse.ide.grunt.internal.ui.views.actions.GoToDefinitionAction;
import tern.eclipse.ide.grunt.internal.ui.views.actions.RefreshExplorerAction;
import tern.eclipse.ide.grunt.internal.ui.views.viewers.GruntContentProvider;
import tern.eclipse.ide.grunt.internal.ui.views.viewers.GruntLabelProvider;
import tern.eclipse.ide.ui.utils.EditorUtils;
import tern.server.protocol.definition.ITernDefinitionCollector;

public class GruntView extends ViewPart {

	private TreeViewer viewer;
	private GoToDefinitionAction openAction;
	private Action doubleClickAction;
	private IAction refreshAction;

	@Override
	public void createPartControl(Composite parent) {
		FillLayout layout = new FillLayout();
		parent.setLayout(layout);

		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		viewer.setContentProvider(new GruntContentProvider());
		viewer.setLabelProvider(new GruntLabelProvider());

		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		viewer.expandAll();

		registerActions();
		registerContextMenu();
		initializeDragAndDrop();
	}

	private void initializeDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance() };
		TreeViewer viewer = getViewer();
		GruntViewDropAdapter adapter = new GruntViewDropAdapter(this);
		viewer.addDropSupport(ops, transfers, adapter);
	}

	/**
	 * Refresh the tree of the view.
	 * 
	 * @param updateLabels
	 *            true if only labels of each tree item must be refreshed and
	 *            false otherwise.
	 */
	public void refreshTree(boolean updateLabels) {
		if (!updateLabels) {
			GruntProject[] projects = GruntProject.getGruntProjects();
			for (int i = 0; i < projects.length; i++) {
				projects[i].getTasksContainer().clear();
			}
		}
		Object[] expandedElements = viewer.getExpandedElements();
		TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();
		viewer.refresh(updateLabels);
		viewer.setExpandedElements(expandedElements);
		viewer.setExpandedTreePaths(expandedTreePaths);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}

	private void registerContextMenu() {
		MenuManager contextMenu = new MenuManager();
		contextMenu.add(openAction);
		contextMenu.add(refreshAction);

		Control control = this.viewer.getControl();
		Menu menu = contextMenu.createContextMenu(control);
		control.setMenu(menu);
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public void registerActions() {
		IToolBarManager manager = getViewSite().getActionBars()
				.getToolBarManager();
		this.refreshAction = new RefreshExplorerAction(this);
		manager.add(refreshAction);
		this.openAction = new GoToDefinitionAction(this);
		manager.add(openAction);
		this.doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				if (!selection.isEmpty()) {
					if (selection.getFirstElement() instanceof Task) {
						Task task = (Task) selection.getFirstElement();
						final ILaunchConfiguration conf = GruntLaunchConfigurationDelegate
								.getOrCreate(task);
						Job job = new Job("Launch grunt task") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									conf.launch("run", monitor, false, true);
								} catch (CoreException e) {
									return new Status(IStatus.ERROR,
											GruntUIPlugin.PLUGIN_ID,
											e.getMessage(), e);
								}
								return Status.OK_STATUS;
							}
						};
						job.setPriority(Job.BUILD);
						job.schedule();
					}
				}
				// GradleProject project = projectSelector.getProject();
				// if (project != null && !selection.isEmpty()) {
				// StringBuilder taskStr = new StringBuilder();
				// for (Object obj : selection.toArray()) {
				// if (obj instanceof Task) {
				// Task task = (Task) obj;
				// taskStr.append(displayProjectLocalTasks ? task.getPath() :
				// task.getName());
				// taskStr.append(' ');
				// }
				// }
				// if (taskStr.length() > 0) {
				// final ILaunchConfiguration conf =
				// GradleLaunchConfigurationDelegate.getOrCreate(project,
				// taskStr.toString());
				// JobUtil.schedule(NO_RULE, new
				// GradleRunnable(project.getDisplayName() + " " +
				// taskStr.toString()) {
				// @Override
				// public void doit(IProgressMonitor mon) throws Exception {
				// conf.launch("run", mon, false, true);
				// }
				// });
				// }
				// }
			}
		};
		doubleClickAction.setDescription("Run Task");
		doubleClickAction.setToolTipText("Run a task");
		// doubleClickAction.setImageDescriptor(GradleUI.getDefault().getImageRegistry().getDescriptor(GradleUI.IMAGE_RUN_TASK));
		manager.add(doubleClickAction);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	public void tryOpenInEditor(Object selection) {
		if (selection instanceof Task) {
			openInEditor(((Task) selection));
		}
		// if (firstSelection instanceof ITernScriptPath) {
		// ITernScriptPath scriptPath = (ITernScriptPath) firstSelection;
		// ITernFile file = (ITernFile) scriptPath.getAdapter(ITernFile.class);
		// if (file != null) {
		// tryToOpenFile(file, file.getFullName(currentTernProject), null,
		// null);
		// }
		// } else if (firstSelection instanceof ITernScriptResource) {
		// ITernScriptResource scriptResource = (ITernScriptResource)
		// firstSelection;
		// ITernFile file = scriptResource.getFile();
		// if (file != null) {
		// tryToOpenFile(file, file.getFullName(currentTernProject), null,
		// null);
		// }
		// } else if (firstSelection instanceof IDefinitionAware) {
		// ((IDefinitionAware) firstSelection)
		// .findDefinition(GruntExplorerView.this);
		// }
	}

	private void openInEditor(Task task) {
		GruntProject gruntProject = task.getParent().getGruntProject();
		final IIDETernProject ternProject = gruntProject.getTernProject();
		TernGruntTaskQuery query = new TernGruntTaskQuery(task.getName());
		IFile gruntFile = gruntProject.getGruntFile();
		if (!gruntFile.exists()) {
			return;
		}
		ITernFile ternFile = ternProject.getFile(gruntFile);
		query.setFile(ternFile.getFileName());
		try {
			ternProject.request(query, ternFile,
					new ITernDefinitionCollector() {

						@Override
						public void setDefinition(String filename, Long start,
								Long end) {
							tryToOpenFile(filename, start, end, ternProject);
						}
					});
		} catch (Exception e) {
			Logger.logException(e);
		}
	}

	/**
	 * Try to open file.
	 * 
	 * @param file
	 */
	// private void tryToOpenFile(ITernFile file, String filename, Long start,
	// Long end) {
	// IStatus status = openFile(file, filename, start, end);
	// if (!status.isOK()) {
	// ErrorDialog.openError(getSite().getShell(),
	// GruntUIMessages.GruntExplorerView_openFileDialog_title,
	// status.getMessage(), status);
	// }
	// }

	/**
	 * Try to open file.
	 * 
	 * @param ternProject
	 * 
	 * @param file
	 */
	private void tryToOpenFile(String filename, Long start, Long end,
			IIDETernProject ternProject) {
		IStatus status = openFile(filename, start, end, ternProject);
		if (!status.isOK()) {
			ErrorDialog.openError(getSite().getShell(),
					GruntUIMessages.GruntView_openFileDialog_title,
					status.getMessage(), status);
		}
	}

	/**
	 * Open the file in an editor if file exists.
	 * 
	 * @param ternProject
	 * 
	 * @param file
	 */
	private IStatus openFile(String filename, Long start, Long end,
			IIDETernProject ternProject) {
		ITernFile tFile = ternProject.getFile(filename);
		IFile file = (IFile) tFile.getAdapter(IFile.class);
		if (file == null) {
			return new Status(IStatus.ERROR, GruntUIPlugin.PLUGIN_ID, NLS.bind(
					GruntUIMessages.GruntView_openFile_error,
					filename != null ? filename : "null"));
		}
		if (!file.exists()) {
			return new Status(IStatus.ERROR, GruntUIPlugin.PLUGIN_ID, NLS.bind(
					GruntUIMessages.GruntView_openFile_error,
					file.getFullPath()));
		}
		EditorUtils.openInEditor(file, start != null ? start.intValue() : 0,
				end != null ? end.intValue() - start.intValue() : 0, true);
		return Status.OK_STATUS;
	}

}
