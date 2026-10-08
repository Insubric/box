import { defineConfig } from 'vite'
import { execSync } from 'child_process'


function getAppVersion() {
    let version
    try {
        version = execSync('git describe --tags --exact-match HEAD', { encoding: 'utf8' }).trim()
    } catch {
        version = execSync('git rev-parse HEAD', { encoding: 'utf8' }).trim()
    }
    const modified = execSync('git status -s --untracked-files=no', { encoding: 'utf8' }).trim()
    if (modified) {
        version += '-SNAPSHOT'
    }
    return version.replace(/^v/, '')
}

const APP_VERSION = getAppVersion()

export default defineConfig({
    optimizeDeps: {
        exclude: ['@electric-sql/pglite'],
    },
    build: {
        outDir: './dist',
        //watch: {},
        emptyOutDir: false,
        rollupOptions: {
            input: ['./workers/postgres.worker.js','./workers/sw.js'],
            // externalize the package so Rollup doesn't bundle its JS/WASM
            output: {
                // keep emitted asset names predictable
                entryFileNames: `ui/workers/[name].${APP_VERSION}.js`,
                chunkFileNames: `ui/workers/[name].${APP_VERSION}.js`,
                assetFileNames: `ui/workers/[name].${APP_VERSION}.[ext]`,
            },
        }

    },
})
