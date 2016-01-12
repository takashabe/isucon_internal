require 'digest/sha2'
require 'mysql2'
require 'mysql2-cs-bind'

class Initializer
  def initialize(host)
    if host == nil
      host = 'localhost'
    end
    @client = Mysql2::Client.new(:host => host, :username => 'isucon', :database => 'isucon')
  end

  def init_db()
    @client.xquery('TRUNCATE TABLE user')
    @client.xquery('TRUNCATE TABLE follow')
    @client.xquery('TRUNCATE TABLE tweet')
  end

  def gen_salt(length)
    Array.new(length){[*:A..:Z, *:a..:z, *0..9].sample}.join
  end

  def gen_hash(salt, password)
    Digest::SHA256.hexdigest("#{salt}#{password}")
  end

  def register_users(user_list_path)
    File.open(user_list_path) do |file|
      file.each_line do |line|
        line.chomp!

        # salt, passhashの生成
        # サーバ側でパスワード照合のために利用する
        salt = gen_salt(16)
        hash = gen_hash(salt, line)

        # ユーザ登録
        # id, emailは適当な値を生成しておく
        latest_id = @client.xquery('SELECT id FROM user ORDER BY id DESC LIMIT 1;').first
        insert_id = if latest_id.nil? then 1 else latest_id['id'] + 1 end
        email = "#{line}@example.com"
        @client.xquery('INSERT INTO user (id, name, email, salt, passhash) VALUES (?, ?, ?, ?, ?);', insert_id, line, email, salt, hash)
      end
    end
  end

  def follow_users()
    # id1~3のユーザはBootStrapCheckerで使用するため決め打ち
    @client.xquery('INSERT INTO follow (user_id, follow_id) VALUES (1, 3), (3, 1)')

    # id4以降のユーザはランダムでfollowする
    cnt_users = @client.xquery('SELECT COUNT(0) as cnt FROM user').first
    (4..cnt_users['cnt']).each do |i|
      max_follow = Random.rand(cnt_users['cnt']).to_i
      follow_seed = (1..max_follow).to_a.shuffle
      query = 'INSERT INTO follow (user_id, follow_id) VALUES '
      (1..max_follow).each do |j|
        break if j == max_follow
        query << "(#{i}, #{follow_seed[j]}),"
      end
      if 0 < max_follow
        query.chop!
        @client.xquery(query)
      end
    end
  end

  def random_tweet(file_path)
    tweet_seed = File.open(file_path).readlines
    tweet_seed.map { |s| s.chomp! }
    seed_size = tweet_seed.size
    max_tweet = Random.rand(10000).to_i

    # ランダムにtweetさせる
    cnt_users = @client.xquery('SELECT COUNT(0) as cnt FROM user').first
    (1..cnt_users['cnt']).each do |i|
      query = 'INSERT INTO tweet (user_id, content) VALUES '
      (1..max_tweet).each do |j|
        tweet_seed.shuffle!
        query << "(#{i}, '#{tweet_seed[j % seed_size]}'),"
      end
      if 0 < max_tweet
        query.chop!
        @client.xquery(query)
      end
    end
  end
end

host = 'localhost'

init = Initializer.new(host)
init.init_db
init.register_users('user_list')
init.follow_users
init.random_tweet('tweet_list')
