
/*
 * Widget for displaying a list of genes, with cofigurable column sets.
 * 
 * Version : $Id$
 * Author : luke, paul
 */
Ext.namespace('Gemma');

/*
 * The maximum number of genes we allow users to put in at once.
 */
Gemma.MAX_GENES_PER_QUERY = 1000;


/**
 * Table of genes 
 * 
 * See also GeneMembersSaveGrid.js
 * 
 * @class Gemma.GeneMembersGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.GeneMembersGrid = Ext.extend(Ext.grid.GridPanel, {

	collapsible : false,
	autoWidth : true,
	stateful : false,
	frame : true,
	layout : 'fit',
	stripeRows : true,
	changeMade : false,
	// bubbleEvents: ['geneListModified'],
	loggedId : null,
	extraButtons:[],
	/*
	 * columnSet can be "reduced" or "full", if "reduced": only symbol and
	 * description are shown if "full": symbol, description, species and 'in
	 * list' boolean are shown
	 */
	columnSet : "reduced",
	allowSaveToSession: true, // if false, user can only save to db

	viewConfig : {
		forceFit : true,
		emptyText : "Multiple genes can be listed here"
	},
	autoScroll : true,

	autoExpandColumn : 'desc',

	showRemoveColumn : function() {
		// if config is set for "full" column model, show more columns
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("remove"), false);
	},

	getFullColumnModel : function() {
		// if config is set for "full" column model, show more columns
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("taxon"), false);
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("inList"), false);
	},
	setSelectedGeneSetValueObject: function(data){
		this.selectedGeneSetValueObject = data;
	},
	getSelectedGeneSetValueObject: function(){
		return this.selectedGeneSetValueObject;
	},
	// used as 'interface' with experimentMembersGrid
	loadSetValueObject: function(gsvo, callback, args){
		this.loadGeneSetValueObject(gsvo, callback, args);
	},
	loadGeneSetValueObject: function(gsvo, callback, args){
		// update title
		this.setTitle("Edit your gene selection, from group: \""+gsvo.name+"\"");
		// update this.selectedGeneSetValueObject
		this.setSelectedGeneSetValueObject(gsvo);
		// update genes in grid
		this.loadGenes(gsvo.geneIds, callback, args);
	},
	// used as 'interface' with experimentMembersGrid
	loadEntities : function(geneIds, callback, args){
		this.loadGenes(geneIds, callback, args);
	},
	/**
	 * Add to table.
	 * 
	 * @param {}
	 *            geneIds
	 * @param {}
	 *            callback optional
	 * @param {}
	 *            args for callback optional
	 */
	loadGenes: function(geneIds, callback, args){
		if (!geneIds || geneIds.length === 0) {
			return;
		}
		if (this.getEl()) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: Gemma.StatusText.Loading.genes,
				msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
			});
			this.loadMask.show();
		}
		
		GenePickerController.getGenes(geneIds, function(genes){
			if (this.loadMask) {
				this.loadMask.hide();
			}
			var geneData = [];
			var i = 0;
			var taxonId = (genes[0])? genes[0].taxonId:-1;
			for (i = 0; i < genes.length; i++) {
				geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol, genes[i].officialName]);
				if (taxonId != genes[i].taxonId) {
					var taxonId = -1;
				}
			}
			if (taxonId != -1) {
				this.setTaxonId(taxonId);
			}
			this.getStore().loadData(geneData);
			
			/*
		 * FIXME this can result in the same gene listed twice. This
		 * is taken care of at the server side but looks funny.
		 */
			if (callback) {
				callback(args);
			}
			this.fireEvent('genesLoaded');
		}.createDelegate(this));
	},

	addGenes : function(searchResultValObj) { // for adding from combo
				if (!searchResultValObj) {
					return;
				}
						
				var geneIdsToAdd = [];
				geneIdsToAdd = searchResultValObj.memberIds;
				
				if (!geneIdsToAdd || geneIdsToAdd === null || geneIdsToAdd.length === 0) {
					return;
				}
				
				GenePickerController.getGenes(geneIdsToAdd, function(genes) {

					for (var j = 0; j < genes.size(); j++) {
						if (this.getStore().find("id", genes[j].id) < 0) {
							var Constructor = this.store.recordType;
							var record = new Constructor(genes[j]);
							this.getStore().add([record]);
						}
					}

				}.createDelegate(this));
				
				
			},
	/*
	 * set the taxon for this grid and for the toolbar to control what can be added from combo
	 */
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
		if (this.getTopToolbar()) {
			Ext.apply(this.getTopToolbar().geneCombo, {
				taxonId: taxonId
			});
		}
		
	},
	/*buttons:[new Ext.Button({
			text: "Done",
			handler: this.done,
			qtip: 'Return to search using your edited list. (Selection will be kept temporarily.)',
			scope: this,
			disabled: true,
			hidden: this.allowSaveToSession
		}), new Ext.Button({
			text: "Export",
			qtip: 'Get a plain text version of this list',
			handler: this.exportToTxt,
			scope: this,
			disabled: false
		}),{
			text: "Cancel",
			handler: this.cancel,
			scope: this
		}],*/
	initComponent : function() {
		Ext.apply(this, {
			store : new Ext.data.SimpleStore({
						fields : [{
									name : 'id',
									type : 'int'
								}, {
									name : 'taxon'
								}, {
									name : 'officialSymbol',
									type : 'string'
								}, {
									name : 'officialName',
									type : 'string'
								}, {
									name : 'inList',
									type : 'boolean',
									defaultValue : true
								}],
						sortInfo : {
							field : 'officialSymbol',
							direction : 'ASC'
						}
					}),
			colModel : new Ext.grid.ColumnModel({
				defaults : {
					sortable : true
				},
				columns : [{
					header : 'Symbol',
					toolTip : 'Gene symbol',
					dataIndex : 'officialSymbol',
					width : 75,
					renderer : function(value, metadata, record, row, col, ds) {
						return String
								.format(
										"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a><br>"+
										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
										record.data.id, record.data.officialSymbol, record.data.officialName);
					}
				}/*
					 * ,{header: 'Name', id: 'desc', toolTip: 'Gene name',
					 * dataIndex: 'officialName' }
					 */, {
					id : 'taxon',
					toolTip : 'Gene\'s Taxon',
					header : 'Taxon',
					dataIndex : 'taxon',
					hidden : true
				}, {
					id : 'inList',
					toolTip : 'Marks whether this gene is present in one of your lists',
					header : 'In List(s)',
					dataIndex : 'inList',
					hidden : true
				}]
			})
		});

		// add columns dependent on columnSet config
		if (this.columnSet === "full") {
			Ext.apply(this, this.getFullColumnModel());
		}
		

		Gemma.GeneMembersGrid.superclass.initComponent.call(this);

		this.addEvents('addgenes', 'removegenes', 'geneListModified');

		

	},// eo initComponent

	listeners: {
		render: function(){
		
			// load genes stored in genes var, which can either be an array or comma
			// separated list of gene ids
			if (this.selectedGeneSetValueObject) {
				this.loadGeneSetValueObject(this.selectedGeneSetValueObject);
			} else if (this.genes || this.geneIds) {
				var gis = ((this.genes) ? this.genes : this.geneIds);
				var genes = gis instanceof Array ? gis : gis.split(",");
				this.loadGenes(gis);
			}
		},
		keypress: function(e){
			if (!this.getTopToolbar().disabled && e.getCharCode() === Ext.EventObject.DELETE) {
				this.removeGene();
			}
		}
	},

	removeGene : function() {
		var selected = this.getSelectionModel().getSelections();
		var i;
		for (i = 0; i < selected.length; i++) {
			this.getStore().remove(selected[i]);
		}
		this.getSelectionModel().selectLastRow();
	},
	
	removeAllGenes : function() {
		this.getStore().removeAll();
	},


	record : Ext.data.Record.create([{
				name : 'id',
				type : 'int'
			}, {
				name : 'taxon'
			}, {
				name : 'officialSymbol',
				type : 'string'
			}, {
				name : 'officialName',
				type : 'string'
			}, {
				name : 'inList',
				type : 'boolean',
				defaultValue : true
			}]),

	addGene : function(gene) {
		if (!gene) {
			return;
		}

		if (this.getStore().find("id", gene.id) < 0) {
			var Constructor = this.record;
			var record = new Constructor(gene);
			this.getStore().add([record]);
		}
	},


	getGeneIds : function() {
		var ids = [];
		var all = this.getStore().getRange();
		var i = 0;
		for (i = 0; i < all.length; ++i) {
			ids.push(all[i].data.id);
		}
		return ids;
	},


	getGenes : function() {
		var genes = [];
		var all = this.getStore().getRange();
		var i = 0;
		for (i = 0; i < all.length; ++i) {
			genes.push(all[i].data);
		}
		return genes;
	},

	/**
	 * When user clicks cancel, just let parent know
	 */
	cancel : function() {
		this.fireEvent('doneModification');
	},
	
	exportToTxt : function(){
		// make download link
		var downloadLink = String.format("/Gemma/gene/downloadGeneList.html?g={0}", this.getGeneIds());
		window.open(downloadLink);
	}

});
Ext.reg('geneMembersGrid', Gemma.GeneMembersGrid);


/**
 * Table of genes with toolbar for searching.
 * 
 * Adjust columns displayed using "columnSet" config (values can be "reduced"
 * (default) or "full") if "full": symbol, description, species and 'in list'
 * boolean are shown if "reduced" (or any other value): only symbol and
 * description are shown
 * 
 * Note configs 
 * 
 * @class Gemma.GeneMembersSaveGrid
 * @extends Gemma.GeneMembersGrid
 */
Gemma.GeneMembersSaveGrid = Ext.extend(Gemma.GeneMembersGrid, {

	// take note of these config options
	/**
	 * @cfg controls presence of 'done' button
	 */
	allowSaveToSession:true,
	/**
	 * @cfg controls presence of top toolbar
	 */
	allowAdditions:true, 
	/**
	 * @cfg controls presence of 'remove experiment' buttons on every row
	 */
	allowRemovals:true,
	/**
	 * @cfg controls whether the data appears in two columns or formatted into one
	 */
	sortableColumnsView:false, 
	/**
	 * @cfg controls whether the cancel button is visible
	 */
	hideCancel:false,
	/**
	 * @cfg controls whether to show a 'save as' button in addition to a save button
	 */
	showSeparateSaveAs: false,
	/**
	 * @cfg if save button is shown, this controls whether or not to leave it 
	 * disabled until an experiment is added or removed
	 */
	enableSaveOnlyAfterModification: false,
	// end of config options

	initComponent : function() {

		var extraButtons = [];
		if(this.allowRemovals){
			var removeSelectedBtn = new Ext.Button({
				text: 'Remove Selected',
				icon: "/Gemma/images/icons/cross.png",
				hidden: true,
				handler: function(){
					var records = this.getSelectionModel().getSelections();
					this.getStore().remove(records);
					removeSelectedBtn.setVisible(false);
				},
				scope:this
			});
			extraButtons = [removeSelectedBtn];
			
			this.getSelectionModel().on('rowselect', function(selModel){
				removeSelectedBtn.setVisible(selModel.getCount() > 1);
			}, this);

		}
		
		if (this.allowAdditions) {
			Ext.apply(this, {
				tbar: new Gemma.GeneAndGroupAdderToolbar({
					extraButtons: extraButtons,
					geneComboWidth: this.width - 50,
					geneGrid: this,
					taxonId: this.taxonId
				})
			});
		}
		
		var columns = [];
		if (this.sortableColumnsView) {
			
			Ext.apply(this, {
				hideHeaders:false,
				autoExpandColumn : 'name'
			});
			columns = [{
				header: 'Symbol',
				toolTip: 'Gene symbol',
				dataIndex: 'officialSymbol',
				width: 75,
				renderer: function(value, metadata, record, row, col, ds){
					return String.format("<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a>" , 
							record.data.id, record.data.officialSymbol);
				}
			}, {
				header: 'Name',
				id: 'name',
				toolTip: 'Gene name',
				dataIndex: 'officialName'
			}];
		}
		else {
			columns = [{
				header: 'Symbol',
				toolTip: 'Gene symbol',
				dataIndex: 'officialSymbol',
				width: 35,
				renderer: function(value, metadata, record, row, col, ds){
					return String.format("<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a><br>" +
					"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ", record.data.id, record.data.officialSymbol, record.data.officialName);
				}
			}];
		}
		
		if (this.allowRemovals) {
			// Create RowActions Plugin
			this.action = new Ext.ux.grid.RowActions({
				header: 'Remove',
				// ,autoWidth:false
				// ,hideMode:'display'
				tooltip: 'Remove gene',
				keepSelection: true,
				actions: [{
					iconCls: 'icon-cross',
					tooltip: 'Remove gene'
				}],
				callbacks: {
					'icon-cross': function(grid, record, action, row, col){
					}
				}
			});
			
			this.action.on({
				action: function(grid, record, action, row, col){
					if (action === 'icon-cross') {
						this.changeMade = true;
						grid.getStore().remove(record);
					}
				},
				// You can cancel the action by returning false from this
				// event handler.
				beforeaction: function(grid, record, action, row, col){
					if (grid.getStore().getCount() == 1 && action === 'icon-cross') {
						return false;
					}
					return true;
				}
			});
			columns.push(this.action);
			Ext.apply(this, {
				plugins: [this.action]
			});
		}
			
		this.saveAsButton = new Ext.Button({
			text: "Save As",
			handler: this.saveAsBtnHandler,
			tooltip: Gemma.HelpText.WidgetDefaults.GeneMembersSaveGrid.saveAsTT,
			scope: this,
			disabled: !this.showSeparateSaveAs,
			hidden: !this.showSeparateSaveAs
		});
		this.saveButton = new Ext.Button({
			text: "Save...",
			handler: this.saveBtnHandler,
			tooltip:  Gemma.HelpText.WidgetDefaults.GeneMembersSaveGrid.saveTT,
			scope: this,
			disabled: this.enableSaveOnlyAfterModification
		});
		this.doneButton = new Ext.Button({
			text: "Done",
			handler: this.done,
			qtip:  Gemma.HelpText.WidgetDefaults.GeneMembersSaveGrid.doneTT,
			scope: this,
			disabled: true,
			hidden: !this.allowSaveToSession
		});
		this.exportButton = new Ext.Button({
			text: "Export",
			qtip:  Gemma.HelpText.WidgetDefaults.GeneMembersSaveGrid.exportTT,
			handler: this.exportToTxt,
			scope: this,
			disabled: false
		});		
		this.cancelButton = new Ext.Button({
			text: "Cancel",
			handler: this.cancel,
			scope: this,
			hidden: this.hideCancel,
			disabled: this.hideCancel
		});
		
		Ext.apply(this, {
			buttons : [this.saveButton, this.saveAsButton, this.doneButton, this.exportButton, this.cancelButton]
		});
		
		// note: using initComponent of super's super!! (otherwise buttons don't work)
		Gemma.GeneMembersGrid.superclass.initComponent.call(this);

		Ext.apply(this, {
			store : new Ext.data.SimpleStore({
						fields : [{
									name : 'id',
									type : 'int'
								}, {
									name : 'taxon'
								}, {
									name : 'officialSymbol',
									type : 'string'
								}, {
									name : 'officialName',
									type : 'string'
								}, {
									name : 'inList',
									type : 'boolean',
									defaultValue : true
								}],
						sortInfo : {
							field : 'officialSymbol',
							direction : 'ASC'
						}
					}),
			colModel : new Ext.grid.ColumnModel({
				defaults : {
					sortable : true
				},
				columns : columns
			})
		});

		// add columns dependent on columnSet config
		if (this.columnSet === "full") {
			Ext.apply(this, this.getFullColumnModel());
		}
		
		this.ajaxLogin = null;
		this.ajaxRegister = null;
		


		this.addEvents('addgenes', 'removegenes', 'geneListModified');

		this.on('doneModification', function() {
			this.changesMade = false;
				// this.saveButton.disable();
				 this.doneButton.disable();
			});

		this.getStore().on("remove", function() {
					this.fireEvent("removegenes");
					this.changesMade = true;
					this.saveButton.enable();
					this.doneButton.enable();
				}, this);

		this.getStore().on("add", function() {
					this.fireEvent("addgenes");
					this.changesMade = true;
					this.saveButton.enable();
					this.doneButton.enable();
				}, this);

		
		this.on('genesLoaded',function(){
			if (this.selectedGeneSetValueObject) {
				GeneSetController.canCurrentUserEditGroup(this.selectedGeneSetValueObject, function(response){
					var dataMsg = Ext.util.JSON.decode(response);
					if (!dataMsg.userCanEditGroup || !dataMsg.groupIsDBBacked) {
						// don't show two save as buttons
						if(!this.showSeparateSaveAs){
							this.saveButton.setText("Save As");
						}else{
							this.saveButton.hide().disable();
						}
					}else{
						this.saveButton.setText("Save...");
					}
				}.createDelegate(this));
			}
		});

	},// eo initComponent

	record : Ext.data.Record.create([{
				name : 'id',
				type : 'int'
			}, {
				name : 'taxon'
			}, {
				name : 'officialSymbol',
				type : 'string'
			}, {
				name : 'officialName',
				type : 'string'
			}, {
				name : 'inList',
				type : 'boolean',
				defaultValue : true
			}]),

	/**
	 * When user clicks cancel, just let parent know
	 */
	cancel : function() {
		this.fireEvent('doneModification');
	},

	/**
	 * Sets ups name and description for new group
	 */
	createDetails : function() {

		// if name for new group wasn't passed from parent component, make one
		// up
		if (!this.selectedGeneSetValueObject && (!this.groupName || this.groupName === null || this.groupName === '')) {
			this.newGroupName = "Gene group created: " + (new Date()).toString();
		} else {
			
			var groupName = (this.selectedGeneSetValueObject && this.selectedGeneSetValueObject.name)? this.selectedGeneSetValueObject.name: this.groupName;
			
			// adding time to end of session-bound group titles in case it's not
			// unique
			var currentTime = new Date();
			var hours = currentTime.getHours();
			var minutes = currentTime.getMinutes();
			if (minutes < 10) {
				minutes = "0" + minutes;
			}
			this.newGroupName = '(' + hours + ':' + minutes + ')';
			this.newGroupName += ' Edited \'' + groupName + '\' group';
		}

		// if description for new group wasn't passed from parent component,
		// make one up
		if (!this.newGroupDescription || this.newGroupDescription === null) {
			this.newGroupDescription = "Temporary experiment group created " + (new Date()).toString();
		}
	},

	/**
	 * When user clicks done, just save to session
	 */
	done : function() {
						
		// check if user is trying to save an empty set
		if(this.getStore().getRange() && this.getStore().getRange().length === 0){
			Ext.Msg.alert('Cannot use empty set', 'You are trying to use an empty set. '+
				'Please add some genes and try again.');
			return;
		}
		this.createDetails();
		this.createInSession();
	},
	
	exportToTxt : function(){
		// make download link
		var downloadLink = String.format("/Gemma/gene/downloadGeneList.html?g={0}", this.getGeneIds());
		window.open(downloadLink);
	},
	
	launchRegisterWidget : function(){
		if (this.ajaxRegister === null){
			
			//Check to see if another register widget is open (rare case but possible)
				var otherOpenRegister = Ext.getCmp('_ajaxRegister');				
				
				//if another register widget is open, fire its event to close it and destroy it before launching this one
				if (otherOpenRegister!==null){
					otherOpenRegister.fireEvent("register_cancelled");
				}	
			
			
			this.ajaxRegister = new Gemma.AjaxRegister({					
					name : 'ajaxRegister',									
					closable : false,
					closeAction : 'hide',													
					title : 'Please Register'
				
					
				});			
			
			this.ajaxRegister.on("register_cancelled",function(){
				
				this.ajaxRegister.destroy();
				this.ajaxRegister = null;
				this.getEl().unmask();				
				
			},this);
			
			this.ajaxRegister.on("register_success",function(){
				
				this.ajaxRegister.destroy();
				this.ajaxRegister = null;
				this.getEl().unmask();				
				
			},this);
			
						
			}
		this.getEl().mask();	
		this.ajaxRegister.show();
	},

	/**
	 * When user clicks 'save', figure out what kind of save to do
	 */
	saveBtnHandler : function() {
				
		Ext.Ajax.request({
         	url : '/Gemma/ajaxLoginCheck.html',
            method: 'GET',                  
            success: function ( response, options ) {		
                    var dataMsg = Ext.util.JSON.decode(response.responseText); 
                    
                    if (dataMsg.success){
						this.loggedInSaveHandler();
					}
                    else{
						this.promptLoginForSave('save');  
                    }
                      
            },
            failure: function ( response, options ) {  
				this.promptLoginForSave('save');  
            },
            scope: this,
            disableCaching: true
       });
	   
	},
			
	/**
	 * When user clicks 'save as', check if they are logged in or not, then in the callback, call saveAsHandler
	 */
	saveAsBtnHandler : function() {
				
		Ext.Ajax.request({
         	url : '/Gemma/ajaxLoginCheck.html',
            method: 'GET',                  
            success: function ( response, options ) {			
					
                    var dataMsg = Ext.util.JSON.decode(response.responseText); 
                    
                    if (dataMsg.success){
						// get name and description set up
						this.createDetails();
						this.saveAsHandler();
					}
                    else{
						this.promptLoginForSave('saveAs');                      	
                    }
            },
            failure: function ( response, options ) {   
				this.promptLoginForSave('saveAs');  
            },
            scope: this,
            disableCaching: true
       });
	},
	promptLoginForSave : function (saveAction) {
		/*//Check to see if another login widget is open (rare case but possible)
		var otherOpenLogin = Ext.getCmp('_ajaxLogin');				
				
		//if another login widget is open, fire its event to close it and destroy it before launching this one
		if (otherOpenLogin!==null){
			otherOpenLogin.fireEvent("login_cancelled");
		}*/		
		
		Gemma.AjaxLogin.showLoginWindowFn();
		
		Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){	
				Ext.getBody().unmask();
				if(saveAction === 'save'){
					this.loggedInSaveHandler();
				}else{
					this.saveAsHandler();
				}
				
			},this);

	},
	loggedInSaveHandler : function () {
				
		// check if user is trying to save an empty set
		if(this.getStore().getRange() && this.getStore().getRange().length === 0){
			Ext.Msg.alert('Cannot save empty set', 'You are trying to save an empty set. '+
				'Please add some genes and try again.');
			return;
		}
		
		// get name and description set up
		this.createDetails();
		
		// check if user is editing a non-existant or session-bound group
		
		// check if group is db-backed and whether current user has editing priveleges
		if(this.selectedGeneSetValueObject){
			
			// if group is db-bound and user has editing privileges, they can either save or save as
			// in all other cases, user can only save as
			GeneSetController.canCurrentUserEditGroup(this.selectedGeneSetValueObject, function(response){
				var dataMsg = Ext.util.JSON.decode(response);
				if(dataMsg.userCanEditGroup && dataMsg.groupIsDBBacked){
					// ask user if they want to save changes
					Ext.Msg.show({
								title : 'Save Changes?',
								msg : 'You have edited an <b>existing group</b>, '+
										'how would you like to save your changes?<br>',
								buttons : {
									ok : 'Save over',
									yes : 'Save as...',
									no : 'Cancel'
								},
								fn : function(btnId){
									if(btnId === 'ok'){
										this.saveHandler();
									}else if(btnId === 'yes'){
										this.saveAsHandler();
									}else if(btnId === 'no'){
										// just close the prompt
									}
								},
								scope:this,
								icon : Ext.MessageBox.QUESTION
							});
				}else{
					this.saveAsHandler();
				}
			}.createDelegate(this));
			
		}else{
			// only save option is to save as
			this.saveAsHandler();
		}
	},
	
	saveAsHandler: function(){
		// input window for creation of new groups
		var detailsWin = new Gemma.CreateSetDetailsWindow();
		detailsWin.lockInTaxonId(this.taxonId, true);
		detailsWin.on("commit", function(args){
			this.newGroupName = args.name;
			this.newGroupDescription = args.description;
			this.newGroupPublik = args.publik;
			this.newGroupTaxon = args.taxon;
			this.createInDatabase();
		}, this);
		detailsWin.on("hide", function(args){
			this.close();
		});
		
		detailsWin.name = this.groupName;
		detailsWin.description = 'Edited search results for: "' + this.groupName + '". Created: ' +
		(new Date()).toString();
		
		//this.detailsWin.name = '';
		//this.detailsWin.description = '';
		detailsWin.show();
	},
	saveHandler: function(){
		this.updateDatabase();
	},
	createInSession : function() {
		var editedGroup;
		editedGroup = new SessionBoundGeneSetValueObject();
		editedGroup.id = null;	
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.geneIds = this.getGeneIds();
		editedGroup.taxonId = this.taxonId;
		editedGroup.size = this.getGeneIds().length;
		editedGroup.modified = true;
		editedGroup.publik = false;
		

		GeneSetController.addSessionGroups(
				[editedGroup], // returns datasets added
				function(geneSets) {
					// should be at least one datasetSet
					if (geneSets === null || geneSets.length === 0) {
						// TODO error message
						return;
					} else {
						this.fireEvent('geneListModified', geneSets, geneSets[0].geneIds);
						this.fireEvent('doneModification');
					}
				}.createDelegate(this));

	},
	createInDatabase: function(){
		var editedGroup;
		if (this.selectedGeneSetValueObject === null || typeof this.selectedGeneSetValueObject === 'undefined' || 
				!(this.selectedGeneSetValueObject instanceof DatabaseBackedGeneSetValueObject)) {
			//group wasn't made before launching 
			editedGroup = new DatabaseBackedGeneSetValueObject();
		}
		else {
			editedGroup = Object.clone(this.selectedGeneSetValueObject);
		}
		
		editedGroup.id = null;	
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.publik = this.newGroupPublik;
		editedGroup.geneIds = this.getGeneIds();
		editedGroup.taxonId = (this.newGroupTaxon)? this.newGroupTaxon.id : this.taxonId;
		editedGroup.size = this.getGeneIds().length;
			
			GeneSetController.create([editedGroup], // returns datasets added
 				function(geneSets){
					// should be at least one datasetSet
					if (geneSets === null || geneSets.length === 0) {
						// TODO error message
						return;
					}
					else {
						this.fireEvent('geneListModified', geneSets, geneSets[0].geneIds);
						this.fireEvent('geneSetCreated', geneSets[0]);
						this.fireEvent('doneModification');
					}
				}.createDelegate(this));
		
		this.fireEvent('doneModification');
		
	},
	updateDatabase : function() {
		var groupId = this.getSelectedGeneSetValueObject().id;
		var geneIds = this.getGeneIds();

		GeneSetController.updateMembers(groupId, geneIds, function(msg) {
					this.selectedGeneSetValueObject.geneIds = geneIds;

					this.fireEvent('geneListModified', [this.selectedGeneSetValueObject], this.selectedGeneSetValueObject.geneIds);
					this.fireEvent('geneListSavedOver');
					this.fireEvent('doneModification');
				}.createDelegate(this));
	}
});
Ext.reg('geneMembersSaveGrid', Gemma.GeneMembersSaveGrid);


/**
 * toolbar for selecting genes or gene groups and adding them to a grid
 * if this.taxonId is set, then searches will be limited by taxon  
 */
Gemma.GeneAndGroupAdderToolbar = Ext.extend(Ext.Toolbar,{
		extraButtons: [],
			initComponent : function() {

				Gemma.GeneAndGroupAdderToolbar.superclass.initComponent.call(this);
				
				this.geneCombo = new Gemma.GeneAndGeneGroupCombo({
					typeAhead : false,
					width : 300,
					listeners : {
								'select' : {
									fn : function(combo, rec, index) {
										this.addBtn.enable();
										if(rec.data.size === 1){
											this.addBtn.setText('Add 1 gene');
										}else{
											this.addBtn.setText('Add '+rec.data.size+' genes');
										}
										
									}.createDelegate(this)
								}
							}
				});

				this.addBtn = new Ext.Toolbar.Button({
							icon : "/Gemma/images/icons/add.png",
							cls : "x-btn-text-icon",
							tooltip : "Add selected genes(s) to the list",
							text: 'Add',
							disabled : true,
							handler : function() {
								this.geneGrid.addGenes(this.geneCombo.getGeneGroup());
								this.geneCombo.reset();
								this.addBtn.setText('Add');
								this.addBtn.disable();
							}.createDelegate(this)
						});

			},
			afterRender : function(c, l) {
				Gemma.GeneAndGroupAdderToolbar.superclass.afterRender.call(this, c, l);
				this.add(this.geneCombo, this.addBtn);
				this.addButton(this.extraButtons);
			}
});


/**
 * classic 'grid' version of a gene listing
 * @class Gemma.GeneGroupMemberPanelClassic
 */
Gemma.GeneGroupMemberPanelClassic = Ext.extend(Gemma.GeneGrid, {

	initComponent : function() {
		Gemma.GeneGroupMemberPanelClassic.superclass.initComponent.call(this);
	},
	showGeneGroup: function(groupRecord){
		this.getEl().mask("Loading genes ...");
		GeneSetController.getGenesInGroup(groupRecord.get('id'), {
			callback: this.afterLoadGenes.createDelegate(this, [groupRecord], true),
			errorHandler: function(e){
				this.getEl().unmask();
				Ext.Msg.alert('There was an error', e);
			}
		});
	},
	
	reset: function(){
		this.getTopToolbar().taxonCombo.reset();
		this.getTopToolbar().geneCombo.reset();
		this.getTopToolbar().taxonCombo.setDisabled(false);
		this.fireEvent("taxonchanged", null);
		this.loadGenes([]);
		this.currentGroupSize = 0;
	},
	/**
	 * functions the same as reset(), except the taxon combo box doesn't lose its value
	 * and an event announcing that the taxon has been changed isn't fired
	 */
	resetKeepTaxon: function(){
		this.getTopToolbar().geneCombo.reset();
		this.getTopToolbar().taxonCombo.setDisabled(false);
		this.loadGenes([]);
		this.currentGroupSize = 0;
	},
	
	lockInTaxon: function(taxon){
		this.getTopToolbar().taxonCombo.setTaxon(taxon);
		this.getTopToolbar().geneCombo.setTaxon(taxon);
		this.getTopToolbar().taxonCombo.setDisabled(true);
	},
	
	afterLoadGenes: function(geneValueObjs, groupRecord){
	
		if (groupRecord.get('currentUserHasWritePermission')) {
			Ext.util.Observable.releaseCapture(this.getStore());
			this.getTopToolbar().setDisabled(false);
		}
		else {
			this.getTopToolbar().setDisabled(true);
			Ext.util.Observable.capture(this.getStore(), function(eventName, args){
				/*
				 * Trap events that would modify an unmodifiable set. Basically 'remove' is the problem.
				 */
				if (eventName === 'add' || eventName === 'remove') {
					Ext.Msg.alert("Access denied", "You don't have permission to edit this set.");
					return false;
				}
				return true;
			}, this);
		}
		
		// If no genes in gene set, enable taxon
		// selection
		this.currentGroupId = groupRecord.get('id');
		
		if (!geneValueObjs || geneValueObjs.size() === 0) {
			this.reset();
		}
		else {
		
			this.currentGroupSize = geneValueObjs.size();
			
			var geneIds = [];
			var taxonId = geneValueObjs[0].taxonId;
			for (var i = 0; i < geneValueObjs.length; i++) {
				if (taxonId !== geneValueObjs[0].taxonId) {
					Ext.Msg.alert('Sorry', 'Gene groups do not support mixed taxa. Please remove this gene group');
					break;
				}
				geneIds.push(geneValueObjs[i].id);
			}
			
			var groupTaxon = {
				id: taxonId,
				commonName: geneValueObjs[0].taxonName
			};
			this.lockInTaxon(groupTaxon);
			this.loadGenes(geneIds);
		}
		
		this.getEl().unmask();
	}
	
});