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

## Automated Deployment (Recommended)

Use GitHub Actions workflows for automated release and deployment:

1. Go to the **Actions** tab in GitHub
2. Select **"Maven Release & Deploy via deploy4j"** workflow
3. Click **"Run workflow"**
4. Enter the release version and deployment target (staging/prod)

See [GitHub Actions Workflows Documentation](.github/workflows/README.md) for detailed setup instructions.

## Manual Deployment

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
