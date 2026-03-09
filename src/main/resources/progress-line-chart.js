var ctx = document.getElementById("myChart").getContext('2d');

new Chart(ctx, {
    type: 'line',
    data: {
        labels: JSON.parse('timeline_labels'),
        datasets: [{
            label: 'Courses played',
            data: JSON.parse('timeline_values'),
            borderColor: '#264653',
            borderWidth: 5,
            backgroundColor: 'rgba(42, 157, 143, 0.35)',
            pointRadius: 0,
            pointHoverRadius: 0,
            fill: true,
            tension: 0.25
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: true,
        aspectRatio: 1,
        plugins: {
            legend: {
                display: false
            },
            datalabels: {
                display: false
            },
            title: {
                display: true,
                text: 'Progress Over Time'
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return 'Courses played: ' + context.parsed.y;
                    }
                }
            }
        },
        scales: {
            x: {
                title: {
                    display: true,
                    text: 'Month',
                    color: '#264653'
                },
                ticks: {
                    color: '#264653',
                    autoSkip: true,
                    maxTicksLimit: 8,
                    maxRotation: 45,
                    minRotation: 45
                },
                grid: {
                    display: false
                }
            },
            y: {
                beginAtZero: true,
                max: JSON.parse('timeline_max'),
                title: {
                    display: true,
                    text: 'Total courses played',
                    color: '#264653'
                },
                ticks: {
                    color: '#264653',
                    precision: 0
                },
                grid: {
                    display: false
                }
            }
        }
    }
});
