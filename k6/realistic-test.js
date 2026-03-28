import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = 'http://3.34.181.102:8080';

export const options = {
    stages: [
        {duration: '30s', target: 10},
        {duration: '1m', target: 10},
        {duration: '10s', target: 200},
        {duration: '3m', target: 200},
        {duration: '10s', target: 10},
        {duration: '1m', target: 10},
        {duration: '30s', target: 0},
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
    },
};

const TYPO_TYPES = ['INITIAL', 'MEDIAL', 'FINAL', 'LETTER'];

function randomTypos() {
    const count = Math.floor(Math.random() * 3);
    const typos = [];
    for (let i = 0; i < count; i++) {
        typos.push({
            expected: 'ㄱ',
            actual: 'ㄴ',
            position: Math.floor(Math.random() * 50),
            type: TYPO_TYPES[Math.floor(Math.random() * TYPO_TYPES.length)],
        });
    }
    return typos;
}

function submitTypingRecord() {
    const payload = JSON.stringify({
        quoteId: Math.floor(Math.random() * 100) + 1,
        cpm: Math.floor(Math.random() * 300) + 100,
        accuracy: Math.random() * 30 + 70,
        charLength: Math.floor(Math.random() * 100) + 20,
        resetCount: Math.floor(Math.random() * 3),
        typos: randomTypos(),
    });

    const res = http.post(`${BASE_URL}/typing-records`, payload, {
        headers: {'Content-Type': 'application/json'},
    });

    check(res, {
        'write status is 200': (r) => r.status === 200,
    });
}

export default function () {
    // 1. 문장 조회 (한 페이지에 10개)
    const seed = Math.random();
    const res = http.get(`${BASE_URL}/quotes?page=1&count=10&seed=${seed}&language=KOREAN`);

    check(res, {
        'read status is 200': (r) => r.status === 200,
    });

    // 2. 10개 문장을 순서대로 타이핑
    const typingCount = Math.floor(Math.random() * 6) + 5; // 5~10개 문장 타이핑
    for (let i = 0; i < typingCount; i++) {
        sleep(Math.random() * 20 + 10); // 10~30초 타이핑 시간
        submitTypingRecord();
    }
}