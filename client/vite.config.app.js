import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { viteStaticCopy } from 'vite-plugin-static-copy';
import fs from 'fs'
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
    base: './',
    server: {
        proxy: {
            '/api': 'http://localhost:8080',
            '/manifest.webmanifest': 'http://localhost:8080',
            '/apple-touch-icon.png': 'http://localhost:8080',
            '/screenshoot': 'http://localhost:8080',
            '/favicon-32x32.png': 'http://localhost:8080',
            '/favicon-16x16.png': 'http://localhost:8080',
            '/icon': 'http://localhost:8080',
            '/pdf': 'http://localhost:8080',
            '/ui/workers': {
                target: 'http://127.0.0.1:5174',
                changeOrigin: true,
                rewrite: (path) => {
                    console.log(path)
                    return path.replace(/^\/ui\/workers/, '')
                },
            }
        },
        // https: {
        //     key: fs.readFileSync('./10.40.1.216-key.pem'),
        //     cert: fs.readFileSync('./10.40.1.216.pem'),
        // },
        cors: true,
        host: '0.0.0.0'
    },
    optimizeDeps: {
        exclude: ['@electric-sql/pglite'],
    },
    resolve: {
        alias: {
            '~bootstrap': './node_modules/bootstrap',
        }
    },
    plugins: [
        scalaJSPlugin({
        // path to the directory containing the sbt build
        // default: '.'
        cwd: '..',

        // sbt project ID from within the sbt build to get fast/fullLinkJS from
        // default: the root project of the sbt build
        projectID: 'client',

        // URI prefix of imports that this plugin catches (without the trailing ':')
        // default: 'scalajs' (so the plugin recognizes URIs starting with 'scalajs:')
        uriPrefix: 'scalajs',
    }),
    viteStaticCopy({
        targets: [
            {
                src: './node_modules/@electric-sql/pglite-repl/dist-webcomponent/*',
                dest: 'public',
                rename: { stripBase: 1 }
            }
        ]
    })
    ],
    build: {
        emptyOutDir: false,
        rollupOptions: {
            output: {
                entryFileNames: `ui/[name].${APP_VERSION}.js`,
                chunkFileNames: `ui/[name].${APP_VERSION}.js`,
                assetFileNames: `ui/[name].${APP_VERSION}.[ext]`,
            },
        },
    },
});