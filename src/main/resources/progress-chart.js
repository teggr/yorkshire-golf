Chart.register(ChartDataLabels);

var ctx = document.getElementById("myChart").getContext('2d');

new Chart(ctx, {
    plugins: [ChartDataLabels],
    type: 'doughnut',
    data: {
        datasets: [
            {
                data: JSON.parse( 'overall_progress' ),
                overall: true,
                coursesPlayed: JSON.parse( 'total_played' ),
                coursesTotal: JSON.parse( 'total_course_count' ),
                visible: [true, false],
                backgroundColor: [
                    '#264653',
                    '#2a9d8f',
                ],
                label: 'Overall',
                weight: 0.6,            },
            {
                data: [1, 1, 1, 1],
                overall: false,
                region: [ 'North', 'East', 'South', 'West' ],
                coursesPlayed: JSON.parse( 'region_played' ),
                coursesTotal: JSON.parse( 'region_course_count' ),
                backgroundColor: [
                    '#8ab17d',
                    '#e9c46a',
                    '#f4a261',
                    '#e76f51',
                ],
                label: 'Regions',
                weight: 0.4,
                rotation: -45
            },
        ],
    },
    options: {
        plugins: {
            title: {
                display: true,
                text: '#yorkshiregolfchallenge',
            },
            legend :{
                display: false
            },
            doughnutLabel: {
                labels: [
                    {
                        text: JSON.parse('overall_percentage') + '%',
                        font: {
                            size: '20',
                            weight: 'bold'
                        },
                        color: '#264653'
                    },
                    {
                        text: 'Yorkshire courses',
                        font: {
                            size: '15'
                        },
                        color: '#264653'
                    },
                    {
                        text: 'played',
                        font: {
                            size: '15'
                        },
                        color: '#264653'
                    },
                ]
            },
            datalabels: {
                backgroundColor: function(context) {
                    return context.dataset.backgroundColor;
                },
                borderColor: 'white',
                borderRadius: 25,
                borderWidth: 2,
                color: 'white',
                display: function(context) {
                    if(context.dataset.overall && context.dataIndex !== 0) {
                        return false;
                    }
                    return true;
                },
                font: {
                    weight: 'bold'
                },
                padding: 6,
                formatter: function(value, context) {
                    if( context.dataset.overall ) {
                        return 'Overall: ' + context.dataset.coursesPlayed[context.dataIndex] + ' / ' + context.dataset.coursesTotal[context.dataIndex];
                    } else {
                        return context.dataset.region[context.dataIndex] +  ': ' + context.dataset.coursesPlayed[context.dataIndex] + ' / ' + context.dataset.coursesTotal[context.dataIndex];
                    }
                }
            },
            tooltip: {
                enabled: false
            }
        }
    },
});
