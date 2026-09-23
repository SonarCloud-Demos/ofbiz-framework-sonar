ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
COPY web/shell/nginx.conf /etc/nginx/conf.d/default.conf
COPY web/shell/index.html web/shell/route-manifest.json /usr/share/nginx/html/
COPY web/shell/assets/ /usr/share/nginx/html/modern/assets/
USER 101
EXPOSE 8080
