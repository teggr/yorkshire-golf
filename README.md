# yorkshire-golf

Web based tracker for Yorkshire Golf

# Copilot Instructions

For consistent naming of pages/sections and guidance for the `scripts` Course Audit tool, see [.github/copilot-instructions.md](.github/copilot-instructions.md).

# Data Store

Managed as resouces:

* `courses.csv` contains the list of yorkshire courses
* `course-records.csv` contains the list of played courses. fk into the courses list

# Build

Requires:

* Java 21
* Maven

```shell
./mvnw clean package
```

# Deploy

Build the Docker image:

```shell
./mvnw clean package
```

Push to Docker Hub (ensure you are logged in with `docker login`):

```shell
./mvnw docker:push
```

Set `GOOGLE_MAPS_API_KEY` in the runtime environment to enable Google Maps embeds on course pages.

# Usage

Go to https://www.yorkshiregolf.life

# Course Audit

Run from repo root:

```shell
jbang scripts/CourseAudit.java
```

By default, closed courses are hidden.

To include closed courses for editing:

```shell
jbang scripts/CourseAudit.java --show-closed
```


# Useful References

https://www.chartjs.org/docs/latest/samples/other-charts/multi-series-pie.html
https://stackoverflow.com/questions/20966817/how-to-add-text-inside-the-doughnut-chart-using-chart-js
