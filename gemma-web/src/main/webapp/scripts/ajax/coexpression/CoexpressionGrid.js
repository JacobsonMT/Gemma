/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

/**
 * Grid for showing coexpression results.
 * 
 * @class Gemma.CoexpressionGrid
 * @extends Ext.grid.GridPanel
 * 
 */

Gemma.SHOW_ONLY_MINE = "Show only my data";
Gemma.SHOW_ALL = "Show all results";

Gemma.CoexpressionGrid = Ext.extend(Ext.grid.GridPanel, {

	collapsible : false,
	editable : false,
	style : "margin-bottom: 1em;",
	height : 300,
	autoScroll : true,
	stateful : false,

	lite : false,

	viewConfig : {
		forceFit : true
	},

	initComponent : function() {

		var si;
		if (this.lite) {
			this.autoScroll = true;
			this.height = 300;
			si = {
				field : 'posSupp',
				direction : 'DESC'
			};
		} else {
			si = {
				field : 'sortKey',
				direction : 'ASC'
			};
		}

		this.ds = new Ext.data.Store({
					proxy : new Ext.data.MemoryProxy([]),
					reader : new Ext.data.ListRangeReader({
								id : "id"
							}, this.record),
					sortInfo : si
				});

		var columns;

		if (this.lite) {
			columns = [{
						id : 'query',
						header : "Query Gene",
						hidden : true,
						dataIndex : "queryGene",
						tooltip : "Query Gene",
						renderer : this.queryGeneStyler.createDelegate(this),
						sortable : true
					}, {
						id : 'visualize',
						header : "Visualize",
						dataIndex : "visualize",
						renderer : this.visStyler.createDelegate(this),
						tooltip : "Link for visualizing raw data",
						sortable : false,
						width : 35

					}, {
						id : 'found',
						header : "Coexpressed Gene",
						dataIndex : "foundGene",
						renderer : this.foundGeneStyler.createDelegate(this),
						tooltip : "Coexpressed Gene",
						sortable : true
					}, {
						id : 'support',
						header : "Support",
						dataIndex : "supportKey",
						width : 70,
						renderer : this.supportStyler.createDelegate(this),
						tooltip : "# of Datasets that confirm coexpression",
						sortable : true
					}, {
						id : 'gene2GeneProteinAssociationStringUrl',
						header : "PPI",
						dataIndex : "gene2GeneProteinAssociationStringUrl",
						width : 30,
						renderer : this.proteinlinkStyler.createDelegate(this),
						tooltip : "Evidence for Protein Protein Interactions from external sources",
						sortable : true
					}]
		} else {
			columns = [{
						id : 'query',
						header : "Query Gene",
						hidden : true,
						dataIndex : "queryGene",
						tooltip : "Query Gene",
						renderer : this.queryGeneStyler.createDelegate(this),
						sortable : true
					}, {
						id : 'details',
						header : "Details",
						dataIndex : 'details',
						renderer : this.detailsStyler.createDelegate(this),
						tooltip : "Links for probe-level details",
						sortable : false,
						width : 30
					}, {
						id : 'visualize',
						header : "Visualize",
						dataIndex : "visualize",
						renderer : this.visStyler.createDelegate(this),
						tooltip : "Link for visualizing raw data",
						sortable : false,
						width : 35

					}, {
						id : 'found',
						header : "Coexpressed Gene",
						dataIndex : "foundGene",
						renderer : this.foundGeneStyler.createDelegate(this),
						tooltip : "Coexpressed Gene",
						sortable : true
					}, {
						id : 'support',
						header : "Support",
						dataIndex : "supportKey",
						width : 75,
						renderer : this.supportStyler.createDelegate(this),
						tooltip : "# of Datasets that confirm coexpression",
						sortable : true
					}, {
						id : 'gene2GeneProteinAssociationStringUrl',
						header : "PPI",
						dataIndex : "gene2GeneProteinAssociationStringUrl",
						width : 30,
						renderer : this.proteinlinkStyler.createDelegate(this),
						tooltip : "Evidence for Protein Protein Interactions from external sources",
						sortable : true
					},

					{
						id : 'go',
						header : "GO Overlap",
						dataIndex : "goSim",
						width : 75,
						renderer : this.goStyler.createDelegate(this),
						tooltip : "GO Similarity Score",
						sortable : true

					}, {
						id : 'datasets',
						header : "Datasets",
						dataIndex : "datasetVector",
						renderer : this.bitImageStyler.createDelegate(this),
						tooltip : "Dataset relevence map",
						sortable : false
					}, {
						id : 'linkOut',
						dataIndex : "foundGene",
						header : "More",
						sortable : false,
						width : 30,
						tooltip : "Links to other websites for more information",
						renderer : this.linkOutStyler
					}]
		}

		Ext.apply(this, {

					columns : columns

				});

		if (!this.lite) {
			Ext.apply(this, {
						tbar : new Ext.Toolbar({
									items : [{
												hidden : !this.user,
												pressed : true,
												enableToggle : true,
												text : Gemma.SHOW_ONLY_MINE,
												tooltip : "Click to show/hide results containing only my data",
												cls : 'x-btn-text-icon details',
												toggleHandler : this.toggleMyData.createDelegate(this)
											}, ' ', ' ', {
												xtype : 'textfield',
												id : this.id + '-search-in-grid',
												tabIndex : 1,
												enableKeyEvents : true,
												emptyText : 'Find gene in results',
												listeners : {
													"keyup" : {
														fn : this.searchForText.createDelegate(this),
														scope : this,
														options : {
															delay : 100
														}
													}
												}
											}]
								})
					});
		}

		Gemma.CoexpressionGrid.superclass.initComponent.call(this);

		this.on("cellclick", this.rowClickHandler.createDelegate(this), this);
	},

	getSupportingDatasetRecords : function(record, grid) {
		var ids = record.data.supportingExperiments;
		var supporting = [];
		var ind = 0;
		// this is quite inefficient, but probably doesn't matter.
		for (var i = 0; i < ids.length; ++i) {
			var id = ids[i];
			for (var j = 0; j < grid.datasets.length; j++) {
				var index = grid.datasets[j].id;
				if (index === id) {
					supporting.push(grid.datasets[j]);
					break;
				}
			}
		}
		return supporting;
	},

	/**
	 * 
	 * @param {}
	 *            isCannedAnalysis
	 * @param {}
	 *            numQueryGenes
	 * @param {}
	 *            data
	 * @param {}
	 *            datasets
	 */
	loadData : function(isCannedAnalysis, numQueryGenes, data, datasets) {
		var queryIndex = this.getColumnModel().getIndexById('query');
		if (numQueryGenes > 1) {
			this.getColumnModel().setHidden(queryIndex, false);
		} else {
			this.getColumnModel().setHidden(queryIndex, true);

		}
		this.getStore().proxy.data = data;
		this.getStore().reload({
					resetPage : true
				});
		// this.getView().refresh(true); // refresh column
		// headers
		if (!this.lite) {
			this.datasets = datasets; // the datasets that are 'relevant'.
			this.resizeDatasetColumn();
		}

		if (this.loadMask) {
			this.loadMask.hide();
		}
	},

	/**
	 * Load the data if there is no data returned an errorState message is set on the result to indicate what the exact
	 * problem was.
	 * 
	 * @param {}
	 *            result
	 */
	loadDataCb : function(result) {
		if (result.errorState) {
			this.handleError(result.errorState);
		} else {
			this.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
					result.knownGeneDatasets);
		}
	},

	doSearch : function(csc) {

		Ext.apply(this, {
					loadMask : new Ext.LoadMask(this.getEl(), {
								msg : "Loading ..."
							})
				});
		this.loadMask.show();
		var errorHandler = this.handleError.createDelegate(this);
		ExtCoexpressionSearchController.doSearch(csc, {
					callback : this.loadDataCb.createDelegate(this),
					errorHandler : errorHandler
				});
	},

	/**
	 * Checks if store contains any results if not print message indicating that there are non. Stop loader. Called when
	 * an error thrown of after data load processing
	 */
	handleError : function(errorMessage) {
		Ext.DomHelper.applyStyles("coexpression-msg", "height: 2.2em");
		Ext.DomHelper.overwrite("coexpression-msg", [{
							tag : 'img',
							src : '/Gemma/images/icons/information.png'
						}, {
							tag : 'span',
							html : "&nbsp;&nbsp;" + errorMessage
						}]);

		this.loadMask.hide();
		this.hide();
	},

	clearError : function() {
		Ext.DomHelper.overwrite("coexpression-messages", "");
	},

	toggleMyData : function(btn, pressed) {
		var buttonText = btn.getText();
		if (buttonText == Gemma.SHOW_ALL) {
			this.getStore().clearFilter();
			btn.setText(Gemma.SHOW_ONLY_MINE);
		} else {
			this.getStore().filterBy(function(r, id) {
						return r.get("containsMyData")
					}, this, 0);
			btn.setText(Gemma.SHOW_ALL);
		}
	},

	searchForText : function(button, keyev) {
		var text = Ext.getCmp(this.id + '-search-in-grid').getValue();
		if (text.length < 2) {
			this.getStore().clearFilter();
			return;
		}
		this.getStore().filterBy(this.filter(text), this, 0);
	},

	filter : function(text) {
		var value = new RegExp(Ext.escapeRe(text), 'i');
		return function(r, id) {
			var foundGene = (r.get("foundGene"));
			if (value.test(foundGene.officialSymbol)) {
				return true;
			}

			return false;
		}
	},

	record : Ext.data.Record.create([{
				name : "queryGene",
				sortType : function(g) {
					return g.officialSymbol;
				}
			}, {
				name : "foundGene",
				sortType : function(g) {
					return g.officialSymbol;
				}
			}, {
				name : "sortKey",
				type : "string"
			}, {
				name : "supportKey",
				type : "int",
				sortType : Ext.data.SortTypes.asInt,
				sortDir : "DESC"
			}, {
				name : "posSupp",
				type : "int"
			}, {
				name : "negSupp",
				type : "int"
			}, {
				name : "numTestedIn",
				type : "int"
			}, {
				name : "nonSpecPosSupp",
				type : "int"
			}, {
				name : "nonSpecNegSupp",
				type : "int"
			}, {
				name : "hybWQuery",
				type : "boolean"
			}, {
				name : "goSim",
				type : "int"
			}, {
				name : "maxGoSim",
				type : "int"
			}, {
				name : "datasetVector",
				type : "string"
			}, {
				name : "supportingExperiments"
			}, {
				name : "gene2GeneProteinAssociationStringUrl",
				type : "string"
			}, {
				name : "gene2GeneProteinInteractionEvidence",
				type : "string"
			}, {
				name : "gene2GeneProteinInteractionConfidenceScore",
				type : "string"
			}, {
				name : "containsMyData",
				type : "boolean"
			}]),

	linkOutStyler : function(value, metadata, record, row, col, ds) {

		var call = "Gemma.CoexpressionGrid.getAllenAtlasImage(\'" + value.officialSymbol + "\')";

		return String
				.format(
						'<span onClick="{0}" id="aba-{1}-button"><img height=15 width =15 src="/Gemma/images/logo/aba-icon.png" ext:qtip="Link to expression data from the Allen Brain Atlas for {2}" /> </span>',
						call, value.officialSymbol, value.officialSymbol);
	},

	// link for protein interactions
	proteinlinkStyler : function(value, metadata, record, row, col, ds) {

		var d = record.data;

		if (d.gene2GeneProteinAssociationStringUrl) {
			return String
					.format(
							'<span> <a href="{0}"  target="_blank" class="external"><img "src="/Gemma/images/logo/string_logo.gif" ext:qtip="Click to view the protein protein interaction obtained from {1} evidence with a combined association score of {2} from STRING" /></a> </span>',
							d.gene2GeneProteinAssociationStringUrl, d.gene2GeneProteinInteractionEvidence,
							d.gene2GeneProteinInteractionConfidenceScore);
		}

	},

	/**
	 * 
	 */
	supportStyler : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		if (d.posSupp || d.negSupp) {
			var s = "";
			if (d.posSupp) {
				s = s +
						String.format("<span class='positiveLink'>{0}{1}</span> ", d.posSupp, this
										.getSpecificLinkString(d.posSupp, d.nonSpecPosSupp));
			}
			if (d.negSupp) {
				s = s +
						String.format("<span class='negativeLink'>{0}{1}</span> ", d.negSupp, this
										.getSpecificLinkString(d.negSupp, d.nonSpecNegSupp));
			}

			if (d.numTestedIn) {
				s = s + String.format("/ {0}", d.numTestedIn);
			}

			return s;
		} else {
			return "-";
		}
	},

	/**
	 * For displaying Gene ontology similarity
	 * 
	 */
	goStyler : function(value, metadata, record, row, col, ds) {
		var d = record.data;
		if (d.goSim || d.maxGoSim) {
			return String.format("{0}/{1}", d.goSim, d.maxGoSim);
		} else {
			return "-";
		}
	},

	getSpecificLinkString : function(total, nonSpecific) {
		return nonSpecific ? String.format("<span class='specificLink'> ({0})</span>", total - nonSpecific) : "";
	},

	/**
	 * Display the target (found) genes.
	 * 
	 */
	foundGeneStyler : function(value, metadata, record, row, col, ds) {

		var g = record.data.foundGene;

		if (g.officialName === null) {
			g.officialName = "";
		}

		if (g.taxonId != null) {
			g.taxonId = g.taxonId;
			g.taxonName = g.taxonCommonName;
		} else {
			g.taxonId = -1;
			g.taxonName = "?";
		}

		return this.foundGeneTemplate.apply(g);
	},

	/**
	 * FIXME this should use the same analysis as the last query. Here we always use 'All'.
	 */
	foundGeneTemplate : new Ext.Template(
			"<a href='/Gemma/searchCoexpression.html?g={id}&s=3&t={taxonId}&an=All {taxonName}'> <img src='/Gemma/images/logo/gemmaTiny.gif' ext:qtip='Make {officialSymbol} the query gene' /> </a>",
			" &nbsp; ", "<a href='/Gemma/gene/showGene.html?id={id}'>{officialSymbol}</a> {officialName}"),

	queryGeneStyler : function(value, metadata, record, row, col, ds) {

		var g = record.data.queryGene;

		if (g.officialName === null) {
			g.officialName = "";
		}

		g.abaGeneUrl = record.data.abaQueryGeneUrl;

		return this.foundGeneTemplate.apply(g);
	},
	bitImageStyler : function(value, metadata, record, row, col, ds) {
		var bits = record.data.datasetVector;
		var width = bits.length * Gemma.CoexpressionGrid.bitImageBarWidth;
		var gap = 0;

		var s = ''
		var maxheight = 0;
		for (var i = 0; i < bits.length; ++i) {
			if (i > 0) {
				s = s + ",";
			}
			var state = bits.charAt(i);
			var b = "";
			if (state === "0") {
				b = "0"; // not tested
			} else if (state === "1") {
				b = "2"; // tested but no support
				if (2 > maxheight) {
					maxheight = 2;
				}
			} else if (state === "2") {
				b = "10"; // supported but nonspecific
				if (10 > maxheight) {
					maxheight = 10;
				}
			} else if (state === "3") {
				maxheight = Gemma.CoexpressionGrid.bitImageBarHeight;
				b = Gemma.CoexpressionGrid.bitImageBarHeight; // supported
				// and
				// specific
			}
			s = s + b;
		}

		var result = '<span style="margin:0;padding-top:' + (Gemma.CoexpressionGrid.bitImageBarHeight - maxheight) +
				'px;height:' + Gemma.CoexpressionGrid.bitImageBarHeight + ';background-color:#EEEEEE" >' +
				'<img style="vertical-align:bottom" src="/Gemma/spark?type=bar&width=' + width + '&height=' +
				maxheight + '&highcolor=black&color=black&spacing=' + gap + '&data=';

		// dataset-bits is defined in typo.css

		// eeMap is created in CoexpressionSearch.js
		result = result + s + '" usemap="#eeMap" /></span>';
		return result;
	},

	visStyler : function(value, metadata, record, row, col, ds) {
		return "<img src='/Gemma/images/icons/chart_curve.png' ext:qtip='Visualize the data' />";
	},

	detailsStyler : function(value, metadata, record, row, col, ds) {
		return "<img src='/Gemma/images/icons/magnifier.png' ext:qtip='Show probe-level details' /> ";
	},

	// Creates a link for downloading raw dedv's data in tab
	// delimited format
	// currently not used as same function in visulazation
	// widget
	downloadDedvStyler : function(value, metadata, record, row, col, ds) {

		var queryGene = record.data.queryGene;
		var foundGene = record.data.foundGene;

		var activeExperimentsString = "";
		var activeExperimentsSize = record.data.supportingExperiments.size();

		for (var i = 0; i < activeExperimentsSize; i++) {
			if (i === 0) {
				activeExperimentsString = record.data.supportingExperiments[i];
			} else {
				activeExperimentsString = String.format("{0}, {1}", activeExperimentsString,
						record.data.supportingExperiments[i]);
			}
		}

		return String.format("<a href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1},{2}' > download </a>",
				activeExperimentsString, queryGene.id, foundGene.id);
	},

	/**
	 * 
	 */
	resizeDatasetColumn : function() {
		var first = this.getStore().getAt(0);
		if (first) {
			var cm = this.getColumnModel();
			var c = cm.getIndexById('datasets');
			var headerWidth = this.view.getHeaderCell(c).firstChild.scrollWidth;
			var imageWidth = Gemma.CoexpressionGrid.bitImageBarWidth * first.data.datasetVector.length;
			cm.setColumnWidth(c, imageWidth < headerWidth ? headerWidth : imageWidth);
		}
	},

	/**
	 * 
	 * @param {}
	 *            grid
	 * @param {}
	 *            rowIndex
	 * @param {}
	 *            columnIndex
	 * @param {}
	 *            e
	 */
	rowClickHandler : function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);
			var queryGene = record.get("queryGene");
			var foundGene = record.get("foundGene");

			if (fieldName == 'foundGene' && columnIndex != 7) {
				// problem
				// with
				// outlink
				// column
				// field
				// name
				// also
				// returns
				// name
				// as
				// foundGene
				// searchPanel.searchForGene(foundGene.id);
			} else if (fieldName == 'visualize') {

				var foundGene = record.data.foundGene;
				var activeExperiments = record.data.supportingExperiments;
				// destroy if already open???
				if (this.coexpVisWindow != null) {
					this.coexpVisWindow.close();
					this.coexpVisWindow = null;
				}

				this.coexpVisWindow = new Gemma.CoexpressionVisualizationWindow({
							admin : false,
							experiments : activeExperiments,
							queryGene : queryGene,
							foundGene : foundGene,
							downloadLink : String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1},{2}",
									activeExperiments.join(','), queryGene.id, foundGene.id),
							title : "Coexpression for:  " + queryGene.name + " + " + foundGene.name
						});

				var params = [];
				params.push(activeExperiments)
				params.push(queryGene.id);
				params.push(foundGene.id);

				this.coexpVisWindow.show({
							params : params
						});

			} else if (fieldName == 'details' && !this.lite) {

				var supporting = this.getSupportingDatasetRecords(record, grid);

				var dsGrid = new Gemma.ExpressionExperimentGrid({
							records : supporting,
							// width : 750,
							// height : 340, Layout will show nothing if
							// this isn't set to something and
							// autoHeight is false.
							// Most likely a loading issue (no data in
							// store, so no height).
							// autoHeight : true,
							stateful : false
						});

				// Close if already open
				if (this.detailsWindow !== undefined && this.detailsWindow != null) {
					this.detailsWindow.close();
				}

				var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							geneId : foundGene.id,
							threshold : 0.01,
							// width : 750,
							// height : 380,
							stateful : false
						});

				var detailsTP = new Ext.TabPanel({
							layoutOnTabChange : true,
							activeTab : 0,
							stateful : false,
							items : [{
										title : "Supporting datasets",
										items : [dsGrid],
										layout : 'fit',
										autoScroll : true
									}, {
										title : "Differential expression of " + foundGene.officialSymbol,
										items : [diffExGrid],
										layout : 'fit',
										autoScroll : true,
										loaded : false,
										listeners : {
											"activate" : {
												fn : function() {
													if (!this.loaded) {
														diffExGrid.getStore().load({
															params : [foundGene.id, Gemma.DIFF_THRESHOLD,
																	Gemma.MAX_DIFF_RESULTS]
														});
													}
													this.loaded = true;
												}
											}
										}

									}]

						});

				this.detailsWindow = new Ext.Window({
							modal : false,
							layout : 'fit',
							title : 'Details for ' + foundGene.officialSymbol,
							closeAction : 'close',
							items : [{
										items : [detailsTP],
										layout : 'fit'
									}],
							width : 760,
							height : 400,
							// autoScroll : true,
							stateful : false
						});

				dsGrid.getStore().load();
				this.detailsWindow.show();

				diffExGrid.getStore().loadData(supporting);

			}
		}
	}

});

Gemma.CoexpressionGrid.bitImageBarHeight = 15;
Gemma.CoexpressionGrid.bitImageBarWidth = 1;

Gemma.CoexpressionGrid.getBitImageMapTemplate = function() {
	if (Gemma.CoexpressionGrid.bitImageMapTemplate === undefined) {
		Gemma.CoexpressionGrid.bitImageMapTemplate = new Ext.XTemplate(
				'<tpl for=".">',
				'<area shape="rect" coords="{[ (xindex - 1) * this.barx ]},0,{[ xindex * this.barx ]},{[ this.bary ]}" ext:qtip="{name}" href="{externalUri}" />',
				'</tpl>', {
					barx : Gemma.CoexpressionGrid.bitImageBarWidth,
					bary : Gemma.CoexpressionGrid.bitImageBarHeight - 1
				});
	}
	return Gemma.CoexpressionGrid.bitImageMapTemplate;
};

Gemma.CoexpressionGrid.getAllenAtlasImage = function(geneSymbol) {
	LinkOutController.getAllenBrainAtlasLink(geneSymbol, Gemma.CoexpressionGrid.linkOutPopUp);

	/*
	 * Show the throbber
	 */

	Ext.DomHelper.overwrite("aba-" + geneSymbol + "-button", {
				tag : 'img',
				src : '/Gemma/images/default/tree/loading.gif'
			});
};

/**
 * Callback.
 */
Gemma.CoexpressionGrid.linkOutPopUp = function(linkOutValueObject) {

	/*
	 * Put the aba icon back for the throbber.
	 */
	Ext.DomHelper.overwrite("aba-" + linkOutValueObject.geneSymbol + "-button", {
				tag : 'img',
				src : '/Gemma/images/logo/aba-icon.png'
			});

	// TODO: Make pop up window show more than one image (have a button for
	// scrolling to next image)
	var popUpHtml;

	if (linkOutValueObject.abaGeneImageUrls.length == 0) {
		window.alert("No Allen Brain Atlas images available for this gene");
		return;
	} else {
		popUpHtml = String.format("<img height=200 width=400 src={0}>", linkOutValueObject.abaGeneImageUrls[0]);
	}

	var abaWindowId = "coexpressionAbaWindow";

	var popUpLinkOutWin = Ext.getCmp(abaWindowId);
	if (popUpLinkOutWin != null) {
		popUpLinkOutWin.close();
		popUpLinkOutWin = null;
	}

	popUpLinkOutWin = new Ext.Window({
				id : abaWindowId,
				html : popUpHtml,
				stateful : false,
				resizable : false
			});
	popUpLinkOutWin
			.setTitle("<a href='" +
					linkOutValueObject.abaGeneUrl +
					"' target='_blank'>  <img src='/Gemma/images/logo/aba-icon.png' ext:qtip='Link to Allen Brain Atlas gene details' />  </a> &nbsp; &nbsp;<img height=15  src=/Gemma/images/abaExpressionLegend.gif>  " +
					linkOutValueObject.geneSymbol);

	// An attempt at adding a button to the window so that the different image
	// from allen brain atlas could be seen clicking on it.
	// failed to work because window wouldn't refresh with new html information.
	// :(
	// Also was a host of scope issue... should have made in own widget.
	// popUpLinkOutWin.linkOutValueObject = linkOutValueObject;
	// popUpLinkOutWin.currentImageIndex = 0;
	//
	// popUpLinkOutWin.nextImage = function(e){
	//					
	// if (e.scope.currentImageIndex ==
	// e.scope.linkOutValueObject.abaGeneImageUrls.length)
	// e.scope.currentImageIndex = 0;
	// else
	// e.scope.currentImageIndex++;
	//						
	// e.scope.innerHTML= String.format("<img height=200 width=400 src={0}>",
	// e.scope.linkOutValueObject.abaGeneImageUrls[e.scope.currentImageIndex]);
	// e.scope.render();
	//					
	// };
	// popUpLinkOutWin.addButton('next image',
	// popUpLinkOutWin.nextImage.createDelegate(this), popUpLinkOutWin);

	popUpLinkOutWin.show(this);

};
