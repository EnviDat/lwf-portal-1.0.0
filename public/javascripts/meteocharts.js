var options2 = {};

$(function () {
    $('#selectStationId').change(function() {
        selectedStation = $('#selectStationId option:selected').val();
        selectedStationName = $('#selectStationId option:selected').text();
         selectedTextMessArt = $('#messArtSelected option:selected').text();
                dateDistance_data = [];
                    $('#bubbleChart').highcharts({
                        chart: {
                            type: 'spline',
                            zoomType: 'x'
                        },

                        title: {
                            text: 'Meteo Data for a week' + selectedStationName
                        },
                        subtitle: {
                            text: 'select to zoom in'
                        },
                        xAxis: {
                            type: 'datetime',
                            title: {
                            text: 'Date Time'
                         }
                        },
                        yAxis: {
                            title: {
                            text: selectedTextMessArt
                            },
                            min: -10
                        },
                        tooltip: {
                            headerFormat: '<b>{series.messdate}</b><br>',
                                    xDateFormat: '%Y-%m-%d',
                            pointFormat: '{point.x:%e- %b- %y %H:%M}: {point.y:.2f} '
                        },
                         plotOptions: {
                             spline: {
                                 marker: {
                                     enabled: true
                                 }
                                }
                          }
                       });
                selectedValueMessArt = $('#messArtSelected option:selected').val();
        		var chart = $('#bubbleChart').highcharts();

                    jQuery.getJSON('/listMeteoDataJson/' + parseInt(selectedStation), function(data) {
                    console.log('Station Number is:' + selectedStation)
                           $.map(data, function(obj, i) {
                           selectedValueMessArt = parseInt($('#messArtSelected').val());
                           if(obj.messart === selectedValueMessArt) {
                                var dateString = obj.messdate.substring(0,10); // 18-04-2017 12:10:00"
                                var timeString = obj.messdate.substring(11,18);

                                var dateParts = dateString.split("-");
                                var timeParts = timeString.split(":");

                                var dateObject = new Date(dateParts[2], dateParts[1]-1, dateParts[0],timeParts[0],timeParts[1],timeParts[2],0);
                                dateDistance_data.push([dateObject, obj.messwert]);
                                }
                            });
        					dateDistance_data = dateDistance_data.reverse();
            				console.log(dateDistance_data);

                while(chart.series.length > 0)
                chart.series[0].remove(true);
                chart.addSeries({ name: selectedTextMessArt,
                                  data: dateDistance_data
                                  });
                 $('#bubbleChart').highcharts().redraw();
              });
            });
            $('#getcsvAnchor').click(function() {
                var chart = $('#bubbleChart').highcharts();
                $(this).attr('href', 'data:text/csv;charset=utf-8,'+escape(chart.getCSV()));
                $(this).attr('download', "data-visualisation.csv");
            });
    });

$(function () {
    $('#messArtSelected').change(function() {
    selectedTextMessArt = $('#messArtSelected option:selected').text();
        dateDistance_data = [];
            $('#bubbleChart').highcharts({
                chart: {
                    type: 'spline',
                    zoomType: 'x'
                },
                title: {
                    text: 'Meteo Data for a week' + selectedStationName
                },
                subtitle: {
                    text: 'select to zoom in'
                },
                xAxis: {
                    type: 'datetime',
                    title: {
                    text: 'Date Time'
                 }
                },
                yAxis: {
                    title: {
                    text: selectedTextMessArt
                    },
                    min: -10
                },
                tooltip: {
                    headerFormat: '<b>{series.messdate}</b><br>',
                    pointFormat: '{point.x:%e. %b}: {point.y:.2f} '
                },
                 plotOptions: {
                     spline: {
                         marker: {
                             enabled: true
                         }
                        }
                  }
               });
        selectedValueMessArt = $('#messArtSelected option:selected').val();
		var chart = $('#bubbleChart').highcharts();

            jQuery.getJSON('/listMeteoDataJson/' + parseInt(selectedStation), function(data) {
            console.log('Station Number is:' + selectedStation)
                   $.map(data, function(obj, i) {
                   selectedValueMessArt = parseInt($('#messArtSelected').val());
                   if(obj.messart === selectedValueMessArt) {
                        var dateString = obj.messdate.substring(0,10); // 18-04-2017 12:10:00"
                        var timeString = obj.messdate.substring(11,18);

                        var dateParts = dateString.split("-");
                        var timeParts = timeString.split(":");

                        var dateObject = new Date(dateParts[2], dateParts[1]-1, dateParts[0],timeParts[0],timeParts[1],timeParts[2],0);
                        dateDistance_data.push([dateObject, obj.messwert]);
                        }
                    });
					dateDistance_data = dateDistance_data.reverse();
    				console.log(dateDistance_data);

        while(chart.series.length > 0)
        chart.series[0].remove(true);
        chart.addSeries({ name: selectedTextMessArt,
                          data: dateDistance_data
                          });
         $('#bubbleChart').highcharts().redraw();
      });
    });
    $('#getcsvAnchor').click(function() {
        var chart = $('#bubbleChart').highcharts();
        $(this).attr('href', 'data:text/csv;charset=utf-8,'+escape(chart.getCSV()));
        $(this).attr('download', "data-visualisation.csv");
    });
});

/*

	$(function () {
                   jQuery.getJSON('/listMeteoDataJson', function(data) {
                          $.map(data, function(obj, i) {
                          selectedValueMessArt = parseInt($('#messArtSelected').val());
                          if(obj.messart === selectedValueMessArt) {
                            var dateString = obj.messdate.substring(0,10); // 18-04-2017 12:10:00"
                            var timeString = obj.messdate.substring(11,18);

                            var dateParts = dateString.split("-");
                            var timeParts = timeString.split(":");

                            var dateObject = new Date(dateParts[2], dateParts[1]-1, dateParts[0],timeParts[0],timeParts[1],timeParts[2],0);
                          					dateDistance_data.push([dateObject, obj.messwert]);
                          					}
                          				});

                          								dateDistance_data = dateDistance_data.reverse();
                          								console.log(dateDistance_data);

    $('#bubbleChart').highcharts({
        chart: {
            type: 'spline',
            zoomType: 'x'
        },

        title: {
            text: 'Meteo Data for a week'
         },
         subtitle: {
                 text: 'select to zoom in'
             },
             xAxis: {
                 type: 'datetime',
                 title: {
                     text: 'Date Time'
                 }
             },
             yAxis: {
                 title: {
                     text: 'Temperature'
                 },
                 min: -10
             },
             tooltip: {
                 headerFormat: '<b>{series.messdate}</b><br>',
                 pointFormat: '{point.x:%e. %b}: {point.y:.2f} '
             },

             plotOptions: {
                 spline: {
                     marker: {
                         enabled: true
                     }
                 }
             },

        series: []
               });
});
})
/*if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}*/

/*
Highcharts.chart('container', {
    chart: {
        type: 'spline'
    },
    title: {
        text: 'Snow depth at Vikjafjellet, Norway'
    },
    subtitle: {
        text: 'Irregular time data in Highcharts JS'
    },
    xAxis: {
        type: 'datetime',
        dateTimeLabelFormats: { // don't display the dummy year
            month: '%e. %b',
            year: '%b'
        },
        title: {
            text: 'Date'
        }
    },
    yAxis: {
        title: {
            text: 'Snow depth (m)'
        },
        min: 0
    },
    tooltip: {
        headerFormat: '<b>{series.name}</b><br>',
        pointFormat: '{point.x:%e. %b}: {point.y:.2f} m'
    },

    plotOptions: {
        spline: {
            marker: {
                enabled: true
            }
        }
    },

    series: [{
        name: 'Winter 2012-2013',
        // Define the data points. All series have a dummy year
        // of 1970/71 in order to be compared on the same x axis. Note
        // that in JavaScript, months start at 0 for January, 1 for February etc.
        data: [
            [Date.UTC(1970, 9, 21), 0],
            [Date.UTC(1970, 10, 4), 0.28],
            [Date.UTC(1970, 10, 9), 0.25],
            [Date.UTC(1970, 10, 27), 0.2],
            [Date.UTC(1970, 11, 2), 0.28],
            [Date.UTC(1970, 11, 26), 0.28],
            [Date.UTC(1970, 11, 29), 0.47],
            [Date.UTC(1971, 0, 11), 0.79],
            [Date.UTC(1971, 0, 26), 0.72],
            [Date.UTC(1971, 1, 3), 1.02],
            [Date.UTC(1971, 1, 11), 1.12],
            [Date.UTC(1971, 1, 25), 1.2],
            [Date.UTC(1971, 2, 11), 1.18],
            [Date.UTC(1971, 3, 11), 1.19],
            [Date.UTC(1971, 4, 1), 1.85],
            [Date.UTC(1971, 4, 5), 2.22],
            [Date.UTC(1971, 4, 19), 1.15],
            [Date.UTC(1971, 5, 3), 0]
        ]
    }, {
        name: 'Winter 2013-2014',
        data: [
            [Date.UTC(1970, 9, 29), 0],
            [Date.UTC(1970, 10, 9), 0.4],
            [Date.UTC(1970, 11, 1), 0.25],
            [Date.UTC(1971, 0, 1), 1.66],
            [Date.UTC(1971, 0, 10), 1.8],
            [Date.UTC(1971, 1, 19), 1.76],
            [Date.UTC(1971, 2, 25), 2.62],
            [Date.UTC(1971, 3, 19), 2.41],
            [Date.UTC(1971, 3, 30), 2.05],
            [Date.UTC(1971, 4, 14), 1.7],
            [Date.UTC(1971, 4, 24), 1.1],
            [Date.UTC(1971, 5, 10), 0]
        ]
    }, {
        name: 'Winter 2014-2015',
        data: [
            [Date.UTC(1970, 10, 25), 0],
            [Date.UTC(1970, 11, 6), 0.25],
            [Date.UTC(1970, 11, 20), 1.41],
            [Date.UTC(1970, 11, 25), 1.64],
            [Date.UTC(1971, 0, 4), 1.6],
            [Date.UTC(1971, 0, 17), 2.55],
            [Date.UTC(1971, 0, 24), 2.62],
            [Date.UTC(1971, 1, 4), 2.5],
            [Date.UTC(1971, 1, 14), 2.42],
            [Date.UTC(1971, 2, 6), 2.74],
            [Date.UTC(1971, 2, 14), 2.62],
            [Date.UTC(1971, 2, 24), 2.6],
            [Date.UTC(1971, 3, 2), 2.81],
            [Date.UTC(1971, 3, 12), 2.63],
            [Date.UTC(1971, 3, 28), 2.77],
            [Date.UTC(1971, 4, 5), 2.68],
            [Date.UTC(1971, 4, 10), 2.56],
            [Date.UTC(1971, 4, 15), 2.39],
            [Date.UTC(1971, 4, 20), 2.3],
            [Date.UTC(1971, 5, 5), 2],
            [Date.UTC(1971, 5, 10), 1.85],
            [Date.UTC(1971, 5, 15), 1.49],
            [Date.UTC(1971, 5, 23), 1.08]
        ]
    }]
});
*/
