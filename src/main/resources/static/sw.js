const CACHE_NAME = 'ai-assistant-static-v2';
const APP_SHELL = [
    '/',
    '/index.html',
    '/chat.html',
    '/manifest.json',
    '/css/style.css',
    '/js/login.js',
    '/js/chat.js',
    '/js/pwa.js',
    '/icons/apple-touch-icon.png',
    '/icons/icon-192.png',
    '/icons/icon-512.png'
];
const STATIC_PATHS = new Set(APP_SHELL);

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(APP_SHELL))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys()
            .then(names => Promise.all(
                names.filter(name => name !== CACHE_NAME)
                    .map(name => caches.delete(name))
            ))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    const request = event.request;
    const url = new URL(request.url);

    if (request.method !== 'GET' || url.origin !== self.location.origin) {
        return;
    }

    // 登录状态、会话列表和聊天内容始终走网络，绝不进入 Service Worker 缓存。
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(fetch(request, { cache: 'no-store' }));
        return;
    }

    if (request.mode === 'navigate') {
        // 只缓存两个公开的静态页面，其他导航请求一律不落盘。
        if (STATIC_PATHS.has(url.pathname)) {
            event.respondWith(networkFirst(request));
        }
        return;
    }

    if (STATIC_PATHS.has(url.pathname)) {
        event.respondWith(cacheFirst(request));
    }
});

async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(CACHE_NAME);
            await cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        return (await caches.match(request)) || (await caches.match('/'));
    }
}

async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) {
        return cached;
    }

    const response = await fetch(request);
    if (response.ok) {
        const cache = await caches.open(CACHE_NAME);
        await cache.put(request, response.clone());
    }
    return response;
}
