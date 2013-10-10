package infinit.e.adobe.eclipse.infinitemodule.plugin.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class AddToActionProperties 
{
	private String file_name;
	private IFile file;
	private IProgressMonitor monitor;
	private String container;
	
	public AddToActionProperties(String _filename,IFile _file, String _container)
	{
		this.file_name = _filename;
		this.file = _file;
		this.container = _container;
		//Path path = new Path(file_name);
	}
	
	/*
	 * function to update the .actionScriptProperties file to read the newly created module 
	 * as a flex module
	 */
	public void updateModulesInFile()
	{
		if(file.exists())
		{
			//variable to hold the lines being read from the file
			String line = "";
			//variable to rebuild the file with the needed line
			String rebuild = "";
		    //get the location of the file so it can be read
			IPath name = file.getLocation();
			//create the file reader
			FileReader _file;
			try {
				//instantiate the fileReader class with the location of the file
				_file = new FileReader(name.toString().replace("/", "\\"));
				//instantiate the reader to read the file
				BufferedReader reader = new BufferedReader(_file);
				
				//loop through the file til the end is reached
				while((line = reader.readLine()) != null)
				{
					//if there is no modules already add the new one
					if(line.contains("<modules/>"))
					{
						String path = getDirectory(container);
						line = "	<modules>\n" +
							   "		<module destPath=\"" + path + file_name.replace("mxml", "swf\"") + " optimize=\"false\" sourcePath=\"src/" + path + file_name + "\"/>\n" +
							   "	</modules>";
					}else
						//if there is already modules append the new one
						if(line.contains("</modules>"))
						{
							String path = getDirectory(container);
							line = "		<module destPath=\"" + path + file_name.replace("mxml","swf\"") + " optimize=\"false\" sourcePath=\"src/" + path + file_name + "\"/>\n" +
								   "	</modules>";
						}
					rebuild += line + "\n";
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//create a new stream with the new information for the file
			InputStream stream = new ByteArrayInputStream(rebuild.getBytes());
			
			try {
				if(file.exists())
				{
					//set the contents of the file with the new information that was built containing
					//the module
					file.setContents(stream, true, true, monitor);	
				}							
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				//close the stream
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void updateModulesOptimize(String application)
	{
		if(file.exists())
		{
			//variable to hold the lines being read from the file
			String line = "";
			//variable to rebuild the file with the needed line
			String rebuild = "";
		    //get the location of the file so it can be read
			IPath name = file.getLocation();
			//create the file reader
			FileReader _file;
			try {
				//instantiate the fileReader class with the location of the file
				_file = new FileReader(name.toString().replace("/", "\\"));
				//instantiate the reader to read the file
				BufferedReader reader = new BufferedReader(_file);
				
				//loop through the file til the end is reached
				while((line = reader.readLine()) != null)
				{
					//if there is no modules already add the new one
					if(line.contains("<modules/>"))
					{
						String path = getDirectory(container);
						line = "	<modules>\n" +
							   "		<module application=\"src/" + application + "\" destPath=\"" + path + file_name.replace("mxml", "swf\"") + " optimize=\"true\" sourcePath=\"src/" + path + file_name + "\"/>\n" +
							   "	</modules>";
					}else
						//if there is already modules append the new one
						if(line.contains("</modules>"))
						{
							String path = getDirectory(container);
							line = "		<module application=\"src/" + application + "\" destPath=\"" + path + file_name.replace("mxml","swf\"") + " optimize=\"true\" sourcePath=\"src/" + path + file_name + "\"/>\n" +
								   "	</modules>";
						}
					rebuild += line + "\n";
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//create a new stream with the new information for the file
			InputStream stream = new ByteArrayInputStream(rebuild.getBytes());
			
			try {
				if(file.exists())
				{
					//set the contents of the file with the new information that was built containing
					//the module
					file.setContents(stream, true, true, monitor);	
				}							
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				//close the stream
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void updateApplications(String applicationName)
	{
		if(file.exists())
		{
			//variable to hold the lines being read from the file
			String line = "";
			//variable to rebuild the file with the needed line
			String rebuild = "";
		    //get the location of the file so it can be read
			IPath name = file.getLocation();
			//create the file reader
			FileReader _file;
			try {
				//instantiate the fileReader class with the location of the file
				_file = new FileReader(name.toString().replace("/", "\\"));
				//instantiate the reader to read the file
				BufferedReader reader = new BufferedReader(_file);
				
				//loop through the file until the end is reached
				while((line = reader.readLine()) != null)
				{
					//there is always going to be at least one application already created so just append this one to the end of the list
					if(line.contains("</applications>"))
					{
							String path = getDirectory(container);
							line = "		<application path=\"" + path + applicationName + "\"/>\n" +
								   "	</applications>";
					}
					rebuild += line + "\n";
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//create a new stream with the new information for the file
			InputStream stream = new ByteArrayInputStream(rebuild.getBytes());
			
			try {
				if(file.exists())
				{
					//set the contents of the file with the new information that was built containing
					//the module
					file.setContents(stream, true, true, monitor);	
				}							
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				//close the stream
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public ArrayList<String> getApplicationsInFile()
	{
		//variable to hold applications already in the project
		ArrayList<String> apps = new ArrayList<String>();
		
		if(file.exists())
		{
			//variable to hold the lines being read from the file
			String line = "";
		    //get the location of the file so it can be read
			IPath name = file.getLocation();
			//create the file reader
			FileReader _file;
			try {
				//instantiate the fileReader class with the location of the file
				_file = new FileReader(name.toString().replace("/", "\\"));
				//instantiate the reader to read the file
				BufferedReader reader = new BufferedReader(_file);
				
				//loop through the file until the end is reached
				while((line = reader.readLine()) != null)
				{
					//get the applications that are in the project and add them to the arrayList
					if(line.contains("application path="))
					{
						String [] app = line.split("path=\"");
						apps.add(app[1].replace("\"/>", ""));
					}
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return apps;
	}
	
	public String getDirectory(String path)
	{
		//check if the directory goes deeper into the src folder
		if(path.contains("src/"))
		{
			String [] directory = path.split("src/");
			return path = directory[1] + "/";
		}else		
			return path = "";
	}
}
