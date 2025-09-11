


const WebApp = {
    output: {
        filename: "client-test-fastopt-bundle.js"
    },
    entry: [
         './client-test-fastopt-loader.js'
    ],
    mode: "development",
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
    resolve: {
        fallback: {
            "stream": require.resolve("stream-browserify"),
            "buffer": require.resolve('buffer'),
            "crypto": require.resolve('crypto-browserify'), //if you want to use this module also don't forget npm i crypto-browserify
        }
    },
    plugins: [
        // new HtmlWebpackPlugin({
        //     'templateContent': ({htmlWebpackPlugin}) => `
        //     <html>
        //       <head>
        //
        //       </head>
        //       <body>
        //         <script src="fixQueryCommandSupported.js"></script>
        //       </body>
        //     </html>
        //   `
        // }),
        // new MonacoWebpackPlugin({
        //     publicPath: "bundle"
        // })
    ]
};

module.exports = WebApp;