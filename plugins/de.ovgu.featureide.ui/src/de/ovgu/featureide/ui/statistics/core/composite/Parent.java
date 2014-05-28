package de.ovgu.featureide.ui.statistics.core.composite;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.ui.statistics.core.StatisticsIds;
import de.ovgu.featureide.ui.statistics.ui.FeatureStatisticsView;
import de.ovgu.featureide.ui.statistics.ui.helper.TreeLabelProvider;

/**
 * 
 * Holds information for {@link TreeViewer}. Is capable of holding an image, a
 * description and a value.
 * 
 * @see LazyParent
 * @see FeatureStatisticsView
 * @see TreeLabelProvider
 * 
 * @author Dominik Hamann
 * @author Patrick Haese
 */
public class Parent implements StatisticsIds, GUIDefaults {
	protected Object value;
	protected String description; // description
	protected Image image;
	
	private boolean isCalculating;
	private boolean sorted = false;
	
	public boolean isSorted() {
		return sorted;
	}
	
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}
	
	protected Parent parent;
	protected List<Parent> children = new LinkedList<Parent>();
	
	private static final String IS_CALCULATING = "(calculating)";
	public static final Image REFESH_TAB_IMAGE = UIPlugin.getImage("refresh_tab.gif");
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public Image getImage() {
		return image;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setImage(Image image) {
		this.image = image;
	}
	
	protected Parent() {}
	
	public Parent(String description) {
		this(description, null);
	}
	
	public Parent(String description, Object value) {
		this.description = description;
		this.value = value;
	}
	
	public void addChild(Parent child) {
		this.children.add(child);
		child.setParent(this);
	}
	
	public void addChildren(List<Parent> input) {
		for (Parent stat : input) {
			children.add(stat);
		}
	}
	
	
	public Parent[] getChildren() {
		if (sorted) {
			sortChildren();
		}
		return children.toArray(new Parent[children.size()]);
	}
	
	protected void sortChildren() {
		Collections.sort(children, new Comparator<Parent>() {
			@Override
			public int compare(Parent o1, Parent o2) {
				return o1.toString().compareToIgnoreCase(o2.toString());
			}
		});
	}
	
	public Boolean hasChildren() {
		return !children.isEmpty();
	}
	
	public void deleteChild(Parent child) {
		children.remove(child);
	}
	
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append((description == null) ? "" : description.toString());
		if (isCalculating) {
			buffer.append(SEPARATOR);
			buffer.append(IS_CALCULATING);
		} else {
			if (description != null && value != null) {
				buffer.append(SEPARATOR);
			}
			buffer.append(((value == null) ? "" : value.toString()));
		}
		return buffer.toString();
	}
	
	public void setParent(Parent parent) {
		this.parent = parent;
	}
	
	public boolean isCalculating() {
		return isCalculating;
	}
	
	public void setCalculating(boolean isCalculating) {
		this.isCalculating = isCalculating;
	}
	
	public Parent getParent() {
		return this.parent;
	}
	
	public void startCalculating(boolean start) {
		setImage(start ? REFESH_TAB_IMAGE : null);
		this.isCalculating = start;
	}
}