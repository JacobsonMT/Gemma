Ext.namespace('Gemma');

// Column of heatmap cells for one gene group.
// Abstracts out heatmap sub column. It encapsulates data and behaviour 
// needed for drawing a subcolumn. 
//
Gemma.MetaHeatmapColumn = Ext.extend(Ext.BoxComponent, {
	initComponent : function() {
		Ext.apply(this, {
					//
					autoEl:'canvas',
					margins : {
						top    : 0,
						right  : 0,
						bottom : Gemma.MetaVisualizationConfig.groupSeparatorHeight,
						left   : 0
					},

					ourType:'MetaHeatmapColumn',

					cellHeight : Gemma.MetaVisualizationConfig.cellHeight, 
					cellWidth  : Gemma.MetaVisualizationConfig.cellWidth,

					collapsedWidth : Gemma.MetaVisualizationConfig.cellWidth,
					expandedWidth  : Gemma.MetaVisualizationConfig.cellWidth * this.factorValueIds.length,
					
					//
					applicationRoot	: this.applicationRoot,

					geneGroupIndex   		 : this.rowGroup, // gene group index
					columnIndex    			 : this.columnIndex, // index within analysis panel
					analysisColumnGroupIndex : this.columnGroupIndex, // index of analysis panel
					datasetColumnGroupIndex  : this.datasetColumnGroupIndex, // index of dataset column group panel
					datasetGroupIndex 		 : this.datasetGroupIndex, // dataset group index
					
					_visualizationValues : this.visualizationSubColumnData,
					qValues				 : this.qValuesSubColumnData,

					factorValueNames : this.factorValueNames,
					factorValueIds 	 : this.factorValueIds,

					_isExpanded : false
		});
		Gemma.MetaHeatmapColumn.superclass.initComponent.apply (this, arguments);
	},

	drawHeatmapSubColumn_ : function(highlightRow, highlightColumn) {
		var ctx = Gemma.MetaVisualizationUtils.getCanvasContext (this.el.dom);
		ctx.canvas.height = this.cellHeight * this._visualizationValues.length;
		
		if (this._isExpanded) {
			ctx.canvas.width = this.expandedWidth;
			this.drawContrasts_ ( highlightRow, highlightColumn );
		} else {
			ctx.canvas.width = this.collapsedWidth;
			this.drawQvalues_ ( highlightRow );
		}		
	},

	collapseSubColumn_ : function () {
		this._isExpanded = false;
		this.setWidth( this.collapsedWidth ); //TODO: Can I override this method?
		
		var ctx = Gemma.MetaVisualizationUtils.getCanvasContext(this.el.dom);
		ctx.canvas.width = this.collapsedWidth;
		
		this.drawQvalues_ ();
	},
	
	expandSubColumn_ : function () {
		this._isExpanded = true;
		this.setWidth( this.expandedWidth );
		
		var ctx = Gemma.MetaVisualizationUtils.getCanvasContext( this.el.dom );
		ctx.canvas.width = this.expandedWidth;		
		
		this.drawContrasts_ ();
	},
	
	drawQvalues_ : function (highlightRow) {	
		var	ctx = Gemma.MetaVisualizationUtils.getCanvasContext(this.el.dom);
		ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

		for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; i++) {
			var color = Gemma.MetaVisualizationConfig.basicColourRange.getCellColorString(this._visualizationValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][i]]);
			this.drawHeatmapCell_(ctx, color, i, 0);
			var geneId = this.applicationRoot.visualizationData.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][i]];
			if (this.applicationRoot._selectedGenes.indexOf(geneId) != -1) {
				this.drawHeatmapSelectedRowCell_(ctx, i, 0);
			}
			if (highlightRow === i) {
				this.drawHeatmapCellBox_(ctx, highlightRow, 0);
			}
		}
	},	
	
	drawContrasts_ : function (highlightRow, highlightColumn) {
		var contrasts = this.ownerCt.contrastsData.contrasts;

		var	ctx = Gemma.MetaVisualizationUtils.getCanvasContext( this.el.dom );		
		ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
		
		// Draw cells.
		for (var geneIndex = 0; geneIndex < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; geneIndex++) {
			
			var geneId = this.applicationRoot._imageArea._heatmapArea.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][geneIndex]];
			var geneContrastsInfo = contrasts[geneId];

			for (var factorValueIndex = 0; factorValueIndex < this.factorValueIds.length; factorValueIndex++) {
				var factorValueId = this.factorValueIds[factorValueIndex];
				var vizValue = null;
				if (typeof geneContrastsInfo !== 'undefined' && geneContrastsInfo !== null) {
					if ( typeof geneContrastsInfo[factorValueId] === 'undefined' || geneContrastsInfo[factorValueId] === null) {
						vizValue = 0;				
					} else {
						if (geneContrastsInfo[factorValueId].foldChangeValue > 0) {
							vizValue = 3;										
						} else {
							vizValue = -3;																	
						}
					}
				}					
				var color = Gemma.MetaVisualizationConfig.contrastsColourRange.getCellColorString(vizValue);
				this.drawHeatmapCell_(ctx, color, geneIndex, factorValueIndex);
				if (highlightRow === geneIndex && highlightColumn === factorValueIndex) {
					this.drawHeatmapCellBox_(ctx, highlightRow, highlightColumn);
				}
			}
		}
	},
	
	drawHeatmapCellBox_ : function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.cellHighlightColor;
		ctx.strokeRect (this.cellWidth * columnIndex, this.cellHeight * rowIndex, this.cellWidth, this.cellHeight);
		ctx.restore();
	},
	
	drawHeatmapSelectedRowCell_ : function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.rowCellSelectColor;
		ctx.beginPath();
		ctx.moveTo (this.cellWidth * columnIndex, this.cellHeight * rowIndex);
		ctx.lineTo (this.cellWidth * columnIndex + this.cellWidth, this.cellHeight * rowIndex);
		ctx.moveTo (this.cellWidth * columnIndex, this.cellHeight * (rowIndex+1) );
		ctx.lineTo (this.cellWidth * columnIndex + this.cellWidth, this.cellHeight * (rowIndex+1));
		ctx.stroke();
		ctx.restore();
	},

	drawHeatmapCell_ : function(ctx, color, rowIndex, columnIndex) {
		ctx.fillStyle = color;
		ctx.fillRect (columnIndex * this.cellWidth, rowIndex * this.cellHeight, this.cellWidth, this.cellHeight);
	},

	__calculateIndexFromXY : function(x, y) {
		var row = Math.floor (y / this.cellHeight);
		var column = Math.floor (x / this.cellWidth);
		return {
			'row' : row,
			'column' : column
		};
	},

	/**
	 * @Override
	 */
	onRender : function() {

		// After the component has been rendered, disable the default browser
		// context menu
		// problem: this disables right click everywhere on the page (not just
		// over component), pretty annoying
		// Ext.getBody().on("contextmenu", Ext.emptyFn, null, {preventDefault:
		// true});

		Gemma.MetaHeatmapColumn.superclass.onRender.apply (this, arguments);
		this.drawHeatmapSubColumn_();

		this.el.on('click', function(e, t) {
			var index = this.__calculateIndexFromXY (e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());

			// if user held down ctrl while clicking, select column or gene
			// instead of popping up window
			// if (e.ctrlKey == true) {
			// Ext.Msg.alert('CTRL Used', 'Ctrl+Click was used');
			// }
			
			var geneId = this.applicationRoot._imageArea._heatmapArea.geneIds[this.rowGroup][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]];
			var eeId = this.ownerCt._dataColumn.datasetId;
			var downloadLink = String.format ("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", eeId, geneId);
			var vizWindow = new Gemma.VisualizationWithThumbsWindow ({
						title : 'Gene Expression',
						thumbnails : false,
						downloadLink: downloadLink,
						prevX : this.applicationRoot.prevVizWindowX,
						prevY : this.applicationRoot.prevVizWindowY						
					});
			vizWindow.show({
				params : [
						[eeId],
						[geneId]]
			});
			
			this.applicationRoot.prevVizWindowX = vizWindow.x;
			this.applicationRoot.prevVizWindowY = vizWindow.y;
    		
		}, this);

		// detect right click.
		// this.el.on('contextmenu', function() {
		// Ext.Msg.alert("Right-o!","You right-clicked!");
		// });

		this.el.on ('mouseover', function(e, t) {
					document.body.style.cursor = 'pointer';
				});
		
		this.el.on ('mouseout', function(e, t) {
					document.body.style.cursor = 'default';
					this.applicationRoot._imageArea._geneLabels.unhighlightGene (this.rowGroup);
					this.applicationRoot._imageArea._hoverDetailsPanel.hide();
					this.drawHeatmapSubColumn_ ();
				}, this);
		
		this.el.on ('mousemove', function(e, t) {
			var index = this.__calculateIndexFromXY (e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			this.drawHeatmapSubColumn_ (index.row, index.column);
			this.applicationRoot._imageArea._geneLabels.highlightGene (this.rowGroup, index.row); 

			// Format p value.
			formatPVal = function(p) {
				if (p === null) {
					return '-';
				}
				if (p < 0.001) {
					return sprintf("%.3e", p);
				} else {
					return sprintf("%.3f", p);
				}
			};
			this.applicationRoot._imageArea._hoverDetailsPanel.show();
			if (this._isExpanded) {
				var contrasts = this.ownerCt.contrastsData.contrasts;
				var geneId = this.applicationRoot._imageArea._heatmapArea.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]];				
				var geneContrastsInfo = contrasts[ geneId ];
				
				var factorValueId = this.factorValueIds[index.column];
				var foldChange = null;
				var contrastPvalue = null;
				if (typeof geneContrastsInfo !== 'undefined' && geneContrastsInfo !== null && typeof geneContrastsInfo[factorValueId] !== 'undefined') {
					foldChange = geneContrastsInfo[factorValueId].foldChangeValue;
					contrastPvalue = geneContrastsInfo[factorValueId].contrastPvalue;					
				}					
									
				this.applicationRoot._imageArea._hoverDetailsPanel.update({
					type : 'contrastCell',
					qvalue : formatPVal(this.qValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]),
					// baselineFactorValue: this.ownerCt.baselineFactorValue,
					factorName : this.ownerCt._dataColumn.factorName,				
					foldChange : formatPVal(foldChange),
					factorCategory : this.ownerCt._dataColumn.factorCategory,
					factorDescription : this.ownerCt._dataColumn.factorDescription,
					factorId : this.ownerCt._dataColumn.factorId,
					datasetId : this.ownerCt._dataColumn.datasetId,
					datasetName : this.ownerCt._dataColumn.datasetName,
					datasetShortName : this.ownerCt._dataColumn.datasetShortName,
					contrastPvalue : formatPVal(contrastPvalue), 
					// numberOfProbes: this.ownerCt.numberOfProbes,
					// numberOfProbesDiffExpressed:
					// this.ownerCt.numberOfProbesDiffExpressed,
					// numberOfProbesDownRegulated:
					// this.ownerCt.numberOfProbesDownRegulated,
					// numberOfProbesUpRegulated:
					// this.ownerCt.numberOfProbesUpRegulated,
					// numberOfProbesTotal: this.ownerCt.numberOfProbesTotal,
					geneSymbol : this.applicationRoot._imageArea._geneLabels.labels[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
					geneId : this.applicationRoot._imageArea._heatmapArea.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
					geneFullName : this.applicationRoot.visualizationData.geneFullNames[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]
				});
			} else {
				this.applicationRoot._imageArea._hoverDetailsPanel.update({
					type : 'cell',
					qvalue : formatPVal(this.qValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]),
					// baselineFactorValue: this.ownerCt.baselineFactorValue,
					factorName : this.ownerCt._dataColumn.factorName, 
					factorCategory : this.ownerCt._dataColumn.factorCategory,
					factorDescription : this.ownerCt._dataColumn.factorDescription,
					factorId : this.ownerCt._dataColumn.factorId,
					datasetId : this.ownerCt._dataColumn.datasetId,
					datasetName : this.ownerCt._dataColumn.datasetName,
					datasetShortName : this.ownerCt._dataColumn.datasetShortName,
					geneSymbol : this.applicationRoot._imageArea._geneLabels.labels[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
					geneId : this.applicationRoot._imageArea._heatmapArea.geneIds[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]],
					geneFullName : this.applicationRoot.visualizationData.geneFullNames[this.geneGroupIndex][this.applicationRoot.geneOrdering[this.geneGroupIndex][index.row]]
				});
			}
			
			var heightOfHoverWin = (typeof this.applicationRoot._imageArea._hoverDetailsPanel.height !== 'undefined')?
					this.applicationRoot._imageArea._hoverDetailsPanel.height:200; 
			var y = ((e.getPageY()+ 20 + heightOfHoverWin) > this.applicationRoot._imageArea.height)?
						e.getPageY() - heightOfHoverWin - 20 : e.getPageY()+ 20;
			this.applicationRoot._imageArea._hoverDetailsPanel.setPagePosition(e.getPageX()+20 , y );
			
		}, this);

	}

});

Ext.reg('metaVizColumn', Gemma.MetaHeatmapColumn);


/**
 * 
 * Abstracts out 
 * 
 * State:
 * 
 * 
 * Data:
 * 
 * 
 * Behaviour:
 * 
 * 
 * @class Gemma.MetaHeatmapExpandableColumn
 * @extends Ext.Panel
 */
Gemma.MetaHeatmapExpandableColumn = Ext.extend(Ext.Panel, {
	initComponent : function() {
		Ext.apply(this, {
			//
			ourType:'MetaHeatmapExpandableColumn',
		
			//
			bodyBorder : false,
			border : false,
			width 	   : Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth,

			collapsedWidth : Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth,
			expandedWidth  : Gemma.MetaVisualizationConfig.cellWidth * this.dataColumn.contrastsFactorValueIds.length + Gemma.MetaVisualizationConfig.columnSeparatorWidth,			
			
			applicationRoot : this.applicationRoot,

			_dataColumn : this.dataColumn,
			
			_numberOfRowGroups : this.dataColumn.visualizationValues.length,
			_columnIndex 	   : this.columnIndex,
			_columnGroupIndex  : this.columnGroupIndex,
			_datasetGroupIndex : this.datasetGroupIndex,
			_columnHidden 	   : false,
			
			isFiltered : false,

			miniPieValue : (this.dataColumn.numberOfProbesTotal === 0)? -1: 360.0 * this.dataColumn.numberOfProbesDiffExpressed / this.dataColumn.numberOfProbesTotal,
			sumOfQvalues : 0.0,

			resultSetId 	  	  : this.dataColumn.resultSetId,
			factorValueIds    	  : this.dataColumn.contrastsFactoreValueIds,
			factorId 	 	  	  : this.dataColumn.factorId,
			factorName 		  	  : this.dataColumn.factorName,
			factorCategory 	  	  : this.dataColumn.factorCategory,
			factorDescription 	  : this.dataColumn.factorDescription,
			factorValueNames 	  : this.dataColumn.contrastsFactorValues,
			factorValueIds 		  : this.dataColumn.contrastsFactorValueIds,
			baselineFactorValue   : this.dataColumn.baselineFactorValue,
			baselineFactorValueId : this.dataColumn.baselineFactorValueId,

			contrastsData : null,  //map geneId -> map of contrasts by factor value id

			updateAllParentContainersWidthBy : function(delta) {
				this.ownerCt.changePanelWidthBy(delta);
			},

			_visualizationColumns : [],
			missingValuesScore : null,

			datasetName : null,
			datasetId : null,
			analysisId : null,
			analysisType : null,
												
			layout : 'vbox',
			items : [{
				xtype : 'button',
				ref : 'expandButton_',
				enableToggle : true,
				height : 10,
				width : 10,
				template : new Ext.Template('<div id="{1}"><canvas {0}></canvas></div>'),
				buttonSelector : 'canvas:first-child',
				getTemplateArgs : function() {
					return [this.cls, this.id];
				},
				listeners : {
					toggle : function(target, checked) {
						if (checked) {
							if (this.contrastsData === null) {
								// Load contrasts.
								DifferentialExpressionSearchController.differentialExpressionAnalysisVisualizationLoadContrastsInfo ( this.resultSetId,
																								 this.applicationRoot._imageArea._heatmapArea.geneIds, 
								function(data) {
									// Callback to display contrasts once they're loaded.
									// TODO: deal with failures?
									// TODO: spinning wheel?
									this.contrastsData = data;
									
									this.expandColumn_(); // Resize.
									
									this.applicationRoot._imageArea.topLabelsPanel._drawTopLabels();
									this.applicationRoot._imageArea._heatmapArea.doLayout();
									
								}.createDelegate(this));
							} else {
								this.expandColumn_(); // Resize.
								
								this.applicationRoot._imageArea.topLabelsPanel._drawTopLabels();
								this.applicationRoot._imageArea._heatmapArea.doLayout();
							}
						} else {
							this.collapseColumn_(); // Resize column and all containers.

							this.applicationRoot._imageArea.topLabelsPanel._drawTopLabels();
							this.applicationRoot._imageArea._heatmapArea.doLayout();
						}
					},
					scope : this
				}
			}]
		});

		Gemma.MetaHeatmapExpandableColumn.superclass.initComponent.apply (this, arguments);
	},

	filterHide : function () {
		this.setWidth(0);
		this.hide();
		this.isFiltered = true;
	},
		
	filterShow : function () {
		if (this.expandButton_.pressed) {this.setWidth(this.expandedWidth);} else {this.setWidth(this.collapsedWidth);} 
		this.show();
		this.isFiltered = false;				
	},
	
	expandColumn_ : function () {
		// Resize.		
		this.setWidth( this.expandedWidth );

		// Propagate size change to parents.	
		var widthChange	= this.expandedWidth - this.collapsedWidth;		
		this.updateAllParentContainersWidthBy( widthChange );
						
		// Redraw children.		
		for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
			this._visualizationColumns[geneGroupSubColumnIndex].expandSubColumn_();
		}

		// Redraw button.
		this.drawButton_('rgba(10,100,10, 0.8)');	
	},
	
	collapseColumn_ : function() {
		// Resize.		
		this.setWidth( this.collapsedWidth );

		// Propagate size change to parents.	
		var widthChange	= this.collapsedWidth - this.expandedWidth;		
		this.updateAllParentContainersWidthBy( widthChange );
						
		// Redraw children.		
		for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
			this._visualizationColumns[geneGroupSubColumnIndex].collapseSubColumn_();
		}		
		// Redraw button.
		this.drawButton_('rgba(10,100,10, 0.8)');			
	},
	
	drawButton_ : function (color) {
		// Clear canvas.
		var	ctx = Gemma.MetaVisualizationUtils.getCanvasContext (this.expandButton_.btnEl.dom);
		ctx.clearRect (0, 0, ctx.canvas.width, ctx.canvas.height);
				
		if (this.expandButton_.pressed) {
			this.setWidth (this.expandedWidth);
			ctx.canvas.width = this.expandedWidth;
			this.drawCollapseButton_ (ctx, color);			
		} else {
			this.setWidth (this.collapsedWidth);
			ctx.canvas.width = this.collapsedWidth;
			this.drawExpandButton_ (ctx, color);			
		}
	},
	
	drawExpandButton_ : function(ctx, color) {
		ctx.strokeStyle = color;
		ctx.lineWidth = 1;
		ctx.beginPath();
		ctx.moveTo(0.5, 0.5);
		ctx.lineTo(0.5, 9.5);
		ctx.lineTo(9.5, 9.5);
		ctx.lineTo(9.5, 0.5);
		ctx.lineTo(0.5, 0.5);
		
		ctx.moveTo(2.5, 4.5);
		
		ctx.lineTo(4.5, 4.5);
		ctx.lineTo(4.5, 2.5);
		ctx.lineTo(5.5, 2.5);
		ctx.lineTo(5.5, 4.5);
		ctx.lineTo(7.5, 4.5);
		ctx.lineTo(7.5, 5.5);
		ctx.lineTo(5.5, 5.5);
		ctx.lineTo(5.5, 7.5);
		ctx.lineTo(4.5, 7.5);
		ctx.lineTo(4.5, 5.5);
		ctx.lineTo(2.5, 5.5);
		ctx.stroke();
	},

	drawCollapseButton_ : function(ctx, color) {
		var width = ctx.canvas.width;
		width = width - 15;

		ctx.strokeStyle = color;
		ctx.lineWidth = 1;
		ctx.beginPath();
		ctx.moveTo(0.5, 0.5);
		ctx.lineTo(0.5, 9.5);
		ctx.lineTo(9.5, 9.5);
		ctx.lineTo(9.5, 0.5);
		ctx.lineTo(0.5, 0.5);
		
		ctx.moveTo(2.5, 4.5);
		ctx.lineTo(7.5, 4.5);
		ctx.moveTo(2.5, 5.5);
		ctx.lineTo(7.5, 5.5);												
		
		ctx.stroke();												
		this.drawHorizontalDottedLine_(ctx,10, 5.5, width);
		
		ctx.beginPath();
		ctx.moveTo(width + 6.5, 5.5);
		ctx.lineTo(width + 10.5, 5.5);
		ctx.lineTo(width + 10.5, 9.5);
		ctx.stroke();				
	},

	drawHorizontalDottedLine_ : function (ctx, xStart, y, length) {
		var xEnd = xStart + length;
		ctx.beginPath();
		ctx.moveTo(xStart,y);
		
		for (var x = xStart; x < xEnd; x=x+4) {
			ctx.lineTo(x+2,y);
			ctx.moveTo(x+4,y);
		}
		ctx.stroke();								
	},
	
	onRender : function() {
		Gemma.MetaHeatmapExpandableColumn.superclass.onRender.apply(this, arguments);

		for (var geneGroupIndex = 0; geneGroupIndex < this._numberOfRowGroups; geneGroupIndex++) {
			var subColumn = new Gemma.MetaHeatmapColumn({
						applicationRoot 		   : this.applicationRoot,
						visualizationSubColumnData : this._dataColumn.visualizationValues[geneGroupIndex],
						qValuesSubColumnData 	   : this._dataColumn.qValues[geneGroupIndex],
						factorValueNames 		   : this.factorValueNames,
						factorValueIds 			   : this.factorValueIds,
						rowGroup 				   : geneGroupIndex,
						columnIndex				   : this._columnIndex,
						columnGroupIndex 		   : this._columnGroupIndex,
						datasetGroupIndex 		   : this._datasetGroupIndex
					});

			this._visualizationColumns.push(subColumn);
			this.add(subColumn);
		}

		this.overallDifferentialExpressionScore = 0;
		this.missingValuesScore = 0;
		
		for (var i = 0; i < this._dataColumn.visualizationValues.length; i++) {
			for (var j = 0; j < this._dataColumn.visualizationValues[i].length; j++) {
				if (this._dataColumn.visualizationValues[i][j] === null) {
					this.missingValuesScore++;
					this.overallDifferentialExpressionScore += 0;
				} else {
					this.overallDifferentialExpressionScore += 0.05 + this._dataColumn.visualizationValues[i][j];
				}
			}
		}
		
		this.updateParentsScores (false);
		
		this.el.on('mouseover', function(e, t) {
					this.drawButton_('rgb(255,10,10)');					
				}, this);
		this.el.on('mouseout', function(e, t) {
					this.drawButton_('rgb(10,100,10)');					
				}, this);
	},
						
	updateParentsScores : function (isHiding) {
		if (isHiding) {
			this.ownerCt.overallDifferentialExpressionScore -= this.overallDifferentialExpressionScore;
			this.ownerCt.ownerCt.overallDifferentialExpressionScore -= this.overallDifferentialExpressionScore;

			this.ownerCt.specificityScore -= this.miniPieValue;
			this.ownerCt.ownerCt.specificityScore -= this.miniPieValue;
						
//			this.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.specificityScore);
//			this.ownerCt.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.ownerCt.specificityScore);
			
		} else {
			this.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;
			this.ownerCt.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;

			this.ownerCt.specificityScore += this.miniPieValue;
			this.ownerCt.ownerCt.specificityScore += this.miniPieValue;
			
//			this.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.specificityScore);
//			this.ownerCt.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.ownerCt.specificityScore);
		}

	},

	redraw_ : function() {
		this.drawButton_('rgba(10,100,10, 0.8)');
		for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
			this._visualizationColumns[geneGroupSubColumnIndex].drawHeatmapSubColumn_();
		}		
	},
	
	refresh : function() {
		this.redraw_ ();
	}
});

