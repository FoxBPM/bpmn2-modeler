package org.eclipse.bpmn2.modeler.ui.features.activity.task;

import org.eclipse.bpmn2.modeler.core.features.IFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.activity.task.ICustomElementFeatureContainer;
import org.eclipse.bpmn2.modeler.core.runtime.CustomTaskDescriptor;
import org.eclipse.bpmn2.modeler.ui.diagram.BPMNFeatureProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;

public class FoxBPMElemetFeatureContainer implements ICustomElementFeatureContainer {
	protected String id;
	protected CustomTaskDescriptor customTaskDescriptor;
	protected IFeatureContainer featureContainerDelegate = null;
	protected BPMNFeatureProvider fp;
	
	@Override
	public boolean isAvailable(IFeatureProvider fp) {
		return false;
	}

	@Override
	public Object getApplyObject(IContext context) {
		return null;
	}

	@Override
	public boolean canApplyTo(Object o) {
		return false;
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IRemoveFeature getRemoveFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public String getDescription() {
		if (customTaskDescriptor!=null)
			return customTaskDescriptor.getDescription();
		return "";
	}

	@Override
	public void setId(String id) {

	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public void setCustomTaskDescriptor(CustomTaskDescriptor customTaskDescriptor) {

	}

	@Override
	public String getId(EObject object) {
		return null;
	}

	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		return null;
	}

}
