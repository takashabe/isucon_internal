require 'mysql2'
require 'mysql2-cs-bind'

class Initializer
  def gen_client(host)
    Mysql2::Client.new(:host => host, :username => 'isucon', :database => 'isucon')
  end

  def register_users(user_list_path, host)
    File.open(user_list_path) do |file|
      file.read.split('\n').each do |line|
        `ruby ./register_user.rb #{line} #{line} #{host}`
      end
    end
  end

  def follow_users()
    # id 1~3のユーザはBootStrapCheckerで使用するため決め打ち

    # ランダムでfollowする
  end

  def random_tweet()

  end
end
