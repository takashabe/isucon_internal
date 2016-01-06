require 'sinatra/base'
require 'mysql2'
require 'mysql2-cs-bind'
require 'tilt/erubis'
require 'erubis'

require 'sinatra/reloader'
require 'logger'

module Isucon
  class AuthenticationError < StandardError; end
  class PermissionDenied < StandardError; end
  class ContentNotFound < StandardError; end
  module TimeWithoutZone
    def to_s
      strftime("%F %H:%M:%S")
    end
  end
  ::Time.prepend TimeWithoutZone
end

class Isucon::WebApp < Sinatra::Base
  use Rack::Session::Cookie
  set :erb, escape_html: true
  set :public_folder, File.expand_path('../../static', __FILE__)
  set :session_secret, ENV['ISUCON5_SESSION_SECRET'] || 'beermoris'
  set :protection, true

  configure :development do
    register Sinatra::Reloader
  end
  logger = Logger.new('/tmp/ruby.log')

  helpers do
    def config
      @config ||= {
        db: {
          host: ENV['ISUCON5_DB_HOST'] || 'localhost',
          port: ENV['ISUCON5_DB_PORT'] && ENV['ISUCON5_DB_PORT'].to_i,
          username: ENV['ISUCON5_DB_USER'] || 'isucon',
          password: ENV['ISUCON5_DB_PASSWORD'],
          database: ENV['ISUCON5_DB_NAME'] || 'isucon',
        },
      }
    end

    def db
      return Thread.current[:isucon5_db] if Thread.current[:isucon5_db]
      client = Mysql2::Client.new(
        host: config[:db][:host],
        port: config[:db][:port],
        username: config[:db][:username],
        password: config[:db][:password],
        database: config[:db][:database],
        reconnect: true,
      )
      client.query_options.merge!(symbolize_keys: true)
      Thread.current[:isucon5_db] = client
      client
    end

    def authenticate(email, password)
      query = <<SQL
SELECT *
FROM user
WHERE email = ? AND passhash = SHA2(CONCAT(salt, ?), 256)
SQL
      result = db.xquery(query, email, password).first
      unless result
        raise Isucon::AuthenticationError
      end
      session[:user_id] = result[:id]
      result
    end

    def current_user
      return @user if @user
      unless session[:user_id]
        return nil
      end
      @user = db.xquery('SELECT * FROM user WHERE id=?', session[:user_id]).first
      unless @user
        session[:user_id] = nil
        session.clear
        raise Isucon::AuthenticationError
      end
      @user
    end

    def authenticated!
      unless current_user
        redirect '/login'
      end
    end

    def get_user(user_id)
      user = db.xquery('SELECT * FROM user WHERE id = ?', user_id).first
      raise Isucon::ContentNotFound unless user
      user
    end

    def user_from_account(account_name)
      user = db.xquery('SELECT * FROM users WHERE account_name = ?', account_name).first
      raise Isucon::ContentNotFound unless user
      user
    end

    def is_friend?(another_id)
      user_id = session[:user_id]
      query = 'SELECT COUNT(1) AS cnt FROM relations WHERE (one = ? AND another = ?) OR (one = ? AND another = ?)'
      cnt = db.xquery(query, user_id, another_id, another_id, user_id).first[:cnt]
      cnt.to_i > 0 ? true : false
    end

    def is_follow?(follow_id)
      user_id = session[:user_id]
      query = 'SELECT COUNT(1) AS cnt FROM follow WHERE user_id = ? AND follow_id = ?'
      cnt = db.xquery(query, user_id, follow_id).first[:cnt]
      cnt.to_i > 0 ? true : false
    end

    def permitted?(another_id)
      another_id == current_user[:id] || is_friend?(another_id)
    end
  end

  error Isucon::AuthenticationError do
    session[:user_id] = nil
    halt 401, erubis(:login, layout: false, locals: { message: 'ログインに失敗しました' })
  end

  error Isucon::PermissionDenied do
    halt 403, erubis(:error, locals: { message: '友人のみしかアクセスできません' })
  end

  error Isucon::ContentNotFound do
    halt 404, erubis(:error, locals: { message: '要求されたコンテンツは存在しません' })
  end

  get '/login' do
    session.clear
    erb :login, layout: false, locals: { message: '高負荷に耐えられるSNSコミュニティサイトへようこそ!' }
  end

  post '/login' do
    authenticate params['email'], params['password']
    redirect '/'
  end

  get '/logout' do
    session[:user_id] = nil
    session.clear
    redirect '/login'
  end

  get '/' do
    authenticated!
    tweets = []
    db.xquery('SELECT * FROM tweet ORDER BY created_at DESC').each do |row|
      next unless is_follow?(row[:user_id]) || current_user[:id] == row[:user_id]
      tweets << row
      break if tweets.size >= 100
    end

    following = db.xquery('SELECT * FROM follow WHERE user_id = ?', current_user[:id])
    followers = db.xquery('SELECT * FROM follow WHERE follow_id = ?', current_user[:id])

    erb :timeline, locals: { tweets: tweets, following: following, followers: followers }
  end

  get '/tweet' do
    authenticated!

    erb :tweet
  end

  post '/tweet' do
    authenticated!
    query = 'INSERT INTO tweet (user_id, content) VALUES (?,?)'
    db.xquery(query, current_user[:id], params['content'])
    redirect "/"
  end

  get '/user/:user_id' do
    user = db.xquery('SELECT * FROM user WHERE id = ?', params['user_id']).first
    erb :user, locals: { user: user, myself: current_user}
  end

  get '/following' do
    authenticated!
    following = db.xquery('SELECT * FROM follow WHERE user_id = ?', current_user[:id])
    following_users = []
    following.each do |f|
      user = db.xquery('SELECT * FROM user WHERE id = ?', f[:follow_id]).first

      following_users << user
    end

    erb :following, locals: { following: following_users }
  end

  post '/follow/:user_id' do
    authenticated!
    db.xquery('INSERT INTO follow (user_id, follow_id) VALUES (?, ?)', current_user[:id], params['user_id'])
    redirect "/"
  end

  get '/followers' do
    authenticated!
    followers = db.xquery('SELECT * FROM follow WHERE follow_id = ?', current_user[:id])
    followers_users = []
    followers.each do |f|
      user = db.xquery('SELECT * FROM user WHERE id = ?', f[:user_id]).first

      followers_users << user
    end

    erb :followers, locals: { followers: followers_users }
  end

  get '/initialize' do
    # TODO: DB初期化スクリプトを叩く
  end
end
