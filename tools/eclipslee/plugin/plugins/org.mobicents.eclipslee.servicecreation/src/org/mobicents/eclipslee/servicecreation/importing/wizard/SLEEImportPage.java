package org.mobicents.eclipslee.servicecreation.importing.wizard;
/*******************************************************************************
 *  @author Paolo Costa
 *	 
 *******************************************************************************/

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.dialogs.WizardResourceImportPage;
import org.eclipse.ui.internal.ide.dialogs.IElementFilter;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.IDataTransferHelpContextIds;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 *	This abstract class is the super class
 *  of the SLEE import components wizard pages
 *	This class is a elaboration of the page 1 
 *	of the base resource import-from-file-system Wizard
 */
public abstract class SLEEImportPage extends WizardResourceImportPage
        implements Listener, ISleeImportPage {
    // widgets
    protected Combo sourceNameField;
    protected Button overwriteExistingResourcesCheckbox;
    protected Button createContainerStructureButton;
    protected Button createOnlySelectedButton;
    protected Button sourceBrowseButton;
    protected Button selectTypesButton;
    protected Button selectAllButton;
    protected Button deselectAllButton;
    //A boolean to indicate if the user has typed anything
    private boolean entryChanged = false;
    private static final String SELECT_TYPES_TITLE = DataTransferMessages.DataTransfer_selectTypes;
    private static final String SELECT_ALL_TITLE = DataTransferMessages.DataTransfer_selectAll;
    private static final String DESELECT_ALL_TITLE = DataTransferMessages.DataTransfer_deselectAll;
    protected static final String SELECT_SOURCE_TITLE = DataTransferMessages.FileImport_selectSourceTitle;
    protected static final String SELECT_SOURCE_MESSAGE = DataTransferMessages.FileImport_selectSource;
    protected static final String SOURCE_EMPTY_MESSAGE = DataTransferMessages.FileImport_sourceEmpty;
    
    /**
     *	Creates an instance of this class
     */
    protected SLEEImportPage(String name, IWorkbench aWorkbench, IStructuredSelection selection) {
        super(name, selection);
        setTitle("JAIN SLEE Components");
        setDescription(selfDescript());
    }

    /**
     * Creates a new button with the given id.
     * <p>
     * The <code>Dialog</code> implementation of this framework method
     * creates a standard push button, registers for selection events
     * including button presses and registers
     * default buttons with its shell.
     * The button id is stored as the buttons client data.
     * Note that the parent's layout is assumed to be a GridLayout and 
     * the number of columns in this layout is incremented.
     * Subclasses may override.
     * </p>
     *
     * @param parent the parent composite
     * @param id the id of the button (see
     *  <code>IDialogConstants.*_ID</code> constants 
     *  for standard dialog button ids)
     * @param label the label from the button
     * @param defaultButton <code>true</code> if the button is to be the
     *   default button, and <code>false</code> otherwise
     */
    protected Button createButton(Composite parent, int id, String label,
            boolean defaultButton) {
        // increment the number of columns in the button bar
        ((GridLayout) parent.getLayout()).numColumns++;

        Button button = new Button(parent, SWT.PUSH);
        button.setFont(parent.getFont());

        GridData buttonData = new GridData(GridData.FILL_HORIZONTAL);
        button.setLayoutData(buttonData);

        button.setData(new Integer(id));
        button.setText(label);

        if (defaultButton) {
            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
            button.setFocus();
        }
        return button;
    }

    /**
     * Creates the buttons for selecting specific types or selecting all or none of the
     * elements.
     *
     * @param parent the parent control
     */
    protected final void createButtonsGroup(Composite parent) {
        // top level group
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = true;
        buttonComposite.setLayout(layout);
        buttonComposite.setFont(parent.getFont());
        GridData buttonData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL);
        buttonData.horizontalSpan = 2;
        buttonComposite.setLayoutData(buttonData);

        // types edit button
        selectTypesButton = createButton(buttonComposite,
                IDialogConstants.SELECT_TYPES_ID, SELECT_TYPES_TITLE, false);

        SelectionListener listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleTypesEditButtonPressed();
            }
        };
        selectTypesButton.addSelectionListener(listener);
        setButtonLayoutData(selectTypesButton);

        selectAllButton = createButton(buttonComposite,
                IDialogConstants.SELECT_ALL_ID, SELECT_ALL_TITLE, false);

        listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setAllSelections(true);
            }
        };
        selectAllButton.addSelectionListener(listener);
        setButtonLayoutData(selectAllButton);

        deselectAllButton = createButton(buttonComposite,
                IDialogConstants.DESELECT_ALL_ID, DESELECT_ALL_TITLE, false);

        listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setAllSelections(false);
            }
        };
        deselectAllButton.addSelectionListener(listener);
        setButtonLayoutData(deselectAllButton);

    }

    /* (non-Javadoc)
     * Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {
        super.createControl(parent);
        validateSourceGroup();
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDataTransferHelpContextIds.FILE_SYSTEM_IMPORT_WIZARD_PAGE);
        //TODO Add help support
    }

    /**
     *	Create the import options specification widgets.
     */
    protected void createOptionsGroupButtons(Group optionsGroup) {};
    
    /**
     *	Create the group for creating the root directory
     */
    protected void createRootDirectoryGroup(Composite parent) {
        Composite sourceContainerGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        sourceContainerGroup.setLayout(layout);
        sourceContainerGroup.setFont(parent.getFont());
        sourceContainerGroup.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

        Label groupLabel = new Label(sourceContainerGroup, SWT.NONE);
        groupLabel.setText(getSourceLabel());
        groupLabel.setFont(parent.getFont());

        // source name entry field
        sourceNameField = new Combo(sourceContainerGroup, SWT.BORDER);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        sourceNameField.setLayoutData(data);
        sourceNameField.setFont(parent.getFont());

        sourceNameField.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFromSourceField();
            }
        });

        sourceNameField.addKeyListener(new KeyListener() {
            /*
             * @see KeyListener.keyPressed
             */
            public void keyPressed(KeyEvent e) {
                //If there has been a key pressed then mark as dirty
                entryChanged = true;
            }

            /*
             * @see KeyListener.keyReleased
             */
            public void keyReleased(KeyEvent e) {
            }
        });

        sourceNameField.addFocusListener(new FocusListener() {
            /*
             * @see FocusListener.focusGained(FocusEvent)
             */
            public void focusGained(FocusEvent e) {
                //Do nothing when getting focus
            }

            /*
             * @see FocusListener.focusLost(FocusEvent)
             */
            public void focusLost(FocusEvent e) {
                //Clear the flag to prevent constant update
                if (entryChanged) {
                    entryChanged = false;
                    updateFromSourceField();
                }

            }
        });

        // source browse button
        sourceBrowseButton = new Button(sourceContainerGroup, SWT.PUSH);
        sourceBrowseButton.setText(DataTransferMessages.DataTransfer_browse);
        sourceBrowseButton.addListener(SWT.Selection, this);
        sourceBrowseButton.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL));
        sourceBrowseButton.setFont(parent.getFont());
        setButtonLayoutData(sourceBrowseButton);
    }

    /**
     * Update the receiver from the source name field.
     */
    private void updateFromSourceField() {

        setSourceName(sourceNameField.getText());
        //Update enablements when this is selected
        updateWidgetEnablements();
    }

    /**
     * Creates and returns a <code>FileSystemElement</code> if the specified
     * file system object merits one.  The criteria for this are:
     * Also create the children.
     */
    protected MinimizedFileSystemElement createRootElement(
            Object fileSystemObject, IImportStructureProvider provider) {
        boolean isContainer = provider.isFolder(fileSystemObject);
        String elementLabel = provider.getLabel(fileSystemObject);

        // Use an empty label so that display of the element's full name
        // doesn't include a confusing label
        MinimizedFileSystemElement dummyParent = new MinimizedFileSystemElement(
                "", null, true);//$NON-NLS-1$
        dummyParent.setPopulated();
        MinimizedFileSystemElement result = new MinimizedFileSystemElement(
                elementLabel, dummyParent, isContainer);
        result.setFileSystemObject(fileSystemObject);

        //Get the files for the element so as to build the first level
        result.getFiles(provider);

        return dummyParent;
    }

    /**
     *	Create the import source specification widgets
     */
    protected void createSourceGroup(Composite parent) {

        createRootDirectoryGroup(parent);
        createFileSelectionGroup(parent);
        createButtonsGroup(parent);
    }

    /**
     * Enable or disable the button group.
     */
    protected void enableButtonGroup(boolean enable) {
        selectTypesButton.setEnabled(enable);
        selectAllButton.setEnabled(enable);
        deselectAllButton.setEnabled(enable);
    }

    /**
     *	Answer a boolean indicating whether the specified source currently exists
     *	and is valid
     */
    protected boolean ensureSourceIsValid() {
        if (new File(getSourceDirectoryName()).isDirectory())
            return true;

        displayErrorDialog(DataTransferMessages.FileImport_invalidSource);
        sourceNameField.setFocus();
        return false;
    }

    /**
     *	Execute the passed import operation.  Answer a boolean indicating success.
     */
    protected boolean executeImportOperation(ImportOperation op) {
        initializeOperation(op);

        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            displayErrorDialog(e.getTargetException());
            return false;
        }

        IStatus status = op.getStatus();
        if (!status.isOK()) {
            ErrorDialog
                    .openError(getContainer().getShell(), DataTransferMessages.FileImport_importProblems,
                            null, // no special message
                            status);
            return false;
        }

        return true;
    }

    /**
     *	The Finish button was pressed.  Try to do the required work now and answer
     *	a boolean indicating success.  If false is returned then the wizard will
     *	not close.
     *
     * @return boolean
     */
    public boolean finish() {
        if (!ensureSourceIsValid())
            return false;

        saveWidgetValues();

        Iterator resourcesEnum = getSelectedResources().iterator();
        List fileSystemObjects = new ArrayList();
        while (resourcesEnum.hasNext()) {
            fileSystemObjects.add(((FileSystemElement) resourcesEnum.next())
                    .getFileSystemObject());
        }

        if (fileSystemObjects.size() > 0)
            return importResources(fileSystemObjects);

        MessageDialog.openInformation(getContainer().getShell(),
                DataTransferMessages.DataTransfer_information,
                DataTransferMessages.FileImport_noneSelected);

        return false;
    }

    /**
     * Returns a content provider for <code>FileSystemElement</code>s that returns 
     * only files as children.
     */
    protected abstract ITreeContentProvider getFileProvider();

    /**
     *	Answer the root FileSystemElement that represents the contents of
     *	the currently-specified source.  If this FileSystemElement is not
     *	currently defined then create and return it.
     */
    protected abstract MinimizedFileSystemElement getFileSystemTree() ;
    
    /**
     * Returns a content provider for <code>FileSystemElement</code>s that returns 
     * only folders as children.
     */
    protected abstract ITreeContentProvider getFolderProvider();
    /**
     * Returns a File object representing the currently-named source directory if
     * it exists as a valid directory, or <code>null</code> otherwise.
     */
    protected File getSourceDirectory() {
        return getSourceDirectory(this.sourceNameField.getText());
    }

    /**
     * Returns a File object representing the currently-named source directory if
     * it exists as a valid directory, or <code>null</code> otherwise.
     *
     * @param path a String not yet formatted for java.io.File compatability
     */
    protected File getSourceDirectory(String path) {
        File sourceDirectory = new File(getSourceDirectoryName(path));
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            return null;
        }

        return sourceDirectory;
    }

    /**
     *	Answer the directory name specified as being the import source.
     *	Note that if it ends with a separator then the separator is first
     *	removed so that java treats it as a proper directory
     */
    protected String getSourceDirectoryName() {
        return getSourceDirectoryName(this.sourceNameField.getText());
    }

    /**
     *	Answer the directory name specified as being the import source.
     *	Note that if it ends with a separator then the separator is first
     *	removed so that java treats it as a proper directory
     */
    protected String getSourceDirectoryName(String sourceName) {
        IPath result = new Path(sourceName.trim());

        if (result.getDevice() != null && result.segmentCount() == 0) // something like "c:"
            result = result.addTrailingSeparator();
        else
            result = result.removeTrailingSeparator();

        return result.toOSString();
    }

    /**
     *	Answer the string to display as the label for the source specification field
     */
    protected abstract String getSourceLabel();

    /**
     *	Handle all events and enablements for widgets in this dialog
     *
     * @param event Event
     */
    public void handleEvent(Event event) {
        if (event.widget == sourceBrowseButton)
            handleSourceBrowseButtonPressed();

        super.handleEvent(event);
    }

    /**
     *	Open an appropriate source browser so that the user can specify a source
     *	to import from
     */
    protected abstract void handleSourceBrowseButtonPressed();
    
    /**
     * Open a registered type selection dialog and note the selections
     * in the receivers types-to-export field.,
     * Added here so that inner classes can have access
     */
    protected void handleTypesEditButtonPressed() {
        super.handleTypesEditButtonPressed();
    }

    /**
     *  Import the resources with extensions as specified by the user
     */
    protected abstract boolean importResources(List fileSystemObjects) ;
    
    /**
     * Initializes the specified operation appropriately.
     */
    protected void initializeOperation(ImportOperation op) {
        op.setOverwriteResources(overwriteExistingResourcesCheckbox
                .getSelection());
    }

    /**
     * Returns whether the extension provided is an extension that
     * has been specified for import by the user.
     *
     * @param extension the resource name
     * @return <code>true</code> if the resource name is suitable for export based 
     *   upon its extension
     */
    protected boolean isExportableExtension(String extension) {
        if (selectedTypes == null) // ie.- all extensions are acceptable
            return true;

        Iterator itr = selectedTypes.iterator();
        while (itr.hasNext()) {
            if (extension.equalsIgnoreCase((String) itr.next()))
                return true;
        }

        return false;
    }

    /**
     *	Repopulate the view based on the currently entered directory.
     */
    protected void resetSelection() {

        MinimizedFileSystemElement currentRoot = getFileSystemTree();
        this.selectionGroup.setRoot(currentRoot);

    }

    /**
     *	Use the dialog store to restore widget values to the values that they held
     *	last time this wizard was used to completion
     */
    protected void restoreWidgetValues() {
    	super.restoreWidgetValues();
    };

    /**
     * 	Since Finish was pressed, write widget values to the dialog store so that they
     *	will persist into the next invocation of this wizard page
     */
    public void saveWidgetValues() {
    	super.restoreWidgetValues();
    }

    /**
     * Invokes a file selection operation using the specified file system and
     * structure provider.  If the user specifies files to be imported then
     * this selection is cached for later retrieval and is returned.
     */
    protected MinimizedFileSystemElement selectFiles(final Object rootFileSystemObject, final IImportStructureProvider structureProvider) {

        final MinimizedFileSystemElement[] results = new MinimizedFileSystemElement[1];

        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                //Create the root element from the supplied file system object
                results[0] = createRootElement(rootFileSystemObject,
                        structureProvider);
            }
        });

        return results[0];
    }

    /**
     * Set all of the selections in the selection group to value. Implemented here
     * to provide access for inner classes.
     * @param value boolean
     */
    protected void setAllSelections(boolean value) {
        super.setAllSelections(value);
    }

    /**
     * Sets the source name of the import to be the supplied path.
     * Adds the name of the path to the list of items in the
     * source combo and selects it.
     *
     * @param path the path to be added
     */
    protected void setSourceName(String path) {

        if (path.length() > 0) {

            String[] currentItems = this.sourceNameField.getItems();
            int selectionIndex = -1;
            for (int i = 0; i < currentItems.length; i++) {
                if (currentItems[i].equals(path))
                    selectionIndex = i;
            }
            if (selectionIndex < 0) {
                int oldLength = currentItems.length;
                String[] newItems = new String[oldLength + 1];
                System.arraycopy(currentItems, 0, newItems, 0, oldLength);
                newItems[oldLength] = path;
                this.sourceNameField.setItems(newItems);
                selectionIndex = oldLength;
            }
            this.sourceNameField.select(selectionIndex);

            resetSelection();
        }
    }

    /**
     * Update the tree to only select those elements that match the selected types
     */
    protected void setupSelectionsBasedOnSelectedTypes() {
        ProgressMonitorDialog dialog = new ProgressMonitorJobsDialog(
                getContainer().getShell());
        final Map selectionMap = new Hashtable();

        final IElementFilter filter = new IElementFilter() {

            public void filterElements(Collection files,
                    IProgressMonitor monitor) throws InterruptedException {
                if (files == null) {
                    throw new InterruptedException();
                }
                Iterator filesList = files.iterator();
                while (filesList.hasNext()) {
                    if (monitor.isCanceled())
                        throw new InterruptedException();
                    checkFile(filesList.next());
                }
            }

            public void filterElements(Object[] files, IProgressMonitor monitor)
                    throws InterruptedException {
                if (files == null) {
                    throw new InterruptedException();
                }
                for (int i = 0; i < files.length; i++) {
                    if (monitor.isCanceled())
                        throw new InterruptedException();
                    checkFile(files[i]);
                }
            }

            private void checkFile(Object fileElement) {
                MinimizedFileSystemElement file = (MinimizedFileSystemElement) fileElement;
                if (isExportableExtension(file.getFileNameExtension())) {
                    List elements = new ArrayList();
                    FileSystemElement parent = file.getParent();
                    if (selectionMap.containsKey(parent))
                        elements = (List) selectionMap.get(parent);
                    elements.add(file);
                    selectionMap.put(parent, elements);
                }
            }

        };

        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(final IProgressMonitor monitor)
                    throws InterruptedException {
                monitor.beginTask(DataTransferMessages.ImportPage_filterSelections, IProgressMonitor.UNKNOWN);
                getSelectedResources(filter, monitor);
            }
        };

        try {
            dialog.run(true, true, runnable);
        } catch (InvocationTargetException exception) {
            //Couldn't start. Do nothing.
            return;
        } catch (InterruptedException exception) {
            //Got interrupted. Do nothing.
            return;
        }
        // make sure that all paint operations caused by closing the progress 
        // dialog get flushed, otherwise extra pixels will remain on the screen until 
        // updateSelections is completed
        getShell().update();
        // The updateSelections method accesses SWT widgets so cannot be executed
        // as part of the above progress dialog operation since the operation forks
        // a new process.	
        if (selectionMap != null) {
            updateSelections(selectionMap);
        }
    }

    /* (non-Javadoc)
     * Method declared on IDialogPage. Set the selection up when it becomes visible.
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        resetSelection();
        if (visible)
            this.sourceNameField.setFocus();
    }

    /**
     * Update the selections with those in map. Implemented here to give inner class
     * visibility
     * @param map Map - key tree elements, values Lists of list elements
     */
    protected void updateSelections(Map map) {
        super.updateSelections(map);
    }

    /**
     * Check if widgets are enabled or disabled by a change in the dialog.
     * Provided here to give access to inner classes.
     */
    protected void updateWidgetEnablements() {
        super.updateWidgetEnablements();
    }

    /**
     *	Answer a boolean indicating whether self's source specification
     *	widgets currently all contain valid values.
     */
    protected boolean validateSourceGroup() {
        File sourceDirectory = getSourceDirectory();
        if (sourceDirectory == null) {
            setMessage(SOURCE_EMPTY_MESSAGE);
            enableButtonGroup(false);
            return false;
        }

        if (sourceConflictsWithDestination(new Path(sourceDirectory.getPath()))) {
            setErrorMessage(getSourceConflictMessage()); //$NON-NLS-1$
            enableButtonGroup(false);
            return false;
        }

        enableButtonGroup(true);
        return true;
    }

    /**
     * Returns whether the source location conflicts
     * with the destination resource. This will occur if
     * the source is already under the destination.
     * 
     * @param sourcePath the path to check
     * @return <code>true</code> if there is a conflict, <code>false</code> if not
     */
    protected boolean sourceConflictsWithDestination(IPath sourcePath) {

        IContainer container = getSpecifiedContainer();
        if (container == null)
            return false;
        
        IPath destinationLocation = getSpecifiedContainer().getLocation();
        if (destinationLocation != null) {
            return destinationLocation.isPrefixOf(sourcePath);
        }
        // null destination location is handled in 
        // WizardResourceImportPage 
        return false;
    }
    
    public abstract String selfDescript();

}
