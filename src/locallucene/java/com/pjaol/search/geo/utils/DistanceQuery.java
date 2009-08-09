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
package com.pjaol.search.geo.utils;

import java.awt.geom.Rectangle2D;

import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryFilter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.RangeFilter;
import org.apache.solr.util.NumberUtils;

import com.pjaol.lucene.search.SerialChainFilter;
import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;

public class DistanceQuery{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BoundaryBoxFilter latFilter;
	public BoundaryBoxFilter lngFilter;
	public DistanceFilter distanceFilter;
	
	private double lat;
	private double lng;
	private double kilometers;
	private String latField;
	private String lngField;
	private boolean useCartesian = false;
	private Filter cartesianFilter;
	
	/**
	 * Create a distance query using
	 * a boundary box wrapper around a more precise
	 * DistanceFilter.
	 * 
	 * @see SerialChainFilter
	 * @param lat
	 * @param lng
	 * @param kilometers
	 */
	public DistanceQuery (double lat, double lng, double kilometers, String latField, String lngField, boolean useCartesian){

	    this.lat = lat;
	    this.lng = lng;
	    this.kilometers = kilometers;
	    this.latField = latField;
	    this.lngField = lngField;
	    this.useCartesian = useCartesian;
	    
	    /* create boundary box filters */
		Rectangle2D box = DistanceUtils.getBoundary(lat, lng, kilometers);
		
		if (! useCartesian) {
			latFilter = new BoundaryBoxFilter(latField, NumberUtils.double2sortableStr(box.getY()), NumberUtils.double2sortableStr(box.getMaxY()), 
		                                  true, true);
			lngFilter = new BoundaryBoxFilter(lngField, NumberUtils.double2sortableStr(box.getX()), NumberUtils.double2sortableStr(box.getMaxX()), 
		                                  true, true);
		} else {
			CartesianPolyFilter cpf = new CartesianPolyFilter();
			cartesianFilter = cpf.getBoundingArea(lat, lng, (int)kilometers);
		}
	    /* create precise distance filter */
		distanceFilter = new DistanceFilter(lat, lng, kilometers, latField, lngField);
	}

   /**
	* Create a distance query using
	* a boundary box wrapper around a more precise
	* DistanceFilter.
	* 
	* @see SerialChainFilter
	* @param lat
	* @param lng
	* @param kilometers
	*/
	public Filter getFilter() {
		
		if (useCartesian){
			return new SerialChainFilter(new Filter[] {cartesianFilter, distanceFilter},
                    new int[] {SerialChainFilter.AND,
		                       SerialChainFilter.SERIALAND});

		}
		
	    return new SerialChainFilter(new Filter[] {latFilter, lngFilter, distanceFilter},
				                     new int[] {SerialChainFilter.AND,
						                        SerialChainFilter.AND,
						                        SerialChainFilter.SERIALAND});
	}
	
	public Filter getFilter(Query query) {
		QueryWrapperFilter qf = new QueryWrapperFilter(query);
		
		if (useCartesian){
			return new SerialChainFilter(new Filter[] {cartesianFilter, qf, distanceFilter},
					new int[] {SerialChainFilter.AND, 
							SerialChainFilter.AND,
							SerialChainFilter.SERIALAND});
		}
		return new SerialChainFilter(new Filter[] {latFilter, lngFilter, qf, distanceFilter},
									new int[] {SerialChainFilter.AND, 
											SerialChainFilter.AND,
											SerialChainFilter.AND,
											SerialChainFilter.SERIALAND});
	}
	  
	public Query getQuery() {
	    return new ConstantScoreQuery(getFilter());
	}
	  
	public String toString() {
		return "DistanceQuery lat: " + lat + " lng: " + lng + " kilometers: "+ kilometers;
	}
}
