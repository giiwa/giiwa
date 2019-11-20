/*
 Highcharts JS v7.2.1 (2019-10-31)

 Vector plot series module

 (c) 2010-2019 Torstein Honsi

 License: www.highcharts.com/license
*/
(function(a){"object"===typeof module&&module.exports?(a["default"]=a,module.exports=a):"function"===typeof define&&define.amd?define("highcharts/modules/vector",["highcharts"],function(c){a(c);a.Highcharts=c;return a}):a("undefined"!==typeof Highcharts?Highcharts:void 0)})(function(a){function c(a,e,c,f){a.hasOwnProperty(e)||(a[e]=f.apply(null,c))}a=a?a._modules:{};c(a,"modules/vector.src.js",[a["parts/Globals.js"],a["parts/Utilities.js"]],function(a,c){var e=c.arrayMax,f=c.pick;c=a.seriesType;c("vector",
"scatter",{lineWidth:2,marker:null,rotationOrigin:"center",states:{hover:{lineWidthPlus:1}},tooltip:{pointFormat:"<b>[{point.x}, {point.y}]</b><br/>Length: <b>{point.length}</b><br/>Direction: <b>{point.direction}\u00b0</b><br/>"},vectorLength:20},{pointArrayMap:["y","length","direction"],parallelArrays:["x","y","length","direction"],pointAttribs:function(a,b){var d=this.options;a=a.color||this.color;var c=this.options.lineWidth;b&&(a=d.states[b].color||a,c=(d.states[b].lineWidth||c)+(d.states[b].lineWidthPlus||
0));return{stroke:a,"stroke-width":c}},markerAttribs:a.noop,getSymbol:a.noop,arrow:function(a){a=a.length/this.lengthMax*this.options.vectorLength/20;var b={start:10*a,center:0,end:-10*a}[this.options.rotationOrigin]||0;return["M",0,7*a+b,"L",-1.5*a,7*a+b,0,10*a+b,1.5*a,7*a+b,0,7*a+b,0,-10*a+b]},translate:function(){a.Series.prototype.translate.call(this);this.lengthMax=e(this.lengthData)},drawPoints:function(){var a=this.chart;this.points.forEach(function(b){var c=b.plotX,d=b.plotY;!1===this.options.clip||
a.isInsidePlot(c,d,a.inverted)?(b.graphic||(b.graphic=this.chart.renderer.path().add(this.markerGroup).addClass("highcharts-point highcharts-color-"+f(b.colorIndex,b.series.colorIndex))),b.graphic.attr({d:this.arrow(b),translateX:c,translateY:d,rotation:b.direction}),this.chart.styledMode||b.graphic.attr(this.pointAttribs(b))):b.graphic&&(b.graphic=b.graphic.destroy())},this)},drawGraph:a.noop,animate:function(c){c?this.markerGroup.attr({opacity:.01}):(this.markerGroup.animate({opacity:1},a.animObject(this.options.animation)),
this.animate=null)}});""});c(a,"masters/modules/vector.src.js",[],function(){})});
//# sourceMappingURL=vector.js.map