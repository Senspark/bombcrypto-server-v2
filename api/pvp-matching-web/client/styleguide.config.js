const webpack = require('webpack');

const webpackConfig = {
    module: {
        rules: [
            {
                test: /.\.md$/,
                type: 'javascript/auto',
            },
            {
                test: /\.tsx?$/,
                use: [{
                    loader: 'ts-loader',
                }],
                exclude: /node_modules/,
            },
            {
                test: /\.css$/i,
                use: ["style-loader", "css-loader"],
            },
        ],
    },
    plugins: [
        // Rewrites the absolute paths to those two files into relative paths
        new webpack.NormalModuleReplacementPlugin(
            /react-styleguidist\/lib\/loaders\/utils\/client\/requireInRuntime$/,
            'react-styleguidist/lib/loaders/utils/client/requireInRuntime'
        ),
        new webpack.NormalModuleReplacementPlugin(
            /react-styleguidist\/lib\/loaders\/utils\/client\/evalInContext$/,
            'react-styleguidist/lib/loaders/utils/client/evalInContext'
        ),
    ],
};

module.exports = {
    components: 'src/SwapServerModal/**/*.tsx',
    webpackConfig,
};