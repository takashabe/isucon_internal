% rebase("layout")
<h2>タイムライン</h2>
<div class="row panel panel-primary" id="timeline">
  <div class="col-md-4">
    <dl>
      <dt>name</dt><dd id="prof-name">{{user["name"]}}</dd>
      <dt>email</dt><dd id="prof-email">{{user["email"]}}</dd>
      <dt>following</dt><dd id="prof-following"><a href="/following">{{len(following)}}</a></dd>
      <dt>followers</dt><dd id="prof-followers"><a href="/followers">{{len(followers)}}</a></dd>
    </dl>
  </div>

  % for tweet in tweets:
  <div class="tweet">
    % tweet_user = get_user(tweet["user_id"])
    <div class="user">
      <a href="/user/{{tweet_user["id"]}}">{{tweet_user["name"]}}</a>
    </div>
    <div class="tweet">
      % for line in tweet["content"].split("\n"):
        {{line}}<br />
      % end
    </div>
    <div class="comment-created-at">投稿時刻:{{tweet['created_at']}}</div>
  </div>
  % end
</div>
