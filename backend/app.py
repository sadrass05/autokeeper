from flask import Flask, request, jsonify
from flask_cors import CORS
import pymysql
import os

app = Flask(__name__)
CORS(app)

# MySQL config - can be overridden by environment variables
MYSQL_HOST = os.environ.get('MYSQL_HOST', 'localhost')
MYSQL_PORT = int(os.environ.get('MYSQL_PORT', 3306))
MYSQL_USER = os.environ.get('MYSQL_USER', 'root')
MYSQL_PASSWORD = os.environ.get('MYSQL_PASSWORD', '')
MYSQL_DB = os.environ.get('MYSQL_DB', 'autobookkeeper')


def get_connection():
    return pymysql.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=MYSQL_DB,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )


def init_db():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute('''
                CREATE TABLE IF NOT EXISTS expenses (
                    id BIGINT PRIMARY KEY,
                    amount DOUBLE NOT NULL,
                    merchant VARCHAR(255) DEFAULT '',
                    platform VARCHAR(64) DEFAULT '',
                    payment_channel VARCHAR(64) DEFAULT '',
                    category VARCHAR(64) DEFAULT '未分类',
                    is_finance_expense TINYINT(1) DEFAULT 0,
                    recorded_at BIGINT NOT NULL,
                    notification_id VARCHAR(255) DEFAULT ''
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            ''')
            cur.execute('''
                CREATE TABLE IF NOT EXISTS finance_positions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_name VARCHAR(255) NOT NULL,
                    platform VARCHAR(64) DEFAULT '',
                    buy_amount DOUBLE DEFAULT 0,
                    current_value DOUBLE DEFAULT 0,
                    profit DOUBLE DEFAULT 0,
                    profit_rate DOUBLE DEFAULT 0,
                    screenshot_path VARCHAR(512) DEFAULT '',
                    updated_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            ''')
        conn.commit()
    finally:
        conn.close()


@app.route('/api/expenses/sync', methods=['POST'])
def sync_expenses():
    data = request.get_json()
    records = data.get('records', [])
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            for r in records:
                cur.execute('''
                    INSERT INTO expenses (id, amount, merchant, platform, payment_channel, category, is_finance_expense, recorded_at, notification_id)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        amount=VALUES(amount), merchant=VALUES(merchant), platform=VALUES(platform),
                        payment_channel=VALUES(payment_channel), category=VALUES(category),
                        is_finance_expense=VALUES(is_finance_expense)
                ''', (
                    r.get('id', 0), r.get('amount', 0), r.get('merchant', ''),
                    r.get('platform', ''), r.get('paymentChannel', ''),
                    r.get('category', '未分类'), 1 if r.get('isFinanceExpense') else 0,
                    r.get('recordedAt', 0), r.get('notificationId', '')
                ))
        conn.commit()
        return jsonify({'success': True, 'count': len(records)})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
    finally:
        conn.close()


@app.route('/api/expenses/list', methods=['GET'])
def list_expenses():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute('SELECT * FROM expenses ORDER BY recorded_at DESC')
            rows = cur.fetchall()
        return jsonify(rows)
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()


@app.route('/api/finance/sync', methods=['POST'])
def sync_finance():
    data = request.get_json()
    positions = data.get('positions', [])
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            for p in positions:
                cur.execute('''
                    INSERT INTO finance_positions (product_name, platform, buy_amount, current_value, profit, profit_rate, screenshot_path, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                ''', (
                    p.get('productName', ''), p.get('platform', ''),
                    p.get('buyAmount', 0), p.get('currentValue', 0),
                    p.get('profit', 0), p.get('profitRate', 0),
                    p.get('screenshotPath', ''), p.get('updatedAt', 0)
                ))
        conn.commit()
        return jsonify({'success': True, 'count': len(positions)})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
    finally:
        conn.close()


@app.route('/api/finance/list', methods=['GET'])
def list_finance():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute('SELECT * FROM finance_positions ORDER BY updated_at DESC')
            rows = cur.fetchall()
        return jsonify(rows)
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()


if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=8080, debug=True)
