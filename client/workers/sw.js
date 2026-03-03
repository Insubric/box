const cacheName = "pwa-assets";
const versionCacheName = "version-cache";

let basePath = ""
self.addEventListener('install', event => {
    basePath = new URL(location).searchParams.get('basePath');

});


const resetCheck = [
    "/bundle/box-app.js",
]

const cachedResources = [
    "/",
    "/bundle/box-app.js",
    "/assets/bootstrap/dist/css/bootstrap.min.css",
    "/assets/@fortawesome/fontawesome-free/js/all.min.js",
    "/assets/flatpickr/dist/flatpickr.min.css",
    "/assets/flatpickr/dist/themes/dark.css",
    "/assets/quill/dist/quill.snow.css",
    "/assets/ol/ol.css",
    "/assets/@fontsource/open-sans/latin-300.css",
    "/assets/@fontsource/open-sans/latin-400.css",
    "/assets/@fontsource/open-sans/latin-600.css",
    "/assets/@fontsource/open-sans/latin-700.css",
    "/assets/@fontsource/open-sans/latin-ext-300.css",
    "/assets/@fontsource/open-sans/latin-ext-400.css",
    "/assets/@fontsource/open-sans/latin-ext-600.css",
    "/assets/@fontsource/open-sans/latin-ext-700.css",
    "/assets/choices.js/public/assets/styles/choices.min.css",
    "/assets/gridstack/dist/gridstack.min.css",
    "/assets/gridstack/dist/gridstack-extra.min.css",
    "/devServer/client-fastopt-library.js?r=hmTUnAjALj",
    "/assets/@fontsource/open-sans/files/open-sans-latin-ext-700-normal.woff2",
    "/assets/@fontsource/open-sans/files/open-sans-latin-ext-600-normal.woff2",
    "/assets/@fontsource/open-sans/files/open-sans-latin-ext-400-normal.woff2",
    "/assets/@fontsource/open-sans/files/open-sans-latin-700-normal.woff2",
    "/assets/@fontsource/open-sans/files/open-sans-latin-600-normal.woff2",

];


function isCacheable(request) {
    return !!cachedResources.find(x => request.url.endsWith(x));
}

function shouldCheckReset(request) {
    return !!resetCheck.find(x => request.url.endsWith(x));
}

async function checkVersion(request) {
    if(shouldCheckReset(request)) {
        const url = basePath + "api/v1/version"
        const versionCache = await caches.open(versionCacheName)
        const cachedVersion = await (await versionCache.match(url))?.json()
        const r = await fetch(url)
        await versionCache.put(url,r.clone())
        const version = await r.json()


        console.log(version + " ---- " + cachedVersion)
        if (version !== cachedVersion) {
            await caches.delete(cacheName)
        }
    }
}

async function cacheResetOnNewVersion(request) {
    checkVersion(request)
    return await cacheFirstWithRefresh(request)
}
async function cacheFirstWithRefresh(request) {
    const maybeResponse = await caches.match(request)
    if(maybeResponse) {
        return maybeResponse
    } else {
        const response = await fetch(request)
        if(response.ok) {
            const cache = await caches.open(cacheName)
            await cache.put(request, response.clone());
        }
        return response;
    }

}

self.addEventListener("fetch", (event) => {
    if (isCacheable(event.request)) {
        event.respondWith(cacheResetOnNewVersion(event.request));
    }
});