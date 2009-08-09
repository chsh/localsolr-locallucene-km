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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Logger;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.RangeFilter;
import org.apache.solr.util.NumberUtils;

import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;
import com.pjaol.search.geo.utils.projections.IProjector;
import com.pjaol.search.geo.utils.projections.SinusoidalProjector;

/**
 * @author pjaol
 *
 */
public class CartesianPolyFilter {

	private IProjector projector = new SinusoidalProjector();
	private Logger log = Logger.getLogger(getClass().getName());
	public RangeFilter boundaryBox (double latitude, double longitude, int miles){
		
		Rectangle2D box = DistanceUtils.getBoundary(latitude, longitude, miles);
		
		double latX = box.getY();
		double latY = box.getMaxY();
		
		double longX = box.getX();
		double longY = box.getMaxX();
		
		CartesianTierPlotter ctp = new CartesianTierPlotter(2, projector);
		int bestFit = ctp.bestFit(miles);
		
		log.fine("Best Fit is : " + bestFit);
		ctp = new CartesianTierPlotter(bestFit, projector);
		
		
		double beginAt = ctp.getTierBoxId(latX, longX);
		double endAt = ctp.getTierBoxId(latY, longY);
		String fieldName = ctp.getTierFieldName();
		
		log.fine("RangeFilter is ("+latX+","+longX+") "+"("+latY+","+longY+") "+fieldName+":["+beginAt +" TO "+ endAt+"]");
		
		RangeFilter f = new RangeFilter(fieldName, 
					NumberUtils.double2sortableStr(beginAt),
					NumberUtils.double2sortableStr(endAt),
					true, true);
		
		
		
		return f;
		
	}
	
	
	public Shape getBoxShape(double latitude, double longitude, int miles){
		
		Rectangle2D box = DistanceUtils.getBoundary(latitude, longitude, miles);
		
		double latX = box.getY();
		double latY = box.getMaxY();
		
		double longX = box.getX();
		double longY = box.getMaxX();
		CartesianTierPlotter ctp = new CartesianTierPlotter(2, projector);
		int bestFit = ctp.bestFit(miles);
		
		log.fine("Best Fit is : " + bestFit);
		ctp = new CartesianTierPlotter(bestFit, projector);
		Shape shape = new Shape(ctp.getTierFieldName());
		
		// generate shape
		// iterate from startX->endX
		// 		iterate from startY -> endY
		//			shape.add(currentLat.currentLong);
		
		double beginAt = ctp.getTierBoxId(latX, longX);
		double endAt = ctp.getTierBoxId(latY, longY);
		
		double tierVert = ctp.getTierVerticalPosDivider();
		log.fine(" | "+ beginAt+" | "+ endAt);
		
		double startX = beginAt - (beginAt %1);
		double startY = beginAt - startX ; //should give a whole number
		
		double endX = endAt - (endAt %1);
		double endY = endAt -endX; //should give a whole number
		
		int scale = (int)Math.log10(tierVert);
		endY = new BigDecimal(endY).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
		startY = new BigDecimal(startY).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
		log.fine("scale "+scale+" startX "+ startX + " endX "+endX +" startY "+ startY + " endY "+ endY +" tierVert "+ tierVert);
		
		double xInc = 1.0d / tierVert;
		xInc = new BigDecimal(xInc).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
		
		for (; startX <= endX; startX++){
			
			double itY = startY;
			while (itY <= endY){
				//create a boxId
				// startX.startY
				double boxId = startX + itY ;
				shape.addBox(boxId);
				itY += xInc;
				
				// java keeps 0.0001 as 1.0E-1
				// which ends up as 0.00011111
				itY = new BigDecimal(itY).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
			}
		}
		
		
		return shape;
	}
	
	public Filter getBoundingArea(double latitude, double longitude, int miles) {

		Shape shape = getBoxShape(latitude, longitude, miles);
		return new CartesianShapeFilter(shape, shape.getTierId());
	}
}
