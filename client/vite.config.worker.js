import { defineConfig } from 'vite'
import { loadEnv } from 'vite';

const env = loadEnv(process.env.NODE_ENV, process.cwd());

export default defineConfig({
    optimizeDeps: {
        exclude: ['@electric-sql/pglite'],
    },
    build: {
        outDir: './dist',
        emptyOutDir: false,
        rollupOptions: {
            input: ['./workers/postgres.worker.js','./workers/sw.js'],
            // externalize the package so Rollup doesn't bundle its JS/WASM
            output: {
                // keep emitted asset names predictable
                entryFileNames: `ui/[name].${env.VITE_BOX_VERSION}.js`,
                chunkFileNames: `ui/[name].${env.VITE_BOX_VERSION}.js`,
                assetFileNames: `ui/[name].${env.VITE_BOX_VERSION}.[ext]`,
            },
        }

    },
})
