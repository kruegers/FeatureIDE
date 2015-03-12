/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.ui.statistics.ui.helper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.ITextEditor;

import de.ovgu.featureide.core.fstmodel.FSTClassFragment;
import de.ovgu.featureide.core.fstmodel.FSTField;
import de.ovgu.featureide.core.fstmodel.FSTMethod;
import de.ovgu.featureide.core.fstmodel.FSTRole;
import de.ovgu.featureide.ui.statistics.core.composite.Parent;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.ClassNodeParent;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.ConfigParentNode;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.FieldNodeParent;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.MethodNodeParent;
import de.ovgu.featureide.ui.statistics.core.composite.lazyimplementations.genericdatatypes.AbstractSortModeNode;
import de.ovgu.featureide.ui.statistics.ui.ConfigDialog;

/**
 * Simple listener for the treeview.
 * 
 * @author Dominik Hamann
 * @author Patrick Haese
 */
public class TreeClickListener implements IDoubleClickListener {

	private TreeViewer view;

	public TreeClickListener(TreeViewer view) {
		super();
		this.view = view;
	}

	/**
	 * Performs actions depending on the type of the clicked note e.g. opening a
	 * dialog for {@link ConfigParentNode.ConfigNode} or
	 * expanding/collapsing nodes(default operation).
	 * 
	 */
	@Override
	public void doubleClick(DoubleClickEvent event) {
		Object[] selectedObjects = ((TreeSelection) event.getSelection()).toArray();

		if (selectedObjects.length == 1 && selectedObjects[0] instanceof Parent && ((Parent) selectedObjects[0]).getValue() instanceof FSTRole) {
			FSTRole role = (FSTRole) ((Parent) selectedObjects[0]).getValue();
			IFile file = role.getFile();
		}

		for (Object selected : selectedObjects) {
			if (selected instanceof ConfigParentNode.ConfigNode) {
				handleConfigNodes(event, selected);
			} else if (selected instanceof AbstractSortModeNode && view.getExpandedState(selected)) {
				final AbstractSortModeNode sortNode = ((AbstractSortModeNode) selected);
				sortNode.setSortByValue(!sortNode.isSortByValue());

				UIJob job = new UIJob("resort node") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						view.refresh(sortNode);
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.INTERACTIVE);
				job.schedule();
			} else if (selected instanceof Parent && ((Parent) selected).hasChildren()) {
				view.setExpandedState(selected, !view.getExpandedState(selected));
			} else if (selected instanceof Parent
					&& (((Parent) selected).getParent() instanceof FieldNodeParent || ((Parent) selected).getParent() instanceof ClassNodeParent || ((Parent) selected)
							.getParent() instanceof MethodNodeParent)) {
				IFile iFile;
				if (((Parent) selected).getValue() instanceof FSTField) {
					iFile = ((FSTField) ((Parent) selected).getValue()).getRole().getFile();
					int line = ((FSTField) ((Parent) selected).getValue()).getLine();
					openEditor(iFile, line);
				} else if (((Parent) selected).getValue() instanceof FSTMethod) {
					iFile = ((FSTMethod) ((Parent) selected).getValue()).getRole().getFile();
					int line = ((FSTMethod) ((Parent) selected).getValue()).getLine();
					openEditor(iFile, line);
				} else {
					iFile = ((FSTRole) ((FSTClassFragment) ((Parent) selected).getValue()).getRole()).getFile();
					openEditor(iFile, 1);
				}
				
			}
		}
	}

	
	/**
	 * Opens a dialog to start the calculation corresponding to the clicked
	 * config-node - but only if their isn't already a calculation in progress.
	 * 
	 */
	private void handleConfigNodes(DoubleClickEvent event, Object selected) {
		ConfigParentNode.ConfigNode clickedNode = (ConfigParentNode.ConfigNode) selected;
		if (!clickedNode.isCalculating()) {
			ConfigDialog dial = new ConfigDialog(event.getViewer().getControl().getShell(), clickedNode.getDescription());
			if (dial.open() == ConfigDialog.OK) {
				clickedNode.calculate(dial.getTimeout(), dial.getPriority());
			}
		}
	}

	public static void scrollToLine(IEditorPart editorPart, int lineNumber) {
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) {
			return;
		}
		final ITextEditor editor = (ITextEditor) editorPart;
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (document != null) {
			IRegion lineInfo = null;
			try {
				lineInfo = document.getLineInformation(lineNumber - 1);
			} catch (BadLocationException e) {
			}
			if (lineInfo != null) {
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		}
	}

	public void openEditor(IFile iFile, int line) {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
		IEditorPart editorPart = null;
		IContentDescription description;
		try {
			description = iFile.getContentDescription();
			final IEditorDescriptor desc = workbench.getEditorRegistry().getDefaultEditor(iFile.getName(),
					(description != null) ? description.getContentType() : null);
			editorPart = activePage.openEditor(new FileEditorInput(iFile), (desc != null) ? desc.getId() : "org.eclipse.ui.DefaultTextEditor");
			scrollToLine(editorPart, line);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
