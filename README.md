# yorkshire-golf

Web based tracker for Yorkshire Golf

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

# Usage

Go to https://www.yorkshiregolf.life


# Useful References

https://www.chartjs.org/docs/latest/samples/other-charts/multi-series-pie.html
https://stackoverflow.com/questions/20966817/how-to-add-text-inside-the-doughnut-chart-using-chart-js
