#!/bin/sh
set -e

PROPS=/app/application.properties
if [ ! -f "$PROPS" ]; then
  cat > "$PROPS" <<EOF
spring.application.name=ShadowVibe
spring.datasource.url=jdbc:postgresql://\${DB_HOST:\${PGHOST:localhost}}:\${DB_PORT:\${PGPORT:5432}}/\${DB_NAME:\${PGDATABASE:shadowvibe}}
spring.datasource.username=\${DB_USER:\${PGUSER:postgres}}
spring.datasource.password=\${DB_PASSWORD:\${PGPASSWORD:}}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=\${SHOW_SQL:false}
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
server.port=\${PORT:8080}
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
app.upload.dir=\${UPLOAD_DIR:uploads}
app.admin.username=\${ADMIN_USERNAME:admin}
app.admin.password=\${ADMIN_PASSWORD:}
app.admin.email=\${ADMIN_EMAIL:admin@example.com}
vk.access-token=\${VK_ACCESS_TOKEN:}
app.push.vapid.public-key=\${VAPID_PUBLIC_KEY:}
app.push.vapid.private-key=\${VAPID_PRIVATE_KEY:}
app.push.vapid.subject=\${VAPID_SUBJECT:mailto:admin@shadowvibe.local}
EOF
  echo "Generated /app/application.properties from environment"
fi

exec java -XX:MaxRAMPercentage=40.0 -jar /app/app.jar