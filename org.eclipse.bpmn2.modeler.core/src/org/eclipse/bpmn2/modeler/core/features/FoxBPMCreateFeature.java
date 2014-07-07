package org.eclipse.bpmn2.modeler.core.features;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.Lane;
import org.eclipse.bpmn2.LaneSet;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.dd.di.Plane;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.context.impl.AreaContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;

public class FoxBPMCreateFeature extends CompoundCreateFeature {
	private Resource resource;
	private Definitions definitions;

	public FoxBPMCreateFeature(IFeatureProvider fp, Resource resource, Definitions definitions) {
		super(fp);
		this.resource = resource;
		this.definitions = definitions;
	}

	@Override
	public void execute(IContext context) {
		DocumentRoot documentRoot = (DocumentRoot) resource.getContents().get(0);
		Definitions templateDefinitions = (Definitions) documentRoot.getDefinitions();

		ContainerShape targetContainer = ((ICreateContext) context).getTargetContainer();
		if (targetContainer == null)
			targetContainer = getDiagram();

		final Process templateProcess = (Process) templateDefinitions.getRootElements().get(0);

		final Diagram diagram = getDiagram();

		final Plane plane = definitions.getDiagrams().get(0).getPlane();

		final Process process = (Process) definitions.getRootElements().get(0);

		for (FlowElement flowElement : templateProcess.getFlowElements()) {
			FlowElement newFlowElement = EcoreUtil.copy(flowElement);
			newFlowElement.setId(null);
			ModelUtil.setID(newFlowElement, process.eResource());

			TreeIterator<EObject> contents = newFlowElement.eAllContents();
			for (; contents.hasNext();) {
				EObject t = contents.next();
				if (t instanceof BaseElement) {
					((BaseElement) t).setId(null);
					ModelUtil.setID(t, process.eResource());
				}
			}

			process.getFlowElements().add(newFlowElement);

			BPMNShape newBpmnShape = null;

			BPMNShape bpmnShape = DIUtils.findBPMNShape(flowElement);
			BPMNEdge bpmnEdge = DIUtils.findBPMNEdge(flowElement);
			if (bpmnShape != null) {
				newBpmnShape = EcoreUtil.copy(bpmnShape);

				newBpmnShape.setId(null);
				ModelUtil.setID(newBpmnShape, process.eResource());
				newBpmnShape.setBpmnElement(newFlowElement);
				plane.getPlaneElement().add(newBpmnShape);

				AddContext ac = new AddContext(new AreaContext(), newFlowElement);
				ac.setTargetContainer(diagram);
				ac.setNewObject(newFlowElement);
				if (null != newBpmnShape) {
					float x1 = newBpmnShape.getBounds().getX();
					float y1 = newBpmnShape.getBounds().getY();
					int x = (int) x1;
					int y = (int) y1;
					ac.setLocation(x, y);
				}

				IAddFeature af = getFeatureProvider().getAddFeature(ac);
				ac.setX(((ICreateContext) context).getX());
				ac.setY(((ICreateContext) context).getY());
				af.add(ac);
			} else if (bpmnEdge != null) {
//				DIUtils.addShape(bpmnEdge, definitions.getDiagrams().get(0));
			}
		}
		
		//Lane
		List<Lane> lanes = new ArrayList<Lane>();
		for (LaneSet laneSet : templateProcess.getLaneSets()) {
			lanes.addAll(laneSet.getLanes());
		}
		
		for (Lane lane : lanes) {
			Lane lane2 = EcoreUtil.copy(lane);
			lane2.setId(null);
			ModelUtil.setID(lane2, process.eResource());

			TreeIterator<EObject> contents = lane2.eAllContents();
			for (; contents.hasNext();) {
				EObject t = contents.next();
				if (t instanceof BaseElement) {
					((BaseElement) t).setId(null);
					ModelUtil.setID(t, process.eResource());
				}
			}

			//现在process类型的流程默认只有一个lanSet
			if(process.getLaneSets().size()<1) {
				LaneSet laneSet = Bpmn2Factory.eINSTANCE.createLaneSet();
				ModelUtil.setID(laneSet, process.eResource());
				laneSet.setName("Lane Set 1");
				process.getLaneSets().add(laneSet);
			}
			process.getLaneSets().get(0).getLanes().add(lane2);

			BPMNShape newBpmnShape = null;

			BPMNShape bpmnShape = DIUtils.findBPMNShape(lane);
			BPMNEdge bpmnEdge = DIUtils.findBPMNEdge(lane);
			if (bpmnShape != null) {
				newBpmnShape = EcoreUtil.copy(bpmnShape);

				newBpmnShape.setId(null);
				ModelUtil.setID(newBpmnShape, process.eResource());
				newBpmnShape.setBpmnElement(lane2);
				plane.getPlaneElement().add(newBpmnShape);

				AddContext ac = new AddContext(new AreaContext(), lane2);
				ac.setTargetContainer(diagram);
				ac.setNewObject(lane2);
				if (null != newBpmnShape) {
					float x1 = newBpmnShape.getBounds().getX();
					float y1 = newBpmnShape.getBounds().getY();
					int x = (int) x1;
					int y = (int) y1;
					ac.setLocation(x, y);
				}

				IAddFeature af = getFeatureProvider().getAddFeature(ac);
				ac.setX(((ICreateContext) context).getX());
				ac.setY(((ICreateContext) context).getY());
				af.add(ac);
			} else if (bpmnEdge != null) {
//				DIUtils.addShape(bpmnEdge, definitions.getDiagrams().get(0));
			}
		}

	}

}
