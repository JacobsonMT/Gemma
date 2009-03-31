/**
 * User management tool.
 * 
 * @author keshav
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Gemma.USER_PAGE_SIZE = 10;

Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	if (Ext.isIE && Ext.isIE6) {
		Ext.DomHelper.append('errorMessage', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	// A simple test store using canned "data" and an array "reader".
	// var store = new Ext.data.Store({
	// data : [[1, "mjudge", "Judge", "foo@foo.com", true],
	// [2, "mjudge", "Judge", "foo@foo.com", true],
	// [3, "mjudge", "Judge", "foo@foo.com", true],
	// [4, "mjudge", "Judge", "foo@foo.com", false]],
	//
	// reader : new Ext.data.ArrayReader({
	// id : 'id'
	// }, ['id', 'userName', 'lastName', 'email', 'enabled'])
	//
	// });

	var record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "userName",
				type : "string"
			}, {
				name : "email",
				type : "string"
			}, {
				name : "role",
				type : "string"
			}, {
				name : "enabled",
				type : "boolean"
			}]);

	var userStore = new Ext.data.Store({
		proxy : new Ext.data.DWRProxy(UserListController.getUsers),
		reader : new Ext.data.ListRangeReader({
					id : 'id'
				}, record),
		remoteSort : false
		// pageSize : Gemma.USER_PAGE_SIZE TODO make this a PagingDataStore before use this
	})
	userStore.load();

	/* create editors for the grid */
	var userNameEdit = new Ext.form.TextField({
		allowBlank : false
	});

	var emailEdit = new Ext.form.TextField({
		allowBlank : false,
		vtype : 'email'
	});

	var checkColumn = new Ext.grid.CheckColumn({
		header : "Enabled?",
		dataIndex : 'enabled',
		width : 55
	});

	var roleEdit = new Ext.form.ComboBox({
		typeAhead : true,
		lazyRender : true,
		triggerAction : 'all',
		mode : 'local',
		selectOnFocus : true,
		displayField : 'role',

		// the role (combo box) editor needs a store
		store : new Ext.data.Store({
			data : [[1, "user"], [2, "admin"]],

			reader : new Ext.data.ArrayReader({
						id : 'id'
					}, ['id', 'role'])

		}),
		valueField : 'role'
	});

	var userGrid = new Ext.grid.EditorGridPanel({

		renderTo : "userList",
		title : "User Management",
		frame : true,
		height : 300,
		width : 900,
		stripeRows : true,
		clicksToEdit : 2,
		plugins : checkColumn,
		loadMask : true,
		autoScroll : true,
		store : userStore,

		tbar : [

				{
					text : 'Add',
					tooltip : 'Add a new user',
					disabled : true,
					icon : 'images/icons/add.png',
					cls : 'x-btn-text-icon',
					handler : function() {
						userGrid.getStore().insert(0, new record({
									userName : 'New User',
									email : '',
									role : 'Select Role',
									enabled : true
								}));
						userGrid.startEditing(0, 0);
					}
				},

				{
					text : 'Remove',
					disabled : true,
					tooltip : 'Remove selected user',
					icon : 'images/icons/delete.png',
					cls : 'x-btn-text-icon',
					handler : function() {
						var sm = userGrid.getSelectionModel();
						var sel = sm.getSelected();
						if (sm.hasSelection()) {
							Ext.Msg.show({
								title : 'Remove User',
								buttons : Ext.MessageBox.YESNO,
								msg : 'Remove ' + sel.data.userName + '?',
								fn : function(btn) {
									if (btn == 'yes') {
										userGrid.getStore().remove(sel);
									}
								}
							});
						};
					}
				},

				{
					text : 'Save',
					tooltip : 'Save the selected user',
					icon : 'images/icons/database_save.png',
					cls : 'x-btn-text-icon',
					handler : function() {
						var sm = userGrid.getSelectionModel();
						var sel = sm.getSelected();
						if (sm.hasSelection()) {
							Ext.Msg.show({
								title : 'Save User',
								buttons : Ext.MessageBox.YESNO,
								msg : 'Save ' + sel.data.userName + '?',
								fn : function(btn) {
									if (btn == 'yes') {
										UserListController.saveUser({
													userName : sel.data.userName,
													email : sel.data.email,
													enabled : sel.data.enabled,
													role : sel.data.role
												}, {
													callback : function() {
														userGrid.getStore().reload();
													}
												});
									}
								}
							});
						};
					}
				}

		],

		// bbar : new Gemma.PagingToolbar({TODO add in when using the PagingDataStore
		// pageSize : Gemma.USER_PAGE_SIZE,
		// store : userStore
		// }),

		cm : new Ext.grid.ColumnModel([{
					header : "Username",
					dataIndex : 'userName',
					editor : userNameEdit,
					sortable : true
				}, {
					header : "Email",
					dataIndex : 'email',
					editor : emailEdit
				}, {
					header : "Role",
					dataIndex : 'role',
					editor : roleEdit
				}, checkColumn]),

		viewConfig : {
			forceFit : true
		},

		sm : new Ext.grid.RowSelectionModel({
			singleSelect : true
		})

	});
});