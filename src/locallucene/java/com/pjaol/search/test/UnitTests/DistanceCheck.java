package com.pjaol.search.test.UnitTests;

import java.text.DecimalFormat;

import com.pjaol.search.geo.utils.DistanceUtils;

public class DistanceCheck {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double lat1 = 0;
		double long1 = 0;
		double lat2 = 0;
		double long2 = 0;
		
		for (int i =0; i < 90; i++){
			double dis = DistanceUtils.getDistanceKm(lat1, long1, lat2, long2);
			lat1 +=1;
			lat2 = lat1 + 0.001;
			
			System.out.println(lat1+","+long1+","+lat2+","+long2+","+formatDistance(dis));
			
		}

	}

	public static String formatDistance (Double d){
		DecimalFormat df1 = new DecimalFormat("####.000000");
		return df1.format(d);
	}
	
}
