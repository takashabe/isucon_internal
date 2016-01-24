% rebase("layout")
<h2>Following list</h2>
<div class="row panel panel-primary" id="following">
  <dl>
    % for f in following:
    <dt class="follow-date">{{f['created_at']}}</dt><dd class="follow-follow"><a href="/user/{{f['id']}}">{{f['name']}}</a></dd>
    % end
  </dl>
</div>
