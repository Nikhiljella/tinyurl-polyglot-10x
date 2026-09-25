FROM php:8.2-cli-alpine
WORKDIR /app
RUN apk add --no-cache sqlite-dev
RUN docker-php-ext-install pdo pdo_sqlite
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer
COPY composer.json ./
RUN composer install --no-dev --optimize-autoloader
COPY . .
ENV PORT=8008
EXPOSE 8008
CMD ["php", "-S", "0.0.0.0:8008", "-t", "public", "public/index.php"]
