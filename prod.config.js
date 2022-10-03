const ScalaJS = require("./scalajs.webpack.config");
const { merge } = require('webpack-merge');
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');

const WebApp = merge(ScalaJS, {
    mode: "production",
    output: {
        filename: "box-app.js",
        publicPath: "bundle/"
    },
    stats: {
        warningsFilter: (warning) => true,
    },
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
        new MiniCssExtractPlugin({}),
        new MonacoWebpackPlugin({
            publicPath: "bundle"
        })
    ]
});

module.exports = WebApp;