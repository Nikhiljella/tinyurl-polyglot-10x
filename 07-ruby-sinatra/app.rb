require 'sinatra'
require 'json'
require 'securerandom'
require 'time'

set :bind, '0.0.0.0'
set :port, ENV['PORT'] || 8007

BASE_URL = ENV['BASE_URL'] || "http://localhost:#{settings.port}"

class MemoryStore
  def initialize
    @store = {}
    @mutex = Mutex.new
  end

  def save(record)
    @mutex.synchronize do
      return false if @store.key?(record[:id])
      @store[record[:id]] = record
      true
    end
  end

  def get(id)
    @mutex.synchronize do
      @store[id]
    end
  end

  def exists?(id)
    @mutex.synchronize do
      @store.key?(id)
    end
  end

  def increment_click(id)
    @mutex.synchronize do
      record = @store[id]
      return nil unless record
      record[:click_count] += 1
      record
    end
  end
end

STORE = MemoryStore.new
BASE62 = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.freeze

def generate_slug(length = 7)
  Array.new(length) { BASE62[SecureRandom.random_number(BASE62.length)] }.join
end

before do
  content_type :json unless request.path_info == '/' || (!request.path_info.start_with?('/api') && request.get?)
end

get '/api/health' do
  content_type :json
  { status: 'ok', stack: '07-ruby-sinatra' }.to_json
end

post '/api/shorten' do
  content_type :json
  begin
    body = JSON.parse(request.body.read)
  rescue StandardError
    halt 400, { error: 'Invalid JSON' }.to_json
  end

  url = body['url']
  if url.nil? || (!url.start_with?('http://') && !url.start_with?('https://'))
    halt 400, { error: 'Invalid URL scheme: must start with http:// or https://' }.to_json
  end

  custom_alias = body['custom_alias']&.strip

  slug = nil
  if custom_alias && !custom_alias.empty?
    if STORE.exists?(custom_alias)
      halt 400, { error: 'Custom alias already exists' }.to_json
    end
    slug = custom_alias
  else
    5.times do
      cand = generate_slug(7)
      unless STORE.exists?(cand)
        slug = cand
        break
      end
    end
    halt 500, { error: 'Failed to generate unique slug' }.to_json if slug.nil?
  end

  record = {
    id: slug,
    original_url: url,
    short_url: "#{BASE_URL}/#{slug}",
    click_count: 0,
    created_at: Time.now.utc.iso8601
  }

  STORE.save(record)
  status 201
  record.to_json
end

get '/api/stats/:id' do
  content_type :json
  record = STORE.get(params[:id])
  halt 404, { error: 'URL not found' }.to_json if record.nil?
  record.to_json
end

get '/:id' do
  id = params[:id]
  pass if id.start_with?('api')

  record = STORE.increment_click(id)
  if record
    redirect record[:original_url], 302
  else
    content_type :json
    halt 404, { error: 'URL not found' }.to_json
  end
end
