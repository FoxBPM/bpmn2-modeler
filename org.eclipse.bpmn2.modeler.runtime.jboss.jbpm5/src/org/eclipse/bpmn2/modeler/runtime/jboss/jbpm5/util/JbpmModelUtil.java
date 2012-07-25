package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.util;

import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.ItemDefinition;
import org.eclipse.bpmn2.ItemKind;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ImportUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.drools.process.core.datatype.DataType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.drools.process.core.datatype.DataTypeFactory;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.drools.process.core.datatype.DataTypeRegistry;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.drools.process.core.datatype.impl.type.EnumDataType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.drools.process.core.datatype.impl.type.UndefinedDataType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.GlobalType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.ImportType;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.ModelFactory;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.ModelPackage;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.bpmn2.modeler.ui.property.dialogs.SchemaImportDialog;
import org.eclipse.bpmn2.modeler.ui.util.PropertyUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

public class JbpmModelUtil {

	/**
	 * Helper method to display a Java class import dialog and create a new ImportType. This method
	 * will also create a corresponding ItemDefinition for the newly imported java type.
	 * 
	 * @param object - a context EObject used to search for the Process in which the new
	 *                 ImportType will be created.
	 * @return an ImportType object if it was created, null if the user canceled the import dialog.
	 */
	public static ImportType addImport(EObject object) {
		Process process = null;
		if (object instanceof Process)
			process = (Process)object;
		else
			process = (Process) ModelUtil.findNearestAncestor(object, new Class[] { Process.class });
		EStructuralFeature feature = ModelPackage.eINSTANCE.getDocumentRoot_ImportType();
		String className = null;
		ImportType newImport = null;
		Shell shell = BPMN2Editor.getActiveEditor().getSite().getShell();
		SchemaImportDialog dialog = new SchemaImportDialog(shell, SchemaImportDialog.ALLOW_JAVA);
		if (dialog.open() == Window.OK) {
			Object result[] = dialog.getResult();
			if (result.length == 1 && result[0] instanceof Class) {
				className = ((Class)result[0]).getName();
			}
		}

		if (className!=null && !className.isEmpty()) {
			Definitions definitions = ModelUtil.getDefinitions(process);
			newImport = (ImportType)ModelFactory.eINSTANCE.create(ModelPackage.eINSTANCE.getImportType());
			newImport.setName(className);
			ModelUtil.addExtensionAttributeValue(process, feature, newImport);
			
			ItemDefinition itemDef = Bpmn2ModelerFactory.create(ItemDefinition.class);
			itemDef.setItemKind(ItemKind.PHYSICAL);
			EObject structureRef = ModelUtil.createStringWrapper(className);
			itemDef.setStructureRef(structureRef);
			if (ImportUtil.findItemDefinition(definitions, itemDef)==null) {

				// create a reference to the ImportType as an extension element to the ItemDefinition
//				ImportType ref = (ImportType)ModelFactory.eINSTANCE.create(ModelPackage.eINSTANCE.getImportType());
//				((InternalEObject)ref).eSetProxyURI(EcoreUtil.getURI(newImport));
//				ModelUtil.addExtensionAttributeValue(itemDef, feature, ref);
				// Nope, don't need this! The ItemDefinition needs to stick around, otherwise the data types
				// for process variables and globals would disappear. Besides, jBPM allows data types
				// (a.k.a. ItemDefinitions) to be defined as sort of "forward references" without actual
				// knowledge of the physicial structure of the data type - these get resolved (somehow,
				// through FM maybe?) at runtime.
				// As a side note: if a type is unknown (i.e. there is no "import") then the structure
				// will be unknown in java scripts (FormalExpressions).

				// add the ItemDefinition to the root elements
				definitions.getRootElements().add(itemDef);
				ModelUtil.setID(itemDef);
			}
		}
		return newImport;
	}
	
	/**
	 * This method compiles a list of all known "data types" (a.k.a. ItemDefinitions) that
	 * are in scope for the given context element.
	 * 
	 * There are 4 different places to look:
	 * 1. the Data Type registry, which contains a list of all known "native" types, e.g. java
	 *    Strings, Integers, etc.
	 * 2. the list of ImportType extension values in the Process ancestor nearest to the given
	 *    context object.
	 * 3. the list of GlobalType extension values, also in the nearest Process ancestor
	 * 4. the list of ItemDefinitions in the root elements.
	 * 
	 * @param object - a context EObject used to search for ItemDefinitions, Globals and Imports
	 * @return a map of Strings and Objects representing the various data types
	 */
	public static Hashtable<String, Object> collectAllDataTypes(EObject object) {

		Hashtable<String,Object> choices = new Hashtable<String,Object>();

		// add all native types (as defined in the DataTypeRegistry)
		DataTypeRegistry.getFactory("dummy");
		for (Entry<String, DataTypeFactory> e : DataTypeRegistry.instance.entrySet()) {
			DataType dt = e.getValue().createDataType();
			if (dt instanceof EnumDataType || dt instanceof UndefinedDataType)
				continue;
			choices.put(dt.getStringType(),dt);
		}
		
		// add all imported data types
		EObject parent = object;
		while (parent!=null && !(parent instanceof org.eclipse.bpmn2.Process))
			parent = parent.eContainer();
		
		String s;
		List<ImportType> imports = ModelUtil.getAllExtensionAttributeValues(parent, ImportType.class);
		for (ImportType it : imports) {
			s = it.getName();
			if (s!=null && !s.isEmpty())
				choices.put(s, it);
		}
		
		// add all Global variable types
		List<GlobalType> globals = ModelUtil.getAllExtensionAttributeValues(parent, GlobalType.class);
		for (GlobalType gt : globals) {
			s = gt.getType();
			if (s!=null && !s.isEmpty())
				choices.put(s, gt);
		}

		// add all ItemDefinitions
		Definitions defs = ModelUtil.getDefinitions(object);
		List<ItemDefinition> itemDefs = ModelUtil.getAllRootElements(defs, ItemDefinition.class);
		for (ItemDefinition id : itemDefs) {
			s = ModelUtil.getStringWrapperValue(id.getStructureRef());
			if (s==null || s.isEmpty())
				s = id.getId();
			choices.put(s,id);
		}
		
		return choices;
	}
	
	/**
	 * This method returns a string representation for a "data type". This is intended to
	 * be used to interpret the various objects in the map returned by collectAllDataTypes().
	 * 
	 * @param value - one of the Object values in the map returned by collectAllDataTypes().
	 * @return a string representation of the data type
	 */
	public static String getDataType(Object value) {
		String stringValue = null;
		if (value instanceof String) {
			stringValue = (String)value;
		}
		else if (value instanceof GlobalType) {
			stringValue = ((GlobalType)value).getType();
		}
		else if (value instanceof DataType) {
			stringValue = ((DataType)value).getStringType();
		}
		else if (value instanceof ImportType) {
			stringValue = ((ImportType)value).getName();
		}
		else if (value instanceof ItemDefinition) {
			stringValue = PropertyUtil.getText((ItemDefinition)value);
		}
		return stringValue;
	}
	
	/**
	 * This method returns an ItemDefinition object for a "data type". This is intended to
	 * be used to interpret the various objects in the map returned by collectAllDataTypes().
	 * 
	 * NOTE: This method will create an ItemDefinition if it does not already exist.
	 * 
	 * @param object - a context EObject used to search for ItemDefinitions, and to create
	 *                 new ItemDefinitions if necessary.
	 * @param value - one of the Object values in the map returned by collectAllDataTypes().
	 * @return an ItemDefinition for the data type
	 */
	public static ItemDefinition getDataType(EObject context, Object value) {
		ItemDefinition itemDef = null;
		if (value instanceof String) {
			itemDef = findOrCreateItemDefinition( context, (String)value );
		}
		else if (value instanceof GlobalType) {
			itemDef = findOrCreateItemDefinition( context, ((GlobalType)value).getType() );
		}
		else if (value instanceof DataType) {
			itemDef = findOrCreateItemDefinition( context, ((DataType)value).getStringType() );
		}
		else if (value instanceof ImportType) {
			itemDef = findOrCreateItemDefinition( context, (String)value );
		}
		else if (value instanceof ItemDefinition) {
			itemDef = (ItemDefinition)value;
		}
		return itemDef;
	}
	
	public static ItemDefinition findOrCreateItemDefinition(EObject context, String structureRef) {
		ItemDefinition itemDef = null;
		Definitions definitions = ModelUtil.getDefinitions(context);
		List<ItemDefinition> itemDefs = ModelUtil.getAllRootElements(definitions, ItemDefinition.class);
		for (ItemDefinition id : itemDefs) {
			String s = ModelUtil.getStringWrapperValue(id.getStructureRef());
			if (s!=null && s.equals(structureRef)) {
				itemDef = id;
				break;
			}
		}
		if (itemDef==null)
		{
			itemDef = Bpmn2ModelerFactory.create(ItemDefinition.class);
			itemDef.setStructureRef(ModelUtil.createStringWrapper(structureRef));
			itemDef.setItemKind(ItemKind.PHYSICAL);
			definitions.getRootElements().add(itemDef);
			ModelUtil.setID(itemDef);
		}
		return itemDef;
	}
}