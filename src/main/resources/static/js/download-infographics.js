(function () {
  var btn = document.getElementById('ygl-download-infographics');
  if (!btn) {
    return;
  }

  btn.addEventListener('click', function () {
    // myChart = progress doughnut chart, progressLineChart = timeline chart
    var charts = [
      { id: 'myChart', filename: 'yorkshire-challenge-progress.png' },
      { id: 'progressLineChart', filename: 'yorkshire-challenge-timeline.png' }
    ];

    charts.forEach(function (chart, index) {
      var canvas = document.getElementById(chart.id);
      if (!canvas) {
        return;
      }
      // Stagger downloads slightly so browsers don't block them as spam
      setTimeout(function () {
        var link = document.createElement('a');
        link.download = chart.filename;
        link.href = canvas.toDataURL('image/png');
        link.click();
      }, index * 500);
    });
  });
})();
