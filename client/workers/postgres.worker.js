import { PGlite } from '@electric-sql/pglite'
import { worker } from '@electric-sql/pglite/worker'

console.log('Worker started');
try {
    worker({
        async init() {
            const params = new URLSearchParams(location.search)
            const version = params.get("version")
            const id = params.get("appId")

            const [pgliteWasmModule, initdbWasmModule, fsBundle] = await Promise.all([
                WebAssembly.compileStreaming(fetch(`./pglite.${version}.wasm`)),
                WebAssembly.compileStreaming(fetch(`./initdb.${version}.wasm`)),
                fetch(`./pglite.${version}.data`).then((response) => response.blob()),
            ])
            // Create and return a PGlite instance
            return PGlite.create(`idb://box-pgdata-${id}`,{
                pgliteWasmModule: pgliteWasmModule,
                initdbWasmModule: initdbWasmModule,
                fsBundle: fsBundle
            })
        },
    })
} catch (error) {
    console.error('Error starting worker:', error);
}
self.onmessage = function(event) {
    console.log('Message received in worker:', event.data);
};
