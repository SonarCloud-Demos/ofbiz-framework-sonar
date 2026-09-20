ARG GATEWAY_BASE_IMAGE=required.invalid/internal/nginx:unset
FROM ${GATEWAY_BASE_IMAGE}

COPY local-dev/gateway/nginx.conf /etc/nginx/nginx.conf
EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
