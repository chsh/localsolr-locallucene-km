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

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import com.traversetechnologies.fulcrum.math.LonLatTrig;

public class DistanceUtils {

	private static final int coordPrime = 100000000;
	private static final String coordPad = "000000000000";

	private static DefaultEllipsoid de = DefaultEllipsoid.WGS84;
	
	public static double orthodromicDistance(double x1, double y1, double x2, double y2) {
		
		return de.orthodromicDistance(y1, x1, y2, x2);
	 }	
/*
	public static double getDistanceMi(double x1, double y1, double x2, double y2) {
		return  (orthodromicDistance(x1, y1, x2, y2) *0.621371) / 1000;
		//return getLLMDistance(x1, y1, x2, y2);
	}
*/
	public static double getDistanceKm(double x1, double y1, double x2, double y2) {
		return  orthodromicDistance(x1, y1, x2, y2) / 1000;
		//return getLLMDistance(x1, y1, x2, y2);
	}
	/**
	 * 
	 * @param x1
	 * @param y1
	 * @param miles
	 * @return boundary rectangle where getY/getX is top left, getMinY/getMinX is bottom right
	 */
	public static Rectangle2D getBoundary (double x1, double y1, double miles) {
		return LonLatTrig.getRadiusBoundsKm(y1, x1, miles);
	}

	public static Rectangle2D getBoundaryMi (double x1, double y1, double miles){
		return LonLatTrig.getRadiusBoundsMi(y1, x1, miles);
	}
	
	public static Rectangle2D getBoundaryKm (double x1, double y1, double km){
		
		return LonLatTrig.getRadiusBoundsKm(y1, x1, km);
	}
	
	public static double getLLMDistance (double x1, double y1, double x2, double y2){	
		return LonLatTrig.getDistanceKm(y1, x1, y2, x2);
	}

}
