console.log('Worker started');
try {
    import { PGlite } from '@electric-sql/pglite'
    import { worker } from '@electric-sql/pglite/worker'
    worker({
        async init() {
            // Create and return a PGlite instance
            return new PGlite('idb://box-pgdata')
        },
    })
} catch (error) {
    console.error('Error starting worker:', error);
}
self.onmessage = function(event) {
    console.log('Message received in worker:', event.data);
};