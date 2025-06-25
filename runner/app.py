import psycopg2
import schedule
import time
from flask import Flask, jsonify
import logging
from datetime import datetime
import threading

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Flask 应用
app = Flask(__name__)

# 数据库配置
DB_CONFIG = {
    'dbname': 'hmby',
    'user': 'postgres',
    'password': 'dev',
    'host': '192.168.123.232',
    'port': '5432'
}

# 路径映射配置（示例，可以从配置文件或数据库加载）
# 路径映射配置
# Windows格式使用 \\ 双反斜杠
# Linux/Unix格式使用 / 正斜杠
# 根据操作系统自动选择正确的路径格式
PATH_MAPPING = {
    '/downloads': '\\\\192.168.123.232\\downloads',
    '/db/output': '\\\\192.168.123.232\\Volume1'
}

APP_CONFIG = {
    'output_path': '/downloads/output',
    'temp_path': 'D:\\temp',
}

def map_path(db_path):
    """将数据库中的路径映射到实际文件路径"""
    if not isinstance(db_path, str):
        logger.error(f"路径映射失败，传入值无效: {db_path}")
        return db_path

    try:
        for db_prefix, actual_prefix in PATH_MAPPING.items():
            if db_path.startswith(db_prefix):
                # 先替换路径前缀
                mapped_path = db_path.replace(db_prefix, actual_prefix, 1)
                # 使用os.path.normpath()将路径分隔符转换为当前系统使用的格式
                return os.path.normpath(mapped_path)
        # 如果没有匹配的映射，返回原路径
        logger.warning(f"路径未找到映射，直接使用: {db_path}")
        return os.path.normpath(db_path)
    except Exception as e:
        logger.error(f"路径映射失败: {str(e)}")
        return db_path

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
import os
import ffmpeg

def process_encode(input_path, meta_title, crf=26, max_resolution=1080, max_fps=30):
    """处理视频编码（压缩）"""
    try:
        output_path = get_output_path(input_path)
        logger.info(f"process_encode 输出目录: {output_path}")

        actual_input_path = map_path(input_path)
        actual_output_path = map_path(output_path)
        if not os.path.exists(actual_input_path):
            raise Exception(f"输入文件不存在: {actual_input_path}")

        # 获取视频信息
        probe = ffmpeg.probe(actual_input_path)
        video_info = next(s for s in probe['streams'] if s['codec_type'] == 'video')
        width = int(video_info['width'])
        height = int(video_info['height'])
        frame_rate = eval(video_info['r_frame_rate'])

        # 构建输入流
        stream = ffmpeg.input(actual_input_path, hwaccel='cuda')

        # 应用视频滤镜
        if height > max_resolution:
            stream = stream.filter('scale', '-2', max_resolution)
        if frame_rate > max_fps:
            stream = stream.filter('fps', fps=max_fps)

        # 设置输出参数
        stream = ffmpeg.output(
            stream,
            actual_output_path,
            vcodec='hevc_nvenc',
            crf=crf,
            preset='medium',
            acodec='aac',
            audio_bitrate='128k',
            metadata={'title': meta_title} if meta_title else None,
            **{'tag:v': 'avc1'},
            overwrite_output=True
        )

        # 获取命令行字符串
        cmd = ' '.join(stream.compile())
        logger.info(f"执行命令: {cmd}")

        # 运行 FFmpeg 进程
        process = stream.run_async(pipe_stdout=True, pipe_stderr=True)
        stderr_output_lines = []
        # 实时读取 stderr 获取进度
        while True:
            output = process.stderr.readline()
            if output == b'' and process.poll() is not None:
                break
            if output:
                output_str = output.decode('utf-8', errors='ignore')
                stderr_output_lines.append(output_str)  # ⬅️ 缓存下来
                if "frame=" in output_str:
                    logger.info(f"FFmpeg进度: {output_str.strip()}")

        # 等待完成
        process.wait()

        # 检查退出状态
        if process.returncode != 0:
            stdout, stderr = process.communicate()
            error_output = stderr.decode('utf-8', errors='ignore') if isinstance(stderr, bytes) else str(stderr)
            logger.error(f"FFmpeg 错误信息: {error_output.strip()}")
            raise Exception(f"FFmpeg 编码失败，返回码非 0。错误信息: {error_output.strip()}")

        processed_size = os.path.getsize(actual_output_path)
        return True, processed_size, None

    except Exception as e:
        logger.error(f"编码出错: {str(e)}")
        return False, 0, str(e)

def get_output_path(input_path, meta_title=None):
    """根据输入路径和标题生成输出路径"""
    # 获取输出目录的基础路径
    # 获取基础输出目录
    base_output_dir = APP_CONFIG.get('output_path')
    if not base_output_dir:
        raise Exception("未配置输出目录")

    # 获取当前年月作为子目录
    current_date = datetime.now()
    year_month = current_date.strftime("%Y%m")

    # 组合完整输出目录
    base_output_dir = os.path.join(base_output_dir, year_month)

    # 确保输出目录存在
    os.makedirs(map_path(base_output_dir), exist_ok=True)

    # 从输入路径中提取文件名
    input_filename = os.path.basename(input_path)
    filename_without_ext = os.path.splitext(input_filename)[0]

    # 构建基础输出文件名
    if meta_title:
        output_filename = f"{meta_title}.mp4"
    else:
        output_filename = f"{filename_without_ext}.mp4"

    # 构建完整输出路径，使用正斜杠确保Linux格式
    output_path = f"{base_output_dir}/{output_filename}".replace('\\', '/')

    # 如果文件已存在，添加数字后缀
    counter = 1
    while os.path.exists(map_path(output_path)):
        new_filename = f"{filename_without_ext}_{counter}.mp4"
        output_path = f"{base_output_dir}/{new_filename}".replace('\\', '/')
        counter += 1

    return output_path
def process_cut(input_path, media_id):
    """处理视频剪切并拼接"""
    try:
        import ffmpeg

        output_path = get_output_path(input_path)
        logger.info(f"process_cut 输出目录: {output_path}")
        # 映射路径
        actual_input_path = map_path(input_path)
        actual_output_path = map_path(output_path)
        if not os.path.exists(actual_input_path):
            raise Exception(f"输入文件不存在: {actual_input_path}")

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
        temp_dir = APP_CONFIG.get('temp_path')
        os.makedirs(temp_dir, exist_ok=True)
        temp_files = []
        temp_list_file = os.path.join(temp_dir, f"concat_list_{media_id}.txt")

        try:
            # 剪切每个片段
            for i, (start_time, end_time) in enumerate(segments):
                temp_output = os.path.join(temp_dir, f"segment_{media_id}_{i}.mp4")
                duration = (end_time - start_time) / 1000.0  # 转换为秒

                # 使用ffmpeg-python剪切片段
                stream = ffmpeg.input(actual_input_path, ss=start_time/1000.0, t=duration)
                stream = ffmpeg.output(stream, temp_output, c='copy', y=None)

                # 运行ffmpeg命令
                try:
                    ffmpeg.run(stream, capture_stdout=True, capture_stderr=True)
                except ffmpeg.Error as e:
                    error_msg = e.stderr.decode('utf-8') if isinstance(e.stderr, bytes) else e.stderr
                    raise Exception(f"FFmpeg 剪切失败: {error_msg}")

                temp_files.append(temp_output)

            # 创建拼接列表文件
            with open(temp_list_file, 'w') as f:
                for temp_file in temp_files:
                    f.write(f"file '{temp_file}'\n")

            # 使用ffmpeg-python拼接片段
            stream = ffmpeg.input(temp_list_file, f='concat', safe=0)
            stream = ffmpeg.output(stream, actual_output_path, c='copy', y=None)

            try:
                ffmpeg.run(stream, capture_stdout=True, capture_stderr=True)
            except ffmpeg.Error as e:
                error_msg = e.stderr.decode('utf-8') if isinstance(e.stderr, bytes) else e.stderr
                raise Exception(f"FFmpeg 拼接失败: {error_msg}")

            processed_size = os.path.getsize(actual_output_path)
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
                # 打印错误日志
                logger.error(f"处理失败: {error_message}")
                # 截取错误信息长度,避免超出数据库字段长度限制(varchar 1000)
                error_message = error_message[:1000] if error_message else None
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
        is_processing = False

# 全局变量，用于跟踪任务执行状态
is_processing = False

def process_pending_videos():
    """处理待处理的视频记录"""
    global service_status, is_processing

    # 如果上次任务执行中，就不要执行了
    if is_processing:
        logger.info("上一次任务仍在执行中，跳过本次执行")
        return

    is_processing = True
    conn = get_db_connection()
    if not conn:
        is_processing = False
        return

    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, input_path, codec, output_path, processed_size, status, type, meta_title
                FROM media_infos
                WHERE status = 'PENDING' AND type IN ('ENCODE', 'CUT')
                """
            )
            records = cur.fetchall()

            for record in records:
                media_id, input_path, codec, output_path, processed_size, status, process_type, meta_title = record
                logger.info(f"开始处理视频: {input_path}, 类型: {process_type}")

                # 根据 type 处理
                if process_type == 'ENCODE':
                    success, new_size, error_message = process_encode(
                        input_path, meta_title, crf=26
                    )
                elif process_type == 'CUT':
                    success, new_size, error_message = process_cut(
                        input_path, media_id
                    )
                else:
                    logger.error(f"未知处理类型: {process_type}")
                    continue

                # 更新记录
                new_status = 'DONE' if success else 'FAIL'
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
        is_processing = False

    service_status['last_check'] = datetime.now().isoformat()

@app.route('/', methods=['GET'])
def get_status():
    """获取服务状态"""
    return jsonify(service_status)

@app.route('/get_output_path', methods=['GET'])
def get_output_path_rest():
    input_path = request.args.get('input_path')
    meta_title = request.args.get('meta_title')
    return get_output_path(input_path, meta_title)

# 在文件顶部添加 Flask 相关导入（如果尚未导入 request）
from flask import request

# 在 Flask 应用中添加手动执行端点
@app.route('/process', methods=['GET'])
def manual_process():
    process_pending_videos()
    return jsonify("{'msg','ok'}")

def run_scheduler():
    """运行定时任务"""
    schedule.every(1).minutes.do(process_pending_videos)
    while True:
        schedule.run_pending()
        time.sleep(60)

if __name__ == '__main__':
    # 启动定时任务线程
    scheduler_thread = threading.Thread(target=run_scheduler, daemon=True)
    scheduler_thread.start()

    # 启动 Flask 服务
    app.run(host='0.0.0.0', port=5123)