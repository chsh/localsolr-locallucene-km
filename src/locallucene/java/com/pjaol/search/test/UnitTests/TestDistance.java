/**
 * * Copyright 2007 Patrick O'Leary 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
 *
 */
package com.pjaol.search.test.UnitTests;

import java.io.IOException;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.solr.util.NumberUtils;

import com.pjaol.lucene.search.SerialChainFilter;
import com.pjaol.search.geo.utils.DistanceFilter;
import com.pjaol.search.geo.utils.DistanceQuery;
import com.pjaol.search.geo.utils.DistanceSortSource;
import com.pjaol.search.geo.utils.DistanceUtils;
import com.pjaol.search.geo.utils.InvalidGeoException;

/**
 * @author pjaol
 *
 */
public class TestDistance extends TestCase{

	
	private RAMDirectory directory;
	private IndexSearcher searcher;
	// reston va
	private double lat = 38.969398; 
	private double lng= -77.386398;
	private String latField = "lat";
	private String lngField = "lng";
	
	
	protected void setUp() throws IOException {
		directory = new RAMDirectory();
		IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
		addData(writer);
		
	}
	
	
	private void addPoint(IndexWriter writer, String name, double lat, double lng) throws IOException{
		
		Document doc = new Document();
		
		doc.add(new Field("name", name,Field.Store.YES, Field.Index.TOKENIZED));
		
		// convert the lat / long to lucene fields
		doc.add(new Field(latField, NumberUtils.double2sortableStr(lat),Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field(lngField, NumberUtils.double2sortableStr(lng),Field.Store.YES, Field.Index.UN_TOKENIZED));
		
		// add a default meta field to make searching all documents easy 
		doc.add(new Field("metafile", "doc",Field.Store.YES, Field.Index.TOKENIZED));
		writer.addDocument(doc);
		
	}
	

	private void addData(IndexWriter writer) throws IOException {
		addPoint(writer,"McCormick &amp; Schmick's Seafood Restaurant",38.9579000,-77.3572000);
		addPoint(writer,"Jimmy's Old Town Tavern",38.9690000,-77.3862000);
		addPoint(writer,"Ned Devine's",38.9510000,-77.4107000);
		addPoint(writer,"Old Brogue Irish Pub",38.9955000,-77.2884000);
		addPoint(writer,"Alf Laylah Wa Laylah",38.8956000,-77.4258000);
		addPoint(writer,"Sully's Restaurant &amp; Supper",38.9003000,-77.4467000);
		addPoint(writer,"TGIFriday",38.8725000,-77.3829000);
		addPoint(writer,"Potomac Swing Dance Club",38.9027000,-77.2639000);
		addPoint(writer,"White Tiger Restaurant",38.9027000,-77.2638000);
		addPoint(writer,"Jammin' Java",38.9039000,-77.2622000);
		addPoint(writer,"Potomac Swing Dance Club",38.9027000,-77.2639000);
		addPoint(writer,"WiseAcres Comedy Club",38.9248000,-77.2344000);
		addPoint(writer,"Glen Echo Spanish Ballroom",38.9691000,-77.1400000);
		addPoint(writer,"Whitlow's on Wilson",38.8889000,-77.0926000);
		addPoint(writer,"Iota Club and Cafe",38.8890000,-77.0923000);
		addPoint(writer,"Hilton Washington Embassy Row",38.9103000,-77.0451000);
		addPoint(writer,"HorseFeathers, Bar & Grill", 39.01220000000001, -77.3942);
		writer.flush();
	}
	
	public void testRange() throws IOException, InvalidGeoException {
		searcher = new IndexSearcher(directory);
		
		double kilometers = 6.0;

		// create a distance query
		DistanceQuery dq = new DistanceQuery(lat, lng, kilometers, latField, lngField, false);
		 
		System.out.println(dq.latFilter.toString() +" "+ dq.lngFilter);
		
		//create a term query to search against all documents
		Query tq = new TermQuery(new Term("metafile", "doc"));

		
		// Create a distance sort
		// As the radius filter has performed the distance calculations
		// already, pass in the filter to reuse the results.
		// 
		DistanceSortSource dsort = new DistanceSortSource(dq.distanceFilter);
		Sort sort = new Sort(new SortField("foo", dsort));
		
		// Perform the search, using the term query, the serial chain filter, and the
		// distance sort
		Hits hits = searcher.search(tq, dq.getFilter(), sort);

		int results = hits.length();
		
		// Get a list of distances 
		Map<Integer,Double> distances = dq.distanceFilter.getDistances();
		
		// distances calculated from filter first pass must be less than total
		// docs, from the above test of 20 items, 12 will come from the boundary box
		// filter, but only 5 are actually in the radius of the results.
		
		// Note Boundary Box filtering, is not accurate enough for most systems.
		
		System.out.println("Distance Filter filtered: " + distances.size());
		System.out.println("Results: " + results);
		System.out.println("=============================");
		assertEquals(4, distances.size());
		assertEquals(4, results);
		
		for(int i =0 ; i < results; i++){
			Document d = hits.doc(i);
			
			String name = d.get("name");
			double rsLat = NumberUtils.SortableStr2double(d.get(latField));
			double rsLng = NumberUtils.SortableStr2double(d.get(lngField)); 
			Double geo_distance = distances.get(hits.id(i));
			
			double distance = DistanceUtils.getDistanceKm(lat, lng, rsLat, rsLng);
			double llm = DistanceUtils.getLLMDistance(lat, lng, rsLat, rsLng);
			System.out.println("Name: "+ name +", Distance (res, ortho, harvesine):"+ distance +" |"+ geo_distance +"|"+ llm);
			assertTrue(Math.abs((distance - llm)) < 1);
			assertTrue((distance < kilometers ));
		}
	}
	
	
	public void testKilometers() {
		double LLM = DistanceUtils.getLLMDistance(lat, lng,39.012200001, -77.3942);
		System.out.println(LLM);
		System.out.println("-->"+DistanceUtils.getDistanceKm(lat, lng, 39.0122, -77.3942));
	}
	
	public void testKilometers2() {
		System.out.println("Test Kilometers 2");
		double LLM = DistanceUtils.getLLMDistance(44.30073, -78.32131,43.687267, -79.39842);
		System.out.println(LLM);
		System.out.println("-->"+DistanceUtils.getDistanceKm(44.30073, -78.32131, 43.687267, -79.39842));
		
	}

	
	public void testDistanceQueryCacheable() throws IOException {

		// create two of the same distance queries
		double miles = 6.0;
		DistanceQuery dq1 = new DistanceQuery(lat, lng, miles, latField, lngField, false);
		DistanceQuery dq2 = new DistanceQuery(lat, lng, miles, latField, lngField, false);

		/* ensure that they hash to the same code, which will cause a cache hit in solr */
		System.out.println("hash differences?");
		assertEquals(dq1.getQuery().hashCode(), dq2.getQuery().hashCode());
		
		/* ensure that changing the radius makes a different hash code, creating a cache miss in solr */
		DistanceQuery widerQuery = new DistanceQuery(lat, lng, miles + 5.0, latField, lngField, false);
		assertTrue(dq1.getQuery().hashCode() != widerQuery.getQuery().hashCode());
	}
}
