# syntax=docker/dockerfile:1

# The web tier has no build step: the page is plain HTML, CSS and JavaScript.
# nginx serves those files and proxies /api/ to the backend service, so this
# image only needs the static assets and one configuration file.

FROM nginx:1.27-alpine

# Replace the default site. It contains the reverse proxy rule that points at
# http://backend:8000, where "backend" is resolved by Docker's internal DNS.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Static assets, served from the root declared in nginx.conf.
COPY index.html styles.css app.js /usr/share/nginx/html/

# Documentation only: compose publishes 8080 on the host and forwards it here.
EXPOSE 80
