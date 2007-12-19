
var bmId; //the id of the biomaterial
       
 
Ext.onReady(function() {
		
	bmId = dwr.util.getValue("bmId");
	var clazz = dwr.util.getValue("bmClass");
	
	// classDelegatingFor is the bioMaterial class.
	var g = {id:bmId, classDelegatingFor:clazz};
	
	var bmRecordType = Ext.data.Record.create([
		{name:"id", type:"int"},
		{name:"factorValue", type:"string"}]);
		
	factorValueDS = new Ext.data.Store(
		{
		proxy:new Ext.data.DWRProxy(BioMaterialController.getFactorValues), 
		reader:new Ext.data.ListRangeReader({id:"id"}, bmRecordType), 
		remoteSort:false
		});
	factorValueDS.setDefaultSort('factorValue');
	
	var cm = new Ext.grid.ColumnModel([
			{header: "Factor Value", width: 650, dataIndex:"factorValue"}]);
	cm.defaultSortable = false;
 
	factorValueGrid = new Ext.grid.GridPanel({
		renderTo:"bmFactorValues",
		ds:factorValueDS,
		cm:cm,
		autoHeight:true,
		viewConfig: { emptyText : "No factor values have been assigned to this biomaterial." }
	});

	factorValueGrid.render();
	
	factorValueDS.load({params:[g]});
		
});