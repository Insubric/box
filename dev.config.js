const ScalaJS = require("./scalajs.webpack.config");
const { merge } = require('webpack-merge');
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');


const WebApp = merge(ScalaJS, {
    mode: "development",
    entry: [
        // Runtime code for hot module replacement
        "webpack/hot/dev-server.js",
        // Dev server client for web socket transport, hot and live reload logic
        "webpack-dev-server/client/index.js?hot=true&live-reload=true",
        // Your entry
        "./client-fastopt-library.js",
        "./client-fastopt.js",
    ],
    resolve: {
        fallback: {
            "stream": require.resolve("stream-browserify"),
            "buffer": require.resolve('buffer'),
            "crypto": require.resolve('crypto-browserify'), //if you want to use this module also don't forget npm i crypto-browserify
        }
    },
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: ['style-loader', 'css-loader'],
            },
            {
                test: /\.(woff|woff2|eot|ttf|otf|svg)$/,
                use: ['file-loader']
            }
        ]
    },
    plugins: [
        new HtmlWebpackPlugin(),
        new MonacoWebpackPlugin({
            publicPath: "bundle"
        })
    ],
    devServer: {
        client: {
            overlay: {
                errors: true,
                warnings: false,
            },
        },
    }
});

module.exports = WebApp;