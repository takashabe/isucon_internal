<h2><?php h($user['name']) ?>さんのプロフィール</h2>
<div class="row" id="prof">
  <dl class="panel panel-primary">
    <dt>Name</dt><dd id="prof-name"><?php h($user['name']) ?></dd>
    <dt>Email</dt><dd id="prof-email"><?php h($user['email']) ?></dd>
  </dl>
</div>
<?php if ($myself['id'] != $user['id'] && !is_follow($user['id'])) { ?>
  <form id="follow-form" method="POST" action="/follow/<?php h($user['id']) ?>">
    <input type="hidden" name="self_user_id" value="<?php $myself['id'] ?>">
    <input type="submit" class="btn btn-default" value="フォローする" />
  </form>
<?php } ?>
<?php foreach ($tweets as $tweet) { ?>
<div class="tweet">
  <?php $tweet_user = get_user($tweet['user_id']) ?>
  <div class="user">
    <a href="/user/<?php h($tweet_user['id']) ?>"><?php h($tweet_user['name']) ?></a>
  </div>
  <div class="tweet">
    <?php foreach (preg_split('/\n/', $tweet['content']) as $line) { ?>
    <?php h($line) ?><br />
    <?php } ?>
  </div>
  <div class="friend-date">投稿時刻:<?php h($tweet['created_at']) ?></div>
</div>
<?php } ?>
