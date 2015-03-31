package tern.eclipse.ide.grunt.internal.ui.views;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

import tern.ITernFile;
import tern.eclipse.ide.core.IIDETernProject;
import tern.eclipse.ide.grunt.core.GruntFile;
import tern.eclipse.ide.grunt.core.GruntProject;
import tern.eclipse.ide.grunt.core.IGruntNode;
import tern.eclipse.ide.grunt.core.IGruntNodeExecutable;
import tern.eclipse.ide.grunt.core.IGruntNodeExecutableProvider;
import tern.eclipse.ide.grunt.core.Target;
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

public class GruntView extends ViewPart implements IResourceChangeListener {

	private static final String MEMENTO_DIALOG_NAME = "memento";

	/**
	 * XML tag used to identify a grunt file in storage
	 */
	private static final String TAG_GRUNT_FILE = "gruntFile"; //$NON-NLS-1$

	/**
	 * XML key used to store an grunt file's path
	 */
	private static final String KEY_PATH = "path"; //$NON-NLS-1$

	private TreeViewer viewer;
	private GoToDefinitionAction openAction;
	private Action doubleClickAction;
	private IAction refreshAction;

	@Override
	public void createPartControl(Composite parent) {
		FillLayout layout = new FillLayout();
		parent.setLayout(layout);

		createGruntFilesViewer(parent);

		registerActions();
		registerContextMenu();
		initializeDragAndDrop();
	}

	protected void createGruntFilesViewer(Composite parent) {
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		viewer.setContentProvider(new GruntContentProvider());
		viewer.setLabelProvider(new GruntLabelProvider());

		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged((IStructuredSelection) event
						.getSelection());
			}
		});
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		GruntProject[] projects = GruntProject.getGruntProjects();
		for (int i = 0; i < projects.length; i++) {
			projects[i].clearFiles();
		}
		String persistedMemento = GruntUIPlugin.getDefault()
				.getDialogSettingsSection(getClass().getName())
				.get(MEMENTO_DIALOG_NAME); //$NON-NLS-1$
		if (persistedMemento != null) {
			try {
				memento = XMLMemento.createReadRoot(new StringReader(
						persistedMemento));
			} catch (WorkbenchException e) {
				// don't do anything. Simply don't restore the settings
			}
		}
		if (memento != null) {
			restoreViewerInput(memento);
			/*
			 * IMemento child = memento.getChild(TAG_FILTER_INTERNAL_TARGETS);
			 * if (child != null) { filterInternalTargets =
			 * Boolean.valueOf(child.getString(KEY_VALUE)).booleanValue(); }
			 */
		}
	}

	/**
	 * Restore the viewer content that was persisted
	 * 
	 * @param memento
	 *            the memento containing the persisted viewer content
	 */
	private void restoreViewerInput(IMemento memento) {
		IMemento[] projects = memento.getChildren(TAG_GRUNT_FILE);
		if (projects.length < 1) {
			return;
		}
		for (int i = 0; i < projects.length; i++) {
			IMemento projectMemento = projects[i];
			String pathString = projectMemento.getString(KEY_PATH);
			IFile gruntFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(pathString));
			if (!gruntFile.exists()) {
				// If the file no longer exists, don't add it.
				continue;
			}
			IProject project = gruntFile.getProject();
			if (GruntProject.hasGruntNature(project)) {
				try {
					GruntProject.getGruntProject(gruntFile.getProject())
							.addGruntFile(gruntFile);
				} catch (CoreException e) {
					Logger.logException(e);
				}
			}
			/*
			 * String nameString = projectMemento.getString(KEY_NAME); String
			 * defaultTarget = projectMemento.getString(KEY_DEFAULT); String
			 * errorString = projectMemento.getString(KEY_ERROR); String
			 * warningString = projectMemento.getString(KEY_WARNING);
			 * 
			 * AntProjectNodeProxy project = null; if (nameString == null) {
			 * nameString = IAntCoreConstants.EMPTY_STRING; } project = new
			 * AntProjectNodeProxy(nameString, pathString); if (errorString !=
			 * null && Boolean.valueOf(errorString).booleanValue()) {
			 * project.setProblemSeverity(AntModelProblem.SEVERITY_ERROR); }
			 * else if (warningString != null &&
			 * Boolean.valueOf(warningString).booleanValue()) {
			 * project.setProblemSeverity(AntModelProblem.SEVERITY_WARNING); }
			 * if (defaultTarget != null) {
			 * project.setDefaultTargetName(defaultTarget); }
			 * fInput.add(project);
			 */
		}
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
				projects[i].clearTasks();
			}
			viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			return;
		}
		Object[] expandedElements = viewer.getExpandedElements();
		TreePath[] expandedTreePaths = viewer.getExpandedTreePaths();
		viewer.refresh(updateLabels);
		viewer.setExpandedElements(expandedElements);
		viewer.setExpandedTreePaths(expandedTreePaths);
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
					if (selection.getFirstElement() instanceof IGruntNodeExecutableProvider) {
						IGruntNodeExecutable executable = ((IGruntNodeExecutableProvider) selection
								.getFirstElement()).getExecutable();
						if (executable != null) {
							final ILaunchConfiguration conf = GruntLaunchConfigurationDelegate
									.getOrCreate(executable);
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
		if (selection instanceof GruntFile) {
			IFile file = ((GruntFile) selection).getGruntFileResource();
			EditorUtils.openInEditor(file, 0, 0, true);
		} else if (selection instanceof IGruntNodeExecutable) {
			openInEditor(((IGruntNodeExecutable) selection));
		}
	}

	private void openInEditor(IGruntNodeExecutable task) {
		GruntProject gruntProject = task.getGruntFile().getGruntProject();
		final IIDETernProject ternProject = gruntProject.getTernProject();
		TernGruntTaskQuery query = new TernGruntTaskQuery(
				task.getExecutableName());
		IFile gruntFile = task.getGruntFile().getGruntFileResource();
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

	/**
	 * Saves the state of the viewer into the dialog settings. Works around the
	 * issues of {@link #saveState()} not being called when a view is closed
	 * while the workbench is still running
	 * 
	 * @since 3.5.500
	 */
	private void saveViewerState() {
		XMLMemento memento = XMLMemento.createWriteRoot("gruntView"); //$NON-NLS-1$
		StringWriter writer = new StringWriter();
		GruntFile file = null;
		IMemento projectMemento;
		GruntFile[] files = null;
		GruntProject[] projects = GruntProject.getGruntProjects();
		for (int i = 0; i < projects.length; i++) {
			files = projects[i].getGruntFiles();
			for (int j = 0; j < files.length; j++) {
				file = files[j];
				projectMemento = memento.createChild(TAG_GRUNT_FILE);
				projectMemento.putString(KEY_PATH, file.getGruntFileName());
			}
		}

		//
		// AntProjectNode[] projects = getProjects();
		// if (projects.length > 0) {
		// AntProjectNode project;
		// IMemento projectMemento;
		// for (int i = 0; i < projects.length; i++) {
		// project = projects[i];
		// projectMemento = memento.createChild(TAG_GRUNT_FILE);
		// projectMemento.putString(KEY_PATH, project.getBuildFileName());
		// // projectMemento.putString(KEY_NAME, project.getLabel());
		// // String defaultTarget = project.getDefaultTargetName();
		// // if (project.isErrorNode()) {
		// // projectMemento.putString(KEY_ERROR, String.valueOf(true));
		// // } else {
		// // if (project.isWarningNode()) {
		// // projectMemento.putString(KEY_WARNING, String.valueOf(true));
		// // }
		// // if (defaultTarget != null) {
		// // projectMemento.putString(KEY_DEFAULT, defaultTarget);
		// // }
		// // projectMemento.putString(KEY_ERROR, String.valueOf(false));
		// // }
		// }
		// //IMemento filterTargets =
		// memento.createChild(TAG_FILTER_INTERNAL_TARGETS);
		// //filterTargets.putString(KEY_VALUE, isFilterInternalTargets() ?
		// String.valueOf(true) : String.valueOf(false));
		// }
		try {
			memento.save(writer);
			GruntUIPlugin.getDefault()
					.getDialogSettingsSection(getClass().getName())
					.put(MEMENTO_DIALOG_NAME, writer.getBuffer().toString()); //$NON-NLS-1$
		} catch (IOException e) {
			// don't do anything. Simply don't store the settings
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		saveViewerState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		saveViewerState();
		super.dispose();
		// if (openWithMenu != null) {
		// openWithMenu.dispose();
		// }
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org
	 * .eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {

		}
	}

	public void addGruntFile(IFile gruntFile) {
		if (GruntProject.hasGruntNature(gruntFile.getProject())) {
			try {
				GruntProject gruntProject = GruntProject
						.getGruntProject(gruntFile.getProject());
				if (gruntProject.hasGruntFile(gruntFile)) {
					return;
				}
				GruntFile file = gruntProject.addGruntFile(gruntFile);
				viewer.refresh();
				ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
				handleSelectionChanged(new StructuredSelection(file));
			} catch (CoreException e) {
				IStatus status = new Status(IStatus.ERROR,
						GruntUIPlugin.PLUGIN_ID, e.getMessage());
				ErrorDialog.openError(getSite().getShell(),
						GruntUIMessages.GruntView_openFileDialog_title,
						status.getMessage(), status);
			}
		}
	}

	/**
	 * Updates the actions and status line for selection change in one of the
	 * viewers.
	 */
	private void handleSelectionChanged(IStructuredSelection selection) {
		// updateProjectActions();
		Iterator<IGruntNode> selectionIter = selection.iterator();
		IGruntNode selected = null;
		if (selectionIter.hasNext()) {
			selected = selectionIter.next();
		}
		String messageString = null;
		if (!selectionIter.hasNext()) {
			if (selected != null) {
				/*
				 * String errorString = selected.getProblemMessage(); if
				 * (errorString != null) {
				 * getViewSite().getActionBars().getStatusLineManager
				 * ().setErrorMessage(errorString); return; }
				 */
			}
			getViewSite().getActionBars().getStatusLineManager()
					.setErrorMessage(null);
			messageString = getStatusLineText(selected);
		}
		getViewSite().getActionBars().getStatusLineManager()
				.setMessage(messageString);
	}

	/**
	 * Returns text appropriate for display in the workbench status line for the
	 * given node.
	 */
	private String getStatusLineText(IGruntNode node) {
		if (node instanceof GruntFile) {
			GruntFile project = (GruntFile) node;
			return project.getGruntFileName();
		} else if (node instanceof Task) {
			return ((Task) node).getName();
		} else if (node instanceof Target) {
			// AntTargetNode target = (AntTargetNode) node;
			// StringBuffer message = new StringBuffer();
			// Enumeration<String> depends =
			// target.getTarget().getDependencies();
			// if (depends.hasMoreElements()) {
			// message.append(AntViewMessages.AntView_3);
			// message.append(depends.nextElement()); // Unroll the loop to
			// // avoid trailing comma
			// while (depends.hasMoreElements()) {
			// String dependancy = depends.nextElement();
			// message.append(',').append(dependancy);
			// }
			// message.append('\"');
			// }
			// String description = target.getTarget().getDescription();
			// if (description != null && description.length() != 0) {
			// message.append(AntViewMessages.AntView_4);
			// message.append(description);
			// message.append('\"');
			// }
			return ((Target) node).getName();
		}
		return null;
	}
}
