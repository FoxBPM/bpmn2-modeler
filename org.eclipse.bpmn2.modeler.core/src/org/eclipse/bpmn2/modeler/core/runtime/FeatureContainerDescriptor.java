/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
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

package org.eclipse.bpmn2.modeler.core.runtime;

import java.lang.reflect.Constructor;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.features.IFeatureContainer;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * @author Bob Brodt
 *
 */
public class FeatureContainerDescriptor extends BaseRuntimeDescriptor {

	protected String type;
	protected String containerClassName;

	/**
	 * @param rt
	 */
	public FeatureContainerDescriptor(TargetRuntime rt) {
		super(rt);
	}

	public Class getType() {
		ClassLoader cl = this.getRuntime().getRuntimeExtension().getClass().getClassLoader();
		try {
			return Class.forName(type, true, cl);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public IFeatureContainer getFeatureContainer() {
		try {
			ClassLoader cl = this.getRuntime().getRuntimeExtension().getClass().getClassLoader();
			Constructor ctor = null;
			//如果是自己的项目，就要换方式load Class
			if(containerClassName.indexOf("org.foxbpm.bpmn.designer.ui")!=-1) {
				Bundle bundle = Platform.getBundle("org.foxbpm.bpmn.designer.ui");
				Class<?> theClass = bundle.loadClass(containerClassName);
				ctor = theClass.getConstructor();
				return (IFeatureContainer)ctor.newInstance();
			}
			Class adapterClass = Class.forName(containerClassName, true, cl);
			ctor = adapterClass.getConstructor();
			return (IFeatureContainer)ctor.newInstance();
		} catch (Exception e) {
			Activator.logError(e);
		}
		return null;
	}
}
