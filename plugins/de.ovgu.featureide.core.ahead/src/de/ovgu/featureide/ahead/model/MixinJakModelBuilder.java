/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.ahead.model;

import java.util.LinkedList;
import java.util.List;

import mixin.AST_Modifiers;
import mixin.AST_ParList;
import mixin.AST_Program;
import mixin.AST_TypeName;
import mixin.AstCursor;
import mixin.AstNode;
import mixin.AstToken;
import mixin.DecNameDim;
import mixin.FldVarDec;
import mixin.MethodDcl;
import mixin.MthDector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import de.ovgu.featureide.ahead.AheadCorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.fstmodel.FSTField;
import de.ovgu.featureide.core.fstmodel.FSTMethod;
import de.ovgu.featureide.core.fstmodel.FSTRole;

/**
 * This builder builds the JakProjectModel, by extracting features, 
 * methods and fields from classes to build. 
 * 
 * @author Tom Brosch
 * @author Constanze Adler
 * @author Jens Meinicke
 * @author Felix Rieger
 */
public class MixinJakModelBuilder extends AbstractJakModelBuilder<AST_Program>{

	public MixinJakModelBuilder(IFeatureProject featureProject) {
		super(featureProject);
	}
	
	@Override
	public void reset() {
		model.reset();
	}

	/**
	 * Adds a class to the jak project model
	 * 
	 * @param className
	 *            Name of the class
	 * @param sources
	 *            source files that were composed to build this class
	 * @param composedASTs
	 *            composed ahead ASTs during the composition step
	 * @param ownASTs
	 *            ahead ASTs of each source file without composing
	 */
	@Override
	public void addClass(String className, List<IFile> sources,
			AST_Program[] composedASTs, AST_Program[] ownASTs) {
		sourceFolder = model.getFeatureProject().getSourceFolder();
		try {
			updateAst(className, sources, composedASTs, ownASTs);
		} catch (Throwable e) {
			AheadCorePlugin.getDefault().logError(e);
		}
	}
	
	@Override
	public void updateAst(String currentClass, List<IFile> sources,
			AST_Program[] composedASTs, AST_Program[] ownASTs) {
		IFile currentFile = null;
		AstCursor c = new AstCursor();
		for (int i = 0; i < sources.size(); i++) {
			currentFile = sources.get(i);
			// The role corresponding to the current source file
			FSTRole role = model.addRole(getFeature((IFolder)currentFile.getParent()), currentClass, currentFile);
		
			// Add methods and fields of the FST to the role
			for (c.First(ownASTs[i]); c.More(); c.PlusPlus()) {
				if (c.node instanceof MethodDcl) {
					FSTMethod method = getMethod((MethodDcl) c.node);
					role.getClassFragment().add(method);
					c.Sibling();
				}
				if (c.node instanceof FldVarDec) {
					for (FSTField field : getFields((FldVarDec) c.node)) {
						field.setLine(getLineNumber(c.node));
						role.getClassFragment().add(field);
					}
					c.Sibling();
				}
			}
		}
	}

	private int getLineNumber(AstNode node) {
		AstCursor cur = new AstCursor();
		for (cur.First(node); cur.More(); cur.PlusPlus()) {
			if (cur.node != null && cur.node.tok != null
					&& cur.node.tok[0] != null) {
				return ((AstToken) cur.node.tok[0]).lineNum();
			}
		}
		return -1;
	}

	private FSTMethod getMethod(MethodDcl methDcl) {
		AstCursor cur = new AstCursor();
		String type = "";
		String name = "";
		String modifiers = "";
		LinkedList<String> paramTypes = new LinkedList<String>();

		// Travers the Subtree and catch the name of the method,
		// its return type and parameter types;

		for (cur.First(methDcl); cur.More(); cur.PlusPlus()) {
			if (cur.node instanceof MthDector) {

				// Get the name of the Method
				name = ((MthDector) cur.node).getQName().GetName();

				// Travers the list of parameters if the method has some
				AST_ParList list = ((MthDector) cur.node).getAST_ParList();
				if (list != null) {
					AstCursor listCur = new AstCursor();
					for (listCur.First(list); listCur.More(); listCur
							.PlusPlus()) {
						if (listCur.node instanceof AST_TypeName) {
							paramTypes.add(((AST_TypeName) listCur.node)
									.GetName());
						}
					}
				}

				cur.Sibling(); // This subtree was complete analysed so we can
				// skip it
			} else if (cur.node instanceof AST_TypeName) {

				// Get the return type of the method
				type = ((AST_TypeName) cur.node).GetName();
				cur.Sibling(); // This subtree was complete analysed so we can
				// skip it
			}
			else if (cur.node instanceof AST_Modifiers) {

				// Get the modifiers of the method
				modifiers = ((AST_Modifiers) cur.node).toString().trim();
				cur.Sibling(); // This subtree was complete analysed so we can
				// skip it
				
			}

		}
		int lineNumber = getLineNumber(methDcl);
		return new FSTMethod(name, paramTypes, type, modifiers, lineNumber);
	}

	private LinkedList<FSTField> getFields(FldVarDec fieldDcl) {
		AstCursor cur = new AstCursor();
		String type = "";
		String modifiers = "";

		LinkedList<FSTField> fields = new LinkedList<FSTField>();

		// Travers the Subtree and get the type and
		// all variable qualifiers

		for (cur.First(fieldDcl); cur.More(); cur.PlusPlus()) {

			if (cur.node instanceof AST_TypeName) {

				// Get the return type of the method
				type = ((AST_TypeName) cur.node).GetName();
				cur.Sibling(); // This subtree was complete analysed so we can
				// skip it
			}
			else if (cur.node instanceof AST_Modifiers) {

				// Get modifiers of the field
				modifiers = ((AST_Modifiers) cur.node).toString().trim();
				cur.Sibling(); // This subtree was complete analysed so we can
				// skip it
			}
			else if (cur.node instanceof DecNameDim) {
				// to do: find out the dimension more correctly
				fields.add(new FSTField(((DecNameDim) cur.node).getQName()
						.GetName(), type, modifiers));
			}

		}

		return fields;
	}

	private String getFeature(IFolder folder) {
		if (((IFolder)folder.getParent()).equals(sourceFolder))
			return folder.getName();
		return getFeature((IFolder)folder.getParent());
	}

}
