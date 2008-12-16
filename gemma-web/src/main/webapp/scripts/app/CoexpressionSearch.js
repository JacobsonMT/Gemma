Ext.namespace("Gemma");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var admin = dwr.util.getValue("hasAdmin");
	var user  = dwr.util.getValue("hasUser");
	
	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('coexpression-all', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	var getSupportingDatasetRecords = function(record, grid) {
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
	};

	var geneRowClickHandler = function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);
			var queryGene = record.get("queryGene");
			var foundGene = record.get("foundGene");

			if (fieldName == 'foundGene') {
				searchPanel.searchForGene(foundGene.id);
			} else if (fieldName == 'visualize') {

				var foundGene = record.data.foundGene;
				var activeExperiments = record.data.supportingExperiments;

				// destroy if already open
				if (visWindow) {
					visWindow.close();
				}

				visWindow = new Gemma.VisualizationWindow({
							admin : admin
						});
				visWindow.displayWindow(activeExperiments, queryGene, foundGene);
			} else if (fieldName == 'details') {

				var supporting = getSupportingDatasetRecords(record, grid);

				var dsGrid = new Gemma.ExpressionExperimentGrid({
							records : supporting,
							width : 750,
							height : 380,
							stateful : false
						});

				dsGrid.getStore().load();

				//Close if already open
				if (detailsWindow){
					detailsWindow.close();
				}
				
				var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							geneId : foundGene.id,
							threshold : 0.01,
							width : 750,
							height : 380,
							stateful : false,
							autoScroll : true 
						});

				var detailsTP = new Ext.TabPanel({
							layoutOnTabChange : true,
							width : 760,
							height : 400,
							activeTab : 0,
							stateful : false,
							items : [{
										title : "Supporting datasets",
										items : [dsGrid]
									}, {
										title : "Differential expression of " + foundGene.officialSymbol,
										items : [diffExGrid],
										loaded : false,
										listeners : {
											"activate" : {
												fn : function() {
													if (!this.loaded) {
														diffExGrid.getStore().load({
																	params : [foundGene.id, 0.01]
																});
													}
													this.loaded = true;
												}
											}
										}

									}]

						});

				 detailsWindow = new Ext.Window({
							modal : false,
							//layout : 'fit',
							title : 'Details for ' + foundGene.officialSymbol,
							closeAction : 'close',
							items : [detailsTP],
							width : 760,
							height : 400, 
							stateful : false 
						});

				detailsWindow.show();

				diffExGrid.getStore().loadData(supporting);

			}
		}
	};

	var searchPanel = new Gemma.CoexpressionSearchForm({
				admin : admin,
				user : user,
				id : 'searchpanel'
			});

	var summaryPanel = new Ext.Panel({
				width : 300,
				id : 'summarypanel',
				height : 300
			});

	var knownGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
				width : 800,
				autoHeight : true,
				colspan : 2
			});

	var knownGeneGrid = new Gemma.CoexpressionGrid({
				width : 800,
				title : "Coexpressed genes",
				autoHeight : true,
				pageSize : 25,
				colspan : 2,
				user : user
				
			});

	var items = [searchPanel, summaryPanel, knownGeneDatasetGrid, knownGeneGrid];

	if (admin) {

		/*
		 * Note: doing allPanel.add doesn't work. Probably something to do with the table layout; indeed:
		 * https://extjs.com/forum/showthread.php?p=173090
		 */

		predictedGeneDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					adjective : "predicted gene",
					collapsed : true
				});

		predictedGeneGrid = new Gemma.CoexpressionGrid({
					width : 800,
					title : "Coexpressed predicted genes",
					pageSize : 25,
					id : 'pred-gene-grid',
					colspan : 2,
					autoHeight : true
				});

		probeAlignedDatasetGrid = new Gemma.CoexpressionDatasetGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					id : 'par-dataset-grid',
					adjective : "probe-aligned region",
					collapsed : true
				});

		probeAlignedGrid = new Gemma.CoexpressionGrid({
					width : 800,
					colspan : 2,
					autoHeight : true,
					title : "Coexpressed probe-aligned regions",
					pageSize : 25
				});

		items.push(predictedGeneDatasetGrid);
		items.push(predictedGeneGrid);
		items.push(probeAlignedDatasetGrid);
		items.push(probeAlignedGrid);

		probeAlignedGrid.on("cellclick", geneRowClickHandler, probeAlignedGrid);
		predictedGeneGrid.on("cellclick", geneRowClickHandler, predictedGeneGrid);
	}

	var allPanel = new Ext.Panel({
				layout : 'table',
				renderTo : 'coexpression-all',
				baseCls : 'x-plain-panel',
				autoHeight : true,
				width : 900,
				layoutConfig : {
					columns : 2
				},
				items : items,
				enabled : false
			});

	var predictedGeneDatasetGrid;
	var probeAlignedDatasetGrid;
	var predictedGeneGrid;
	var probeAlignedGrid;
	var visWindow;
	var detailsWindow;

	knownGeneGrid.on("cellclick", geneRowClickHandler, knownGeneGrid);

	/*
	 * Handler for the coexpression search results.
	 */
	searchPanel.on("aftersearch", function(panel, result) {

				/*
				 * Report any errors.
				 */
				if (result.errorState) {
					Ext.DomHelper.overwrite('coexpression-messages', result.errorState);
					return;
				}

				var eeMap = {};
				if (result.datasets) {
					for (var i = 0; i < result.datasets.length; ++i) {
						var ee = result.datasets[i];
						eeMap[ee.id] = ee;
					}
				}

				Ext.DomHelper.overwrite('summarypanel', "");
				summaryPanel = new Gemma.CoexpressionSummaryGrid({
							genes : result.queryGenes,
							renderTo : "summarypanel",
							summary : result.summary
						});

				// create expression experiment image map
				var imageMap = Ext.get("eeMap");
				if (!imageMap) {
					imageMap = Ext.getBody().createChild({
								tag : 'map',
								id : 'eeMap',
								name : 'eeMap'
							});
				}

				Gemma.CoexpressionGrid.getBitImageMapTemplate().overwrite(imageMap, result.datasets);

				var link = panel.getBookmarkableLink();
				knownGeneGrid
						.setTitle(String
								.format(
										"Coexpressed genes <a href='{0}'>(bookmarkable link)</a> <a href='{0}&export'>(export as text)</a>",
										link));

				Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.knownGeneDatasets, eeMap);
				knownGeneDatasetGrid.loadData(result.knownGeneDatasets);
				knownGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
						result.knownGeneDatasets);

				if (admin) {
					Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.predictedGeneDatasets, eeMap);
					predictedGeneDatasetGrid.loadData(result.predictedGeneDatasets);
					predictedGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
							result.predictedGeneResults, result.predictedGeneDatasets);

					Gemma.CoexpressionDatasetGrid.updateDatasetInfo(result.probeAlignedRegionDatasets, eeMap);
					probeAlignedDatasetGrid.loadData(result.probeAlignedRegionDatasets);
					probeAlignedGrid.loadData(result.isCannedAnalysis, result.queryGenes.length,
							result.probeAlignedRegionResults, result.probeAlignedRegionDatasets);
				}
			});

});
