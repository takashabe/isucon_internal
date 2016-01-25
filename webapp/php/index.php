<?php
require 'vendor/autoload.php';

date_default_timezone_set('Asia/Tokyo');
mb_internal_encoding('UTF-8');

class IsuconView extends \Slim\View
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
    'view' => new IsuconView(),
    'db' => array(
        'host' => getenv('ISUCON_DB_HOST') ?: 'localhost',
        'port' => (int)getenv('ISUCON_DB_PORT') ?: 3306,
        'username' => getenv('ISUCON_DB_USER') ?: 'isucon',
        'password' => getenv('ISUCON_DB_PASSWORD'),
        'database' => getenv('ISUCON_DB_NAME') ?: 'isucon'
    ),
    'cookies.encrypt' => true,
));

$app->add(new \Slim\Middleware\SessionCookie(array(
    'secret' => getenv('ISUCON_SESSION_SECRET') ?: 'isucon',
    'expires' => 0,
)));

function abort_authentication_error()
{
    global $app;
    $_SESSION['user_id'] = null;
    $app->view->setLayout(null);
    $app->render('login.php', array('message' => 'ログインに失敗しました'), 401);
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

function is_follow($follow_id)
{
    $user_id = $_SESSION['user_id'];
    $query = 'SELECT COUNT(1) AS cnt FROM follow WHERE user_id = ? AND follow_id = ?';
    $cnt = db_execute($query, array($user_id, $follow_id))->fetch()['cnt'];
    return $cnt > 0;
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

    $tweets = db_execute(
        'SELECT * FROM tweet WHERE USER_ID IN (SELECT follow_id FROM follow WHERE USER_ID = ?) OR USER_ID = ? ORDER BY created_at DESC LIMIT 100',
        array($current_user['id'], $current_user['id']))->fetchAll();

    $following = db_execute('SELECT * FROM follow WHERE user_id = ?', array($current_user['id']))->fetchAll();
    $followers = db_execute('SELECT * FROM follow WHERE follow_id = ?', array($current_user['id']))->fetchAll();

    $locals = array(
        'user' => current_user(),
        'tweets' => $tweets,
        'following' => $following,
        'followers' => $followers,
    );
    $app->render('index.php', $locals);
});

$app->get('/tweet', function () use ($app) {
    authenticated();
    $app->render('tweet.php');
});

$app->post('/tweet', function () use ($app) {
    authenticated();
    $params = $app->request->params();
    db_execute('INSERT INTO tweet (user_id, content) VALUES (?, ?)', array(current_user()['id'], $params['content']));
    $app->redirect('/');
});

$app->get('/user/:user_id', function ($user_id) use ($app) {
    authenticated();
    $user = get_user($user_id);
    $tweets = db_execute('SELECT * FROM tweet WHERE user_id = ? ORDER BY created_at DESC LIMIT 100', array($user_id))->fetchAll();
    $locals = array(
        'user' => $user,
        'tweets' => $tweets,
        'myself' => current_user(),
    );
    $app->render('user.php', $locals);
});

$app->get('/following', function () use ($app) {
    authenticated();
    $following = db_execute('SELECT * FROM follow WHERE user_id = ?', array(current_user()['id']))->fetchAll();
    $following_user = array();
    foreach($following as $f) {
        $user = db_execute('SELECT * FROM user WHERE id = ?', array($f['follow_id']))->fetch();
        $following_user[] = $user;
    }
    $locals = array(
        'following' => $following_user,
    );
    $app->render('following.php', $locals);
});

$app->post('/follow/:user_id', function ($user_id) use ($app) {
    authenticated();
    db_execute('INSERT INTO follow (user_id, follow_id) VALUES (?, ?)', array(current_user()['id'], $user_id));
    $app->redirect('/');
});

$app->get('/followers', function () use ($app) {
    authenticated();
    $followers = db_execute('SELECT * FROM follow WHERE follow_id = ?', array(current_user()['id']))->fetchAll();
    $followers_user = array();
    foreach($followers as $f) {
        $user = db_execute('SELECT * FROM user WHERE id = ?', array($f['user_id']))->fetch();
        $followers_user[] = $user;
    }
    $locals = array(
        'followers' => $followers_user,
    );
    $app->render('followers.php', $locals);
});

$app->get('/initialize', function () use ($app) {
    exec('/bin/sh ../tools/init.sh');
});

$app->run();
