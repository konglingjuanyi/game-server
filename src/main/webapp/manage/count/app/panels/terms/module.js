/** @scratch /panels/5
 *
 * include::panels/terms.asciidoc[]
 */

/** @scratch /panels/terms/0
 *
 * == terms
 * Status: *Stable*
 *
 * A table, bar chart or pie chart based on the results of an Elasticsearch terms facet.
 *
 */
define([
  'angular',
  'app',
  'lodash',
  'jquery',
  'kbn'
],
function (angular, app, _, $, kbn) {
  'use strict';

  var module = angular.module('kibana.panels.terms', []);
  app.useModule(module);

  var game;
  
  module.controller('terms', function($scope, querySrv, dashboard, filterSrv, fields) {
    game  = dashboard.current.game; 
    $scope.panelMeta = {
      modals : [
        {
          description: "Inspect",
          icon: "icon-info-sign",
          partial: "app/partials/inspector.html",
          show: $scope.panel.spyable
        }
      ],
      editorTabs : [
        {title:'Queries', src:'app/partials/querySelect.html'}
      ],
      status  : "Stable",
      description : "Displays the results of an elasticsearch facet as a pie " +
        "chart, bar chart, or a table.<br>" +
        "In order to be forward compatible, we reserve the right to display " +
        "min and order by max/avg/total. So we have to use stats aggregate, " +
        "But I think single min/max/avg/sum aggregation performs better.<br>" +
        "And also we remove total_count option in editor.html<br>" +
        "We do not support missing/other, but i will add them later<br>" +
        "为了向前兼容, 我们选择了用stats aggregations, " +
        "但考虑到性能以及可能不会有人按max排序,但展示的是最小值, " +
        "换用单一的min/max/avg/sum aggregation, 可能会更好.<br>" +
        "同时aggs里面不再有total_count的返回值,在配置界面中也拿掉了.<br>" +
        "暂时没有missing/other功能,以后会补上.<br>"
    };

    // Set and populate defaults
    var _d = {
      /** @scratch /panels/terms/5
       * === Parameters
       *
       * field:: The field on which to computer the facet
       */
      field   : '_type',
      /** @scratch /panels/terms/5
       * exclude:: terms to exclude from the results
       */
      exclude : [],
      /** @scratch /panels/terms/5
       * missing:: Set to false to disable the display of a counter showing how much results are
       * missing the field
       */
      missing : true,
      /** @scratch /panels/terms/5
       * other:: Set to false to disable the display of a counter representing the aggregate of all
       * values outside of the scope of your +size+ property
       */
      other   : true,
      /** @scratch /panels/terms/5
       * size:: Show this many terms
       */
      size    : 10,
      /** @scratch /panels/terms/5
       * order:: In terms mode: count, term, reverse_count or reverse_term,
       * in terms_stats mode: term, reverse_term, count, reverse_count,
       * total, reverse_total, min, reverse_min, max, reverse_max, mean or reverse_mean
       */
      order   : 'count',
      style   : { "font-size": '10pt'},
      /** @scratch /panels/terms/5
       * donut:: In pie chart mode, draw a hole in the middle of the pie to make a tasty donut.
       */
      donut   : false,
      /** @scratch /panels/terms/5
       * tilt:: In pie chart mode, tilt the chart back to appear as more of an oval shape
       */
      tilt    : false,
      /** @scratch /panels/terms/5
       * lables:: In pie chart mode, draw labels in the pie slices
       */
      labels  : true,
      /** @scratch /panels/terms/5
       * arrangement:: In bar or pie mode, arrangement of the legend. horizontal or vertical
       */
      arrangement : 'horizontal',
      /** @scratch /panels/terms/5
       * chart:: table, bar or pie
       */
      chart       : 'bar',
      /** @scratch /panels/terms/5
       * counter_pos:: The location of the legend in respect to the chart, above, below, or none.
       */
      counter_pos : 'above',
      /** @scratch /panels/terms/5
       * spyable:: Set spyable to false to disable the inspect button
       */
      spyable     : true,
      /** @scratch /panels/terms/5
       *
       * ==== Queries
       * queries object:: This object describes the queries to use on this panel.
       * queries.mode::: Of the queries available, which to use. Options: +all, pinned, unpinned, selected+
       * queries.ids::: In +selected+ mode, which query ids are selected.
       */
      queries     : {
        mode        : 'all',
        ids         : []
      },
      /** @scratch /panels/terms/5
       * multiterms:: Multi terms: used to either filterSrv
       */
      multiterms  : [],
      /** @scratch /panels/terms/5
       * fmode:: Field mode: normal or script
       */
      fmode       : 'normal',
      /** @scratch /panels/terms/5
       * tmode:: Facet mode: terms or terms_stats
       */
      tmode       : 'terms',
      /** @scratch /panels/terms/5
       * tstat:: Terms_stats facet stats field
       */
      tstat       : 'total',
      /** @scratch /panels/terms/5
       * valuefield:: Terms_stats facet value field
       */
      valuefield  : ''
    };

    _.defaults($scope.panel,_d);

    $scope.init = function () {
      $scope.hits = 0;

      $scope.$on('refresh',function(){
        $scope.get_data();
      });
      $scope.get_data();

    };

    $scope.get_data = function() {
      // Make sure we have everything for the request to complete
      if(dashboard.indices.length === 0) {
        return;
      }

      $scope.panelMeta.loading = true;
      var request,
        terms_facet,
        termstats_facet,
        results,
        boolQuery,
        queries;

      $scope.field = _.contains(fields.list,$scope.panel.field+'.raw') ?
        $scope.panel.field+'.raw' : $scope.panel.field;

      request = $scope.ejs.Request();

      $scope.panel.queries.ids = querySrv.idsByMode($scope.panel.queries);
      queries = querySrv.getQueryObjs($scope.panel.queries.ids);

      // This could probably be changed to a BoolFilter
      boolQuery = $scope.ejs.BoolQuery();
      _.each(queries,function(q) {
        boolQuery = boolQuery.should(querySrv.toEjsObj(q));
      });

      var query = $scope.ejs.FilteredQuery(
        boolQuery,
        filterSrv.getBoolFilter(filterSrv.ids())
      );
      // request = request.query(query);

      // Terms mode
      if($scope.panel.tmode === 'terms') {
        var terms_aggs = $scope.ejs.TermsAggregation('terms')
          .field($scope.field)
          .size($scope.panel.size)
          .exclude($scope.panel.exclude);
          if($scope.panel.fmode === 'script') {
            terms_aggs.script($scope.panel.script);
          }
          switch($scope.panel.order) {
            case 'term':
            terms_aggs.order('_term','asc');
            break;
            case 'count':
            terms_aggs.order('_count');
            break;
            case 'reverse_count':
            terms_aggs.order('_count','asc');
            break;
            case 'reverse_term':
            terms_aggs.order('_term');
            break;
            default:
            terms_aggs.order('_count');
          }
        request = request.query(query).agg(terms_aggs).size(0);
      }
      else if($scope.panel.tmode === 'terms_stats') {
        var terms_aggs = $scope.ejs.TermsAggregation('terms')
          .field($scope.panel.field)
          .size($scope.panel.size);

          var sub_aggs = $scope.ejs.StatsAggregation('subaggs')
            .field($scope.panel.valuefield);;
          // switch($scope.panel.tstat) {
          //   case 'count':
          //   break;
          //   case 'min':
          //   sub_aggs = $scope.ejs.MinAggregation('subaggs')
          //     .field($scope.panel.valuefield);
          //   break;
          //   case 'max':
          //   sub_aggs = $scope.ejs.MaxAggregation('subaggs')
          //     .field($scope.panel.valuefield);
          //   break;
          //   case 'total':
          //   sub_aggs = $scope.ejs.SumAggregation('subaggs')
          //     .field($scope.panel.valuefield);
          //   break;
          //   case 'mean':
          //   sub_aggs = $scope.ejs.AvgAggregation('subaggs')
          //     .field($scope.panel.valuefield);
          //   break;
          // }

          switch($scope.panel.order) {
            case 'term':
              terms_aggs.order('_term','asc');
              break;
            case 'reverse_term':
              terms_aggs.order('_term','desc');
              break;
            case 'count':
              terms_aggs.order('_count','desc');
              break;
            case 'reverse_count':
              terms_aggs.order('_count','asc');
              break;
            case 'total':
              terms_aggs.order('subaggs.sum','desc');
              break;
            case 'reverse_total':
              terms_aggs.order('subaggs.sum','asc');
              break;
            case 'min':
              terms_aggs.order('subaggs.min','desc');
              break;
            case 'reverse_min':
              terms_aggs.order('subterms.min','asc');
              break;
            case 'max':
              terms_aggs.order('subaggs.max','desc');
              break;
            case 'reverse_max':
              terms_aggs.order('subaggs.max','asc');
              break;
            case 'mean':
              terms_aggs.order('subaggs.avg','desc');
              break;
            case 'reverse_mean':
              terms_aggs.order('subaggs.avg','asc');
              break;
          }

        request = request.query(query)
        .agg(terms_aggs.agg(sub_aggs)).size(0);
      }

      // Populate the inspector panel
      $scope.inspector = request.toJSON();

      results = $scope.ejs.doSearch(dashboard.indices, request);

      // Populate scope when we have results
      results.then(function(results) {
        $scope.panelMeta.loading = false;
        $scope.hits = results.hits.total;

        $scope.results = results;

        $scope.$emit('render');
      });
    };

    $scope.build_search = function(term,negate) {
      if($scope.panel.fmode === 'script') {
        filterSrv.set({type:'script',script:$scope.panel.script + ' == \"' + term.label + '\"',
          mandate:(negate ? 'mustNot':'must')});
      } else if(_.isUndefined(term.meta)) {
        filterSrv.set({type:'terms',field:$scope.field,value:term.label,
          mandate:(negate ? 'mustNot':'must')});
      } else if(term.meta === 'missing') {
        filterSrv.set({type:'exists',field:$scope.field,
          mandate:(negate ? 'must':'mustNot')});
      } else {
        return;
      }
    };

    var build_multi_search = function(term) {
      if($scope.panel.fmode === 'script') {
        return({type:'script',script:$scope.panel.script + ' == \"' + term.label + '\"',
          mandate:'either', alias: term.label});
      } else if(_.isUndefined(term.meta)) {
        return({type:'terms',field:$scope.field,value:term.label, mandate:'either'});
      } else if(term.meta === 'missing') {
        return({type:'exists',field:$scope.field, mandate:'either'});
      } else {
        return;
      }
    };

    $scope.multi_search = function() {
      _.each($scope.panel.multiterms, function(t) {
        var f = build_multi_search(t);
        filterSrv.set(f, undefined, true)
      });
      dashboard.refresh();
    };
    $scope.add_multi_search = function(term) {
      $scope.panel.multiterms.push(term);
    };
    $scope.delete_multi_search = function(term) {
      _.remove($scope.panel.multiterms, term);
    };
    $scope.check_multi_search = function(term) {
      return _.indexOf($scope.panel.multiterms, term) >= 0;
    };

    $scope.set_refresh = function (state) {
      $scope.refresh = state;
    };

    $scope.close_edit = function() {
      if($scope.refresh) {
        $scope.get_data();
      }
      $scope.refresh =  false;
      $scope.$emit('render');
    };

    $scope.showMeta = function(term) {
      if(_.isUndefined(term.meta)) {
        return true;
      }
      if(term.meta === 'other' && !$scope.panel.other) {
        return false;
      }
      if(term.meta === 'missing' && !$scope.panel.missing) {
        return false;
      }
      return true;
    };

  });

  module.directive('termsChart', function(querySrv) {
    return {
      restrict: 'A',
      link: function(scope, elem) {
        var plot;

        // Receive render events
        scope.$on('render',function(){
          render_panel();
        });

        function build_results() {
          //forward_compatible_map
          var _fcm = {
            "sum":"sum",
            "total":"sum",
            "mean":"avg",
            "avg":"avg",
            "min":"min",
            "max":"max",
            "count":"count"
          };
          var k = 0;
          scope.data = [];

          var all =[];
          _.each(scope.results.facets.terms.terms, function(v) {
            all.push(v.term);
          });
          var map = {};
          $.ajax({                                               
          url: 'findallvalue?name='+scope.panel.field + '&game=' + game, 
          type: 'POST',
          contentType: "application/json;charset=UTF-8",    
          data: JSON.stringify(all), 
          dataType: 'json',
          async : false,
          success: function(dat){
               map = eval(dat);
            },error:function(xhr){}//回调看看是否有出错
          });

          _.each(scope.results.aggregations.terms.buckets, function(v) {
            var slice;
            if(scope.panel.tmode === 'terms') {
                if(scope.panel.field==="运营大区ID" ||scope.panel.field==="渠道ID" ||scope.panel.field==="获得途径" ||scope.panel.field==="消耗途径" || scope.panel.field==="日志道具id" || scope.panel.field==="功能编号" || scope.panel.field==="注册渠道"){
                    slice = { label : map[v.term] , name : v.term, data : [[k,v.doc_count]], actions: true};
                }else if(scope.panel.field==="服务器ID"){
                      if(game === "fb"){
                        slice = { label : "fb_s"+v.term, name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else if(game === "kds"){
                      slice = { label : "kds_s"+v.term, name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else if(game ==="kun"){
                      slice = { label : "kun_s"+v.term, name : v.term, data : [[k,v.doc_count]], actions: true};
                    }
                }else if(scope.panel.field==="人物等级"||scope.panel.field==="角色等级"){
                  slice = { label : v.term+'级', name : v.term, data : [[k,v.doc_count]], actions: true};
                }else if(scope.panel.field==="支付货币"){
                  if(v.term=='1'){
                  slice = { label : '人民币',  name : v.term, data : [[k,v.doc_count]], actions: true};
                  }else if(v.term=='2'){
                  slice = { label : '美元',  name : v.term, data : [[k,v.doc_count]], actions: true};
                  }else{
                    slice = { label : v.term,  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }
                }else if(scope.panel.field==="登录设备系统"){
                  if(v.term=='1'){
                    slice = { label : 'android',  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else if(v.term=='2'){
                    slice = { label : 'ios',  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else if(v.term=='0'){
                    slice = { label : '未知登陆设备',  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else{
                    slice = { label : v.term,  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }
                  }else if(scope.panel.field==="是否首次登录"){
                  if(v.term=='0'){
                      slice = { label : '老用户',  name : v.term, data : [[k,v.doc_count]], actions: true};
                  }else if(v.term=='1'){
                      slice = { label : '新用户',  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }else{
                      slice = { label : v.term,  name : v.term, data : [[k,v.doc_count]], actions: true};
                    }
                  }else{
                  slice = { label : v.term,  name : v.term, data : [[k,v.doc_count]], actions: true};
                }
            }
            if(scope.panel.tmode === 'terms_stats') {
                if(scope.panel.field==="运营大区ID" ||scope.panel.field==="渠道ID" ||scope.panel.field==="获得途径" ||scope.panel.field==="消耗途径" || scope.panel.field==="日志道具id" || scope.panel.field==="功能编号" || scope.panel.field==="注册渠道"){
                      slice = { label : map[v.term],  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                }
                else if(scope.panel.field==="服务器ID"){
                      if(game === "fb"){
                        slice = { label : "fb_s"+v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }else if(game === "kds"){
                      slice = { label : "kds_s"+v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }else if(game ==="kun"){
                      slice = { label : "kun_s"+v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }
                }
                else if(scope.panel.field==="人物等级"||scope.panel.field==="角色等级"){
                  slice = { label : v.term+'级',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                }else if(scope.panel.field==="支付货币"){
                  if(v.term=='1'){
                      slice = { label : '人民币',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                      }else if(v.term=='2'){
                      slice = { label : '美元',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }else{
                    slice = { label : v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }
                }else if(scope.panel.field==="登录设备系统"){
                  if(v.term=='1'){
                      slice = { label : 'android',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                      }else if(v.term=='2'){
                      slice = { label : 'ios',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                      }else if(v.term=='0'){
                      slice = { label : '未知登陆设备',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                      }else{
                        slice = { label : v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                        }
                  }else if(scope.panel.field==="是否首次登录"){
                  if(v.term=='0'){
                      slice = { label : '老用户',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                      }else if(v.term=='1'){
                      slice = { label : '新用户',  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                    }else{
                    slice = { label : v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                  }
                }else{
                    slice = { label : v.term,  name : v.term, data : [[k,v['subaggs'][_fcm[scope.panel.tstat]]]], actions: true};
                }              
            }
            scope.data.push(slice);
            k = k + 1;
          });

          // scope.data.push({label:'Missing field',
          //   data:[[k,scope.results.facets.terms.missing]],meta:"missing",color:'#aaa',opacity:0});
          //
          // if(scope.panel.tmode === 'terms') {
          //   scope.data.push({label:'Other values',
          //     data:[[k+1,scope.results.facets.terms.other]],meta:"other",color:'#444'});
          // }
        }

        // Function for rendering panel
        function render_panel() {
          var chartData;

          build_results();

          // IE doesn't work without this
          elem.css({height:scope.panel.height||scope.row.height});

          // Make a clone we can operate on.
          chartData = _.clone(scope.data);
          chartData = scope.panel.missing ? chartData :
            _.without(chartData,_.findWhere(chartData,{meta:'missing'}));
          chartData = scope.panel.other ? chartData :
          _.without(chartData,_.findWhere(chartData,{meta:'other'}));

          // Populate element.
          require(['jquery.flot.pie'], function(){
            // Populate element
            try {
              // Add plot to scope so we can build out own legend
              if(scope.panel.chart === 'bar') {
                plot = $.plot(elem, chartData, {
                  legend: { show: false },
                  series: {
                    lines:  { show: false, },
                    bars:   { show: true,  fill: 1, barWidth: 0.8, horizontal: false },
                    shadowSize: 1
                  },
                  yaxis: { show: true, min: 0, color: "#c8c8c8" },
                  xaxis: { show: false },
                  grid: {
                    borderWidth: 0,
                    borderColor: '#c8c8c8',
                    color: "#c8c8c8",
                    hoverable: true,
                    clickable: true
                  },
                  colors: querySrv.colors
                });
              }
              if(scope.panel.chart === 'pie') {
                var labelFormat = function(name, series){
                  return '<div ng-click="build_search(panel.field,\''+name+'\')'+
                    ' "style="font-size:8pt;text-align:center;padding:2px;color:white;">'+
                    name+'<br/>'+Math.round(series.percent)+'%</div>';
                };
                plot = $.plot(elem, chartData, {
                  legend: { show: false },
                  series: {
                    pie: {
                      innerRadius: scope.panel.donut ? 0.4 : 0,
                      tilt: scope.panel.tilt ? 0.45 : 1,
                      radius: 1,
                      show: true,
                      combine: {
                        color: '#999',
                        label: 'The Rest'
                      },
                      stroke: {
                        width: 0
                      },
                      label: {
                        show: scope.panel.labels,
                        radius: 2/3,
                        formatter: labelFormat,
                        threshold: 0.1
                      }
                    }
                  },
                  //grid: { hoverable: true, clickable: true },
                  grid:   { hoverable: true, clickable: true, color: '#c8c8c8' },
                  colors: querySrv.colors
                });
              }

              // Populate legend
              if(elem.is(":visible")){
                setTimeout(function(){
                  scope.legend = plot.getData();
                  if(!scope.$$phase) {
                    scope.$apply();
                  }
                });
              }

            } catch(e) {
              elem.text(e);
            }
          });
        }

        elem.bind("plotclick", function (event, pos, object) {
          if(object) {
            scope.build_search(scope.data[object.seriesIndex]);
          }
        });

        var $tooltip = $('<div>');
        elem.bind("plothover", function (event, pos, item) {
          if (item) {
            var value = scope.panel.chart === 'bar' ? item.datapoint[1] : item.datapoint[1][0][1];
            $tooltip
              .html(
                kbn.query_color_dot(item.series.color, 20) + ' ' +
                item.series.label + " (" + value.toFixed(0) +
                (scope.panel.chart === 'pie' ? (", " + Math.round(item.datapoint[0]) + "%") : "") + ")"
              )
              .place_tt(pos.pageX, pos.pageY);
          } else {
            $tooltip.remove();
          }
        });

      }
    };
  });

});
