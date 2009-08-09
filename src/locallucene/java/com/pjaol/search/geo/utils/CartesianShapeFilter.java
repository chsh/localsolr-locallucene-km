package com.pjaol.search.geo.utils;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Filter;
import org.apache.solr.util.NumberUtils;

public class CartesianShapeFilter extends Filter{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Shape shape;
	private Logger log = Logger.getLogger(getClass().getName());
	private String fieldName;
	
	CartesianShapeFilter(Shape shape, String fieldName){
		this.shape = shape;
		this.fieldName = fieldName;
	}
	
	@Override
	public BitSet bits(IndexReader reader) throws IOException {
		long start = System.currentTimeMillis();
    	
        BitSet bits = new BitSet(reader.maxDoc());
		
        TermDocs termDocs = reader.termDocs();
        List area = shape.getArea();
        int sz = area.size();
        
        // iterate through each boxid
		for (int i =0; i< sz; i++){
        	
			double boxId = ((Double)area.get(i)).doubleValue();
        	
        	termDocs.seek(new Term(fieldName,
        			NumberUtils.double2sortableStr(boxId)));
        	
        	// iterate through all documents
        	// which have this boxId
        	while (termDocs.next()) {
                bits.set(termDocs.doc());
                
            }
        }
        
        
        long end = System.currentTimeMillis();
        log.info("BoundaryBox Time Taken: "+ (end - start));
        return bits;
	}

}
