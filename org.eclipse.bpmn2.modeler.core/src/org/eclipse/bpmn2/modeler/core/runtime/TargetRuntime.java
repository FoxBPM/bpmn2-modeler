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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.modeler.core.AbstractPropertyChangeListenerProvider;
import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.IBpmn2RuntimeExtension;
import org.eclipse.bpmn2.modeler.core.features.activity.task.ICustomElementFeatureContainer;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle;
import org.eclipse.bpmn2.modeler.core.runtime.ModelExtensionDescriptor.Property;
import org.eclipse.bpmn2.modeler.core.runtime.ModelExtensionDescriptor.Value;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.util.Bpmn2Resource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.graphiti.ui.editor.DiagramEditor;


public class TargetRuntime extends AbstractPropertyChangeListenerProvider {

	// extension point ID for Target Runtimes
	public static final String RUNTIME_EXTENSION_ID = "org.foxbpm.runtime"; //$NON-NLS-1$
	public static final String DEFAULT_RUNTIME_ID = "org.eclipse.bpmn2.modeler.runtime.none"; //$NON-NLS-1$
	// ID for BPMN2 specific problem markers
	public static final String BPMN2_MARKER_ID = "org.eclipse.bpmn2.modeler.core.problemMarker"; //$NON-NLS-1$
	
	// our cached registry of target runtimes contributed by other plugins
	protected static TargetRuntime targetRuntimes[];
	protected static TargetRuntime currentRuntime;
	
	protected String name;
	protected String[] versions;
	protected String id;
	protected String description;
	private IBpmn2RuntimeExtension runtimeExtension;
	protected ModelDescriptor modelDescriptor;
	protected ArrayList<Bpmn2TabDescriptor> tabDescriptors;
	protected ArrayList<CustomTaskDescriptor> customTasks;
	protected ArrayList<ModelExtensionDescriptor> modelExtensions;
	protected ArrayList<ModelEnablementDescriptor> modelEnablements;
	protected ModelEnablementDescriptor defaultModelEnablements;
	protected ArrayList<PropertyExtensionDescriptor> propertyExtensions;
	protected ArrayList<FeatureContainerDescriptor> featureContainers;
	protected ArrayList<ToolPaletteDescriptor> toolPalettes;
	protected HashMap<Class, ShapeStyle> shapeStyles;
	protected Bpmn2Resource bpmnResource;
	protected String problemMarkerId;

	public TargetRuntime(String id, String name, String versions, String description) {
		this.id = id;
		this.name = name;
		if (versions!=null)
			this.versions = versions.split("[, ]"); //$NON-NLS-1$
		this.description = description;
	}
	
	public static TargetRuntime getRuntime(String id) {
		if (getAllRuntimes() == null) {
			return null;
		}
		
		for (TargetRuntime rt : getAllRuntimes()) {
			if (rt.id.equals(id))
				return rt;
		}
		return null;
	}
	
	public static TargetRuntime getCurrentRuntime() {
		return currentRuntime;
	}
	
	public static void setCurrentRuntime(TargetRuntime rt) {
		currentRuntime = rt;
	}
	
	public static TargetRuntime getDefaultRuntime() {
		return getRuntime(DEFAULT_RUNTIME_ID);
	}
	
	/**
	 * 
	 * @return the first runtime which is non default, if there are more than on runtime or if there is only the default runtime
	 * return the DEFAULT_RUNTIME_ID
	 */
	public static String getFirstNonDefaultId(){
		String runtimeId = null;
		int nonDefaultRuntimeCount = 0;
		
		if (TargetRuntime.getAllRuntimes() == null) {
			return TargetRuntime.DEFAULT_RUNTIME_ID;
		}
		
		for (TargetRuntime rt :TargetRuntime.getAllRuntimes()) {
			if (!rt.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)){
				nonDefaultRuntimeCount++;
				runtimeId = rt.getId();
			}
		}
		
		if (nonDefaultRuntimeCount == 1 && runtimeId != null){
			return runtimeId;
		}else{
			return TargetRuntime.DEFAULT_RUNTIME_ID;
		}
	}
	
	public void setResourceSet(ResourceSet resourceSet) {
		resourceSet.getResourceFactoryRegistry().getContentTypeToFactoryMap().put(
				Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID, modelDescriptor.getResourceFactory());
	}
	
	public static TargetRuntime[] getAllRuntimes() {
		if (targetRuntimes==null) {
			// load runtimes contributions from other plugins
			ArrayList<TargetRuntime> rtList = new ArrayList<TargetRuntime>();
			
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
					RUNTIME_EXTENSION_ID);

			try {
				// first create all the Target Runtimes because other
				// extension elements refer to these by runtimeId
				for (IConfigurationElement e : config) {
					if (e.getName().equals("runtime")) { //$NON-NLS-1$
						String id = e.getAttribute("id"); //$NON-NLS-1$
						String name = e.getAttribute("name"); //$NON-NLS-1$
						String versions = e.getAttribute("versions"); //$NON-NLS-1$
						String description = e.getAttribute("description"); //$NON-NLS-1$
						TargetRuntime rt = new TargetRuntime(id,name,versions,description);
						
						rt.setRuntimeExtension((IBpmn2RuntimeExtension) e.createExecutableExtension("class")); //$NON-NLS-1$
					
						rtList.add(rt);

						// add validation problem marker IDs
						IContributor contributor = e.getDeclaringExtension().getContributor();
						IConfigurationElement[] markers = Platform.getExtensionRegistry().getConfigurationElementsFor(
								"org.eclipse.core.resources.markers"); //$NON-NLS-1$
						for (IConfigurationElement m : markers) {
							if (m.getDeclaringExtension().getContributor() == contributor) {
								rt.setProblemMarkerId(m.getDeclaringExtension().getUniqueIdentifier());
							}
						}
					}
				}
				
				targetRuntimes = rtList.toArray(new TargetRuntime[rtList.size()]);
				
				// process model first
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) { //$NON-NLS-1$
						currentRuntime = getRuntime(e);
						
						if (e.getName().equals("model")) { //$NON-NLS-1$
							ModelDescriptor md = new ModelDescriptor();
							if (e.getAttribute("uri")!=null) { //$NON-NLS-1$
								String uri = e.getAttribute("uri"); //$NON-NLS-1$
								md.setEPackage(EPackage.Registry.INSTANCE.getEPackage(uri));
								md.setEFactory(md.getEPackage().getEFactoryInstance());
							}
							if (e.getAttribute("resourceFactory")!=null) //$NON-NLS-1$
								md.setResourceFactory((ResourceFactoryImpl) e.createExecutableExtension("resourceFactory")); //$NON-NLS-1$
							currentRuntime.setModelDescriptor(md);
						}
					}
				}
				
				// need to process the Default Runtime first (defined in o.e.b.m.ui) because
				// other plugins can refer to this.
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) { //$NON-NLS-1$
						currentRuntime = getRuntime(e);
						if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
							if (e.getName().equals("propertyTab")) { //$NON-NLS-1$
								Bpmn2TabDescriptor td = new Bpmn2TabDescriptor(e);
								Bpmn2SectionDescriptor sd = new Bpmn2SectionDescriptor(td,e);
								currentRuntime.getTabs().add(td);
							}
							if (e.getName().equals("modelEnablement")) { //$NON-NLS-1$
								ModelEnablementDescriptor me;
								String type = e.getAttribute("type"); //$NON-NLS-1$
								String profile = e.getAttribute("profile"); //$NON-NLS-1$
								String ref = e.getAttribute("ref"); //$NON-NLS-1$
								currentRuntime.addModelEnablements(me = new ModelEnablementDescriptor(currentRuntime));
								me.setDiagramType(Bpmn2DiagramType.fromString(type));
								me.setProfile(profile);
								if (ref!=null) {
									String a[] = ref.split(":"); //$NON-NLS-1$
									TargetRuntime rt = TargetRuntime.getRuntime(a[0]);
									type = a[1];
									profile = a[2];
									me.initializeFromTargetRuntime(rt, Bpmn2DiagramType.fromString(type), profile);
								}
								
								for (IConfigurationElement c : e.getChildren()) {
									String object = c.getAttribute("object"); //$NON-NLS-1$
									String feature = c.getAttribute("feature"); //$NON-NLS-1$
									if (c.getName().equals("enable")) { //$NON-NLS-1$
										me.setEnabled(object, feature, true);
									} else if (c.getName().equals("disable")) { //$NON-NLS-1$
										me.setEnabled(object, feature, false);
									}
								}

							}
						}
					}
				}
				
				// process propertyTab, propertyExtension, customTask, modelExtension and modelEnablement next
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) { //$NON-NLS-1$
						currentRuntime = getRuntime(e);
						
						if (e.getName().equals("propertyTab")) { //$NON-NLS-1$
							if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
								// already done
								continue;
							}
							Bpmn2TabDescriptor td = new Bpmn2TabDescriptor(e);
							Bpmn2SectionDescriptor sd = new Bpmn2SectionDescriptor(td,e);
							currentRuntime.getTabs().add(td);
						}
						else if (e.getName().equals("customTask")) { //$NON-NLS-1$
							String id = e.getAttribute("id"); //$NON-NLS-1$
							String name = e.getAttribute("name"); //$NON-NLS-1$
							CustomTaskDescriptor ct = new CustomTaskDescriptor(id,name);
							ct.type = e.getAttribute("type"); //$NON-NLS-1$
							ct.description = e.getAttribute("description"); //$NON-NLS-1$
							ct.category = e.getAttribute("category"); //$NON-NLS-1$
							ct.icon = e.getAttribute("icon"); //$NON-NLS-1$
							String tabs = e.getAttribute("propertyTabs"); //$NON-NLS-1$
							if (tabs!=null) {
								ct.propertyTabs = tabs.split(" "); //$NON-NLS-1$
							}
							ct.featureContainer = (ICustomElementFeatureContainer) e.createExecutableExtension("featureContainer"); //$NON-NLS-1$
							ct.featureContainer.setCustomTaskDescriptor(ct);
							ct.featureContainer.setId(id);
							ct.setPermanent(true);
							getModelExtensionProperties(ct,e);
							currentRuntime.addCustomTask(ct);
						}
						else if (e.getName().equals("propertyExtension")) { //$NON-NLS-1$
							String id = e.getAttribute("id"); //$NON-NLS-1$
							PropertyExtensionDescriptor pe = new PropertyExtensionDescriptor(currentRuntime, e);
							pe.type = e.getAttribute("type"); //$NON-NLS-1$
							pe.adapterClassName = e.getAttribute("class"); //$NON-NLS-1$
							currentRuntime.addPropertyExtension(pe);
						}
						else if (e.getName().equals("featureContainer")) { //$NON-NLS-1$
							String id = e.getAttribute("id"); //$NON-NLS-1$
							FeatureContainerDescriptor fc = new FeatureContainerDescriptor(currentRuntime);
							fc.type = e.getAttribute("type"); //$NON-NLS-1$
							fc.containerClassName = e.getAttribute("class"); //$NON-NLS-1$
							currentRuntime.addFeatureContainer(fc);
						}
						else if (e.getName().equals("modelExtension")) { //$NON-NLS-1$
							String id = e.getAttribute("id"); //$NON-NLS-1$
							String name = e.getAttribute("name"); //$NON-NLS-1$
							ModelExtensionDescriptor me = new ModelExtensionDescriptor(id,name);
							me.type = e.getAttribute("type"); //$NON-NLS-1$
							me.description = e.getAttribute("description"); //$NON-NLS-1$
							getModelExtensionProperties(me,e);
							currentRuntime.addModelExtension(me);
						}
						else if (e.getName().equals("modelEnablement")) { //$NON-NLS-1$
							if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
								// already done
								continue;
							}
							ModelEnablementDescriptor me;
							String type = e.getAttribute("type"); //$NON-NLS-1$
							String profile = e.getAttribute("profile"); //$NON-NLS-1$
							String ref = e.getAttribute("ref"); //$NON-NLS-1$
							currentRuntime.addModelEnablements(me = new ModelEnablementDescriptor(currentRuntime));
							me.setDiagramType(Bpmn2DiagramType.fromString(type));
							me.setProfile(profile);
							if (ref!=null) {
								String a[] = ref.split(":"); //$NON-NLS-1$
								TargetRuntime rt = TargetRuntime.getRuntime(a[0]);
								type = a[1];
								profile = a[2];
								me.initializeFromTargetRuntime(rt, Bpmn2DiagramType.fromString(type), profile);
							}
							
							for (IConfigurationElement c : e.getChildren()) {
								String object = c.getAttribute("object"); //$NON-NLS-1$
								String feature = c.getAttribute("feature"); //$NON-NLS-1$
								if (c.getName().equals("enable")) { //$NON-NLS-1$
									me.setEnabled(object, feature, true);
								} else if (c.getName().equals("disable")) { //$NON-NLS-1$
									me.setEnabled(object, feature, false);
								}
							}

						}
						else if (e.getName().equals("style")) { //$NON-NLS-1$
							String object = e.getAttribute("object"); //$NON-NLS-1$
							String foreground = e.getAttribute("foreground"); //$NON-NLS-1$
							String background = e.getAttribute("background"); //$NON-NLS-1$
							String textColor = e.getAttribute("textColor"); //$NON-NLS-1$
							String font = e.getAttribute("font"); //$NON-NLS-1$
							EClass eclass = (EClass)Bpmn2Package.eINSTANCE.getEClassifier(object);
							ShapeStyle ss = new ShapeStyle(foreground, background, textColor, font);
							currentRuntime.getShapeStyles().put(eclass.getInstanceClass(), ss);
						}
						else if (e.getName().equals("toolPalette")) { //$NON-NLS-1$
							ToolPaletteDescriptor toolPalette = null;
							String id = e.getAttribute("id"); //$NON-NLS-1$
							for (ToolPaletteDescriptor tp : currentRuntime.getToolPalettes()) {
								if (tp.id.equals(id)) {
									toolPalette = tp;
									break;
								}
							}
							if (toolPalette==null) {
								toolPalette = new ToolPaletteDescriptor();
								currentRuntime.addToolPalette(toolPalette);
							}
							toolPalette.create(e);
						}
					}
				}


				// All done parsing configuration elements
				// now go back and fix up some things...
				for (TargetRuntime rt : targetRuntimes) {
					
					if (rt.modelDescriptor==null) {
						rt.modelDescriptor = getDefaultRuntime().getModelDescriptor(); 
					}
					for (ToolPaletteDescriptor tp : rt.getToolPalettes()) {
						tp.sortCategories();
					}
					
					// add customTask and modelExtension features to modelEnablements
//					for (ModelEnablementDescriptor me : rt.getModelEnablements()) {
//						for (ModelExtensionDescriptor med : rt.getModelExtensions()) {
//							for (Property p : med.getProperties()) {
//								me.setEnabled(med.getType(), p.name, true);
//								EClassifier eClass = ModelUtil.getEClassifierFromString(med.getEPackage(), med.getType());
//								ModelUtil.createDynamicAttribute(med.getEPackage(),
//										eClass, p.name, p.type);
//							}
//						}
						
//						for (CustomTaskDescriptor ct : rt.getCustomTasks()) {
//							// the tool palette checks for enablement of this custom task ID
//							me.setEnabled(ct.getId(), true);
//							for (Property p : ct.getProperties()) {
//								me.setEnabled(ct.getType(), p.name, true);
//							}
//						}
					
					// DEBUG:
//					for (ModelEnablementDescriptor me : rt.getModelEnablements()) {
//						System.out.println("Runtime: '"+rt.getName()+
//								"'\nEnablement type: '"+me.getType()+
//								"' Profile: '"+me.getProfile()+
//								"'\nNumber of enabled model elements: "+me.getAllEnabled().size());
//						List<String> classes = new ArrayList<String>(me.getAllEnabled().size());
//						classes.addAll(me.getAllEnabled());
//						Collections.sort(classes);
//						for (String c : classes) {
//							System.out.println(c);
//							List<String> features = new ArrayList<String>(me.getAllEnabled(c).size());
//							features.addAll(me.getAllEnabled(c));
//							Collections.sort(features);
//							for (String f : features) {
//								System.out.println("  "+f);
//							}
//						}
//						System.out.println("");
//					}
//					System.out.println("");
					
				}
				
			} catch (Exception ex) {
				Activator.logError(ex);
			}
			
			currentRuntime = getDefaultRuntime();
			
			CustomTaskImageProvider.registerAvailableImages();
		}
		return targetRuntimes;
	}
	
	private static Object getModelExtensionProperties(ModelExtensionDescriptor ct, IConfigurationElement e) {
		
		String elem = e.getName();
		if ("value".equals(elem)) { //$NON-NLS-1$
			String id = e.getAttribute("id"); //$NON-NLS-1$
			Value val = new Value(id);
			for (IConfigurationElement c : e.getChildren()) {
				Object propValue = getModelExtensionProperties(null, c);
				val.getValues().add(propValue);
			}
			return val;
		}
		else if ("property".equals(elem)) { //$NON-NLS-1$
			String name = e.getAttribute("name"); //$NON-NLS-1$
			String value = e.getAttribute("value"); //$NON-NLS-1$
			String ref = e.getAttribute("ref"); //$NON-NLS-1$
			String type = e.getAttribute("type"); //$NON-NLS-1$
			String description = e.getAttribute("description"); //$NON-NLS-1$
			Property prop = new Property(name,description);
			prop.type = type;
			if (value!=null)
				prop.getValues().add(value);
			else if (ref!=null) {
				prop.ref = ref;
			}
			else if(e.getChildren().length > 0){
				Object o = getModelExtensionProperties(null, e.getChildren()[0]);
				if (o instanceof Value)
					prop.getValues().addAll(((Value)o).getValues());
			}
			return prop;
		}
		else {
			for (IConfigurationElement c : e.getChildren()) {
				Object o = getModelExtensionProperties(null, c);
				if (o instanceof Property && ct!=null)
					ct.getProperties().add((Property)o);
			}
		}
		return null;
	}

	private static TargetRuntime getRuntime(IConfigurationElement e) {
		String runtimeId = e.getAttribute("runtimeId"); //$NON-NLS-1$
		TargetRuntime rt = getRuntime(runtimeId);
		if (rt==null)
			return currentRuntime;
		return rt;
	}
	
	public ModelDescriptor getModelDescriptor() {
		return modelDescriptor;
	}
	
	public void setModelDescriptor(ModelDescriptor md) {
		md.setRuntime(this);
		this.modelDescriptor = md;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDescription() {
		return description;
	}
	
	public ArrayList<CustomTaskDescriptor> getCustomTasks()
	{
		if (customTasks==null) {
			customTasks = new ArrayList<CustomTaskDescriptor>();
		}
		return customTasks;
	}
	
	public CustomTaskDescriptor getCustomTask( String id ) {
		Iterator<CustomTaskDescriptor> ctIter = customTasks.iterator();
		while (ctIter.hasNext()) {
			CustomTaskDescriptor ctd = ctIter.next();
			if (ctd.getId().equalsIgnoreCase(id)) 
				return ctd;
		}
		return null;
	}

	public boolean customTaskExists ( String id ) {
		Iterator<CustomTaskDescriptor> ctIter = customTasks.iterator();
		while (ctIter.hasNext()) {
			CustomTaskDescriptor ctd = ctIter.next();
			if (ctd.getId().equalsIgnoreCase(id)) 
				return true;
		}
		return false;
	}
	
	public void addCustomTask(CustomTaskDescriptor ct) {
		ct.setRuntime(this);
		getCustomTasks().add(ct);
	}
	
	public ArrayList<ModelExtensionDescriptor> getModelExtensions()
	{
		if (modelExtensions==null) {
			modelExtensions = new ArrayList<ModelExtensionDescriptor>();
		}
		return modelExtensions;
	}
	
	public void addModelExtension(ModelExtensionDescriptor me) {
		me.setRuntime(this);
		getModelExtensions().add(me);
	}
	
	public ArrayList<PropertyExtensionDescriptor> getPropertyExtensions()
	{
		if (propertyExtensions==null) {
			propertyExtensions = new ArrayList<PropertyExtensionDescriptor>();
		}
		return propertyExtensions;
	}
	
	public void addPropertyExtension(PropertyExtensionDescriptor me) {
		me.setRuntime(this);
		getPropertyExtensions().add(me);
	}

	public PropertyExtensionDescriptor getPropertyExtension(Class clazz) {
		for (PropertyExtensionDescriptor ped : getPropertyExtensions()) {
			String className = clazz.getName();
			if (className.equals(ped.type))
				return ped;
			// well, that didn't work...
			// The "type" name should be the BPMN2 element's interface definition;
			// if it's an implementation class name, try to convert it to its
			// interface name.
			className = className.replaceFirst("\\.impl\\.", "."); //$NON-NLS-1$ //$NON-NLS-2$
			className = className.replaceFirst("Impl$", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (className.equals(ped.type))
				return ped;
		}
		return null;
	}
	
	public ArrayList<FeatureContainerDescriptor> getFeatureContainers()
	{
		if (featureContainers==null) {
			featureContainers = new ArrayList<FeatureContainerDescriptor>();
		}
		return featureContainers;
	}
	
	public void addFeatureContainer(FeatureContainerDescriptor me) {
		me.setRuntime(this);
		getFeatureContainers().add(me);
	}

	public FeatureContainerDescriptor getFeatureContainer(EClass clazz) {
		for (FeatureContainerDescriptor fcd : getFeatureContainers()) {
			String className = clazz.getInstanceClassName();
			if (className.equals(fcd.type))
				return fcd;
			// well, that didn't work...
			// The "type" name should be the BPMN2 element's interface definition;
			// if it's an implementation class name, try to convert it to its
			// interface name.
			className = className.replaceFirst("\\.impl\\.", "."); //$NON-NLS-1$ //$NON-NLS-2$
			className = className.replaceFirst("Impl$", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (className.equals(fcd.type))
				return fcd;
		}
		return null;
	}
	
	public ArrayList<ModelEnablementDescriptor> getModelEnablements()
	{
		if (modelEnablements==null) {
			modelEnablements = new ArrayList<ModelEnablementDescriptor>();
		}
		return modelEnablements;
	}
	
	/**
	 * @param object
	 * @return
	 * @deprecated
	 */
	public ModelEnablementDescriptor getModelEnablements(EObject object)
	{
		// TODO: At some point the separation of "Core" and "UI" plugins is going to become
		// an unmanageable problem: I am having to resort to using DiagramEditor.getAdapter()
		// more and more just to get things done.
		// Think about either reorganizing these two plugins, or simply combining them...
		TargetRuntime rt = this;
		DiagramEditor diagramEditor = ModelUtil.getEditor(object);
		if (diagramEditor!=null) {
			rt = (TargetRuntime) diagramEditor.getAdapter(TargetRuntime.class);
		}
		ArrayList<ModelEnablementDescriptor> meds = rt.getModelEnablements();
		if (meds.size()>0)
			return meds.get(0);
		return null;
	}
	
	public ArrayList<ModelEnablementDescriptor>  getModelEnablements(Bpmn2DiagramType diagramType)
	{
		ArrayList<ModelEnablementDescriptor> list = new ArrayList<ModelEnablementDescriptor>();
		for (ModelEnablementDescriptor me : getModelEnablements()) {
			if (diagramType == Bpmn2DiagramType.NONE && me.getDiagramType()==null) {
				list.add(me);
			}
			else if (diagramType == me.getDiagramType()) {
				list.add(me);
			}
		}
		return list;
	}
	
	public ModelEnablementDescriptor getModelEnablements(Bpmn2DiagramType diagramType, String profile)
	{
		if (profile!=null && profile.isEmpty())
			profile = null;
		
		for (ModelEnablementDescriptor me : getModelEnablements()) {
			if (diagramType == Bpmn2DiagramType.NONE && me.getDiagramType()==null) {
				if (profile==null || profile.equalsIgnoreCase(me.getProfile()))
					return me;
			}
			if (diagramType == me.getDiagramType()) {
				if (profile==null || profile.equalsIgnoreCase(me.getProfile()))
					return me;
			}
		}
		if (this != getDefaultRuntime()) {
			// fall back to enablements from Default Runtime
			return getDefaultRuntime().getModelEnablements(diagramType, profile);
		}
		
		if (defaultModelEnablements==null)
			defaultModelEnablements = new ModelEnablementDescriptor(getDefaultRuntime());
		return defaultModelEnablements;
	}
	
	public void addModelEnablements(ModelEnablementDescriptor me) {
		me.setRuntime(this);
		getModelEnablements().add(me);
	}
	
	
	//////////////////////

	public ArrayList<ToolPaletteDescriptor> getToolPalettes()
	{
		if (toolPalettes==null) {
			toolPalettes = new ArrayList<ToolPaletteDescriptor>();
		}
		return toolPalettes;
	}
	
	public ToolPaletteDescriptor getToolPalette(EObject object)
	{
		DiagramEditor diagramEditor = ModelUtil.getEditor(object);
		return (ToolPaletteDescriptor) diagramEditor.getAdapter(ToolPaletteDescriptor.class);
	}
	
	public ToolPaletteDescriptor getToolPalette(Bpmn2DiagramType diagramType, String profile)
	{
		ToolPaletteDescriptor defaultToolPalette = null;
		for (ToolPaletteDescriptor tp : getToolPalettes()) {
			String s = diagramType.name();
			if (diagramType == Bpmn2DiagramType.NONE && tp.getType()==null) {
				if (profile==null)
					return tp;
				 for (String p : tp.getProfiles()) {
					 if (profile.equalsIgnoreCase(p))
						 return tp;
				 }
			}
			if (s.equalsIgnoreCase(tp.getType())) {
				if (profile==null)
					return tp;
				 for (String p : tp.getProfiles()) {
					 if (profile.equalsIgnoreCase(p))
						 return tp;
				 }
			}
			if (tp.getType()==null || tp.getType().isEmpty())
				defaultToolPalette = tp;
		}

		if (defaultToolPalette!=null)
			return defaultToolPalette;
		
		if (this != getDefaultRuntime()) {
			// fall back to toolPalettes from Default Runtime
			return getDefaultRuntime().getToolPalette(diagramType, profile);
		}
		return null;
	}
	
	public void addToolPalette(ToolPaletteDescriptor tp) {
		tp.setRuntime(this);
		getToolPalettes().add(tp);
	}
	/////////////////////
	
	
	public Map<Class, ShapeStyle> getShapeStyles() {
		if (shapeStyles==null) {
			shapeStyles = new HashMap<Class, ShapeStyle>();
		}
		return shapeStyles;
	}
	
	public ShapeStyle getShapeStyle(Class c) {
		ShapeStyle ss = getShapeStyles().get(c);
		if (ss==null) {
			ss = new ShapeStyle(ss);
		}
		return ss;
	}

	private void addAfterTab(ArrayList<Bpmn2TabDescriptor> list, Bpmn2TabDescriptor tab) {
		
		getAllRuntimes();
		String afterTab = tab.getAfterTab();
		if (afterTab!=null && !afterTab.isEmpty() && !afterTab.equals("top")) { //$NON-NLS-1$
			String id = tab.getId();
			for (TargetRuntime rt : targetRuntimes) {
				for (Bpmn2TabDescriptor td : rt.getTabs()) {
					if (tab!=td) {
						if (td.getId().equals(afterTab) || td.isReplacementForTab(afterTab)) {
							addAfterTab(list,td);
							if (rt==this || rt==TargetRuntime.getDefaultRuntime()) {
								if (!list.contains(td))
									list.add(td);
							}
						}
					}
				}
			}
		}
	}
	
	private ArrayList<Bpmn2TabDescriptor> getTabs() {
		if (tabDescriptors==null)
			tabDescriptors = new ArrayList<Bpmn2TabDescriptor>();
		return tabDescriptors;
	}

	public static Bpmn2TabDescriptor findTabDescriptor(String id) {
		for (TargetRuntime rt : TargetRuntime.getAllRuntimes()) {
			Bpmn2TabDescriptor tab = rt.getTabDescriptor(id);
			if (tab!=null)
				return tab;
		}
		return null;
	}
	
	public Bpmn2TabDescriptor getTabDescriptor(String id) {
		for (Bpmn2TabDescriptor tab : getTabs()) {
			if (tab.getId().equals(id))
				return tab;
		}
		return null;
	}
	
	/**
	 * @return
	 */
	public ArrayList<Bpmn2TabDescriptor> getTabDescriptors() {
		ArrayList<Bpmn2TabDescriptor> list = new ArrayList<Bpmn2TabDescriptor>();
		for (Bpmn2TabDescriptor tab : getTabs()) {
			addAfterTab(list, tab);
			if (!list.contains(tab))
				list.add(tab);
		}
		
		return list;
	}
	
	public IBpmn2RuntimeExtension getRuntimeExtension() {
		return runtimeExtension;
	}

	public void setRuntimeExtension(IBpmn2RuntimeExtension runtimeExtension) {
		this.runtimeExtension = runtimeExtension;
	}
	
	public void setResource(Bpmn2Resource bpmnResource) {
		this.bpmnResource = bpmnResource;
		
		TreeIterator<EObject> iter = bpmnResource.getAllContents();
		while (iter.hasNext()) {
			EObject object = iter.next();
			String className = object.eClass().getName();
			// attach ModelExtensionDescriptor.Property adapters to all <modelExtension>
			// objects defined in the target runtime's plugin.xml
			for (ModelExtensionDescriptor med : getModelExtensions()) {
	    		if (className.equals(med.getType())) {
					med.adaptObject(object);
	    			break;
	    		}
			}
			// do the same thing for all <customTask> elements, but only if the object
			// is identified by the CustomTaskFeatureContainer as a genuine "custom task"
			for (CustomTaskDescriptor ctd : getCustomTasks()) {
	    		if (className.equals(ctd.getType())) {
	    			if (ctd.getFeatureContainer().getId(object)!=null) {
	    				ctd.adaptObject(object);
	    				break;
	    			}
	    		}
			}
		}
	}
	
	public Bpmn2Resource getResource() {
		return bpmnResource;
	}
	
	protected void setProblemMarkerId(String id) {
		problemMarkerId = id;
	}

	public String getProblemMarkerId() {
		if (problemMarkerId==null)
			return BPMN2_MARKER_ID;
		return problemMarkerId;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof TargetRuntime) {
			if (id!=null && id.equals(((TargetRuntime)arg0).getId()))
				return true;
		}
		else if (arg0 instanceof String) {
			return ((String)arg0).equals(id);
		}
		return super.equals(arg0);
	}
}
