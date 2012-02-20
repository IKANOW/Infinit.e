package com.ikanow.infinit.e.harvest.enrichment.legacy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.*;

/************************************************************
	- Simple Calais client to process file or files in a folder
	- Takes 2 arguments
		1. File or folder name to process
		2. Output folder name to store response from Calais
	- Please specify the correct web service location url for CALAIS_URL variable
	- Please adjust the values of different request parameters in the createPostMethod
	
**************************************************************/

public class HttpClientPost {

	private static final String CALAIS_URL = "http://api.opencalais.com/tag/rs/enrich";

    private File input;
    private File output;
    private HttpClient client;

    private PostMethod createPostMethod() {

        PostMethod method = new PostMethod(CALAIS_URL);

        // Set mandatory parameters
        method.setRequestHeader("x-calais-licenseID", "1232323232");

        // Set input content type
        method.setRequestHeader("Content-Type", "text/xml; charset=UTF-8");
        //method.setRequestHeader("Content-Type", "text/html; charset=UTF-8");
        //method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");

		// Set response/output format
        method.setRequestHeader("Accept", "xml/rdf");
        //method.setRequestHeader("Accept", "application/json");

        // Enable Social Tags processing
        method.setRequestHeader("enableMetadataType", "SocialTags");

        return method;
    }

	private void run() {
		try {
            if (input.isFile()) {
                postFile(input, createPostMethod());
            } else if (input.isDirectory()) {
                System.out.println("working on all files in " + input.getAbsolutePath());
                for (File file : input.listFiles()) {
                    if (file.isFile())
                        postFile(file, createPostMethod());
                    else
                        System.out.println("skipping "+file.getAbsolutePath());
                }
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private void doRequest(File file, PostMethod method) {
        try {
            int returnCode = client.executeMethod(method);
            if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                System.err.println("The Post method is not implemented by this URI");
                // still consume the response body
                method.getResponseBodyAsString();
            } else if (returnCode == HttpStatus.SC_OK) {
                System.out.println("File post succeeded: " + file);
                saveResponse(file, method);
            } else {
                System.err.println("File post failed: " + file);
                System.err.println("Got code: " + returnCode);
                System.err.println("response: "+method.getResponseBodyAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            method.releaseConnection();
        }
    }

    private void saveResponse(File file, PostMethod method) throws IOException {
        PrintWriter writer = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    method.getResponseBodyAsStream(), "UTF-8"));
            File out = new File(output, file.getName() + ".xml");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(out)));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) try {writer.close();} catch (Exception ignored) {}
        }
    }

    private void postFile(File file, PostMethod method) throws IOException {
        method.setRequestEntity(new FileRequestEntity(file, null));
        doRequest(file, method);
    }


    public static void main(String[] args) {
        verifyArgs(args);
        HttpClientPost httpClientPost = new HttpClientPost();
        httpClientPost.input = new File(args[0]);
        httpClientPost.output = new File(args[1]);
        httpClientPost.client = new HttpClient();
        httpClientPost.client.getParams().setParameter("http.useragent", "Calais Rest Client");

        httpClientPost.run();
    }

    private static void verifyArgs(String[] args) {
        if (args.length==0) {
            usageError("no params supplied");
        } else if (args.length < 2) {
            usageError("2 params are required");
        } else {
            if (!new File(args[0]).exists())
                usageError("file " + args[0] + " doesn't exist");
            File outdir = new File(args[1]);
            if (!outdir.exists() && !outdir.mkdirs())
                usageError("couldn't create output dir");
        }
    }

    private static void usageError(String s) {
        System.err.println(s);
        System.err.println("Usage: java " + (new Object() { }.getClass().getEnclosingClass()).getName() + " input_dir output_dir");
        System.exit(-1);
    }

}
