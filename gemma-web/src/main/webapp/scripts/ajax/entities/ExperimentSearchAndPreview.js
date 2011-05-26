/**
 * @author thea
 * @version $Id: ExperimentSearchAndPreview.js,v 1.7 2011/05/06 01:05:42
 *          tvrossum Exp $
 */
Ext.namespace('Gemma');

Gemma.ExperimentSearchAndPreview = Ext.extend(Ext.Panel, {
	taxonId: null, // might be set by parent to control combo
	/**
	 * Show the selected eeset members
	 */
	loadExperimentOrGroup : function(record, query) {

		this.selectedExperimentOrGroupRecord = record.data;

		var id = record.data.reference.id;
		var isGroup = record.get("isGroup");
		var type = record.get("type");
		var reference = record.get("reference");
		var name = record.get("name");

		var taxonId = record.get("taxonId");
		var taxonName = record.get("taxonName");
		
		// load preview of group if group was selected
		if (isGroup) {
			eeIds = record.get('memberIds');
			this.experimentGroupId = id;
			if (!eeIds || eeIds === null || eeIds.length === 0) {
				return;
			}
			this.loadExperiments(eeIds);

		}
		// load single experiment if experiment was selected
		else {
			this.experimentIds = [id];
			this.searchForm.experimentIds = [id];
			// reset the experiment preview panel content
			this.resetExperimentPreview();

			// update the gene preview panel content
			this.experimentPreviewContent.update({
						shortName : record.get("name"),
						name : record.get("description"),
						id : record.get("id"),
						taxon : record.get("taxonName")
					});
			this.experimentPreviewContent.setTitle(
				'<span style="font-size:1.2em">'+this.experimentCombo.getRawValue()
				+'</span> &nbsp;&nbsp;<span style="font-weight:normal">(1 experiment)');
			this.experimentSelectionEditorBtn.setText('0 more');
			this.experimentSelectionEditorBtn.disable();
			this.experimentSelectionEditorBtn.show();
			this.experimentPreviewContent.expand();
		}
	},

	/**
	 * update the contents of the gene preview box and the this.geneIds value
	 * using a list of gene Ids
	 * 
	 * @param geneIds
	 *            an array of geneIds to use
	 */
	loadExperiments : function(ids) {

		// store selected ids for searching
		this.searchForm.experimentIds.push(ids);
		this.experimentIds = ids;

		this.loadExperimentPreview();

	},

	/**
	 * update the contents of the epxeriment preview box using the
	 * this.experimentIds value
	 * 
	 * @param geneIds
	 *            an array of geneIds to use
	 */
	loadExperimentPreview : function() {

		this.maskExperimentPreview();

		// store selected ids for searching
		ids = this.experimentIds;

		if (!ids || ids === null || ids.length === 0) {
			this.resetExperimentPreview();
			return;
		}

		// reset the experiment preview panel content
		this.resetExperimentPreview();

		// load some experiments for previewing
		var limit = (ids.size() < this.searchForm.PREVIEW_SIZE) ? ids.size() : this.searchForm.PREVIEW_SIZE;
		var idsToPreview = [];
		for (var i = 0; i < limit; i++) {
			idsToPreview[i] = ids[i];
		}
		ExpressionExperimentController.loadExpressionExperiments(idsToPreview, function(ees) {

					for (var j = 0; j < ees.size(); j++) {
						this.experimentPreviewContent.update(ees[j]);
					}
					this.experimentPreviewContent.setTitle(
						'<span style="font-size:1.2em">'+this.experimentCombo.getRawValue()+'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + ids.size() + " experiments)");
					this.experimentSelectionEditorBtn.setText('<a>' + (ids.size() - limit) + ' more - Edit</a>');
					this.showExperimentPreview();

					if (ids.size() === 1) {
						this.experimentSelectionEditorBtn.setText('0 more');
						this.experimentSelectionEditorBtn.disable();
						this.experimentSelectionEditorBtn.show();
					}

				}.createDelegate(this));

	},

	/**
	 * 
	 */
	launchExperimentSelectionEditor : function() {
		this.searchForm.getEl().mask();

		this.experimentSelectionEditorWindow.show();

		this.experimentSelectionEditor.loadMask = new Ext.LoadMask(this.experimentSelectionEditor.getEl(), {
					msg : "Loading experiments ..."
				});
		this.experimentSelectionEditor.loadMask.show();
		Ext.apply(this.experimentSelectionEditor, {
					experimentGroupId : this.experimentGroupId,
					selectedExperimentGroup : this.experimentCombo.getExpressionExperimentGroup(),
					groupName : this.experimentCombo.getExpressionExperimentGroup().name
				});
		this.experimentSelectionEditor.loadExperiments(this.experimentIds, function() {
					this.experimentSelectionEditor.loadMask.hide();
				}.createDelegate(this, [], false));
	},

	/**
	 * 
	 */
	maskExperimentPreview : function() {
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Loading Experiments ..."
					});
		}
		this.loadMask.show();
	},

	showExperimentPreview : function() {
		this.loadMask.hide();
		this.experimentSelectionEditorBtn.enable();
		this.experimentSelectionEditorBtn.show();
		this.experimentPreviewContent.show();
		this.experimentPreviewContent.expand();
	},
	resetExperimentPreview : function() {
		Ext.DomHelper.overwrite(this.experimentPreviewContent.body, {
					cn : ''
				});
		// this.experimentPreviewExpandBtn.disable().hide();
		// this.experimentSelectionEditorBtn.disable().hide();
		// this.experimentPreviewContent.collapse();
	},

	initComponent : function() {

		/**
		 * **** EE COMBO
		 * ***************************************************************************
		 */

		// Shows the combo box for EE groups
		this.newBoxTriggered = false;
		this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo({
					typeAhead : false,
					width : 300,
					taxonId: this.taxonId
					
				});
		this.experimentCombo.on('select', function(combo, record, index) {

					// if the EE has changed taxon, reset the gene combo
					this.searchForm.taxonChanged(record.get("taxonId"), record.get("taxonName"));
					
					this.experimentSelectionEditor.setTaxonId(record.get("taxonId"));

					// store the eeid(s) selected and load some EE into the
					// previewer
					// store the taxon associated with selection
					var query = combo.store.baseParams.query;
					this.loadExperimentOrGroup(record, query);
					this.experimentPreviewContent.show();

					// if this was the first time a selection was made using
					// this box
					if (combo.startValue === '' && this.newBoxTriggered === false) {
						this.fireEvent('madeFirstSelection');
						this.newBoxTriggered = true;
						this.helpBtn.hide();
						this.removeBtn.show();
						// this.relayEvents(this.experimentCombo, ['select']);
					}
					combo.disable().hide();
					//this.selectionTitle.update(combo.getValue());
					this.removeBtn.setPosition(300,0);
					this.doLayout();

				}, this);

		/**
		 * ***** EXPERIMENT SELECTION EDITOR
		 * *****************************************************************
		 */

		this.experimentSelectionEditor = new Gemma.ExpressionExperimentMembersGrid({
					// id : 'experimentSelectionEditor',
					name : 'experimentSelectionEditor',
					height : 200,
					// hidden: 'true',
					hideHeaders : true,
					frame : false,
					queryText : this.experimentCombo.getValue()
				});

		this.experimentSelectionEditor.on('experimentListModified', function(newRecords) {
					var i;
					for (i = 0; i < newRecords.length; i++) { // should only
						// be one
						if (newRecords[i]) {
							this.loadExperiments(newRecords[i].expressionExperimentIds);
							// update record
							this.selectedExperimentOrGroupRecord = newRecords[i];
							this.experimentCombo.setSelected = this.selectedExperimentOrGroupRecord;
						}
						if (newRecords[i].name) {
							this.experimentCombo.setRawValue(newRecords[i].name);
						}
					}
					this.experimentCombo.getStore().reload();
				}, this);

		this.experimentSelectionEditor.on('doneModification', function() {
					this.searchForm.getEl().unmask();
					this.experimentSelectionEditorWindow.hide();
				}, this);

		/**
		 * 
		 */
		this.experimentSelectionEditorBtn = new Ext.LinkButton({
					handler : this.launchExperimentSelectionEditor,
					scope : this,
					style : 'float:right;text-align:right; ',
					width : '200px',
					tooltip : "Edit your selection",
					hidden : true,
					disabled : true,
					ctCls : 'right-align-btn transparent-btn'
				});

		this.experimentSelectionEditorWindow = new Ext.Window({
					// id : 'experimentSelectionEditorWindow',
					// closeAction: 'hide',
					closable : false,
					layout : 'fit',
					items : this.experimentSelectionEditor,
					title : 'Edit Your Experiment Selection'
				});

		/**
		 * **** EE PREVIEW
		 * *************************************************************************
		 */

		this.experimentPreviewContent = new Ext.Panel({
			width : 315,
			// id:'experimentPreview',
			// html:'<div style="padding: 7px 0 ;font-weight:bold;">Experiment
			// Selection Preview</div>',
			tpl : new Ext.XTemplate(
					'<tpl for="."><div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
					'{id}"',
					' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon})</span></div></tpl>'),
			tplWriteMode : 'append',
			//style : 'padding-top:7px;',
			title : 'Experiment Selection Preview',
			collapsible : true,
			tools:[{
				id:'delete',
				handler:function(event, toolEl, panel, toolConfig){
						this.searchForm.removeExperimentChooser(this.id);
						// this.fireEvent('removeExperiment');
				}.createDelegate(this, [], true),
				qtip:'Remove this experiment or group from your search'
			}],
			cls : 'unstyledTitle',
			hidden : true,
			listeners : {
				collapse : function() {
					this.experimentSelectionEditorBtn.hide();
				}.createDelegate(this, [], true),
				expand : function() {
					this.experimentSelectionEditorBtn.show();
				}.createDelegate(this, [], true)
			}
		});
		this.removeBtn = new Ext.Button({
					icon : "/Gemma/images/icons/cross.png",
					cls : "x-btn-icon",
					tooltip : 'Remove this experiment or group from your search',
					hidden : true,
					handler : function() {
						this.searchForm.removeExperimentChooser(this.id);
						// this.fireEvent('removeExperiment');
					}.createDelegate(this, [], true)
				});
		this.helpBtn = new Ext.Button({
			icon : "/Gemma/images/icons/question_blue.png",
			cls : "x-btn-icon",
			tooltip : 'Select a general group of experiments or try searching for experiments by name or keywords such as: schizophrenia, hippocampus, GPL96 etc.',
			hidden : false,
			handler : function() {
				Ext.Msg
						.alert(
								'Experiment Selection Help',
								'Select a general group of experiments or try searching for experiments by name or keywords such as: schizophrenia, hippocampus, GPL96 etc.');
			}
		});
		Ext.apply(this, {
			frame: true,
			width: 330,
			items: [
			{
				layout: 'hbox',
				items: [this.experimentCombo, this.helpBtn]
			}, this.experimentPreviewContent, this.experimentSelectionEditorBtn]
		});
		Gemma.ExperimentSearchAndPreview.superclass.initComponent.call(this);
	}
});

Ext.reg('experimentSearchAndPreview', Gemma.ExperimentSearchAndPreview);
