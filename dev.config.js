const ScalaJS = require("./scalajs.webpack.config");
const { merge } = require('webpack-merge');
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');


const WebApp = merge(ScalaJS, {
    mode: "development",
    entry: [

        // Your entry
        "./client-fastopt.js",
    ],
    output: {
        filename: "client-fastopt-library.js"
    },
    devtool: 'source-map',
    ignoreWarnings: [(warning) => true],
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
            publicPath: "dev"
        })
    ],
    optimization: {
        usedExports: true,
        sideEffects: true,
    },
});

module.exports = WebApp;