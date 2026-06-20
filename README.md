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

# Release

```shell
./mvnw versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false

./mvnw clean package docker:push
```

# Deploy

Build the Docker image:

```shell
deploy4j deploy --version=0.0.13
```

# Post-Deploy

```shell
./mvnw versions:set -DnextSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```

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
