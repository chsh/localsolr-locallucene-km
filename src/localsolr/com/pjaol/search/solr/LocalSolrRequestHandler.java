package com.pjaol.search.solr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.handler.component.DebugComponent;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.handler.component.MoreLikeThisComponent;
import org.apache.solr.handler.component.SearchComponent;

import com.pjaol.search.solr.component.LocalSolrQueryComponent;

/**
 * LocalSolrRequestHandler is the geographical search handler for solr
 * Configuration is in solrconfig.xml as:
 * <pre>
 *  &lt;requestHandler name="geo" class="com.pjaol.search.solr.LocalSolrRequestHandler"&gt;
 *    &lt;str name="latField"&gt;lat&lt;/str&gt;
 *    &lt;str name="lngField"&gt;lng&lt;/str&gt;
 *    &lt;str name="radiusField"&gt;radius&lt;/str&gt;
 *  &lt;/requestHandler&gt;
 * </pre>
 * @author pjaol
 *
 */
public class LocalSolrRequestHandler extends SearchHandler {

	@Override
	public void inform(SolrCore core) {
		
		super.inform(core);
		String latField = (String) initArgs.get("latField");
		String lngField = (String) initArgs.get("lngField");
		String radiusField = (String) initArgs.get("radiusField");
		
		components = new ArrayList<SearchComponent>(5);
		components.add(new LocalSolrQueryComponent(latField, lngField, radiusField));
		components.add(new FacetComponent());
		components.add(new MoreLikeThisComponent());
		components.add(new HighlightComponent());
		components.add(new DebugComponent());
	}

	
}
