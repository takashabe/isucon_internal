require 'digest/sha2'
require 'mysql2'
require 'mysql2-cs-bind'

def gen_salt(length)
  Array.new(length){[*:A..:Z, *:a..:z, *0..9].sample}.join
end

def gen_hash(salt, password)
  Digest::SHA256.hexdigest("#{salt}#{password}")
end

def gen_client()
  Mysql2::Client.new(:host => '192.168.33.10', :username => 'isucon', :database => 'isucon')
end

name = ARGV[0]
pass = ARGV[1]

if name.nil? or pass.nil?
  raise "require name and pass. (e.g. ./register_user.rb <name> <pass>)"
end

// salt, passhashの生成
// サーバ側でパスワード照合のために利用する
salt = gen_salt(16)
hash = gen_hash(salt, pass)

client = gen_client()

// ユーザ登録
// id, emailは適当な値を生成しておく
latest_id = client.xquery('SELECT id FROM user ORDER BY id DESC LIMIT 1;').first
email = "#{name}@example.com"
client.xquery('INSERT INTO user (id, name, email, salt, passhash) VALUES (?, ?, ?, ?, ?);', latest_id['id']+1, name, email, salt, hash)
