ARG NODE_BUILD_IMAGE=required.invalid/internal/node:unset
ARG LEGACY_OFBIZ_IMAGE=required.invalid/internal/ofbiz:unset

FROM ${NODE_BUILD_IMAGE} AS theme-assets

WORKDIR /assets
COPY themes/common-theme/webapp/common-theme/js/package.json ./
COPY themes/common-theme/webapp/common-theme/js/package-lock.json ./

RUN --mount=type=secret,id=npmrc,target=/root/.npmrc,required=true \
    --mount=type=secret,id=corporate_ca,target=/run/secrets/corporate-ca.pem,required=true \
    NODE_EXTRA_CA_CERTS=/run/secrets/corporate-ca.pem \
    npm ci --ignore-scripts --legacy-peer-deps

FROM ${LEGACY_OFBIZ_IMAGE}

COPY --from=theme-assets /assets/node_modules \
    /ofbiz/themes/common-theme/webapp/common-theme/js/node_modules

USER ofbiz
