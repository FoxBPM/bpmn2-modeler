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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.preferences;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.preferences.ModelEnablementTreeEntry;
import org.eclipse.bpmn2.modeler.core.preferences.ModelEnablements;
import org.eclipse.bpmn2.modeler.core.preferences.ToolProfilesPreferencesHelper;
import org.eclipse.bpmn2.modeler.core.runtime.ModelEnablementDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor.CategoryDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor.ToolDescriptor;
import org.eclipse.bpmn2.modeler.core.runtime.ToolPaletteDescriptor.ToolPart;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.modeler.ui.Activator;
import org.eclipse.bpmn2.modeler.ui.FeatureMap;
import org.eclipse.bpmn2.modeler.ui.IConstants;
import org.eclipse.bpmn2.modeler.ui.editor.BPMN2Editor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

public class ToolProfilesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	// Page ID must be the same as defined in plugin.xml
	public static String PAGE_ID = "org.eclipse.bpmn2.modeler.Profiles"; //$NON-NLS-1$
	
	private Bpmn2Preferences preferences;
	private TargetRuntime currentRuntime;
	private Bpmn2DiagramType currentDiagramType;
	private String currentProfile;
	private final List<ModelEnablementTreeEntry> bpmnEntries = new ArrayList<ModelEnablementTreeEntry>();
	private final List<ModelEnablementTreeEntry> extensionEntries = new ArrayList<ModelEnablementTreeEntry>();
	
	private Combo cboRuntimes;
	private Combo cboDiagramTypes;
	private Combo cboProfiles;
	private Button btnUseAsDefaultProfile;
	private Button btnCreateProfile;
	private Button btnDeleteProfile;
	private TabFolder folder;
	private Button btnShowIds;
	private ModelEnablementTreeViewer bpmnTreeViewer;
	private Tree bpmnTree;
	private ModelEnablementTreeViewer extensionTreeViewer;
	private Tree extensionTree;
	private TreeViewer paletteTreeViewer;
	private Tree paletteTree;
	private Button btnAddDrawer;
	private Button btnDeleteDrawer;
	private Button btnAddTool;
	private Button btnDeleteTool;
	private Button btnEditTool;

	private static ToolPaletteDescriptor defaultToolPalette = null;
	


	// a list of ToolProfilesPreferencesHelpers, one for each permutation of Target Runtime, Diagram Type
	// and Tool Profiles defined in the Preferences. Helpers contain the Model Enablement list and are used
	// as factories for Model Enablement Tree Entries. 
	private Hashtable<TargetRuntime,
				Hashtable<Bpmn2DiagramType,
					Hashtable<String, ToolProfilesPreferencesHelper>>> helpers =
		new Hashtable<TargetRuntime,
				Hashtable<Bpmn2DiagramType,
					Hashtable<String,ToolProfilesPreferencesHelper>>>();

	/**
	 * Create the property page.
	 */
	public ToolProfilesPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
		preferences = Bpmn2Preferences.getInstance();
		ToolProfilesPreferencesHelper.setEnableIdAttribute(preferences.getShowIdAttribute());
	}

	private ToolProfilesPreferencesHelper getHelper(TargetRuntime rt, Bpmn2DiagramType diagramType, String profile) {
		Hashtable<Bpmn2DiagramType, Hashtable<String,ToolProfilesPreferencesHelper>> map1 = helpers.get(rt);
		
		if (map1==null) {
			map1 = new Hashtable<Bpmn2DiagramType, Hashtable<String,ToolProfilesPreferencesHelper>>();
			helpers.put(rt, map1);
		}
		
		Hashtable<String,ToolProfilesPreferencesHelper> map2 = map1.get(diagramType);
		if (map2==null) {
			map2 = new Hashtable<String,ToolProfilesPreferencesHelper>();
			map1.put(diagramType, map2);
		}
		
		ToolProfilesPreferencesHelper helper = map2.get(profile);
		if (helper==null) {
			helper = new ToolProfilesPreferencesHelper(rt, diagramType, profile);
			ModelEnablements me = preferences.getModelEnablements(rt, diagramType, profile);
			helper.setModelEnablements(me);
			map2.put(profile, helper);
		}
		else {
			ToolProfilesPreferencesHelper.setEnableIdAttribute(btnShowIds.getSelection());
			ModelEnablements me = helper.getModelEnablements();
			if (me==null) {
				me = preferences.getModelEnablements(rt, diagramType, profile);
			}
			helper.setModelEnablements(me);
		}

		return helper;
	}

	private boolean hasHelper(TargetRuntime rt, Bpmn2DiagramType diagramType, String profile) {
		Hashtable<Bpmn2DiagramType, Hashtable<String,ToolProfilesPreferencesHelper>> map1 = helpers.get(rt);
		
		if (map1==null) {
			map1 = new Hashtable<Bpmn2DiagramType, Hashtable<String,ToolProfilesPreferencesHelper>>();
			helpers.put(rt, map1);
		}
		
		Hashtable<String,ToolProfilesPreferencesHelper> map2 = map1.get(diagramType);
		if (map2==null) {
			map2 = new Hashtable<String,ToolProfilesPreferencesHelper>();
			map1.put(diagramType, map2);
		}
		
		ToolProfilesPreferencesHelper helper = map2.get(profile);
		if (helper==null) {
			return false;
		}


		return true;
	}
	
	/**
	 * Create contents of the property page.
	 * 
	 * @param parent
	 */
	@Override
	public Control createContents(final Composite parent) {
		
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(4, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 1, 1));

		currentRuntime = TargetRuntime.getCurrentRuntime();
		if (BPMN2Editor.getActiveEditor()!=null)
			currentDiagramType = ModelUtil.getDiagramType(BPMN2Editor.getActiveEditor().getBpmnDiagram());
		else
			currentDiagramType = Bpmn2DiagramType.PROCESS;
		currentProfile = ""; //$NON-NLS-1$
		
		final Label lblRuntime = new Label(container, SWT.NONE);
		lblRuntime.setText(Messages.ToolProfilesPreferencePage_TargetRuntime_Label);
		lblRuntime.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

		cboRuntimes = new Combo(container, SWT.READ_ONLY);
		cboRuntimes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		cboRuntimes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = cboRuntimes.getText();
				currentRuntime = (TargetRuntime) cboRuntimes.getData(s);
				fillProfilesCombo();
				fillModelEnablementTrees();
				fillPaletteTree();
			}
		});
		fillRuntimesCombo();

		final Label lblDiagramType = new Label(container, SWT.NONE);
		lblDiagramType.setText(Messages.ToolProfilesPreferencePage_DiagramType_Label);
		lblDiagramType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

		cboDiagramTypes = new Combo(container, SWT.READ_ONLY);
		cboDiagramTypes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		cboDiagramTypes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = cboDiagramTypes.getText();
				currentDiagramType = (Bpmn2DiagramType) cboDiagramTypes.getData(s);
				fillProfilesCombo();
				fillModelEnablementTrees();
				fillPaletteTree();
			}
		});
		fillDiagramTypesCombo();

		final Label lblProfile = new Label(container, SWT.NONE);
		lblProfile.setText(Messages.ToolProfilesPreferencePage_ToolProfile_Label);
		lblProfile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 3, 1));

		cboProfiles = new Combo(container, SWT.READ_ONLY);
		cboProfiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cboProfiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentProfile = cboProfiles.getText();
				fillModelEnablementTrees();
				fillPaletteTree();
			}
		});
		
		
		Composite buttonContainer = new Composite(container, SWT.NULL);
		buttonContainer.setLayout(new GridLayout(4, false));
		buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 4, 1));
		
		btnUseAsDefaultProfile = new Button(buttonContainer, SWT.CHECK);
		btnUseAsDefaultProfile.setText(Messages.ToolProfilesPreferencePage_SetDefaultProfile_Button);
		btnUseAsDefaultProfile.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		
		final Label lblFiller = new Label(buttonContainer, SWT.NONE);
		lblFiller.setText(""); //$NON-NLS-1$
		lblFiller.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		btnCreateProfile = new Button(buttonContainer, SWT.PUSH);
		btnCreateProfile.setText(Messages.ToolProfilesPreferencePage_NewProfile_Button);
		btnCreateProfile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnCreateProfile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreateProfileDialog dlg = new CreateProfileDialog(parent.getShell());
				if (dlg.open() == Window.OK) {
					currentProfile = dlg.getValue();
					preferences.createToolProfile(currentRuntime, currentDiagramType, currentProfile);
					preferences.setDefaultToolProfile(currentRuntime, currentDiagramType, currentProfile);
					if (dlg.getCopyProfile()!=null) {
						// make a copy of an existing Tool Profile: get the Model Enablements to be copied
						Bpmn2DiagramType saveDiagramType = currentDiagramType;
						currentDiagramType = dlg.getCopyDiagramType();
						currentProfile = dlg.getCopyProfile();
						ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
						ModelEnablements copyMe = helper.getModelEnablements();

						// create a helper for the new Tool Profile
						currentProfile = dlg.getValue();
						currentDiagramType = saveDiagramType;
						helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
						
						// and copy the ModelEnablements into it
						helper.copyModelEnablements(copyMe);
						preferences.setModelEnablements(currentRuntime, currentDiagramType, currentProfile, helper.getModelEnablements());
					}					
					currentProfile = dlg.getValue();
					fillProfilesCombo();
					fillModelEnablementTrees();
					fillPaletteTree();
				}
			}
		});
		
		btnDeleteProfile = new Button(buttonContainer, SWT.PUSH);
		btnDeleteProfile.setText(Messages.ToolProfilesPreferencePage_DeleteProfile_Button);
		btnDeleteProfile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnDeleteProfile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openQuestion(getShell(),
						Messages.ToolProfilesPreferencePage_DeleteProfile_Title,
						Messages.ToolProfilesPreferencePage_DeleteProfile_Message)) {
					
					preferences.deleteToolProfile(currentRuntime, currentDiagramType, currentProfile);
					fillProfilesCombo();
					fillModelEnablementTrees();
					fillPaletteTree();
				}
			}
		});

		fillProfilesCombo();
		
		// Create a Tab Folder for the Model Enablements Trees and the Tool Palette definition
		folder = new TabFolder(container, SWT.NONE);
		folder.setBackground(parent.getBackground());
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		folder.setLayout(layout);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		folder.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				fillPaletteTree();
			}
			
		});

		final TabItem elementsTab = new TabItem(folder, SWT.NONE);
		elementsTab.setText(Messages.ToolProfilesPreferencePage_EnabledElements_Tab);
		final TabItem paletteTab = new TabItem(folder, SWT.NONE);
		paletteTab.setText(Messages.ToolProfilesPreferencePage_ToolPalette_Tab);

		final Composite elementsContainer = new Composite(folder, SWT.NONE);
		elementsContainer.setLayout(new GridLayout(2, false));
		elementsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		
		btnShowIds = new Button(elementsContainer, SWT.CHECK);
		btnShowIds.setText(Messages.ToolProfilesPreferencePage_ShowID_Button);
		btnShowIds.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnShowIds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ToolProfilesPreferencesHelper.setEnableIdAttribute(btnShowIds.getSelection());
				getHelper(currentRuntime, currentDiagramType, currentProfile);
				fillModelEnablementTrees();
				fillPaletteTree();
			}
		});
		btnShowIds.setSelection(preferences.getShowIdAttribute());
		
		// Create Checkbox Tree Viewers for standard BPMN 2.0 elements and any extension elements
		// this listener updates the helper's Model Enablements as changes are made in the Tree Viewers
		ICheckStateListener checkStateListener = new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked = event.getChecked();
				Object element = event.getElement();
				if (element instanceof ModelEnablementTreeEntry) {
					ModelEnablementTreeEntry entry = (ModelEnablementTreeEntry)element;
					ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
					helper.setEnabled(entry, checked);
				}
			}
		};
		
		bpmnTreeViewer = new ModelEnablementTreeViewer(elementsContainer, Messages.ToolProfilePreferencePage_Standard_Elements_Label);
		bpmnTree = bpmnTreeViewer.getTree();
		bpmnTreeViewer.addCheckStateListener(checkStateListener);

		extensionTreeViewer = new ModelEnablementTreeViewer(elementsContainer, Messages.ToolProfilePreferencePage_Extension_Elements_Label);
		extensionTree = extensionTreeViewer.getTree();
		extensionTreeViewer.addCheckStateListener(checkStateListener);

		// Create a Tree control for Tool Palette definition widgets
		final Composite paletteContainer = new Composite(folder, SWT.NONE);
		paletteContainer.setLayout(new GridLayout(2, false));
		paletteContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		paletteTreeViewer = new TreeViewer(paletteContainer, SWT.BORDER);
		paletteTree = paletteTreeViewer.getTree();
		paletteTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		final Composite paletteButtonsContainer = new Composite(paletteContainer, SWT.NONE);
		paletteButtonsContainer.setLayout(new GridLayout(1, false));
		GridData data = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1);
		data.exclude = true;
		paletteButtonsContainer.setLayoutData(data);
		paletteButtonsContainer.setVisible(false);
		
		btnAddDrawer = new Button(paletteButtonsContainer, SWT.PUSH);
		btnAddDrawer.setText(Messages.ToolProfilesPreferencePage_AddDrawer_Button);
		btnAddDrawer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnAddDrawer.setEnabled(false);
		
		btnDeleteDrawer = new Button(paletteButtonsContainer, SWT.PUSH);
		btnDeleteDrawer.setText(Messages.ToolProfilesPreferencePage_DeleteDrawer_Button);
		btnDeleteDrawer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnDeleteDrawer.setEnabled(false);
		
		btnAddTool = new Button(paletteButtonsContainer, SWT.PUSH);
		btnAddTool.setText(Messages.ToolProfilesPreferencePage_AddTool_Button);
		btnAddTool.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnAddTool.setEnabled(false);
		
		btnDeleteTool = new Button(paletteButtonsContainer, SWT.PUSH);
		btnDeleteTool.setText(Messages.ToolProfilesPreferencePage_DeleteTool_Button);
		btnDeleteTool.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnDeleteTool.setEnabled(false);
		
		btnEditTool = new Button(paletteButtonsContainer, SWT.PUSH);
		btnEditTool.setText(Messages.ToolProfilesPreferencePage_EditTool_Button);
		btnEditTool.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnEditTool.setEnabled(false);
		
		
		// define the Tabs
		elementsTab.setControl(elementsContainer);
		elementsContainer.setBackground(parent.getBackground());
		paletteTab.setControl(paletteContainer);
		paletteContainer.setBackground(parent.getBackground());
		
		// Create the Import/Export buttons below the Tab Folder
		Composite importExportButtons = new Composite(container, SWT.NONE);
		importExportButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 4, 1));
		importExportButtons.setLayout(new FillLayout());

		Button btnImportProfile = new Button(importExportButtons, SWT.NONE);
		btnImportProfile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.NULL);
				String path = dialog.open();
				if (path != null) {
					try {
						bpmnEntries.clear();
						extensionEntries.clear();
						getHelper(currentRuntime, currentDiagramType, currentProfile).importProfile(path);
						fillModelEnablementTrees();
						fillPaletteTree();
					} catch (Exception e1) {
						Activator.showErrorWithLogging(e1);
					}
				}
			}
		});
		btnImportProfile.setText(Messages.ToolProfilePreferencePage_Import);

		Button btnExportProfile = new Button(importExportButtons, SWT.NONE);
		btnExportProfile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.SAVE);
				String path = dialog.open();
				if (path != null) {
					try {
						getHelper(currentRuntime, currentDiagramType, currentProfile).exportProfile(path);
					} catch (Exception e1) {
						Activator.showErrorWithLogging(e1);
					}
				}
			}
		});
		btnExportProfile.setText(Messages.ToolProfilePreferencePage_Export);

		fillModelEnablementTrees();
		fillPaletteTree();
		
		bpmnTree.setEnabled(true);
		extensionTree.setEnabled(true);

		return container;
	}

	private void fillRuntimesCombo() {
		int i = 0;
		for (TargetRuntime r : TargetRuntime.getAllRuntimes()) {
			cboRuntimes.add(r.getName());
			cboRuntimes.setData(r.getName(), r);
			if (r == currentRuntime)
				cboRuntimes.select(i);
			++i;
		}
	}
	
	private void fillDiagramTypesCombo() {
		int i = 0;
		for (Bpmn2DiagramType t : Bpmn2DiagramType.values()) {
			cboDiagramTypes.add(t.toString());
			cboDiagramTypes.setData(t.toString(), t);
			if (t==currentDiagramType)
				cboDiagramTypes.select(i);
			++i;
		}
	}
	
	private void fillProfilesCombo() {
		if (cboProfiles!=null) {
			int i = 0;
			int iSelected = -1;
			currentProfile = preferences.getDefaultToolProfile(currentRuntime, currentDiagramType);
	
			cboProfiles.removeAll();
			for (String profile : preferences.getAllToolProfiles(currentRuntime, currentDiagramType)) {
				String text = profile;
				if (text==null || text.isEmpty())
					text = Messages.ToolProfilePreferencePage_Unnamed + (i+1);
				cboProfiles.add(text);
				if (iSelected<0 && (currentProfile!=null && currentProfile.equals(profile)))
					cboProfiles.select(iSelected = i);
				++i;
			}
			if (btnDeleteProfile!=null)
				btnDeleteProfile.setEnabled(cboProfiles.getItemCount()>0);

		}
	}
	
	private void fillModelEnablementTrees() {
		loadModelEnablements();
		bpmnTreeViewer.setInput(bpmnEntries);
		extensionTreeViewer.setInput(extensionEntries);

		if (TargetRuntime.DEFAULT_RUNTIME_ID.equals(currentRuntime.getId())) {
			extensionTreeViewer.setVisible(false);
		}
		else {
			extensionTreeViewer.setVisible(true);
		}
	}
	
	private boolean isEnabled(ToolDescriptor td) {
		ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
		for (ToolPart tp : td.getToolParts()) {
			if (!isEnabled(helper, tp))
				return false;
		}
		return true;
	}
	
	private boolean isEnabled(ToolPart tp) {
		ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
		return isEnabled(helper,tp);
	}
	
	private boolean isEnabled(ToolProfilesPreferencesHelper helper, ToolPart tp) {
		String name = tp.getName();
		if (name!=null && !name.isEmpty()) {
			if (!helper.isEnabled(name))
				return false;
		}
		for (ToolPart child : tp.getChildren()) {
			if (!isEnabled(helper,child))
				return false;
		}
		return true;
	}
	
	private void fillPaletteTree() {
		if (paletteTreeViewer==null)
			return;
		
		loadPalette();
		ToolPaletteDescriptor toolPaletteDescriptor = currentRuntime.getToolPalette(currentDiagramType, currentProfile);
		if (toolPaletteDescriptor==null)
			toolPaletteDescriptor = defaultToolPalette;
		
		if (paletteTreeViewer.getContentProvider()==null) {
			paletteTreeViewer.setContentProvider(new ITreeContentProvider() {

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}

				@Override
				public void dispose() {
				}

				@Override
				public boolean hasChildren(Object element) {
					return getChildren(element) != null;
				}

				@Override
				public Object getParent(Object element) {
					if (element instanceof ToolPaletteDescriptor) {
						return null;
					}
					else if (element instanceof CategoryDescriptor) {
						return ((CategoryDescriptor) element).getParent();
					}
					else if (element instanceof ToolDescriptor) {
						return ((ToolDescriptor) element).getParent();
					}
					else if (element instanceof ToolPart) {
						return ((ToolPart) element).getParent();
					}
					return null;
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return getChildren(inputElement);
				}

				@Override
				public Object[] getChildren(Object element) {
					if (element instanceof ToolPaletteDescriptor) {
						List<CategoryDescriptor> kids = new ArrayList<CategoryDescriptor>();
						for (CategoryDescriptor cd : ((ToolPaletteDescriptor) element).getCategories()) {
							if (cd.getName()==null && ToolPaletteDescriptor.DEFAULT_PALETTE_ID.equals(cd.getId())) {
								kids.addAll(defaultToolPalette.getCategories());
							}
							else if (cd.getFromPalette()!=null) {
								for (TargetRuntime rt : TargetRuntime.getAllRuntimes()) {
									for (ToolPaletteDescriptor td : rt.getToolPalettes()) {
										if (cd.getFromPalette().equals(td.getId())) {
											for (CategoryDescriptor cd2 : td.getCategories()) {
												if (cd.getId().equals(cd2.getId())) {
													kids.add(cd2);
													break;
												}
											}
										}
									}
								}
							}
							else
								kids.add(cd);
						}
						return kids.toArray();
					}
					else if (element instanceof CategoryDescriptor) {
						return ((CategoryDescriptor) element).getTools().toArray();
					}
//					else if (element instanceof ToolDescriptor) {
//						return ((ToolDescriptor) element).getToolParts().toArray();
//					}
//					else if (element instanceof ToolPart) {
//						return ((ToolPart) element).getChildren().toArray();
//					}
					return null;
				}
			});

			paletteTreeViewer.setLabelProvider(new ILabelProvider() {
				@Override
				public void removeListener(ILabelProviderListener listener) {
				}

				@Override
				public boolean isLabelProperty(Object element, String property) {
					return false;
				}

				@Override
				public void dispose() {

				}

				@Override
				public void addListener(ILabelProviderListener listener) {
				}

				@Override
				public Image getImage(Object element) {
					if (element instanceof CategoryDescriptor) {
						CategoryDescriptor cd = (CategoryDescriptor) element;
						int enabled = 0;
						for (ToolDescriptor td : cd.getTools()) {
							if (isEnabled(td))
								++enabled;
						}
						if (enabled==0)
							return Activator.getDefault().getImage(IConstants.ICON_FOLDER_DISABLED);
						return Activator.getDefault().getImage(IConstants.ICON_FOLDER);
					}
					else if (element instanceof ToolDescriptor) {
						ToolDescriptor td = (ToolDescriptor) element;
						if (!isEnabled(td))
							return Activator.getDefault().getImage(IConstants.ICON_DISABLED);
						List<ToolPart> tps = td.getToolParts();
						if (!tps.isEmpty()) {
							if (tps.size()>2) {
								return Activator.getDefault().getImage(IConstants.ICON_PROCESS);
							}
							ToolPart tp = tps.get(0);
							String name = "16/" + tp.getName() + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
							if (!tp.getChildren().isEmpty()) {
								name = "16/" + tp.getChildren().get(0).getName() + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
							}
							return Activator.getDefault().getImage(name);
						}
					}
					else if (element instanceof ToolPart) {
//						ToolPart tp = (ToolPart) element;
//						if (!isEnabled(tp))
//							return Activator.getDefault().getImage(IConstants.ICON_SCREW_DISABLED);
//						if (tp.getProperties().get(ToolPaletteDescriptor.TOOLPART_OPTIONAL)!=null)
//							return Activator.getDefault().getImage(IConstants.ICON_NUT);
//						return Activator.getDefault().getImage(IConstants.ICON_SCREW);
					}
					return null;
				}

				@Override
				public String getText(Object element) {
					if (element instanceof ToolPaletteDescriptor) {
						return ((ToolPaletteDescriptor) element).getProfiles().get(0);
					}
					else if (element instanceof CategoryDescriptor) {
						CategoryDescriptor cd = (CategoryDescriptor) element;
						if (cd.getFromPalette()!=null)
							return cd.getFromPalette();
						if (cd.getName()==null)
							return cd.getId();
						return cd.getName();
					}
					else if (element instanceof ToolDescriptor) {
						return ((ToolDescriptor) element).getName();
					}
					else if (element instanceof ToolPart) {
						ToolPart tp = (ToolPart) element;
						String props = ""; //$NON-NLS-1$
						for (Entry<String, String> entry : tp.getProperties().entrySet()) {
							if (props.isEmpty())
								props = entry.getKey() + "=" + entry.getValue(); //$NON-NLS-1$
							else
								props += "," + entry.getKey() + "=" + entry.getValue(); //$NON-NLS-1$ //$NON-NLS-2$
						}
						return tp.getName() + "[" + props + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return ""; //$NON-NLS-1$
				}
			});
		}
		paletteTreeViewer.setInput(toolPaletteDescriptor);
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		String path = Bpmn2Preferences.getToolProfilePath(currentRuntime, currentDiagramType);
		preferences.setToDefault(path);
		path = Bpmn2Preferences.getModelEnablementsPath(currentRuntime, currentDiagramType, currentProfile);
		preferences.setToDefault(path);
		fillProfilesCombo();
		// force the helper's Model Enablements to be reloaded from default preferences
		ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);
		helper.setModelEnablements(null);
		fillModelEnablementTrees();
		fillPaletteTree();
	}

	private void loadModelEnablements() {
		ToolProfilesPreferencesHelper helper = getHelper(currentRuntime, currentDiagramType, currentProfile);

		loadModelEnablements(helper, bpmnEntries, null, null);
		ModelEnablementDescriptor med = currentRuntime.getModelEnablements(currentDiagramType, currentProfile);
		if (med!=null)
			loadModelEnablements(helper, extensionEntries, bpmnEntries, med);
	}
	
	private Object[] loadModelEnablements(ToolProfilesPreferencesHelper helper, List<ModelEnablementTreeEntry> entries, List<ModelEnablementTreeEntry> bpmnEntries, ModelEnablementDescriptor med) {
		entries.clear();
		if (med!=null)
			entries.addAll(helper.getAllExtensionElements(med, bpmnEntries));
		else
			entries.addAll(helper.getAllElements());

		ArrayList<ModelEnablementTreeEntry> enabled = new ArrayList<ModelEnablementTreeEntry>();
		for (ModelEnablementTreeEntry entry : entries) {
			if (entry.getEnabled()) {
				enabled.add(entry);
			}
			ArrayList<ModelEnablementTreeEntry> children = entry.getChildren();
			for (ModelEnablementTreeEntry t : children) {
				if (t.getEnabled()) {
					enabled.add(t);
				}
			}
		}
		return enabled.toArray();
	}

	private void loadPalette() {
		if (defaultToolPalette == null) {
			defaultToolPalette = new ToolPaletteDescriptor();
			
			CategoryDescriptor cd;
			
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_Connectors_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.CONNECTORS) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_Tasks_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.TASKS) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_Gateways_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.GATEWAYS) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_Events_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.EVENTS) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_EventDefinitions_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.EVENT_DEFINITIONS) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_DataItems_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.DATA) {
				loadCategory(cd,c);
			}
			cd = new CategoryDescriptor(defaultToolPalette,null,Messages.ToolProfilesPreferencePage_Other_Category,null,null);
			defaultToolPalette.getCategories().add(cd);
			for (Class c : FeatureMap.OTHER) {
				loadCategory(cd,c);
			}
		}
	}
	
	private void loadCategory(CategoryDescriptor cd, Class c) {
		ToolDescriptor td = new ToolDescriptor(cd, null, ModelUtil.toDisplayName(c.getSimpleName()),null,null);
		cd.getTools().add(td);
		ToolPart tp = new ToolPart(td,c.getSimpleName());
		td.getToolParts().add(tp);
	}
	
	@Override
	public boolean performOk() {
		setErrorMessage(null);
		try {
			for (TargetRuntime rt : TargetRuntime.getAllRuntimes()) {
				for (Bpmn2DiagramType diagramType : Bpmn2DiagramType.values()) {
					for (String profile : preferences.getAllToolProfiles(rt, diagramType)) {
						if (hasHelper(rt, diagramType, profile)) {
							ToolProfilesPreferencesHelper helper = getHelper(rt, diagramType, profile);
							preferences.setModelEnablements(rt, diagramType, profile, helper.getModelEnablements());
						}
					}
				}
			}

			preferences.setShowIdAttribute(btnShowIds.getSelection());
			if (btnUseAsDefaultProfile.getSelection())
				preferences.setDefaultToolProfile(currentRuntime, currentDiagramType, currentProfile);
			preferences.setRuntime(currentRuntime);
			preferences.flush();
		} catch (BackingStoreException e) {
			Activator.showErrorWithLogging(e);
		}
		return true;
	}
	
	private class CreateProfileDialog extends InputDialog {

		private String copySelection = null;
		
		public CreateProfileDialog(Shell parentShell) {
			super(parentShell,
					Messages.ToolProfilesPreferencePage_CreateProfile_Title,
					NLS.bind(Messages.ToolProfilesPreferencePage_CreateProfile_Message, currentRuntime.getName()),
					"", //$NON-NLS-1$
					new IInputValidator() {

				@Override
				public String isValid(String newText) {
					if (newText==null || newText.isEmpty())
						return Messages.ToolProfilesPreferencePage_Profile_Empty;
					for (String p : cboProfiles.getItems()) {
						if (newText.equals(p))
							return NLS.bind(Messages.ToolProfilesPreferencePage_Profile_Duplicate,p);
					}
					return null;
				}
				
			});
		}

		public String getCopyProfile() {
			if (copySelection!=null && copySelection.contains("/")) { //$NON-NLS-1$
				int i = copySelection.indexOf("/"); //$NON-NLS-1$
				return copySelection.substring(i+1);
			}
			return null;
		}

		public Bpmn2DiagramType getCopyDiagramType() {
			if (copySelection!=null && copySelection.contains("/")) { //$NON-NLS-1$
				int i = copySelection.indexOf("/"); //$NON-NLS-1$
				String s = copySelection.substring(0,i);
				return Bpmn2DiagramType.fromString(s);
			}
			return null;
		}
		
		@Override
	    protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);

			Composite container = new Composite(composite, SWT.NULL);
			container.setLayout(new GridLayout(2, false));
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
			
			final Button btnCopy = new Button(container, SWT.CHECK);
			btnCopy.setText(Messages.ToolProfilesPreferencePage_CopyProfile_Button);
			btnCopy.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

			final Combo cboCopy = new Combo(container, SWT.READ_ONLY);
			cboCopy.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			cboCopy.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					copySelection = cboCopy.getText();
				}
			});
			
			for (Bpmn2DiagramType diagramType : Bpmn2DiagramType.values()) {
				for (String profile : preferences.getAllToolProfiles(currentRuntime, diagramType)) {
					String key = diagramType + "/" + profile; //$NON-NLS-1$
					cboCopy.add(key);
				}
			}
			cboCopy.setEnabled(false);
			
			btnCopy.setEnabled(cboCopy.getItemCount()>0);
			btnCopy.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					cboCopy.setEnabled(btnCopy.getSelection());
					if (!btnCopy.getSelection())
						copySelection = null;
				}
			});
			
			return composite;
		}
	}
	
	/**
	 * The "Show ID Attribute" preference is shared with the Behavior page.
	 * 
	 * @return
	 */
	public boolean getShowIdAttribute() {
		if (btnShowIds!=null)
			return btnShowIds.getSelection();
		return preferences.getShowIdAttribute();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && btnShowIds!=null) {
			// copy the "Show ID Attribute" setting from the Behavior page if it is active
			Bpmn2EditorBehaviorPreferencePage page = (Bpmn2EditorBehaviorPreferencePage) Bpmn2HomePreferencePage.getPage(getContainer(), Bpmn2EditorBehaviorPreferencePage.PAGE_ID);
			if (page!=null)
				btnShowIds.setSelection(page.getShowIdAttribute());
		}
	}
}
