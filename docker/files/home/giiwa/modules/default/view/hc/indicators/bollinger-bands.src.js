/**
 * @license Highstock JS v7.2.1 (2019-10-31)
 *
 * Indicator series type for Highstock
 *
 * (c) 2010-2019 Paweł Fus
 *
 * License: www.highcharts.com/license
 */
'use strict';
(function (factory) {
    if (typeof module === 'object' && module.exports) {
        factory['default'] = factory;
        module.exports = factory;
    } else if (typeof define === 'function' && define.amd) {
        define('highcharts/indicators/bollinger-bands', ['highcharts', 'highcharts/modules/stock'], function (Highcharts) {
            factory(Highcharts);
            factory.Highcharts = Highcharts;
            return factory;
        });
    } else {
        factory(typeof Highcharts !== 'undefined' ? Highcharts : undefined);
    }
}(function (Highcharts) {
    var _modules = Highcharts ? Highcharts._modules : {};
    function _registerModule(obj, path, args, fn) {
        if (!obj.hasOwnProperty(path)) {
            obj[path] = fn.apply(null, args);
        }
    }
    _registerModule(_modules, 'mixins/multipe-lines.js', [_modules['parts/Globals.js'], _modules['parts/Utilities.js']], function (H, U) {
        /**
         *
         *  (c) 2010-2019 Wojciech Chmiel
         *
         *  License: www.highcharts.com/license
         *
         *  !!!!!!! SOURCE GETS TRANSPILED BY TYPESCRIPT. EDIT TS FILE ONLY. !!!!!!!
         *
         * */
        var defined = U.defined;
        var each = H.each, merge = H.merge, error = H.error, SMA = H.seriesTypes.sma;
        /**
         * Mixin useful for all indicators that have more than one line.
         * Merge it with your implementation where you will provide
         * getValues method appropriate to your indicator and pointArrayMap,
         * pointValKey, linesApiNames properites. Notice that pointArrayMap
         * should be consistent with amount of lines calculated in getValues method.
         *
         * @private
         * @mixin multipleLinesMixin
         */
        var multipleLinesMixin = {
            /* eslint-disable valid-jsdoc */
            /**
             * Lines ids. Required to plot appropriate amount of lines.
             * Notice that pointArrayMap should have more elements than
             * linesApiNames, because it contains main line and additional lines ids.
             * Also it should be consistent with amount of lines calculated in
             * getValues method from your implementation.
             *
             * @private
             * @name multipleLinesMixin.pointArrayMap
             * @type {Array<string>}
             */
            pointArrayMap: ['top', 'bottom'],
            /**
             * Main line id.
             *
             * @private
             * @name multipleLinesMixin.pointValKey
             * @type {string}
             */
            pointValKey: 'top',
            /**
             * Additional lines DOCS names. Elements of linesApiNames array should
             * be consistent with DOCS line names defined in your implementation.
             * Notice that linesApiNames should have decreased amount of elements
             * relative to pointArrayMap (without pointValKey).
             *
             * @private
             * @name multipleLinesMixin.linesApiNames
             * @type {Array<string>}
             */
            linesApiNames: ['bottomLine'],
            /**
             * Create translatedLines Collection based on pointArrayMap.
             *
             * @private
             * @function multipleLinesMixin.getTranslatedLinesNames
             * @param {string} [excludedValue]
             *        Main line id
             * @return {Array<string>}
             *         Returns translated lines names without excluded value.
             */
            getTranslatedLinesNames: function (excludedValue) {
                var translatedLines = [];
                each(this.pointArrayMap, function (propertyName) {
                    if (propertyName !== excludedValue) {
                        translatedLines.push('plot' +
                            propertyName.charAt(0).toUpperCase() +
                            propertyName.slice(1));
                    }
                });
                return translatedLines;
            },
            /**
             * @private
             * @function multipleLinesMixin.toYData
             * @param {Highcharts.Point} point
             *        Indicator point
             * @return {Array<number>}
             *         Returns point Y value for all lines
             */
            toYData: function (point) {
                var pointColl = [];
                each(this.pointArrayMap, function (propertyName) {
                    pointColl.push(point[propertyName]);
                });
                return pointColl;
            },
            /**
             * Add lines plot pixel values.
             *
             * @private
             * @function multipleLinesMixin.translate
             * @return {void}
             */
            translate: function () {
                var indicator = this, pointArrayMap = indicator.pointArrayMap, LinesNames = [], value;
                LinesNames = indicator.getTranslatedLinesNames();
                SMA.prototype.translate.apply(indicator, arguments);
                each(indicator.points, function (point) {
                    each(pointArrayMap, function (propertyName, i) {
                        value = point[propertyName];
                        if (value !== null) {
                            point[LinesNames[i]] = indicator.yAxis.toPixels(value, true);
                        }
                    });
                });
            },
            /**
             * Draw main and additional lines.
             *
             * @private
             * @function multipleLinesMixin.drawGraph
             * @return {void}
             */
            drawGraph: function () {
                var indicator = this, pointValKey = indicator.pointValKey, linesApiNames = indicator.linesApiNames, mainLinePoints = indicator.points, pointsLength = mainLinePoints.length, mainLineOptions = indicator.options, mainLinePath = indicator.graph, gappedExtend = {
                    options: {
                        gapSize: mainLineOptions.gapSize
                    }
                }, 
                // additional lines point place holders:
                secondaryLines = [], secondaryLinesNames = indicator.getTranslatedLinesNames(pointValKey), point;
                // Generate points for additional lines:
                each(secondaryLinesNames, function (plotLine, index) {
                    // create additional lines point place holders
                    secondaryLines[index] = [];
                    while (pointsLength--) {
                        point = mainLinePoints[pointsLength];
                        secondaryLines[index].push({
                            x: point.x,
                            plotX: point.plotX,
                            plotY: point[plotLine],
                            isNull: !defined(point[plotLine])
                        });
                    }
                    pointsLength = mainLinePoints.length;
                });
                // Modify options and generate additional lines:
                each(linesApiNames, function (lineName, i) {
                    if (secondaryLines[i]) {
                        indicator.points = secondaryLines[i];
                        if (mainLineOptions[lineName]) {
                            indicator.options = merge(mainLineOptions[lineName].styles, gappedExtend);
                        }
                        else {
                            error('Error: "There is no ' + lineName +
                                ' in DOCS options declared. Check if linesApiNames' +
                                ' are consistent with your DOCS line names."' +
                                ' at mixin/multiple-line.js:34');
                        }
                        indicator.graph = indicator['graph' + lineName];
                        SMA.prototype.drawGraph.call(indicator);
                        // Now save lines:
                        indicator['graph' + lineName] = indicator.graph;
                    }
                    else {
                        error('Error: "' + lineName + ' doesn\'t have equivalent ' +
                            'in pointArrayMap. To many elements in linesApiNames ' +
                            'relative to pointArrayMap."');
                    }
                });
                // Restore options and draw a main line:
                indicator.points = mainLinePoints;
                indicator.options = mainLineOptions;
                indicator.graph = mainLinePath;
                SMA.prototype.drawGraph.call(indicator);
            }
        };

        return multipleLinesMixin;
    });
    _registerModule(_modules, 'indicators/bollinger-bands.src.js', [_modules['parts/Globals.js'], _modules['parts/Utilities.js'], _modules['mixins/multipe-lines.js']], function (H, U, multipleLinesMixin) {
        /**
         *
         *  License: www.highcharts.com/license
         *
         *  !!!!!!! SOURCE GETS TRANSPILED BY TYPESCRIPT. EDIT TS FILE ONLY. !!!!!!!
         *
         * */
        var isArray = U.isArray;
        var merge = H.merge, SMA = H.seriesTypes.sma;
        /* eslint-disable valid-jsdoc */
        // Utils:
        /**
         * @private
         */
        function getStandardDeviation(arr, index, isOHLC, mean) {
            var variance = 0, arrLen = arr.length, std = 0, i = 0, value;
            for (; i < arrLen; i++) {
                value = (isOHLC ? arr[i][index] : arr[i]) - mean;
                variance += value * value;
            }
            variance = variance / (arrLen - 1);
            std = Math.sqrt(variance);
            return std;
        }
        /* eslint-enable valid-jsdoc */
        /**
         * Bollinger Bands series type.
         *
         * @private
         * @class
         * @name Highcharts.seriesTypes.bb
         *
         * @augments Highcharts.Series
         */
        H.seriesType('bb', 'sma', 
        /**
         * Bollinger bands (BB). This series requires the `linkedTo` option to be
         * set and should be loaded after the `stock/indicators/indicators.js` file.
         *
         * @sample stock/indicators/bollinger-bands
         *         Bollinger bands
         *
         * @extends      plotOptions.sma
         * @since        6.0.0
         * @product      highstock
         * @requires     stock/indicators/indicators
         * @requires     stock/indicators/bollinger-bands
         * @optionparent plotOptions.bb
         */
        {
            params: {
                period: 20,
                /**
                 * Standard deviation for top and bottom bands.
                 */
                standardDeviation: 2,
                index: 3
            },
            /**
             * Bottom line options.
             */
            bottomLine: {
                /**
                 * Styles for a bottom line.
                 */
                styles: {
                    /**
                     * Pixel width of the line.
                     */
                    lineWidth: 1,
                    /**
                     * Color of the line. If not set, it's inherited from
                     * [plotOptions.bb.color](#plotOptions.bb.color).
                     *
                     * @type  {Highcharts.ColorString}
                     */
                    lineColor: undefined
                }
            },
            /**
             * Top line options.
             *
             * @extends plotOptions.bb.bottomLine
             */
            topLine: {
                styles: {
                    lineWidth: 1,
                    /**
                     * @type {Highcharts.ColorString}
                     */
                    lineColor: undefined
                }
            },
            tooltip: {
                pointFormat: '<span style="color:{point.color}">\u25CF</span><b> {series.name}</b><br/>Top: {point.top}<br/>Middle: {point.middle}<br/>Bottom: {point.bottom}<br/>'
            },
            marker: {
                enabled: false
            },
            dataGrouping: {
                approximation: 'averages'
            }
        }, 
        /**
         * @lends Highcharts.Series#
         */
        H.merge(multipleLinesMixin, {
            pointArrayMap: ['top', 'middle', 'bottom'],
            pointValKey: 'middle',
            nameComponents: ['period', 'standardDeviation'],
            linesApiNames: ['topLine', 'bottomLine'],
            init: function () {
                SMA.prototype.init.apply(this, arguments);
                // Set default color for lines:
                this.options = merge({
                    topLine: {
                        styles: {
                            lineColor: this.color
                        }
                    },
                    bottomLine: {
                        styles: {
                            lineColor: this.color
                        }
                    }
                }, this.options);
            },
            getValues: function (series, params) {
                var period = params.period, standardDeviation = params.standardDeviation, xVal = series.xData, yVal = series.yData, yValLen = yVal ? yVal.length : 0, 
                // 0- date, 1-middle line, 2-top line, 3-bottom line
                BB = [], 
                // middle line, top line and bottom line
                ML, TL, BL, date, xData = [], yData = [], slicedX, slicedY, stdDev, isOHLC, point, i;
                if (xVal.length < period) {
                    return false;
                }
                isOHLC = isArray(yVal[0]);
                for (i = period; i <= yValLen; i++) {
                    slicedX = xVal.slice(i - period, i);
                    slicedY = yVal.slice(i - period, i);
                    point = SMA.prototype.getValues.call(this, {
                        xData: slicedX,
                        yData: slicedY
                    }, params);
                    date = point.xData[0];
                    ML = point.yData[0];
                    stdDev = getStandardDeviation(slicedY, params.index, isOHLC, ML);
                    TL = ML + standardDeviation * stdDev;
                    BL = ML - standardDeviation * stdDev;
                    BB.push([date, TL, ML, BL]);
                    xData.push(date);
                    yData.push([TL, ML, BL]);
                }
                return {
                    values: BB,
                    xData: xData,
                    yData: yData
                };
            }
        }));
        /**
         * A bollinger bands indicator. If the [type](#series.bb.type) option is not
         * specified, it is inherited from [chart.type](#chart.type).
         *
         * @extends   series,plotOptions.bb
         * @since     6.0.0
         * @excluding dataParser, dataURL
         * @product   highstock
         * @requires  stock/indicators/indicators
         * @requires  stock/indicators/bollinger-bands
         * @apioption series.bb
         */
        ''; // to include the above in the js output

    });
    _registerModule(_modules, 'masters/indicators/bollinger-bands.src.js', [], function () {


    });
}));