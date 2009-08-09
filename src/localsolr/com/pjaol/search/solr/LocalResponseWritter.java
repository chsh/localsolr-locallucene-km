package com.pjaol.search.solr;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Searcher;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.XML;
import org.apache.solr.request.QueryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
/**
 * @deprecated
 * @author pjaol
 *
 */
@Deprecated
public class LocalResponseWritter implements QueryResponseWriter {

	public static float CURRENT_VERSION = 2.2f;

	final int version = 1;

	private Set<String> defaultFieldList;

	private Writer writer;

	private Map distances;
	
	private SolrIndexSearcher searcher;

	//
	// static thread safe part
	//
	private static final char[] XML_START1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			.toCharArray();

	private static final char[] XML_STYLESHEET = "<?xml-stylesheet type=\"text/xsl\" href=\"/admin/"
			.toCharArray();

	private static final char[] XML_STYLESHEET_END = ".xsl\"?>\n".toCharArray();

	private static final char[] XML_START2_SCHEMA = ("<response xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ " xsi:noNamespaceSchemaLocation=\"http://pi.cnet.com/cnet-search/response.xsd\">\n")
			.toCharArray();

	private static final char[] XML_START2_NOSCHEMA = ("<response>\n")
			.toCharArray();

	private final ArrayList tlst = new ArrayList();
	
	private IndexSchema schema ; // needed to write fields of docs
	
	public LocalResponseWritter(Writer writer, IndexSchema schema, Searcher searcher, SolrQueryResponse rsp) {
		this.writer = writer;
		this.searcher = (SolrIndexSearcher)searcher;
		this.schema = schema;
		
	}

	public LocalResponseWritter() {
		// NOOP required for initialization
	}

	public String getContentType(SolrQueryRequest request,
			SolrQueryResponse response) {

		return CONTENT_TYPE_XML_UTF8;
	}

	public void init(NamedList args) {
		// TODO Auto-generated method stub

	}
	
	 private static final Comparator fieldnameComparator = new Comparator() {
		    public int compare(Object o, Object o1) {
		      Fieldable f1 = (Fieldable)o; Fieldable f2 = (Fieldable)o1;
		      int cmp = f1.name().compareTo(f2.name());
		      return cmp;
		      // note - the sort is stable, so this should not have affected
				// the ordering
		      // of fields with the same name w.r.t eachother.
		    }
		  };

	public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp)
			throws IOException {

		String ver = req.getParam("version");

		writer.write(XML_START1);

		String stylesheet = req.getParam("stylesheet");
		if (stylesheet != null && stylesheet.length() > 0) {
			writer.write(XML_STYLESHEET);
			writer.write(stylesheet);
			writer.write(XML_STYLESHEET_END);
		}

		String noSchema = req.getParam("noSchema");
		// todo - change when schema becomes available?
		if (false && noSchema == null)
			writer.write(XML_START2_SCHEMA);
		else
			writer.write(XML_START2_NOSCHEMA);

		// create an instance for each request to handle
		// non-thread safe stuff (indentation levels, etc)
		// and to encapsulate writer, schema, and searcher so
		// they don't have to be passed around in every function.
		//
		LocalResponseWritter xw = new LocalResponseWritter(writer,req.getSchema(), req
				.getSearcher(), rsp);
		xw.defaultFieldList = rsp.getReturnFields();

		
		String indent = req.getParam("indent");

		// dump response values
		NamedList lst = rsp.getValues();
		int sz = lst.size();
		int start = 0;

		// special case the response header if the version is 2.1 or less
		if (xw.version <= 2100 && sz > 0) {
			Object header = lst.getVal(0);
			if (header instanceof NamedList
					&& "responseHeader".equals(lst.getName(0))) {
				writer.write("<responseHeader>");

				NamedList nl = (NamedList) header;
				for (int i = 0; i < nl.size(); i++) {
					String name = nl.getName(i);
					Object val = nl.getVal(i);
					if ("status".equals(name) || "QTime".equals(name)) {
						xw.writePrim(name, null, val.toString(), false);
					} else {
						xw.writeVal(name, val);
					}
				}

				writer.write("</responseHeader>");
				start = 1;
			}
		}

		
		for (int i = start; i < sz; i++) {
			if (lst.getName(i).equals("distances") && lst.getVal(i) instanceof Map){
				xw.setDistances((Map)lst.getVal(i));
			}else {
				xw.writeVal(lst.getName(i), lst.getVal(i));
			}
		}
		lst = null;
		xw.cleanUp();
		writer.write("\n</response>\n");
		rsp.setAllValues(null);
		
	}

	private void writeVal(String name, Object val) throws IOException {

		
		if (val instanceof DocList) {
			writeDocList(name, (DocList)val, defaultFieldList);
		} else if (val instanceof NamedList) {
			writeNamedList(name, (NamedList) val);
		}else {
			writePrim("str", name, val.toString(), true);
		}

	}

	//
	// OPT - specific writeInt, writeFloat, methods might be faster since
	// there would be less write calls (write("<int name=\"" + name + ... +
	// </int>)
	//
	public void writePrim(String tag, String name, String val, boolean escape)
			throws IOException {
		// OPT - we could use a temp char[] (or a StringBuilder) and if the
		// size was small enough to fit (if escape==false we can calc exact
		// size)
		// then we could put things directly in the temp buf.
		// need to see what percent of CPU this takes up first though...
		// Could test a reusable StringBuilder...

		// is this needed here???
		// Only if a fieldtype calls writeStr or something
		// with a null val instead of calling writeNull
		/***********************************************************************
		 * if (val==null) { if (name==null) writer.write("<null/>"); else
		 * writer.write("<null name=\"" + name + "/>"); }
		 **********************************************************************/

		int contentLen = val.length();
		
		startTag(tag, name, contentLen == 0);
		if (contentLen == 0)
			return;

		if (escape) {
			XML.escapeCharData(val, writer);
		} else {
			writer.write(val, 0, contentLen);
		}

		writer.write("</");
		writer.write(tag);
		writer.write('>');
	}

	public void startTag(String tag, String name, boolean closeTag)
			throws IOException {

		writer.write('<');
		writer.write(tag);
		if (name != null) {
			writeAttr("name", name);
			if (closeTag) {
				writer.write("/>");
			} else {
				writer.write(">");
			}
		} else {
			if (closeTag) {
				writer.write("/>");
			} else {
				writer.write('>');
			}
		}
	}

	public void writeAttr(String name, String val) throws IOException {
		if (val != null) {
			writer.write(' ');
			writer.write(name);
			writer.write("=\"");
			XML.escapeAttributeValue(val, writer);
			writer.write('"');
		}
	}

	public final void writeDocList(String name, DocList ids, Set<String> fields)
			throws IOException {
	
		boolean includeScore = false;
		if (fields != null) {
			includeScore = fields.contains("score");
			if (fields.size() == 0 || (fields.size() == 1 && includeScore)
					|| fields.contains("*")) {
				fields = null; // null means return all stored fields
			}
		}

		int sz = ids.size();

		writer.write("<result");
		writeAttr("name", name);
		writeAttr("numFound", Integer.toString(ids.matches()));
		writeAttr("start", Integer.toString(ids.offset()));
		if (includeScore) {
			writeAttr("maxScore", Float.toString(ids.maxScore()));
		}
		if (sz == 0) {
			writer.write("/>");
			return;
		} else {
			writer.write('>');
		}

		DocIterator iterator = ids.iterator();
		for (int i = 0; i < sz; i++) {
			int id = iterator.nextDoc();
			Document doc = searcher.doc(id, fields);
			writeDoc(null, doc, fields, (includeScore ? iterator.score() : 0.0f), includeScore, id);
			
		}

		writer.write("</result>");
		cleanUp();
	}

	
	public void setDistances (Map distances){
		this.distances = distances;
	}
	
	public void cleanUp () {
		distances = null;
	}
	
	public String formatDistance (Double d){
		DecimalFormat df1 = new DecimalFormat("####.000000");
		return df1.format(d);
	}
	
	
	public final void writeDoc(String name, Document doc, Set<String> returnFields, float score, boolean includeScore, int docid) throws IOException {
	    startTag("doc", name, false);
	    
	//	    writer.write("<distance><![CDATA["+ formatDistance((Double) distances.get(docid))+"]]></distance>");
	//		
	      
	    if(distances != null)
	    	writer.write("<str name=\"geo_distance\">" 	+
	    			formatDistance((Double) distances.get(docid)) +
	    			"</str>");
	    // Lucene Documents have multivalued types as multiple fields
	    // with the same name.
	    // The XML needs to represent these as
	    // an array. The fastest way to detect multiple fields
	    // with the same name is to sort them first.
	    
	      
	    // using global tlst here, so we shouldn't call any other
	    // function that uses it until we are done.
	    tlst.clear();
	    //System.out.println("Fields "+ doc.getFields().size());
	    for (Object obj : doc.getFields()) {
	      Fieldable ff = (Fieldable)obj;
	      // skip this field if it is not a field to be returned.
	      if (returnFields!=null && !returnFields.contains(ff.name())) {
	    	
	        continue;
	      }
	      tlst.add(ff);
	     
	    }
	    Collections.sort(tlst, fieldnameComparator);
	    
	    int sz = tlst.size();
	   
	    int fidx1 = 0, fidx2 = 0;
	    while (fidx1 < sz) {
	      Fieldable f1 = (Fieldable)tlst.get(fidx1);
	      String fname = f1.name();
	  
	      // find the end of fields with this name
	    
	      writeVal(fname, schema.getFieldType(fname).toExternal((Fieldable)tlst.get(fidx1)));
	      fidx1++;
	    }
	    writer.write("</doc>");
	}
	
	  public void writeNamedList(String name, NamedList val) throws IOException {
		    int sz = val.size();
		    startTag("lst", name, sz<=0);

		    for (int i=0; i<sz; i++) {
		      writeVal(val.getName(i),val.getVal(i));
		    }
		    
		    if (sz > 0) {
		   
		      writer.write("</lst>");
		    }
		  }
}
