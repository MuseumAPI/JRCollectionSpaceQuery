package org.jasper.collectionspace.smk.datasource;




/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2011 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRAbstractTextDataSource;
import net.sf.jasperreports.engine.util.JsonUtil;
import net.sf.jasperreports.repo.RepositoryUtil;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.FormatUtils;
import net.sf.jasperreports.engine.util.JRDataUtils;
import net.sf.jasperreports.engine.util.JRDateLocaleConverter;
import net.sf.jasperreports.engine.util.JRFloatLocaleConverter;

import org.apache.commons.beanutils.locale.LocaleConvertUtilsBean;

/**
 * JSON data source implementation
 * 
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 * @version $Id: JsonDataSource.java 5026 2012-03-05 09:32:15Z teodord $
 */
public class JsonCSDataSource implements JRDataSource {

	// the JSON select expression that gives the nodes to iterate
	private String selectExpression;

	private Iterator<JsonNode> jsonNodesIterator;

	// the current node
	private JsonNode currentJsonNode;

	private final String PROPERTY_SEPARATOR = ".";

	private final String ARRAY_LEFT = "[";

	private final String ARRAY_RIGHT = "]";
	
	private final String ATTRIBUTE_LEFT = "(";
	
	private final String ATTRIBUTE_RIGHT = ")";
	
	// the JSON tree as it is obtained from the JSON source
	private JsonNode jsonTree;
	
	private ObjectMapper mapper;
	
	private InputStream jsonStream;
	
	private boolean toClose;
	
	public JsonCSDataSource(InputStream stream) throws JRException {
		this(stream, null);
	}
	
	public JsonCSDataSource(InputStream jsonStream, String selectExpression) throws JRException {
		try {
			this.jsonStream = jsonStream;
			this.mapper = new ObjectMapper();
			
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
			mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
			
			this.jsonTree = mapper.readTree(jsonStream);
			this.selectExpression = selectExpression;
			
			moveFirst();
		} catch (JsonProcessingException e) {
			throw new JRException(e);
		} catch (IOException e) {
			throw new JRException(e);
		}
	}


	public JsonCSDataSource(File file) throws FileNotFoundException, JRException {
		this(file, null);
	}
	

	public JsonCSDataSource(File file, String selectExpression) throws FileNotFoundException, JRException {
		this(new FileInputStream(file), selectExpression);
		
		toClose = true;
	}

	/**
	 * Creates a data source instance that reads JSON data from a given location
	 * @param location a String representing JSON data source
	 */
	public JsonCSDataSource(String location, String selectExpression) throws JRException {
		this(RepositoryUtil.getInputStream(location), selectExpression);
		
		toClose = true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jasperreports.engine.JRRewindableDataSource#moveFirst()
	 */
	public void moveFirst() throws JRException {
		if (jsonTree == null || jsonTree.isMissingNode()) {
			throw new JRException("No JSON data to operate on!");
		}

		currentJsonNode = null;
		JsonNode result = getJsonData(jsonTree, selectExpression);
		if (result != null && result.isObject()) {
//			System.out.println("result is object");
			final List<JsonNode> list = new ArrayList<JsonNode>();
			list.add(result);
			jsonNodesIterator = new Iterator<JsonNode>() {
				private int count = -1;
				public void remove() {
					list.remove(count);
				}
				
				public JsonNode next() {
					count ++;
					return list.get(count);
				}
				
				public boolean hasNext() {
					return count < list.size()-1;
				}
			};
		} else if (result != null && result.isArray()) {
//			System.out.println("result is array");
			jsonNodesIterator = result.getElements();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jasperreports.engine.JRDataSource#next()
	 */
	public boolean next() {
		if(jsonNodesIterator == null || !jsonNodesIterator.hasNext()) {
			return false;
		}
		currentJsonNode = jsonNodesIterator.next();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jasperreports.engine.JRDataSource#getFieldValue(net.sf.jasperreports.engine.JRField)
	 */
	public Object getFieldValue(JRField jrField) throws JRException 
	{
		if(currentJsonNode == null) {
			return null;
		}
		String expression = jrField.getDescription();
		if (expression == null || expression.length() == 0)
		{
			return null;
		}
		Object value = null;
		
		Class valueClass = jrField.getValueClass();
		JsonNode selectedObject = getJsonData(currentJsonNode, expression);
		
		if(Object.class != valueClass) 
		{
			if (selectedObject != null) 
			{
				try {
					if (valueClass.equals(String.class)) {
						value = selectedObject.getValueAsText();
						
					} else if (valueClass.equals(Boolean.class)) {
						value = selectedObject.getBooleanValue();
						
					} else if (Number.class.isAssignableFrom(valueClass)) {
							value = convertStringValue(selectedObject.getValueAsText(), valueClass);
							
					}
					else if (Date.class.isAssignableFrom(valueClass)) {
							value = convertStringValue(selectedObject.getValueAsText(), valueClass);
							
					} else {
						throw new JRException("Field '" + jrField.getName() + "' is of class '" + valueClass.getName() + "' and cannot be converted");
					}
				} catch (Exception e) {
					throw new JRException("Unable to get value for field '" + jrField.getName() + "' of class '" + valueClass.getName() + "'", e);
				}
			}
		}
		else
		{
			value = selectedObject;
		}
		
		return value;
	}
	
	/**
	 * Extracts the JSON nodes based on the query expression
	 * 
	 * @param rootNode
	 * @param jsonExpression
	 * @throws JRException
	 */
	protected JsonNode getJsonData(JsonNode rootNode, String jsonExpression) throws JRException {
		if (jsonExpression == null || jsonExpression.length() == 0) {
			return rootNode;
		}
		JsonNode tempNode = rootNode;
		StringTokenizer tokenizer = new StringTokenizer(jsonExpression, PROPERTY_SEPARATOR);
		
		while(tokenizer.hasMoreTokens()) {
			String currentToken = tokenizer.nextToken();
			int currentTokenLength = currentToken.length();
			int indexOfLeftSquareBracket = currentToken.indexOf(ARRAY_LEFT);

			// got Left Square Bracket - LSB
			if (indexOfLeftSquareBracket != -1) {
				// a Right Square Bracket must be the last character in the current token
				if(currentToken.lastIndexOf(ARRAY_RIGHT) != (currentTokenLength-1)) {
					throw new JRException("Invalid expression: " + jsonExpression + "; current token " + currentToken + " not ended properly");
				}
				
				// LSB not first character
				if (indexOfLeftSquareBracket > 0) {
					// extract nodes at property
					String property = currentToken.substring(0, indexOfLeftSquareBracket);
					tempNode = goDownPathWithAttribute(tempNode, property);
					
					String arrayOperators = currentToken.substring(indexOfLeftSquareBracket);
					StringTokenizer arrayOpsTokenizer = new StringTokenizer(arrayOperators,ARRAY_RIGHT);
					while(arrayOpsTokenizer.hasMoreTokens()) {
						if (!tempNode.isMissingNode() && tempNode.isArray()) {
							String currentArrayOperator = arrayOpsTokenizer.nextToken();
							tempNode = tempNode.path(Integer.parseInt(currentArrayOperator.substring(1)));
						}
					}
				} else { // LSB first character
					String arrayOperators = currentToken.substring(indexOfLeftSquareBracket);
					StringTokenizer arrayOpsTokenizer = new StringTokenizer(arrayOperators,ARRAY_RIGHT);
					while(arrayOpsTokenizer.hasMoreTokens()) {
						if (!tempNode.isMissingNode() && tempNode.isArray()) {
							String currentArrayOperator = arrayOpsTokenizer.nextToken();
							tempNode = tempNode.path(Integer.parseInt(currentArrayOperator.substring(1)));
						}
					}
				}
			} else {
				tempNode = goDownPathWithAttribute(tempNode, currentToken);
			}
		}
		
		return tempNode;
	}
	
	
	/**
	 * Extracts the JSON nodes that match the attribute expression
	 * 
	 * @param rootNode
	 * @param pathWithAttributeExpression : e.g. Orders(CustomerId == HILAA)
	 * @throws JRException
	 */
	protected JsonNode goDownPathWithAttribute(JsonNode rootNode, String pathWithAttributeExpression) throws JRException {
		// check if path has attribute selector
		int indexOfLeftRoundBracket = pathWithAttributeExpression.indexOf(ATTRIBUTE_LEFT); 
		if (indexOfLeftRoundBracket != -1) {
			
			// a Right Round Bracket must be the last character in the current pathWithAttribute
			if(pathWithAttributeExpression.indexOf(ATTRIBUTE_RIGHT) != (pathWithAttributeExpression.length() - 1)) {
				throw new JRException("Invalid attribute selection expression: " + pathWithAttributeExpression);
			}
			
			if(rootNode != null && !rootNode.isMissingNode()) {
				
				String path = pathWithAttributeExpression.substring(0, indexOfLeftRoundBracket);
				
				// an expression in a form like: attribute==value
				String attributeExpression = pathWithAttributeExpression.substring(indexOfLeftRoundBracket + 1, pathWithAttributeExpression.length() - 1);
				
				JsonNode result = null;
				if (rootNode.isObject()) {
					// select only those nodes for which the attribute expression applies
					if (!rootNode.path(path).isMissingNode()) {
						if (rootNode.path(path).isObject()) {
							if (isValidExpression(rootNode.path(path), attributeExpression)) {
								result = rootNode.path(path);
							}
						} else if (rootNode.path(path).isArray()) {
							result = mapper.createArrayNode();
							for (JsonNode node: rootNode.path(path)) {
								if (isValidExpression(node, attributeExpression)) {
									((ArrayNode)result).add(node);
								} 
							}
						}
					}
				} else if (rootNode.isArray()) {
					result = mapper.createArrayNode();
					for (JsonNode node: rootNode) {
						JsonNode deeperNode = node.path(path);
						if (!deeperNode.isMissingNode() && isValidExpression(deeperNode, attributeExpression)) {
							((ArrayNode)result).add(deeperNode);
						} 
					}
				}
				return result;
			} 
			
		} else { // path has no attribute selectors
			return goDownPath(rootNode, pathWithAttributeExpression);
		}
		return rootNode;
	}
	
	
	/**
	 * Extracts the JSON nodes under the simple path
	 * 
	 * @param rootNode
	 * @param simplePath - a simple field name, with no selection by attribute
	 */
	protected JsonNode goDownPath(JsonNode rootNode, String simplePath) {
		if(rootNode != null && !rootNode.isMissingNode()) {
			JsonNode result = null;
			if (rootNode.isObject()) {
				result = rootNode.path(simplePath);
			} else if (rootNode.isArray()) {
				result = mapper.createArrayNode();
				for (JsonNode node: rootNode) {
					JsonNode deeperNode = node.path(simplePath);
					if (!deeperNode.isMissingNode()) {
						((ArrayNode)result).add(deeperNode);
					} 
				}
			}
			return result;
		} 
		return rootNode;
	}
	
	
	/**
	 * Validates an attribute expression on a JsonNode
	 * 
	 * @param operand
	 * @param attributeExpression
	 * @throws JRException
	 */
	protected boolean isValidExpression(JsonNode operand, String attributeExpression) throws JRException{
		return JsonUtil.evaluateJsonExpression(operand, attributeExpression);
	}
	
	
	
	public static void main(String[] args) throws Exception {
	}

	public void close() {
		if (toClose) {
			try	{
				jsonStream.close();
			} catch(Exception e) {
				//nothing to do
			}
		}
	}
	
	private LocaleConvertUtilsBean convertBean;
	
	private Locale locale;
	private String datePattern;
	private String numberPattern;
	private TimeZone timeZone;

	protected Object convertStringValue(String text, Class<?> valueClass)
	{
		Object value = null;
		if (String.class.equals(valueClass))
		{
			value = text;
		}
		else if (Number.class.isAssignableFrom(valueClass))
		{
			value = getConvertBean().convert(text.trim(), valueClass, locale, numberPattern);
		}
		else if (Date.class.isAssignableFrom(valueClass))
		{
			value = getConvertBean().convert(text.trim(), valueClass, locale, datePattern);
		}
		else if (Boolean.class.equals(valueClass))
		{
			value = Boolean.valueOf(text);
		}
		return value;
	}

	protected Object convertNumber(Number number, Class<?> valueClass) throws JRException
	{
		Number value = null;
		if (valueClass.equals(Byte.class))
		{
			value = new Byte(number.byteValue());
		}
		else if (valueClass.equals(Short.class))
		{
			value = new Short(number.shortValue());
		}
		else if (valueClass.equals(Integer.class))
		{
			value = Integer.valueOf(number.intValue());
		}
		else if (valueClass.equals(Long.class))
		{
			value = new Long(number.longValue());
		}
		else if (valueClass.equals(Float.class))
		{
			value = new Float(number.floatValue());
		}
		else if (valueClass.equals(Double.class))
		{
			value = new Double(number.doubleValue());
		}
		else if (valueClass.equals(BigInteger.class))
		{
			value = BigInteger.valueOf(number.longValue());
		}
		else if (valueClass.equals(BigDecimal.class))
		{
			value = new BigDecimal(Double.toString(number.doubleValue()));
		}
		else
		{
			throw new JRException("Unknown number class " + valueClass.getName());
		}
		return value;
	}

	/**
	 * @deprecated Replaced by {@link FormatUtils#getFormattedNumber(NumberFormat, String, Class)}
	 */
	protected Number getFormattedNumber(NumberFormat numberFormat, String fieldValue, Class<?> valueClass) throws ParseException
	{
		return FormatUtils.getFormattedNumber(numberFormat, fieldValue, valueClass);
	}
	
	/**
	 * @deprecated Replaced by {@link FormatUtils#getFormattedDate(DateFormat, String, Class)}
	 */
	protected Date getFormattedDate(DateFormat dateFormat, String fieldValue, Class<?> valueClass) throws ParseException 
	{
		return FormatUtils.getFormattedDate(dateFormat, fieldValue, valueClass);
	}

	protected LocaleConvertUtilsBean getConvertBean() 
	{
		if (convertBean == null)
		{
			convertBean = new LocaleConvertUtilsBean();
			if (locale != null)
			{
				convertBean.setDefaultLocale(locale);
				convertBean.deregister();
				//convertBean.lookup();
			}
			convertBean.register(
				new JRDateLocaleConverter(timeZone), 
				java.util.Date.class,
				locale
				);
			
			// fix for https://issues.apache.org/jira/browse/BEANUTILS-351
			// remove on upgrade to BeanUtils 1.8.1
			JRFloatLocaleConverter floatConverter = new JRFloatLocaleConverter(
					locale == null ? Locale.getDefault() : locale);
			convertBean.register(floatConverter, Float.class, locale);
			convertBean.register(floatConverter, Float.TYPE, locale);
		}
		return convertBean;
	}

	/**
	 * Copy the text parsing attributes for another object.
	 * 
	 * @param textDataSource the object to copy the attributes from
	 */
	public void setTextAttributes(JRAbstractTextDataSource textDataSource)
	{
		setLocale(textDataSource.getLocale());
		setDatePattern(textDataSource.getDatePattern());
		setNumberPattern(textDataSource.getNumberPattern());
		setTimeZone(textDataSource.getTimeZone());
	}
	
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		convertBean = null;
	}
	
	public void setLocale(String locale) {
		setLocale(JRDataUtils.getLocale(locale));
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
		convertBean = null;
	}

	public String getNumberPattern() {
		return numberPattern;
	}

	public void setNumberPattern(String numberPattern) {
		this.numberPattern = numberPattern;
		convertBean = null;
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
		convertBean = null;
	}
	
	public void setTimeZone(String timeZoneId){
		setTimeZone(JRDataUtils.getTimeZone(timeZoneId));
	}
	
	



}
