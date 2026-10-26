ARG TOOLCHAIN=usermanager03-toolchain:7e663ce2-43fc-488e-a1ee-d1a44950d594
FROM ${TOOLCHAIN} AS build
WORKDIR /workspace
COPY api api
COPY frontend frontend
COPY platform/frontend platform/frontend
RUN npm --prefix frontend ci && npm --prefix frontend run generate:unicode && npm --prefix frontend test && npm --prefix frontend run build
COPY backend backend
COPY platform/openapi platform/openapi
COPY platform/backend platform/backend
RUN python3 platform/backend/generate-unicode.py && mkdir -p backend/src/main/resources/static && cp -r frontend/dist/. backend/src/main/resources/static/ && mvn -B -ntp -f backend/pom.xml -DskipTests package
FROM ubuntu:24.04@sha256:18b33ff3829707f31ada654a963b9ee35f2e6f0241b964bd17a22a9b330cb63d
ARG RUN_ID
LABEL com.hariklabs.project=usermanager03 com.hariklabs.lifecycle=permanent com.hariklabs.run=${RUN_ID}
RUN apt-get update && apt-get install --no-install-recommends -y ca-certificates curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /opt/java /opt/java
COPY --from=build /workspace/backend/target/usermanager03-0.1.0.jar /app/application.jar
USER 1001:1001
WORKDIR /app
ENV PATH="/opt/java/bin:${PATH}"
EXPOSE 8443
ENTRYPOINT ["java","-jar","/app/application.jar"]
