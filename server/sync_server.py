from flask import Flask, request, jsonify
import pymysql
import os
import traceback
from datetime import datetime

app = Flask(__name__)

DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_PORT = int(os.environ.get('DB_PORT', 3306))
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '123456')
DB_NAME = os.environ.get('DB_NAME', 'autobookkeeper')


def get_connection():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
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
                    amount DOUBLE,
                    merchant VARCHAR(255),
                    platform VARCHAR(50),
                    payment_channel VARCHAR(50),
                    category VARCHAR(50),
                    is_finance_expense TINYINT(1),
                    recorded_at BIGINT,
                    notification_id VARCHAR(255) DEFAULT '',
                    is_deleted TINYINT(1) DEFAULT 0
                )
            ''')
            cur.execute('''
                CREATE TABLE IF NOT EXISTS finance_positions (
                    id INT PRIMARY KEY,
                    product_name VARCHAR(255),
                    platform VARCHAR(50),
                    buy_amount DOUBLE,
                    current_value DOUBLE,
                    profit DOUBLE,
                    profit_rate DOUBLE,
                    screenshot_path VARCHAR(500) DEFAULT '',
                    updated_at BIGINT
                )
            ''')
        conn.commit()
    finally:
        conn.close()


@app.before_request
def log_request():
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    client_ip = request.remote_addr
    print(f'[{timestamp}] {request.method} {request.path} from {client_ip}')


@app.after_request
def log_response(response):
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    print(f'[{timestamp}] → {response.status_code}')
    return response


@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, OPTIONS'
    return response


@app.route('/ping', methods=['GET', 'OPTIONS'])
def ping():
    if request.method == 'OPTIONS':
        return '', 200
    return jsonify({'status': 'ok'})


@app.route('/sync/expenses', methods=['POST', 'OPTIONS'])
def sync_expenses():
    if request.method == 'OPTIONS':
        return '', 200
    try:
        data = request.get_json()
        records = data.get('records', [])
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                for r in records:
                    cur.execute('''
                        REPLACE INTO expenses (id, amount, merchant, platform, payment_channel, category, is_finance_expense, recorded_at, notification_id)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ''', (
                        r.get('id', 0), r.get('amount', 0), r.get('merchant', ''),
                        r.get('platform', ''), r.get('paymentChannel', ''),
                        r.get('category', '未分类'), 1 if r.get('isFinanceExpense') else 0,
                        r.get('recordedAt', 0), r.get('notificationId', '')
                    ))
            conn.commit()
            result = jsonify({'success': True, 'message': f'已同步 {len(records)} 条支出记录'})
            print(f'  synced {len(records)} expense records')
            return result
        finally:
            conn.close()
    except Exception as e:
        print(f'  ERROR: {traceback.format_exc()}')
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/sync/positions', methods=['POST', 'OPTIONS'])
def sync_positions():
    if request.method == 'OPTIONS':
        return '', 200
    try:
        data = request.get_json()
        positions = data.get('positions', [])
        conn = get_connection()
        try:
            with conn.cursor() as cur:
                for p in positions:
                    cur.execute('''
                        REPLACE INTO finance_positions (id, product_name, platform, buy_amount, current_value, profit, profit_rate, screenshot_path, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ''', (
                        p.get('id', 0), p.get('productName', ''), p.get('platform', ''),
                        p.get('buyAmount', 0), p.get('currentValue', 0),
                        p.get('profit', 0), p.get('profitRate', 0),
                        p.get('screenshotPath', ''), p.get('updatedAt', 0)
                    ))
            conn.commit()
            result = jsonify({'success': True, 'message': f'已同步 {len(positions)} 条理财记录'})
            print(f'  synced {len(positions)} position records')
            return result
        finally:
            conn.close()
    except Exception as e:
        print(f'  ERROR: {traceback.format_exc()}')
        return jsonify({'success': False, 'error': str(e)}), 500


if __name__ == '__main__':
    print(f'DB_HOST={DB_HOST}, DB_PORT={DB_PORT}, DB_USER={DB_USER}, DB_NAME={DB_NAME}')
    init_db()
    print('服务已启动在 http://0.0.0.0:5000')
    print('等待请求...')
    app.run(host='0.0.0.0', port=5000, debug=False)