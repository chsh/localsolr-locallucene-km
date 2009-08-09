package com.pjaol.search.solr.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;
import org.apache.solr.update.DocumentBuilder;
import org.apache.solr.util.SolrPluginUtils;

import com.pjaol.search.geo.utils.DistanceQuery;
import com.pjaol.search.geo.utils.DistanceSortSource;
import com.pjaol.search.geo.utils.DistanceUtils;
import com.pjaol.search.solr.LocalSolrRequestHandler;
import com.pjaol.search.solr.LocalSolrSortParser;

/**
 * {@link LocalSolrQueryComponent} Can be loaded through
 * {@link LocalSolrRequestHandler}
 * 
 * @see LocalSolrRequestHandler
 * @author pjaol
 * 
 */
public class LocalSolrQueryComponent extends SearchComponent {

	private String DistanceQuery = "DistanceQuery";

	private Logger log = Logger.getLogger(getClass().getName());
	private String latField = "lat";
	private String lngField = "lng";
	private String radiusField = "radius";

	public LocalSolrQueryComponent() {

	}

	public LocalSolrQueryComponent(String lat, String lng, String radius) {
		if (lat == null) lat = "lat";
		if (lng == null) lng = "lng";
		if (radius == null) radius = "radius";
		log.info("Setting latField to " + lat + " setting lngField to "
				+ lng);
		latField = lat;
		lngField = lng;

		log.info("Setting radiusField to " + radius);
		radiusField = radius;
	}

	@Override
	public void init(NamedList initArgs) {

		String lat = (String) initArgs.get("latField");
		String lng = (String) initArgs.get("lngField");
		String radius = (String) initArgs.get("radiusField");

		if (lat == null) lat = "lat";
		if (lng == null) lng = "lng";
		if (radius == null) radius = "radius";
		log.info("Setting latField to " + lat + " setting lngField to "
				+ lng);
		latField = lat;
		lngField = lng;

		log.info("Setting radiusField to " + radius);
		radiusField = radius;
	}

	public void prepare(ResponseBuilder builder) throws IOException {

		SolrQueryRequest req = builder.req;
		SolrQueryResponse rsp = builder.rsp;
		SolrParams params = req.getParams();

		// Set field flags
		String fl = params.get(CommonParams.FL);
		int fieldFlags = 0;

		if (fl != null) {
			fieldFlags |= SolrPluginUtils.setReturnFields(fl, rsp);
		}
		builder.setFieldFlags(fieldFlags);

		builder.setQueryString(params.get(CommonParams.Q));

		String lat = params.get(latField);
		String lng = params.get(lngField);

		String radius = params.get(radiusField);

		DistanceQuery dq = null;

		if (lat != null && lng != null && radius != null) {

			double dlat = new Double(lat).doubleValue();
			double dlng = new Double(lng).doubleValue();
			double dradius = new Double(radius).doubleValue();

			// TODO pull latitude /longitude from configuration
			dq = new DistanceQuery(dlat, dlng, dradius, latField, lngField,
					true);

		}

		// parse the query from the 'q' parameter (sort has been striped)
		String defaultField = params.get(CommonParams.DF);
		builder.setQuery(QueryParsing.parseQuery(builder.getQueryString(),
				defaultField, params, req.getSchema()));

		// parse filters
		List<Query> filters = builder.getFilters();
		String[] fqs = req.getParams().getParams(
				org.apache.solr.common.params.CommonParams.FQ);
		if (fqs != null && fqs.length != 0) {

			if (filters == null) {
				filters = new ArrayList<Query>();
				builder.setFilters(filters);
			}
			for (String fq : fqs) {
				if (fq != null && fq.trim().length() != 0) {
					QParser fqp = null;
					try {
						fqp = QParser.getParser(fq, null, req);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						filters.add(fqp.getQuery());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if (filters != null) {
			filters.add(dq.getQuery());

		} else {
			filters = new ArrayList<Query>();
			filters.add(dq.getQuery());

		}

		builder.setFilters(filters);

		req.getContext().put(DistanceQuery, dq);

	}

	@Override
	public void process(ResponseBuilder builder) throws IOException {
		SolrQueryRequest req = builder.req;
		SolrQueryResponse rsp = builder.rsp;
		SolrIndexSearcher searcher = req.getSearcher();

		SolrParams params = req.getParams();
		String lat = params.get(latField);
		String lng = params.get(lngField);

		
		DocSet f = null;

		// simply return id's
		String ids = params.get("ids");
		if (ids != null) {
			SchemaField idField = req.getSchema().getUniqueKeyField();
			String[] idArr = ids.split(","); // TODO... handle escaping
			int[] luceneIds = new int[idArr.length];
			int docs = 0;
			for (int i = 0; i < idArr.length; i++) {
				int id = req.getSearcher().getFirstMatch(
						new Term(idField.getName(), idField.getType()
								.toInternal(idArr[i])));

				if (id >= 0)
					luceneIds[docs++] = id;
			}

			DocListAndSet res = new DocListAndSet();
			res.docList = new DocSlice(0, docs, luceneIds, null, docs, 0);
			builder.setResults(res);

			log.fine("Adding SolrDocumentList from id's " + ids);

			SolrDocumentList sdoclist = mergeResultsDistances(builder
					.getResults().docList, null, searcher, rsp
					.getReturnFields(), new Double(lat).doubleValue(),
					new Double(lng).doubleValue());

			rsp.add("response", sdoclist);
			return;
		}

		DistanceQuery dq = (DistanceQuery) req.getContext().get(DistanceQuery);

		Filter optimizedDistanceFilter = dq.getFilter(builder.getQuery());

		Map<Integer, Double> distances = null;

		// Run the optimized geography filter
		f = searcher.convertFilter(optimizedDistanceFilter);
		
		DistanceSortSource dsort = null;
		dsort = new DistanceSortSource(dq.distanceFilter);

		// Parse sort
		String sortStr = params.get(CommonParams.SORT);
		if (sortStr == null) {
			// TODO? should we disable the ';' syntax with config?
			// legacy mode, where sreq is query;sort
			List<String> commands = StrUtils.splitSmart(builder
					.getQueryString(), ';');
			if (commands.size() == 2) {
				// TODO? add a deprication warning to the response header
				builder.setQueryString(commands.get(0));
				sortStr = commands.get(1);
			} else if (commands.size() == 1) {
				// This is need to support the case where someone sends:
				// "q=query;"
				builder.setQueryString(commands.get(0));
			} else if (commands.size() > 2) {
				throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
						"If you want to use multiple ';' in the query, use the 'sort' param.");
			}
		}

		if (sortStr != null) {
			SortSpec lss = new LocalSolrSortParser().parseSort(sortStr, req
					.getSchema(), dsort);
			if (lss != null) {
				builder.setSortSpec(lss);
			}

		}

		Sort sort = null;
		if (builder.getSortSpec() != null) {
			sort = builder.getSortSpec().getSort();
		}

		if (builder.isNeedDocSet()) {

			// use a standard query
			log.fine("Standard query...");

			builder.setResults(searcher.getDocListAndSet(builder.getQuery(), f,
					sort, params.getInt(CommonParams.START, 0), params.getInt(
							CommonParams.ROWS, 10), builder.getFieldFlags()));
			

		} else {

			log.fine("DocList query....");
			DocListAndSet results = new DocListAndSet();

			log.fine("Using reqular...");

			results.docList = searcher.getDocList(builder.getQuery(), f, sort,
					params.getInt(CommonParams.START, 0), params.getInt(
							CommonParams.ROWS, 10));
			builder.setResults(results);

		}

		if (distances == null)
			distances = dq.distanceFilter.getDistances();

		// pre-fetch returned documents
		SolrPluginUtils.optimizePreFetchDocs(builder.getResults().docList,
				builder.getQuery(), req, rsp);

		SolrDocumentList sdoclist = mergeResultsDistances(
				builder.getResults().docList, distances, searcher, rsp
						.getReturnFields(), new Double(lat).doubleValue(),
				new Double(lng).doubleValue());

		log.finer("Adding SolrDocumentList " + sdoclist.size());
		rsp.add("response", sdoclist);

		// Add distance sorted response for merging later...
		// Part of the MainQueryPhase response

		boolean fsv = req.getParams().getBool(
				ResponseBuilder.FIELD_SORT_VALUES, false);

		// field sort values used for sharding / distributed searching
		if (fsv) {
			// provide a sort_value in the response
			// do i really need a comparator if there is a
			// fieldable object with an internal - external representation ?
			SortField[] sortFields = sort == null ? new SortField[] { SortField.FIELD_SCORE }
					: sort.getSort();
			ScoreDoc sd = new ScoreDoc(0, 1.0f); // won't work for
			// comparators that look
			// at the score
			NamedList sortVals = new NamedList();

			for (SortField sortField : sortFields) {
				int type = sortField.getType();
				if (type == SortField.SCORE || type == SortField.DOC)
					continue;

				String fieldname = sortField.getField();
				FieldType ft = fieldname == null ? null : req.getSchema()
						.getFieldTypeNoEx(fieldname);

				DocList docList = builder.getResults().docList;
				ArrayList<Object> vals = new ArrayList<Object>(docList.size());
				DocIterator it = builder.getResults().docList.iterator();

				int docPosition = 0;
				while (it.hasNext()) {
					sd.doc = it.nextDoc();

					if (type != SortField.STRING) {
						// assume this is only used for shard-ing
						// thus field value should be internal representation of
						// the object
						try {
							if (type == SortField.CUSTOM
									&& (!fieldname.equals("geo_distance"))) {
								// assume it's a double, as there's a bug in
								// sdouble type
								vals.add(ft.toInternal(new Double(
										(Double) sdoclist.get(docPosition)
												.getFieldValue(fieldname))
										.toString()));
							} else {
								vals.add(ft.toInternal((String) sdoclist.get(
										docPosition).getFieldValue(fieldname)));
							}
						} catch (Exception e) {
							vals.add(sdoclist.get(docPosition).getFieldValue(
									fieldname));
						}
					} else {
						vals.add(sdoclist.get(docPosition).getFieldValue(
								fieldname));
					}
					docPosition++;
				}
				sortVals.add(fieldname, vals);
			}
			rsp.add("sort_values", sortVals);
		}

		if (dq.distanceFilter != null) {
			dsort.cleanUp();
			sort = null;
			distances = null;
			optimizedDistanceFilter = null;
			f = null;

		}

	}

	private SolrDocumentList mergeResultsDistances(DocList docs, Map distances,
			SolrIndexSearcher searcher, Set<String> fields, double latitude,
			double longitude) {
		SolrDocumentList sdoclist = new SolrDocumentList();
		sdoclist.setNumFound(docs.matches());
		sdoclist.setMaxScore(docs.maxScore());
		sdoclist.setStart(docs.offset());

		DocIterator dit = docs.iterator();

		while (dit.hasNext()) {
			int docid = dit.nextDoc();
			try {
				SolrDocument sd = luceneDocToSolrDoc(docid, searcher, fields);
				if (distances != null) {
					sd.addField("geo_distance", new String(distances.get(docid)
							.toString()).toString());
				} else {

					double docLat = (Double) sd.getFieldValue(latField);
					double docLong = (Double) sd.getFieldValue(lngField);
					double distance = DistanceUtils.getDistanceKm(docLat,
							docLong, latitude, longitude);

					sd.addField("geo_distance", distance);
				}

				// this may be removed if XMLWriter gets patched to
				// include score from doc iterator in solrdoclist
				if (docs.hasScores()) {
					sd.addField("score", dit.score());
				} else {
					sd.addField("score", 0.0f);
				}
				sdoclist.add(sd);

			} catch (IOException e) {
				// TODO possible slip or should we fail?
				e.printStackTrace();
			}

		}

		return sdoclist;
	}

	public SolrDocument luceneDocToSolrDoc(int docid,
			SolrIndexSearcher searcher, Set fields) throws IOException {
		Document luceneDoc = searcher.doc(docid, fields);

		SolrDocument sdoc = new SolrDocument();
		DocumentBuilder db = new DocumentBuilder(searcher.getSchema());
		sdoc = db.loadStoredFields(sdoc, luceneDoc);
		return sdoc;
	}

	/*
	 * Solr InfoBean
	 */

	@Override
	public String getDescription() {

		return "LocalSolrQueryComponent: $File: $";
	}

	@Override
	public String getSource() {

		return "$File: $";
	}

	@Override
	public String getSourceId() {

		return "$Id: $";
	}

	@Override
	public String getVersion() {

		return "$Version: $";
	}
}
