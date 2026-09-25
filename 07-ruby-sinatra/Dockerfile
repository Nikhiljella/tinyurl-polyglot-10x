FROM ruby:3.2-slim
WORKDIR /app
RUN apt-get update && apt-get install -y build-essential && rm -rf /var/lib/apt/lists/*
COPY Gemfile ./
RUN bundle install
COPY . .
ENV PORT=8007
EXPOSE 8007
CMD ["bundle", "exec", "ruby", "app.rb"]
