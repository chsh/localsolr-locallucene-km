/**
 * Copyright 2007 Patrick O'Leary 
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

import java.util.HashMap;

/**
 * Provide a high level access point to distances
 * Used by DistanceSortSource and DistanceQuery
 *  
 * @author pjaol
 *
 */
public class DistanceHandler {
	
	private HashMap distances;
	public enum precision {EXACT, TWOFEET, TWENTYFEET, TWOHUNDREDFEET};
	private precision precise;
	
	public DistanceHandler (HashMap distances, precision precise){
		this.distances = distances;
		this.precise = precise; 
		
	}
	
	
	public static double getPrecision(double x, precision thisPrecise){
		
		
		if(thisPrecise != null){
			double dif = 0;
			switch(thisPrecise){
			
			case EXACT:
				return x;
			case TWOFEET:
				dif = x % 0.0001;
				
			case TWENTYFEET:
				dif = x % 0.001;
				
			case TWOHUNDREDFEET:
				dif = x % 0.01;
						
			}
			
			return x - dif;
			
		}
		
		return x;
	}
	
	public precision getPrecision() {
		return precise;
	}
	
	public static void main(String args[]){
		
		DistanceHandler db = new DistanceHandler(new HashMap(), precision.TWOHUNDREDFEET);
		System.out.println(db.getPrecision(-1234.123456789, db.getPrecision()));
	}

	
	public double getDistance(int docid, double centerLat, double centerLng, double lat, double lng){
	
		// check to see if we have distances
		// if not calculate the distance
		if(distances == null){
			return DistanceUtils.orthodromicDistance(centerLat, centerLng, lat, lng);
		}
		
		// check to see if the doc id has a cached distance
		Double docd = (Double)distances.get(docid);
		if (docd != null){
			return docd.doubleValue();
		}
		
		//check to see if we have a precision code
		// and if another lat/long has been calculated at
		// that rounded location
		if (precise != null) {
			double xLat = getPrecision(lat, precise);
			double xLng = getPrecision(lng, precise);
			
			String k = new Double(xLat).toString() +","+ new Double(xLng).toString();
		
			Double d = ((Double)distances.get(k));
			if (d != null){
				return d.doubleValue();
			}
		}
		
		//all else fails calculate the distances		
		return DistanceUtils.orthodromicDistance(centerLat, centerLng, lat, lng);
		
	}
}
