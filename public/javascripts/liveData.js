$(function () {
    $('#liveChart').highcharts({
        chart: {
            type: 'spline',
            animation: Highcharts.svg, // don't animate in old IE
            marginRight: 10,
            events: {
                load: function () {

                   // set up the updating of the chart each second
                    var series = this.series[0];
                    setInterval(function () {
                        var x = (new Date()).getTime(), // current time
                        y = Math.random();
                        series.addPoint([x, y], true, true);
                    }, 1000);
                    // set up the updating of the chart each second
                  /*  var series = this.series[0];
                    setInterval(function () {
                    selectedStation = $('#selectStationId option:selected').val();
                    selectedValueMessArt = $('#messArtSelected option:selected').val();

                    selectedStationName = $('#selectStationId option:selected').text();
                    selectedTextMessArt = $('#messArtSelected option:selected').text();

                    jQuery.getJSON('/listLatestMeteoDataJson/' + parseInt(selectedStation) + "/" + parseInt(selectedValueMessArt), function(data) {


                                 console.log('Station Number is:' + selectedStation)
                                           $.map(data, function(obj, i) {
                                           selectedValueMessArt = parseInt($('#messArtSelected').val());
                                           if(obj.messart === selectedValueMessArt) {
                                                var dateString = obj.messdate.substring(0,10); // 18-04-2017 12:10:00"
                                                var timeString = obj.messdate.substring(11,18);

                                                var dateParts = dateString.split("-");
                                                var timeParts = timeString.split(":");

                                                var dateObject = new Date(dateParts[2], dateParts[1]-1, dateParts[0],timeParts[0],timeParts[1],timeParts[2],0);
                                                                            				console.log(obj.messdate, obj.messwert);

                                                                        series.addPoint([obj.messdate, obj.messwert], true, true);
                                                }
                                            });

                    })}, 1000);*/
                }
            }
        },
        title: {
            text: 'Live data'
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 150
        },
        yAxis: {
            title: {
                text: 'Value'
            },
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        tooltip: {
            formatter: function () {
                return '<b>' + this.series.name + '</b><br/>' +
                    Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) + '<br/>' +
                    Highcharts.numberFormat(this.y, 2);
            }
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        series: [{
            name: 'Random data',
            data: (function () {
                // generate an array of random data
                var data = [],
                    time = (new Date()).getTime(),
                    i;

                for (i = -19; i <= 0; i += 1) {
                    data.push({
                        x: time + i * 1000,
                        y: Math.random()
                    });
                }
                return data;
            }())
        }]
    });
});