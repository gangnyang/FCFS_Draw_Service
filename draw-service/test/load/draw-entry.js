import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || '1');
const DRAW_PATH = __ENV.DRAW_PATH || '/api/v1/draws';
const TOTAL_REQUESTS = Number(__ENV.TOTAL_REQUESTS || '1000');
const DURATION_SECONDS = Number(__ENV.DURATION_SECONDS || '3');
const LOG_REJECTIONS = (__ENV.LOG_REJECTIONS || 'false').toLowerCase() === 'true';
const LOG_FAILURES = (__ENV.LOG_FAILURES || 'false').toLowerCase() === 'true';

const queuedCount = new Counter('draw_queued_count');
const winCount = new Counter('draw_win_count');
const loseCount = new Counter('draw_lose_count');
const soldOutCount = new Counter('draw_sold_out_count');
const alreadyEnteredCount = new Counter('draw_already_entered_count');
const idempotencyConflictCount = new Counter('draw_idempotency_conflict_count');
const lockTimeoutCount = new Counter('draw_lock_timeout_count');
const invalidRequestCount = new Counter('draw_invalid_request_count');
const notFoundCount = new Counter('draw_not_found_count');
const serverErrorCount = new Counter('draw_server_error_count');
const networkErrorCount = new Counter('draw_network_error_count');
const http4xxCount = new Counter('draw_http_4xx_count');
const http5xxCount = new Counter('draw_http_5xx_count');
const parseErrorCount = new Counter('draw_parse_error_count');
const unexpectedErrorCount = new Counter('draw_unexpected_error_count');

export const options = {
  scenarios: {
    draw_ramped_entry: {
      executor: 'constant-arrival-rate',
      rate: Math.ceil(TOTAL_REQUESTS / DURATION_SECONDS),
      timeUnit: '1s',
      duration: `${DURATION_SECONDS}s`,
      preAllocatedVUs: Number(__ENV.VUS || '1000'),
      maxVUs: Number(__ENV.MAX_VUS || '3000'),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.50'],
    http_req_duration: ['p(95)<30000'],
  },
};

export default function () {
  touchAllCounters();

  const requestId = `draw-${__VU}-${__ITER}-${Date.now()}`;
  const userId = (__VU * 1_000_000) + __ITER;
  const payload = JSON.stringify({
    requestId,
    productId: PRODUCT_ID,
    userId,
  });

  const response = http.post(`${BASE_URL}${DRAW_PATH}`, payload, jsonHeaders(requestId));

  // HTTP 성공률은 통신 관점이고, WIN/LOSE는 아래 커스텀 카운터에서 따로 집계한다.
  check(response, {
    'draw response is handled': (res) => res.status >= 200 && res.status < 500,
  });

  recordDrawResult(response, requestId, userId);

  sleep(1);
}

function touchAllCounters() {
  queuedCount.add(0);
  winCount.add(0);
  loseCount.add(0);
  soldOutCount.add(0);
  alreadyEnteredCount.add(0);
  idempotencyConflictCount.add(0);
  lockTimeoutCount.add(0);
  invalidRequestCount.add(0);
  notFoundCount.add(0);
  serverErrorCount.add(0);
  networkErrorCount.add(0);
  http4xxCount.add(0);
  http5xxCount.add(0);
  parseErrorCount.add(0);
  unexpectedErrorCount.add(0);
}

function recordDrawResult(response, requestId, userId) {
  recordHttpStatus(response);

  if (response.error || response.status === 0) {
    networkErrorCount.add(1);
    logFailure(`network error. requestId=${requestId}, userId=${userId}, error=${response.error}`);
    return;
  }
  try {
    const body = JSON.parse(response.body);

    if (!body.success) {
      recordFailureCode(body.code, requestId, userId, response.status, body.data?.message);
      return;
    }

    const result = body.data?.result;
    const failReason = body.data?.failReason;
    const status = body.data?.status;

    if (response.status === 202 && status === 'WAITING') {
      queuedCount.add(1);
      return;
    }

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
    logFailure(`failed to parse response. requestId=${requestId}, status=${response.status}, body=${response.body}`);
  }
}

function recordHttpStatus(response) {
  if (response.status >= 400 && response.status < 500) {
    http4xxCount.add(1);
  } else if (response.status >= 500) {
    http5xxCount.add(1);
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

function recordFailureCode(code, requestId, userId, status, message) {
  if (code === 'IDEMPOTENCY_CONFLICT') {
    idempotencyConflictCount.add(1);
  } else if (code === 'LOCK_TIMEOUT') {
    lockTimeoutCount.add(1);
  } else if (code === 'INVALID_REQUEST') {
    invalidRequestCount.add(1);
  } else if (code === 'PRODUCT_NOT_FOUND' || code === 'NOT_FOUND') {
    notFoundCount.add(1);
  } else if (code === 'INTERNAL_SERVER_ERROR') {
    serverErrorCount.add(1);
  } else {
    unexpectedErrorCount.add(1);
  }

  logFailure(`draw request failed. requestId=${requestId}, userId=${userId}, status=${status}, code=${code}, message=${message}`);
}

function logFailure(message) {
  if (LOG_FAILURES) {
    console.warn(message);
  }
}

function jsonHeaders(traceId) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'X-Trace-Id': traceId,
    },
  };
}
