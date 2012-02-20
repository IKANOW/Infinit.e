package infinit.e.adobe.eclipse.infinitemodule.plugin.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;

import infinit.e.adobe.eclipse.infinitemodule.plugin.Activator;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mxml". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class InfinteModuleWizard extends Wizard implements INewWizard {
	private InfinteModuleWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for Infint.eModuleWizard.
	 */
	public InfinteModuleWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new InfinteModuleWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		//final String width = page.getWidth();
		//final String height = page.getHeight();
		//final String application = page.getApplication();
		//final String layout = page.getModuleLayout();
		//final boolean optimize = page.getOptimization();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor, "", "", "", "", false);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(
		String containerName,
		String fileName,
		IProgressMonitor monitor,
		String width,
		String height,
		String application,
		String layout,
		boolean optimize)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		//make sure we are always at the highest level parent
		//get the parent directory 
		IContainer parent = container.getParent();
		//get the full path name of the parent directory
		IPath name = parent.getFullPath();
		//turn the path name into a string
		String directory = name.toString();
		//split the directory name so we have the root folder name
		String [] location = directory.split("/src");
		//find the root directory by tacking back on the src folder tag
		IResource res = root.findMember(location[0] + "/src");
//		if (!res.exists() || !(res instanceof IContainer)) {
//			throwCoreException("Container \"" + location[0] + "/src" + "\" does not exist.");
//		}
		//cast the resource into a container
		IContainer con = (IContainer) res;
		//get the parent directory as a type IContainer
		IContainer rootDirectory = con.getParent();		
		
		//get the path of the file to be modified
		final IFile acfile = rootDirectory.getFile(new Path("/.actionScriptProperties"));
		//create instance of the prop class to update the file with the modifications
		AddToActionProperties prop = new AddToActionProperties(fileName, acfile, containerName);
		//if optimize is true run the optimization method else run the regular method to update the modules
		if(optimize)
		{
			prop.updateModulesOptimize(application);
		}else
		{
			prop.updateModulesInFile();
		}
		
		final IFile appFile = container.getFile(new Path("InfiniteTestApplication.mxml"));
		try 
		{			
			createAssetsFolder(container,monitor);
			
			final IFile styleFile = container.getFile(new Path("com/ikanow/infinit/e/assets/styles/infiniteStyles.css"));
			final IFile fontFile1 = container.getFile(new Path("com/ikanow/infinit/e/assets/fonts/MyriadWebPro-Bold.ttf"));
			final IFile fontFile2 = container.getFile(new Path("com/ikanow/infinit/e/assets/fonts/MyriadWebPro-Italic.ttf"));
			final IFile fontFile3 = container.getFile(new Path("com/ikanow/infinit/e/assets/fonts/MyriadWebPro.ttf"));
			InputStream stream = openContentStream(width, height, layout);
			InputStream appStream = openAppContentStream(fileName,prop.getDirectory(containerName));
			InputStream styleStream = openStyleSheetStream();
			InputStream fontStream1 = openFont1Stream();
			InputStream fontStream2 = openFont2Stream();
			InputStream fontStream3 = openFont3Stream();
			//css stylesheet
			if ( styleFile.exists())
			{
				styleFile.setContents(styleStream, true, true, monitor);
			}
			else
			{
				styleFile.create(styleStream, true, monitor);
			}
			styleStream.close();
			//myriad bold
			if ( fontFile1.exists())
			{
				fontFile1.setContents(fontStream1, true, true, monitor);
			}
			else
			{
				fontFile1.create(fontStream1, true, monitor);
			}
			fontStream1.close();
			//myriad italic
			if ( fontFile2.exists())
			{
				fontFile2.setContents(fontStream2, true, true, monitor);
			}
			else
			{
				fontFile2.create(fontStream2, true, monitor);
			}
			fontStream2.close();
			//myriad regular
			if ( fontFile3.exists())
			{
				fontFile3.setContents(fontStream3, true, true, monitor);
			}
			else
			{
				fontFile3.create(fontStream3, true, monitor);
			}
			fontStream3.close();
			
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
				if(appFile.exists())
				{
					//application file that will be used to get the contents of the application				
					appFile.setContents(appStream, true, true, monitor);
				}
				else
				{
					appFile.create(appStream, true, monitor);
				}
			} 
			else 
			{
				file.create(stream, true, monitor);
				if(appFile.exists())
				{
					appFile.setContents(appStream, true, true, monitor);
				}
				else
				{
					appFile.create(appStream, true, monitor);
				}
				if(appFile != null)
				{
					prop.updateApplications(appFile.getName());
				}
			}
			stream.close();
			appStream.close();
		} catch (IOException e) {
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}
	
	private boolean createAssetsFolder(IContainer container, IProgressMonitor monitor)
	{
		String[] folderpath = {"com","ikanow","infinit","e","assets"};
		try
		{
			String fullpath = "";
			for ( String path : folderpath)
			{
				fullpath += "/" + path;
				IFolder folder = container.getFolder(new Path(fullpath));
				if ( !folder.exists())
					folder.create(true,true,monitor);
			}
			IFolder folder1 = container.getFolder(new Path(fullpath + "/styles"));
			if ( !folder1.exists())
				folder1.create(true,true,monitor);
			IFolder folder2 = container.getFolder(new Path(fullpath + "/fonts"));
			if ( !folder2.exists())
				folder2.create(true,true,monitor);
			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
		
	}
	
	/**
	 * Create an application file that the user can use to test the module when its done 
	 * being created
	 * @throws CoreException 
	 */
	
	private InputStream openAppContentStream(String moduleName, String fileLocation) throws CoreException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/InfiniteSandbox"), false);
			String appFile = convertStreamToString(inputStream);
			appFile = appFile.replace("WIDGET_NAME", moduleName.replace(".mxml", ""));
			return new ByteArrayInputStream(appFile.getBytes());
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}
	
	private String convertStreamToString(InputStream is) throws IOException
	{
		if ( null != is )
		{
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try
			{
				Reader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1)
				{
					writer.write(buffer,0,n);
				}
			}
			finally
			{
				is.close();
			}
			return writer.toString();
		}
		else
			return "";
	}
	
	/**
	 * We will initialize a file with our widget sample text
	 * loaded externally from the files folder.
	 * @throws CoreException 
	 */

	private InputStream openContentStream(String width, String height, String layout) throws CoreException {
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/InfiniteWidgetSkeleton"), false);			
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}
	
	private InputStream openStyleSheetStream() throws CoreException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/infiniteStyles.css"), false);			
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}
	
	private InputStream openFont1Stream() throws CoreException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/MyriadWebPro-Bold.ttf"), false);			
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}
	
	private InputStream openFont2Stream() throws CoreException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/MyriadWebPro-Italic.ttf"), false);			
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}
	
	private InputStream openFont3Stream() throws CoreException
	{
		InputStream inputStream = null;
		try
		{
			inputStream = FileLocator.openStream(Activator.getCurrentBundle(), new Path("files/MyriadWebPro.ttf"), false);			
		}
		catch (Exception e)
		{
			throwCoreException(e.getMessage());
		}
		return inputStream;
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "infinit.e.adobe.eclipse.infiniteModule.plugin", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}