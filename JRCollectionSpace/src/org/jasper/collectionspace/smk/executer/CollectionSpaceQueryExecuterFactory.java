package org.jasper.collectionspace.smk.executer;

import java.io.File;
import java.util.Map;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.query.JRQueryExecuter;
import net.sf.jasperreports.engine.query.JRQueryExecuterFactory;

public class CollectionSpaceQueryExecuterFactory implements JRQueryExecuterFactory {

	public Object[] getBuiltinParameters() {
		return new Object[]{};
	}

	public JRQueryExecuter createQueryExecuter(JRDataset jrd, Map map)
	throws JRException {
		String json_select= jrd.getQuery().getText(); 
		String uri = new String();
		try{
			String solr_adress = new String();
			String solr_request = new String();

			if (map.containsKey("SOLR_URL")){
				JRValueParameter paramvalue = (JRValueParameter) map.get("SOLR_URL");				
				solr_adress = String.valueOf(paramvalue.getValue()); 
			}			
			if (map.containsKey("SOLR_REQUEST")){
				JRValueParameter paramvalue = (JRValueParameter) map.get("SOLR_REQUEST");
				solr_request = String.valueOf(paramvalue.getValue());
			}

			uri = String.format("%sselect?wt=json&q=%s", solr_adress,  solr_request);			

		} catch (Exception ex)
		{
			throw new JRException(ex);
		}

		return new CollectionSpaceQueryExecuter(uri, json_select);
	}

	public boolean supportsQueryParameterType(String string) {
		return true;
	}
}