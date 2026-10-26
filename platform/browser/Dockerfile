FROM usermanager03-toolchain:7e663ce2-43fc-488e-a1ee-d1a44950d594
ARG RUN_ID
LABEL com.hariklabs.project=usermanager03 com.hariklabs.lifecycle=temporary com.hariklabs.run=${RUN_ID}
WORKDIR /opt/browser
COPY platform/browser/package.json platform/browser/package-lock.json ./
RUN npm ci && npx playwright install --with-deps chromium && apt-get install --no-install-recommends -y libnss3-tools && rm -rf /var/lib/apt/lists/*
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
RUN mv /root/.cache/ms-playwright /opt/browsers && chmod -R a+rX /opt/browsers
ENV PLAYWRIGHT_BROWSERS_PATH=/opt/browsers
WORKDIR /workspace
USER 1001:1001
