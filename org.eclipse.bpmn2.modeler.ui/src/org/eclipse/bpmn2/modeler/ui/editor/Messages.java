package org.eclipse.bpmn2.modeler.ui.editor;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.ui.editor.messages"; //$NON-NLS-1$
	public static String BPMN2Editor_Cannot_Create_Editor_Input;
	public static String BPMN2MultiPageEditor_Source_Tab;
	public static String BPMN2PersistencyBehavior_Cannot_Save_Title;
	public static String DesignEditor_Delete_Diagram_Action;
	public static String DesignEditor_Delete_Page_Title;
	public static String DesignEditor_DeletePage_Message;
	public static String DesignEditor_Diagram_Tab;
	public static String DesignEditor_Hide_Source_View_Action;
	public static String DesignEditor_Show_Source_View_Action;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
