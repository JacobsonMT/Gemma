Ext.namespace('Gemma');

Gemma.MetaVisualizationConfig = {};

// Sizes
Gemma.MetaVisualizationConfig.cellWidth = 14;
Gemma.MetaVisualizationConfig.cellHeight = 10;
Gemma.MetaVisualizationConfig.groupSeparatorHeight = 4;
Gemma.MetaVisualizationConfig.columnSeparatorWidth = 1;
Gemma.MetaVisualizationConfig.groupSeparatorWidth = 4;
Gemma.MetaVisualizationConfig.geneLabelFontSize = 9;
Gemma.MetaVisualizationConfig.columnLabelFontSize = 9;

Gemma.MetaVisualizationConfig.labelAngle = 315.0;
Gemma.MetaVisualizationConfig.labelBaseYCoor = 196;
Gemma.MetaVisualizationConfig.columnLabelHeight = 210;

Gemma.MetaVisualizationConfig.panelWidth = (Ext.getBody().getViewSize().width > 1000)
		? Ext.getBody().getViewSize().width * .95
		: 1000;
Gemma.MetaVisualizationConfig.panelHeight = 600;// (Ext.getBody().getViewSize().height
// > 500)?
// Ext.getBody().getViewSize().height*.90 : 500;

mm = Math.tan((360 - Gemma.MetaVisualizationConfig.labelAngle) * Math.PI / 180);
Gemma.MetaVisualizationConfig.labelExtraSpace = Math.floor(Gemma.MetaVisualizationConfig.labelBaseYCoor / mm);

// Colors
Gemma.MetaVisualizationConfig.cellHighlightColor = 'yellow';
Gemma.MetaVisualizationConfig.defaultLabelColor = 'black';
Gemma.MetaVisualizationConfig.geneLabelHighlightColor = 'red';
Gemma.MetaVisualizationConfig.analysisLabelHighlightColor = 'rgb(255,140,0)';
Gemma.MetaVisualizationConfig.baselineFactorValueColor = 'rgb(128, 0, 0)';
Gemma.MetaVisualizationConfig.factorValueDefaultColor = 'rgb(46,139,87)';
Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor1 = 'rgba(10,100,10, 0.1)';
Gemma.MetaVisualizationConfig.analysisLabelBackgroundColor2 = 'rgba(10,100,10, 0.05)';

Gemma.MetaVisualizationConfig.miniPieColor = 'rgb(95,158,160)';
Gemma.MetaVisualizationConfig.miniPieColorInvalid = 'rgb(192,192,192)';

Gemma.MetaVisualizationConfig.basicColourRange = new org.systemsbiology.visualization.DiscreteColorRange(20, {
			min : -1,
			max : 1
		}, {
			maxColor : {
				r : 255,
				g : 215,
				b : 0,
				a : 1
			},
			minColor : {
				r : 255,
				g : 255,
				b : 0,
				a : 1
			},
			emptyDataColor : {
				r : 100,
				g : 100,
				b : 100,
				a : 0.8
			},
			passThroughBlack : true
		});

Gemma.MetaVisualizationConfig.contrastsColourRange = new org.systemsbiology.visualization.DiscreteColorRange(20, {
			min : -1,
			max : 1
		}, {
			maxColor : {
				r : 0,
				g : 200,
				b : 255,
				a : 1
			},
			minColor : {
				r : 255,
				g : 255,
				b : 0,
				a : 1
			},
			emptyDataColor : {
				r : 100,
				g : 100,
				b : 100,
				a : 0.8
			},
			passThroughBlack : true
		});
