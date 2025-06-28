from flask import Flask, request, jsonify
from llama_cpp import Llama
import numpy as np
import time

app = Flask(__name__)

# ✅ 模型加载
llm = Llama(
    model_path="/Users/ws/.lmstudio/models/Qwen/Qwen3-Embedding-0.6B-GGUF/Qwen3-Embedding-0.6B-Q8_0.gguf",  # 修改为你的路径
    embedding=True,
    n_ctx=8192,
    n_threads=8
)

# ✅ 向量归一化函数
def normalize(vec):
    vec = np.array(vec)
    norm = np.linalg.norm(vec)
    return (vec / norm).tolist() if norm > 0 else vec.tolist()

# ✅ OpenAI 兼容接口
@app.route("/v1/embeddings", methods=["POST"])
def embedding_endpoint():
    start = time.time()
    payload = request.json
    input_data = payload.get("input")

    if isinstance(input_data, str):
        input_data = [input_data]

    results = []
    for i, text in enumerate(input_data):
        text += "<|endoftext|>"  # Qwen embedding 要求
        embedding = llm.embed(text)
        # 如果是二维嵌套列表，说明是多个 token 的 embedding
        if isinstance(embedding[0], list):
            # Qwen 推荐使用最后一个 token 的向量
            embedding = embedding[-1]
        embedding = normalize(embedding)
        results.append({
            "object": "embedding",
            "index": i,
            "embedding": embedding
        })

    response = {
        "object": "list",
        "data": results,
        "model": "qwen3-embedding",
        "usage": {
            "prompt_tokens": 0,
            "total_tokens": 0
        }
    }
    print(f"[{time.time() - start:.2f}s] {len(results)} embedding(s)")
    return jsonify(response)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8001)
