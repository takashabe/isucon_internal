% rebase("layout")
<h2>{{user['name']}}さんのプロフィール</h2>
<div class="row" id="prof">
  <dl class="panel panel-primary">
    <dt>Name</dt><dd id="prof-name">{{user['name']}}</dd>
    <dt>Email</dt><dd id="prof-email">{{user['email']}}</dd>
  </dl>
</div>
% if myself['id'] != user['id'] and not is_follow(user['id']):
<form id="follow-form" method="POST" action="/follow/{{user['id']}}">
  <input type="hidden" name="self_user_id" value="{{myself['id']}}">
  <input type="submit" class="btn btn-default" value="フォローする" />
</form>
% end

% for tweet in tweets:
<div class="tweet">
  % tweet_user = get_user(tweet['user_id'])
  <div class="user">
    <a href="/user/{{tweet_user['id']}}">{{tweet_user['name']}}</a>
  </div>
  <div class="tweet">
    % for line in tweet["content"].split("\n"):
      {{line}}<br />
    % end
  </div>
  <div class="comment-created-at">投稿時刻:{{tweet['created_at']}}</div>
</div>
% end
