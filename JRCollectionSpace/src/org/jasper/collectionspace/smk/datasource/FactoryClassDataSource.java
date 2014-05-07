package org.jasper.collectionspace.smk.datasource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;

public class FactoryClassDataSource {

	/**
	 * Factory Class responsible for setting the JRdatasource.
	 * @return
	 * @throws JRException 
	 * @throws IOException 
	 */
	
	public static JRDataSource generateDS() throws JRException, IOException{
		InputStream stream;
		URL url = new URL("http://solr.smk.dk:8080/solr/prod_all_dk/select?q=id_s%3AKMS8402&wt=json&indent=true");
		stream = url.openStream();
		//stream = new FileInputStream("C://Users//sbr//Desktop//temp//KMS1.json");
		//String location  = "C://Users//sbr//Desktop//temp//KMS8402.json";
		JRDataSource csdatasource = new JsonCSDataSource(stream, null); 
		return csdatasource;
	}
	
	
}