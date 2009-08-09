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

package com.pjaol.lucene.search;
import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;

/**
 * Provide an optimized filter, by allowing the bitset from 
 * previous filters in the bitset to be used in the next part of the chain.
 * 
 * @author admin
 *
 */
public abstract class ISerialChainFilter extends Filter{

	
	public abstract BitSet bits(IndexReader reader, BitSet bits) throws CorruptIndexException, IOException, Exception;
}
