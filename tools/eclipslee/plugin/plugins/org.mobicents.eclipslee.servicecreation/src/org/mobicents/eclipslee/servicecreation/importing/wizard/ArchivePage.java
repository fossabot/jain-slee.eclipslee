package org.mobicents.eclipslee.servicecreation.importing.wizard;

/*******************************************************************************
 *  @author Paolo Costa
 *	 
 *******************************************************************************/
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.IDataTransferHelpContextIds;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
//import org.eclipse.ui.internal.wizards.datatransfer.*;

/**
 *	Note that importing from .jar is identical to importing from .zip, so
 *	all references to .zip in this class are equally applicable to .jar
 *	references.
 *	This class is a elaboration of the page 1 
 *	of the base resource import-from-zip Wizard
 */

public class ArchivePage extends SLEEImportPage {

	    ILeveledImportStructureProvider currentProvider;

	    // constants
	    private static final String[] FILE_IMPORT_MASK = { "*.jar;*.zip;*.tar;*.tar.gz;*.tgz", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
	    // dialog store id constants
	    private final static String STORE_SOURCE_NAMES_ID = "SLEEImportArchivePage.STORE_SOURCE_NAMES_ID";//$NON-NLS-1$
	    private final static String STORE_OVERWRITE_EXISTING_RESOURCES_ID = "SLEEImportArchivePage.STORE_OVERWRITE_EXISTING_RESOURCES_ID";//$NON-NLS-1$
	    private final static String STORE_CREATE_CONTAINER_STRUCTURE_ID = "SLEEImportArchivePage.STORE_CREATE_CONTAINER_STRUCTURE_ID";//$NON-NLS-1$
	    private final static String STORE_SELECTED_TYPES_ID = "SLEEImportArchivePage.STORE_SELECTED_TYPES_ID"; //$NON-NLS-1$

	     


	    /**
	     *	Creates an instance of this class
	     * @param aWorkbench IWorkbench
	     * @param selection IStructuredSelection
	     */
	    public ArchivePage(IWorkbench aWorkbench, IStructuredSelection selection) {
	        super("archivePage", aWorkbench, selection); //$NON-NLS-1$
	    }
	    
	    /**
	     * Called when the user presses the Cancel button. Return a boolean
	     * indicating permission to close the wizard.
	     *
	     * @return boolean
	     */
	    public boolean cancel() {
	        clearProviderCache();
	        return true;
	    }

	    /**
	     * Clears the cached structure provider after first finalizing
	     * it properly.
	     */
	    protected void clearProviderCache() {
	        ArchiveFileManipulations.closeStructureProvider(currentProvider, getShell());
	        currentProvider= null;
	    }

	    /**
	     * Attempts to close the passed zip file, and answers a boolean indicating success.
	     */
	    protected boolean closeZipFile(ZipFile file) {
	        try {
	            file.close();
	        } catch (IOException e) {
	            displayErrorDialog(NLS.bind(DataTransferMessages.ZipImport_couldNotClose, file.getName()));
	            return false;
	        }

	        return true;
	    }
	    

	    /** (non-Javadoc)
	     * Method declared on IDialogPage.
	     */
	    public void createControl(Composite parent) {
	        super.createControl(parent);
	        //PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDataTransferHelpContextIds.ZIP_FILE_IMPORT_WIZARD_PAGE);
	        //	      TODO Add help support
	    }

	    /**
	     *	Create the options specification widgets. There is only one
	     * in this case so create no group.
	     *
	     *	@param parent org.eclipse.swt.widgets.Composite
	     */
	    protected void createOptionsGroup(Composite parent) {

	        // overwrite... checkbox
	        overwriteExistingResourcesCheckbox = new Button(parent, SWT.CHECK);
	        overwriteExistingResourcesCheckbox.setText(DataTransferMessages.FileImport_overwriteExisting);
	        overwriteExistingResourcesCheckbox.setFont(parent.getFont());
	    }

	    private boolean validateSourceFile(String fileName) {
	    	if(ArchiveFileManipulations.isTarFile(fileName)) {
	    		TarFile tarFile = getSpecifiedTarSourceFile(fileName);
	    		return (tarFile != null);
	    	}
	    	ZipFile zipFile = getSpecifiedZipSourceFile(fileName);
	    	if(zipFile != null) {
	    		ArchiveFileManipulations.closeZipFile(zipFile, getContainer()
						.getShell());
	    		return true;
	    	}
	    	return false;
	    }

	    /**
	     *	Answer a boolean indicating whether the specified source currently exists
	     *	and is valid (ie.- proper format)
	     */
	    private boolean ensureZipSourceIsValid() {
	        ZipFile specifiedFile = getSpecifiedZipSourceFile();
	        if (specifiedFile == null) {
	            return false;
	        }
	        return ArchiveFileManipulations.closeZipFile(specifiedFile,
					getContainer().getShell());
	    }

	    private boolean ensureTarSourceIsValid() {
	    	TarFile specifiedFile = getSpecifiedTarSourceFile();
	    	if( specifiedFile == null ) {
	    		return false;
	    	}
	    	return true;
	    }

	    /**
	     *	Answer a boolean indicating whether the specified source currently exists
	     *	and is valid (ie.- proper format)
	     */
	    protected boolean ensureSourceIsValid() {
	    	if (ArchiveFileManipulations.isTarFile(sourceNameField.getText())) {
	    		return ensureTarSourceIsValid();
	    	}
	    	return ensureZipSourceIsValid();
	    }
	    
	    /**
	     * The Finish button was pressed.  Try to do the required work now and answer
	     * a boolean indicating success.  If <code>false</code> is returned then the
	     * wizard will not close.
	     *
	     * @return boolean
	     */
	    public boolean finish() {
	        if (!super.finish())
	            return false;

	        clearProviderCache();
	        return true;
	    }

	    /**
	     * Returns a content provider for <code>FileSystemElement</code>s that returns 
	     * only files as children.
	     */
	    protected ITreeContentProvider getFileProvider() {
	        return new WorkbenchContentProvider() {
	            public Object[] getChildren(Object o) {
	                if (o instanceof MinimizedFileSystemElement) {
	                    MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
	                    AdaptableList l;
                    	l = element.getFiles(currentProvider);
	                    return l.getChildren(element);
	                }
	                return new Object[0];
	            }
	        };
	    }

	    /**
	     *	Answer the root FileSystemElement that represents the contents of the
	     *	currently-specified .zip file.  If this FileSystemElement is not
	     *	currently defined then create and return it.
	     */
	    protected MinimizedFileSystemElement getFileSystemTree() {
	    	if(ArchiveFileManipulations.isTarFile(sourceNameField.getText())) {    		
	        	TarFile sourceTarFile = getSpecifiedTarSourceFile();
	        	if (sourceTarFile == null) {
	                //Clear out the provider as well
	        	    currentProvider = null;
	                return null;
	        	}
		
	        	ILeveledImportStructureProvider provider = new TarLeveledStructureProvider(sourceTarFile);
	        	currentProvider = provider;
	            return selectFiles(provider.getRoot(), provider);
	    	}

	        ZipFile sourceFile = getSpecifiedZipSourceFile();
	        if (sourceFile == null) {
	            //Clear out the provider as well
	            currentProvider = null;
	            return null;
	        }

	        ILeveledImportStructureProvider provider = new ZipLeveledStructureProvider(sourceFile); 
	        currentProvider = provider;
	        return selectFiles(provider.getRoot(), provider);
	    }
	    
	    /**
	     * Returns a content provider for <code>FileSystemElement</code>s that returns 
	     * only folders as children.
	     */
	    protected ITreeContentProvider getFolderProvider() {
	        return new WorkbenchContentProvider() {
	            public Object[] getChildren(Object o) {
	                if (o instanceof MinimizedFileSystemElement) {
	                    MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
	                    AdaptableList l;
                    	l = element.getFolders(currentProvider);
	                    return l.getChildren(element);
	                }
	                return new Object[0];
	            }

	            public boolean hasChildren(Object o) {
	                if (o instanceof MinimizedFileSystemElement) {
	                    MinimizedFileSystemElement element = (MinimizedFileSystemElement) o;
	                    if (element.isPopulated())
	                        return getChildren(element).length > 0;

	                    //If we have not populated then wait until asked
	                    return true;
	                }
	                return false;
	            }
	        };
	    }
	    
	    /**
	     *	Answer the string to display as the label for the source specification field
	     */
	    protected String getSourceLabel() {
	        return DataTransferMessages.ArchiveImport_fromFile;
	    }

	    /**
	     *	Answer a handle to the zip file currently specified as being the source.
	     *	Return null if this file does not exist or is not of valid format.
	     */
	    protected ZipFile getSpecifiedZipSourceFile() {
	        return getSpecifiedZipSourceFile(sourceNameField.getText());
	    }

	    /**
	     *	Answer a handle to the zip file currently specified as being the source.
	     *	Return null if this file does not exist or is not of valid format.
	     */
	    private ZipFile getSpecifiedZipSourceFile(String fileName) {
	        if (fileName.length() == 0)
	            return null;

	        try {
	            return new ZipFile(fileName);
	        } catch (ZipException e) {
	            displayErrorDialog(DataTransferMessages.ZipImport_badFormat);
	        } catch (IOException e) {
	            displayErrorDialog(DataTransferMessages.ZipImport_couldNotRead);
	        }

	        sourceNameField.setFocus();
	        return null;
	    }

	    /**
	     *	Answer a handle to the zip file currently specified as being the source.
	     *	Return null if this file does not exist or is not of valid format.
	     */
	    protected TarFile getSpecifiedTarSourceFile() {
	        return getSpecifiedTarSourceFile(sourceNameField.getText());
	    }

	    /**
	     *	Answer a handle to the zip file currently specified as being the source.
	     *	Return null if this file does not exist or is not of valid format.
	     */
	    private TarFile getSpecifiedTarSourceFile(String fileName) {
	        if (fileName.length() == 0)
	            return null;

	        try {
	            return new TarFile(fileName);
	        } catch (TarException e) {
	        	displayErrorDialog(DataTransferMessages.TarImport_badFormat);
	        } catch (IOException e) {
	            displayErrorDialog(DataTransferMessages.ZipImport_couldNotRead);
	        }

	        sourceNameField.setFocus();
	        return null;
	    }

	    /**
	     *	Open a FileDialog so that the user can specify the source
	     *	file to import from
	     */
	    protected void handleSourceBrowseButtonPressed() {
	        String selectedFile = queryZipFileToImport();

	        if (selectedFile != null) {
	            //Be sure it is valid before we go setting any names
	            if (!selectedFile.equals(sourceNameField.getText())
						&& validateSourceFile(selectedFile)) {
	                setSourceName(selectedFile);
	                selectionGroup.setFocus();
	            }
	        }
	    }

	    /**
	     *  Import the resources with extensions as specified by the user
	     */
	    protected boolean importResources(List fileSystemObjects) {
	    	boolean result = false;

	    	if (ArchiveFileManipulations.isTarFile(sourceNameField.getText())) {
	    		if( ensureTarSourceIsValid()) {
		    		TarFile tarFile = getSpecifiedTarSourceFile();
		    		TarLeveledStructureProvider structureProvider = new TarLeveledStructureProvider(tarFile);
		    		ImportOperation operation = new ImportOperation(getContainerFullPath(),
		    				structureProvider.getRoot(), structureProvider, this,
							fileSystemObjects);
		
		    		operation.setContext(getShell());
		    		return executeImportOperation(operation);
	    		}
	    	}

	    	if(ensureZipSourceIsValid()) {
	    		ZipFile zipFile = getSpecifiedZipSourceFile();
	            ZipLeveledStructureProvider structureProvider = new ZipLeveledStructureProvider(zipFile);
				ImportOperation operation = new ImportOperation(
						getContainerFullPath(), structureProvider.getRoot(),
						structureProvider, this, fileSystemObjects);

	    		operation.setContext(getShell());
	    		result = executeImportOperation(operation);

	    		closeZipFile(zipFile);
	    	}
	    	return result;
	    }

	    /**
	     * Opens a file selection dialog and returns a string representing the
	     * selected file, or <code>null</code> if the dialog was canceled.
	     */
	    protected String queryZipFileToImport() {
	        FileDialog dialog = new FileDialog(sourceNameField.getShell(), SWT.OPEN);
	        dialog.setFilterExtensions(FILE_IMPORT_MASK);
	        dialog.setText(DataTransferMessages.ArchiveImportSource_title);

	        String currentSourceString = sourceNameField.getText();
	        int lastSeparatorIndex = currentSourceString
	                .lastIndexOf(File.separator);
	        if (lastSeparatorIndex != -1)
	            dialog.setFilterPath(currentSourceString.substring(0,
	                    lastSeparatorIndex));

	        return dialog.open();
	    }

	    /**
	     *	Repopulate the view based on the currently entered directory.
	     */
	    protected void resetSelection() {
	        super.resetSelection();
	        setAllSelections(true); //TODO only Java & XML?
	    }

	    /**
	     *	Use the dialog store to restore widget values to the values that they held
	     *	last time this wizard was used to completion
	     */
	    protected void restoreWidgetValues() {
	        IDialogSettings settings = getDialogSettings();
	        if (settings != null) {
	            String[] sourceNames = settings.getArray(STORE_SOURCE_NAMES_ID);
	            if (sourceNames == null)
	                return; // ie.- no settings stored

	            // set filenames history
	            for (int i = 0; i < sourceNames.length; i++)
	                sourceNameField.add(sourceNames[i]);

	            // radio buttons and checkboxes	
	            overwriteExistingResourcesCheckbox.setSelection(settings
	                    .getBoolean(STORE_OVERWRITE_EXISTING_RESOURCES_ID));
	        }
	    }

	    /**
	     * 	Since Finish was pressed, write widget values to the dialog store so that they
	     *	will persist into the next invocation of this wizard page.
	     *
	     *	Note that this method is identical to the one that appears in the superclass.
	     *	This is necessary because proper overriding of instance variables is not occurring.
	     */
	    public void saveWidgetValues() {
	        IDialogSettings settings = getDialogSettings();
	        if (settings != null) {
	            // update source names history
	            String[] sourceNames = settings.getArray(STORE_SOURCE_NAMES_ID);
	            if (sourceNames == null)
	                sourceNames = new String[0];

	            sourceNames = addToHistory(sourceNames, sourceNameField.getText());
	            settings.put(STORE_SOURCE_NAMES_ID, sourceNames);

	            // update specific types to import history
	            String[] selectedTypesNames = settings
	                    .getArray(STORE_SELECTED_TYPES_ID);
	            if (selectedTypesNames == null)
	                selectedTypesNames = new String[0];

	            settings.put(STORE_OVERWRITE_EXISTING_RESOURCES_ID,
	                    overwriteExistingResourcesCheckbox.getSelection());
	        }
	    }

	    /**
	     *	Answer a boolean indicating whether self's source specification
	     *	widgets currently all contain valid values.
	     */
	    protected boolean validateSourceGroup() {

	        //If there is nothing being provided to the input then there is a problem
	        if (currentProvider == null) {
	            setMessage(SOURCE_EMPTY_MESSAGE);
	            enableButtonGroup(false);
	            return false;
	        } 
	        enableButtonGroup(true);
	        return true;
	    }
	    
	    public String selfDescript() {
	    	return "Import JAIN SLEE components created externally to the EclipSLEE plugin from a JAR file.";
	    }

}