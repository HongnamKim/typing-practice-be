import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = 'http://3.34.181.102:8080';

export const options = {
    stages: [
        {duration: '30s', target: 10},   // 평소 트래픽
        {duration: '1m', target: 10},    // 유지
        {duration: '10s', target: 200},  // 급격히 200명으로 스파이크
        {duration: '1m', target: 200},   // 스파이크 유지
        {duration: '10s', target: 10},   // 급격히 감소
        {duration: '1m', target: 10},    // 회복 확인
        {duration: '30s', target: 0},    // 정리
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
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