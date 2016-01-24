# -*- coding: utf-8 -*-

import os
import bottle
import pymysql
import logging


app = bottle.default_app()
app.config.load_dict({
    "db": {
        "host": os.environ.get("ISUCON_DB_HOST") or "localhost",
        "port": int(os.environ.get("ISUCON_DB_PORT") or 3306),
        "username": os.environ.get("ISUCON_DB_USER") or "isucon",
        "password": os.environ.get("ISUCON_DB_PASSWORD"),
        "database": os.environ.get("ISUCON_DB_NAME") or "isucon",
    },
    "session_secret": os.environ.get("ISUCON_SESSION_SECRET") or "isucon",
})

logging.basicConfig(filename='/tmp/python.log', format=logging.BASIC_FORMAT)


def abort_authentication_error():
    set_session_user_id(None)
    response = bottle.HTTPResponse(status=401, body=bottle.template("login", {"message": "ログインに失敗しました"}))
    response.add_header("WWW-Authenticate", 'Login realm="hi"')
    raise response


def abort_content_not_found():
    raise bottle.HTTPResponse(status=404, body=bottle.template("error", {"message": "要求されたコンテンツは存在しません"}))


def get_session_user_id():
    try:
        return bottle.request.get_cookie("user_id", secret=app.config["session_secret"])
    except ValueError:
        set_session_user_id(None)
        return None


def set_session_user_id(user_id):
    bottle.response.set_cookie("user_id", user_id, secret=app.config["session_secret"])


def db():
    try:
        return bottle.local.db
    except AttributeError:
        bottle.local.db = pymysql.connect(
            host=app.config["db.host"],
            port=app.config["db.port"],
            user=app.config["db.username"],
            password=app.config["db.password"],
            db=app.config["db.database"],
            charset="utf8mb4",
            autocommit=True,
            cursorclass=pymysql.cursors.DictCursor)
        return bottle.local.db


def db_fetchone(query, *args):
    args = args if args else None
    with db().cursor() as cursor:
        cursor.execute(query, args)
        return cursor.fetchone()


def db_fetchall(query, *args):
    args = args if args else None
    with db().cursor() as cursor:
        cursor.execute(query, args)
        return cursor.fetchall()


def db_execute(query, *args):
    args = args if args else None
    with db().cursor() as cursor:
        cursor.execute(query, args)
    db().commit()


def authenticate(email, password):
    query = "SELECT * FROM user WHERE email = %s AND passhash = SHA2(CONCAT(salt, %s), 256)"
    result = db_fetchone(query, email, password)
    if not result:
        abort_authentication_error()
    set_session_user_id(result["id"])


def current_user():
    try:
        return bottle.request.user
    except AttributeError:
        user_id = get_session_user_id()
        if user_id:
            query = "SELECT * FROM user WHERE id = %s"
            bottle.request.user = db_fetchone(query, get_session_user_id())
            if not bottle.request.user:
                set_session_user_id(None)
                abort_authentication_error()
        else:
            bottle.request.user = None
        return bottle.request.user


def authenticated():
    if not current_user():
        bottle.redirect("/login", 302)


def get_user(user_id):
    query = "SELECT * FROM user WHERE id = %s"
    result = db_fetchone(query, user_id)
    if not result:
        abort_content_not_found()
    return result


def user_from_account(account_name):
    query = "SELECT * FROM user WHERE account_name = %s"
    result = db_fetchone(query, account_name)
    if not result:
        abort_content_not_found()
    return result


def is_follow(follow_id):
    user_id = get_session_user_id()
    if not user_id:
        return False
    query = "SELECT COUNT(1) AS cnt FROM follow WHERE user_id = %s AND follow_id = %s"
    res = db_fetchone(query, user_id, follow_id)
    return res['cnt'] > 0


@app.get("/login")
def get_login():
    set_session_user_id(None)
    return bottle.template("login", {"message": "高負荷に耐えられるSNSコミュニティサイトへようこそ!"})


@app.post("/login")
def post_login():
    email = bottle.request.forms.getunicode("email")
    password = bottle.request.forms.getunicode("password")
    authenticate(email, password)
    bottle.redirect("/", 303)


@app.get("/logout")
def get_logout():
    set_session_user_id(None)
    bottle.redirect("/login", 303)


@app.get("/")
def get_index():
    authenticated()

    query = "SELECT * FROM tweet WHERE user_id IN (SELECT follow_id FROM follow WHERE user_id = %s) OR user_id = %s ORDER BY created_at DESC LIMIT 100"
    tweets = db_fetchall(query, current_user()["id"], current_user()["id"])

    following = db_fetchall("SELECT * FROM follow WHERE user_id = %s", current_user()["id"])
    followers = db_fetchall("SELECT * FROM follow WHERE follow_id = %s", current_user()["id"])

    return bottle.template("index", {
        "user": current_user(),
        "tweets": tweets,
        "following": following,
        "followers": followers,
    })


@app.get("/tweet")
def get_tweet():
    authenticated()

    return bottle.template("tweet", {
        "user": current_user(),
    })


@app.post("/tweet")
def post_tweet():
    authenticated()
    arg = bottle.request.forms.getunicode("content")
    db_execute("INSERT INTO tweet (user_id, content) VALUES (%s, %s)", current_user()["id"], arg)

    bottle.redirect("/", 303)


@app.get("/user/<user_id>")
def get_user_detail(user_id):
    authenticated()

    user = get_user(user_id)
    query = "SELECT * FROM tweet WHERE user_id = %s ORDER BY created_at DESC LIMIT 100"
    tweets = db_fetchall(query, user_id)

    return bottle.template("user", {
        "user": user,
        "tweets": tweets,
        "myself": current_user(),
    })


@app.get("/following")
def get_following():
    authenticated()
    following = db_fetchall("SELECT * FROM follow WHERE user_id = %s", current_user()["id"])
    following_users = []
    for f in following:
        user = db_fetchone("SELECT * FROM user WHERE id = %s", f["follow_id"])
        following_users.append(user)

    return bottle.template("following", {
        "user": user,
        "following": following_users,
    })


@app.post("/follow/<user_id>")
def post_follow(user_id):
    authenticated()
    db_execute("INSERT INTO follow (user_id, follow_id) VALUES (%s, %s)", current_user()["id"], user_id)

    bottle.redirect("/", 303)


@app.get("/followers")
def get_followers():
    authenticated()
    followers = db_fetchall("SELECT * FROM follow WHERE follow_id = %s", current_user()["id"])
    followers_users = []
    for f in followers:
        user = db_fetchone("SELECT * FROM user WHERE id = %s", f["user_id"])
        followers_users.append(user)

    return bottle.template("followers", {
        "user": current_user(),
        "followers": followers_users,
    })


@app.get("/css/<filename:path>")
def get_css(filename):
    return get_static("css", filename)


@app.get("/fonts/<filename:path>")
def get_fonts(filename):
    return get_static("fonts", filename)


@app.get("/js/<filename:path>")
def get_js(filename):
    return get_static("js", filename)


def get_static(dirname, filename):
    basedir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    staticdir = os.path.join(basedir, "static", dirname)
    return bottle.static_file(filename, root=staticdir)


@app.get("/initialize")
def get_initialize():
    os.system('/bin/sh ../tools/init.sh')
    return ""


bottle.BaseTemplate.defaults = {
    "db": db,
    "db_fetchone": db_fetchone,
    "get_user": get_user,
    "is_follow": is_follow,
    "current_user": current_user,
}

if __name__ == "__main__":
    app.run(server="wsgiref",
            host="127.0.0.1",
            port=8080,
            reloader=True,
            quiet=False,
            debug=True)
