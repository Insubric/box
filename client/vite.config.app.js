import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { loadEnv } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';

const env = loadEnv(process.env.NODE_ENV, process.cwd());

export default defineConfig({
    base: './',
    server: {
        proxy: {
            '/api': 'http://localhost:8080',
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
                entryFileNames: `ui/[name].${env.VITE_BOX_VERSION}.js`,
                chunkFileNames: `ui/[name].${env.VITE_BOX_VERSION}.js`,
                assetFileNames: `ui/[name].${env.VITE_BOX_VERSION}.[ext]`,
            },
        },
    },
});