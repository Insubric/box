import { PGlite } from '@electric-sql/pglite'
import { worker } from '@electric-sql/pglite/worker'

console.log('Worker started');
try {
    worker({
        async init() {
            const params = new URLSearchParams(location.search)
            const version = params.get("version")

            const [pgliteWasmModule, fsBundle] = await Promise.all([
                WebAssembly.compileStreaming(fetch(`./postgres.${version}.wasm`)),
                fetch(`./postgres.${version}.data`).then((response) => response.blob()),
            ])
            // Create and return a PGlite instance
            return new PGlite('idb://box-pgdata',{
                pgliteWasmModule: pgliteWasmModule,
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
