import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_ID = Number(__ENV.USER_ID || '1');
const INITIAL_BALANCE = Number(__ENV.INITIAL_BALANCE || '100000');
const PRICE = Number(__ENV.PRICE || '1000');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 100 },
    { duration: '30s', target: 500 },
    { duration: '30s', target: 1000 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.20'],
    http_req_duration: ['p(95)<1000'],
  },
};

export function setup() {
  const payload = JSON.stringify({ balance: INITIAL_BALANCE });
  const response = http.put(`${BASE_URL}/api/v1/wallets/${USER_ID}`, payload, jsonHeaders('setup-wallet'));

  check(response, {
    'wallet setup succeeded': (res) => res.status === 200,
  });
}

export default function () {
  const requestId = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    requestId,
    userId: USER_ID,
    price: PRICE,
  });

  const response = http.post(`${BASE_URL}/api/v1/payments`, payload, jsonHeaders(requestId));

  check(response, {
    'payment response is handled': (res) => res.status === 200 || res.status === 400,
  });

  sleep(1);
}

function jsonHeaders(traceId) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'X-Trace-Id': traceId,
    },
  };
}
