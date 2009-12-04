/*
 * The 'probe viewer' application.
 * 
 * This handles situations where we're viewing probes from an array design, or those for a specific gene. For array
 * designs it allows searches.
 * 
 * @author Paul
 * 
 * @version $Id$
 */
Ext.namespace('Gemma.ProbeBrowser');

Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

// FIXME make this configurable
Gemma.GEMMA_BASE_URL = "http://www.chibi.ubc.ca/Gemma/";

Gemma.UCSC_ICON = "/Gemma/images/logo/ucsc.gif";
Gemma.NCBI_ICON = "/Gemma/images/logo/ncbi.gif";

/**
 * 
 */
Gemma.ProbeBrowser.app = function() {

	return {
		init : function() {
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
			Ext.QuickTips.init();

			var csidstr = Ext.get("cslist").getValue();

			if (csidstr) {
				var csids = csidstr.split(',');
			}

			var arrayDesignId = Ext.get("arrayDesignId").getValue();

			var detailsDs = this.initDetails();
			this.initMainGrid(arrayDesignId, csids);
			this.mainGrid.reset();
		},

		/**
		 * Initialize the main grid.
		 * 
		 * @param {boolean}
		 *            isArrayDesign
		 */
		initMainGrid : function(arrayDesignId, csIds) {
			this.mainGrid = new Gemma.ProbeGrid({
						csIds : csIds,
						arrayDesignId : arrayDesignId,
						detailsDataSource : this.detailsGrid.getStore(),
						renderTo : "probe-grid",
						pageSize : 20,
						height : 350,
						width : 630
					});

		},

		/**
		 * Separate grid for 'details' about the probe and its alignment results.
		 */

		initDetails : function() {
			this.detailsGrid = new Gemma.ProbeDetailsGrid({
						renderTo : "probe-details",
						height : 100,
						width : 620
					});
		},

		/**
		 * Used for displaying on the details of a given probe (for probe details page)
		 */

		initOneDetail : function() {
			// create the grid for details.
			this.initDetails();
			// Get this id
			var csId = dwr.util.getValue("cs");

			// Load the details to be displayed.
			this.detailsGrid.getStore().load({
						params : [{
									id : csId
								}]
					});

		}
	};
}();

/**
 * 
 * @class Gemma.ProbeDetailsGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.ProbeDetailsGrid = Ext.extend(Ext.grid.GridPanel, {

	loadMask : {
		msg : "Loading details ..."
	},

	autoExpandColumn : 'alignment',
	autoScroll : true,
	stateful : false,

	record : Ext.data.Record.create([{
				name : "identity",
				type : "float"
			}, {
				name : "score",
				type : "float"
			}, {
				name : "blatResult"
			}, {
				name : "compositeSequence"
			}, {
				name : "geneProductIdMap"
			}, {
				name : "geneProductIdGeneMap"
			}]),

	numberformat : function(d) {
		return Math.round(d * 100) / 100;
	},

	gpMapRender : function(data) {
		var res = "";
		for (var i in data) {
			if (data[i].name) {
				res = res + data[i].name + "<br />";
			}
		}
		return res;
	},

	geneMapRender : function(data) {
		var res = "";

		for (var i in data) {
			if (data[i].id) {
				res = res
						+ "<a title='View gene details (opens new window)' target='_blank' href='/Gemma/gene/showGene.html?id="
						+ data[i].id + "'>" + data[i].officialSymbol + "</a><br />";
			}
		}
		return res;
	},

	blatResRender : function(d, metadata, record, row, column, store) {
		if (!d.targetChromosome) {
			return "";
		}

		var res = "chr" + d.targetChromosome.name + " (" + d.strand + ") " + d.targetStart + "-" + d.targetEnd;

		var organism = d.targetChromosome.taxon;
		var database = this.getDb(organism);
		if (database) {
			var link = "http://genome.ucsc.edu/cgi-bin/hgTracks?org=" + organism + "&pix=850&db=" + database
					+ "&hgt.customText=" + Gemma.GEMMA_BASE_URL + "blatTrack.html?id=" + d.id;
			res = res + "&nbsp;<a title='Genome browser view (opens in new window)' target='_blank' href='" + link
					+ "'><img src='" + Gemma.UCSC_ICON + "' /></a>";
		}
		return res;
	},

	getDb : function(taxon) {
		if (taxon.externalDatabase) {
			return taxon.externalDatabase.name;
		}
	},

	initComponent : function() {
		Ext.apply(this, {
					columns : [{
								sortable : true,
								id : "alignment",
								header : "Alignment",
								dataIndex : "blatResult",
								renderer : this.blatResRender.createDelegate(this),
								tooltip : "Alignments to the genome"
							}, {
								sortable : true,
								id : "score",
								header : "Score",
								width : 60,
								dataIndex : "score",
								renderer : this.numberformat.createDelegate(this),
								tooltip : "BLAT score"
							}, {
								sortable : true,
								id : "identity",
								header : "Identity",
								width : 60,
								dataIndex : "identity",
								renderer : this.numberformat.createDelegate(this),
								tooltip : "Sequence alignment identity"
							}, {
								sortable : true,
								id : 'genes',
								header : "Genes",
								dataIndex : "geneProductIdGeneMap",
								renderer : this.geneMapRender.createDelegate(this),
								tooltip : "Genes at this genomic location"
							}, {
								sortable : true,
								id : 'transcripts',
								header : "Transcripts",
								dataIndex : "geneProductIdMap",
								renderer : this.gpMapRender.createDelegate(this),
								tooltip : "Transcripts at this genomic location"
							}],
					store : new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(CompositeSequenceController.getBlatMappingSummary),
								reader : new Ext.data.ListRangeReader({
										// id : "blatResult" // don't use an id; let Ext define
										// one for us.
										}, this.record),
								remoteSort : false,
								sortInfo : {
									field : "score",
									direction : "DESC"
								}
							})
				});

		this.getStore().on("load", this.updateSequenceInfo.createDelegate(this));
		Gemma.ProbeDetailsGrid.superclass.initComponent.call(this);

	},

	/**
	 * Update the details listing.
	 */
	updateSequenceInfo : function() {

		var dh = Ext.DomHelper;

		if (this.getStore().getCount() === 0) {
			// This shouldn't happen any more because we always return at least
			// a
			// dummy record holding the sequence
			dh.overwrite("probe-description", {
						tag : 'li',
						id : "probe-description",
						html : "Probe description: " + "[unavailable]"
					});
			return;
		}
		var record = this.getStore().getAt(0);

		// Note this can be a dummy with no real blat result.
		var seq = record.get("blatResult").querySequence;
		var cs = record.get("compositeSequence");

		if (cs !== null) {
			var csDesc = cs.description !== null ? cs.description : "[None provided]";
			dh.overwrite("probe-description", {
						tag : 'li',
						id : "probe-description",
						html : "Probe description: " + csDesc,
						"ext:qtip" : "Provider's description, may not be accurate"
					});
		}

		dh.append("sequence-info", {
					tag : 'li',
					html : "Length: " + seq.length,
					"ext:qtip" : "Sequence length in bases"
				});
		dh.append("sequence-info", {
					tag : 'li',
					html : "Type: " + seq.type.value,
					"ext:qtip" : "Sequence type as classified by Gemma"
				});
		var repeatFrac = seq.fractionRepeats ? Math.round((seq.fractionRepeats * 1000) / 10) : 0;
		dh.append("sequence-info", {
					tag : 'li',
					html : "Repeat-masked bases: " + repeatFrac + "%",
					"ext:qtip" : "Percent bases masked by RepeatMasker"
				});

		dh.append("sequence-info", {
			tag : 'li',
			html : "Sequence: <div ext:qtip='Bases in lower-case were masked by RepeatMasker' class='clob' style='margin:3px;height:30px;font-size:smaller;font-style:courier'>"
					+ seq.sequence + "</div>"
		});

		if (seq.sequenceDatabaseEntry) {
			dh.append("probe-sequence-name", {
						tag : 'a',
						id : "ncbiLink",
						target : "_blank",
						title : "view at NCBI",
						href : "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term="
								+ seq.sequenceDatabaseEntry.accession,
						html : "<img src ='" + Gemma.NCBI_ICON + "'/>",
						"ext:qtip" : "View sequence at NCBI"
					});
		}
	}

});

/**
 * 
 * @class Gemma.ProbeGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.ProbeGrid = Ext.extend(Ext.grid.GridPanel, {

	autoExpandColumn : 'genes',

	pageSize : 30,

	loadMask : {
		msg : "Loading probes ..."
	},

	viewConfig : {
		forceFit : true
	},

	/**
	 * Show first batch of data.
	 * 
	 * @param {Object}
	 *            isArrayDesign
	 * @param {Object}
	 *            id
	 */
	reset : function() {
		if (this.arrayDesignId) {
			this.showArrayDesignProbes(this.arrayDesignId);
		} else {
			this.showprobes(this.csIds);
		}

		// reset the toolbar.
		if (this.paging) {
			this.paging.getEl().unmask();
			this.paging.getEl().select("input,a,button").each(function(e) {
						e.dom.disabled = false;
					});
		}
	},

	arraylink : function(data, metadata, record, row, column, store) {
		return "<a href='/Gemma/arrays/showArrayDesign.html?id=" + record.get("arrayDesignId") + "'>"
				+ record.get("arrayDesignName") + "</a>";
	},

	// CompositeSequenceMapValueObject

	convertgps : function(d) { // not used
		var r = "";
		for (var gp in d) {
			r = r + d[gp].name + ",";
		}
		r = r.substr(0, r.length - 1);
		return r;
	},

	convertgenes : function(d) {
		var r = "";
		var count = 0;
		for (var g in d) {
			if (d[g].id) {
				r = r
						+ "&nbsp;<a  title='View gene details (opens new window)' target='_blank' href='/Gemma/gene/showGene.html?id="
						+ d[g].id + "'>" + d[g].officialSymbol + "</a>,";
				++count;
			}
		}
		if (count > 3) {
			r = "(" + count + ")" + r;
		}
		if (r.length > 0) {
			r = r.substr(0, r.length - 1);// trim tailing comma.
		}
		return r;
	},

	sequencelink : function(data, metadata, record, row, column, store) {
		if (data === "null") {
			return "<a title='[unavailable]'>-</a>";
		}
		return data;
	},

	record : Ext.data.Record.create([{
				name : "compositeSequenceId",
				type : "int"
			}, {
				name : "compositeSequenceName",
				type : "string"
			}, {
				name : "arrayDesignName",
				type : "string"
			}, {
				name : "arrayDesignId",
				type : "int"
			}, {
				name : "bioSequenceId",
				type : "int"
			}, {
				name : "bioSequenceName",
				type : "string"
			}, {
				name : "numBlatHits",
				type : "int"
			}, {
				name : "bioSequenceNcbiId",
				type : "string"
			}, {
				name : "genes"
			}]),

	initComponent : function() {

		var reader = new Ext.data.ListRangeReader({
					id : "compositeSequenceId"
				}, this.record);

		this.isArrayDesign = dwr.util.getValue("arrayDesignId");

		var proxy;
		if (this.isArrayDesign) {
			proxy = new Ext.data.DWRProxy(ArrayDesignController.getCsSummaries);
		} else {
			proxy = new Ext.data.DWRProxy(CompositeSequenceController.getCsSummaries);
		}

		proxy.on("loadexception", this.handleLoadError.createDelegate(this));

		Ext.apply(this, {

			columns : [{
						sortable : true,
						id : 'arraydesign',
						header : "ArrayDesign",
						width : 100,
						dataIndex : "arrayDesignName",
						renderer : this.arraylink.createDelegate(this),
						tooltip : "Name of array design (click for details - leaves this page)"
					}, {
						sortable : true,
						id : 'probe',
						header : "Probe Name",
						width : 130,
						dataIndex : "compositeSequenceName",
						tooltip : "Name of probe"
					}, {
						sortable : true,
						id : 'sequence',
						header : "Sequence",
						width : 130,
						dataIndex : "bioSequenceName",
						renderer : this.sequencelink.createDelegate(this),
						tooltip : "Name of sequence"
					}, {
						sortable : true,
						id : 'hits',
						header : "#Hits",
						width : 50,
						dataIndex : "numBlatHits",
						tooltip : "Number of high-quality BLAT alignments"

					}, {
						sortable : true,
						id : 'genes',
						header : "Genes",
						width : 200,
						dataIndex : "genes",
						tooltip : "Symbols of genes this probe potentially targets; if there are more than 3, the total count is provided in parentheses",
						renderer : this.convertgenes.createDelegate(this)
					}],
			store : new Gemma.PagingDataStore({
						proxy : proxy,
						reader : reader,
						pageSize : this.pageSize
					}),
			selModel : new Ext.grid.RowSelectionModel({
						singleSelect : true
					})

		});

		Ext.apply(this, {
					bbar : new Ext.PagingToolbar({
								pageSize : this.pageSize,
								store : this.store
							})
				});

		if (this.isArrayDesign) {
			Ext.apply(this, {
						tbar : [{
									xtype : 'textfield',
									name : 'search-field',
									emptyText : 'Search for probes',
									id : 'search-field',
									listeners : {
										'specialkey' : {
											fn : function(f, e) {
												if (e.getKey() == e.ENTER) {
													this.search();
												}
											}.createDelegate(this),
											scope : this
										}
									},
									width : 100
								}, {
									xtype : 'button',
									name : 'Search',
									text : 'Search',
									tooltip : 'Search for probes on this array design',
									id : 'search-button',
									handler : this.search.createDelegate(this)

								}, {
									xtype : 'button',
									name : 'Reset',
									text : 'Reset',
									id : 'reset-button',
									tooltip : 'Return to full list',
									handler : this.reset.createDelegate(this)

								}]

					});
		}

		Gemma.ProbeGrid.superclass.initComponent.call(this);

		this.store.on("load", this.loadHandler.createDelegate(this));

		this.getSelectionModel().on("rowselect", function() {
					var sm = this.getSelectionModel();
					var id = sm.getSelected().get("compositeSequenceId");
					this.showDetails(id);
				}.createDelegate(this));

	},

	/**
	 * Event handler for searches. Update the lower grid.
	 * 
	 * @param {Object}
	 *            event
	 */
	search : function(event) {
		if (!this.isArrayDesign) {
			return;
		}
		var id = Ext.get("arrayDesignId").getValue();
		var query = Ext.getCmp('search-field').getValue();

		// swap out table proxy, temporarily.
		var oldprox = this.getStore().proxy;
		this.getStore().proxy = new Ext.data.DWRProxy(CompositeSequenceController.search);
		this.getStore().load({
					params : [query, id]
				});
		this.getStore().proxy = oldprox;
	},

	showprobes : function(ids) {
		// note how we pass the new array in directly, without wrapping it in an
		// object first. We're not returning an object, just a bare array.
		this.getStore().load({
					params : [ids],
					callback : function(r, options, success, scope) {
						if (success) {
							Ext.DomHelper.overwrite("messages", this.getCount() + " probes shown");
						} else {
							Ext.DomHelper.overwrite("messages", "There was an error.");
						}
					}
				});
	},

	showArrayDesignProbes : function(id) {
		this.getStore().load({
					params : [{
								id : id,
								classDelegatingFor : "ArrayDesignImpl"
							}],
					callback : function(r, options, success, scope) {
						if (!success) {
							Ext.DomHelper.overwrite("messages", "There was an error.");
						}
					}
				});
	},

	/**
	 * Event handler for clicks in bottom grid.
	 * 
	 * @param {Object}
	 *            id
	 */
	showDetails : function(id) {
		var record = this.getStore().getById(id);

		this.detailsDataSource.load({
					params : [{
								id : id
							}]
				});

		if (record === undefined) {
			return;
		}
		var csname = record.get("compositeSequenceName");
		var seqName = record.get("bioSequenceName");
		if (!seqName) {
			seqName = "[Unavailable]";
		}
		var arName = record.get("arrayName") ? " on " + record.get("arrayDesignName") : "";

		var dh = Ext.DomHelper;
		dh.overwrite("details-title", {
					tag : 'h2',
					html : "Details for probe: " + csname + arName
				});
		dh.append("details-title", {
					tag : 'ul',
					id : 'sequence-info',
					children : [{
								tag : 'li',
								id : "probe-description",
								html : "Probe description: " + "[pending]"
							}, {
								tag : 'li',
								id : "probe-sequence-name",
								html : "Sequence name: " + seqName + "&nbsp;"
							}]
				});
	},

	/**
	 * Event handler for when the main grid loads: show the first sequence.
	 */
	loadHandler : function() {
		if (this.getStore().getCount() > 0) {
			var v = this.getStore().getAt(0);
			var c = v.get("compositeSequenceId");
			this.showDetails(c);
		}
	},

	handleLoadError : function(scope, b, message, exception) {
		Ext.DomHelper.overwrite("messages", {
					tag : 'img',
					src : '/Gemma/images/iconWarning.gif'
				});
		Ext.DomHelper.overwrite("messages", {
					tag : 'span',
					html : "There was an error while loading data: " + exception
							+ "<br />. Try again or contact the webmaster."
				});
	}

});
