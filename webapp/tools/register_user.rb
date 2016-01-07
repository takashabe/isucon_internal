require 'digest/sha2'
require 'mysql2'
require 'mysql2-cs-bind'

def gen_salt(length)
  Array.new(length){[*:A..:Z, *:a..:z, *0..9].sample}.join
end

def gen_hash(salt, password)
  Digest::SHA256.hexdigest("#{salt}#{password}")
end

def gen_client(host)
  Mysql2::Client.new(:host => host, :username => 'isucon', :database => 'isucon')
end

name = ARGV[0]
pass = ARGV[1]
host = if ARGV[2].nil? then 'localhost' else ARGV[2] end

if name.nil? or pass.nil?
  raise "require name and pass. (e.g. ./register_user.rb <name> <pass>)"
end

# salt, passhashの生成
# サーバ側でパスワード照合のために利用する
salt = gen_salt(16)
hash = gen_hash(salt, pass)

client = gen_client(host)

# ユーザ登録
# id, emailは適当な値を生成しておく
latest_id = client.xquery('SELECT id FROM user ORDER BY id DESC LIMIT 1;').first
insert_id = if latest_id.nil? then 1 else latest_id['id'] + 1 end
email = "#{name}@example.com"
client.xquery('INSERT INTO user (id, name, email, salt, passhash) VALUES (?, ?, ?, ?, ?);', insert_id, name, email, salt, hash)
