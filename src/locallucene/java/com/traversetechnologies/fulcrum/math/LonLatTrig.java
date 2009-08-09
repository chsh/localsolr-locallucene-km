/*
   LonLatTrig.java
   Copyright (C) 2004 Traverse Technologies Inc.

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/
package com.traversetechnologies.fulcrum.math;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * <p>This class provides common calculations involving the use of longitude and 
 * latitude world coordinates. Since longitude and latitude representations of
 * spatial data are not planar, the calculations for distances and paths become
 * significantly more complex than the flat equivalents. The methods in this 
 * class have not yet been tested outside the northern hemisphere and may 
 * require sign changes in places. Likewise, for special applications, the 
 * radius of the earth may need to be adjusted. Earth is treated as a sphere
 * for the purpose of these calculations unless otherwise noted.</p>
 *
 * <p>It's possible to use the <code>CoordSpaceTransform</code> class to 
 * extrapolate longitude and latitude positions from screen coordinates. 
 * Keep in mind, however, that the extrapolation process has inherent errors 
 * that are magnified when the number of pixels are small and the geographic 
 * area being displayed is large. All this is a disclaimer that the calculations
 * derived from the workings of this class, with or without the <code>
 * CoordSpaceTransform</code> class, should not be used with applications 
 * requiring high accuracy and precision (but probably good enough for what 
 * Fulcrum was initially designed for - web based mapping applications). </p>
 *
 * @author Carleton Tsui
 */
public abstract class LonLatTrig {
   
   /**
    * The earth radius according to the FAI sphere in kilometers is 6371 km. 
    */
   public static final double EARTH_RADIUS = 6371;
   /**
    * Get the Great Circle distance in kilometers between two points given the
    * longitude and latitude positions of two points. Adapted from <em>
    * Trigonometry DeMystified</em> by Stan Gibilisco. Not suited for precise 
    * measurements, but probably good enough for web based applications where 
    * getting the longitude/latitude positions interpolated from pixel locations
    * are spotty in the first place.
    *
    * @param lon1 The longitude degree of the first point.
    * @param lat1 The latitude degree of the first point.
    * @param lon2 The longitude degree of the second point.
    * @param lat2 The latitude degree of the second point.
    * @return The distance in kilometers.
    */
   public static final double getDistanceKm(double lon1, double lat1, 
                                            double lon2, double lat2) {
      double latO = 90; // north pole
      double sphAngle = Math.toRadians(Math.abs(lon1-lon2));
      double s1 = Math.toRadians(Math.abs(latO-lat1));
      double s2 = Math.toRadians(Math.abs(latO-lat2));
      double radDist = Math.acos( (Math.cos(s1)*Math.cos(s2))+
         (Math.sin(s1)*Math.sin(s2)*Math.cos(sphAngle)));
      return radDist*EARTH_RADIUS;
   }
   
   /**
    * Get the Great Circle distance in miles between two points given the
    * longitude and latitude positions of two points. This method calls the 
    * getDistanceKm method and converts the results to miles. See notes for that
    * method.
    *
    * @param lon1 The longitude degree of the first point.
    * @param lat1 The latitude degree of the first point.
    * @param lon2 The longitude degree of the second point.
    * @param lat2 The latitude degree of the second point.
    * @return The distance in miles.
    */   
   public static final double getDistanceMi(double lon1, double lat1,
                                            double lon2, double lat2) {
      return getDistanceKm(lon1,lat1,lon2,lat2)*0.621371; //km to miles
   }
   
   /**
    * Determine the end longitude/latitude point given a start point, distance 
    * and bearing. The calculations in this method were adapted from the 
    * <a href="http://williams.best.vwh.net/avform.htm">Aviation Formulary 
    * v1.30</a> by Ed Williams and proofed against the working examples 
    * found there. There appear to be slight deviations between the proofs and
    * the results returned here due to differences in Java's Math.toRadians 
    * method returns and calculating it directly. The Java method was used in 
    * order to preserve consistency in conversions between methods in this 
    * class.
    * 
    * @param lon The starting longitude point.
    * @param lat The starting latitude point.
    * @param km The distance in kilometers.
    * @param radial The bearing in degrees (e.g. 0=north, 90=east, etc.)
    * @return A <code>Point2D</code> with the longitude and latitude coordinate
    * of the spot along the distance 'km' on course 'radial'.
    */ 
   public static final Point2D getPointOnRadialKm(double lon, double lat, 
                                                  double km, double radial) {
      /* Convert km to nautical miles to arc radians; 1 nm = 1.852km */
      double dRadArc = ((km/1.852)*Math.PI)/10800; 
      double tc = Math.toRadians(radial);
      double lat1 = Math.toRadians(lat);
      double lon1 = Math.toRadians(lon);
      double pLat = Math.asin((Math.sin(lat1)*Math.cos(dRadArc))+
                              (Math.cos(lat1)*Math.sin(dRadArc)*Math.cos(tc)));
      double dLon = Math.atan2(Math.sin(tc)*Math.sin(dRadArc)*Math.cos(lat1),
                               Math.cos(dRadArc)-Math.sin(lat1)*Math.sin(lat1));
      double pLon = (lon1+dLon+Math.PI % (2*Math.PI))-Math.PI;
      /* Convert radians back into decimal degrees */
      pLat = (pLat*180)/Math.PI;
      pLon = (pLon*180)/Math.PI;
      return new Point2D.Double(pLon,pLat);
   }
   
   /**
    * Determine the end longitude/latitude point given a start point, distance 
    * and bearing. See the notes for getPointOnRadialKm for more information.
    *
    * @param lon The starting longitude point.
    * @param lat The starting latitude point.
    * @param mi The distance in miles.
    * @param radial The bearing in degrees (e.g. 0=north, 90=east, etc.)
    * @return A <code>Point2D</code> with the longitude and latitude coordinate
    * of the spot along the distance 'km' on course 'radial'.
    */    
   public static final Point2D getPointOnRadialMi(double lon, double lat,
                                                  double mi, double radial) {
      return getPointOnRadialKm(lon,lat,mi*1.6093,radial);
   }
   
   /**
    * Given a center point and a distance this method produces a bounding box.
    * @param lon The center point longitude.
    * @param lat The center point latitutde.
    * @param km The distance in kilometers.
    * @return The bounding box of +/- km distance from the center point on the 
    * x and y axes.
    */
   public static final Rectangle2D getRadiusBoundsKm(double lon, double lat, 
                                                     double km) {
      /* Get the east point for maximum x */
      double maxX= getPointOnRadialKm(lon,lat,km,90).getX();
      /* Get the north point for maximum y*/
      double maxY= getPointOnRadialKm(lon,lat,km,0).getY();
      /* Get the point west for minimum x */
      double minX = getPointOnRadialKm(lon,lat,km,270).getX();
      /* Get the point south for minimum y */
      double minY= getPointOnRadialKm(lon,lat,km,180).getY();
      return new Rectangle2D.Double(minX, minY, Math.abs(maxX-minX),
                                    Math.abs(maxY-minY));
   }

   /**
    * Given a center point and a distance this method produces a bounding box.
    * This method calls the getRadiusBoundsKm method with distances converted 
    * from the miles distance. 
    *
    * @param lon The center point longitude.
    * @param lat The center point latitutde.
    * @param mi The distance in miles.
    * @return The bounding box of +/- mi distance from the center point on the 
    * x and y axes.
    */   
   public static final Rectangle2D getRadiusBoundsMi(double lon, double lat, 
                                                     double mi) {
      return getRadiusBoundsKm(lon,lat,mi*1.6093);
   }   
   
}