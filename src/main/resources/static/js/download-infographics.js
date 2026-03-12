(function () {
  function buildExportDataUrl(sourceCanvas) {
    var rect = sourceCanvas.getBoundingClientRect();
    var baseWidth = Math.max(Math.round(rect.width || 0), sourceCanvas.width || 0, 1);
    var baseHeight = Math.max(Math.round(rect.height || 0), sourceCanvas.height || 0, 1);
    var scale = 2;

    var exportCanvas = document.createElement('canvas');
    exportCanvas.width = baseWidth * scale;
    exportCanvas.height = baseHeight * scale;

    var context = exportCanvas.getContext('2d');
    if (!context) {
      return sourceCanvas.toDataURL('image/png');
    }

    // Ensure exported chart PNGs have a solid background for sharing.
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, exportCanvas.width, exportCanvas.height);
    context.drawImage(sourceCanvas, 0, 0, exportCanvas.width, exportCanvas.height);

    return exportCanvas.toDataURL('image/png');
  }

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
        link.href = buildExportDataUrl(canvas);
        link.click();
      }, index * 500);
    });
  });
})();
