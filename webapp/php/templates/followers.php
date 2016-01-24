<h2>Followers List</h2>
<div class="row panel panel-primary" id="followers">
  <dl>
    <?php foreach ($followers as $f) { ?>
      <dt class="follow-date">
        <?php h($f['created_at']) ?>
      </dt>
      <dd class="follow-follow">
        <a href="/user/<?php h($f['id']) ?>"><?php h($f['name']) ?></a>
      </dd>
    <?php } ?>
  </dl>
</div>
