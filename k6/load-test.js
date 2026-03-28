import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = 'http://3.34.181.102:8080';

export const options = {
    stages: [
        {duration: '30s', target: 10}, // 30초 동안 10명까지 증가
        {duration: '2m', target: 10}, // 2분 동안 10명 유지
        {duration: '30s', target: 0} // 30초 동안 0명으로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'] // p95 요청이 500ms 이내
    }
};

export default function () {
    // 랜덤 문장 조회
    const seed = Math.random();
    const res = http.get(`${BASE_URL}/quotes?page=1&count=10&seed=${seed}&language=KOREAN`);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1); // 사용자 간 1초 대기 (실제 사용 패턴 시뮬레이션)
}