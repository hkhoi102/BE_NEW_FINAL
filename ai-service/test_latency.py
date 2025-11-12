"""
Script để test độ trễ (latency) của AI Chatbot
Gửi 10 yêu cầu HTTP đồng thời và đo latency
"""

import asyncio
import aiohttp
import time
import statistics
from typing import List, Dict, Any, Optional
import json
import sys
import random
import os
from datetime import datetime


# Cấu hình
API_URL = "http://localhost:8000/chat"  # Có thể thay đổi thành URL thực tế
NUM_REQUESTS = 10
TEST_QUESTION = "cửa hàng có redbull không"  # Câu hỏi test (dùng khi không có file questions)
QUESTIONS_FILE = "test_questions.json"  # File chứa danh sách câu hỏi


class LatencyTestResult:
    """Lưu trữ kết quả test latency"""
    def __init__(self):
        self.request_id: int = 0
        self.latency_ms: float = 0.0
        self.status_code: int = 0
        self.success: bool = False
        self.error: str = ""
        self.response_size: int = 0
        self.question: str = ""  # Câu hỏi đã sử dụng


def load_test_questions(question_type: Optional[str] = None) -> List[str]:
    """
    Load danh sách câu hỏi từ file JSON

    Args:
        question_type: Loại câu hỏi (simple, complex_sql, complex_natural_language, mixed_complexity, rag_questions)
                      Nếu None, trả về tất cả câu hỏi

    Returns:
        List[str]: Danh sách câu hỏi
    """
    questions_file_path = os.path.join(os.path.dirname(__file__), QUESTIONS_FILE)

    if not os.path.exists(questions_file_path):
        print(f"⚠️  File {QUESTIONS_FILE} không tồn tại, sử dụng câu hỏi mặc định")
        return [TEST_QUESTION]

    try:
        with open(questions_file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        if question_type and question_type in data:
            return data[question_type]
        elif question_type == "all":
            # Trả về tất cả câu hỏi từ tất cả categories
            all_questions = []
            for category, questions in data.items():
                all_questions.extend(questions)
            return all_questions
        else:
            # Mặc định dùng mixed_complexity nếu có, không thì dùng simple
            if "mixed_complexity" in data:
                return data["mixed_complexity"]
            elif "simple" in data:
                return data["simple"]
            else:
                return [TEST_QUESTION]
    except Exception as e:
        print(f"⚠️  Lỗi khi đọc file {QUESTIONS_FILE}: {e}")
        return [TEST_QUESTION]


def select_questions(question_list: List[str], num_requests: int, randomize: bool = True) -> List[str]:
    """
    Chọn câu hỏi từ danh sách

    Args:
        question_list: Danh sách câu hỏi
        num_requests: Số lượng requests cần
        randomize: Có chọn ngẫu nhiên không

    Returns:
        List[str]: Danh sách câu hỏi đã chọn
    """
    if len(question_list) == 0:
        return [TEST_QUESTION] * num_requests

    if randomize:
        # Chọn ngẫu nhiên với replacement
        return [random.choice(question_list) for _ in range(num_requests)]
    else:
        # Chọn tuần tự, lặp lại nếu cần
        selected = []
        for i in range(num_requests):
            selected.append(question_list[i % len(question_list)])
        return selected


async def send_chat_request(
    session: aiohttp.ClientSession,
    request_id: int,
    question: str = None
) -> LatencyTestResult:
    """
    Gửi một request đến API chatbot và đo latency

    Args:
        session: aiohttp session
        request_id: ID của request (để tracking)
        question: Câu hỏi để gửi (mặc định dùng TEST_QUESTION)

    Returns:
        LatencyTestResult: Kết quả đo latency
    """
    result = LatencyTestResult()
    result.request_id = request_id

    if question is None:
        question = TEST_QUESTION

    result.question = question

    # Payload
    payload = {
        "question": question,
        "user_id": f"test_user_{request_id}",
        "top_k": 4
    }

    # Đo thời gian bắt đầu
    start_time = time.time()

    try:
        async with session.post(
            API_URL,
            json=payload,
            timeout=aiohttp.ClientTimeout(total=120)  # Timeout 120 giây
        ) as response:
            # Đo thời gian kết thúc
            end_time = time.time()

            # Tính latency (milliseconds)
            result.latency_ms = (end_time - start_time) * 1000
            result.status_code = response.status

            # Đọc response
            response_data = await response.json()
            result.response_size = len(json.dumps(response_data))

            # Kiểm tra success
            if response.status == 200:
                result.success = True
                # Có thể log answer nếu cần
                # print(f"Request {request_id}: {response_data.get('answer', '')[:50]}")
            else:
                result.success = False
                result.error = response_data.get("detail", f"HTTP {response.status}")

    except asyncio.TimeoutError:
        end_time = time.time()
        result.latency_ms = (end_time - start_time) * 1000
        result.success = False
        result.error = "Timeout (120s)"
        result.status_code = 0

    except Exception as e:
        end_time = time.time()
        result.latency_ms = (end_time - start_time) * 1000
        result.success = False
        result.error = str(e)
        result.status_code = 0

    return result


async def check_service_health() -> bool:
    """
    Kiểm tra xem AI service có đang chạy không

    Returns:
        bool: True nếu service đang chạy, False nếu không
    """
    # Tạo health URL từ API URL
    if API_URL.endswith("/chat"):
        health_url = API_URL.replace("/chat", "/health")
    else:
        # Nếu URL không kết thúc bằng /chat, thử thêm /health
        base_url = API_URL.rstrip("/")
        health_url = f"{base_url}/health"

    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(
                health_url,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    return data.get("status") == "ok"
                return False
    except Exception as e:
        return False


async def run_concurrent_tests(
    num_requests: int = NUM_REQUESTS,
    questions: Optional[List[str]] = None,
    question_type: Optional[str] = None
) -> List[LatencyTestResult]:
    """
    Chạy nhiều requests đồng thời

    Args:
        num_requests: Số lượng requests cần gửi
        questions: Danh sách câu hỏi (nếu None, sẽ load từ file hoặc dùng mặc định)
        question_type: Loại câu hỏi (simple, complex_sql, complex_natural_language, mixed_complexity, rag_questions, all)

    Returns:
        List[LatencyTestResult]: Danh sách kết quả
    """
    # Load câu hỏi nếu chưa có
    if questions is None:
        if question_type:
            questions = load_test_questions(question_type)
        else:
            questions = load_test_questions("mixed_complexity")

    # Chọn câu hỏi cho từng request
    selected_questions = select_questions(questions, num_requests, randomize=True)

    # Hiển thị thông tin
    unique_questions = len(set(selected_questions))
    print(f"\n{'='*60}")
    print(f"🚀 Bắt đầu test latency với {num_requests} requests đồng thời")
    print(f"📡 API URL: {API_URL}")
    if question_type:
        print(f"📝 Loại câu hỏi: {question_type}")
    print(f"❓ Số câu hỏi unique: {unique_questions}/{num_requests}")
    if unique_questions <= 5:
        print(f"❓ Câu hỏi mẫu: {selected_questions[0][:50]}...")
    print(f"{'='*60}\n")

    # Tạo session
    async with aiohttp.ClientSession() as session:
        # Tạo tasks cho tất cả requests với câu hỏi tương ứng
        tasks = [
            send_chat_request(session, i+1, selected_questions[i])
            for i in range(num_requests)
        ]

        # Chạy tất cả requests đồng thời
        print(f"⏳ Đang gửi {num_requests} requests đồng thời...")
        start_time = time.time()
        results = await asyncio.gather(*tasks)
        end_time = time.time()

        total_time = end_time - start_time
        print(f"✅ Hoàn thành trong {total_time:.2f} giây\n")

        return results


def calculate_statistics(results: List[LatencyTestResult]) -> Dict[str, Any]:
    """
    Tính toán thống kê từ kết quả

    Args:
        results: Danh sách kết quả

    Returns:
        Dict chứa các thống kê
    """
    # Lọc các requests thành công
    successful_results = [r for r in results if r.success]
    failed_results = [r for r in results if not r.success]

    if not successful_results:
        return {
            "total": len(results),
            "success": 0,
            "failed": len(failed_results),
            "success_rate": 0.0,
            "error": "Không có request nào thành công"
        }

    # Lấy latencies (milliseconds)
    latencies = [r.latency_ms for r in successful_results]

    # Tính toán thống kê
    stats = {
        "total": len(results),
        "success": len(successful_results),
        "failed": len(failed_results),
        "success_rate": (len(successful_results) / len(results)) * 100,
        "latencies_ms": latencies,
        "min_latency_ms": min(latencies),
        "max_latency_ms": max(latencies),
        "avg_latency_ms": statistics.mean(latencies),
        "median_latency_ms": statistics.median(latencies),
    }

    # Tính percentile
    if len(latencies) > 1:
        sorted_latencies = sorted(latencies)
        stats["p95_latency_ms"] = sorted_latencies[int(len(sorted_latencies) * 0.95)]
        stats["p99_latency_ms"] = sorted_latencies[int(len(sorted_latencies) * 0.99)]
    else:
        stats["p95_latency_ms"] = latencies[0]
        stats["p99_latency_ms"] = latencies[0]

    # Tính standard deviation
    if len(latencies) > 1:
        stats["std_dev_ms"] = statistics.stdev(latencies)
    else:
        stats["std_dev_ms"] = 0.0

    # Lỗi nếu có
    if failed_results:
        error_summary = {}
        for r in failed_results:
            error_key = r.error[:50] if r.error else "Unknown"
            error_summary[error_key] = error_summary.get(error_key, 0) + 1
        stats["errors"] = error_summary

    return stats


def print_results(results: List[LatencyTestResult], stats: Dict[str, Any]):
    """
    In kết quả test ra console

    Args:
        results: Danh sách kết quả
        stats: Thống kê đã tính toán
    """
    print(f"\n{'='*60}")
    print(f"📊 KẾT QUẢ TEST LATENCY")
    print(f"{'='*60}\n")

    # Tổng quan
    print(f"📈 TỔNG QUAN:")
    print(f"   • Tổng số requests: {stats['total']}")
    print(f"   • Thành công: {stats['success']} ({stats['success_rate']:.1f}%)")
    print(f"   • Thất bại: {stats['failed']}")

    if stats.get('errors'):
        print(f"\n   ⚠️  Lỗi:")
        for error, count in stats['errors'].items():
            print(f"      - {error}: {count} lần")

    if stats.get('error'):
        print(f"\n   ❌ {stats['error']}")
        return

    # Latency statistics
    print(f"\n⏱️  LATENCY (milliseconds):")
    print(f"   • Min:     {stats['min_latency_ms']:>10.2f} ms")
    print(f"   • Max:     {stats['max_latency_ms']:>10.2f} ms")
    print(f"   • Average: {stats['avg_latency_ms']:>10.2f} ms")
    print(f"   • Median:  {stats['median_latency_ms']:>10.2f} ms")
    print(f"   • P95:     {stats['p95_latency_ms']:>10.2f} ms")
    print(f"   • P99:     {stats['p99_latency_ms']:>10.2f} ms")
    print(f"   • Std Dev: {stats['std_dev_ms']:>10.2f} ms")

    # Latency trong giây (để dễ đọc)
    print(f"\n⏱️  LATENCY (seconds):")
    print(f"   • Min:     {stats['min_latency_ms']/1000:>10.3f} s")
    print(f"   • Max:     {stats['max_latency_ms']/1000:>10.3f} s")
    print(f"   • Average: {stats['avg_latency_ms']/1000:>10.3f} s")
    print(f"   • Median:  {stats['median_latency_ms']/1000:>10.3f} s")
    print(f"   • P95:     {stats['p95_latency_ms']/1000:>10.3f} s")
    print(f"   • P99:     {stats['p99_latency_ms']/1000:>10.3f} s")

    # Chi tiết từng request
    print(f"\n📋 CHI TIẾT TỪNG REQUEST:")
    print(f"{'ID':<5} {'Latency (ms)':<15} {'Status':<10} {'Success':<10}")
    print(f"{'-'*50}")
    for r in results:
        status = f"HTTP {r.status_code}" if r.status_code > 0 else "Error"
        success = "✅" if r.success else "❌"
        print(f"{r.request_id:<5} {r.latency_ms:>13.2f} {status:<10} {success:<10}")

    print(f"\n{'='*60}\n")


def save_results_to_file(results: List[LatencyTestResult], stats: Dict[str, Any], question_type: Optional[str] = None):
    """
    Lưu kết quả vào file JSON

    Args:
        results: Danh sách kết quả
        stats: Thống kê
        question_type: Loại câu hỏi đã sử dụng
    """
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"latency_test_results_{timestamp}.json"

    output = {
        "timestamp": timestamp,
        "config": {
            "api_url": API_URL,
            "num_requests": len(results),
            "question_type": question_type or "mixed_complexity",
            "test_question": TEST_QUESTION
        },
        "statistics": stats,
        "results": [
            {
                "request_id": r.request_id,
                "question": r.question,
                "latency_ms": r.latency_ms,
                "status_code": r.status_code,
                "success": r.success,
                "error": r.error,
                "response_size": r.response_size
            }
            for r in results
        ]
    }

    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)

    print(f"💾 Kết quả đã được lưu vào: {filename}\n")


async def main():
    """Hàm main"""
    # Parse arguments
    num_requests = NUM_REQUESTS
    question_type = None
    global API_URL

    # Usage: python test_latency.py [num_requests] [question_type] [api_url]
    if len(sys.argv) > 1:
        try:
            num_requests = int(sys.argv[1])
        except ValueError:
            # Có thể là question_type
            if sys.argv[1] in ["simple", "complex_sql", "complex_natural_language", "mixed_complexity", "rag_questions", "all"]:
                question_type = sys.argv[1]
            else:
                print(f"⚠️  Số requests không hợp lệ, sử dụng mặc định: {NUM_REQUESTS}")

    if len(sys.argv) > 2:
        arg2 = sys.argv[2]
        if arg2 in ["simple", "complex_sql", "complex_natural_language", "mixed_complexity", "rag_questions", "all"]:
            question_type = arg2
        elif arg2.startswith("http"):
            API_URL = arg2

    if len(sys.argv) > 3:
        arg3 = sys.argv[3]
        if arg3.startswith("http"):
            API_URL = arg3

    # Kiểm tra service đã chạy chưa
    print(f"🔍 Đang kiểm tra AI service tại {API_URL}...")
    is_healthy = await check_service_health()
    if not is_healthy:
        print(f"\n❌ LỖI: AI service chưa chạy hoặc không thể kết nối!")
        print(f"   URL: {API_URL}")
        print(f"\n💡 Hãy chạy AI service trước:")
        print(f"   1. Chạy trực tiếp: uvicorn app.main:app --reload --port 8000")
        print(f"   2. Hoặc Docker: docker-compose up ai-service")
        print(f"   3. Kiểm tra: curl http://localhost:8000/health")
        sys.exit(1)

    print(f"✅ AI service đang chạy!\n")

    # Hiển thị hướng dẫn sử dụng
    if question_type:
        print(f"📝 Sử dụng loại câu hỏi: {question_type}")
        print(f"   Các loại có sẵn: simple, complex_sql, complex_natural_language, mixed_complexity, rag_questions, all")
        print()

    # Chạy test
    try:
        results = await run_concurrent_tests(num_requests, question_type=question_type)
        stats = calculate_statistics(results)
        print_results(results, stats)
        save_results_to_file(results, stats, question_type)

    except KeyboardInterrupt:
        print("\n\n⚠️  Test bị hủy bởi người dùng")
    except Exception as e:
        print(f"\n\n❌ Lỗi khi chạy test: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    # Chạy test
    asyncio.run(main())


