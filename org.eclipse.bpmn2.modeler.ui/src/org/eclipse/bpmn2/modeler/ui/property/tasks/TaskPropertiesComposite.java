/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/


package org.eclipse.bpmn2.modeler.ui.property.tasks;

import org.eclipse.bpmn2.modeler.ui.property.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.ui.property.DefaultPropertiesComposite;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Composite;

public class TaskPropertiesComposite extends DefaultPropertiesComposite {

	public TaskPropertiesComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public TaskPropertiesComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.bpmn2.modeler.ui.property.AbstractBpmn2PropertiesComposite
	 * #createBindings(org.eclipse.emf.ecore.EObject)
	 */
	@Override
	public void createBindings(EObject be) {
		bindAttribute(be,"completionQuantity");
		bindAttribute(be,"startQuantity");
		bindAttribute(be,"isForCompensation");
		
		bindList(be, "boundaryEventDefs");
//		bindList(be, "dataInputAssociations");
//		bindList(be, "dataOutputAssociations");
		bindList(be, "properties");
		bindList(be, "resources");

//		bindReference(be,"ioSpecification");
		bindReference(be,"loopCharacteristics");
	}
}