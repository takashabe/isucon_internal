% rebase("layout")
<h2>Followers list</h2>
<div class="row panel panel-primary" id="followers">
  <dl>
    % for f in followers:
    <dt class="follow-date">{{f['created_at']}}</dt><dd class="follow-follow"><a href="/user/{{f['id']}}">{{f['name']}}</a></dd>
    % end
  </dl>
</div>
