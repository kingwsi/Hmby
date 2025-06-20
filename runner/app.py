import psycopg2
import schedule
import time
import subprocess
import os
from flask import Flask, jsonify
import logging
from datetime import datetime
import threading
import json

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Flask 应用
app = Flask(__name__)

# 数据库配置
DB_CONFIG = {
    'dbname': 'your_database',
    'user': 'your_username',
    'password': 'your_password',
    'host': 'localhost',
    'port': '5432'
}

# 服务状态
service_status = {
    'status': 'running',
    'last_check': None,
    'processed_count': 0,
    'error_count': 0
}

def get_db_connection():
    """获取数据库连接"""
    try:
        return psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        logger.error(f"数据库连接失败: {str(e)}")
        return None

def get_video_info(input_path):
    """获取视频信息（分辨率、帧率等）"""
    try:
        cmd = [
            'ffprobe',
            '-v', 'error',
            '-show_entries', 'stream=width,height,r_frame_rate',
            '-of', 'json',
            input_path
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        info = json.loads(result.stdout)
        stream = info['streams'][0]
        width, height = stream['width'], stream['height']
        frame_rate = eval(stream['r_frame_rate'])  # 转换为浮点数，如 30/1 -> 30.0
        return width, height, frame_rate
    except Exception as e:
        logger.error(f"获取视频信息失败: {str(e)}")
        return None, None, None

def process_encode(input_path, output_path, meta_title, crf=23, max_resolution=1080, max_fps=30):
    """处理视频编码（压缩）"""
    try:
        if not os.path.exists(input_path):
            raise Exception(f"输入文件不存在: {input_path}")

        width, height, frame_rate = get_video_info(input_path)
        if width is None:
            raise Exception("无法获取视频信息")

        scale_filter = ""
        if height > max_resolution:
            scale_filter = f"-vf scale=-2:{max_resolution}"

        fps_filter = ""
        if frame_rate > max_fps:
            fps_filter = f"-r {max_fps}"

        cmd = [
            'ffmpeg',
            '-i', input_path,
            '-hwaccel', 'cuvid'
            '-c:v', "h264_cuvid",
            '-crf', str(crf),
            '-preset', 'medium',
            '-tag:v', 'hvc1'
            '-c:a', 'aac',
            '-b:a', '128k',
        ]
        
        if meta_title:
            cmd.extend(['-metadata', f'title=\'{meta_title}\''])

        if scale_filter or fps_filter:
            filters = []
            if scale_filter:
                filters.append(scale_filter.lstrip('-vf '))
            if fps_filter:
                filters.append(fps_filter.lstrip('-r '))
            cmd.extend(['-vf', ','.join(filters)])

        cmd.extend(['-y', output_path])

        process = subprocess.run(cmd, capture_output=True, text=True)
        if process.returncode != 0:
            raise Exception(f"FFmpeg 编码失败: {process.stderr}")

        processed_size = os.path.getsize(output_path)
        return True, processed_size, None
    except Exception as e:
        return False, 0, str(e)

def process_cut(input_path, output_path, media_id):
    """处理视频剪切并拼接"""
    try:
        if not os.path.exists(input_path):
            raise Exception(f"输入文件不存在: {input_path}")

        # 获取 media_marks 表中的片段
        conn = get_db_connection()
        if not conn:
            raise Exception("数据库连接失败")

        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT start_time, end_time
                    FROM media_marks
                    WHERE media_id = %s
                    ORDER BY start_time
                    """,
                    (media_id,)
                )
                segments = cur.fetchall()
        finally:
            conn.close()

        if not segments:
            raise Exception("未找到视频片段")

        # 创建临时文件列表用于拼接
        temp_dir = "temp_segments"
        os.makedirs(temp_dir, exist_ok=True)
        temp_files = []
        temp_list_file = os.path.join(temp_dir, f"concat_list_{media_id}.txt")

        try:
            # 剪切每个片段
            for i, (start_time, end_time) in enumerate(segments):
                temp_output = os.path.join(temp_dir, f"segment_{media_id}_{i}.mp4")
                duration = (end_time - start_time) / 1000.0  # 转换为秒
                cmd = [
                    'ffmpeg',
                    '-i', input_path,
                    '-ss', str(start_time / 1000.0),
                    '-t', str(duration),
                    '-c', 'copy',  # 使用流复制避免重新编码
                    '-y', temp_output
                ]
                process = subprocess.run(cmd, capture_output=True, text=True)
                if process.returncode != 0:
                    raise Exception(f"FFmpeg 剪切失败: {process.stderr}")
                temp_files.append(temp_output)

            # 创建拼接列表文件
            with open(temp_list_file, 'w') as f:
                for temp_file in temp_files:
                    f.write(f"file '{temp_file}'\n")

            # 拼接片段
            cmd = [
                'ffmpeg',
                '-f', 'concat',
                '-safe', '0',
                '-i', temp_list_file,
                '-c', 'copy',
                '-y', output_path
            ]
            process = subprocess.run(cmd, capture_output=True, text=True)
            if process.returncode != 0:
                raise Exception(f"FFmpeg 拼接失败: {process.stderr}")

            processed_size = os.path.getsize(output_path)
            return True, processed_size, None

        finally:
            # 清理临时文件
            for temp_file in temp_files:
                if os.path.exists(temp_file):
                    os.remove(temp_file)
            if os.path.exists(temp_list_file):
                os.remove(temp_list_file)

    except Exception as e:
        return False, 0, str(e)

def update_media_record(media_id, status, processed_size, error_message=None):
    """更新数据库记录"""
    conn = get_db_connection()
    if not conn:
        return False

    try:
        with conn.cursor() as cur:
            if error_message:
                cur.execute(
                    """
                    UPDATE media_infos
                    SET status = %s, processed_size = %s, error_message = %s, updated_at = %s
                    WHERE id = %s
                    """,
                    (status, processed_size, error_message, datetime.now(), media_id)
                )
            else:
                cur.execute(
                    """
                    UPDATE media_infos
                    SET status = %s, processed_size = %s, updated_at = %s
                    WHERE id = %s
                    """,
                    (status, processed_size, datetime.now(), media_id)
                )
        conn.commit()
        return True
    except Exception as e:
        logger.error(f"更新记录失败: {str(e)}")
        conn.rollback()
        return False
    finally:
        conn.close()

def process_pending_videos():
    """处理待处理的视频记录"""
    global service_status
    conn = get_db_connection()
    if not conn:
        return

    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, input_path, codec, output_path, processed_size, status, type, meta_title
                FROM media_infos
                WHERE status = 'pending' AND type IN ('ENCODE', 'CUT')
                """
            )
            records = cur.fetchall()

            for record in records:
                media_id, input_path, codec, output_path, processed_size, status, process_type, meta_title = record
                logger.info(f"开始处理视频: {input_path}, 类型: {process_type}")

                # 根据 type 处理
                if process_type == 'ENCODE':
                    success, new_size, error_message = process_encode(
                        input_path, output_path, meta_title, crf=23
                    )
                elif process_type == 'CUT':
                    success, new_size, error_message = process_cut(
                        input_path, output_path, media_id
                    )
                else:
                    logger.error(f"未知处理类型: {process_type}")
                    continue

                # 更新记录
                new_status = 'completed' if success else 'failed'
                if update_media_record(media_id, new_status, new_size, error_message):
                    service_status['processed_count'] += 1 if success else 0
                    service_status['error_count'] += 1 if not success else 0
                    logger.info(f"视频处理{'成功' if success else '失败'}: {input_path}")
                else:
                    logger.error(f"更新记录失败: {input_path}")

    except Exception as e:
        logger.error(f"处理视频时发生错误: {str(e)}")
    finally:
        conn.close()

    service_status['last_check'] = datetime.now().isoformat()

@app.route('/status', methods=['GET'])
def get_status():
    """获取服务状态"""
    return jsonify(service_status)

@app.route('/pending/list', methods=['GET'])
def get_pending_status():
    """获取服务状态"""
    return jsonify(service_status)

def run_scheduler():
    """运行定时任务"""
    schedule.every(5).minutes.do(process_pending_videos)
    while True:
        schedule.run_pending()
        time.sleep(60)

if __name__ == '__main__':
    # 启动定时任务线程
    scheduler_thread = threading.Thread(target=run_scheduler, daemon=True)
    scheduler_thread.start()

    # 启动 Flask 服务
    app.run(host='0.0.0.0', port=5123)