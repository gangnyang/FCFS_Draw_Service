import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || '1');
const LOG_REJECTIONS = (__ENV.LOG_REJECTIONS || 'false').toLowerCase() === 'true';

const winCount = new Counter('draw_win_count');
const loseCount = new Counter('draw_lose_count');
const soldOutCount = new Counter('draw_sold_out_count');
const alreadyEnteredCount = new Counter('draw_already_entered_count');
const idempotencyConflictCount = new Counter('draw_idempotency_conflict_count');
const parseErrorCount = new Counter('draw_parse_error_count');
const unexpectedErrorCount = new Counter('draw_unexpected_error_count');

export const options = {
  stages: [
    { duration: '5s', target: 500 }
  ],
  thresholds: {
    http_req_failed: ['rate<0.20'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  const requestId = `draw-${__VU}-${__ITER}-${Date.now()}`;
  const userId = Number(`${__VU}${__ITER}`);
  const payload = JSON.stringify({
    requestId,
    productId: PRODUCT_ID,
    userId,
  });

  const response = http.post(`${BASE_URL}/api/v1/draws`, payload, jsonHeaders(requestId));

  // HTTP 성공률은 통신 관점이고, WIN/LOSE는 아래 커스텀 카운터에서 따로 집계한다.
  check(response, {
    'draw response is handled': (res) => res.status >= 200 && res.status < 500,
  });

  recordBusinessResult(response, requestId, userId);

  sleep(1);
}

function recordBusinessResult(response, requestId, userId) {
  try {
    const body = JSON.parse(response.body);

    if (!body.success) {
      recordFailureCode(body.code, requestId, userId);
      return;
    }

    const result = body.data?.result;
    const failReason = body.data?.failReason;

    if (result === 'WIN') {
      winCount.add(1);
      return;
    }

    if (result === 'LOSE') {
      loseCount.add(1);
      recordLoseReason(failReason, requestId, userId);
      return;
    }

    unexpectedErrorCount.add(1);
    console.warn(`unexpected draw result. requestId=${requestId}, userId=${userId}, body=${response.body}`);
  } catch (e) {
    parseErrorCount.add(1);
    console.warn(`failed to parse response. requestId=${requestId}, status=${response.status}, body=${response.body}`);
  }
}

function recordLoseReason(failReason, requestId, userId) {
  if (failReason === 'SOLD_OUT') {
    soldOutCount.add(1);
  } else if (failReason === 'ALREADY_ENTERED') {
    alreadyEnteredCount.add(1);
  } else {
    unexpectedErrorCount.add(1);
  }

  if (LOG_REJECTIONS) {
    console.log(`draw rejected. requestId=${requestId}, userId=${userId}, productId=${PRODUCT_ID}, reason=${failReason}`);
  }
}

function recordFailureCode(code, requestId, userId) {
  if (code === 'IDEMPOTENCY_CONFLICT') {
    idempotencyConflictCount.add(1);
  } else {
    unexpectedErrorCount.add(1);
  }

  console.warn(`draw request failed. requestId=${requestId}, userId=${userId}, code=${code}`);
}

function jsonHeaders(traceId) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'X-Trace-Id': traceId,
    },
  };
}
