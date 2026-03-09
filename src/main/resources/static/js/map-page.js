(function () {
  function buildMap() {
    const mapElement = document.getElementById("course-map");
    if (!mapElement) {
      return;
    }

    const config = window.yglCourseMapConfig || {};
    const mapPoints = Array.isArray(config.mapPoints) ? config.mapPoints : [];
    if (!window.google || !window.google.maps || mapPoints.length === 0) {
      return;
    }

    const map = new google.maps.Map(mapElement, {
      center: { lat: 53.9, lng: -1.2 },
      zoom: 8,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: true
    });

    const bounds = new google.maps.LatLngBounds();
    const infoWindow = new google.maps.InfoWindow();

    mapPoints.forEach(function (point) {
      if (typeof point.lat !== "number" || typeof point.lng !== "number") {
        return;
      }

      const position = { lat: point.lat, lng: point.lng };
      const marker = new google.maps.Marker({
        position: position,
        map: map,
        title: point.name || "Course"
      });

      bounds.extend(position);

      marker.addListener("click", function () {
        const content = document.createElement("div");

        const name = document.createElement("div");
        name.textContent = point.name || "Course";
        content.appendChild(name);

        if (typeof point.coursePath === "string" && point.coursePath.startsWith("/courses/")) {
          const link = document.createElement("a");
          link.href = point.coursePath;
          link.textContent = "View course page";
          link.className = "ygl-map-page__popup-link";
          content.appendChild(link);
        }

        infoWindow.setContent(content);
        infoWindow.open({
          map: map,
          anchor: marker
        });
      });
    });

    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, 48);
    }
  }

  window.yglInitCourseMap = buildMap;
})();
