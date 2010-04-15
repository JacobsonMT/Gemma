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

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 75;

/**
 * 
 */
Gemma.GeneGOGrid = Ext.extend(Gemma.GemmaGridPanel, {
			record : Ext.data.Record.create([{
						name : "id",
						type : "int"
					}, {
						name : "termUri"
					}, {
						name : "termName"
					}, {
						name : "evidenceCode"
					}]),

			golink : function(d) {
				var g = d.replace("_", ":");
				return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
						+ g + "'>" + g + "</a>";
			},

			initComponent : function() {
				Ext.apply(this, {
							columns : [{
										header : "ID",
										dataIndex : "termUri",
										renderer : this.golink
									}, {
										header : "Term",
										dataIndex : "termName"
									}, {
										header : "Evidence Code",
										dataIndex : "evidenceCode"
									}],

							store : new Ext.data.Store({
										proxy : new Ext.data.DWRProxy(GeneController.findGOTerms),
										reader : new Ext.data.ListRangeReader({
													id : "id"
												}, this.record),
										remoteSort : false
									})
						});

				Gemma.GeneGOGrid.superclass.initComponent.call(this);

				this.getStore().setDefaultSort('termUri');

				this.getStore().load({
							params : [this.geneid]
						});
			}

		});

/**
 * 
 */
Gemma.GeneProductGrid = Ext.extend(Gemma.GemmaGridPanel, {

			record : Ext.data.Record.create([{
						name : "id"
					}, {
						name : "name"
					}, {
						name : "description"
					}, {
						name : "type",
						convert : function(d) {
							return d.value;
						}.createDelegate()
					}]),

			initComponent : function() {
				Ext.apply(this, {
							columns : [{
										header : "Name",
										dataIndex : "name"
									}, {
										header : "Type",
										dataIndex : "type"
									}, {
										header : "Description",
										dataIndex : "description"
									}],

							store : new Ext.data.Store({
										proxy : new Ext.data.DWRProxy(GeneController.getProducts),
										reader : new Ext.data.ListRangeReader({
													id : "id"
												}, this.record),
										remoteSort : false
									})
						});

				Gemma.GeneProductGrid.superclass.initComponent.call(this);

				this.getStore().setDefaultSort('type', 'name');

				this.getStore().load({
							params : [this.geneid]
						});
			}

		});

Gemma.GeneGroupDataView = Ext.extend(Ext.DataView, {

	readMethod : GeneSetController.findGeneSetsByGene,

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name",
				type : "string"
			}, {
				name : "description",
				type : "string"
			}, {
				name : "owner"
			}, {
				name : "size",
				type : "int"
			}]),

	getReadParams : function() {
		return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
	},

	/*
	 * Assumes search scope should be experiments only ..
	 */
	tpl : new Ext.XTemplate(
			'<tpl for=".">',
			'<span class="ann-wrap" ext:qtip="{description}" ><span  class="x-editable">'
					+ '<a ext:qtip="{description} : {size}" href="/Gemma/geneGroupManager.html" style="text-decoration:underline;">{name}</a></span></span>&nbsp;&nbsp;',
			'</tpl>'),

	itemSelector : 'ann-wrap',
	emptyText : 'Not currently a member of any gene group',

	initComponent : function() {

		Ext.apply(this, {
					store : new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(this.readMethod),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record)
							})
				});

		Gemma.GeneGroupDataView.superclass.initComponent.call(this);

		this.store.load({
					params : this.getReadParams()
				});
	}

});

Ext.onReady(function() {

			Ext.QuickTips.init();

			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			geneid = dwr.util.getValue("gene");

			var gpGrid = new Gemma.GeneProductGrid({
						geneid : geneid,
						renderTo : "geneproduct-grid",
						height : 200,
						width : 400
					});

			var gogrid = new Gemma.GeneGOGrid({
						renderTo : "go-grid",
						geneid : geneid,
						height : 200,
						width : 500
					});

			var geneGroupDataView = new Gemma.GeneGroupDataView({
						renderTo : "gene-group-grid",
						readParams : [geneid]
					});

			// Coexpression grid.

			var coexpressedGeneGrid = new Gemma.CoexpressionGrid({
						width : 400,
						colspan : 2,
						// user : false,
						lite : true,
						renderTo : "coexpression-grid"
					});

			coexpressedGeneGrid.doSearch({
						geneIds : [geneid],
						quick : true,
						stringency : 2,
						forceProbeLevelSearch : false
					});

			// diff expression grid

			var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
						width : 725,
						height : 200,
						renderTo : "diff-grid"
					});
			// Hide Expression experiment full name
			// var eeNameColumnIndex =
			// diffExGrid.getColumnModel().getIndexById('expressionExperimentName');
			// diffExGrid.getColumnModel().setHidden(eeNameColumnIndex, true);
			var visColumnIndex = diffExGrid.getColumnModel().getIndexById('visualize');
			diffExGrid.getColumnModel().setHidden(visColumnIndex, false);

			diffExGrid.getStore().load({
						params : [geneid, Gemma.DIFF_THRESHOLD, Gemma.MAX_DIFF_RESULTS]
					});

		});

Gemma.geneLinkOutPopUp = function(abaImageUrl) {

	if (abaImageUrl == null)
		return;

	var abaWindowId = "geneDetailsAbaWindow";
	var win = Ext.getCmp(abaWindowId);
	if (win != null) {
		win.close();
	}

	win = new Ext.Window({
		html : "<img src='" + abaImageUrl + "'>",
		id : abaWindowId,
		stateful : false,
		title : "<img height='15'  src='/Gemma/images/abaExpressionLegend.gif'>"
			// ,
			// width : 500,
			// height : 400,
			// autoScroll : true
		});
	win.show(this);

};
