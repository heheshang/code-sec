import sqlite3


def unsafe_query(user_id):
    conn = sqlite3.connect("test.db")
    cursor = conn.cursor()
    # ❌ SQL injection: string concatenation
    query = "SELECT * FROM users WHERE id = '" + user_id + "'"
    cursor.execute(query)
    return cursor.fetchall()


def unsafe_query_fstring(user_id):
    conn = sqlite3.connect("test.db")
    cursor = conn.cursor()
    # ❌ SQL injection: f-string
    query = f"SELECT * FROM users WHERE id = {user_id}"
    cursor.execute(query)
    return cursor.fetchall()


def safe_query(user_id):
    conn = sqlite3.connect("test.db")
    cursor = conn.cursor()
    # ✅ Safe: parameterized query
    cursor.execute("SELECT * FROM users WHERE id = ?", (user_id,))
    return cursor.fetchall()
