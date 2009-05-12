/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2009  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package featureide.core.projectstructure.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import featureide.core.CorePlugin;
import featureide.core.projectstructure.nodetypes.NonTerminalNode;
import featureide.core.projectstructure.nodetypes.ProjectTreeNode;
import featureide.core.projectstructure.nodetypes.TerminalNode;
import featureide.core.projectstructure.trees.LeafTree;
import featureide.core.projectstructure.trees.ProjectTree;

/**
 * parses an FST to a file.trees.ProjectTree
 * 
 * @author Janet Feigenspan
 * @deprecated
 */
public class TreeBuilderFeatureHouse {

	/**
	 * The node from which on the nodes will be created
	 */
	private String startNode;

	/**
	 * The name of the project
	 */
	private String projectName;

	/**
	 * The project Tree
	 */
	private ProjectTree projectTree;

	/**
	 * Returns the tree representing the project
	 * 
	 * @return the projectTree
	 */
	public ProjectTree getProjectTree() {
		return projectTree;
	}

	/**
	 * Creates a new TreeBuilderFeatureHouse with the underlying language
	 * <code> language </code> and sets the name of the project to
	 * <code> projectName </code>.
	 * 
	 * @param language
	 *            the underlying language of the feature house project
	 * @param projectName
	 *            the name of the project.
	 */
	public TreeBuilderFeatureHouse(String projectName) {
		this.projectName = projectName;
		projectTree = new ProjectTree();
	}

	/**
	 * Creates the project Tree from the list <code> fstNodes </code>
	 * 
	 * @param fstNodes
	 *            the list of elements of the FST as generated by FeatureHouse
	 */
	public void createProjectTree(ArrayList<FSTNode> fstNodes) {
		String language = getLanguage(fstNodes);
		startNode = StartNode.determineStartNode(language);
		CorePlugin.getDefault().logInfo("start: " + startNode);

		Iterator<FSTNode> iterator = fstNodes.iterator();
		FSTNode node;
		String featureName = "none";

		// create the node that denotes the project
		insertProjectTreeNode("language", language, null);
		insertProjectTreeNode("project", projectName, projectTree.findNodeByName(language));

		while (iterator.hasNext()) {
			node = iterator.next();

			// create a new node, when a new feature is found
			if (node.getType().equalsIgnoreCase("feature")
					&& !node.getName().equals(featureName)) {
				featureName = node.getName();

				insertProjectTreeNode(node.getType(), featureName, projectTree
						.findNodeByName(projectName));
			}

			// create a new node, when a new file is found and then create the
			// LeafTree that belongs to the file
			if (node.getType().equals("EOF Marker")) {

				String name = node.getName();
				name = name
						.substring(name.lastIndexOf("\\") + 1, name.length());
				ProjectTreeNode fileNode = insertProjectTreeNode("file", name,
						projectTree.findNodeByName(featureName));

				LeafTree leafTree = createLeafTree(iterator, fileNode);
				projectTree.insertLeafTreeNode(leafTree, fileNode);
				leafTree.setIdentifier(fileNode.getIdentifier());
			}
		}
		projectTree.print();
	}

	private String getLanguage(ArrayList<FSTNode> fstNodes) {
		String language = fstNodes.get(0).getName();
		if (language.equalsIgnoreCase(".java"))
			return "java";
		else if (language.equalsIgnoreCase(".cs"))
			return "c#";
		else if (language.equalsIgnoreCase(".hs"))
			return "haskell";
		else if (language.equalsIgnoreCase(".c"))
			return "c";
		else if (language.equalsIgnoreCase(".h"))
			return "c";
		else if (language.equalsIgnoreCase(".xml"))
			return "xml";
		else if (language.equalsIgnoreCase(".xmi"))
			return "xml";
		// System.out.println("language: " + language);
		return null;
	}

	/**
	 * Creates a new ProjectTreeNode with the given type, name and parent and
	 * inserts it into the projectTree
	 * 
	 * @param type
	 *            the type of the new node
	 * @param name
	 *            the name of the new node
	 * @param parent
	 *            the parent of the new node
	 * @return the created ProjectTreeNode
	 */
	private ProjectTreeNode insertProjectTreeNode(String type, String name,
			ProjectTreeNode parent) {
		ProjectTreeNode node = new ProjectTreeNode(type, name, null);
		projectTree.insertProjectTreeNode(node, parent);
		node.setIdentifier(node.getParent().getIdentifier() + "/"
				+ node.getName());
		return node;
	}

	/**
	 * Creates a LeafTree beginning with the startnode. Takes the nodes from the
	 * iterator and sets the identifier of the first node according to the
	 * identifier of the ProjectTreeNode parent.
	 * 
	 * @param iterator
	 *            the iterator containing the FSTNodes from which the LeafTree
	 *            should be created
	 * @return the created LeafTree
	 */
	private LeafTree createLeafTree(Iterator<FSTNode> iterator,
			ProjectTreeNode parent) {
		LeafTree tree = new LeafTree();
		tree.setParent(parent);
		FSTNode node;
		while (iterator.hasNext()) {
			node = iterator.next();
			if (node.getType().equals(startNode)) {
				if (node instanceof FSTNonTerminal) {

					NonTerminalNode nonTerminal = insertNonTerminalNode(tree,
							tree.getRoot(), node);
					nonTerminal.setIdentifier(parent.getIdentifier() + "/"
							+ nonTerminal.getName());
					tree = fillTree(tree,
							((FSTNonTerminal) node).getChildren(), nonTerminal);
					tree.setName(nonTerminal.getIdentifier());
					return tree;
				}
			}
		}
		return null;
	}

	/**
	 * Recursively fills the tree with nodes from list and the parent.
	 * 
	 * @param tree
	 *            the tree that is filled
	 * @param list
	 *            the list of all children of the node parent
	 * @param parent
	 *            the parent of the nodes contained in the list
	 * @return a tree with the correct child-parent-relationship or just the
	 *         tree, if the list is empty
	 */
	private LeafTree fillTree(LeafTree tree, List<FSTNode> list,
			NonTerminalNode parent) {
		Iterator<FSTNode> iterator = list.iterator();
		// System.out.println(list.size());
		while (iterator.hasNext()) {
			FSTNode node = iterator.next();
			if (node instanceof FSTNonTerminal) {
				NonTerminalNode nonTerminal = insertNonTerminalNode(tree,
						parent, node);
				if (((FSTNonTerminal) node).getChildren() != null)
					fillTree(tree, ((FSTNonTerminal) node).getChildren(),
							nonTerminal);

			} else {
				assert node instanceof FSTTerminal;
				if (!node.getName().equals("-")
						&& !Pattern.matches("\\s", node.getName())) {
					insertTerminalNode(tree, parent, node);
				}
			}
		}
		return tree;
	}

	/**
	 * Creates a new TerminalNode from the FSTNode node and inserts it into the
	 * LeafTree tree with the according parent parent
	 * 
	 * @param tree
	 *            the tree in which the node should be inserted
	 * @param parent
	 *            the parent of the new node
	 * @param node
	 *            the node that should be inserted
	 * @return the newly created Node
	 */
	private TerminalNode insertTerminalNode(LeafTree tree,
			NonTerminalNode parent, FSTNode node) {
		TerminalNode terminal = new TerminalNode(node.getType(),
				node.getName(), null);
		terminal.setContent(((FSTTerminal) node).getBody());
		tree.insertTerminal(terminal, parent);
		terminal.setIdentifier(terminal.getParent().getIdentifier() + "/"
				+ terminal.getName());
		return terminal;
	}

	/**
	 * Creates a new NonTerminalNode from the FSTNode node and inserts it into
	 * the LeafTree tree with the according parent parent
	 * 
	 * @param tree
	 *            the tree in which the node should be inserted
	 * @param parent
	 *            the parent of the new node
	 * @param node
	 *            the node that should be inserted
	 * @return the newly created Node
	 */
	private NonTerminalNode insertNonTerminalNode(LeafTree tree,
			NonTerminalNode parent, FSTNode node) {
		NonTerminalNode nonTerminal = new NonTerminalNode(node.getType(), node
				.getName(), null);
		if (node.getName().equals("-")) {
			nonTerminal.setName("no name");
		}
		tree.insertNonTerminal(nonTerminal, parent);
		nonTerminal.setIdentifier(nonTerminal.getParent().getIdentifier() + "/"
				+ nonTerminal.getName());
		return nonTerminal;
	}
}
