package com.pjaol.search.solr;

import java.util.regex.Pattern;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SortSpec;

import com.pjaol.search.geo.utils.DistanceSortSource;

public class LocalSolrSortParser {

	private static Pattern sortSep = Pattern.compile(",");
	
	private String stringValue;
	
	public SortSpec parseSort(String sortSpec,
			IndexSchema schema,  DistanceSortSource ds) {
		if (sortSpec == null || sortSpec.length() == 0)
			return null;

		stringValue = sortSpec;
		
		String[] parts = sortSep.split(sortSpec.trim());
		if (parts.length == 0)
			return null;

		SortField[] lst = new SortField[parts.length];
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i].trim();
			boolean top = true;

			int idx = part.indexOf(' ');
			if (idx > 0) {
				String order = part.substring(idx + 1).trim();
				if ("desc".equals(order) || "top".equals(order)) {
					top = true;
				} else if ("asc".equals(order) || "bottom".equals(order)) {
					top = false;
				} else {
					throw new SolrException(
							SolrException.ErrorCode.BAD_REQUEST,
							"Unknown sort order: " + order);
				}
				part = part.substring(0, idx).trim();
			} else {
				throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
						"Missing sort order.");
			}

			if ("score".equals(part)) {
				if (top) {
					// If thre is only one thing in the list, just do the
					// regular
					// thing...
					if (parts.length == 1) {
						return null; // do normal scoring...
					}
					lst[i] = SortField.FIELD_SCORE;
				} else {
					lst[i] = new SortField(null, SortField.SCORE, true);
				}
			} else if (part.equals("geo_distance")) {
			
				lst[i] = new SortField("geo_distance", ds, top);
			
			} else {
				// getField could throw an exception if the name isn't found
				SchemaField f = null;
				try {
					f = schema.getField(part);
				} catch (SolrException e) {
					throw new SolrException(
							SolrException.ErrorCode.BAD_REQUEST,
							"can not sort on undefined field: " + part, e);
				}
				if (f == null || !f.indexed()) {
					throw new SolrException(
							SolrException.ErrorCode.BAD_REQUEST,
							"can not sort on unindexed field: " + part);
				}
				lst[i] = f.getType().getSortField(f, top);
			}

		}
		// return null;
		// For more info on the 'num' field, -1,
		// see: https://issues.apache.org/jira/browse/SOLR-99
		return new SortSpec(new Sort(lst), -1);
		//return new QueryParsing.SortSpec(new Sort(lst), -1);

	}

	public String toString() {
		return stringValue;
	}
	
	/***************************************************************************
	 * SortSpec encapsulates a Lucene Sort and a count of the number of
	 * documents to return.
	 */
	public static class LocalSolrSortSpec{

		private final Sort sort;

		private final int num;
		private String stringValue;
		
		public LocalSolrSortSpec(Sort sort, int num, String stringValue) {
			this.sort = sort;
			this.num = num;
			this.stringValue = stringValue;
		}

		/**
		 * Gets the Lucene Sort object, or null for the default sort by score
		 * descending.
		 */
		public Sort getSort() {
			return sort;
		}

		/**
		 * Gets the number of documens to return after sorting.
		 * 
		 * @return number of docs to return, or -1 for no cut off (just sort)
		 */
		public int getCount() {
			return num;
		}
		
		public String toString(){
			return stringValue;
		}
	}

}
