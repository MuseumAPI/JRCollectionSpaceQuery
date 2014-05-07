package org.jasper.collectionspace.smk.datasource;

import java.io.InputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

public class CollectionSpaceDataSource extends JsonDataSource{

	public CollectionSpaceDataSource(String location, String selectExpression) throws
			JRException {
		super(location, selectExpression);
		// TODO Auto-generated constructor stub
	}

}
