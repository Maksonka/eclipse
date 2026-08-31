# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

# Run stage: JRE + Python runtime for voice transcription (faster-whisper)
FROM eclipse-temurin:25-jre
WORKDIR /app

# Python utility environment
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir faster-whisper

# Transcription script + model cache predownload (cache model at build time)
ARG WHISPER_MODEL=Systran/faster-whisper-small
ENV WHISPER_MODEL=${WHISPER_MODEL}
COPY whisper/whisper_local.py /app/whisper/whisper_local.py
RUN /opt/venv/bin/python -c "from faster_whisper import WhisperModel; import os; WhisperModel(os.environ['WHISPER_MODEL'], device='cpu', compute_type='int8', download_root='/app/whisper/models-ct2'); print('model cached: ' + os.environ['WHISPER_MODEL'])"

# Application
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8080
ENV PORT=8080
ENV WHISPER_PYTHON=/opt/venv/bin/python
ENV WHISPER_SCRIPT=/app/whisper/whisper_local.py
ENV WHISPER_MODEL=Systran/faster-whisper-small

ENTRYPOINT ["/app/docker-entrypoint.sh"]