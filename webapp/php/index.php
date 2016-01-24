<?php
require 'vendor/autoload.php';

date_default_timezone_set('Asia/Tokyo');
mb_internal_encoding('UTF-8');

class Isucon5View extends \Slim\View
{
    protected $layout = 'layout.php';

    public function setLayout($layout)
    {
        $this->layout = $layout;
    }

    public function render($template, $data = NULL)
    {
        if ($this->layout) {
            $_html = parent::render($template);
            $this->set('_html', $_html);
            $template = $this->layout;
            $this->layout = null;
        }
        return parent::render($template);
    }
}

$app = new \Slim\Slim(array(
    'view' => new Isucon5View(),
    'db' => array(
        'host' => getenv('ISUCON5_DB_HOST') ?: 'localhost',
        'port' => (int)getenv('ISUCON5_DB_PORT') ?: 3306,
        'username' => getenv('ISUCON5_DB_USER') ?: 'isucon',
        'password' => getenv('ISUCON5_DB_PASSWORD'),
        'database' => getenv('ISUCON5_DB_NAME') ?: 'isucon'
    ),
    'cookies.encrypt' => true,
));

$app->add(new \Slim\Middleware\SessionCookie(array(
    'secret' => getenv('ISUCON5_SESSION_SECRET') ?: 'beermoris',
    'expires' => 0,
)));

function debug_logging($message)
{
    error_log($message."\n", 3, '/tmp/php.log');
}

function abort_authentication_error()
{
    global $app;
    $_SESSION['user_id'] = null;
    $app->view->setLayout(null);
    $app->render('login.php', array('message' => 'ログインに失敗しました'), 401);
    $app->stop();
}

function abort_permission_denied()
{
    global $app;
    $app->render('error.php', array('message' => '友人のみしかアクセスできません'), 403);
    $app->stop();
}

function abort_content_not_found()
{
    global $app;
    $app->render('error.php', array('message' => '要求されたコンテンツは存在しません'), 404);
    $app->stop();
}

function h($string)
{
    echo htmlspecialchars($string, ENT_QUOTES, 'UTF-8');
}

function db()
{
    global $app;
    static $db;
    if (!$db) {
        $config = $app->config('db');
        $dsn = sprintf("mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4", $config['host'], $config['port'], $config['database']);
        if ($config['host'] === 'localhost') $dsn .= ";unix_socket=/var/lib/mysql/mysql.sock";
        $options = array(
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        );
        $db = new PDO($dsn, $config['username'], $config['password'], $options);
    }
    return $db;
}

function db_execute($query, $args = array())
{
    $stmt = db()->prepare($query);
    $stmt->execute($args);
    return $stmt;
}

function authenticate($email, $password)
{
    $query = <<<SQL
SELECT *
FROM user
WHERE email = ? AND passhash = SHA2(CONCAT(salt, ?), 256)
SQL;
    $result = db_execute($query, array($email, $password))->fetch();
    if (!$result) {
        abort_authentication_error();
    }
    $_SESSION['user_id'] = $result['id'];
    return $result;
}

function current_user()
{
    static $user;
    if ($user) return $user;
    if (!isset($_SESSION['user_id'])) return null;
    $user = db_execute('SELECT * FROM user WHERE id=?', array($_SESSION['user_id']))->fetch();
    if (!$user) {
        $_SESSION['user_id'] = null;
        abort_authentication_error();
    }
    return $user;
}

function authenticated()
{
    global $app;
    if (!current_user()) {
        $app->redirect('/login');
    }
}

function get_user($user_id)
{
    $user = db_execute('SELECT * FROM user WHERE id = ?', array($user_id))->fetch();
    if (!$user) abort_content_not_found();
    return $user;
}

function user_from_account($account_name)
{
    $user = db_execute('SELECT * FROM user WHERE account_name = ?', array($account_name))->fetch();
    if (!$user) abort_content_not_found();
    return $user;
}

function is_friend($another_id)
{
    $user_id = $_SESSION['user_id'];
    $query = 'SELECT COUNT(1) AS cnt FROM relations WHERE (one = ? AND another = ?) OR (one = ? AND another = ?)';
    $cnt = db_execute($query, array($user_id, $another_id, $another_id, $user_id))->fetch()['cnt'];
    return $cnt > 0 ? true : false;
}

function is_friend_account($account_name)
{
    return is_friend(user_from_account($account_name)['id']);
}

function permitted($another_id)
{
    return $another_id == current_user()['id'] || is_friend($another_id);
}

function mark_footprint($user_id)
{
    if ($user_id != current_user()['id']) {
        $query = 'INSERT INTO footprints (user_id,owner_id) VALUES (?,?)';
        db_execute($query, array($user_id, current_user()['id']));
    }
}

function prefectures()
{
    static $PREFS = array(
        '未入力',
        '北海道', '青森県', '岩手県', '宮城県', '秋田県', '山形県', '福島県', '茨城県', '栃木県', '群馬県', '埼玉県', '千葉県', '東京都', '神奈川県', '新潟県', '富山県',
        '石川県', '福井県', '山梨県', '長野県', '岐阜県', '静岡県', '愛知県', '三重県', '滋賀県', '京都府', '大阪府', '兵庫県', '奈良県', '和歌山県', '鳥取県', '島根県',
        '岡山県', '広島県', '山口県', '徳島県', '香川県', '愛媛県', '高知県', '福岡県', '佐賀県', '長崎県', '熊本県', '大分県', '宮崎県', '鹿児島県', '沖縄県'
    );
    return $PREFS;
}

$app->get('/login', function () use ($app) {
    $app->view->setLayout(null);
    $app->render('login.php', array('message' => 'バルスでも落ちないツイッターへようこそ！'));
});

$app->post('/login', function () use ($app) {
    $params = $app->request->params();
    authenticate($params['email'], $params['password']);
    $app->redirect('/');
});

$app->get('/logout', function () use ($app) {
    $_SESSION['user_id'] = null;
    $app->redirect('/login');
});

$app->get('/', function () use ($app) {
    authenticated();
    $current_user = current_user();

    $tweets = db_execute('SELECT * FROM tweet WHERE USER_ID IN (SELECT follow_id FROM follow WHERE USER_ID = ?) ORDER BY created_at DESC LIMIT 100', array($current_user['id']))->fetchAll();

    $following = db_execute('SELECT * FROM follow WHERE user_id = ?', array($current_user['id']));
    $followers = db_execute('SELECT * FROM follow WHERE follow_id = ?', array($current_user['id']));

    $locals = array(
        'user' => current_user(),
        'tweets' => $tweets,
        'following' => $following,
        'followers' => $followers,
    );
    $app->render('index.php', $locals);
});

$app->get('/profile/:account_name', function ($account_name) use ($app) {
    authenticated();
    $owner = user_from_account($account_name);
    $prof = db_execute('SELECT * FROM profiles WHERE user_id = ?', array($owner['id']))->fetch();
    if (!$prof) $prof = array();
    if (permitted($owner['id'])) {
        $query = 'SELECT * FROM entries WHERE user_id = ? ORDER BY created_at LIMIT 5';
    } else {
        $query = 'SELECT * FROM entries WHERE user_id = ? AND private=0 ORDER BY created_at LIMIT 5';
    }
    $entries = array();
    $stmt = db_execute($query, array($owner['id']));
    while ($entry = $stmt->fetch()) {
        $entry['is_private'] = ($entry['private'] == 1);
        list($title, $content) = preg_split('/\n/', $entry['body'], 2);
        $entry['title'] = $title;
        $entry['content'] = $content;
        $entries[] = $entry;
    }
    mark_footprint($owner['id']);
    $locals = array(
        'owner' => $owner,
        'profile' => $prof,
        'entries' => $entries,
        'private' => permitted($owner['id']),
    );
    $app->render('profile.php', $locals);
});

$app->post('/profile/:account_name', function ($account_name) use ($app) {
    authenticated();
    if ($account_name != current_user()['account_name']) {
        abort_permission_denied();
    }
    $params = $app->request->params();
    $args = array($params['first_name'], $params['last_name'], $params['sex'], $params['birthday'], $params['pref']);

    $prof = db_execute('SELECT * FROM profiles WHERE user_id = ?', array(current_user()['id']))->fetch();
    if ($prof) {
      $query = <<<SQL
UPDATE profiles
SET first_name=?, last_name=?, sex=?, birthday=?, pref=?, updated_at=CURRENT_TIMESTAMP()
WHERE user_id = ?
SQL;
      $args[] = current_user()['id'];
    } else {
      $query = <<<SQL
INSERT INTO profiles (user_id,first_name,last_name,sex,birthday,pref) VALUES (?,?,?,?,?,?)
SQL;
        array_unshift($args, current_user()['id']);
    }
    db_execute($query, $args);
    $app->redirect('/profile/'.$account_name);
});

$app->get('/diary/entries/:account_name', function ($account_name) use ($app) {
    authenticated();
    $owner = user_from_account($account_name);
    if (permitted($owner['id'])) {
        $query = 'SELECT * FROM entries WHERE user_id = ? ORDER BY created_at DESC LIMIT 20';
    } else {
        $query = 'SELECT * FROM entries WHERE user_id = ? AND private=0 ORDER BY created_at DESC LIMIT 20';
    }
    $entries = array();
    $stmt = db_execute($query, array($owner['id']));
    while ($entry = $stmt->fetch()) {
        $entry['is_private'] = ($entry['private'] == 1);
        list($title, $content) = preg_split('/\n/', $entry['body'], 2);
        $entry['title'] = $title;
        $entry['content'] = $content;
        $entries[] = $entry;
    }
    mark_footprint($owner['id']);
    $locals = array(
        'owner' => $owner,
        'entries' => $entries,
        'myself' => (current_user()['id'] == $owner['id']),
    );
    $app->render('entries.php', $locals);
});

$app->get('/diary/entry/:entry_id', function ($entry_id) use ($app) {
    authenticated();
    $entry = db_execute('SELECT * FROM entries WHERE id = ?', array($entry_id))->fetch();
    if (!$entry) abort_content_not_found();
    list($title, $content) = preg_split('/\n/', $entry['body'], 2);
    $entry['title'] = $title;
    $entry['content'] = $content;
    $entry['is_private'] = ($entry['private'] == 1);
    $owner = get_user($entry['user_id']);
    if ($entry['is_private'] && !permitted($owner['id'])) {
        abort_permission_denied();
    }
    $comments = db_execute('SELECT * FROM comments WHERE entry_id = ?', array($entry['id']))->fetchAll();
    mark_footprint($owner['id']);
    $locals = array(
        'owner' => $owner,
        'entry' => $entry,
        'comments' => $comments,
    );
    $app->render('entry.php', $locals);
});

$app->post('/diary/entry', function () use ($app) {
    authenticated();
    $query = 'INSERT INTO entries (user_id, private, body) VALUES (?,?,?)';
    $params = $app->request->params();
    $title = isset($params['title']) ? $params['title'] : "タイトルなし";
    $content = isset($params['content']) ? $params['content'] : "";
    $body = $title . "\n" . $content;
    db_execute($query, array(current_user()['id'], (isset($params['private']) ? '1' : '0'), $body));
    $app->redirect('/diary/entries/'.current_user()['account_name']);
});

$app->post('/diary/comment/:entry_id', function ($entry_id) use ($app) {
    authenticated();
    $entry = db_execute('SELECT * FROM entries WHERE id = ?', array($entry_id))->fetch();
    if (!$entry) abort_content_not_found();
    $entry['is_private'] = ($entry['private'] == 1);
    if ($entry['is_private'] && !permitted($entry['user_id'])) {
        abort_permission_denied();
    }
    $query = 'INSERT INTO comments (entry_id, user_id, comment) VALUES (?,?,?)';
    $params = $app->request->params();
    db_execute($query, array($entry['id'], current_user()['id'], $params['comment']));
    $app->redirect('/diary/entry/'.$entry['id']);
});

$app->get('/footprints', function () use ($app) {
    authenticated();
    $query = <<<SQL
SELECT user_id, owner_id, DATE(created_at) AS date, MAX(created_at) as updated
FROM footprints
WHERE user_id = ?
GROUP BY user_id, owner_id, DATE(created_at)
ORDER BY updated DESC
LIMIT 50
SQL;
    $footprints = db_execute($query, array(current_user()['id']))->fetchAll();
    $app->render('footprints.php', array('footprints' => $footprints));
});

$app->get('/friends', function () use ($app) {
    authenticated();
    $query = 'SELECT * FROM relations WHERE one = ? OR another = ? ORDER BY created_at DESC';
    $friends = array();
    $stmt = db_execute($query, array(current_user()['id'], current_user()['id']));
    while ($rel = $stmt->fetch()) {
        $key = ($rel['one'] == current_user()['id'] ? 'another' : 'one');
        if (!isset($friends[$rel[$key]])) $friends[$rel[$key]] = $rel['created_at'];
    }
    $app->render('friends.php', array('friends' => $friends));
});

$app->post('/friends/:account_name', function ($account_name) use ($app) {
    authenticated();
    if (!is_friend_account($account_name)) {
        $user = user_from_account($account_name);
        if (!$user) abort_content_not_found();
        db_execute('INSERT INTO relations (one, another) VALUES (?,?), (?,?)', array(current_user()['id'], $user['id'], $user['id'], current_user()['id']));
        $app->redirect('/friends');
    }
});

$app->get('/initialize', function () use ($app) {
});

$app->run();
