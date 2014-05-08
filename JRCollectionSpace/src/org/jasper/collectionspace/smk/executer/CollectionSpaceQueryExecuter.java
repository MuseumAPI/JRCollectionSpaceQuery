package org.jasper.collectionspace.smk.executer;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.jasper.collectionspace.smk.datasource.JsonCSDataSource;


import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.query.JRQueryExecuter;

public class CollectionSpaceQueryExecuter implements JRQueryExecuter {
	String location = null;
	String selectExpression = null;
	public CollectionSpaceQueryExecuter(String location, String selectExpression)
	{
		this.location = location;
		this.selectExpression = selectExpression;
	}

	public JRDataSource createDatasource() throws JRException {
		// Creates a list of files and present them using the CustomDataSource
		InputStream stream;
		URL url;
		try {
			url = new URL(location);
			stream = url.openStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new JRException("Invalid url!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new JRException("Invalid url!");
		}

		return new JsonCSDataSource(stream, selectExpression);

	}
	public void close() {
		// Nothing to do in this implementation
	}
	public boolean cancelQuery() throws JRException {
		// Too fast to be interrupted... :-)
		return false;
	}
}