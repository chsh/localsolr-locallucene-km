package com.pjaol.search.solr.test;

import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.util.TestHarness;
import org.apache.solr.util.TestHarness.LocalRequestFactory;

public class TestHandlers {

	private String base = "/Users/pjaol/tmp/solr/apache-solr-1.1.0-incubating/example/solr";
	private String dataDir = base +"/data";
	private String config = base +"/conf/solrconfig.xml";
	private String schema = base + "/conf/schema.xml";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestHandlers th = new TestHandlers();
		th.runTest();
	}

	public void runTest (){
		
		TestHarness test = new TestHarness(dataDir,config, schema);
		LocalRequestFactory lrf = test.getRequestFactory("geo", 0, 100);
		LocalSolrQueryRequest x = lrf.makeRequest(new String[] {"q", "metadoc:file"});
		
	}
	
}
