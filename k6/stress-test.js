import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = 'http://3.34.181.102:8080';

export const options = {
    stages: [
        {duration: '30s', target: 10},   // 워밍업
        {duration: '1m', target: 30},    // 30명
        {duration: '1m', target: 50},    // 50명
        {duration: '1m', target: 100},   // 100명
        {duration: '1m', target: 150},   // 150명
        {duration: '30s', target: 0},    // 정리
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
    },
};

export default function () {
    const seed = Math.random();
    const res = http.get(`${BASE_URL}/quotes?page=1&count=10&seed=${seed}&language=KOREAN`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);
}