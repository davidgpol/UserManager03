FROM ubuntu:24.04@sha256:18b33ff3829707f31ada654a963b9ee35f2e6f0241b964bd17a22a9b330cb63d

ARG RUN_ID
LABEL com.hariklabs.project="usermanager03" \
      com.hariklabs.lifecycle="temporary" \
      com.hariklabs.run="${RUN_ID}"

RUN apt-get update && apt-get install --no-install-recommends -y \
      ca-certificates curl python3 xz-utils git \
    && rm -rf /var/lib/apt/lists/*
COPY platform/toolchain/install.py platform/toolchain/distributions.json /opt/bootstrap/
RUN python3 /opt/bootstrap/install.py
ENV JAVA_HOME=/opt/java
ENV PATH="/opt/java/bin:/opt/maven/bin:/opt/node/bin:${PATH}"
RUN java -version && mvn -version && node --version && npm --version \
    && java -jar /opt/openapi-generator.jar version
WORKDIR /workspace
CMD ["bash"]
