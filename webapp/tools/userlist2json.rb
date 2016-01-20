require 'digest/sha2'
require 'mysql2'
require 'mysql2-cs-bind'

client = Mysql2::Client.new(:host => '192.168.33.10', :username => 'isucon', :database => 'isucon')
client.xquery('SELECT * FROM user').each do |user|
  puts "{ \"id\":#{user['id']}, \"name\":\"#{user['name']}\", \"email\":\"#{user['email']}\", \"password\":\"#{user['name']}\" },"
end

# {
#     "id":1,
#     "name":"Helen",
#     "email":"Helen@example.com",
#     "password":"Helen"
# },